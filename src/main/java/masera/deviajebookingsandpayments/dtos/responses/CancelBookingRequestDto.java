package masera.deviajebookingsandpayments.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para solicitud de cancelación de reserva.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelBookingRequestDto {

  /**
   * Razón de la cancelación (opcional).
   */
  private String reason;

  /**
   * Id del usuario que solicita la cancelación.
   */
  private Integer requestedBy;

  /**
   * Rol del usuario que solicita la cancelación.
   */
  private String requestedByRole;
}
