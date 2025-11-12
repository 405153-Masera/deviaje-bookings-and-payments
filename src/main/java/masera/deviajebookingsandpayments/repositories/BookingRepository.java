package masera.deviajebookingsandpayments.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import masera.deviajebookingsandpayments.entities.BookingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para acceder a los datos de Booking.
 */
@Repository
public interface BookingRepository extends JpaRepository<BookingEntity, Long> {

  /**
   * Encuentra reservas por ID de cliente.
   *
   * @param clientId ID del cliente
   * @return Lista de reservas del cliente
   */
  List<BookingEntity> findByClientId(Integer clientId);

  /**
   * Encuentra reservas por ID de cliente y tipo.
   *
   * @param clientId ID del cliente
   * @param type Tipo de reserva (FLIGHT, HOTEL, PACKAGE)
   * @return Lista de reservas del cliente del tipo especificado
   */
  List<BookingEntity> findByClientIdAndType(Integer clientId, BookingEntity.BookingType type);

  /**
   * Encuentra reservas por ID de agente.
   *
   * @param agentId ID del agente
   * @return Lista de reservas del agente
   */
  List<BookingEntity> findByAgentId(Integer agentId);

  // Agregar este método al BookingRepository existente

  /**
   * Encuentra reservas por estado.
   *
   * @param status Estado de la reserva
   * @return Lista de reservas con el estado especificado
   */
  List<BookingEntity> findByStatus(BookingEntity.BookingStatus status);

  /**
   * Encuentra una reserva por su booking reference.
   *
   * @param bookingReference Referencia de la reserva
   * @return Reserva opcional
   */
  Optional<BookingEntity> findByBookingReference(String bookingReference);

  /**
   * Encuentra reservas por rango de fechas.
   *
   * @param startDate Fecha inicial
   * @param endDate Fecha final
   * @return Lista de reservas en el rango de fechas
   */
  List<BookingEntity> findByCreatedDatetimeBetween(LocalDateTime startDate, LocalDateTime endDate);

  /**
   * Encuentra reservas por cliente y estado.
   *
   * @param clientId ID del cliente
   * @param status Estado de la reserva
   * @return Lista de reservas del cliente con el estado especificado
   */
  List<BookingEntity> findByClientIdAndStatus(Integer clientId, BookingEntity.BookingStatus status);
}
