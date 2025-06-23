package masera.deviajebookingsandpayments.dtos.bookings;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import masera.deviajebookingsandpayments.dtos.payments.PaymentRequestDto;
import masera.deviajebookingsandpayments.dtos.payments.PricesDto;

/**
 * DTO que representa la solicitud para reservar un paquete y realizar el pago.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookPackageAndPayRequest {


  @NotNull(message = "Los datos de la reserva son obligatorios")
  @Valid
  private CreatePackageBookingRequestDto packageBookingRequest;

  @NotNull(message = "Los datos del pago son obligatorios")
  @Valid
  private PaymentRequestDto paymentRequest;

  private PricesDto prices;
}
