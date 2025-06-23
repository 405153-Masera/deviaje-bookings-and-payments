package masera.deviajebookingsandpayments.dtos.bookings.hotels;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import masera.deviajebookingsandpayments.dtos.payments.PaymentRequestDto;
import masera.deviajebookingsandpayments.dtos.payments.PricesDto;

/**
 * DTO que encapsula tanto la reserva de hotel como el pago en una sola solicitud.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookHotelAndPayRequest {

  @NotNull(message = "Los datos de la reserva son obligatorios")
  @Valid
  private CreateHotelBookingRequestDto bookingRequest;

  @NotNull(message = "Los datos del pago son obligatorios")
  @Valid
  private PaymentRequestDto paymentRequest;

  private PricesDto prices;
}