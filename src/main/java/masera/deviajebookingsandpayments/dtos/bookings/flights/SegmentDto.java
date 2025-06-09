package masera.deviajebookingsandpayments.dtos.bookings.flights;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que el segmento de un itinerario de vuelo.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SegmentDto {
  private DepartureArrivalDto departure;
  private DepartureArrivalDto arrival;
  private String carrierCode;
  private String number;
  private AircraftDto aircraft;
  private OperatingDto operating;
  private String duration;
  private String id;
  private Integer numberOfStops;
  @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
  private Boolean blacklistedInEU;
  private List<Co2EmissionDto> co2Emissions;
}
