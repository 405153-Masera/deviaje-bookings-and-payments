package masera.deviajebookingsandpayments.dtos.responses;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import masera.deviajebookingsandpayments.dtos.bookings.flights.ItineraryDto;
import masera.deviajebookingsandpayments.dtos.bookings.hotels.HotelBookingApi;
import masera.deviajebookingsandpayments.dtos.bookings.travelers.TravelerDto;

/**
 * DTO para respuesta con detalles completos de una reserva.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingDetailsResponseDto {

  private Long id;

  private String bookingReference;

  private String externalReference;

  private Integer clientId;

  private Integer agentId;

  private String status;

  private String type;

  private BigDecimal totalAmount;

  private BigDecimal commission;

  private BigDecimal discount;

  private BigDecimal taxes;

  private String currency;

  private String holderName;

  private String countryCallingCode;

  private String phone;

  private String email;

  private LocalDateTime createdDatetime;

  private FlightBookingDetails flightDetails;

  private HotelBookingDetails hotelDetails;

  private List<PaymentInfo> payments;

  /**
   * Detalles de reserva de vuelo.
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class FlightBookingDetails {

    private String externalId;

    private String origin;

    private String destination;

    private String carrier;

    private String departureDate;

    private String arrivalDate;

    private Integer adults;

    private Integer children;

    private Integer infants;

    private BigDecimal totalPrice;

    private String currency;

    private List<ItineraryDto> itineraries;

    private List<TravelerDto> travelers;
  }

  /**
   * Detalles de reserva de hotel.
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class HotelBookingDetails {

    private String externalId;

    private String hotelName;

    private String destinationName;

    private String countryName;

    private String roomName;

    private String boardName;

    private LocalDate checkInDate;

    private LocalDate checkOutDate;

    private Integer numberOfNights;

    private Integer numberOfRooms;

    private Integer adults;

    private Integer children;

    private BigDecimal totalPrice;

    private BigDecimal taxes;

    private String currency;

    private HotelBookingApi hotelBooking;
  }

  /**
   * Información de pago.
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PaymentInfo {

    private Long id;

    private String paymentType;

    private String paymentMethod;

    private BigDecimal amount;

    private String currency;

    private String status;

    private Long externalId;

    private LocalDateTime createdDatetime;
  }
}
