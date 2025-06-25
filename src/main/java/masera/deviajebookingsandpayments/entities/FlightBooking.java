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

  @Column(length = 3)
  private String origin;

  @Column(length = 3)
  private String destination;

  @Column(name = "departure_date")
  private String departureDate;

  @Column(name = "return_date")
  private String returnDate;

  @Column(length = 2)
  private String carrier; // Código IATA de aerolínea

  @Column()
  private Integer adults;

  @Column()
  private Integer children;

  @Column()
  private Integer infants;

  @Column(columnDefinition = "JSON")
  private String itineraries; // Guardar lista de itinerarios en JSON

  @Column(name = "total_price", nullable = false)
  private BigDecimal totalPrice; // Grand total de Amadeus

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

  @Column(name = "created_user")
  private Long createdUser;

  @PrePersist
  protected void onCreate() {
    this.createdDatetime = LocalDateTime.now();
  }
}