package masera.deviajebookingsandpayments.dtos.bookings.flights;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa el operador de un vuelo.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperatingDto {
  private String carrierCode;
  private String carrierName;
}
