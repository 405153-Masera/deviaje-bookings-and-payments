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

  // Métodos de conveniencia para la creación rápida de respuestas
  public static PaymentResponseDto approved(Long id, String externalPaymentId, BigDecimal amount, String currency) {
    return PaymentResponseDto.builder()
            .id(id)
            .externalPaymentId(externalPaymentId)
            .amount(amount)
            .currency(currency)
            .status("APPROVED")
            .date(LocalDateTime.now())
            .build();
  }

  public static PaymentResponseDto rejected(String errorCode, String errorMessage) {
    return PaymentResponseDto.builder()
            .status("REJECTED")
            .errorCode(errorCode)
            .errorMessage(errorMessage)
            .date(LocalDateTime.now())
            .build();
  }

  public static PaymentResponseDto pending(String externalPaymentId, BigDecimal amount, String paymentUrl) {
    return PaymentResponseDto.builder()
            .externalPaymentId(externalPaymentId)
            .amount(amount)
            .status("PENDING")
            .paymentUrl(paymentUrl)
            .date(LocalDateTime.now())
            .build();
  }

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