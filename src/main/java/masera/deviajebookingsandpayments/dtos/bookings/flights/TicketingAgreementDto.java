package masera.deviajebookingsandpayments.dtos.bookings.flights;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa un ticket de avión.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketingAgreementDto {

  private String option;

  private String delay;
}