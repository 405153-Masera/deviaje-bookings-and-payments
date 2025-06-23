package masera.deviajebookingsandpayments.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO de respuesta principal para reservas.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponseDto {

  private Long id;
  private Long clientId;
  private Long agentId;
  private String status;
  private String bookingType; // "FLIGHT", "HOTEL", "PACKAGE"
  private BigDecimal totalAmount;
  private String currency;
  private BigDecimal discount;
  private BigDecimal taxes;

}