package masera.deviajebookingsandpayments.dtos.payments;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * DTO para procesar pago (Forma B - Backend directo).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequestDto {

  @NotNull(message = "El monto es obligatorio")
  @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
  private BigDecimal amount;

  @NotBlank(message = "La moneda es obligatoria")
  @Size(max = 3, message = "La moneda debe tener máximo 3 caracteres")
  private String currency;

  @NotBlank(message = "El método de pago es obligatorio")
  private String paymentMethod; // "mercado_pago", "credit_card", etc.

  @NotBlank(message = "El token de pago es obligatorio")
  private String paymentToken; // Token seguro de MP o tarjeta

  @Min(value = 1, message = "Las cuotas deben ser mínimo 1")
  @Max(value = 24, message = "Las cuotas no pueden ser más de 24")
  private Integer installments = 1;

  private String description;

  // Datos adicionales para Mercado Pago
  private PayerDto payer;

  // id del emisor de la tarjeta (opcional)
  private String issuerId;
}

