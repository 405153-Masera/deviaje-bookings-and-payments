package masera.deviajebookingsandpayments.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
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
public class HotelBookingEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "booking_id", nullable = false)
  private BookingEntity bookingEntity;

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

  @Column(nullable = false)
  private Integer adults;

  @Column(nullable = false)
  @Builder.Default
  private Integer children = 0;

  private BigDecimal totalPrice; // net de hotelbeds

  @Column()
  private BigDecimal taxes;

  @Column(length = 3)
  private String currency;

  @Lob
  @Column(columnDefinition = "JSON")
  private String hotelBooking;

  private LocalDateTime createdDatetime;

  /**
   * Metodo que se ejecuta antes de guardarse en la base de datos.
   */
  @PrePersist
  protected void onCreate() {
    this.createdDatetime = LocalDateTime.now();
  }
}
