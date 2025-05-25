package masera.deviajebookingsandpayments.clients;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import masera.deviajebookingsandpayments.configs.AmadeusConfig;
import masera.deviajebookingsandpayments.utils.AmadeusErrorHandler;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * Cliente para realizar búsquedas a la API de vuelos.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FlightClient {

  private final WebClient webClient;

  private final AmadeusErrorHandler errorHandler;

  private final AmadeusConfig amadeusConfig;

  private static final String FLIGHT_OFFERS_URL_V1 = "/v1/shopping/flight-offers";


  /**
   * Verifica y devuelve una oferta de vuelo con el precio actualizado.
   *
   * @param flightOffer objeto que contiene la oferta de vuelo a verificar
   * @param token token de autenticación
   * @return oferta de vuelo con precio actualizado
   */
  public Mono<Object> verifyFlightOfferPrice(Object flightOffer, String token) {
    log.info("Verificando el precio de la oferta de vuelo");

    String uri = amadeusConfig.getBaseUrl() + FLIGHT_OFFERS_URL_V1 + "/pricing";

    return webClient.post()
            .uri(uri)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(flightOffer)
            .retrieve()
            .bodyToMono(Object.class)
            .doOnSuccess(response -> log.info("Precio verificado para la oferta"))
            .doOnError(error -> log.error("Error al verificar el precio: {}", error.getMessage()))
            .onErrorResume(WebClientResponseException.class, e -> {
              throw errorHandler.handleAmadeusError(e);
            });
  }
}
