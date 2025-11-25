package masera.deviajebookingsandpayments.services.interfaces;

import java.util.List;
import masera.deviajebookingsandpayments.dtos.responses.BookingDetailsResponseDto;
import masera.deviajebookingsandpayments.dtos.responses.BookingResponseDto;
import masera.deviajebookingsandpayments.entities.BookingEntity;
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
   * Metodo que trae todas las reservas del cliente.
   *
   * @param clientId id del cliente
   * @param email email del cliente
   * @param holderName nombre del cliente
   * @return una lista de reservas
   */
  List<BookingResponseDto> getClientBookings(Integer clientId,
                                             String email,
                                             String holderName);

  /**
   * Metodo que trae todas las reservas del agente.
   *
   * @param agentId id del agente
   * @param clientId id del cliente
   * @param email email del cliente
   * @param holderName nombre del cliente
   * @return una lista de reservas
   */
  List<BookingResponseDto> getAgentBookings(Integer agentId,
                                            Integer clientId,
                                            String email,
                                            String holderName);

  /**
   * Metodo que trae todas las reservas.
   *
   * @param agentId id del agente
   * @param clientId id del cliente
   * @param email email del cliente
   * @param holderName nombre del cliente
   * @return una lista de reservas
   */
  List<BookingResponseDto> getAllBookings(Integer agentId,
                                          Integer clientId,
                                          String email,
                                          String holderName);

  /**
   * Metodo que devuelve una reserva.
   *
   * @param bookingId id de la reserva
   * @return la reserva
   */
  BookingResponseDto getBookingById(Long bookingId);

  /**
   * Obtiene los detalles completos de una reserva por su bookingReference.
   *
   * @param bookingReference referencia de la reserva
   * @return la reserva con detalles completos
   */
  BookingDetailsResponseDto getBookingDetailsByReference(String bookingReference);

  /**
   * Obtiene los detalles completos de una reserva.
   *
   * @param bookingId id de la reserva
   * @return la reserva
   */
  BookingDetailsResponseDto getBookingDetails(Long bookingId);

  /**
   * Metodo para descargar el voucher de la reserva.
   *
   * @param bookingId id de la reserva
   * @return el voucher
   */
  byte[] downloadVoucher(Long bookingId);

  /**
   * Metodo que genera una referencia más amigable.
   *
   * @param bookingId ID de la reserva
   * @param type tipo de reserva
   * @return la referencia amigable para el usuario
   */
  String generateBookingReference(Long bookingId, BookingEntity.BookingType type);

  /**
   * Devuelve la referencia amigable.
   *
   * @param bookingId id de la reserva
   * @return la referencia
   */
  String getBookingReference(Long bookingId);
}
