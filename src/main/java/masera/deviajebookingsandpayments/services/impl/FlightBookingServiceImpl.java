package masera.deviajebookingsandpayments.services.impl;

import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import masera.deviajebookingsandpayments.clients.FlightClient;
import masera.deviajebookingsandpayments.dtos.bookings.flights.CreateFlightBookingRequestDto;
import masera.deviajebookingsandpayments.dtos.bookings.flights.FlightOfferDto;
import masera.deviajebookingsandpayments.dtos.payments.PaymentRequestDto;
import masera.deviajebookingsandpayments.dtos.payments.PricesDto;
import masera.deviajebookingsandpayments.dtos.responses.BookingReferenceResponse;
import masera.deviajebookingsandpayments.dtos.responses.BookingResponseDto;
import masera.deviajebookingsandpayments.dtos.responses.FlightBookingDetailsDto;
import masera.deviajebookingsandpayments.dtos.responses.PaymentResponseDto;
import masera.deviajebookingsandpayments.entities.Booking;
import masera.deviajebookingsandpayments.entities.FlightBooking;
import masera.deviajebookingsandpayments.repositories.BookingRepository;
import masera.deviajebookingsandpayments.repositories.FlightBookingRepository;
import masera.deviajebookingsandpayments.services.interfaces.BookingService;
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

  private final BookingService bookingService;

  private final BookingRepository bookingRepository;

  private final FlightBookingRepository flightBookingRepository;

  private final ModelMapper modelMapper;

  @Override
  @Transactional
  public BookingReferenceResponse bookAndPay(CreateFlightBookingRequestDto bookingRequest,
                                             PaymentRequestDto paymentRequest, PricesDto prices) {

    log.info("Iniciando proceso de reserva y pago para vuelo. Cliente: {}",
            bookingRequest.getClientId());

    Object amadeusBookingData = prepareAmadeusBookingData(bookingRequest);
    Object amadeusResponse = flightClient.createFlightOrder(amadeusBookingData).block();

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

    this.bookingService.updatePaymentWithBookingId(paymentResult.getId(), savedBooking.getId());

    log.info("Reserva de vuelo completada exitosamente. ID: {}", savedBooking.getId());
    return new BookingReferenceResponse(savedBooking.getBookingReference());
  }

  @Override
  public FlightBookingDetailsDto getBasicBookingInfo(Long bookingId) {
    return flightBookingRepository.findById(bookingId)
            .map(this::convertToFlightBookingResponse)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Reserva de vuelo no encontrada con ID: " + bookingId
            ));
  }

  @Override
  public Object verifyFlightOfferPrice(Object flightOfferData) {
    log.info("Verificando disponibilidad y precio de oferta de vuelo");
    return flightClient.verifyFlightOfferPrice(flightOfferData).block();
  }

  // =============== MÉTODOS PÚBLICOS PARA REUTILIZACIÓN ===============

  /**
   * Prepara los datos de la reserva en el formato requerido por Amadeus.
   */
  @Override
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
   * Llama a Amadeus para crear la reserva de vuelo con manejo de errores específico
   */
  @Override
  public Object callAmadeusCreateOrder(Object amadeusBookingData) {
      return flightClient.createFlightOrder(amadeusBookingData).block();
  }

  /**
   * Extrae el ID externo de la respuesta de Amadeus.
   * Si no se encuentra, genera un ID temporal.
   *
   * @param amadeusResponse respuesta de Amadeus
   * @return ID externo o temporal
   */
  @Override
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

  @Override
  public void createFlightBookingEntity(CreateFlightBookingRequestDto request,
                                        FlightOfferDto flightOffer,
                                        Booking booking,
                                        String externalId,
                                        PricesDto prices) {

    String itinerariesJson = null;
    try {
      ObjectMapper mapper = new ObjectMapper();
      itinerariesJson = mapper.writeValueAsString(flightOffer.getItineraries());
    } catch (Exception e) {
      log.warn("Error al convertir itinerarios a JSON: {}", e.getMessage());
    }

    FlightBooking flightBooking = FlightBooking.builder()
            .booking(booking)
            .externalId(externalId)
            .origin(request.getOrigin())
            .destination(request.getDestination())
            .departureDate(extractDepartureDate(flightOffer))
            .returnDate(extractReturnDate(flightOffer))
            .carrier(extractCarrier(flightOffer))
            .adults(countAdults(request))
            .children(countChildren(request))
            .infants(countInfants(request))
            .itineraries(itinerariesJson)
            .totalPrice(prices.getGrandTotal())
            .taxes(prices.getTaxesFlight())
            .currency(prices.getCurrency())
            .cancellationFrom(request.getCancellationFrom())
            .cancellationAmount(request.getCancellationAmount())
            .build();

    flightBookingRepository.save(flightBooking);
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
  @Override
  @Transactional
  public Booking saveBookingInDatabase(CreateFlightBookingRequestDto request,
                                          FlightOfferDto flightOffer,
                                          PricesDto payment,
                                          String externalId) {

    String holderName = request.getTravelers().getFirst().getName().getFirstName() + ", " +
            request.getTravelers().getFirst().getName().getLastName();

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
            .holderName(holderName)
            .email(request.getTravelers().getFirst().getContact().getEmailAddress())
            .phone(request.getTravelers().getFirst()
                    .getContact().getPhones().getFirst().getNumber())
            .countryCallingCode(request.getTravelers().getFirst()
                    .getContact().getPhones().getFirst().getCountryCallingCode())
            .build();

    Booking savedBooking = bookingRepository.save(booking);

    String bookingReference = this.bookingService
            .generateBookingReference(savedBooking.getId(), savedBooking.getType());
    savedBooking.setBookingReference(bookingReference);
    savedBooking = bookingRepository.save(savedBooking);

    createFlightBookingEntity(request, flightOffer, savedBooking, externalId, payment);
    return savedBooking;
  }

  @Override
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

  @Override
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

  @Override
  public String extractCarrier(FlightOfferDto offer) {
    // Extraer aerolínea principal del primer segmento
    if (offer != null && offer.getItineraries() != null && !offer.getItineraries().isEmpty()
            && offer.getItineraries().getFirst().getSegments() != null
            && !offer.getItineraries().getFirst().getSegments().isEmpty()) {
      return offer.getItineraries().getFirst().getSegments().getFirst().getCarrierCode();
    }
    return "XX"; // Código de fallback
  }

  @Override
  public Integer countAdults(CreateFlightBookingRequestDto request) {
    return (int) request.getTravelers().stream()
            .filter(t -> "ADULT".equals(t.getTravelerType()))
            .count();
  }

  @Override
  public Integer countChildren(CreateFlightBookingRequestDto request) {
    return (int) request.getTravelers().stream()
            .filter(t -> "CHILD".equals(t.getTravelerType()))
            .count();
  }

  @Override
  public Integer countInfants(CreateFlightBookingRequestDto request) {
    return (int) request.getTravelers().stream()
            .filter(t -> "INFANT".equals(t.getTravelerType()))
            .count();
  }

  @Override
  public FlightBookingDetailsDto convertToFlightBookingResponse(FlightBooking flightBooking) {
    return modelMapper.map(flightBooking, FlightBookingDetailsDto.class);
  }

  @Override
  public BookingResponseDto convertToBookingResponse(Booking booking) {
    return modelMapper.map(booking, BookingResponseDto.class);
  }
}
