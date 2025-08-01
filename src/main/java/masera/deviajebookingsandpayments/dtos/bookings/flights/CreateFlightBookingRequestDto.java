package masera.deviajebookingsandpayments.dtos.bookings.flights;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import masera.deviajebookingsandpayments.dtos.bookings.travelers.TravelerDto;
import masera.deviajebookingsandpayments.dtos.payments.PricesDto;


/**
 * DTO para crear una reserva de vuelo.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateFlightBookingRequestDto {

  private Integer clientId;

  private Integer agentId;

  //Política de cancelación
  private LocalDate cancellationFrom;
  private BigDecimal cancellationAmount;

  @NotNull(message = "La oferta de vuelo es obligatoria")
  private FlightOfferDto flightOffer;

  @NotEmpty(message = "La lista de pasajeros no puede estar vacía")
  private List<TravelerDto> travelers;
}