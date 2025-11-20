package masera.deviajebookingsandpayments.dtos.responses;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para respuesta de cancelación de reserva.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancellationResponseDto {

  /**
   * Indica si la cancelación fue exitosa.
   */
  private boolean success;

  /**
   * Mensaje descriptivo del resultado.
   */
  private String message;

  /**
   * Política de cancelación aplicada.
   */
  private String cancellationPolicy;

  /**
   * Monto total de la reserva.
   */
  private BigDecimal totalAmount;

  /**
   * Monto del reembolso.
   */
  private BigDecimal refundAmount;

  /**
   * Monto de la penalidad.
   */
  private BigDecimal penaltyAmount;

  /**
   * Moneda.
   */
  private String currency;

  /**
   * Indica si es reembolsable.
   */
  private boolean isRefundable;

  /**
   * Id del reembolso en MercadoPago (si aplica).
   */
  private Long mercadoPagoRefundId;

  /**
   * Referencia de la reserva cancelada.
   */
  private String bookingReference;
}
