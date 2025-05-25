package masera.deviajebookingsandpayments.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import masera.deviajebookingsandpayments.clients.HotelClient;
import masera.deviajebookingsandpayments.services.interfaces.HotelBookingService;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;

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
  public BookAndPayResponseDto bookAndPay(CreateHotelBookingRequestDto bookingRequest,
                                          PaymentRequestDto paymentRequest) {

    log.info("Iniciando proceso de reserva y pago para hotel. Cliente: {}", bookingRequest.getClientId());

    try {
      // 1. Verificar disponibilidad y precio actual
      String rateKey = extractRateKey(bookingRequest);
      Object rateVerification = checkRates(rateKey);

      if (!isRateAvailable(rateVerification)) {
        return BookAndPayResponseDto.verificationFailed("La tarifa seleccionada ya no está disponible");
      }

      // 2. Procesar pago PRIMERO
      log.info("Procesando pago para reserva de hotel");
      PaymentResponseDto paymentResult = paymentService.processPayment(paymentRequest);

      if (!"APPROVED".equals(paymentResult.getStatus())) {
        log.warn("Pago rechazado: {}", paymentResult.getErrorMessage());
        return BookAndPayResponseDto.paymentFailed(paymentResult.getErrorMessage());
      }

      // 3. Si pago exitoso, crear reserva en HotelBeds
      log.info("Pago aprobado, creando reserva en HotelBeds");
      Object hotelBedsResponse = hotelClient.createBooking(bookingRequest).block();

      if (hotelBedsResponse == null) {
        // Pago exitoso pero reserva falló → Reembolsar
        log.error("Reserva falló en HotelBeds, iniciando reembolso");
        paymentService.refundPayment(paymentResult.getId());
        return BookAndPayResponseDto.bookingFailed("No se pudo confirmar la reserva. El pago será reembolsado.");
      }

      // 4. Extraer datos de la respuesta de HotelBeds
      String externalId = extractExternalId(hotelBedsResponse);
      Map<String, Object> hotelDetails = extractHotelDetails(hotelBedsResponse);

      // 5. Guardar en nuestra base de datos
      log.info("Guardando reserva en base de datos");
      Booking savedBooking = saveBookingInDatabase(bookingRequest, paymentResult, externalId, hotelDetails);

      // 6. Convertir a DTOs de respuesta
      HotelBookingResponseDto bookingResponse = convertToHotelBookingResponse(savedBooking.getHotelBookings().get(0));

      log.info("Reserva de hotel completada exitosamente. ID: {}", savedBooking.getId());
      return BookAndPayResponseDto.success(
              convertToBookingResponse(savedBooking),
              paymentResult
      );

    } catch (Exception e) {
      log.error("Error inesperado en reserva de hotel", e);
      return BookAndPayResponseDto.bookingFailed("Error interno: " + e.getMessage());
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
      // Nota: HotelBeds normalmente tiene un endpoint GET /bookings/{reference}
      return hotelClient.getBookingDetails(externalId).block();
    } catch (Exception e) {
      log.error("Error al obtener detalles de HotelBeds: {}", externalId, e);
      throw new RuntimeException("No se pudieron obtener los detalles de la reserva");
    }
  }

  @Override
  @Transactional
  public BookAndPayResponseDto cancelBooking(Long bookingId) {
    log.info("Cancelando reserva de hotel: {}", bookingId);

    try {
      // 1. Obtener reserva de la BD
      Optional<HotelBooking> hotelBookingOpt = hotelBookingRepository.findById(bookingId);

      if (hotelBookingOpt.isEmpty()) {
        return BookAndPayResponseDto.bookingFailed("Reserva no encontrada");
      }

      HotelBooking hotelBooking = hotelBookingOpt.get();

      // 2. Verificar si se puede cancelar
      if ("CANCELLED".equals(hotelBooking.getStatus().name())) {
        return BookAndPayResponseDto.bookingFailed("La reserva ya está cancelada");
      }

      // 3. Cancelar en HotelBeds (si la API lo permite)
      String externalId = hotelBooking.getExternalId();
      if (externalId != null) {
        try {
          hotelClient.cancelBooking(externalId).block();
        } catch (Exception e) {
          log.warn("No se pudo cancelar en HotelBeds: {}", e.getMessage());
          // Continuar con cancelación local
        }
      }

      // 4. Actualizar estado en nuestra BD
      hotelBooking.setStatus(HotelBooking.HotelBookingStatus.CANCELLED);
      hotelBooking.getBooking().setStatus(Booking.BookingStatus.CANCELLED);

      hotelBookingRepository.save(hotelBooking);
      bookingRepository.save(hotelBooking.getBooking());

      // 5. Procesar reembolso si aplica
      paymentService.processRefundForBooking(hotelBooking.getBooking().getId());

      log.info("Reserva cancelada exitosamente: {}", bookingId);

      return BookAndPayResponseDto.builder()
              .success(true)
              .message("Reserva cancelada exitosamente")
              .booking(convertToBookingResponse(hotelBooking.getBooking()))
              .build();

    } catch (Exception e) {
      log.error("Error al cancelar reserva: {}", bookingId, e);
      return BookAndPayResponseDto.bookingFailed("Error al cancelar: " + e.getMessage());
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

  // ============================================================================
  // MÉTODOS PRIVADOS DE UTILIDAD
  // ============================================================================

  private String extractRateKey(CreateHotelBookingRequestDto request) {
    if (request.getRooms() == null || request.getRooms().isEmpty()) {
      throw new RuntimeException("No se encontraron habitaciones en la solicitud");
    }
    return request.getRooms().get(0).getRateKey();
  }

  private boolean isRateAvailable(Object rateVerification) {
    // Implementar lógica según la respuesta de HotelBeds
    // Por ejemplo, verificar si hay errores o si el rate está disponible
    return rateVerification != null;
  }

  private String extractExternalId(Object hotelBedsResponse) {
    // Extraer el 'reference' de la respuesta de HotelBeds
    // Implementar según la estructura real de la respuesta
    if (hotelBedsResponse instanceof Map) {
      Map<String, Object> response = (Map<String, Object>) hotelBedsResponse;
      Object booking = response.get("booking");
      if (booking instanceof Map) {
        return (String) ((Map<String, Object>) booking).get("reference");
      }
    }
    return null;
  }

  private Map<String, Object> extractHotelDetails(Object hotelBedsResponse) {
    // Extraer detalles del hotel de la respuesta
    // Implementar según la estructura real
    return (Map<String, Object>) hotelBedsResponse;
  }

  @Transactional
  protected Booking saveBookingInDatabase(CreateHotelBookingRequestDto request,
                                          PaymentResponseDto payment,
                                          String externalId,
                                          Map<String, Object> hotelDetails) {

    // 1. Crear booking principal
    Booking booking = Booking.builder()
            .clientId(request.getClientId())
            .agentId(request.getAgentId())
            .branchId(request.getBranchId())
            .status(Booking.BookingStatus.CONFIRMED)
            .type(Booking.BookingType.HOTEL)
            .totalAmount(payment.getAmount())
            .currency(payment.getCurrency())
            .discount(BigDecimal.ZERO)
            .taxes(BigDecimal.ZERO)
            .build();

    Booking savedBooking = bookingRepository.save(booking);

    // 2. Crear hotel booking
    String firstRateKey = request.getRooms().get(0).getRateKey();
    LocalDate checkIn = extractCheckInDate(firstRateKey);
    LocalDate checkOut = extractCheckOutDate(firstRateKey);

    HotelBooking hotelBooking = HotelBooking.builder()
            .booking(savedBooking)
            .externalId(externalId)
            .hotelCode(extractHotelCode(hotelDetails))
            .hotelName(extractHotelName(hotelDetails))
            .destinationCode(extractDestinationCode(hotelDetails))
            .destinationName(extractDestinationName(hotelDetails))
            .checkInDate(checkIn)
            .checkOutDate(checkOut)
            .numberOfNights((int) ChronoUnit.DAYS.between(checkIn, checkOut))
            .numberOfRooms(request.getRooms().size())
            .adults(countAdults(request))
            .children(countChildren(request))
            .basePrice(payment.getAmount().multiply(new BigDecimal("0.85"))) // Estimado
            .taxes(payment.getAmount().multiply(new BigDecimal("0.15"))) // Estimado
            .discounts(BigDecimal.ZERO)
            .totalPrice(payment.getAmount())
            .currency(payment.getCurrency())
            .status(HotelBooking.HotelBookingStatus.CONFIRMED)
            .build();

    hotelBookingRepository.save(hotelBooking);

    // 3. Guardar el pago
    Payment paymentEntity = Payment.builder()
            .booking(savedBooking)
            .amount(payment.getAmount())
            .currency(payment.getCurrency())
            .method(payment.getMethod())
            .paymentProvider(payment.getPaymentProvider())
            .externalPaymentId(payment.getExternalPaymentId())
            .status(Payment.PaymentStatus.valueOf(payment.getStatus()))
            .build();

    paymentRepository.save(paymentEntity);

    return savedBooking;
  }

  private LocalDate extractCheckInDate(String rateKey) {
    // Extraer fecha de check-in del rateKey (formato: 20250615|20250620|...)
    String[] parts = rateKey.split("\\|");
    if (parts.length > 0) {
      String dateStr = parts[0]; // 20250615
      return LocalDate.parse(dateStr, java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
    }
    return LocalDate.now().plusDays(7); // Default
  }

  private LocalDate extractCheckOutDate(String rateKey) {
    // Extraer fecha de check-out del rateKey
    String[] parts = rateKey.split("\\|");
    if (parts.length > 1) {
      String dateStr = parts[1]; // 20250620
      return LocalDate.parse(dateStr, java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
    }
    return LocalDate.now().plusDays(10); // Default
  }

  private String extractHotelCode(Map<String, Object> hotelDetails) {
    // Implementar según estructura de HotelBeds
    return "HOTEL_001"; // Placeholder
  }

  private String extractHotelName(Map<String, Object> hotelDetails) {
    return "Hotel Name"; // Placeholder
  }

  private String extractDestinationCode(Map<String, Object> hotelDetails) {
    return "MAD"; // Placeholder
  }

  private String extractDestinationName(Map<String, Object> hotelDetails) {
    return "Madrid"; // Placeholder
  }

  private Integer countAdults(CreateHotelBookingRequestDto request) {
    return request.getRooms().stream()
            .mapToInt(room -> (int) room.getPaxes().stream()
                    .filter(pax -> "AD".equals(pax.getType()))
                    .count())
            .sum();
  }

  private Integer countChildren(CreateHotelBookingRequestDto request) {
    return request.getRooms().stream()
            .mapToInt(room -> (int) room.getPaxes().stream()
                    .filter(pax -> "CH".equals(pax.getType()))
                    .count())
            .sum();
  }

  private HotelBookingResponseDto convertToHotelBookingResponse(HotelBooking hotelBooking) {
    HotelBookingResponseDto dto = modelMapper.map(hotelBooking, HotelBookingResponseDto.class);

    // Campos calculados
    dto.setOccupancyDescription(buildOccupancyDescription(hotelBooking.getAdults(), hotelBooking.getChildren()));
    dto.setStayDescription(buildStayDescription(hotelBooking.getNumberOfNights(), hotelBooking.getDestinationName()));
    dto.setPricePerNight(hotelBooking.getTotalPrice().divide(new BigDecimal(hotelBooking.getNumberOfNights())));

    return dto;
  }

  private BookingResponseDto convertToBookingResponse(Booking booking) {
    return modelMapper.map(booking, BookingResponseDto.class);
  }

  private String buildOccupancyDescription(Integer adults, Integer children) {
    if (children == 0) {
      return adults + (adults == 1 ? " adulto" : " adultos");
    }
    return adults + (adults == 1 ? " adulto" : " adultos") +
            ", " + children + (children == 1 ? " niño" : " niños");
  }

  private String buildStayDescription(Integer nights, String destination) {
    return nights + (nights == 1 ? " noche" : " noches") + " en " + destination;
  }
}
