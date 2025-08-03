package masera.deviajebookingsandpayments.clients;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import masera.deviajebookingsandpayments.configs.HotelbedsConfig;
import masera.deviajebookingsandpayments.utils.AmadeusErrorHandler;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * Cliente para consumir el microservicio de hoteles de la API de Amadeus.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HotelClient {

  private final WebClient webClient;
  private final HotelbedsConfig hotelbedsConfig;
  private final AmadeusErrorHandler errorHandler;

  private static final String CHECK_RATES_ENDPOINT = "/hotel-api/1.0/checkrates";
  private static final String BOOKING_ENDPOINT = "/hotel-api/1.0/bookings";


  /**
   * Verifica la disponibilidad y precio de una tarifa.
   *
   * @param rateKey clave de la tarifa a verificar
   * @return información actualizada de la tarifa
   */
  public Mono<Object> checkRates(String rateKey) {
    log.info("Verificando tarifa con clave: {}", rateKey);

    // Crear el cuerpo de la solicitud
    Map<String, Object> request = new java.util.HashMap<>();
    java.util.List<Map<String, String>> rooms = new java.util.ArrayList<>();

    // Crear un objeto para cada rateKey
    Map<String, String> room = new java.util.HashMap<>();
    room.put("rateKey", rateKey);
    rooms.add(room);

    request.put("rooms", rooms);

    return webClient
            .post()
            .uri(hotelbedsConfig.getBaseUrl() + CHECK_RATES_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON)
            .headers(this::addHotelbedsHeaders)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Object.class)
            .doOnSuccess(response -> log.info("Verificación de tarifa completada exitosamente"))
            .doOnError(error -> {
              if (error instanceof WebClientResponseException webError) {
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
   * Crea una reserva de hotel.
   *
   * @param bookingRequest solicitud de reserva
   * @return confirmación de la reserva
   */
  public Mono<Object> createBooking(Object bookingRequest) {
    log.info("Creando reserva de hotel");

    return webClient
            .post()
            .uri(hotelbedsConfig.getBaseUrl() + BOOKING_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON)
            .headers(this::addHotelbedsHeaders)
            .bodyValue(bookingRequest)
            .retrieve()
            .bodyToMono(Object.class)
            .doOnSuccess(response -> log.info("Reserva creada exitosamente"))
            .doOnError(error -> {
              if (error instanceof WebClientResponseException webError) {
                log.error("Error al crear reserva de hotel - Status: {}, Body: {}",
                        webError.getStatusCode(), webError.getResponseBodyAsString());
              } else {
                log.error("Error al crear reserva de vuelo: {}", error.getMessage());
              }
            });
            //.onErrorResume(throwable -> Mono.empty());
  }


  /**
   * Obtiene los detalles de una reserva.
   *
   * @param bookingId ID de la reserva en HotelBeds
   * @return detalles de la reserva
   */
  public Mono<Object> getBookingDetails(String bookingId) {
    log.info("Obteniendo detalles de reserva: {}", bookingId);

    return webClient
            .get()
            .uri(hotelbedsConfig.getBaseUrl() + BOOKING_ENDPOINT + "/" + bookingId)
            .headers(this::addHotelbedsHeaders)
            .retrieve()
            .bodyToMono(Object.class)
            .doOnSuccess(response -> log.info("Detalles de reserva obtenidos exitosamente"))
            .doOnError(error -> log.error("Error al obtener detalles de reserva: {}",
                            error.getMessage()))
            .onErrorResume(WebClientResponseException.class, this::apply);
  }

  /**
   * Cancela una reserva de hotel.
   *
   * @param bookingId ID de la reserva en HotelBeds
   * @return confirmación de la cancelación
   */
  public Mono<Object> cancelBooking(String bookingId) {
    log.info("Cancelando reserva: {}", bookingId);

    return webClient
            .delete()
            .uri(hotelbedsConfig.getBaseUrl() + BOOKING_ENDPOINT + "/" + bookingId)
            .headers(this::addHotelbedsHeaders)
            .retrieve()
            .bodyToMono(Object.class)
            .doOnSuccess(response -> log.info("Reserva cancelada exitosamente"))
            .doOnError(error -> log.error("Error al cancelar reserva: {}", error.getMessage()))
            .onErrorResume(WebClientResponseException.class, this::apply);
  }

  private void addHotelbedsHeaders(HttpHeaders headers) {
    long timestamp = System.currentTimeMillis() / 1000;
    String signature = DigestUtils.sha256Hex(hotelbedsConfig.getApiKey()
            + hotelbedsConfig.getApiSecret() + timestamp);

    headers.set("Api-Key", hotelbedsConfig.getApiKey());
    headers.set("X-Signature", signature);
    headers.set("Accept", "application/json");
  }

  private Mono<?> apply(WebClientResponseException e) {
    log.error("Error de respuesta de Hotelbeds: {}", e.getResponseBodyAsString());
    throw errorHandler.handleAmadeusError(e);
  }
}
