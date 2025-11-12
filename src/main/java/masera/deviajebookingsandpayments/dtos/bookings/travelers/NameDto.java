package masera.deviajebookingsandpayments.dtos.bookings.travelers;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa el nombre completo.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NameDto {

  private String firstName;

  private String lastName;
}
