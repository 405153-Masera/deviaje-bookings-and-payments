package masera.deviajebookingsandpayments.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de respuesta principal para reservas.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponseDto {

  private Long id;
  private Integer clientId;
  private Integer agentId;
  private String status;
  private String type; // "FLIGHT", "HOTEL", "PACKAGE" - debe coincidir con el campo de la BD
  private BigDecimal totalAmount;
  private BigDecimal commission;
  private BigDecimal discount;
  private BigDecimal taxes;
  private String currency;
  private String holderName; // Campo faltante
  private String phone; // Campo faltante
  private String email; // Campo faltante
  private LocalDateTime createdDatetime; // Campo faltante
  private Integer createdUser; //

}