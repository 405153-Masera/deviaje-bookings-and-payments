package masera.deviajebookingsandpayments.dtos.bookings.flights;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa un itinerario de vuelo.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItineraryDto {

  private String duration;

  private List<SegmentDto> segments;
}
