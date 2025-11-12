package masera.deviajebookingsandpayments.dtos.bookings.flights;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa los detalles de una tarifa.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FareDetailsBySegmentDto {

  private String segmentId;

  private String cabin;

  private String fareBasis;

  private String brandedFare;

  private String brandedFareLabel;

  @JsonProperty("class")
  private String classCode;

  private String sliceDiceIndicator;

  private IncludedCheckedBagsDto includedCheckedBags;

  private IncludedCabinBagsDto includedCabinBags;
}
