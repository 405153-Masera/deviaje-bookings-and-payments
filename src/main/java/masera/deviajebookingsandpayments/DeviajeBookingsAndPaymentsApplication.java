package masera.deviajebookingsandpayments;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Puto de inicio de la aplicación.
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class DeviajeBookingsAndPaymentsApplication {

  /**
   * Metodo que da el inicio de la aplicación.
   *
   * @param args un string
   */
  public static void main(String[] args) {
    SpringApplication.run(DeviajeBookingsAndPaymentsApplication.class, args);
  }
}
