package masera.deviajebookingsandpayments.dtos.bookings.flights;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelerPricingDto {
  private String travelerId;
  private String fareOption;
  private String travelerType;
  private PriceDetailDto price;
  private List<FareDetailsBySegmentDto> fareDetailsBySegment;
  private List<AmenityDto> amenities; // Agregado del GET
}
