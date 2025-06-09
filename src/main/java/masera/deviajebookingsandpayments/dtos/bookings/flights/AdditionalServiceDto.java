package masera.deviajebookingsandpayments.dtos.bookings.flights;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa un servicio adicional para un vuelo.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdditionalServiceDto {
  private String amount;
  private String type;
}
