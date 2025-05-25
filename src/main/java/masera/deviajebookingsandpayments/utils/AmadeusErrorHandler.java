package masera.deviajebookingsandpayments.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import masera.deviajebookingsandpayments.exceptions.AmadeusApiException;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;


/**
 * Clase para manejar errores de Amadeus.
 * Esta clase se encarga de manejar los errores que pueden
 * ocurrir al interactuar con la API de Amadeus.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AmadeusErrorHandler {

  private final ObjectMapper objectMapper;

  /**
   * Procesa una excepción WebClientResponseException de Amadeus
   * y la convierte en una AmadeusApiException con los detalles adecuados.
   *
   * @param e La excepción original
   * @return Una nueva AmadeusApiException con los detalles de error de Amadeus
   */
  public AmadeusApiException handleAmadeusError(WebClientResponseException e) {
    try {
      // Convierte la respuesta de error a un Map
      Map<String, Object> errorBody = objectMapper.readValue(
              e.getResponseBodyAsString(), new TypeReference<>() {});

      // Extrae los detalles del error
      if (errorBody.containsKey("errors") && errorBody.get("errors") instanceof List) {
        List<Map<String, Object>> errors = (List<Map<String, Object>>) errorBody.get("errors");
        if (!errors.isEmpty()) {
          Map<String, Object> firstError = errors.getFirst();

          // Obtiene los campos que nos interesan
          int status = firstError.containsKey("status")
                  ? Integer.parseInt(firstError.get("status")
                  .toString()) : e.getStatusCode().value();
          String detail = firstError.containsKey("detail")
                  ? firstError.get("detail").toString() : "Error desconocido";
          String code = firstError.containsKey("code")
                  ? firstError.get("code").toString() : "";
          String title = firstError.containsKey("title")
                  ? firstError.get("title").toString() : "";

          // Construye un mensaje más informativo si está disponible
          String message = detail;
          if (!title.isEmpty()) {
            message = title + ": " + message;
          }
          if (!code.isEmpty()) {
            message = "Error " + code + " - " + message;
          }

          return new AmadeusApiException(message, status);
        }
      }
    } catch (JsonProcessingException ex) {
      log.error("Error al parsear respuesta de error de Amadeus", ex);
    }

    // Si no pudimos parsear el error, devolvemos una excepción genérica
    return new AmadeusApiException(
            "Error al comunicarse con Amadeus: " + e.getStatusText(),
            e.getStatusCode().value());
  }
}
