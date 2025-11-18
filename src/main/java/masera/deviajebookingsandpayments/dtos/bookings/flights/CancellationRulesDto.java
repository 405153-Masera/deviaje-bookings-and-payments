package masera.deviajebookingsandpayments.dtos.bookings.flights;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * Representa las políticas de cancelación de los vuelos.
 */
@Data
public class CancellationRulesDto {

  private String cancellationPolicy;

  private BigDecimal penaltyAmount;

  private String penaltyCurrency;

  private LocalDateTime deadline;

  private String rawText;
}