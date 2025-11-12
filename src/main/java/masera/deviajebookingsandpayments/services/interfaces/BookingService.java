package masera.deviajebookingsandpayments.services.interfaces;

import masera.deviajebookingsandpayments.dtos.bookings.hotels.CreateHotelBookingRequestDto;
import masera.deviajebookingsandpayments.dtos.payments.PricesDto;
import masera.deviajebookingsandpayments.entities.Booking;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public interface BookingService {


  void updatePaymentWithBookingId(Long paymentId, Long bookingId);

  String generateBookingReference(Long bookingId, Booking.BookingType type);

  /**
   * Guarda un intento de reserva fallido para auditoría (solo Booking principal).
   * Usa REQUIRES_NEW para que se guarde independientemente del rollback de la transacción principal.
   *
   * @param bookingType Tipo de reserva (HOTEL, FLIGHT, PACKAGE)
   * @param prices Información de precios
   * @param externalReference Referencia externa (HotelBeds, Amadeus, etc.)
   * @param failureReason Razón del fallo
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  void saveFailedBookingAttempt(CreateHotelBookingRequestDto bookingRequestDto,
                                Booking.BookingType bookingType,
                                PricesDto prices,
                                String externalReference,
                                String failureReason
  );

  /**
   * Maneja un error crítico al cancelar una reserva en API externa.
   * Registra el error y envía alertas para revisión manual.
   *
   * @param externalReference Referencia en la API externa
   * @param apiSource Fuente de la API (HOTELBEDS, AMADEUS, etc.)
   * @param exception Excepción que causó el error
   */
  void handleCriticalCancellationError(
          String externalReference,
          String apiSource,
          Exception exception
  );
}
