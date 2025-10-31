package masera.deviajebookingsandpayments.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de respuesta para detalles completos de reserva con relaciones.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingDetailsResponseDto {

  private Long id;
  private String bookingReference;
  private Integer clientId;
  private Integer agentId;
  private String status;
  private String type;
  private BigDecimal totalAmount;
  private BigDecimal commission;
  private BigDecimal discount;
  private BigDecimal taxes;
  private String currency;
  private String holderName;
  private String phone;
  private String email;
  private LocalDateTime createdDatetime;

  private List<FlightBookingDetailsDto> flightBookings;
  private List<HotelBookingDetailsDto> hotelBookings;
}
