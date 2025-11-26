package masera.deviajebookingsandpayments.dtos.cancellations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para respuesta de cancelación de reserva.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelBookingResponseDto {

  private Long bookingId;

  private String bookingReference;

  private String status;

  private String bookingType;

  private LocalDateTime cancelledAt;

  private String message;

  private BigDecimal flightRefundAmount;

  private BigDecimal hotelRefundAmount;

  private BigDecimal totalRefundAmount;

  private String currency;

  private boolean emailSent;
}
