package masera.deviajebookingsandpayments.services.interfaces;

import masera.deviajebookingsandpayments.dtos.bookings.CreatePackageBookingRequestDto;
import masera.deviajebookingsandpayments.dtos.payments.PaymentRequestDto;
import masera.deviajebookingsandpayments.dtos.responses.BookAndPayResponseDto;
import masera.deviajebookingsandpayments.dtos.responses.BookingResponseDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Interfaz para el servicio de reservas de paquetes.
 */
@Service
public interface PackageBookingService {

  /**
   * Procesa una reserva de paquete y su pago de forma unificada.
   *
   * @param bookingRequest datos de la reserva
   * @param paymentRequest datos del pago
   * @return respuesta unificada con resultado de la operación
   */
  BookAndPayResponseDto bookAndPay(CreatePackageBookingRequestDto bookingRequest, PaymentRequestDto paymentRequest);

  /**
   * Obtiene las reservas de paquetes de un cliente.
   *
   * @param clientId ID del cliente
   * @return lista de reservas del cliente
   */
  List<BookingResponseDto> getClientPackageBookings(Long clientId);

  /**
   * Obtiene detalles de una reserva de paquete.
   *
   * @param bookingId ID de la reserva
   * @return detalles de la reserva
   */
  BookingResponseDto getPackageBookingDetails(Long bookingId);

  /**
   * Cancela una reserva de paquete.
   *
   * @param bookingId ID de la reserva a cancelar
   * @return respuesta con el resultado de la cancelación
   */
  BookAndPayResponseDto cancelBooking(Long bookingId);

  /**
   * Verifica disponibilidad y precio de un paquete.
   *
   * @param packageDetails Detalles del paquete a verificar
   * @return información actualizada del paquete
   */
  Map<String, Object> verifyPackagePrice(Map<String, Object> packageDetails);
}