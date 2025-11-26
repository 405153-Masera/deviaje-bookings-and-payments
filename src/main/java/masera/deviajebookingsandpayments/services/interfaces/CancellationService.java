package masera.deviajebookingsandpayments.services.interfaces;

import masera.deviajebookingsandpayments.dtos.cancellations.CancelBookingRequestDto;
import masera.deviajebookingsandpayments.dtos.cancellations.CancelBookingResponseDto;
import org.springframework.stereotype.Service;

/**
 * Servicio para gestionar cancelaciones de reservas.
 */
@Service
public interface CancellationService {

  /**
   * Cancela una reserva y procesa el reembolso si corresponde.
   *
   * @param bookingId id de la reserva a cancelar
   * @param request datos de la solicitud de cancelación
   * @return respuesta con detalles de la cancelación
   */
  CancelBookingResponseDto cancelBooking(Long bookingId, CancelBookingRequestDto request);
}
