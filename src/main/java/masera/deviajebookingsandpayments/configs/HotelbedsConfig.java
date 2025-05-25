package masera.deviajebookingsandpayments.configs;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración para la API de Hotelbeds.
 */
@Configuration
@Getter
public class HotelbedsConfig {

  @Value("${hotelbeds.api.key}")
  private String apiKey;

  @Value("${hotelbeds.api.secret}")
  private String apiSecret;

  @Value("${hotelbeds.api.base-url}")
  private String baseUrl;
}
