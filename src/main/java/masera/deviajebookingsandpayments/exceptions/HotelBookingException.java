package masera.deviajebookingsandpayments.exceptions;

public class HotelBookingException extends RuntimeException {

  public HotelBookingException(String message) {
    super(message);
  }

  public HotelBookingException(String message, Throwable cause) {
    super(message, cause);
  }
}
