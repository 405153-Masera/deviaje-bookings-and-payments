package masera.deviajebookingsandpayments.configs;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de Amadeus API.
 */
@Configuration
@Getter
public class AmadeusConfig {

  @Value("${amadeus.api.key}")
  private String apiKey;

  @Value("${amadeus.api.secret}")
  private String apiSecret;

  @Value("${amadeus.api.base-url}")
  private String baseUrl;

  @Value("${amadeus.api.token-url}")
  private String tokenUrl;

  // Duración del token en segundos (30 minutos por defecto para Amadeus)
  @Value("${amadeus.token.duration:1800}")
  private int tokenDuration;
}
