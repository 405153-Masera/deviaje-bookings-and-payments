package masera.deviajebookingsandpayments.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Respuesta genérica para cualquier tipo de dato.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaseResponse<T> {

  private boolean success;
  private T data;
  private String message;

  // Métodos factory para crear respuestas comunes
  public static <T> BaseResponse<T> success(T data) {
    return BaseResponse.<T>builder()
            .success(true)
            .data(data)
            .message("Operación exitosa")
            .build();
  }

  public static <T> BaseResponse<T> success(T data, String message) {
    return BaseResponse.<T>builder()
            .success(true)
            .data(data)
            .message(message)
            .build();
  }

  public static <T> BaseResponse<T> error(String message) {
    return BaseResponse.<T>builder()
            .success(false)
            .data(null)
            .message(message)
            .build();
  }
}
