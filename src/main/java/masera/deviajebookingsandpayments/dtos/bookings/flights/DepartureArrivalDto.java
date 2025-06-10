package masera.deviajebookingsandpayments.dtos.bookings.flights;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa la información de salida y llegada de un vuelo.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartureArrivalDto {
  private String iataCode;
  private String terminal;
  private String at;
}
