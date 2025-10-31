package masera.deviajebookingsandpayments.exceptions;

import lombok.Getter;

/**
 * Excepción personalizada para errores de la API de HotelBeds.
 * Esta excepción se lanza cuando ocurre un error al comunicarse con la API de HotelBeds.
 */
@Getter
public class HotelBedsApiException extends RuntimeException {

  private final int statusCode;

  /**
   * Constructor de la excepción.
   *
   * @param message    Mensaje de error
   * @param statusCode Código de estado HTTP
   */
  public HotelBedsApiException(String message, int statusCode) {
    super(message);
    this.statusCode = statusCode;
  }

  /**
   * Constructor con causa.
   *
   * @param message    Mensaje de error
   * @param statusCode Código de estado HTTP
   * @param cause      Causa de la excepción
   */
  public HotelBedsApiException(String message, int statusCode, Throwable cause) {
    super(message, cause);
    this.statusCode = statusCode;
  }
}
