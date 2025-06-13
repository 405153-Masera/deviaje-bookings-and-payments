package masera.deviajebookingsandpayments.dtos.bookings.flights;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class TravelerPricingDto {
  private String travelerId;
  private String fareOption;
  private String travelerType;
  private String associatedAdultId;
  private PriceDetailDto price;
  private List<FareDetailsBySegmentDto> fareDetailsBySegment;
  private List<AmenityDto> amenities; // Agregado del GET
}
