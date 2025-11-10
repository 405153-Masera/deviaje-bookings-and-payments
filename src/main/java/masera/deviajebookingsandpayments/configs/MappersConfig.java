package masera.deviajebookingsandpayments.configs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.modelmapper.Conditions;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ModelMapper y ObjectMapper para la aplicación.
 * Esta clase se encarga de la configuración de ModelMapper y ObjectMapper
 */
@Configuration
public class MappersConfig {

  /**
   * El ModelMapper por default.
   *
   * @return el ModelMapper por default.
   */
  @Bean
  public ModelMapper modelMapper() {
    return new ModelMapper();
  }

  /**
   * El ModelMapper que se utiliza para actualizar objetos.
   *
   * @return el ModelMapper para actualizaciones.
   */
  @Bean("mergerMapper")
  public ModelMapper mergerMapper() {
    ModelMapper mapper = new ModelMapper();
    mapper.getConfiguration()
            .setPropertyCondition(Conditions.isNotNull());
    return mapper;
  }

  /**
   * El ObjectMapper que sirve para mapear objetos a JSON y viceversa.
   *
   * @return el ObjectMapper creado.
   */
  @Bean
  public ObjectMapper objectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);
    return objectMapper;
  }
}
