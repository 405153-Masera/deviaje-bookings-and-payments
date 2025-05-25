package masera.deviajebookingsandpayments.dtos.bookings.flights;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO unificado para FlightOffer (compatible con GET y VERIFY).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlightOfferDto {

  private String id;
  private String source;
  private Boolean instantTicketingRequired;
  private Boolean nonHomogeneous;
  private Boolean oneWay; // Agregado del GET
  private Boolean isUpsellOffer; // Agregado del GET
  private Boolean paymentCardRequired;
  private LocalDate lastTicketingDate;
  private String lastTicketingDateTime; // Agregado del GET
  private Integer numberOfBookableSeats; // Agregado del GET
  private List<ItineraryDto> itineraries;
  private PriceDto price;
  private PricingOptionsDto pricingOptions; // Agregado del GET
  private List<String> validatingAirlineCodes;
  private List<TravelerPricingDto> travelerPricings;
}

