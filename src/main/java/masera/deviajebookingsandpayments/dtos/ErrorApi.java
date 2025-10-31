package masera.deviajebookingsandpayments.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Error API DTO class.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ErrorApi {

  /**
   * Tiempo cuando ocurrió el error.
   */
  private String timestamp;

  /**
   * Código de error.
   */
  private Integer status;

  /**
   * Nombre del código de error.
   */
  private String error;

  /**
   * Descripción del código de error.
   */
  private String message;

  private String source;
}
