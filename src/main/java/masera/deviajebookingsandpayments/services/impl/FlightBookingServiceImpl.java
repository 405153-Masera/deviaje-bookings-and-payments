package masera.deviajebookingsandpayments.services.impl;

import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import masera.deviajebookingsandpayments.clients.FlightClient;
import masera.deviajebookingsandpayments.dtos.bookings.flights.CreateFlightBookingRequestDto;
import masera.deviajebookingsandpayments.dtos.bookings.flights.FlightOfferDto;
import masera.deviajebookingsandpayments.dtos.payments.PaymentRequestDto;
import masera.deviajebookingsandpayments.dtos.payments.PricesDto;
import masera.deviajebookingsandpayments.dtos.responses.BookAndPayResponseDto;
import masera.deviajebookingsandpayments.dtos.responses.BookingResponseDto;
import masera.deviajebookingsandpayments.dtos.responses.FlightBookingResponseDto;
import masera.deviajebookingsandpayments.dtos.responses.PaymentResponseDto;
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
                                          PaymentRequestDto paymentRequest, PricesDto prices) {

    log.info("Iniciando proceso de reserva y pago para vuelo. Cliente: {}",
            bookingRequest.getClientId());

    log.info("Datos del pago: {}", paymentRequest.getAmount());

    try {

      // 1. Crear la reserva en Amadeus
      log.info("Creando reserva en Amadeus");
      Object amadeusBookingData = prepareAmadeusBookingData(bookingRequest);
      Object amadeusResponse = flightClient.createFlightOrder(amadeusBookingData).block();

      if (amadeusResponse == null) {
        log.error("Reserva falló en Amadeus: respuesta nula");
        return BookAndPayResponseDto.bookingFailed(
                "El vuelo seleccionado ya no está disponible. "
                        + "Por favor, realice una nueva búsqueda");
      }

      // 2. Extraer datos de la respuesta de Amadeus
      String externalId = extractExternalId(amadeusResponse);

      // 3. Guardar en nuestra base de datos
      log.info("Guardando reserva en base de datos");
      FlightOfferDto flightOffer = bookingRequest.getFlightOffer();
      Booking savedBooking = saveBookingInDatabase(bookingRequest,
              flightOffer, prices, externalId);

      // 4. Procesar pago PRIMERO
      log.info("Procesando pago para reserva de vuelo");
      PaymentResponseDto paymentResult = paymentService.processPayment(paymentRequest);

      if (!"APPROVED".equals(paymentResult.getStatus())) {
        log.warn("Pago rechazado: {}", paymentResult.getErrorMessage());
        return BookAndPayResponseDto.paymentFailed("Pago rechazado. "
                + "Datos incorrectos o fondos insuficientes. ");
      }

      // 5. Asociar el pago con la reserva
      updatePaymentWithBookingId(paymentResult.getId(), savedBooking.getId());

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
      throw new ResponseStatusException(HttpStatus.NOT_FOUND,
              "Reserva de vuelo no encontrada: " + bookingId);
    }

    return convertToFlightBookingResponse(flightBooking.get());
  }

  @Override
  public Object getFullBookingDetails(Long bookingId) {
    log.info("Obteniendo detalles completos de reserva: {}", bookingId);

    // 1. Obtener externalId de nuestra BD
    Optional<FlightBooking> flightBooking = flightBookingRepository.findById(bookingId);

    if (flightBooking.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND,
              "Reserva no encontrada: " + bookingId);
    }

    String externalId = flightBooking.get().getExternalId();

    if (externalId == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
              "ExternalId no disponible para la reserva: " + bookingId);
    }

    try {
      // 2. Llamar a Amadeus API para obtener detalles completos
      return flightClient.getFlightOrder(externalId).block();
    } catch (Exception e) {
      log.error("Error al obtener detalles de Amadeus: {}", externalId, e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
              "No se pudieron obtener los detalles de la reserva");
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

  // MÉTODOS PRIVADOS DE UTILIDAD

  /**
   * Prepara los datos de la reserva en el formato requerido por Amadeus.
   */
  public Object prepareAmadeusBookingData(
          CreateFlightBookingRequestDto bookingRequest) {

    return java.util.Map.of(
            "data", java.util.Map.of(
                    "type", "flight-order",
                    "flightOffers", java.util.List.of(bookingRequest.getFlightOffer()),
                    "travelers", bookingRequest.getTravelers(),
                    "remarks", java.util.Map.of(
                            "general", java.util.List.of(
                                    java.util.Map.of(
                                            "subType", "GENERAL_MISCELLANEOUS",
                                            "text", "BOOKING FROM DEVIAJE"
                                    )
                            )
                    ),
                    "ticketingAgreement", java.util.Map.of(
                            "option", "CONFIRM"
                    )
            )
    );
  }

  /**
   * Actualiza el pago con el ID de la reserva.
   */
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

  /**
   * Extrae el ID externo de la respuesta de Amadeus.
   * Si no se encuentra, genera un ID temporal.
   *
   * @param amadeusResponse respuesta de Amadeus
   * @return ID externo o temporal
   */
  public String extractExternalId(Object amadeusResponse) {
    if (amadeusResponse instanceof Map) {
      Map<String, Object> response = (Map<String, Object>) amadeusResponse;
      if (response.containsKey("data")) {
        Object data = response.get("data");
        if (data instanceof Map) {
          return (String) ((Map<String, Object>) data).get("id");
        }
      }
    }
    return "EXT" + System.currentTimeMillis(); // Fallback temporal
  }

  /**
   * Guarda la reserva de vuelo y el pago en la base de datos.
   *
   * @param request datos de la reserva
   * @param flightOffer datos de la oferta de vuelo
   * @param payment datos del pago
   * @param externalId ID externo de la reserva en Amadeus
   * @return la reserva guardada
   */
  @Transactional
  protected Booking saveBookingInDatabase(CreateFlightBookingRequestDto request,
                                          FlightOfferDto flightOffer,
                                          PricesDto payment,
                                          String externalId) {

    // 1. Crear booking principal
    Booking booking = Booking.builder()
            .clientId(request.getClientId())
            .agentId(request.getAgentId())
            .status(Booking.BookingStatus.CONFIRMED)
            .type(Booking.BookingType.FLIGHT)
            .totalAmount(payment.getTotalAmount())
            .commission(payment.getCommission())
            .discount(payment.getDiscount())
            .taxes(payment.getTaxesFlight().add(payment.getTaxesHotel()))
            .currency(payment.getCurrency())
            .email(request.getTravelers().getFirst().getContact().getEmailAddress())
            .phone(request.getTravelers().getFirst()
                    .getContact().getPhones().getFirst().getNumber())
            .build();

    Booking savedBooking = bookingRepository.save(booking);

    // 2. Crear flight booking
    FlightBooking flightBooking = FlightBooking.builder()
            .booking(savedBooking)
            .externalId(externalId)
            .origin(extractOrigin(flightOffer))
            .destination(extractDestination(flightOffer))
            .departureDate(extractDepartureDate(flightOffer))
            .returnDate(extractReturnDate(flightOffer))
            .carrier(extractCarrier(flightOffer))
            .adults(countAdults(request))
            .children(countChildren(request))
            .infants(countInfants(request))
            .totalPrice(payment.getGrandTotal())
            .taxes(payment.getTaxesFlight())
            .currency(payment.getCurrency()) //Solo faltaría la política de cancelación
            .build();

    flightBookingRepository.save(flightBooking);
    return savedBooking;
  }


  /**
   * Extrae el origen del primer segmento del primer itinerario de la oferta de vuelo.
   *
   * @param offer la oferta de vuelo
   * @return el código IATA del origen o "XXX" si no se encuentra
   */
  public String extractOrigin(FlightOfferDto offer) {
    // Extraer origen del primer segmento del primer itinerario
    if (offer != null && offer.getItineraries() != null && !offer.getItineraries().isEmpty()
            && offer.getItineraries().getFirst().getSegments() != null
            && !offer.getItineraries().getFirst().getSegments().isEmpty()) {
      return offer.getItineraries().getFirst()
              .getSegments().getFirst().getDeparture().getIataCode();
    }
    return "XXX"; // Código de fallback
  }

  public String extractDestination(FlightOfferDto offer) {
    // Extraer destino final del primer itinerario
    if (offer != null && offer.getItineraries() != null && !offer.getItineraries().isEmpty()
            && offer.getItineraries().getFirst().getSegments() != null
            && !offer.getItineraries().getFirst().getSegments().isEmpty()) {
      int lastIndex = offer.getItineraries().getFirst().getSegments().size() - 1;
      return offer.getItineraries().getFirst()
              .getSegments().get(lastIndex).getArrival().getIataCode();
    }
    return "XXX"; // Código de fallback
  }

  public String extractDepartureDate(FlightOfferDto offer) {
    // Extraer fecha de salida del primer segmento del primer itinerario
    if (offer != null && offer.getItineraries() != null && !offer.getItineraries().isEmpty()
            && offer.getItineraries().getFirst().getSegments() != null
            && !offer.getItineraries().getFirst().getSegments().isEmpty()) {
      if (offer.getItineraries().getFirst().getSegments()
              .getFirst().getDeparture().getAt() != null) {
        return offer.getItineraries().getFirst().getSegments().getFirst().getDeparture().getAt();
      }
    }
    return "2025-06-16T10:00:00"; // Fecha de fallback
  }

  public String extractReturnDate(FlightOfferDto offer) {
    // Extraer fecha de retorno (si existe)
    if (offer != null && offer.getItineraries() != null && offer.getItineraries().size() > 1
            && offer.getItineraries().get(1).getSegments() != null
            && !offer.getItineraries().get(1).getSegments().isEmpty()) {
      int lastIndex = offer.getItineraries().get(1).getSegments().size() - 1;
      if (offer.getItineraries().get(1).getSegments().get(lastIndex).getArrival().getAt() != null) {
        return offer.getItineraries().get(1).getSegments().get(lastIndex).getArrival().getAt();
      }
    }
    return null; // Puede ser null para vuelos solo de ida
  }

  public String extractCarrier(FlightOfferDto offer) {
    // Extraer aerolínea principal del primer segmento
    if (offer != null && offer.getItineraries() != null && !offer.getItineraries().isEmpty()
            && offer.getItineraries().getFirst().getSegments() != null
            && !offer.getItineraries().getFirst().getSegments().isEmpty()) {
      return offer.getItineraries().getFirst().getSegments().getFirst().getCarrierCode();
    }
    return "XX"; // Código de fallback
  }

  public Integer countAdults(CreateFlightBookingRequestDto request) {
    return (int) request.getTravelers().stream()
            .filter(t -> "ADULT".equals(t.getTravelerType()))
            .count();
  }

  public Integer countChildren(CreateFlightBookingRequestDto request) {
    return (int) request.getTravelers().stream()
            .filter(t -> "CHILD".equals(t.getTravelerType()))
            .count();
  }

  public Integer countInfants(CreateFlightBookingRequestDto request) {
    return (int) request.getTravelers().stream()
            .filter(t -> "INFANT".equals(t.getTravelerType()))
            .count();
  }

  public FlightBookingResponseDto convertToFlightBookingResponse(FlightBooking flightBooking) {
    return modelMapper.map(flightBooking, FlightBookingResponseDto.class);
  }

  public BookingResponseDto convertToBookingResponse(Booking booking) {
    return modelMapper.map(booking, BookingResponseDto.class);
  }
}