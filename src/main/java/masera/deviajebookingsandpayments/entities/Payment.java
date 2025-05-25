package masera.deviajebookingsandpayments.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad para pagos (sin detalles de tarjeta).
 */
@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "booking_id", nullable = false)
  private Booking booking;

  @Column(nullable = false)
  private BigDecimal amount;

  @Column(length = 3, nullable = false)
  @Builder.Default
  private String currency = "ARS";

  @Column(length = 50, nullable = false)
  private String method; // CREDIT_CARD, MERCADO_PAGO, TRANSFER, etc.

  @Column(name = "payment_provider", length = 50)
  private String paymentProvider; // MERCADO_PAGO, STRIPE, etc.

  @Column(name = "external_payment_id", length = 100)
  private String externalPaymentId; // ID del proveedor de pago

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PaymentStatus status;

  @Column
  private LocalDateTime date;

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
    if (this.date == null) {
      this.date = LocalDateTime.now();
    }
  }

  @PreUpdate
  protected void onUpdate() {
    this.lastUpdatedDatetime = LocalDateTime.now();
  }

  /**
   * Enum para estados de pago.
   */
  public enum PaymentStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED,
    REFUNDED
  }
}
