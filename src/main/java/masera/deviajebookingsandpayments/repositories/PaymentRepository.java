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
   * Encuentra un pago por booking y estado.
   *
   * @param bookingId la entidad de reserva
   * @param status el estado del pago
   * @return Pago si existe
   */
  Optional<PaymentEntity> findByBookingEntityIdAndStatus(
          Long bookingId,
          PaymentEntity.PaymentStatus status
  );
}
