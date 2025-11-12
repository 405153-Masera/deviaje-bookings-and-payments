package masera.deviajebookingsandpayments.services.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import masera.deviajebookingsandpayments.entities.BookingEntity;
import masera.deviajebookingsandpayments.entities.PaymentEntity;
import masera.deviajebookingsandpayments.repositories.BookingRepository;
import masera.deviajebookingsandpayments.repositories.PaymentRepository;
import masera.deviajebookingsandpayments.services.interfaces.BookingService;
import org.springframework.stereotype.Service;

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
    Optional<PaymentEntity> paymentOpt = paymentRepository.findById(paymentId);
    if (paymentOpt.isPresent()) {
      PaymentEntity paymentEntity = paymentOpt.get();
      BookingEntity bookingEntity = bookingRepository.findById(bookingId).orElse(null);
      if (bookingEntity != null) {
        paymentEntity.setBookingEntity(bookingEntity);
        paymentRepository.save(paymentEntity);
      }
    }
  }

  @Override
  public String generateBookingReference(Long bookingId, BookingEntity.BookingType type) {
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
