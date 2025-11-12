package masera.deviajebookingsandpayments.dtos.bookings.travelers;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa un contacto.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactDto {

  private String emailAddress;

  private List<PhoneDto> phones;
}
