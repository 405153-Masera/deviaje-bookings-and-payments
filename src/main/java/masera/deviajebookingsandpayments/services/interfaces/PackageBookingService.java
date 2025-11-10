package masera.deviajebookingsandpayments.services.interfaces;

import java.util.List;
import masera.deviajebookingsandpayments.dtos.bookings.CreatePackageBookingRequestDto;
import masera.deviajebookingsandpayments.dtos.payments.PaymentRequestDto;
import masera.deviajebookingsandpayments.dtos.payments.PricesDto;
import masera.deviajebookingsandpayments.dtos.responses.BookingReferenceResponse;
import masera.deviajebookingsandpayments.dtos.responses.BookingResponseDto;
import org.springframework.stereotype.Service;

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
  BookingReferenceResponse bookAndPay(CreatePackageBookingRequestDto bookingRequest,
                                      PaymentRequestDto paymentRequest,
                                      PricesDto prices);

  /**
   * Obtiene las reservas de paquetes de un cliente.
   *
   * @param clientId ID del cliente
   * @return lista de reservas del cliente
   */
  List<BookingResponseDto> getClientPackageBookings(Integer clientId);

  /**
   * Obtiene detalles de una reserva de paquete.
   *
   * @param bookingId ID de la reserva
   * @return detalles de la reserva
   */
  BookingResponseDto getPackageBookingDetails(Long bookingId);
}