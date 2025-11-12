package masera.deviajebookingsandpayments.dtos.bookings.flights;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa la emisión de CO2 de un vuelo.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Co2EmissionDto {

  private Integer weight;

  private String weightUnit;

  private String cabin;
}
