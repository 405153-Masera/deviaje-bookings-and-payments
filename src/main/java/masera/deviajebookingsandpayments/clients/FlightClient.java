package masera.deviajebookingsandpayments.clients;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import masera.deviajebookingsandpayments.configs.AmadeusConfig;
import masera.deviajebookingsandpayments.services.interfaces.AmadeusTokenService;
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

  private final AmadeusTokenService amadeusTokenService;

  private static final String FLIGHT_OFFERS_PRICING_URL = "/v1/shopping/flight-offers/pricing";

  private static final String FLIGHT_ORDERS_URL = "/v1/booking/flight-orders";

  /**
   * Verifica y devuelve una oferta de vuelo con el precio actualizado.
   *
   * @param flightOffer objeto que contiene la oferta de vuelo a verificar
   * @return oferta de vuelo con precio actualizado
   */
  public Mono<Object> verifyFlightOfferPrice(Object flightOffer) {
    log.info("Verificando el precio de la oferta de vuelo: {}", flightOffer);

    Object requestBody = java.util.Map.of(
            "data", java.util.Map.of(
                    "type", "flight-offers-pricing",
                    "flightOffers", java.util.List.of(flightOffer)
            )
    );

    String token = amadeusTokenService.getToken();
    String uri = amadeusConfig.getBaseUrl() + FLIGHT_OFFERS_PRICING_URL;

    return webClient.post()
            .uri(uri)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(Object.class)
            .doOnSuccess(response -> log.info("Precio verificado para la oferta"))
            .doOnError(error -> {
              if (error instanceof WebClientResponseException) {
                WebClientResponseException webError = (WebClientResponseException) error;
                log.error("Error al verificar la oferta - Status: {}, Body: {}",
                        webError.getStatusCode(), webError.getResponseBodyAsString());
              } else {
                log.error("Error al verificar la oferta de vuelo: {}", error.getMessage());
              }
            })
            .onErrorResume(WebClientResponseException.class, e -> {
              throw errorHandler.handleAmadeusError(e);
            });
  }

  /**
   * Crea una nueva reserva de vuelo (flight order) en Amadeus.
   *
   * @param bookingData datos de la reserva a crear.
   * @return respuesta de creación de orden
   */
  public Mono<Object> createFlightOrder(Object bookingData) {
    log.info("Creando reserva de vuelo en Amadeus");

    log.info("datos de reserva: {}", bookingData);

    String uri = amadeusConfig.getBaseUrl() + FLIGHT_ORDERS_URL;
    String token = amadeusTokenService.getToken();

    return webClient.post()
            .uri(uri)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(bookingData)
            .retrieve()
            .bodyToMono(Object.class)
            .doOnSuccess(response -> log.info("Reserva de vuelo creada exitosamente"))
            .doOnError(error -> {
              if (error instanceof WebClientResponseException) {
                WebClientResponseException webError = (WebClientResponseException) error;
                log.error("Error al crear reserva de vuelo - Status: {}, Body: {}",
                        webError.getStatusCode(), webError.getResponseBodyAsString());
              } else {
                log.error("Error al crear reserva de vuelo: {}", error.getMessage());
              }
            })
            .onErrorResume(WebClientResponseException.class, e -> {
              throw errorHandler.handleAmadeusError(e);
            });
  }

  /**
   * Obtiene los detalles de una reserva de vuelo existente.
   *
   * @param flightOrderId ID de la reserva en Amadeus
   * @return detalles de la reserva
   */
  public Mono<Object> getFlightOrder(String flightOrderId) {
    log.info("Obteniendo detalles de reserva de vuelo: {}", flightOrderId);

    String uri = amadeusConfig.getBaseUrl() + FLIGHT_ORDERS_URL + "/" + flightOrderId;
    String token = amadeusTokenService.getToken();

    return webClient.get()
            .uri(uri)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .retrieve()
            .bodyToMono(Object.class)
            .doOnSuccess(response -> log.info("Detalles de reserva obtenidos"))
            .doOnError(error -> log.error("Error al obtener detalles de reserva: {}",
                                                      error.getMessage()))
            .onErrorResume(WebClientResponseException.class, e -> {
              throw errorHandler.handleAmadeusError(e);
            });
  }

  /**
   * Cancela una reserva de vuelo existente.
   *
   * @param flightOrderId ID de la reserva en Amadeus
   * @return resultado de la cancelación
   */
  public Mono<Object> cancelFlightOrder(String flightOrderId) {
    log.info("Cancelando reserva de vuelo: {}", flightOrderId);

    String uri = amadeusConfig.getBaseUrl() + FLIGHT_ORDERS_URL + "/" + flightOrderId;
    String token = amadeusTokenService.getToken();

    return webClient.delete()
            .uri(uri)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .retrieve()
            .bodyToMono(Object.class)
            .doOnSuccess(response -> log.info("Reserva cancelada exitosamente"))
            .doOnError(error -> log.error("Error al cancelar reserva: {}", error.getMessage()))
            .onErrorResume(WebClientResponseException.class, e -> {
              throw errorHandler.handleAmadeusError(e);
            });
  }
}
