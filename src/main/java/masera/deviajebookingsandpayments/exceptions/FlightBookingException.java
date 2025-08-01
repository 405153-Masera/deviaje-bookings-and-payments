package masera.deviajebookingsandpayments.exceptions;

public class FlightBookingException extends RuntimeException {
  /**
   * Constructor con mensaje
   */
  public FlightBookingException(String message) {
    super(message);
  }

  /**
   * Constructor con mensaje y causa
   */
  public FlightBookingException(String message, Throwable cause) {
    super(message, cause);
  }
}
