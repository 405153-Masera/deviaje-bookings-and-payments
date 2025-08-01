package masera.deviajebookingsandpayments.services.impl;

import java.time.LocalDate;
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
import masera.deviajebookingsandpayments.dtos.responses.BaseResponse;
import masera.deviajebookingsandpayments.dtos.responses.BookingResponseDto;
import masera.deviajebookingsandpayments.dtos.responses.HotelBookingResponseDto;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
  public BaseResponse bookAndPay(CreateHotelBookingRequestDto bookingRequest,
                                 PaymentRequestDto paymentRequest,
                                 PricesDto prices) {

    log.info("Iniciando proceso de reserva y pago para hotel. Cliente: {}",
            bookingRequest.getClientId());

    try {

      // 1. Crear reserva en HotelBeds
      log.info("Creando reserva en HotelBeds");
      Map<String, Object> hotelBedsRequest = prepareHotelBedsBookingRequest(bookingRequest);
      Object hotelBedsResponse = hotelClient.createBooking(hotelBedsRequest).block();

      if (hotelBedsResponse == null) {
        return BaseResponse.bookingFailed(
                "La habitación ya no está disponible."
                        + " Por favor, realice una nueva búsqueda");
      }

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

      if (!"APPROVED".equals(paymentResult.getStatus())) {
        log.warn("Pago rechazado: {}", paymentResult.getErrorMessage());
        return BaseResponse.paymentFailed(paymentResult.getErrorMessage());
      }

      // 5. Actualizar el pago con la reserva
      updatePaymentWithBookingId(paymentResult.getId(), savedBooking.getId());

      // 6. Convertir a DTOs de respuesta
      BookingResponseDto bookingResponse = convertToBookingResponse(savedBooking);

      log.info("Reserva de hotel completada exitosamente. ID: {}", savedBooking.getId());
      return BaseResponse.success(bookingResponse);

    } catch (Exception e) {
      log.error("Error inesperado en reserva de hotel", e);
      return BaseResponse.bookingFailed("Error interno: " + e.getMessage());
    }
  }

  @Override
  public HotelBookingResponseDto getBasicBookingInfo(Long bookingId) {
    log.info("Obteniendo información básica de reserva de hotel: {}", bookingId);

    Optional<HotelBooking> hotelBooking = hotelBookingRepository.findById(bookingId);

    if (hotelBooking.isEmpty()) {
      throw new RuntimeException("Reserva de hotel no encontrada: " + bookingId);
    }

    return convertToHotelBookingResponse(hotelBooking.get());
  }

  @Override
  public Object getFullBookingDetails(Long bookingId) {
    log.info("Obteniendo detalles completos de reserva: {}", bookingId);

    // 1. Obtener externalId de nuestra BD
    Optional<HotelBooking> hotelBooking = hotelBookingRepository.findById(bookingId);

    if (hotelBooking.isEmpty()) {
      throw new RuntimeException("Reserva no encontrada: " + bookingId);
    }

    String externalId = hotelBooking.get().getExternalId();

    if (externalId == null) {
      throw new RuntimeException("ExternalId no disponible para la reserva: " + bookingId);
    }

    try {
      // 2. Llamar a HotelBeds API para obtener detalles completos
      return hotelClient.getBookingDetails(externalId).block();
    } catch (Exception e) {
      log.error("Error al obtener detalles de HotelBeds: {}", externalId, e);
      throw new RuntimeException("No se pudieron obtener los detalles de la reserva");
    }
  }

  @Override
  public Object checkRates(String rateKey) {
    log.info("Verificando disponibilidad de tarifa: {}", rateKey);

    try {
      return hotelClient.checkRates(rateKey).block();
    } catch (Exception e) {
      log.error("Error al verificar tarifa: {}", rateKey, e);
      throw new RuntimeException("No se pudo verificar la disponibilidad de la tarifa");
    }
  }

  // MÉTODOS PRIVADOS DE UTILIDAD

  /**
   * Prepara la solicitud de reserva para HotelBeds.
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
    // Extraer detalles del hotel de la respuesta
    // Implementar según la estructura real
    return (Map<String, Object>) hotelBedsResponse;
  }

  public HotelBooking createHotelBookingEntity(CreateHotelBookingRequestDto request,
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

    return hotelBookingRepository.save(hotelBooking);
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
            .build();

    Booking savedBooking = bookingRepository.save(booking);

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
  public HotelBookingResponseDto convertToHotelBookingResponse(HotelBooking hotelBooking) {
    HotelBookingResponseDto dto = modelMapper.map(hotelBooking, HotelBookingResponseDto.class);
    return dto;
  }

  @Override
  public BookingResponseDto convertToBookingResponse(Booking booking) {
    return modelMapper.map(booking, BookingResponseDto.class);
  }
}
