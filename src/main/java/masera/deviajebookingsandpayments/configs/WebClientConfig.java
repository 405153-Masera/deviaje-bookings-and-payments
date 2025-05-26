package masera.deviajebookingsandpayments.configs;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;


/**
 * WebClient clase de configuración.
 * Esta clase se encarga de la configuración del WebClient para
 * realizar peticiones HTTP.
 */
@Configuration
public class WebClientConfig {

  private static final int CONNECT_TIMEOUT = 5000;
  private static final int READ_TIMEOUT = 10000;
  private static final int WRITE_TIMEOUT = 5000;
  //private static final int MAX_IN_MEMORY_SIZE = 16 * 1024 * 1024; // 16MB
  private static final int MAX_IN_MEMORY_SIZE = 200 * 1024 * 1024;

  /**
   * Metodo para crear una instancia de WebClient.
   * Este metodo configura el WebClient con un timeout y
   * establece la URL base y los encabezados por defecto.
   *
   * @return una instancia de WebClient configurada.
   */
  @Bean
  public WebClient webClient() {
    HttpClient httpClient = getHttpClient();
    ExchangeStrategies exchangeStrategies = getExchangeStrategies();

    return WebClient.builder()
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .exchangeStrategies(exchangeStrategies)
            .build();
  }

  /**
   * Configura el cliente HTTP con timeouts y otros parámetros.
   *
   * @return HttpClient configurado
   */
  private HttpClient getHttpClient() {
    return HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT)
            .responseTimeout(Duration.ofMillis(READ_TIMEOUT))
            .doOnConnected(conn -> conn
                    .addHandlerLast(new ReadTimeoutHandler(READ_TIMEOUT, TimeUnit.MILLISECONDS))
                    .addHandlerLast(new WriteTimeoutHandler(WRITE_TIMEOUT, TimeUnit.MILLISECONDS)));
  }

  /**
   * Configura las estrategias de intercambio para WebClient.
   *
   * @return ExchangeStrategies configuradas
   */
  private ExchangeStrategies getExchangeStrategies() {
    return ExchangeStrategies.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_SIZE))
            .build();
  }
}
