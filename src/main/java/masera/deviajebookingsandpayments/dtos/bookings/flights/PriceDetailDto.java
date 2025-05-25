package masera.deviajebookingsandpayments.dtos.bookings.flights;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
