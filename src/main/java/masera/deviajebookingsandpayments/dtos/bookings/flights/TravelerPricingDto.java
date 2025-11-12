package masera.deviajebookingsandpayments.dtos.bookings.flights;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa detalles de los precios de cada pasajero.
 */
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

  private List<AmenityDto> amenities;
}
