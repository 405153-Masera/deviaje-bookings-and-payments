package masera.deviajebookingsandpayments.utils.dtos;

import lombok.Data;

/**
 * DTO para representar un error de Amadeus.
 */
@Data
public class AmadeusError {

  private Integer code;

  private String title;

  private String detail;

  private Integer status;
}
