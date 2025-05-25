package masera.deviajebookingsandpayments.configs;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración para la API de Mercado Pago.
 */
@Configuration
@Getter
public class PagoConfig {

  @Value("${mercadopago.access-token}")
  private String accessToken;

  @Value("${mercadopago.public-key}")
  private String publicKey;

  @Value("${mercadopago.urls.success}")
  private String successUrl;

  @Value("${mercadopago.urls.failure}")
  private String failureUrl;

  @Value("${mercadopago.urls.pending}")
  private String pendingUrl;

  @Value("${mercadopago.webhook-url}")
  private String webhookUrl;
}
