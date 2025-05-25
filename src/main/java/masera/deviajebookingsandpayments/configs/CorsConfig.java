package masera.deviajebookingsandpayments.configs;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración de CORS para la aplicación.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

  /**
   * Clase principal de la configuración de Cors.
   *
   * @param registry registro del Cors.
   */
  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**")
            .allowedOrigins("*")
            .allowedMethods("GET", "POST", "PUT", "DELETE")
            .allowedHeaders("*");
  }
}
