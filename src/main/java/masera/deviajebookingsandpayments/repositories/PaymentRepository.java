package masera.deviajebookingsandpayments.repositories;

import java.util.List;
import java.util.Optional;
import masera.deviajebookingsandpayments.entities.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para acceder a los datos de Payment.
 */
@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {

  /**
   * Encuentra pagos por ID de reserva.
   *
   * @param bookingId ID de la reserva
   * @return Lista de pagos de la reserva
   */
  List<PaymentEntity> findByBookingEntityId(Long bookingId);

  /**
   * Encuentra pagos por ID externo de pago.
   *
   * @param externalPaymentId id externo del pago (del proveedor)
   * @return Pago
   */
  Optional<PaymentEntity> findByExternalPaymentId(String externalPaymentId);

  /**
   * Encuentra pagos por estado.
   *
   * @param status Estado del pago
   * @return Lista de pagos con el estado especificado
   */
  List<PaymentEntity> findByStatus(PaymentEntity.PaymentStatus status);

  /**
   * Encuentra pagos por método de pago.
   *
   * @param method Método de pago
   * @return Lista de pagos con el método especificado
   */
  List<PaymentEntity> findByMethod(String method);
}