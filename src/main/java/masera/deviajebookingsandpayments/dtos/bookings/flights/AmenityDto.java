package masera.deviajebookingsandpayments.dtos.bookings.flights;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AmenityDto {
  private String description;
  private Boolean isChargeable;
  private String amenityType;
  private AmenityProviderDto amenityProvider;
}
