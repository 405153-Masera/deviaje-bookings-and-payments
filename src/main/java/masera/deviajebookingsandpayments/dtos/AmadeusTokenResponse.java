package masera.deviajebookingsandpayments.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa la respuesta de autenticación de Amadeus.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AmadeusTokenResponse {

  @JsonProperty("access_token")
  private String accessToken;

  @JsonProperty("expires_in")
  private Integer expiresIn;

  @JsonProperty("token_type")
  private String tokenType;
}
