package masera.deviajebookingsandpayments.dtos.bookings.flights;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import masera.deviajebookingsandpayments.dtos.bookings.travelers.TravelerDto;

import java.util.List;

/**
 * DTO para crear una reserva de vuelo.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateFlightBookingRequestDto {

  @NotNull(message = "El ID del cliente es obligatorio")
  private Long clientId;

  private Long agentId;

  private Long branchId;

  @NotNull(message = "La oferta de vuelo es obligatoria")
  private FlightOfferDto flightOffer;

  @NotEmpty(message = "La lista de pasajeros no puede estar vacía")
  private List<TravelerDto> travelers;

  private TicketingAgreementDto ticketingAgreement;
}