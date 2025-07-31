package masera.deviajebookingsandpayments.repositories;

import java.time.LocalDateTime;
import java.util.List;
import masera.deviajebookingsandpayments.entities.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para acceder a los datos de Booking.
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

  /**
   * Encuentra reservas por ID de cliente.
   *
   * @param clientId ID del cliente
   * @return Lista de reservas del cliente
   */
  List<Booking> findByClientId(Integer clientId);

  /**
   * Encuentra reservas por ID de cliente y tipo.
   *
   * @param clientId ID del cliente
   * @param type Tipo de reserva (FLIGHT, HOTEL, PACKAGE)
   * @return Lista de reservas del cliente del tipo especificado
   */
  List<Booking> findByClientIdAndType(Integer clientId, Booking.BookingType type);

  /**
   * Encuentra reservas por ID de agente.
   *
   * @param agentId ID del agente
   * @return Lista de reservas del agente
   */
  List<Booking> findByAgentId(Integer agentId);

  // Agregar este método al BookingRepository existente

  /**
   * Encuentra reservas por estado.
   *
   * @param status Estado de la reserva
   * @return Lista de reservas con el estado especificado
   */
  List<Booking> findByStatus(Booking.BookingStatus status);

  /**
   * Encuentra reservas por rango de fechas.
   *
   * @param startDate Fecha inicial
   * @param endDate Fecha final
   * @return Lista de reservas en el rango de fechas
   */
  List<Booking> findByCreatedDatetimeBetween(LocalDateTime startDate, LocalDateTime endDate);

  /**
   * Encuentra reservas por cliente y estado.
   *
   * @param clientId ID del cliente
   * @param status Estado de la reserva
   * @return Lista de reservas del cliente con el estado especificado
   */
  List<Booking> findByClientIdAndStatus(Integer clientId, Booking.BookingStatus status);
}
