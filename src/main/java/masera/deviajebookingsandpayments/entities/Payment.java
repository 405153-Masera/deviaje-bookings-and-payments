package masera.deviajebookingsandpayments.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
  @JoinColumn(name = "booking_id")
  private Booking booking;

  @Column()
  private BigDecimal amount;

  @Column(length = 3)
  @Builder.Default
  private String currency = "ARS";

  @Column(length = 50)
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
  @PrePersist
  protected void onCreate() {
    if (this.date == null) {
      this.date = LocalDateTime.now();
    }
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
