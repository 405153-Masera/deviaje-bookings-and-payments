package masera.deviajebookingsandpayments.dtos.bookings.hotels;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomDto {

  @NotBlank(message = "El rateKey es obligatorio")
  private String rateKey;

  @NotEmpty(message = "La lista de huéspedes no puede estar vacía")
  private List<PaxDto> paxes;
}
