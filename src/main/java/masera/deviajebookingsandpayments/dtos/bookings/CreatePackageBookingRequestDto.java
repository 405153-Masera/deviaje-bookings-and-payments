package masera.deviajebookingsandpayments.dtos.bookings;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import masera.deviajebookingsandpayments.dtos.bookings.flights.CreateFlightBookingRequestDto;
import masera.deviajebookingsandpayments.dtos.bookings.hotels.CreateHotelBookingRequestDto;

/**
 * DTO para crear una reserva de paquete.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePackageBookingRequestDto {

  private Integer clientId;

  private Integer agentId;

  private CreateFlightBookingRequestDto flightBooking;

  private CreateHotelBookingRequestDto hotelBooking;
}