package masera.deviajebookingsandpayments.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad principal de reservas unificada.
 */
@Entity
@Table(name = "bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY) // Esto hace el auto increment
  private Long id;

  @Column(unique = true)
  private String bookingReference;

  private String externalReference;

  private Integer clientId;

  private Integer agentId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private BookingStatus status;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private BookingType type;

  @Column(nullable = false)
  private BigDecimal totalAmount;

  @Column()
  @Builder.Default
  private BigDecimal commission = BigDecimal.ZERO; // AGREGADO: campo commission

  @Column()
  @Builder.Default
  private BigDecimal discount = BigDecimal.ZERO;

  @Column()
  @Builder.Default
  private BigDecimal taxes = BigDecimal.ZERO;

  @Column(length = 3, nullable = false)
  @Builder.Default
  private String currency = "ARS";

  private String holderName;

  @Column(length = 5)
  private String countryCallingCode;

  @Column(length = 20)
  private String phone;

  @Column(length = 100)
  private String email;

  private LocalDateTime createdDatetime;

  // Relaciones
  @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<FlightBookingEntity> flightBookingEntities;

  @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<HotelBookingEntity> hotelBookingEntities;

  @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<PaymentEntity> paymentEntities;

  /**
   * Metodo que se activa antes de persistir en la base de datos.
   */
  @PrePersist
  protected void onCreate() {
    this.createdDatetime = LocalDateTime.now();
  }

  /**
   * Enum para los estados de reserva.
   */
  public enum BookingStatus {
    CONFIRMED,
    CANCELLED
  }

  /**
   * Enum para los tipos de reserva.
   */
  public enum BookingType {
    FLIGHT,
    HOTEL,
    PACKAGE
  }
}