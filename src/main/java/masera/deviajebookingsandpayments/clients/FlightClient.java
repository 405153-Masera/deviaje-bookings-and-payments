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

  private static final String FLIGHT_ORDERS_URL = "/v1/booking/flight-orders";

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

  /**
   * Crea una nueva reserva de vuelo (flight order) en Amadeus.
   *
   * @param bookingRequest detalles de la reserva
   * @param token token de autenticación
   * @return respuesta de creación de orden
   */
  public Mono<Object> createFlightOrder(CreateFlightBookingRequestDto bookingRequest, String token) {
    log.info("Creando reserva de vuelo en Amadeus");

    String uri = amadeusConfig.getBaseUrl() + FLIGHT_ORDERS_URL;

    // Construir el cuerpo de la solicitud según el formato de Amadeus
    Object requestBody = formatCreateFlightOrderRequest(bookingRequest);

    return webClient.post()
            .uri(uri)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(Object.class)
            .doOnSuccess(response -> log.info("Reserva de vuelo creada exitosamente"))
            .doOnError(error -> log.error("Error al crear reserva de vuelo: {}", error.getMessage()))
            .onErrorResume(WebClientResponseException.class, e -> {
              throw errorHandler.handleAmadeusError(e);
            });
  }

  /**
   * Obtiene los detalles de una reserva de vuelo existente.
   *
   * @param flightOrderId ID de la reserva en Amadeus
   * @param token token de autenticación
   * @return detalles de la reserva
   */
  public Mono<Object> getFlightOrder(String flightOrderId, String token) {
    log.info("Obteniendo detalles de reserva de vuelo: {}", flightOrderId);

    String uri = amadeusConfig.getBaseUrl() + FLIGHT_ORDERS_URL + "/" + flightOrderId;

    return webClient.get()
            .uri(uri)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .retrieve()
            .bodyToMono(Object.class)
            .doOnSuccess(response -> log.info("Detalles de reserva obtenidos"))
            .doOnError(error -> log.error("Error al obtener detalles de reserva: {}", error.getMessage()))
            .onErrorResume(WebClientResponseException.class, e -> {
              throw errorHandler.handleAmadeusError(e);
            });
  }

  /**
   * Cancela una reserva de vuelo existente.
   *
   * @param flightOrderId ID de la reserva en Amadeus
   * @param token token de autenticación
   * @return resultado de la cancelación
   */
  public Mono<Object> cancelFlightOrder(String flightOrderId, String token) {
    log.info("Cancelando reserva de vuelo: {}", flightOrderId);

    String uri = amadeusConfig.getBaseUrl() + FLIGHT_ORDERS_URL + "/" + flightOrderId;

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

  /**
   * Formatea la solicitud para crear una reserva según el formato de Amadeus.
   *
   * @param bookingRequest datos de la reserva
   * @return objeto formateado para la API de Amadeus
   */
  private Object formatCreateFlightOrderRequest(CreateFlightBookingRequestDto bookingRequest) {
    // En un caso real, convertiríamos el DTO a la estructura requerida por Amadeus
    // Por ahora, retornamos un objeto simplificado para la simulación
    return java.util.Map.of(
            "data", java.util.Map.of(
                    "type", "flight-order",
                    "flightOffers", java.util.List.of(bookingRequest.getFlightOffer()),
                    "travelers", bookingRequest.getTravelers()
            )
    );
  }

  /**
   * Verifica la disponibilidad y precio de una oferta de vuelo.
   *
   * @param flightOfferData datos de la oferta a verificar
   * @return objeto con la oferta verificada
   */
  public Mono<Object> verifyFlightOfferPrice(Object flightOfferData) {
    log.info("Verificando precio y disponibilidad de oferta de vuelo");

    // Obtener token de autenticación
    return getAmadeusToken()
            .flatMap(token -> {
              // Formatear la solicitud para el endpoint de precios
              Object requestBody = formatPricingRequest(flightOfferData);

              // Realizar la verificación de precio
              return verifyFlightOfferPrice(requestBody, token);
            });
  }

  /**
   * Crea una reserva de vuelo.
   *
   * @param bookingRequest datos de la reserva
   * @return respuesta de la creación
   */
  public Mono<Object> createFlightOrder(CreateFlightBookingRequestDto bookingRequest) {
    log.info("Creando reserva de vuelo");

    // Obtener token de autenticación
    return getAmadeusToken()
            .flatMap(token -> createFlightOrder(bookingRequest, token));
  }

  /**
   * Obtiene los detalles de una reserva de vuelo.
   *
   * @param flightOrderId ID de la reserva
   * @return detalles de la reserva
   */
  public Mono<Object> getFlightOrder(String flightOrderId) {
    log.info("Obteniendo detalles de reserva: {}", flightOrderId);

    // Obtener token de autenticación
    return getAmadeusToken()
            .flatMap(token -> getFlightOrder(flightOrderId, token));
  }

  /**
   * Cancela una reserva de vuelo.
   *
   * @param flightOrderId ID de la reserva
   * @return resultado de la cancelación
   */
  public Mono<Object> cancelFlightOrder(String flightOrderId) {
    log.info("Cancelando reserva: {}", flightOrderId);

    // Obtener token de autenticación
    return getAmadeusToken()
            .flatMap(token -> cancelFlightOrder(flightOrderId, token));
  }

  /**
   * Formatea la solicitud para verificación de precios.
   *
   * @param flightOfferData datos de la oferta
   * @return objeto formateado para la API de Amadeus
   */
  private Object formatPricingRequest(Object flightOfferData) {
    // En un caso real, convertiríamos el DTO a la estructura requerida por Amadeus
    // Por ahora, retornamos un objeto simplificado para la simulación
    return java.util.Map.of(
            "data", java.util.Map.of(
                    "type", "flight-offers-pricing",
                    "flightOffers", java.util.List.of(flightOfferData)
            )
    );
  }

  /**
   * Obtiene un token de autenticación de Amadeus.
   *
   * @return token de autenticación
   */
  private Mono<String> getAmadeusToken() {
    return getAmadeusTokenFromTokenClient()
            .map(tokenResponse -> tokenResponse.getAccessToken());
  }

  /**
   * Obtiene un token de autenticación de Amadeus desde el servicio de tokens.
   *
   * @return respuesta con el token
   */
  private Mono<masera.deviajebookingsandpayments.dtos.AmadeusTokenResponse> getAmadeusTokenFromTokenClient() {
    return webClient.post()
            .uri(amadeusConfig.getBaseUrl() + amadeusConfig.getTokenUrl())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(org.springframework.web.reactive.function.BodyInserters.fromFormData("grant_type", "client_credentials")
                    .with("client_id", amadeusConfig.getApiKey())
                    .with("client_secret", amadeusConfig.getApiSecret()))
            .retrieve()
            .bodyToMono(masera.deviajebookingsandpayments.dtos.AmadeusTokenResponse.class)
            .doOnSuccess(token -> log.info("Token de Amadeus obtenido correctamente"))
            .doOnError(error -> log.error("Error al obtener token de Amadeus: {}", error.getMessage()));
  }
}
