package masera.deviajebookingsandpayments.services.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import masera.deviajebookingsandpayments.dtos.bookings.hotels.CreateHotelBookingRequestDto;
import masera.deviajebookingsandpayments.dtos.payments.PricesDto;
import masera.deviajebookingsandpayments.entities.Booking;
import masera.deviajebookingsandpayments.entities.Payment;
import masera.deviajebookingsandpayments.repositories.BookingRepository;
import masera.deviajebookingsandpayments.repositories.PaymentRepository;
import masera.deviajebookingsandpayments.services.interfaces.BookingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio común para los demás servicios.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceIml implements BookingService {

  private final BookingRepository bookingRepository;

  private final PaymentRepository paymentRepository;

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
   * Guarda el intento de reserva fallido para auditoría.
   * Este metodo usa REQUIRES_NEW para que se ejecute en una transacción independiente.
   */
  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void saveFailedBookingAttempt( CreateHotelBookingRequestDto bookingRequestDto,
                                        Booking.BookingType bookingType,
                                        PricesDto prices,
                                        String externalReference,
                                        String failureReason) {

    try {
      Booking failedBooking = Booking.builder()
              .clientId(bookingRequestDto.getClientId())
              .agentId(bookingRequestDto.getAgentId())
              .status(Booking.BookingStatus.PAYMENT_FAILED)
              .type(bookingType)
              .holderName(bookingRequestDto.getHolder().getName() + ", " + bookingRequestDto.getHolder().getSurname())
              .totalAmount(prices.getTotalAmount())
              .commission(prices.getCommission())
              .discount(prices.getDiscount())
              .taxes(prices.getTaxesFlight().add(prices.getTaxesHotel()))
              .currency(prices.getCurrency())
              .email(bookingRequestDto.getHolder().getEmail())
              .phone(bookingRequestDto.getHolder().getPhone())
              .countryCallingCode(bookingRequestDto.getHolder().getCountryCallingCode())
              .externalReference(externalReference)
              .build();

      Booking savedBooking = bookingRepository.save(failedBooking);

      String bookingReference = generateBookingReference(
              savedBooking.getId(),
              savedBooking.getType()
      );
      savedBooking.setBookingReference(bookingReference);
      bookingRepository.save(savedBooking);

      log.info("Intento de reserva fallido guardado: {} - Tipo: {} - Razón: {}",
              savedBooking.getBookingReference(), bookingType, failureReason);

    } catch (Exception e) {
      log.error("No se pudo guardar el intento fallido (no crítico): {}", e.getMessage());
      // No relanzamos la excepción porque esto es solo auditoría
    }
  }

  /**
   * Maneja errores críticos al cancelar reservas en APIs externas.
   */
  @Override
  public void handleCriticalCancellationError(
          String externalReference,
          String apiSource,
          Exception exception) {

    log.error("═══════════════════════════════════════════════════════════");
    log.error("ERROR CRÍTICO: No se pudo cancelar reserva en {}", apiSource);
    log.error("Referencia externa: {}", externalReference);
    log.error("Mensaje: {}", exception.getMessage());
    log.error("═══════════════════════════════════════════════════════════");
  }
}
