package masera.deviajebookingsandpayments.dtos.bookings.flights;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa los detalles del precio.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceDetailDto {

  private String currency;

  private String total;

  private String base;

  private List<TaxDto> taxes;

  private String refundableTaxes;
}
