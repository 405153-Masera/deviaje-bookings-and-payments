package masera.deviajebookingsandpayments.utils.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * DTO para representar un error de Amadeus.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AmadeusError {

  private Integer code;

  private String title;

  private String detail;

  private Integer status;
}
