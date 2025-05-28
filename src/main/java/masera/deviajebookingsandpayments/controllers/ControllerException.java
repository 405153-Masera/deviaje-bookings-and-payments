package masera.deviajebookingsandpayments.controllers;

import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import masera.deviajebookingsandpayments.dtos.ErrorApi;
import masera.deviajebookingsandpayments.exceptions.AmadeusApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;


/**
 * Clase para el manejo global de excepciones.
 */
@ControllerAdvice
@Data
public class ControllerException {

  /**
   * Manejador para errores de la API de Amadeus.
   *
   * @param e excepción de la API de Amadeus.
   * @return ResponseEntity con el error.
   */
  @ExceptionHandler(AmadeusApiException.class)
  public ResponseEntity<ErrorApi> handleAmadeusApiException(AmadeusApiException e) {
    HttpStatus status = HttpStatus.valueOf(e.getStatusCode());
    ErrorApi error = buildError(e.getMessage(), status);
    return ResponseEntity.status(status).body(error);
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
    response.put("validationErrors", fieldErrors);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  /**
   * Manejador para excepciones HTTP específicas.
   */
  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ErrorApi> handleError(ResponseStatusException e) {
    ErrorApi error = buildError(e.getReason(), HttpStatus.valueOf(e.getStatusCode().value()));
    return ResponseEntity.status(e.getStatusCode()).body(error);
  }

  /**
   * Manejador global para cualquier otra excepción (500).
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorApi> handleError(Exception e) {
    ErrorApi error = buildError(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }

  /**
   * Metodo para construir un objeto error API.
   *
   * @param message mensaje de error a arrojar.
   * @param status  código de HTTP.
   * @return un ErrorApi.
   */
  private ErrorApi buildError(String message, HttpStatus status) {
    return ErrorApi.builder()
            .timestamp(String.valueOf(Timestamp.from(ZonedDateTime.now().toInstant())))
            .error(status.getReasonPhrase())
            .status(status.value())
            .message(message)
            .build();
  }
}
