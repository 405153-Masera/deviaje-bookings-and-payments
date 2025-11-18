package masera.deviajebookingsandpayments.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import masera.deviajebookingsandpayments.clients.HotelClient;
import masera.deviajebookingsandpayments.dtos.bookings.hotels.CreateHotelBookingRequestDto;
import masera.deviajebookingsandpayments.dtos.bookings.hotels.HotelBookingApi;
import masera.deviajebookingsandpayments.dtos.bookings.hotels.HotelBookingResponse;
import masera.deviajebookingsandpayments.dtos.bookings.hotels.PaxDto;
import masera.deviajebookingsandpayments.dtos.bookings.hotels.RoomDto;
import masera.deviajebookingsandpayments.dtos.payments.PaymentRequestDto;
import masera.deviajebookingsandpayments.dtos.payments.PricesDto;
import masera.deviajebookingsandpayments.dtos.responses.BookingReferenceResponse;
import masera.deviajebookingsandpayments.dtos.responses.HotelBookingDetailsDto;
import masera.deviajebookingsandpayments.dtos.responses.PaymentResponseDto;
import masera.deviajebookingsandpayments.entities.BookingEntity;
import masera.deviajebookingsandpayments.entities.HotelBookingEntity;
import masera.deviajebookingsandpayments.exceptions.HotelBedsApiException;
import masera.deviajebookingsandpayments.exceptions.MercadoPagoException;
import masera.deviajebookingsandpayments.repositories.BookingRepository;
import masera.deviajebookingsandpayments.repositories.HotelBookingRepository;
import masera.deviajebookingsandpayments.services.interfaces.BookingService;
import masera.deviajebookingsandpayments.services.interfaces.HotelBookingService;
import masera.deviajebookingsandpayments.services.interfaces.PaymentService;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Implementación del servicio de reservas de hoteles.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HotelBookingServiceImpl implements HotelBookingService {

  private final HotelClient hotelClient;

  private final PaymentService paymentService;

  private final BookingService bookingService;

  private final BookingRepository bookingRepository;

  private final HotelBookingRepository hotelBookingRepository;

  private final ModelMapper modelMapper;

  private final ObjectMapper objectMapper;

  /**
   * Procesa una reserva de hotel y su pago de forma unificada.
   *
   * @param bookingRequest datos de la reserva
   * @param paymentRequest datos del pago
   * @param prices representa los detalles del precio
   * @return respuesta unificada con resultado de la operación
   */
  @Override
  @Transactional
  public BookingReferenceResponse bookAndPay(CreateHotelBookingRequestDto bookingRequest,
                                             PaymentRequestDto paymentRequest,
                                             PricesDto prices) {

    log.info("Iniciando proceso de reserva y pago para hotel. Cliente: {}",
            bookingRequest.getClientId());
    Map<String, Object> hotelBedsRequest = prepareHotelBedsBookingRequest(bookingRequest);
    HotelBookingResponse hotelBedsResponse = hotelClient.createBooking(hotelBedsRequest).block();

    if (hotelBedsResponse == null || hotelBedsResponse.getBooking() == null) {
      throw new HotelBedsApiException("Respuesta inválida de HotelBeds al crear la reserva", 500);
    }
    String hotelBedsReference = hotelBedsResponse.getBooking().getReference();

    try {
      BookingEntity savedBookingEntity = saveBookingInDatabase(
              bookingRequest, prices, hotelBedsReference, hotelBedsResponse.getBooking()
      );

      log.info("Procesando pago para reserva de hotel");
      paymentRequest.setBookingId(savedBookingEntity.getId());
      PaymentResponseDto paymentResult = paymentService.processPayment(paymentRequest);

      bookingService.updatePaymentWithBookingId(paymentResult.getId(), savedBookingEntity.getId());
      return new BookingReferenceResponse(savedBookingEntity.getBookingReference());

    } catch (MercadoPagoException e) {
      log.error("Error en pago. Guardando intento fallido y cancelando en HotelBeds: {}",
              hotelBedsReference);

      cancelInHotelBeds(hotelBedsReference);
      throw e;
    } catch (Exception e) {
      log.error("Error en proceso de reserva y pago para hotel. Cancelando en HotelBeds: {}",
              hotelBedsReference);

      cancelInHotelBeds(hotelBedsReference);
      throw e;
    }
  }

  /**
   * Cancela la reserva en HotelBeds.
   */
  private void cancelInHotelBeds(String hotelBedsReference) {
    try {
      hotelClient.cancelBooking(hotelBedsReference).block();
      log.info("Reserva cancelada en HotelBeds: {}", hotelBedsReference);

    } catch (Exception e) {
      log.info("La reserva no pudo ser cancelada en HotelBeds: {}", hotelBedsReference);
    }
  }

  @Override
  public HotelBookingDetailsDto getBasicBookingInfo(Long bookingId) {
    return hotelBookingRepository.findById(bookingId)
            .map(this::convertToHotelBookingResponse)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Reserva de hotel no encontrada con ID: " + bookingId
            ));
  }

  @Override
  public Object getFullBookingDetails(Long bookingId) {
    log.info("Obteniendo detalles completos de reserva: {}", bookingId);
    HotelBookingDetailsDto hotelBooking = getBasicBookingInfo(bookingId);
    return hotelClient.getBookingDetails(hotelBooking.getExternalId()).block();
  }

  @Override
  public Object checkRates(String rateKey) {
    log.info("Verificando disponibilidad de tarifa: {}", rateKey);
    return hotelClient.checkRates(rateKey).block();
  }

  /**
   * Prepara la solicitud de creación de una reserva.
   *
   * @param request representa la solicitud
   * @return la solicitud
   */
  @Override
  public Map<String, Object> prepareHotelBedsBookingRequest(CreateHotelBookingRequestDto request) {
    Map<String, Object> bookingRequest = new HashMap<>();

    // Datos del titular
    Map<String, String> holder = new HashMap<>();
    holder.put("name", request.getHolder().getName());
    holder.put("surname", request.getHolder().getSurname());

    // Habitaciones
    List<Map<String, Object>> rooms = request.getRooms().stream()
            .map(this::convertRoomToMap)
            .collect(Collectors.toList());

    // Solicitud completa
    bookingRequest.put("holder", holder);
    bookingRequest.put("rooms", rooms);
    bookingRequest.put("clientReference", "Deviaje");
    bookingRequest.put("remark", request.getRemark());
    bookingRequest.put("tolerance", request.getTolerance());
    bookingRequest.put("language", "CAS");
    return bookingRequest;
  }

  /**
   * Llama a HotelBeds para crear la reserva con manejo de errores específico.
   */
  @Override
  public HotelBookingResponse callHotelBedsCreateBooking(Map<String, Object> hotelBedsRequest) {

    return hotelClient.createBooking(hotelBedsRequest).block();
  }

  /**
   * Convierte un objeto RoomDto a un Map para la API de HotelBeds.
   */
  private Map<String, Object> convertRoomToMap(RoomDto room) {
    Map<String, Object> roomMap = new HashMap<>();
    roomMap.put("rateKey", room.getRateKey());

    List<Map<String, String>> paxes = room.getPaxes().stream()
            .map(this::convertPaxToMap)
            .collect(Collectors.toList());

    roomMap.put("paxes", paxes);
    return roomMap;
  }

  /**
   * Convierte un objeto PaxDto a un Map para la API de HotelBeds.
   */
  private Map<String, String> convertPaxToMap(PaxDto pax) {
    Map<String, String> paxMap = new HashMap<>();
    paxMap.put("roomId", pax.getRoomId().toString());
    paxMap.put("type", pax.getType());
    paxMap.put("name", pax.getName());
    paxMap.put("surname", pax.getSurname());
    return paxMap;
  }

  @Override
  public void createHotelBookingEntity(CreateHotelBookingRequestDto request,
                                       BookingEntity bookingEntity,
                                       String externalId,
                                       PricesDto prices,
                                       HotelBookingApi hotelDetails) {

    LocalDate checkIn = LocalDate.parse(hotelDetails.getHotel().getCheckIn());
    LocalDate checkOut = LocalDate.parse(hotelDetails.getHotel().getCheckOut());

    HotelBookingEntity hotelBookingEntity = HotelBookingEntity.builder()
            .bookingEntity(bookingEntity)
            .externalId(externalId)
            .hotelName(hotelDetails.getHotel().getName())
            .destinationName(hotelDetails.getHotel().getDestinationName())
            .countryName(request.getCountryName())
            .roomName(request.getRooms().getFirst().getRoomName())
            .boardName(request.getRooms().getFirst().getBoardName())
            .checkInDate(checkIn)
            .checkOutDate(checkOut)
            .numberOfNights((int) ChronoUnit.DAYS.between(checkIn, checkOut))
            .numberOfRooms(request.getRooms().size())
            .adults(countAdults(request))
            .children(countChildren(request))
            .totalPrice(prices.getNet())
            .taxes(prices.getTaxesHotel())
            .currency(prices.getCurrency())
            .build();
    try {
      hotelBookingEntity.setHotelBooking(objectMapper.writeValueAsString(hotelDetails));
    } catch (JsonProcessingException e) {
      log.error("Error al serializar el hotelBookingEntity", e);
    }
    hotelBookingRepository.save(hotelBookingEntity);
  }

  /**
   * Metodo que guarda la reserva de hoteles en la base de datos.
   *
   * @param request representa la petición de hotelbeds
   * @param payment representa los detalles del precio
   * @param externalId representa el id de hotelbeds
   * @param hotelDetails representa la reserva de hotelbeds
   * @return la reserva ya creada
   */
  @Transactional
  @Override
  public BookingEntity saveBookingInDatabase(CreateHotelBookingRequestDto request,
                                             PricesDto payment,
                                             String externalId,
                                             HotelBookingApi hotelDetails) {

    BookingEntity bookingEntity = BookingEntity.builder()
            .clientId(request.getClientId())
            .agentId(request.getAgentId())
            .status(BookingEntity.BookingStatus.CONFIRMED)
            .type(BookingEntity.BookingType.HOTEL)
            .holderName(request.getHolder().getName() + request.getHolder().getSurname())
            .totalAmount(payment.getTotalAmount())
            .commission(payment.getCommission())
            .discount(payment.getDiscount())
            .taxes(payment.getTaxesFlight().add(payment.getTaxesHotel()))
            .currency(payment.getCurrency())
            .email(request.getHolder().getEmail())
            .phone(request.getHolder().getPhone())
            .countryCallingCode(request.getHolder().getCountryCallingCode())
            .build();

    BookingEntity savedBookingEntity = bookingRepository.save(bookingEntity);

    String bookingReference = this.bookingService
            .generateBookingReference(savedBookingEntity.getId(), savedBookingEntity.getType());
    savedBookingEntity.setBookingReference(bookingReference);
    savedBookingEntity = bookingRepository.save(savedBookingEntity);
    createHotelBookingEntity(request, savedBookingEntity, externalId, payment, hotelDetails);
    return savedBookingEntity;
  }

  @Override
  public Integer countAdults(CreateHotelBookingRequestDto request) {
    return request.getRooms().stream()
            .mapToInt(room -> (int) room.getPaxes().stream()
                    .filter(pax -> "AD".equals(pax.getType()))
                    .count())
            .sum();
  }

  @Override
  public Integer countChildren(CreateHotelBookingRequestDto request) {
    return request.getRooms().stream()
            .mapToInt(room -> (int) room.getPaxes().stream()
                    .filter(pax -> "CH".equals(pax.getType()))
                    .count())
            .sum();
  }

  @Override
  public HotelBookingDetailsDto convertToHotelBookingResponse(
          HotelBookingEntity hotelBookingEntity) {
    return modelMapper.map(hotelBookingEntity, HotelBookingDetailsDto.class);
  }
}
