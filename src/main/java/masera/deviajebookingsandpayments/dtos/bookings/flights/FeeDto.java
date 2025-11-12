package masera.deviajebookingsandpayments.dtos.bookings.flights;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa tasas adicionales.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeDto {

  private String amount;

  private String type;
}
