package masera.deviajebookingsandpayments.exceptions;

import lombok.Getter;

/**
 * Excepción personalizada para errores de MercadoPago.
 * Esta excepción se lanza cuando ocurre un error al procesar pagos con MercadoPago.
 */
@Getter
public class MercadoPagoException extends RuntimeException {

  private final int statusCode;

  /**
   * Constructor de la excepción.
   *
   * @param message    Mensaje de error
   * @param statusCode Código de estado HTTP
   */
  public MercadoPagoException(String message, int statusCode) {
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
  public MercadoPagoException(String message, int statusCode, Throwable cause) {
    super(message, cause);
    this.statusCode = statusCode;
  }
}
