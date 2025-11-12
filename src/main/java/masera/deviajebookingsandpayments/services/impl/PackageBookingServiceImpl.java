package masera.deviajebookingsandpayments.services.impl;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import masera.deviajebookingsandpayments.dtos.bookings.CreatePackageBookingRequestDto;
import masera.deviajebookingsandpayments.dtos.bookings.hotels.HotelBookingResponse;
import masera.deviajebookingsandpayments.dtos.payments.PaymentRequestDto;
import masera.deviajebookingsandpayments.dtos.payments.PricesDto;
import masera.deviajebookingsandpayments.dtos.responses.BookingReferenceResponse;
import masera.deviajebookingsandpayments.dtos.responses.BookingResponseDto;
import masera.deviajebookingsandpayments.dtos.responses.PaymentResponseDto;
import masera.deviajebookingsandpayments.entities.BookingEntity;
import masera.deviajebookingsandpayments.entities.PaymentEntity;
import masera.deviajebookingsandpayments.repositories.BookingRepository;
import masera.deviajebookingsandpayments.services.interfaces.FlightBookingService;
import masera.deviajebookingsandpayments.services.interfaces.HotelBookingService;
import masera.deviajebookingsandpayments.services.interfaces.PackageBookingService;
import masera.deviajebookingsandpayments.services.interfaces.PaymentService;
import masera.deviajebookingsandpayments.services.interfaces.BookingService;
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

  private final BookingService bookingService;

  private final FlightBookingService flightBookingService;

  private final HotelBookingService hotelBookingService;

  @Override
  @Transactional
  public BookingReferenceResponse bookAndPay(CreatePackageBookingRequestDto bookingRequest,
                                             PaymentRequestDto paymentRequest, PricesDto prices) {

    log.info("Iniciando proceso de reserva y pago para paquete. Cliente: {}",
            bookingRequest.getClientId());

    log.info("Creando reserva principal de paquete");
    BookingEntity packageBookingEntity = createPackageBooking(bookingRequest, prices);

    log.info("Creando reserva de vuelo para paquete");
    createFlightReservation(bookingRequest, packageBookingEntity, prices);

    // 3. CREAR RESERVA DE HOTEL
    log.info("Creando reserva de hotel para paquete");
    createHotelReservation(bookingRequest, packageBookingEntity, prices);

    // 4. PROCESAR PAGO
    log.info("Procesando pago para reserva de paquete");
    PaymentResponseDto paymentResult = paymentService.processPayment(
            paymentRequest, PaymentEntity.Type.PACKAGE);

    // 5. ASOCIAR PAGO CON LA RESERVA
    bookingService.updatePaymentWithBookingId(paymentResult.getId(), packageBookingEntity.getId());

    log.info("Reserva de paquete completada exitosamente. ID: {}", packageBookingEntity.getId());
    return new BookingReferenceResponse(packageBookingEntity.getBookingReference());
  }

  @Override
  public List<BookingResponseDto> getClientPackageBookings(Integer clientId) {
    log.info("Obteniendo reservas de paquetes para el cliente: {}", clientId);

    List<BookingEntity> bookingEntities = bookingRepository.findByClientIdAndType(clientId, BookingEntity.BookingType.PACKAGE);
    return bookingEntities.stream()
            .map(flightBookingService::convertToBookingResponse)
            .collect(Collectors.toList());
  }

  @Override
  public BookingResponseDto getPackageBookingDetails(Long bookingId) {
    log.info("Obteniendo detalles de reserva de paquete: {}", bookingId);

    Optional<BookingEntity> bookingOpt = bookingRepository.findById(bookingId);
    if (bookingOpt.isEmpty() || !BookingEntity.BookingType.PACKAGE.equals(bookingOpt.get().getType())) {
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
  private BookingEntity createPackageBooking(CreatePackageBookingRequestDto request, PricesDto prices) {

    // Obtener email y teléfono del primer viajero del vuelo
    String email = request.getFlightBooking().getTravelers().getFirst()
            .getContact().getEmailAddress();
    String phone = request.getFlightBooking().getTravelers().getFirst()
            .getContact().getPhones().getFirst().getNumber();
    String countryCallingCode = request.getFlightBooking().getTravelers().getFirst()
            .getContact().getPhones().getFirst().getCountryCallingCode();

    BookingEntity bookingEntity = BookingEntity.builder()
            .clientId(request.getClientId())
            .agentId(request.getAgentId())
            .status(BookingEntity.BookingStatus.CONFIRMED)
            .type(BookingEntity.BookingType.PACKAGE)
            .totalAmount(prices.getTotalAmount())
            .commission(prices.getCommission())
            .discount(prices.getDiscount())
            .taxes(prices.getTaxesFlight().add(prices.getTaxesHotel()))
            .currency(prices.getCurrency())
            .email(email)
            .phone(phone)
            .countryCallingCode(countryCallingCode)
            .build();

    BookingEntity savedBookingEntity = bookingRepository.save(bookingEntity);

    String bookingReference = bookingService.generateBookingReference(savedBookingEntity.getId(), savedBookingEntity.getType());
    savedBookingEntity.setBookingReference(bookingReference);
    return bookingRepository.save(savedBookingEntity);
  }

  /**
   * Crea la reserva de vuelo usando las apis correspondientes.
   */
  private void createFlightReservation(CreatePackageBookingRequestDto request,
                                       BookingEntity packageBookingEntity,
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
            packageBookingEntity,
              externalId,
              prices);

    log.info("Reserva de vuelo creada con external ID: {}", externalId);
  }

  /**
   * Crea la reserva de hotel usando las apis correspondientes.
   */
  private void createHotelReservation(CreatePackageBookingRequestDto request,
                                      BookingEntity packageBookingEntity,
                                      PricesDto prices) {

    Map<String, Object> hotelBedsBookingData = hotelBookingService.prepareHotelBedsBookingRequest(
              request.getHotelBooking());
    HotelBookingResponse hotelBedsResponse = hotelBookingService
            .callHotelBedsCreateBooking(hotelBedsBookingData);

    hotelBookingService.createHotelBookingEntity(
            request.getHotelBooking(),
            packageBookingEntity,
            hotelBedsResponse.getBooking().getReference(),
            prices,
            hotelBedsResponse.getBooking());

    log.info("Reserva de hotel creada con external ID: {}",
            hotelBedsResponse.getBooking().getReference());
  }
}