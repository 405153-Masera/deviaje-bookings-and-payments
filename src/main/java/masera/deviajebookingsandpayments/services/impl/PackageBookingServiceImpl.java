package masera.deviajebookingsandpayments.services.impl;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import masera.deviajebookingsandpayments.clients.FlightClient;
import masera.deviajebookingsandpayments.clients.HotelClient;
import masera.deviajebookingsandpayments.dtos.bookings.CreatePackageBookingRequestDto;
import masera.deviajebookingsandpayments.dtos.payments.PaymentRequestDto;
import masera.deviajebookingsandpayments.dtos.payments.PricesDto;
import masera.deviajebookingsandpayments.dtos.responses.BookAndPayResponseDto;
import masera.deviajebookingsandpayments.dtos.responses.BookingResponseDto;
import masera.deviajebookingsandpayments.dtos.responses.PaymentResponseDto;
import masera.deviajebookingsandpayments.entities.Booking;
import masera.deviajebookingsandpayments.entities.FlightBooking;
import masera.deviajebookingsandpayments.entities.HotelBooking;
import masera.deviajebookingsandpayments.repositories.BookingRepository;
import masera.deviajebookingsandpayments.services.interfaces.FlightBookingService;
import masera.deviajebookingsandpayments.services.interfaces.HotelBookingService;
import masera.deviajebookingsandpayments.services.interfaces.PackageBookingService;
import masera.deviajebookingsandpayments.services.interfaces.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Implementación del servicio de reservas de paquetes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PackageBookingServiceImpl implements PackageBookingService {

  private final BookingRepository bookingRepository;
  private final PaymentService paymentService;
  private final FlightBookingService flightBookingService;
  private final HotelBookingService hotelBookingService;
  private final FlightClient flightClient;
  private final HotelClient hotelClient;

  @Override
  @Transactional
  public BookAndPayResponseDto bookAndPay(CreatePackageBookingRequestDto bookingRequest,
                                          PaymentRequestDto paymentRequest, PricesDto prices) {

    log.info("Iniciando proceso de reserva y pago para paquete. Cliente: {}",
            bookingRequest.getClientId());

    try {

      // 1. CREAR RESERVA PRINCIPAL DEL PAQUETE
      log.info("Creando reserva principal de paquete");
      Booking packageBooking = createPackageBooking(bookingRequest, prices);

      // 2. CREAR RESERVA DE VUELO
      log.info("Creando reserva de vuelo para paquete");
      String flightExternalId = createFlightReservation(
              bookingRequest, packageBooking, prices);

      if (flightExternalId == null) {
        log.error("Error al crear reserva de vuelo");
        return BookAndPayResponseDto.bookingFailed("Error al crear la reserva de vuelo");
      }

      // 3. CREAR RESERVA DE HOTEL
      log.info("Creando reserva de hotel para paquete");
      String hotelExternalId = createHotelReservation(
              bookingRequest, packageBooking, prices);

      if (hotelExternalId == null) {
        log.error("Error al crear reserva de hotel");
        return BookAndPayResponseDto.bookingFailed("Error al crear la reserva de hotel");
      }

      // 4. PROCESAR PAGO
      log.info("Procesando pago para reserva de paquete");
      PaymentResponseDto paymentResult = paymentService.processPayment(paymentRequest);

      if (!"APPROVED".equals(paymentResult.getStatus())) {
        log.warn("Pago rechazado: {}", paymentResult.getErrorMessage());
        return BookAndPayResponseDto.paymentFailed(
                "Pago rechazado. " + paymentResult.getErrorMessage());
      }

      // 5. ASOCIAR PAGO CON LA RESERVA
      flightBookingService.updatePaymentWithBookingId(paymentResult.getId(), packageBooking.getId());

      // 6. CONVERTIR A DTO DE RESPUESTA
      BookingResponseDto bookingResponse = flightBookingService.convertToBookingResponse(packageBooking);

      log.info("Reserva de paquete completada exitosamente. ID: {}", packageBooking.getId());
      return BookAndPayResponseDto.success(bookingResponse);

    } catch (Exception e) {
      log.error("Error inesperado en reserva de paquete", e);
      return BookAndPayResponseDto.bookingFailed("Error interno: " + e.getMessage());
    }
  }

  @Override
  public List<BookingResponseDto> getClientPackageBookings(Integer clientId) {
    log.info("Obteniendo reservas de paquetes para el cliente: {}", clientId);

    List<Booking> bookings = bookingRepository.findByClientIdAndType(clientId, Booking.BookingType.PACKAGE);
    return bookings.stream()
            .map(booking -> flightBookingService.convertToBookingResponse(booking))
            .collect(Collectors.toList());
  }

  @Override
  public BookingResponseDto getPackageBookingDetails(Long bookingId) {
    log.info("Obteniendo detalles de reserva de paquete: {}", bookingId);

    Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);
    if (bookingOpt.isEmpty() || !Booking.BookingType.PACKAGE.equals(bookingOpt.get().getType())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva de paquete no encontrada");
    }

    return flightBookingService.convertToBookingResponse(bookingOpt.get());
  }

  @Override
  @Transactional
  public BookAndPayResponseDto cancelBooking(Long bookingId) {
    log.info("Cancelando reserva de paquete: {}", bookingId);

    Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);
    if (bookingOpt.isEmpty() || !Booking.BookingType.PACKAGE.equals(bookingOpt.get().getType())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva de paquete no encontrada");
    }

    // TODO: Implementar lógica de cancelación
    return BookAndPayResponseDto.bookingFailed("Cancelación de paquetes no implementada aún");
  }

  // ============================================================================
  // MÉTODOS PRIVADOS
  // ============================================================================

  /**
   * Crea la reserva principal del paquete.
   */
  private Booking createPackageBooking(CreatePackageBookingRequestDto request, PricesDto prices) {

    // Obtener email y teléfono del primer viajero del vuelo
    String email = request.getFlightBooking().getTravelers().get(0)
            .getContact().getEmailAddress();
    String phone = request.getFlightBooking().getTravelers().get(0)
            .getContact().getPhones().get(0).getNumber();

    Booking booking = Booking.builder()
            .clientId(request.getClientId())
            .agentId(request.getAgentId())
            .status(Booking.BookingStatus.CONFIRMED)
            .type(Booking.BookingType.PACKAGE)
            .totalAmount(prices.getTotalAmount())
            .commission(prices.getCommission())
            .discount(prices.getDiscount())
            .taxes(prices.getTaxesFlight().add(prices.getTaxesHotel()))
            .currency(prices.getCurrency())
            .email(email)
            .phone(phone)
            .build();

    return bookingRepository.save(booking);
  }

  /**
   * Crea la reserva de vuelo usando las APIs correspondientes.
   */
  private String createFlightReservation(CreatePackageBookingRequestDto request,
                                         Booking packageBooking,
                                         PricesDto prices) {
    try {
      // 1. Preparar datos para Amadeus
      Object amadeusBookingData = flightBookingService.prepareAmadeusBookingData(
              request.getFlightBooking());

      // 2. Crear reserva en Amadeus
      Object amadeusResponse = flightClient.createFlightOrder(amadeusBookingData).block();

      if (amadeusResponse == null) {
        log.error("Error al crear reserva de vuelo en Amadeus");
        return null;
      }

      // 3. Extraer external ID
      String externalId = flightBookingService.extractExternalId(amadeusResponse);

      // 4. Crear entidad FlightBooking asociada al paquete
      FlightBooking flightBooking = flightBookingService.createFlightBookingEntity(
              request.getFlightBooking(),
              request.getFlightBooking().getFlightOffer(),
              packageBooking,
              externalId,
              prices);

      log.info("Reserva de vuelo creada con external ID: {}", externalId);
      return externalId;

    } catch (Exception e) {
      log.error("Error al crear reserva de vuelo para paquete", e);
      return null;
    }
  }

  /**
   * Crea la reserva de hotel usando las APIs correspondientes.
   */
  private String createHotelReservation(CreatePackageBookingRequestDto request,
                                        Booking packageBooking,
                                        PricesDto prices) {
    try {
      // 1. Preparar datos para HotelBeds
      Object hotelBedsBookingData = hotelBookingService.prepareHotelBedsBookingRequest(
              request.getHotelBooking());

      // 2. Crear reserva en HotelBeds
      Object hotelBedsResponse = hotelClient.createBooking(hotelBedsBookingData).block();

      if (hotelBedsResponse == null) {
        log.error("Error al crear reserva de hotel en HotelBeds");
        return null;
      }

      // 3. Extraer external ID y detalles
      String externalId = hotelBookingService.extractExternalId(hotelBedsResponse);

      // 4. Extraer hotelDetails usando el método público del servicio
      Map<String, Object> hotelDetails = ((HotelBookingServiceImpl) hotelBookingService)
              .extractHotelDetails(hotelBedsResponse);

      // 5. Crear entidad HotelBooking asociada al paquete con hotelDetails
      HotelBooking hotelBooking = ((HotelBookingServiceImpl) hotelBookingService)
              .createHotelBookingEntity(
                      request.getHotelBooking(),
                      packageBooking,
                      externalId,
                      prices,
                      hotelDetails);

      log.info("Reserva de hotel creada con external ID: {}", externalId);
      return externalId;

    } catch (Exception e) {
      log.error("Error al crear reserva de hotel para paquete", e);
      return null;
    }
  }
}