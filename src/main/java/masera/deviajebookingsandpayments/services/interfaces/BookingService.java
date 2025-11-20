package masera.deviajebookingsandpayments.services.interfaces;

import masera.deviajebookingsandpayments.dtos.responses.BookingDetailsResponseDto;
import masera.deviajebookingsandpayments.dtos.responses.BookingResponseDto;
import masera.deviajebookingsandpayments.entities.BookingEntity;
import org.springframework.stereotype.Service;

import java.util.List;


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

  List<BookingResponseDto> getClientBookings(Integer clientId,
                                             String email,
                                             String holderName);

  List<BookingResponseDto> getAgentBookings(Integer agentId,
                                            Integer clientId,
                                            String email,
                                            String holderName);

  List<BookingResponseDto> getAllBookings(Integer agentId,
                                          Integer clientId,
                                          String email,
                                          String holderName);

  BookingResponseDto getBookingById(Long bookingId);


  byte[] downloadVoucher(Long bookingId);

  /**
   * Metodo que genera una referencia más amigable.
   *
   * @param bookingId ID de la reserva
   * @param type tipo de reserva
   * @return la referencia amigable para el usuario
   */
  String generateBookingReference(Long bookingId, BookingEntity.BookingType type);
}
