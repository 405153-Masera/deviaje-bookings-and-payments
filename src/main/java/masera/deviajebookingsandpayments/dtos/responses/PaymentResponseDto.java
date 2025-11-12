package masera.deviajebookingsandpayments.dtos.responses;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para respuestas de operaciones de pago.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponseDto {

  private Long id;

  private String externalPaymentId;

  private BigDecimal amount;

  private String currency;

  private String status; // "APPROVED", "REJECTED", "PENDING", "CANCELLED", "REFUNDED"

  private String method;

  private String paymentProvider;

  private String errorCode;

  private String errorMessage;

  private LocalDateTime date;

  private String paymentUrl; // URL para redirección en caso de pago externo

  /**
   * Metodo que crea un pago aprobado.
   *
   * @param id clave primaria de la base de datos
   * @param externalPaymentId clave generada por mercado pago
   * @param amount precio
   * @param currency moneda
   * @return el pago aprobado
   */
  public static PaymentResponseDto approved(
          Long id, String externalPaymentId, BigDecimal amount, String currency) {
    return PaymentResponseDto.builder()
            .id(id)
            .externalPaymentId(externalPaymentId)
            .amount(amount)
            .currency(currency)
            .status("APPROVED")
            .date(LocalDateTime.now())
            .build();
  }

  /**
   * Metodo que crea un pago reembolsado.
   *
   * @param id clave primaria de la base de datos
   * @param externalPaymentId clave generada por mercado pago
   * @param amount precio
   * @return el pago reembolsado
   */
  public static PaymentResponseDto refunded(Long id, String externalPaymentId, BigDecimal amount) {
    return PaymentResponseDto.builder()
            .id(id)
            .externalPaymentId(externalPaymentId)
            .amount(amount)
            .status("REFUNDED")
            .date(LocalDateTime.now())
            .build();
  }
}
