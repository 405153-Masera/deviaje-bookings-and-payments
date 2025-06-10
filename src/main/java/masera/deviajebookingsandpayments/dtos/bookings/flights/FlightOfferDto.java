package masera.deviajebookingsandpayments.dtos.bookings.flights;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO unificado para FlightOffer (compatible con GET y VERIFY).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlightOfferDto {

  private String type = "flight-offer";
  private String id;
  private String source = "GDS"; // Agregado del GET
  private Boolean instantTicketingRequired;
  private Boolean nonHomogeneous;
  private Boolean oneWay; // Agregado del GET
  private Boolean isUpsellOffer; // Agregado del GET
  private Boolean paymentCardRequired;
  private String lastTicketingDate;
  private String lastTicketingDateTime; // Agregado del GET
  private Integer numberOfBookableSeats; // Agregado del GET
  private List<ItineraryDto> itineraries;
  private PriceDto price;
  private PricingOptionsDto pricingOptions; // Agregado del GET
  private List<String> validatingAirlineCodes;
  private List<TravelerPricingDto> travelerPricings;
}

