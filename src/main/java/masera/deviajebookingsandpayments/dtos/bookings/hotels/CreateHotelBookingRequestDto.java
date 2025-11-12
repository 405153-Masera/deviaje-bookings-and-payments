package masera.deviajebookingsandpayments.dtos.bookings.hotels;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para crear una reserva de hotel.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateHotelBookingRequestDto {

  private Integer clientId;

  private Integer agentId;

  @NotNull(message = "El titular de la reserva es obligatorio")
  private HolderDto holder;

  private String countryName;

  @NotEmpty(message = "La lista de habitaciones no puede estar vacía")
  private List<RoomDto> rooms;

  private String clientReference;

  private String remark;

  private Integer tolerance = 2;
}
