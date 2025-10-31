package masera.deviajebookingsandpayments.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import masera.deviajebookingsandpayments.exceptions.AmadeusApiException;
import masera.deviajebookingsandpayments.exceptions.HotelBedsApiException;
import masera.deviajebookingsandpayments.utils.dtos.AmadeusError;
import masera.deviajebookingsandpayments.utils.dtos.AmadeusErrorResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;

/**
 * Clase para manejar errores de Amadeus.
 * Esta clase se encarga de manejar los errores que pueden
 * ocurrir al interactuar con la API de Amadeus.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ErrorHandler {

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
      AmadeusErrorResponse errorBody = objectMapper.readValue(
              e.getResponseBodyAsString(), AmadeusErrorResponse.class);

      if (errorBody.getErrors() != null) {
        List<AmadeusError> errors = errorBody.getErrors();

        if (!errors.isEmpty()) {
          AmadeusError amadeusError = errors.getFirst();

          int status = amadeusError.getStatus() != null
                  ? amadeusError.getStatus() : e.getStatusCode().value();
          String message = getString(amadeusError);

          return new AmadeusApiException(message, status);
        }
      }
    } catch (JsonProcessingException ex) {
      log.error("Error al parsear respuesta de error de Amadeus", ex);
    }
    
    return new AmadeusApiException(
            "Error al comunicarse con Amadeus: " + e.getStatusText(),
            e.getStatusCode().value());
  }

  /**
   * Procesa una excepción WebClientResponseException de HotelBeds
   * y la convierte en una HotelBedsApiException con los detalles adecuados.
   *
   * @param e La excepción original de WebClient
   * @return Una nueva HotelBedsApiException con los detalles formateados
   */
  public HotelBedsApiException handleHotelBedsError(WebClientResponseException e) {

    String responseBody = e.getResponseBodyAsString();
    int statusCode = e.getStatusCode().value();

    log.error("Error de HotelBeds API - Status: {}, Body: {}", statusCode, responseBody);

    if (responseBody.trim().isEmpty()) {
      return createGenericException(statusCode, e.getStatusText());
    }

    try {

      JsonNode rootNode = objectMapper.readTree(responseBody);

      if (!rootNode.has("error")) {
        return createGenericException(statusCode, e.getStatusText());
      }

      JsonNode errorNode = rootNode.get("error");

      if (errorNode.isObject() && errorNode.has("code") && errorNode.has("message")) {
        return handleCompleteErrorFormat(errorNode, statusCode, e.getStatusText());
      }

      if (errorNode.isTextual()) {
        return handleSimpleErrorFormat(errorNode.asText(), statusCode, e.getStatusText());
      }

      log.warn("Formato de error desconocido de HotelBeds: {}", responseBody);
      return createGenericException(statusCode, responseBody);

    } catch (JsonProcessingException ex) {
      log.error("Error al parsear respuesta de "
              + "HotelBeds (no es JSON válido): {}", responseBody, ex);
      return createGenericException(statusCode, responseBody);

    } catch (Exception ex) {
      log.error("Error inesperado al procesar error de HotelBeds", ex);
      return createGenericException(statusCode, e.getStatusText());
    }
  }

  /**
   * Maneja el formato completo de error de HotelBeds.
   * Formato: {"error": {"code": "INVALID_DATA", "message": "..."}}
   *
   * @param errorNode Nodo JSON que contiene el error
   * @param statusCode Código HTTP
   * @param statusText Texto del estado HTTP
   * @return HotelBedsApiException con el mensaje formateado
   */
  private HotelBedsApiException handleCompleteErrorFormat(
          JsonNode errorNode, int statusCode, String statusText) {
    String code = errorNode.has("code")
            ? errorNode.get("code").asText() : "UNKNOWN";
    String message = errorNode.has("message")
            ? errorNode.get("message").asText() : "Sin descripción";

    String formattedMessage = String.format(
            "— %d %s - HotelBeds API Error [%s]: %s",
            statusCode,
            statusText.toUpperCase(),
            code,
            message
    );
    return new HotelBedsApiException(formattedMessage, statusCode);
  }

  /**
   * Maneja el formato simple para el error de HotelBeds.
   * Formato: {"error": "Access to this API has been disallowed"}
   *
   * @param errorMessage Mensaje de error
   * @param statusCode Código HTTP
   * @param statusText Texto del estado HTTP
   * @return HotelBedsApiException con el mensaje formateado
   */
  private HotelBedsApiException handleSimpleErrorFormat(
          String errorMessage, int statusCode, String statusText) {

    String formattedMessage = String.format(
            "— %d %s - HotelBeds API Error: %s",
            statusCode,
            statusText.toUpperCase(),
            errorMessage
    );

    return new HotelBedsApiException(formattedMessage, statusCode);
  }

  /**
   * Crea una excepción genérica cuando no se puede parsear el error.
   *
   * @param statusCode Código HTTP
   * @param statusText Texto del estado HTTP
   * @return HotelBedsApiException genérica
   */
  private HotelBedsApiException createGenericException(int statusCode, String statusText) {
    String message = String.format(
            "— %d %s - Error al comunicarse con HotelBeds API",
            statusCode,
            statusText.toUpperCase()
    );
    return new HotelBedsApiException(message, statusCode);
  }

  /**
   * Construye el mensaje de error de Amadeus.
   *
   * @param amadeusError Error de Amadeus
   * @return Mensaje formateado
   */
  private static String getString(AmadeusError amadeusError) {
    String detail = amadeusError.getDetail() != null
            ? amadeusError.getDetail() : "Error desconocido";
    int code = amadeusError.getCode() != null
            ? amadeusError.getCode() : 0;
    String title = amadeusError.getTitle() != null
            ? amadeusError.getTitle() : "";

    String message = detail;
    if (!title.isEmpty()) {
      message = title + ": " + message;
    }
    if (code != 0) {
      message = "Error " + code + " - " + message;
    }
    return message;
  }
}
