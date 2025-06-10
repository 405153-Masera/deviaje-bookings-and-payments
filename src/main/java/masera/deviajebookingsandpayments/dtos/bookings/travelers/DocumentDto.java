package masera.deviajebookingsandpayments.dtos.bookings.travelers;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentDto {
  private String documentType;
  private String birthPlace;
  private String issuanceLocation;
  private String issuanceDate;
  private String number;
  private String expiryDate;
  private String issuanceCountry;
  private String validityCountry;
  private String nationality;
  private Boolean holder;
}
