package masera.deviajebookingsandpayments.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import masera.deviajebookingsandpayments.dtos.bookings.CreatePackageBookingRequestDto;
import masera.deviajebookingsandpayments.dtos.bookings.flights.FlightOfferDto;
import masera.deviajebookingsandpayments.dtos.payments.PaymentRequestDto;
import masera.deviajebookingsandpayments.dtos.responses.BookAndPayResponseDto;
import masera.deviajebookingsandpayments.dtos.responses.BookingResponseDto;
import masera.deviajebookingsandpayments.dtos.responses.PaymentResponseDto;
import masera.deviajebookingsandpayments.entities.Booking;
import masera.deviajebookingsandpayments.entities.FlightBooking;
import masera.deviajebookingsandpayments.entities.HotelBooking;
import masera.deviajebookingsandpayments.entities.Payment;
import masera.deviajebookingsandpayments.repositories.BookingRepository;
import masera.deviajebookingsandpayments.repositories.FlightBookingRepository;
import masera.deviajebookingsandpayments.repositories.HotelBookingRepository;
import masera.deviajebookingsandpayments.repositories.PaymentRepository;
import masera.deviajebookingsandpayments.services.interfaces.FlightBookingService;
import masera.deviajebookingsandpayments.services.interfaces.HotelBookingService;
import masera.deviajebookingsandpayments.services.interfaces.PackageBookingService;
import masera.deviajebookingsandpayments.services.interfaces.PaymentService;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de reservas de paquetes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PackageBookingServiceImpl implements PackageBookingService {

  private final BookingRepository bookingRepository;
  private final FlightBookingRepository flightBookingRepository;
  private final HotelBookingRepository hotelBookingRepository;
  private final PaymentRepository paymentRepository;
  private final PaymentService paymentService;
  private final FlightBookingService flightBookingService;
  private final HotelBookingService hotelBookingService;
  private final ModelMapper modelMapper;

  @Override
  @Transactional
  public BookAndPayResponseDto bookAndPay(CreatePackageBookingRequestDto bookingRequest, PaymentRequestDto paymentRequest) {
    log.info("Iniciando proceso de reserva y pago para paquete. Cliente: {}", bookingRequest.getClientId());

    try {
      // 1. Verificar disponibilidad y precio actual
      // Verificar vuelo
      Object flightOffer = bookingRequest.getFlightBooking().getFlightOffer();
      Object verifiedFlightOffer = flightBookingService.verifyFlightOfferPrice(flightOffer);

      if (verifiedFlightOffer == null) {
        return BookAndPayResponseDto.verificationFailed("La oferta de vuelo seleccionada ya no está disponible");
      }

      // Verificar hotel
      String rateKey = bookingRequest.getHotelBooking().getRooms().get(0).getRateKey();
      Object verifiedHotelOffer = hotelBookingService.checkRates(rateKey);

      if (verifiedHotelOffer == null) {
        return BookAndPayResponseDto.verificationFailed("La tarifa del hotel seleccionada ya no está disponible");
      }

      // 2. Procesar pago PRIMERO
      log.info("Procesando pago para reserva de paquete");
      PaymentResponseDto paymentResult = paymentService.processPayment(paymentRequest);

      if (!"APPROVED".equals(paymentResult.getStatus())) {
        log.warn("Pago rechazado: {}", paymentResult.getErrorMessage());
        return BookAndPayResponseDto.paymentFailed(paymentResult.getErrorMessage());
      }

      // 3. Si pago exitoso, crear reservas
      log.info("Pago aprobado, creando reservas del paquete");

      // 3.1 Crear la reserva principal (paquete)
      Booking packageBooking = Booking.builder()
              .clientId(bookingRequest.getClientId())
              .agentId(bookingRequest.getAgentId())
              .branchId(bookingRequest.getBranchId())
              .status(Booking.BookingStatus.CONFIRMED)
              .type(Booking.BookingType.PACKAGE)
              .totalAmount(paymentResult.getAmount())
              .currency(paymentResult.getCurrency())
              .discount(BigDecimal.ZERO)
              .taxes(calculateTotalTaxes(verifiedFlightOffer, verifiedHotelOffer, paymentResult.getAmount()))
              .build();

      Booking savedPackageBooking = bookingRepository.save(packageBooking);

      // 3.2 Crear reserva de vuelo
      String flightExternalId = createFlightBooking(bookingRequest, verifiedFlightOffer, savedPackageBooking);
      if (flightExternalId == null) {
        // Si falla la reserva de vuelo, reembolsar y cancelar
        paymentService.refundPayment(paymentResult.getId());
        bookingRepository.delete(savedPackageBooking);
        return BookAndPayResponseDto.bookingFailed("No se pudo confirmar la reserva de vuelo");
      }

      // 3.3 Crear reserva de hotel
      String hotelExternalId = createHotelBooking(bookingRequest, verifiedHotelOffer, savedPackageBooking);
      if (hotelExternalId == null) {
        // Si falla la reserva de hotel, reembolsar y cancelar
        paymentService.refundPayment(paymentResult.getId());
        bookingRepository.delete(savedPackageBooking);
        return BookAndPayResponseDto.bookingFailed("No se pudo confirmar la reserva de hotel");
      }

      // 3.4 Guardar el pago
      Payment paymentEntity = Payment.builder()
              .booking(savedPackageBooking)
              .amount(paymentResult.getAmount())
              .currency(paymentResult.getCurrency())
              .method(paymentResult.getMethod())
              .paymentProvider(paymentResult.getPaymentProvider())
              .externalPaymentId(paymentResult.getExternalPaymentId())
              .status(Payment.PaymentStatus.valueOf(paymentResult.getStatus()))
              .date(LocalDateTime.now())
              .build();

      paymentRepository.save(paymentEntity);

      log.info("Reserva de paquete completada exitosamente. ID: {}", savedPackageBooking.getId());
      return BookAndPayResponseDto.success(convertToBookingResponse(savedPackageBooking));

    } catch (Exception e) {
      log.error("Error inesperado en reserva de paquete", e);
      return BookAndPayResponseDto.bookingFailed("Error interno: " + e.getMessage());
    }
  }

  @Override
  public List<BookingResponseDto> getClientPackageBookings(Long clientId) {
    log.info("Obteniendo reservas de paquetes para el cliente: {}", clientId);

    List<Booking> bookings = bookingRepository.findByClientIdAndType(clientId, Booking.BookingType.PACKAGE);
    return bookings.stream()
            .map(this::convertToBookingResponse)
            .collect(Collectors.toList());
  }

  @Override
  public BookingResponseDto getPackageBookingDetails(Long bookingId) {
    log.info("Obteniendo detalles de reserva de paquete: {}", bookingId);

    Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);
    if (bookingOpt.isEmpty() || !Booking.BookingType.PACKAGE.equals(bookingOpt.get().getType())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva de paquete no encontrada");
    }

    return convertToBookingResponse(bookingOpt.get());
  }

  @Override
  @Transactional
  public BookAndPayResponseDto cancelBooking(Long bookingId) {
    log.info("Cancelando reserva de paquete: {}", bookingId);

    Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);
    if (bookingOpt.isEmpty() || !Booking.BookingType.PACKAGE.equals(bookingOpt.get().getType())) {
      return BookAndPayResponseDto.bookingFailed("Reserva de paquete no encontrada");
    }

    Booking booking = bookingOpt.get();

    // Verificar si ya está cancelada
    if (Booking.BookingStatus.CANCELLED.equals(booking.getStatus())) {
      return BookAndPayResponseDto.bookingFailed("La reserva ya está cancelada");
    }

    try {
      // Cancelar reserva de vuelo
      List<FlightBooking> flightBookings = flightBookingRepository.findByBookingId(bookingId);
      for (FlightBooking flightBooking : flightBookings) {
        // Intentar cancelar en Amadeus si tiene externalId
        if (flightBooking.getExternalId() != null) {
          try {
            flightBookingService.cancelBooking(flightBooking.getId());
          } catch (Exception e) {
            log.warn("Error al cancelar reserva de vuelo en Amadeus: {}", e.getMessage());
            // Continuar con la cancelación local
          }
        }

        // Actualizar estado localmente
        flightBooking.setStatus(FlightBooking.FlightBookingStatus.CANCELLED);
        flightBookingRepository.save(flightBooking);
      }

      // Cancelar reserva de hotel
      List<HotelBooking> hotelBookings = hotelBookingRepository.findByBookingId(bookingId);
      for (HotelBooking hotelBooking : hotelBookings) {
        // Intentar cancelar en HotelBeds si tiene externalId
        if (hotelBooking.getExternalId() != null) {
          try {
            hotelBookingService.cancelBooking(hotelBooking.getId());
          } catch (Exception e) {
            log.warn("Error al cancelar reserva de hotel en HotelBeds: {}", e.getMessage());
            // Continuar con la cancelación local
          }
        }

        // Actualizar estado localmente
        hotelBooking.setStatus(HotelBooking.HotelBookingStatus.CANCELLED);
        hotelBookingRepository.save(hotelBooking);
      }

      // Actualizar estado de la reserva principal
      booking.setStatus(Booking.BookingStatus.CANCELLED);
      bookingRepository.save(booking);

      // Procesar reembolso
      paymentService.processRefundForBooking(bookingId);

      log.info("Reserva de paquete cancelada exitosamente: {}", bookingId);

      return BookAndPayResponseDto.builder()
              .success(true)
              .message("Reserva de paquete cancelada exitosamente")
              .booking(convertToBookingResponse(booking))
              .build();

    } catch (Exception e) {
      log.error("Error al cancelar reserva de paquete: {}", bookingId, e);
      return BookAndPayResponseDto.bookingFailed("Error al cancelar: " + e.getMessage());
    }
  }

  @Override
  public Map<String, Object> verifyPackagePrice(Map<String, Object> packageDetails) {
    log.info("Verificando precio y disponibilidad de paquete");

    try {
      Map<String, Object> result = new HashMap<>();

      // Verificar vuelo
      if (packageDetails.containsKey("flightOffer")) {
        Object flightOffer = packageDetails.get("flightOffer");
        Object verifiedFlightOffer = flightBookingService.verifyFlightOfferPrice(flightOffer);
        result.put("flightOffer", verifiedFlightOffer);
      }

      // Verificar hotel
      if (packageDetails.containsKey("hotelRateKey")) {
        String rateKey = (String) packageDetails.get("hotelRateKey");
        Object verifiedHotelOffer = hotelBookingService.checkRates(rateKey);
        result.put("hotelOffer", verifiedHotelOffer);
      }

      // Calcular total del paquete
      BigDecimal totalPackagePrice = calculateTotalPrice(result);
      result.put("totalPrice", totalPackagePrice);

      return result;
    } catch (Exception e) {
      log.error("Error al verificar precio de paquete", e);
      throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST,
              "Error al verificar precio: " + e.getMessage()
      );
    }
  }

  // ============================================================================
  // MÉTODOS PRIVADOS DE UTILIDAD
  // ============================================================================

  /**
   * Calcula los impuestos totales del paquete
   */
  private BigDecimal calculateTotalTaxes(Object flightOffer, Object hotelOffer, BigDecimal totalAmount) {
    // Por simplicidad, usamos un 15% del total como impuestos
    return totalAmount.multiply(new BigDecimal("0.15"));
  }

  /**
   * Crea la reserva de vuelo como parte del paquete
   */
  private String createFlightBooking(CreatePackageBookingRequestDto request, Object verifiedFlightOffer, Booking packageBooking) {
    try {
      // En un caso real, aquí llamaríamos a Amadeus para crear la reserva
      // Por ahora, simulamos la creación de la reserva

      FlightOfferDto flightOfferDto = new FlightOfferDto(); // Suponemos que se ha convertido del objeto verificado

      FlightBooking flightBooking = FlightBooking.builder()
              .booking(packageBooking)
              .externalId("FLIGHT_" + System.currentTimeMillis()) // En un caso real, sería el ID de Amadeus
              .origin(extractOrigin(flightOfferDto))
              .destination(extractDestination(flightOfferDto))
              .departureDate(extractDepartureDate(flightOfferDto))
              .returnDate(extractReturnDate(flightOfferDto))
              .carrier(extractCarrier(flightOfferDto))
              .basePrice(packageBooking.getTotalAmount().multiply(new BigDecimal("0.6"))) // 60% del total para el vuelo (ejemplo)
              .taxes(packageBooking.getTotalAmount().multiply(new BigDecimal("0.09"))) // 15% de impuestos sobre el 60%
              .discounts(BigDecimal.ZERO)
              .totalPrice(packageBooking.getTotalAmount().multiply(new BigDecimal("0.6")))
              .currency(packageBooking.getCurrency())
              .status(FlightBooking.FlightBookingStatus.CONFIRMED)
              .build();

      flightBookingRepository.save(flightBooking);

      return flightBooking.getExternalId();
    } catch (Exception e) {
      log.error("Error al crear reserva de vuelo para paquete", e);
      return null;
    }
  }

  /**
   * Crea la reserva de hotel como parte del paquete
   */
  private String createHotelBooking(CreatePackageBookingRequestDto request, Object verifiedHotelOffer, Booking packageBooking) {
    try {
      // En un caso real, aquí llamaríamos a HotelBeds para crear la reserva
      // Por ahora, simulamos la creación de la reserva

      HotelBooking hotelBooking = HotelBooking.builder()
              .booking(packageBooking)
              .externalId("HOTEL_" + System.currentTimeMillis()) // En un caso real, sería el ID de HotelBeds
              .hotelCode("HTL123")
              .hotelName("Hotel Example")
              .destinationCode("MAD")
              .destinationName("Madrid")
              .checkInDate(java.time.LocalDate.now().plusDays(30)) // Ejemplo
              .checkOutDate(java.time.LocalDate.now().plusDays(35)) // Ejemplo
              .numberOfNights(5)
              .numberOfRooms(1)
              .adults(2)
              .children(0)
              .basePrice(packageBooking.getTotalAmount().multiply(new BigDecimal("0.4"))) // 40% del total para el hotel (ejemplo)
              .taxes(packageBooking.getTotalAmount().multiply(new BigDecimal("0.06"))) // 15% de impuestos sobre el 40%
              .discounts(BigDecimal.ZERO)
              .totalPrice(packageBooking.getTotalAmount().multiply(new BigDecimal("0.4")))
              .currency(packageBooking.getCurrency())
              .status(HotelBooking.HotelBookingStatus.CONFIRMED)
              .build();

      hotelBookingRepository.save(hotelBooking);

      return hotelBooking.getExternalId();
    } catch (Exception e) {
      log.error("Error al crear reserva de hotel para paquete", e);
      return null;
    }
  }

  /**
   * Calcula el precio total del paquete basado en las ofertas verificadas
   */
  private BigDecimal calculateTotalPrice(Map<String, Object> verifiedOffers) {
    // En un caso real, extraeríamos los precios de las ofertas verificadas
    // Por simplicidad, devolvemos un valor fijo
    return new BigDecimal("1500.00");
  }

  /**
   * Convierte un objeto Booking a BookingResponseDto
   */
  private BookingResponseDto convertToBookingResponse(Booking booking) {
    return modelMapper.map(booking, BookingResponseDto.class);
  }

  /**
   * Métodos auxiliares para extraer información de la oferta de vuelo
   */
  private String extractOrigin(FlightOfferDto offer) {
    // Extraer origen del primer segmento del primer itinerario
    if (offer != null && offer.getItineraries() != null && !offer.getItineraries().isEmpty()
            && offer.getItineraries().get(0).getSegments() != null
            && !offer.getItineraries().get(0).getSegments().isEmpty()) {
      return offer.getItineraries().get(0).getSegments().get(0).getDeparture().getIataCode();
    }
    return "MAD"; // Código de fallback
  }

  private String extractDestination(FlightOfferDto offer) {
    // Extraer destino final del primer itinerario
    if (offer != null && offer.getItineraries() != null && !offer.getItineraries().isEmpty()
            && offer.getItineraries().get(0).getSegments() != null
            && !offer.getItineraries().get(0).getSegments().isEmpty()) {
      int lastIndex = offer.getItineraries().get(0).getSegments().size() - 1;
      return offer.getItineraries().get(0).getSegments().get(lastIndex).getArrival().getIataCode();
    }
    return "BCN"; // Código de fallback
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
    return LocalDateTime.now().plusDays(30); // Fecha de fallback
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
    return LocalDateTime.now().plusDays(35); // Fecha de fallback para vuelos de ida y vuelta
  }

  private String extractCarrier(FlightOfferDto offer) {
    // Extraer aerolínea principal del primer segmento
    if (offer != null && offer.getItineraries() != null && !offer.getItineraries().isEmpty()
            && offer.getItineraries().get(0).getSegments() != null
            && !offer.getItineraries().get(0).getSegments().isEmpty()) {
      return offer.getItineraries().get(0).getSegments().get(0).getCarrierCode();
    }
    return "IB"; // Código de fallback
  }
}