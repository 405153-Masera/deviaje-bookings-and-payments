package masera.deviajebookingsandpayments.dtos.bookings.travelers;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelerDto {
  private String id;
  private String dateOfBirth;
  private NameDto name;
  private String gender;
  private ContactDto contact;
  private List<DocumentDto> documents;
}
