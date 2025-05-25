package masera.deviajebookingsandpayments.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import masera.deviajebookingsandpayments.clients.FlightClient;
import masera.deviajebookingsandpayments.dtos.bookings.flights.CreateFlightBookingRequestDto;
import masera.deviajebookingsandpayments.dtos.bookings.flights.FlightOfferDto;
import masera.deviajebookingsandpayments.dtos.payments.PaymentRequestDto;
import masera.deviajebookingsandpayments.dtos.payments.PaymentResponseDto;
import masera.deviajebookingsandpayments.dtos.responses.BookAndPayResponseDto;
import masera.deviajebookingsandpayments.dtos.responses.BookingResponseDto;
import masera.deviajebookingsandpayments.dtos.responses.FlightBookingResponseDto;
import masera.deviajebookingsandpayments.entities.Booking;
import masera.deviajebookingsandpayments.entities.FlightBooking;
import masera.deviajebookingsandpayments.entities.Payment;
import masera.deviajebookingsandpayments.repositories.BookingRepository;
import masera.deviajebookingsandpayments.repositories.FlightBookingRepository;
import masera.deviajebookingsandpayments.repositories.PaymentRepository;
import masera.deviajebookingsandpayments.services.interfaces.FlightBookingService;
import masera.deviajebookingsandpayments.services.interfaces.PaymentService;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Implementación del servicio de reservas de vuelos.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FlightBookingServiceImpl implements FlightBookingService {

  private final FlightClient flightClient;
  private final PaymentService paymentService;
  private final BookingRepository bookingRepository;
  private final FlightBookingRepository flightBookingRepository;
  private final PaymentRepository paymentRepository;
  private final ModelMapper modelMapper;

  @Override
  @Transactional
  public BookAndPayResponseDto bookAndPay(CreateFlightBookingRequestDto bookingRequest,
                                          PaymentRequestDto paymentRequest) {

    log.info("Iniciando proceso de reserva y pago para vuelo. Cliente: {}", bookingRequest.getClientId());

    try {
      // 1. Verificar disponibilidad y precio actual de la oferta
      Object flightOffer = bookingRequest.getFlightOffer();
      Object verifiedOffer = verifyFlightOfferPrice(flightOffer);

      if (verifiedOffer == null) {
        return BookAndPayResponseDto.verificationFailed("La oferta seleccionada ya no está disponible");
      }

      // 2. Procesar pago PRIMERO
      log.info("Procesando pago para reserva de vuelo");
      PaymentResponseDto paymentResult = paymentService.processPayment(paymentRequest);

      if (!"APPROVED".equals(paymentResult.getStatus())) {
        log.warn("Pago rechazado: {}", paymentResult.getErrorMessage());
        return BookAndPayResponseDto.paymentFailed(paymentResult.getErrorMessage());
      }

      // 3. Si pago exitoso, crear reserva en Amadeus
      log.info("Pago aprobado, creando reserva en Amadeus");
      Object amadeusResponse = flightClient.createFlightOrder(bookingRequest).block();

      if (amadeusResponse == null) {
        // Pago exitoso pero reserva falló → Reembolsar
        log.error("Reserva falló en Amadeus, iniciando reembolso");
        paymentService.refundPayment(paymentResult.getId());
        return BookAndPayResponseDto.bookingFailed("No se pudo confirmar la reserva. El pago será reembolsado.");
      }

      // 4. Extraer datos de la respuesta de Amadeus
      String externalId = extractExternalId(amadeusResponse);
      FlightOfferDto confirmedOffer = extractConfirmedOffer(amadeusResponse);

      // 5. Guardar en nuestra base de datos
      log.info("Guardando reserva en base de datos");
      Booking savedBooking = saveBookingInDatabase(bookingRequest, confirmedOffer, paymentResult, externalId);

      // 6. Convertir a DTOs de respuesta
      BookingResponseDto bookingResponse = convertToBookingResponse(savedBooking);

      log.info("Reserva de vuelo completada exitosamente. ID: {}", savedBooking.getId());
      return BookAndPayResponseDto.success(bookingResponse);

    } catch (Exception e) {
      log.error("Error inesperado en reserva de vuelo", e);
      return BookAndPayResponseDto.bookingFailed("Error interno: " + e.getMessage());
    }
  }

  @Override
  public FlightBookingResponseDto getBasicBookingInfo(Long bookingId) {
    log.info("Obteniendo información básica de reserva de vuelo: {}", bookingId);

    Optional<FlightBooking> flightBooking = flightBookingRepository.findById(bookingId);

    if (flightBooking.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva de vuelo no encontrada: " + bookingId);
    }

    return convertToFlightBookingResponse(flightBooking.get());
  }

  @Override
  public Object getFullBookingDetails(Long bookingId) {
    log.info("Obteniendo detalles completos de reserva: {}", bookingId);

    // 1. Obtener externalId de nuestra BD
    Optional<FlightBooking> flightBooking = flightBookingRepository.findById(bookingId);

    if (flightBooking.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva no encontrada: " + bookingId);
    }

    String externalId = flightBooking.get().getExternalId();

    if (externalId == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ExternalId no disponible para la reserva: " + bookingId);
    }

    try {
      // 2. Llamar a Amadeus API para obtener detalles completos
      return flightClient.getFlightOrder(externalId).block();
    } catch (Exception e) {
      log.error("Error al obtener detalles de Amadeus: {}", externalId, e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudieron obtener los detalles de la reserva");
    }
  }

  @Override
  @Transactional
  public BookAndPayResponseDto cancelBooking(Long bookingId) {
    log.info("Cancelando reserva de vuelo: {}", bookingId);

    try {
      // 1. Obtener reserva de la BD
      Optional<FlightBooking> flightBookingOpt = flightBookingRepository.findById(bookingId);

      if (flightBookingOpt.isEmpty()) {
        return BookAndPayResponseDto.bookingFailed("Reserva no encontrada");
      }

      FlightBooking flightBooking = flightBookingOpt.get();

      // 2. Verificar si se puede cancelar
      if (FlightBooking.FlightBookingStatus.CANCELLED.equals(flightBooking.getStatus())) {
        return BookAndPayResponseDto.bookingFailed("La reserva ya está cancelada");
      }

      // 3. Cancelar en Amadeus (si la API lo permite)
      String externalId = flightBooking.getExternalId();
      if (externalId != null) {
        try {
          flightClient.cancelFlightOrder(externalId).block();
        } catch (Exception e) {
          log.warn("No se pudo cancelar en Amadeus: {}", e.getMessage());
          // Continuar con cancelación local
        }
      }

      // 4. Actualizar estado en nuestra BD
      flightBooking.setStatus(FlightBooking.FlightBookingStatus.CANCELLED);
      flightBooking.getBooking().setStatus(Booking.BookingStatus.CANCELLED);

      flightBookingRepository.save(flightBooking);
      bookingRepository.save(flightBooking.getBooking());

      // 5. Procesar reembolso si aplica
      paymentService.processRefundForBooking(flightBooking.getBooking().getId());

      log.info("Reserva cancelada exitosamente: {}", bookingId);

      return BookAndPayResponseDto.builder()
              .success(true)
              .message("Reserva cancelada exitosamente")
              .booking(convertToBookingResponse(flightBooking.getBooking()))
              .build();

    } catch (Exception e) {
      log.error("Error al cancelar reserva: {}", bookingId, e);
      return BookAndPayResponseDto.bookingFailed("Error al cancelar: " + e.getMessage());
    }
  }

  @Override
  public Object verifyFlightOfferPrice(Object flightOfferData) {
    log.info("Verificando disponibilidad y precio de oferta de vuelo");

    try {
      return flightClient.verifyFlightOfferPrice(flightOfferData).block();
    } catch (Exception e) {
      log.error("Error al verificar oferta: {}", e.getMessage());
      return null;
    }
  }

  // ============================================================================
  // MÉTODOS PRIVADOS DE UTILIDAD
  // ============================================================================

  private String extractExternalId(Object amadeusResponse) {
    // Extraer el ID de la reserva de la respuesta de Amadeus
    // Implementar según la estructura real de la respuesta

    // Ejemplo (adaptar según la estructura real)
    if (amadeusResponse instanceof java.util.Map) {
      java.util.Map<String, Object> response = (java.util.Map<String, Object>) amadeusResponse;
      if (response.containsKey("data")) {
        Object data = response.get("data");
        if (data instanceof java.util.Map) {
          return (String) ((java.util.Map<String, Object>) data).get("id");
        }
      }
    }
    return "EXT" + System.currentTimeMillis(); // Fallback temporal
  }

  private FlightOfferDto extractConfirmedOffer(Object amadeusResponse) {
    // Extraer la oferta confirmada de la respuesta de Amadeus
    // Implementar según la estructura real de la respuesta

    // Por ahora, devolvemos un objeto vacío
    return new FlightOfferDto();
  }

  @Transactional
  protected Booking saveBookingInDatabase(CreateFlightBookingRequestDto request,
                                          FlightOfferDto confirmedOffer,
                                          PaymentResponseDto payment,
                                          String externalId) {

    // 1. Crear booking principal
    Booking booking = Booking.builder()
            .clientId(request.getClientId())
            .agentId(request.getAgentId())
            .branchId(request.getBranchId())
            .status(Booking.BookingStatus.CONFIRMED)
            .type(Booking.BookingType.FLIGHT)
            .totalAmount(payment.getAmount())
            .currency(payment.getCurrency())
            .discount(BigDecimal.ZERO)
            .taxes(calculateTaxes(confirmedOffer, payment.getAmount()))
            .build();

    Booking savedBooking = bookingRepository.save(booking);

    // 2. Crear flight booking
    FlightBooking flightBooking = FlightBooking.builder()
            .booking(savedBooking)
            .externalId(externalId)
            .origin(extractOrigin(confirmedOffer))
            .destination(extractDestination(confirmedOffer))
            .departureDate(extractDepartureDate(confirmedOffer))
            .returnDate(extractReturnDate(confirmedOffer))
            .carrier(extractCarrier(confirmedOffer))
            .basePrice(payment.getAmount().multiply(new BigDecimal("0.85"))) // Estimado
            .taxes(payment.getAmount().multiply(new BigDecimal("0.15"))) // Estimado
            .discounts(BigDecimal.ZERO)
            .totalPrice(payment.getAmount())
            .currency(payment.getCurrency())
            .status(FlightBooking.FlightBookingStatus.CONFIRMED)
            .build();

    flightBookingRepository.save(flightBooking);

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

  private BigDecimal calculateTaxes(FlightOfferDto offer, BigDecimal totalAmount) {
    // Calcular impuestos basado en la oferta
    // Por simplicidad, usamos un 15% del total
    return totalAmount.multiply(new BigDecimal("0.15"));
  }

  private String extractOrigin(FlightOfferDto offer) {
    // Extraer origen del primer segmento del primer itinerario
    if (offer != null && offer.getItineraries() != null && !offer.getItineraries().isEmpty()
            && offer.getItineraries().get(0).getSegments() != null
            && !offer.getItineraries().get(0).getSegments().isEmpty()) {
      return offer.getItineraries().get(0).getSegments().get(0).getDeparture().getIataCode();
    }
    return "XXX"; // Código de fallback
  }

  private String extractDestination(FlightOfferDto offer) {
    // Extraer destino final del primer itinerario
    if (offer != null && offer.getItineraries() != null && !offer.getItineraries().isEmpty()
            && offer.getItineraries().get(0).getSegments() != null
            && !offer.getItineraries().get(0).getSegments().isEmpty()) {
      int lastIndex = offer.getItineraries().get(0).getSegments().size() - 1;
      return offer.getItineraries().get(0).getSegments().get(lastIndex).getArrival().getIataCode();
    }
    return "XXX"; // Código de fallback
  }

  private LocalDateTime extractDepartureDate(FlightOfferDto offer) {
    // Extraer fecha de salida del primer segmento del primer itinerario
    if (offer != null && offer.getItineraries() != null && !offer.getItineraries().isEmpty()
            && offer.getItineraries().get(0).getSegments() != null
            && !offer.getItineraries().get(0).getSegments().isEmpty()) {
      if (offer.getItineraries().get(0).getSegments().get(0).getDeparture().getAt() != null) {
        return offer.getItineraries().get(0).getSegments().get(0).getDeparture().getAt();
      }
    }
    return LocalDateTime.now().plusDays(7); // Fecha de fallback
  }

  private LocalDateTime extractReturnDate(FlightOfferDto offer) {
    // Extraer fecha de retorno (si existe)
    if (offer != null && offer.getItineraries() != null && offer.getItineraries().size() > 1
            && offer.getItineraries().get(1).getSegments() != null
            && !offer.getItineraries().get(1).getSegments().isEmpty()) {
      int lastIndex = offer.getItineraries().get(1).getSegments().size() - 1;
      if (offer.getItineraries().get(1).getSegments().get(lastIndex).getArrival().getAt() != null) {
        return offer.getItineraries().get(1).getSegments().get(lastIndex).getArrival().getAt();
      }
    }
    return null; // Puede ser null para vuelos solo ida
  }

  private String extractCarrier(FlightOfferDto offer) {
    // Extraer aerolínea principal del primer segmento
    if (offer != null && offer.getItineraries() != null && !offer.getItineraries().isEmpty()
            && offer.getItineraries().get(0).getSegments() != null
            && !offer.getItineraries().get(0).getSegments().isEmpty()) {
      return offer.getItineraries().get(0).getSegments().get(0).getCarrierCode();
    }
    return "XX"; // Código de fallback
  }

  private FlightBookingResponseDto convertToFlightBookingResponse(FlightBooking flightBooking) {
    return modelMapper.map(flightBooking, FlightBookingResponseDto.class);
  }

  private BookingResponseDto convertToBookingResponse(Booking booking) {
    return modelMapper.map(booking, BookingResponseDto.class);
  }
}