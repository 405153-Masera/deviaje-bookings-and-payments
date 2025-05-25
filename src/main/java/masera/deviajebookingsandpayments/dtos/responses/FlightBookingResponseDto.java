package masera.deviajebookingsandpayments.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de respuesta para reservas de vuelo.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlightBookingResponseDto {

  private Long id;
  private Long bookingId;
  private String externalId; // ID de Amadeus
  private String origin;
  private String destination;
  private LocalDateTime departureDate;
  private LocalDateTime returnDate;
  private String carrier;
  private String carrierName;
  private BigDecimal basePrice;
  private BigDecimal taxes;
  private BigDecimal discounts;
  private BigDecimal totalPrice;
  private String currency;
  private String status;
}
