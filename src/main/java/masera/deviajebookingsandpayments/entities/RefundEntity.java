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
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad que representa un reembolso.
 */
@Entity
@Table(name = "refunds")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * Booking asociado al reembolso.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "booking_id", nullable = false)
  private BookingEntity bookingEntity;

  /**
   * Pago original que se está reembolsando.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "original_payment_id", nullable = false)
  private PaymentEntity originalPayment;

  /**
   * Monto del reembolso.
   */
  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal amount;

  /**
   * Moneda (ARS, USD, etc.).
   */
  @Column(nullable = false, length = 3)
  private String currency;

  /**
   * Estado del reembolso.
   */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private RefundStatus status;

  /**
   * Id del reembolso en MercadoPago.
   */
  @Column(length = 100)
  private String externalRefundId;

  /**
   * Fecha de creación del reembolso.
   */
  @Column(nullable = false)
  private LocalDateTime createdAt;

  /**
   * Fecha de confirmación del reembolso.
   */
  private LocalDateTime completedAt;

  /**
   * Motivo del reembolso (opcional).
   */
  @Column(length = 500)
  private String reason;

  /**
   * Estados posibles de un reembolso.
   */
  public enum RefundStatus {
    PENDING,
    COMPLETED,
    FAILED
  }

  /**
   * Set en estado pendiente.
   */
  @PrePersist
  protected void onCreate() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
    if (status == null) {
      status = RefundStatus.PENDING;
    }
  }
}
