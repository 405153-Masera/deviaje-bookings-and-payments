package masera.deviajebookingsandpayments.dtos.bookings.hotels;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para crear una reserva de hotel.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateHotelBookingRequestDto {

  @NotNull(message = "El ID del cliente es obligatorio")
  private Long clientId;

  private Long agentId;

  private Long branchId;

  @NotNull(message = "El titular de la reserva es obligatorio")
  private HolderDto holder;

  @NotEmpty(message = "La lista de habitaciones no puede estar vacía")
  private List<RoomDto> rooms;

  private String clientReference;

  private String remark;

  private Integer tolerance = 2;
}

