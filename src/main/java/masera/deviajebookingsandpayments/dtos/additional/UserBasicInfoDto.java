package masera.deviajebookingsandpayments.dtos.additional;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para información básica de usuario.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBasicInfoDto {

  private Integer id;

  private String username;

  private String firstName;

  private String lastName;

  private String email;

  /**
   * Obtiene el nombre completo del usuario.
   *
   * @return nombre completo
   */
  public String getUserName() {
    return username != null ? username : "Usuario desconocido";
  }
}