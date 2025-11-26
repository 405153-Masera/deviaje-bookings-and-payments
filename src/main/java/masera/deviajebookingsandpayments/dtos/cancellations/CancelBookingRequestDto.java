package masera.deviajebookingsandpayments.dtos.cancellations;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Data;

/**
 * DTO para solicitud de cancelación de reserva.
 */
@Data
public class CancelBookingRequestDto {

  @Size(max = 500, message = "El motivo no puede exceder 500 caracteres")
  private String cancellationReason;

  @Size(max = 500, message = "Los detalles no pueden exceder 500 caracteres")
  private String additionalDetails;

  @NotNull(message = "El monto de reembolso es requerido")
  private BigDecimal refundAmount;
}
