package masera.deviajebookingsandpayments.dtos.payments;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa un pasajero.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayerDto {

  private String email;

  private String firstName;

  private String lastName;

  private String identification;

  private String identificationType;
}
