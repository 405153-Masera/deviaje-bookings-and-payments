package masera.deviajebookingsandpayments.dtos.bookings.travelers;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhoneDto {
  private String deviceType;
  private String countryCallingCode;
  private String number;
}
