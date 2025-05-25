package masera.deviajebookingsandpayments.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de respuesta para reservas de hotel.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotelBookingResponseDto {

  private Long id;
  private Long bookingId;
  private String externalId;
  private String hotelName;
  private String destinationName;
  private LocalDate checkInDate;
  private LocalDate checkOutDate;
  private Integer numberOfNights;
  private Integer numberOfRooms;
  private Integer adults;
  private Integer children;
  private BigDecimal basePrice;
  private BigDecimal taxes;
  private BigDecimal discounts;
  private BigDecimal totalPrice;
  private String currency;
  private String status;

}