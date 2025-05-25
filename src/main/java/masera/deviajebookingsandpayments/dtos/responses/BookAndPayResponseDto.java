package masera.deviajebookingsandpayments.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta unificada para operaciones de reserva y pago.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookAndPayResponseDto {

  private Boolean success;
  private String message;
  private String errorCode;

  // Si success = true
  private BookingResponseDto booking;

  // Si success = false
  private String failureReason; // "PAYMENT_FAILED", "BOOKING_FAILED", "VERIFICATION_FAILED"
  private String detailedError;

  // Métodos de conveniencia
  public static BookAndPayResponseDto success(BookingResponseDto booking) {
    return BookAndPayResponseDto.builder()
            .success(true)
            .message("Reserva y pago procesados exitosamente")
            .booking(booking)
            .build();
  }

  public static BookAndPayResponseDto paymentFailed(String errorMessage) {
    return BookAndPayResponseDto.builder()
            .success(false)
            .message("Error en el procesamiento del pago")
            .failureReason("PAYMENT_FAILED")
            .detailedError(errorMessage)
            .build();
  }

  public static BookAndPayResponseDto bookingFailed(String errorMessage) {
    return BookAndPayResponseDto.builder()
            .success(false)
            .message("Error al crear la reserva")
            .failureReason("BOOKING_FAILED")
            .detailedError(errorMessage)
            .build();
  }

  public static BookAndPayResponseDto verificationFailed(String errorMessage) {
    return BookAndPayResponseDto.builder()
            .success(false)
            .message("Error en la verificación de precios")
            .failureReason("VERIFICATION_FAILED")
            .detailedError(errorMessage)
            .build();
  }
}