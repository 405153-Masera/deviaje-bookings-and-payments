package masera.deviajebookingsandpayments.exceptions;

import lombok.Getter;

/**
 * Excepción personalizada para errores relacionados con la API de Amadeus.
 */
@Getter
public class AmadeusApiException extends  RuntimeException {

  private final int statusCode;

  /**
   * Constructor de la clase.
   *
   * @param message Mensaje de error.
   */
  public AmadeusApiException(String message) {
    super(message);
    this.statusCode = 500; // Por defecto
  }

  /**
   * Constructor de la clase.
   *
   * @param message Mensaje de error.
   * @param cause Causa de la excepción.
   */
  public AmadeusApiException(String message, Throwable cause) {
    super(message, cause);
    this.statusCode = 500; // Por defecto
  }

  /**
   * Constructor de la clase.
   *
   * @param message Mensaje de error.
   * @param statusCode Código de estado HTTP.
   */
  public AmadeusApiException(String message, int statusCode) {
    super(message);
    this.statusCode = statusCode;
  }

  /**
   * Constructor de la clase.
   *
   * @param message Mensaje de error.
   * @param cause Causa de la excepción.
   * @param statusCode Código de estado HTTP.
   */
  public AmadeusApiException(String message, Throwable cause, int statusCode) {
    super(message, cause);
    this.statusCode = statusCode;
  }
}
