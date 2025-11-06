package masera.deviajebookingsandpayments.services.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import masera.deviajebookingsandpayments.clients.HotelClient;
import masera.deviajebookingsandpayments.dtos.bookings.hotels.CreateHotelBookingRequestDto;
import masera.deviajebookingsandpayments.dtos.bookings.hotels.PaxDto;
import masera.deviajebookingsandpayments.dtos.bookings.hotels.RoomDto;
import masera.deviajebookingsandpayments.dtos.payments.PaymentRequestDto;
import masera.deviajebookingsandpayments.dtos.payments.PricesDto;
import masera.deviajebookingsandpayments.dtos.responses.BookingResponseDto;
import masera.deviajebookingsandpayments.dtos.responses.HotelBookingDetailsDto;
import masera.deviajebookingsandpayments.dtos.responses.PaymentResponseDto;
import masera.deviajebookingsandpayments.entities.Booking;
import masera.deviajebookingsandpayments.entities.HotelBooking;
import masera.deviajebookingsandpayments.entities.Payment;
import masera.deviajebookingsandpayments.repositories.BookingRepository;
import masera.deviajebookingsandpayments.repositories.HotelBookingRepository;
import masera.deviajebookingsandpayments.repositories.PaymentRepository;
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

  private final BookingRepository bookingRepository;

  private final HotelBookingRepository hotelBookingRepository;

  private final PaymentRepository paymentRepository;

  private final ModelMapper modelMapper;

  @Override
  @Transactional
  public String bookAndPay(CreateHotelBookingRequestDto bookingRequest,
                                 PaymentRequestDto paymentRequest,
                                 PricesDto prices) {

    log.info("Iniciando proceso de reserva y pago para hotel. Cliente: {}",
            bookingRequest.getClientId());

    // 1. Crear reserva en HotelBeds (lanza HotelBedsApiException si falla)
    Map<String, Object> hotelBedsRequest = prepareHotelBedsBookingRequest(bookingRequest);
    Object hotelBedsResponse = hotelClient.createBooking(hotelBedsRequest).block();

    // 2. Extraer datos de la respuesta de HotelBeds
    String externalId = extractExternalId(hotelBedsResponse);
    Map<String, Object> hotelDetails = extractHotelDetails(hotelBedsResponse);

    // 3. Guardar en nuestra base de datos
    log.info("Guardando reserva en base de datos");
    Booking savedBooking = saveBookingInDatabase(bookingRequest,
            prices, externalId, hotelDetails);

    // 4. Procesar pago PRIMERO
    log.info("Procesando pago para reserva de hotel");
    PaymentResponseDto paymentResult = paymentService.processPayment(paymentRequest);

      // 5. Actualizar el pago con la reserva
    updatePaymentWithBookingId(paymentResult.getId(), savedBooking.getId());
    return  savedBooking.getBookingReference();
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

  // MÉTODOS DE UTILIDAD

  /**
   * Prepara la solicitud de creación de una reserva
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

    return bookingRequest;
  }

  /**
   * Llama a HotelBeds para crear la reserva con manejo de errores específico
   */
  @Override
  public Object callHotelBedsCreateBooking(Map<String, Object> hotelBedsRequest) {
      return hotelClient.createBooking(hotelBedsRequest).block();
  }

  @Override
  public String generateBookingReference(Long bookingId, Booking.BookingType type) {
    String prefix = switch (type) {
      case FLIGHT -> "FL";
      case HOTEL -> "HT";
      case PACKAGE -> "PK";
    };

    String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    String paddedId = String.format("%05d", bookingId);

    return prefix + "-" + date + "-" + paddedId;
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

  /**
   * Actualiza el pago con el ID de la reserva.
   */
  @Override
  public void updatePaymentWithBookingId(Long paymentId, Long bookingId) {
    Optional<Payment> paymentOpt = paymentRepository.findById(paymentId);
    if (paymentOpt.isPresent()) {
      Payment payment = paymentOpt.get();
      Booking booking = bookingRepository.findById(bookingId).orElse(null);
      if (booking != null) {
        payment.setBooking(booking);
        paymentRepository.save(payment);
      }
    }
  }

  @Override
  public String extractExternalId(Object hotelBedsResponse) {
    // Extraer el 'reference' de la respuesta de HotelBeds
    // Implementar según la estructura real de la respuesta
    if (hotelBedsResponse instanceof Map) {
      Map<String, Object> response = (Map<String, Object>) hotelBedsResponse;
      Object booking = response.get("booking");
      if (booking instanceof Map) {
        return (String) ((Map<String, Object>) booking).get("reference");
      }
    }
    return "HOTEL_" + System.currentTimeMillis(); // Fallback temporal
  }

  @Override
  public Map<String, Object> extractHotelDetails(Object hotelBedsResponse) {
    return (Map<String, Object>) hotelBedsResponse;
  }

  @Override
  public void createHotelBookingEntity(CreateHotelBookingRequestDto request,
                                       Booking booking,
                                       String externalId,
                                       PricesDto prices,
                                       Map<String, Object> hotelDetails) {

    String firstRateKey = request.getRooms().getFirst().getRateKey();
    LocalDate checkIn = extractCheckInDate(firstRateKey);
    LocalDate checkOut = extractCheckOutDate(firstRateKey);

    HotelBooking hotelBooking = HotelBooking.builder()
            .booking(booking)
            .externalId(externalId)
            .hotelName(extractHotelName(hotelDetails))
            .destinationName(extractDestinationName(hotelDetails))
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
            .cancellationFrom(request.getCancellationFrom())
            .cancellationAmount(request.getCancellationAmount())
            .build();

    hotelBookingRepository.save(hotelBooking);
  }

  @Transactional
  @Override
  public Booking saveBookingInDatabase(CreateHotelBookingRequestDto request,
                                       PricesDto payment,
                                       String externalId,
                                       Map<String, Object> hotelDetails) {

    // 1. Crear booking principal
    Booking booking = Booking.builder()
            .clientId(request.getClientId())
            .agentId(request.getAgentId())
            .status(Booking.BookingStatus.CONFIRMED)
            .type(Booking.BookingType.HOTEL)
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

    Booking savedBooking = bookingRepository.save(booking);

    String bookingReference = generateBookingReference(savedBooking.getId(), savedBooking.getType());
    savedBooking.setBookingReference(bookingReference);
    savedBooking = bookingRepository.save(savedBooking);

    createHotelBookingEntity(request, savedBooking, externalId, payment, hotelDetails);
    return savedBooking;
  }

  @Override
  public LocalDate extractCheckInDate(String rateKey) {
    // Extraer fecha de check-in del rateKey (formato: 20250615|20250620|...)
    String[] parts = rateKey.split("\\|");
    if (parts.length > 0) {
      String dateStr = parts[0]; // 20250615
      return LocalDate.parse(dateStr, java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
    }
    return LocalDate.now().plusDays(7); // Default
  }

  @Override
  public LocalDate extractCheckOutDate(String rateKey) {
    // Extraer fecha de check-out del rateKey
    String[] parts = rateKey.split("\\|");
    if (parts.length > 1) {
      String dateStr = parts[1]; // 20250620
      return LocalDate.parse(dateStr, java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
    }
    return LocalDate.now().plusDays(10); // Default
  }


  private String extractHotelCode(Map<String, Object> hotelDetails) {
    if (hotelDetails.containsKey("booking") && hotelDetails.get("booking") instanceof Map) {
      Map<String, Object> booking = (Map<String, Object>) hotelDetails.get("booking");
      if (booking.containsKey("hotel") && booking.get("hotel") instanceof Map) {
        Map<String, Object> hotel = (Map<String, Object>) booking.get("hotel");
        if (hotel.containsKey("code")) {
          return String.valueOf(hotel.get("code"));
        }
      }
    }
    return "HOTEL_001"; // Placeholder
  }

  @Override
  public String extractHotelName(Map<String, Object> hotelDetails) {
    if (hotelDetails.containsKey("booking") && hotelDetails.get("booking") instanceof Map) {
      Map<String, Object> booking = (Map<String, Object>) hotelDetails.get("booking");
      if (booking.containsKey("hotel") && booking.get("hotel") instanceof Map) {
        Map<String, Object> hotel = (Map<String, Object>) booking.get("hotel");
        if (hotel.containsKey("name")) {
          return String.valueOf(hotel.get("name"));
        }
      }
    }
    return "Hotel Name"; // Placeholder
  }

  private String extractDestinationCode(Map<String, Object> hotelDetails) {
    if (hotelDetails.containsKey("booking") && hotelDetails.get("booking") instanceof Map) {
      Map<String, Object> booking = (Map<String, Object>) hotelDetails.get("booking");
      if (booking.containsKey("hotel") && booking.get("hotel") instanceof Map) {
        Map<String, Object> hotel = (Map<String, Object>) booking.get("hotel");
        if (hotel.containsKey("destinationCode")) {
          return String.valueOf(hotel.get("destinationCode"));
        }
      }
    }
    return "MAD"; // Placeholder
  }

  @Override
  public String extractDestinationName(Map<String, Object> hotelDetails) {
    if (hotelDetails.containsKey("booking") && hotelDetails.get("booking") instanceof Map) {
      Map<String, Object> booking = (Map<String, Object>) hotelDetails.get("booking");
      if (booking.containsKey("hotel") && booking.get("hotel") instanceof Map) {
        Map<String, Object> hotel = (Map<String, Object>) booking.get("hotel");
        if (hotel.containsKey("destinationName")) {
          return String.valueOf(hotel.get("destinationName"));
        }
      }
    }
    return "Madrid"; // Placeholder
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
  public HotelBookingDetailsDto convertToHotelBookingResponse(HotelBooking hotelBooking) {
    return modelMapper.map(hotelBooking, HotelBookingDetailsDto.class);
  }

  @Override
  public BookingResponseDto convertToBookingResponse(Booking booking) {
    return modelMapper.map(booking, BookingResponseDto.class);
  }
}
