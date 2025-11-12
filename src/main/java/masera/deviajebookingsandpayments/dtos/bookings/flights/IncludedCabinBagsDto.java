package masera.deviajebookingsandpayments.dtos.bookings.flights;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa las maletas de cabina incluidas.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncludedCabinBagsDto {

  private Integer weight;

  private String weightUnit;
}
