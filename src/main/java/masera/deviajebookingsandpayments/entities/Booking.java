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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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
public class Booking {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY) // Esto hace el auto increment
  private Long id;  // Cambiar de UUID a Long

  @Column(name = "client_id")
  private Long clientId;

  @Column(name = "agent_id")
  private Long agentId;

  @Column(name = "branch_id")
  private Long branchId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private BookingStatus status;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private BookingType type;

  @Column(name = "total_amount", nullable = false)
  private BigDecimal totalAmount;

  @Column(length = 3, nullable = false)
  @Builder.Default
  private String currency = "ARS";

  @Column()
  @Builder.Default
  private BigDecimal discount = BigDecimal.ZERO;

  @Column()
  @Builder.Default
  private BigDecimal taxes = BigDecimal.ZERO;

  @Column(length = 20)
  private String phone;

  @Column(length = 100)
  private String email;

  @Column(name = "created_datetime")
  private LocalDateTime createdDatetime;

  @Column(name = "created_user")
  private Long createdUser;

  @Column(name = "last_updated_datetime")
  private LocalDateTime lastUpdatedDatetime;

  @Column(name = "last_updated_user")
  private Long lastUpdatedUser;

  // Relaciones
  @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<FlightBooking> flightBookings;

  @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<HotelBooking> hotelBookings;

  @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<Payment> payments;

  @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<Invoice> invoices;

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