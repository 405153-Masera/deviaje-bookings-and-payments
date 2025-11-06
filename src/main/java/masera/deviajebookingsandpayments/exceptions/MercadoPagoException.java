package masera.deviajebookingsandpayments.exceptions;

import lombok.Getter;

/**
 * Excepción personalizada para errores de MercadoPago.
 * Esta excepción se lanza cuando ocurre un error al procesar pagos con MercadoPago.
 */
@Getter
public class MercadoPagoException extends RuntimeException {

  private final int statusCode;
  private String internalCode;

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
   * Constructor con código interno de MercadoPago.
   *
   * @param message      Mensaje de error
   * @param statusCode   Código de estado HTTP
   * @param internalCode Código interno de error de MercadoPago
   */
  public MercadoPagoException(String message, int statusCode, String internalCode) {
    super(message);
    this.statusCode = statusCode;
    this.internalCode = internalCode;
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
