package masera.deviajebookingsandpayments.services.interfaces;

import masera.deviajebookingsandpayments.entities.Booking;
import org.springframework.stereotype.Service;

/**
 * Interface para el servicio común de todos los bookings.
 */
@Service
public interface BookingService {

  /**
   * Metodo que actualiza el pago con su respectiva reserva.
   *
   * @param paymentId id del pago
   * @param bookingId id de la reserva
   */
  void updatePaymentWithBookingId(Long paymentId, Long bookingId);

  /**
   * Metodo que genera una referencia más amigable.
   *
   * @param bookingId ID de la reserva
   * @param type tipo de reserva
   * @return la referencia amigable para el usuario
   */
  String generateBookingReference(Long bookingId, Booking.BookingType type);
}
