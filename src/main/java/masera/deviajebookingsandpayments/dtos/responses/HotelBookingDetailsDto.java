package masera.deviajebookingsandpayments.dtos.responses;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para detalles de reservas de hotel.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotelBookingDetailsDto {

  private Long id;

  private String externalId;

  private String hotelName;

  private String destinationName;

  private String roomName;

  private String boardName;

  private String checkInDate;

  private String checkOutDate;

  private Integer numberOfNights;

  private Integer numberOfRooms;

  private Integer adults;

  private Integer children;

  private BigDecimal totalPrice;

  private BigDecimal taxes;

  private String currency;

  private String cancellationFrom;

  private BigDecimal cancellationAmount;

  private LocalDateTime createdDatetime;
}
