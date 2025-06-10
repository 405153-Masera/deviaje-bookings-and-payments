package masera.deviajebookingsandpayments.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad para reservas de vuelos (datos mínimos).
 */
@Entity
@Table(name = "flights_bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlightBooking {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "booking_id", nullable = false)
  private Booking booking;

  @Column(name = "external_id", length = 50)
  private String externalId; // ID de Amadeus

  @Column(length = 3, nullable = false)
  private String origin;

  @Column(length = 3, nullable = false)
  private String destination;

  @Column(name = "departure_date", nullable = false)
  private String departureDate;

  @Column(name = "return_date")
  private String returnDate;

  @Column(length = 2, nullable = false)
  private String carrier; // Código IATA de aerolínea

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
  private FlightBookingStatus status;

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
   * Enum para estados de reserva de vuelos.
   */
  public enum FlightBookingStatus {
    CONFIRMED,
    CANCELLED
  }
}