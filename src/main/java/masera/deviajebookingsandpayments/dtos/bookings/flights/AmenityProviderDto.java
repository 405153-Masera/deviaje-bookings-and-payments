package masera.deviajebookingsandpayments.dtos.bookings.flights;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa el proveedor del servicio.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AmenityProviderDto {

  private String name;
}
