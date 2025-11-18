package masera.deviajebookingsandpayments.controllers;

import jakarta.persistence.EntityNotFoundException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import masera.deviajebookingsandpayments.dtos.ErrorApi;
import masera.deviajebookingsandpayments.exceptions.AmadeusApiException;
import masera.deviajebookingsandpayments.exceptions.HotelBedsApiException;
import masera.deviajebookingsandpayments.exceptions.MercadoPagoException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Clase para el manejo global de excepciones.
 */
@RestControllerAdvice
@Slf4j
public class ControllerException {

  private static final DateTimeFormatter TIMESTAMP_FORMATTER =
          DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSS");

  /**
   * Manejador para errores de la API de Amadeus.
   *
   * @param e excepción de la API de Amadeus.
   * @return ResponseEntity con el error.
   */
  @ExceptionHandler(AmadeusApiException.class)
  public ResponseEntity<ErrorApi> handleAmadeusApiException(AmadeusApiException e) {
    log.error("Error en Amadeus API: {} - Status: {}", e.getMessage(), e.getStatusCode());
    HttpStatus status = HttpStatus.valueOf(e.getStatusCode());
    ErrorApi error = buildError(e.getMessage(), status, "AMADEUS");
    error.setCodeErrorApi(String.valueOf(e.getInternalCode()));
    return ResponseEntity.status(status).body(error);
  }

  /**
   * Manejador para errores de la API de Hotelbeds.
   *
   * @param e excepción de la API de Hotelbeds.
   * @return ResponseEntity con el error.
   */
  @ExceptionHandler(HotelBedsApiException.class)
  public ResponseEntity<ErrorApi> handleHotelBedsApiException(HotelBedsApiException e) {
    log.error("Error en HotelBeds API: {} - Status: {}", e.getMessage(), e.getStatusCode());
    HttpStatus status = HttpStatus.valueOf(e.getStatusCode());
    ErrorApi error = buildError(e.getMessage(), status, "HOTELBEDS");
    error.setCodeErrorApi(e.getInternalCode());
    return ResponseEntity.status(status).body(error);
  }

  /**
   * Maneja errores de MercadoPago.
   *
   * @param e excepción de MercadoPago
   * @return ResponseEntity con el error
   */
  @ExceptionHandler(MercadoPagoException.class)
  public ResponseEntity<ErrorApi> handleMercadoPagoException(MercadoPagoException e) {
    log.error("Error en MercadoPago: {} - Status: {}", e.getMessage(), e.getStatusCode());
    HttpStatus status = HttpStatus.valueOf(e.getStatusCode());
    ErrorApi error = buildError(e.getMessage(), status, "MERCADO_PAGO");

    if (e.getInternalCode() != null) {
      error.setCodeErrorApi(e.getInternalCode());
    }
    return ResponseEntity.status(status).body(error);
  }

  /**
   * Maneja errores HTTP genéricos del WebClient (cuando no se capturaron específicamente).
   *
   * @param e excepción de respuesta HTTP
   * @return ResponseEntity con el error
   */
  @ExceptionHandler(WebClientResponseException.class)
  public ResponseEntity<ErrorApi> handleWebClientResponseException(
          WebClientResponseException e
  ) {
    log.error("Error en API externa: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
    HttpStatus status = HttpStatus.valueOf(e.getStatusCode().value());
    ErrorApi error = this.buildError(
            "Error al comunicarse con un servicio externo", status, "EXTERNAL_API");
    return ResponseEntity.status(e.getStatusCode()).body(error);
  }

  /**
   * Maneja errores de timeout y conexión con API externas.
   *
   * @param ex Excepción de petición HTTP
   * @return ResponseEntity con el error formateado
   */
  @ExceptionHandler(WebClientRequestException.class)
  public ResponseEntity<ErrorApi> handleWebClientRequestException(WebClientRequestException ex) {
    log.error("Error de conexión con API externa: {}", ex.getMessage());
    ErrorApi error = this.buildError(
            "No se pudo conectar con el servicio externo",
            HttpStatus.SERVICE_UNAVAILABLE, "EXTERNAL_API");
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
  }

  /**
   * Maneja errores de acceso a la base de datos.
   *
   * @param e excepción de acceso a datos
   * @return ResponseEntity con el error
   */
  @ExceptionHandler(DataAccessException.class)
  public ResponseEntity<ErrorApi> handleDatabaseError(
          DataAccessException e
  ) {
    log.error("Error de base de datos: {}", e.getMessage(), e);
    ErrorApi error = this.buildError(
            "Error al acceder a la base de datos", HttpStatus.INTERNAL_SERVER_ERROR, "BACKEND");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }

  /**
   * Maneja errores cuando no se encuentra una entidad en la BD.
   *
   * @param e excepción de entidad no encontrada
   * @return ResponseEntity con el error
   */
  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<ErrorApi> handleEntityNotFound(
          EntityNotFoundException e
  ) {
    log.error("Entidad no encontrada: {}", e.getMessage());

    ErrorApi error = buildError(
            e.getMessage(),
            HttpStatus.NOT_FOUND,
            "BACKEND"
    );

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
  }

  /**
   * Manejador para errores de validación (400).
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidationExceptions(
          MethodArgumentNotValidException e) {
    Map<String, String> fieldErrors = new HashMap<>();
    e.getBindingResult().getAllErrors().forEach((error) -> {
      String fieldName = ((FieldError) error).getField();
      String errorMessage = error.getDefaultMessage();
      fieldErrors.put(fieldName, errorMessage);
    });

    Map<String, Object> response = new HashMap<>();
    response.put("timestamp", String.valueOf(Timestamp.from(ZonedDateTime.now().toInstant())));
    response.put("status", HttpStatus.BAD_REQUEST.value());
    response.put("error", HttpStatus.BAD_REQUEST.getReasonPhrase());
    response.put("source", "BACKEND");
    response.put("validationErrors", fieldErrors);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  /**
   * Manejador para excepciones HTTP específicas.
   */
  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ErrorApi> handleError(ResponseStatusException e) {
    ErrorApi error = buildError(
            e.getReason(), HttpStatus.valueOf(e.getStatusCode().value()), "BACKEND");
    return ResponseEntity.status(e.getStatusCode()).body(error);
  }

  /**
   * Manejador global para cualquier otra excepción (500).
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorApi> handleError(Exception e) {
    ErrorApi error = buildError(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, "BACKEND");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }

  /**
   * Metodo para construir un objeto error API.
   *
   * @param message mensaje de error a arrojar.
   * @param status  código de HTTP.
   * @return un ErrorApi.
   */
  private ErrorApi buildError(String message, HttpStatus status, String source) {
    return ErrorApi.builder()
            .timestamp(getCurrentTimestamp())
            .error(status.getReasonPhrase())
            .status(status.value())
            .source(source)
            .message(message)
            .build();
  }

  /**
   * Obtiene el timestamp actual en formato consistente.
   *
   * @return timestamp formateado
   */
  private String getCurrentTimestamp() {
    return LocalDateTime.now().format(TIMESTAMP_FORMATTER);
  }
}
