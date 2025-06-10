package masera.deviajebookingsandpayments.dtos.bookings.flights;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FareDetailsBySegmentDto {
  private String segmentId;
  private String cabin;
  private String fareBasis;
  private String brandedFare;
  private String brandedFareLabel; // Agregado del GET
  @JsonProperty("class")
  private String classCode;
  private String sliceDiceIndicator;
  private IncludedCheckedBagsDto includedCheckedBags;
  private IncludedCabinBagsDto includedCabinBags; // Agregado del GET
}
