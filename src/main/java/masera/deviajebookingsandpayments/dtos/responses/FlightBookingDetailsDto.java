package masera.deviajebookingsandpayments.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para detalles de reservas de vuelo.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightBookingDetailsDto {
  private Long id;
  private String externalId;
  private String origin;
  private String destination;
  private String departureDate;
  private String returnDate;
  private String carrier;
  private Integer adults;
  private Integer children;
  private Integer infants;
  private String itineraries;
  private BigDecimal totalPrice;
  private BigDecimal taxes;
  private String currency;
  private String cancellationFrom;
  private BigDecimal cancellationAmount;
  private LocalDateTime createdDatetime;
}