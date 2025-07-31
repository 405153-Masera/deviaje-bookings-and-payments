package masera.deviajebookingsandpayments.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * Entidad para reservas de hoteles (datos mínimos).
 */
@Entity
@Table(name = "hotels_bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotelBooking {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "booking_id", nullable = false)
  private Booking booking;

  @Column(name = "external_id", length = 50)
  private String externalId; // Reference de HotelBeds

  @Column(name = "hotel_name", length = 100)
  private String hotelName;

  @Column(name = "destination_name", length = 50)
  private String destinationName;

  @Column(name = "room_name", length = 50)
  private String roomName;

  @Column(name = "board_name", length = 50)
  private String boardName;

  @Column(name = "check_in_date")
  private LocalDate checkInDate;

  @Column(name = "check_out_date")
  private LocalDate checkOutDate;

  @Column(name = "number_of_nights")
  private Integer numberOfNights;

  @Column(name = "number_of_rooms")
  private Integer numberOfRooms;

  @Column(nullable = false)
  private Integer adults;

  @Column(nullable = false)
  @Builder.Default
  private Integer children = 0;

  @Column(name = "total_price")
  private BigDecimal totalPrice; // net de hotelsbeds

  @Column()
  private BigDecimal taxes;

  @Column(length = 3)
  private String currency;

  @Column(name = "cancellation_from")
  private LocalDate cancellationFrom;

  @Column(name = "cancellation_amount")
  @Builder.Default
  private BigDecimal cancellationAmount = BigDecimal.ZERO;

  @Column(name = "created_datetime")
  private LocalDateTime createdDatetime;

  @PrePersist
  protected void onCreate() {
    this.createdDatetime = LocalDateTime.now();
  }
}
