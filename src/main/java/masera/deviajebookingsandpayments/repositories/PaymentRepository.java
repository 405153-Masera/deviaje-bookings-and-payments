package masera.deviajebookingsandpayments.repositories;

import java.util.List;
import java.util.Optional;
import masera.deviajebookingsandpayments.entities.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para acceder a los datos de Payment.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

  /**
   * Encuentra pagos por ID de reserva.
   *
   * @param bookingId ID de la reserva
   * @return Lista de pagos de la reserva
   */
  List<Payment> findByBookingId(Long bookingId);

  /**
   * Encuentra pagos por ID externo de pago.
   *
   * @param externalPaymentId id externo del pago (del proveedor)
   * @return Pago
   */
  Optional<Payment> findByExternalPaymentId(String externalPaymentId);

  /**
   * Encuentra pagos por estado.
   *
   * @param status Estado del pago
   * @return Lista de pagos con el estado especificado
   */
  List<Payment> findByStatus(Payment.PaymentStatus status);

  /**
   * Encuentra pagos por método de pago.
   *
   * @param method Método de pago
   * @return Lista de pagos con el método especificado
   */
  List<Payment> findByMethod(String method);
}