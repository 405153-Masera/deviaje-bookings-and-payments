package masera.deviajebookingsandpayments.repositories;

import masera.deviajebookingsandpayments.entities.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

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
  List<Booking> findByClientId(Long clientId);

  /**
   * Encuentra reservas por ID de cliente y tipo.
   *
   * @param clientId ID del cliente
   * @param type Tipo de reserva (FLIGHT, HOTEL, PACKAGE)
   * @return Lista de reservas del cliente del tipo especificado
   */
  List<Booking> findByClientIdAndType(Long clientId, Booking.BookingType type);

  /**
   * Encuentra reservas por ID de agente.
   *
   * @param agentId ID del agente
   * @return Lista de reservas del agente
   */
  List<Booking> findByAgentId(Long agentId);

  /**
   * Encuentra reservas por ID de sucursal.
   *
   * @param branchId ID de la sucursal
   * @return Lista de reservas de la sucursal
   */
  List<Booking> findByBranchId(Long branchId);
}
