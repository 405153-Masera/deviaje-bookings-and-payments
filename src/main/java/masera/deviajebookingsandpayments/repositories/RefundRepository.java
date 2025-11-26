package masera.deviajebookingsandpayments.repositories;

import java.util.List;
import java.util.Optional;
import masera.deviajebookingsandpayments.entities.BookingEntity;
import masera.deviajebookingsandpayments.entities.RefundEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para RefundEntity.
 */
@Repository
public interface RefundRepository extends JpaRepository<RefundEntity, Long> {

  /**
   * Busca un reembolso por su ID externo de MercadoPago.
   */
  Optional<RefundEntity> findByExternalRefundId(String externalRefundId);

  /**
   * Busca todos los reembolsos de un booking.
   */
  List<RefundEntity> findByBookingEntity(BookingEntity bookingEntity);

  /**
   * Busca todos los reembolsos de un booking por su ID.
   */
  List<RefundEntity> findByBookingEntityId(Long bookingId);
}
