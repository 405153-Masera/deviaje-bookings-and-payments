package masera.deviajebookingsandpayments.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import masera.deviajebookingsandpayments.dtos.bookings.flights.CreateFlightBookingRequestDto;
import masera.deviajebookingsandpayments.dtos.payments.PaymentRequestDto;

/**
 * DTO que combina la reserva y el pago en un solo request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookFlightAndPayRequest {


  @NotNull(message = "Los datos de la reserva son obligatorios")
  @Valid
  private CreateFlightBookingRequestDto bookingRequest;

  @NotNull(message = "Los datos del pago son obligatorios")
  @Valid
  private PaymentRequestDto paymentRequest;
}
