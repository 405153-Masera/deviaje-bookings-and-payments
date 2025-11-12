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
   * @param statusCode Código de estado HTTP.
   */
  public AmadeusApiException(String message, int statusCode) {
    super(message);
    this.statusCode = statusCode;
  }
}
