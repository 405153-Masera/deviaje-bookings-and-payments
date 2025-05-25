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
  private Boolean blacklistedInEU;
  private List<Co2EmissionDto> co2Emissions;
}
