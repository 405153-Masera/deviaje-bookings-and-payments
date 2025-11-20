package masera.deviajebookingsandpayments.services.interfaces;

import masera.deviajebookingsandpayments.dtos.responses.BookingDetailsResponseDto;
import masera.deviajebookingsandpayments.dtos.responses.CancelBookingRequestDto;
import masera.deviajebookingsandpayments.dtos.responses.CancellationResponseDto;
import masera.deviajebookingsandpayments.entities.BookingEntity;
import org.springframework.stereotype.Service;

/**
 * Servicio para gestionar cancelaciones de reservas.
 */
@Service
public interface BookingCancellationService {

  /**
   * Cancela una reserva y procesa el reembolso si corresponde.
   *
   * @param booking la reserva a cancelar
   * @param request datos de la solicitud de cancelación
   * @return respuesta con detalles de la cancelación
   */
  CancellationResponseDto cancelBooking(BookingEntity booking, CancelBookingRequestDto request);

  /**
   * Obtiene información sobre la cancelación sin procesarla.
   *
   * @param booking la reserva
   * @return información de cancelación (política, monto reembolsable, etc.)
   */
  CancellationResponseDto getCancellationInfo(BookingEntity booking);

}