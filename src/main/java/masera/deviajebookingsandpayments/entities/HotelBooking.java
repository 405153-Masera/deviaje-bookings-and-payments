package masera.deviajebookingsandpayments.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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

  @Column(name = "hotel_code", length = 10, nullable = false)
  private String hotelCode;

  @Column(name = "hotel_name", length = 100, nullable = false)
  private String hotelName;

  @Column(name = "destination_code", length = 10, nullable = false)
  private String destinationCode;

  @Column(name = "destination_name", length = 50, nullable = false)
  private String destinationName;

  @Column(name = "check_in_date", nullable = false)
  private LocalDate checkInDate;

  @Column(name = "check_out_date", nullable = false)
  private LocalDate checkOutDate;

  @Column(name = "number_of_nights", nullable = false)
  private Integer numberOfNights;

  @Column(name = "number_of_rooms", nullable = false)
  private Integer numberOfRooms;

  @Column(nullable = false)
  private Integer adults;

  @Column(nullable = false)
  @Builder.Default
  private Integer children = 0;

  @Column(name = "base_price", nullable = false)
  private BigDecimal basePrice;

  @Column(nullable = false)
  private BigDecimal taxes;

  @Column()
  @Builder.Default
  private BigDecimal discounts = BigDecimal.ZERO;

  @Column(name = "total_price", nullable = false)
  private BigDecimal totalPrice;

  @Column(length = 3, nullable = false)
  private String currency;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private HotelBookingStatus status;

  @Column(name = "created_datetime")
  private LocalDateTime createdDatetime;

  @Column(name = "created_user")
  private Long createdUser;

  @Column(name = "last_updated_datetime")
  private LocalDateTime lastUpdatedDatetime;

  @Column(name = "last_updated_user")
  private Long lastUpdatedUser;

  @PrePersist
  protected void onCreate() {
    this.createdDatetime = LocalDateTime.now();
    this.lastUpdatedDatetime = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    this.lastUpdatedDatetime = LocalDateTime.now();
  }

  /**
   * Enum para estados de reserva de hoteles.
   */
  public enum HotelBookingStatus {
    CONFIRMED,
    CANCELLED
  }
}
