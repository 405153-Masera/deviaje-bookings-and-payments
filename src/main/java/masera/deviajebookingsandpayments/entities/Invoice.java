package masera.deviajebookingsandpayments.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad para facturas.
 */
@Entity
@Table(name = "invoices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "booking_id", nullable = false)
  private Booking booking;

  @Column(name = "invoice_number", length = 20, nullable = false, unique = true)
  private String invoiceNumber;

  @Column
  private LocalDateTime date;

  @Column(nullable = false)
  private BigDecimal subtotal;

  @Column(nullable = false)
  private BigDecimal taxes;

  @Column(precision = 10, scale = 2)
  @Builder.Default
  private BigDecimal discounts = BigDecimal.ZERO;

  @Column(nullable = false)
  private BigDecimal total;

  @Column(length = 3, nullable = false)
  @Builder.Default
  private String currency = "ARS";

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private InvoiceStatus status;

  @Column(name = "pdf_path", length = 255)
  private String pdfPath;

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
   * Enum para estados de factura.
   */
  public enum InvoiceStatus {
    ISSUED,
    CANCELLED,
  }
}