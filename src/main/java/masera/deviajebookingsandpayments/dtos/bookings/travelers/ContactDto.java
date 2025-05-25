package masera.deviajebookingsandpayments.dtos.bookings.travelers;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactDto {
  private String emailAddress;
  private List<PhoneDto> phones;
}
