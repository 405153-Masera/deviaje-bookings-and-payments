package masera.deviajebookingsandpayments.dtos.bookings.travelers;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa un pasajero.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelerDto {

  private String id;

  private String dateOfBirth;

  private NameDto name;

  private String gender;

  private String travelerType; // ADULT, CHILD, INFANT

  private String associatedAdultId;

  private ContactDto contact;

  private List<DocumentDto> documents;
}
