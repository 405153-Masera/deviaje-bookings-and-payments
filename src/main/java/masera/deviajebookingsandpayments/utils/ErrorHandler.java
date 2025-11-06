package masera.deviajebookingsandpayments.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import masera.deviajebookingsandpayments.exceptions.AmadeusApiException;
import masera.deviajebookingsandpayments.exceptions.HotelBedsApiException;
import masera.deviajebookingsandpayments.exceptions.MercadoPagoException;
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
   * Procesa excepciones de MercadoPago (MPApiException y MPException)
   * y las convierte en MercadoPagoException con los detalles adecuados.
   *
   * @param e La excepción original de MercadoPago
   * @return Una nueva MercadoPagoException con los detalles formateados
   */
  public MercadoPagoException handleMercadoPagoError(MPApiException e) {

    log.error("Error de MercadoPago API - Status: {}, Message: {}", e.getStatusCode(), e.getMessage());

    try {

      String errorContent = e.getApiResponse() != null ? e.getApiResponse().getContent() : null;

      if (errorContent != null && !errorContent.trim().isEmpty()) {
        JsonNode rootNode = objectMapper.readTree(errorContent);

        if (rootNode.has("cause") && rootNode.get("cause").isArray()) {
          return handleMercadoPagoCauseFormat(rootNode, e.getStatusCode());
        }

        if (rootNode.has("message") || rootNode.has("code")) {
          return handleMercadoPagoSimpleFormat(rootNode, e.getStatusCode());
        }
      }

      return createMercadoPagoGenericException(e.getMessage(), e.getStatusCode());

    } catch (JsonProcessingException ex) {
      log.error("Error al parsear respuesta de MercadoPago (no es JSON válido)", ex);
      return createMercadoPagoGenericException(e.getMessage(), e.getStatusCode());
    } catch (Exception ex) {
      log.error("Error inesperado al procesar error de MercadoPago", ex);
      return createMercadoPagoGenericException(e.getMessage(), e.getStatusCode());
    }
  }

  /**
   * Maneja errores generales de MercadoPago (MPException sin statusCode).
   *
   * @param e La excepción MPException
   * @return MercadoPagoException
   */
  public MercadoPagoException handleMercadoPagoError(MPException e) {
    log.error("Error general de MercadoPago: {}", e.getMessage());

    int statusCode = 500;
    String message = String.format(
            "— %d INTERNAL SERVER ERROR - MercadoPago Error: %s",
            statusCode,
            e.getMessage() != null ? e.getMessage() : "Error de conexión con MercadoPago"
    );
    return new MercadoPagoException(message, statusCode, e);
  }

  /**
   * Maneja el formato "cause" de errores de MercadoPago.
   * Formato: {"cause": [{"code": "INVALID_CARD", "description": "Tarjeta inválida"}]}
   *
   * @param rootNode Nodo JSON raíz
   * @param statusCode Código HTTP
   * @return MercadoPagoException con el mensaje formateado
   */
  private MercadoPagoException handleMercadoPagoCauseFormat(JsonNode rootNode, int statusCode) {
    JsonNode causeArray = rootNode.get("cause");

    if (causeArray.isArray() && !causeArray.isEmpty()) {
      JsonNode firstCause = causeArray.get(0);

      String code = firstCause.has("code")
              ? firstCause.get("code").asText() : "UNKNOWN";
      String description = firstCause.has("description")
              ? firstCause.get("description").asText() : "Sin descripción";

      String formattedMessage = String.format(
              "— %d %s - MercadoPago Error [%s]: %s",
              statusCode,
              getHttpStatusText(statusCode),
              code,
              description
      );
      return new MercadoPagoException(formattedMessage, statusCode, code);
    }

    return createMercadoPagoGenericException("Error en MercadoPago", statusCode);
  }

  /**
   * Maneja el formato simple de errores de MercadoPago.
   * Formato: {"message": "Invalid token", "error": "bad_request", "status": 400}
   *
   * @param rootNode Nodo JSON raíz
   * @param statusCode Código HTTP
   * @return MercadoPagoException con el mensaje formateado
   */
  private MercadoPagoException handleMercadoPagoSimpleFormat(JsonNode rootNode, int statusCode) {
    String message = rootNode.has("message")
            ? rootNode.get("message").asText() : null;
    String code = rootNode.has("code")
            ? rootNode.get("code").asText() : null;

    String errorCode = code != null ? code.toUpperCase() : "MERCADOPAGO_ERROR";
    String errorMessage = message != null ? message : "Error al procesar con MercadoPago";

    String formattedMessage = String.format(
            "— %d %s - MercadoPago Error [%s]: %s",
            statusCode,
            getHttpStatusText(statusCode),
            errorCode,
            errorMessage
    );

    return new MercadoPagoException(formattedMessage, statusCode, errorCode);
  }

  /**
   * Crea una excepción genérica de MercadoPago.
   *
   * @param message Mensaje de error
   * @param statusCode Código HTTP
   * @return MercadoPagoException genérica
   */
  private MercadoPagoException createMercadoPagoGenericException(String message, int statusCode) {
    String formattedMessage = String.format(
            "— %d %s - Error al procesar pago con MercadoPago: %s",
            statusCode,
            getHttpStatusText(statusCode),
            message != null ? message : "Error desconocido"
    );

    return new MercadoPagoException(formattedMessage, statusCode);
  }

  /**
   * Obtiene el texto del status HTTP.
   *
   * @param statusCode Código HTTP
   * @return Texto del status
   */
  private String getHttpStatusText(int statusCode) {
    return switch (statusCode) {
      case 400 -> "BAD REQUEST";
      case 401 -> "UNAUTHORIZED";
      case 402 -> "PAYMENT REQUIRED";
      case 403 -> "FORBIDDEN";
      case 404 -> "NOT FOUND";
      case 422 -> "UNPROCESSABLE ENTITY";
      case 500 -> "INTERNAL SERVER ERROR";
      case 503 -> "SERVICE UNAVAILABLE";
      default -> "ERROR";
    };
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
    return new HotelBedsApiException(formattedMessage, statusCode, code);
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
