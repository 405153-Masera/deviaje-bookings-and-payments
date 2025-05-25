package masera.deviajebookingsandpayments.clients;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import masera.deviajebookingsandpayments.configs.AmadeusConfig;
import masera.deviajebookingsandpayments.dtos.AmadeusTokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Cliente para gestionar la autenticación con la API de Amadeus.
 */
@Component
@RequiredArgsConstructor
@Slf4j // añade un logger para registrar eventos o errores
public class AmadeusAuthClient {

  private final WebClient webClient;

  private final AmadeusConfig amadeusConfig;

  @Value("${amadeus.api.key}")
  private String apiKey;

  @Value("${amadeus.api.secret}")
  private String apiSecret;

  @Value("${amadeus.api.token-url}")
  private String tokenUrl;


  /**
   * Metodo que realiza una solicitud POST a la API de Amadeus
   * para obtener un token de autenticación.
   *
   * @return Mono AmadeusTokenResponse que representa una operación asíncrona
   *      que emite un solo objeto o un error.
   */
  public Mono<AmadeusTokenResponse> getAmadeusToken() {

    log.info("Solicitando el token de autenticación a Amadeus");

    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("grant_type", "client_credentials");
    formData.add("client_id", apiKey);
    formData.add("client_secret", apiSecret);

    return webClient.post()
            .uri(amadeusConfig.getBaseUrl() + tokenUrl)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED) //establecemos el tipo de contenido
            .body(BodyInserters.fromFormData(formData))
            .retrieve() //realizamos la solicitud
            .bodyToMono(AmadeusTokenResponse.class) // procesa la respuesta
            .doOnSuccess(token -> log.info("Token obtenido correctamente"))
            .doOnError(error -> log.error("Error al obtener el token de autenticación: "
                    + "{}", error.getMessage()));
  }
}
