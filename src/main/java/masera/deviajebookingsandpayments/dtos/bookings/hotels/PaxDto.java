package masera.deviajebookingsandpayments.dtos.bookings.hotels;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa un pasajero.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaxDto {

  @NotNull(message = "El ID de la habitación es obligatorio")
  private Integer roomId;

  @NotBlank(message = "El tipo de huésped es obligatorio")
  @Pattern(regexp = "AD|CH|IN", message = "El tipo debe ser AD (adulto), CH (niño) o IN (infante)")
  private String type;

  @NotBlank(message = "El nombre del huésped es obligatorio")
  private String name;

  @NotBlank(message = "El apellido del huésped es obligatorio")
  private String surname;
}
