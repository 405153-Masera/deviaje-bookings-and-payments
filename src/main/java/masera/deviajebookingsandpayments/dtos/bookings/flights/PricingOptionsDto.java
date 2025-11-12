package masera.deviajebookingsandpayments.dtos.bookings.flights;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa las opciones del precio.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingOptionsDto {

  private List<String> fareType;

  private Boolean includedCheckedBagsOnly;
}
