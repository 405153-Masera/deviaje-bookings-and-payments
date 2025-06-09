package masera.deviajebookingsandpayments.dtos.bookings.flights;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa el código de un avión.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AircraftDto {
  private String code;
}
