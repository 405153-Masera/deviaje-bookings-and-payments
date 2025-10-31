package masera.deviajebookingsandpayments.utils.dtos;

import lombok.Data;

import java.util.List;

/**
 * DTO para manejar respuestas de error de Amadeus.
 */
@Data
public class AmadeusErrorResponse {

  private List<AmadeusError> errors;
}
