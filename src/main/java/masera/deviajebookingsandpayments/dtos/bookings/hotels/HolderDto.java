package masera.deviajebookingsandpayments.dtos.bookings.hotels;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa el titular de la reserva.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HolderDto {

  @NotBlank(message = "El nombre del titular es obligatorio")
  private String name;

  @NotBlank(message = "El apellido del titular es obligatorio")
  private String surname;

  private String email;

  private String phone;

  private String countryCallingCode;
}
