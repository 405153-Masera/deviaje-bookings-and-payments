package masera.deviajebookingsandpayments.exceptions;

import lombok.Getter;

/**
 * Excepción personalizada para errores de la API de HotelBeds.
 * Esta excepción se lanza cuando ocurre un error al comunicarse con la API de HotelBeds.
 */
@Getter
public class HotelBedsApiException extends RuntimeException {

  private final int statusCode;
  private String internalCode;

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
   * Constructor de la clase.
   *
   * @param message Mensaje de error.
   * @param statusCode Código de estado HTTP.
   * @param internalCode Código interno de hotelbeds
   */
  public HotelBedsApiException(String message, int statusCode,  String internalCode) {
    super(message);
    this.statusCode = statusCode;
    this.internalCode = internalCode;
  }
}
