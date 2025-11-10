package masera.deviajebookingsandpayments.services.impl;

import lombok.RequiredArgsConstructor;
import masera.deviajebookingsandpayments.entities.Booking;
import masera.deviajebookingsandpayments.entities.Payment;
import masera.deviajebookingsandpayments.repositories.BookingRepository;
import masera.deviajebookingsandpayments.repositories.PaymentRepository;
import masera.deviajebookingsandpayments.services.interfaces.BookingService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@RequiredArgsConstructor
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
}
