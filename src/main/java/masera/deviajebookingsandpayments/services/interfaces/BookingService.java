package masera.deviajebookingsandpayments.services.interfaces;

import masera.deviajebookingsandpayments.entities.Booking;
import org.springframework.stereotype.Service;

@Service
public interface BookingService {


  void updatePaymentWithBookingId(Long paymentId, Long bookingId);

  String generateBookingReference(Long bookingId, Booking.BookingType type);
}
