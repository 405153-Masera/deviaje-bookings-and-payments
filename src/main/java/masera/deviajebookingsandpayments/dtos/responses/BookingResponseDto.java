package masera.deviajebookingsandpayments.dtos.responses;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta principal para reservas.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponseDto {

  private Long id;

  private String bookingReference;

  private Integer clientId;

  private Integer agentId;

  private String status;

  private String type; // "FLIGHT", "HOTEL", "PACKAGE"

  private BigDecimal totalAmount;

  private BigDecimal commission;

  private BigDecimal discount;

  private BigDecimal taxes;

  private String currency;

  private String holderName;

  private String phone;

  private String email;

  private LocalDateTime createdDatetime;
}
