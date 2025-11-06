package masera.deviajebookingsandpayments.services.impl;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import masera.deviajebookingsandpayments.dtos.bookings.CreatePackageBookingRequestDto;
import masera.deviajebookingsandpayments.dtos.payments.PaymentRequestDto;
import masera.deviajebookingsandpayments.dtos.payments.PricesDto;
import masera.deviajebookingsandpayments.dtos.responses.BookingResponseDto;
import masera.deviajebookingsandpayments.dtos.responses.PaymentResponseDto;
import masera.deviajebookingsandpayments.entities.Booking;
import masera.deviajebookingsandpayments.exceptions.MercadoPagoException;
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

  @Override
  @Transactional
  public String bookAndPay(CreatePackageBookingRequestDto bookingRequest,
                                 PaymentRequestDto paymentRequest, PricesDto prices) {

    log.info("Iniciando proceso de reserva y pago para paquete. Cliente: {}",
            bookingRequest.getClientId());

      // 1. CREAR RESERVA PRINCIPAL DEL PAQUETE
    log.info("Creando reserva principal de paquete");
    Booking packageBooking = createPackageBooking(bookingRequest, prices);

      // 2. CREAR RESERVA DE VUELO
    log.info("Creando reserva de vuelo para paquete");
    createFlightReservation(bookingRequest, packageBooking, prices);

    // 3. CREAR RESERVA DE HOTEL
    log.info("Creando reserva de hotel para paquete");
    createHotelReservation(bookingRequest, packageBooking, prices);

    // 4. PROCESAR PAGO
    log.info("Procesando pago para reserva de paquete");
    PaymentResponseDto paymentResult = paymentService.processPayment(paymentRequest);

    // 5. ASOCIAR PAGO CON LA RESERVA
    flightBookingService.updatePaymentWithBookingId(paymentResult.getId(), packageBooking.getId());

    log.info("Reserva de paquete completada exitosamente. ID: {}", packageBooking.getId());
    return packageBooking.getBookingReference();
  }

  @Override
  public List<BookingResponseDto> getClientPackageBookings(Integer clientId) {
    log.info("Obteniendo reservas de paquetes para el cliente: {}", clientId);

    List<Booking> bookings = bookingRepository.findByClientIdAndType(clientId, Booking.BookingType.PACKAGE);
    return bookings.stream()
            .map(flightBookingService::convertToBookingResponse)
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

  // ============================================================================
  // MÉTODOS PRIVADOS
  // ============================================================================

  /**
   * Crea la reserva principal del paquete.
   */
  private Booking createPackageBooking(CreatePackageBookingRequestDto request, PricesDto prices) {

    // Obtener email y teléfono del primer viajero del vuelo
    String email = request.getFlightBooking().getTravelers().getFirst()
            .getContact().getEmailAddress();
    String phone = request.getFlightBooking().getTravelers().getFirst()
            .getContact().getPhones().getFirst().getNumber();
    String countryCallingCode = request.getFlightBooking().getTravelers().getFirst()
            .getContact().getPhones().getFirst().getCountryCallingCode();

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
            .countryCallingCode(countryCallingCode)
            .build();

    Booking savedBooking = bookingRepository.save(booking);

    String bookingReference = flightBookingService.generateBookingReference(savedBooking.getId(), savedBooking.getType());
    savedBooking.setBookingReference(bookingReference);
    return bookingRepository.save(savedBooking);
  }

  /**
   * Crea la reserva de vuelo usando las APIs correspondientes.
   */
  private void createFlightReservation(CreatePackageBookingRequestDto request,
                                       Booking packageBooking,
                                       PricesDto prices) {

    // 1. Preparar datos para Amadeus
    Object amadeusBookingData = flightBookingService.prepareAmadeusBookingData(
              request.getFlightBooking());

    // 2. Crear reserva en Amadeus
    Object amadeusResponse = flightBookingService.callAmadeusCreateOrder(amadeusBookingData);

    // 3. Extraer external ID
    String externalId = flightBookingService.extractExternalId(amadeusResponse);

    // 4. Crear entidad FlightBooking asociada al paquete
    flightBookingService.createFlightBookingEntity(
              request.getFlightBooking(),
              request.getFlightBooking().getFlightOffer(),
              packageBooking,
              externalId,
              prices);

    log.info("Reserva de vuelo creada con external ID: {}", externalId);
  }

  /**
   * Crea la reserva de hotel usando las APIs correspondientes.
   */
  private void createHotelReservation(CreatePackageBookingRequestDto request,
                                      Booking packageBooking,
                                      PricesDto prices) {

    // 1. Preparar datos para HotelBeds
    Object hotelBedsBookingData = hotelBookingService.prepareHotelBedsBookingRequest(
              request.getHotelBooking());

    // 2. Crear reserva en HotelBeds (puede lanzar HotelBookingException)
    Object hotelBedsResponse = hotelBookingService.callHotelBedsCreateBooking((Map<String, Object>) hotelBedsBookingData);

    // 3. Extraer external ID y detalles
    String externalId = hotelBookingService.extractExternalId(hotelBedsResponse);

    // 4. Extraer hotelDetails usando el método público del servicio
    Map<String, Object> hotelDetails =  hotelBookingService.extractHotelDetails(hotelBedsResponse);

    // 5. Crear entidad HotelBooking asociada al paquete con hotelDetails
     hotelBookingService.createHotelBookingEntity(
                      request.getHotelBooking(),
                      packageBooking,
                      externalId,
                      prices,
                      hotelDetails);

    log.info("Reserva de hotel creada con external ID: {}", externalId);

  }
}