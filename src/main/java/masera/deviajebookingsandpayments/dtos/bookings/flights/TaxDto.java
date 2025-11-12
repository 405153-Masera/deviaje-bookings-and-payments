package masera.deviajebookingsandpayments.dtos.bookings.flights;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa un impuesto.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxDto {

  private String amount;

  private String code;
}
