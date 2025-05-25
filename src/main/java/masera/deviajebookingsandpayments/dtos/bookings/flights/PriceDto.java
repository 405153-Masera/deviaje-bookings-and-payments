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
public class PriceDto {
  private String currency;
  private String total;
  private String base;
  private List<FeeDto> fees;
  private String grandTotal;
  private String billingCurrency;
  private List<AdditionalServiceDto> additionalServices;
}
