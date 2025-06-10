package masera.deviajebookingsandpayments.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import masera.deviajebookingsandpayments.entities.FlightBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


/**
 * Repositorio para acceder a los datos de FlightBooking.
 */
@Repository
public interface FlightBookingRepository extends JpaRepository<FlightBooking, Long> {

  /**
   * Encuentra una reserva de vuelo por su ID externo.
   *
   * @param externalId ID externo (de Amadeus)
   * @return Reserva de vuelo
   */
  Optional<FlightBooking> findByExternalId(String externalId);

  /**
   * Encuentra reservas de vuelo por origen y destino.
   *
   * @param origin Origen (código IATA)
   * @param destination Destino (código IATA)
   * @return Lista de reservas de vuelo
   */
  List<FlightBooking> findByOriginAndDestination(String origin, String destination);

  /**
   * Encuentra reservas de vuelo por ID de reserva principal.
   *
   * @param bookingId ID de la reserva principal
   * @return Lista de reservas de vuelo
   */
  List<FlightBooking> findByBookingId(Long bookingId);

  /**
   * Encuentra reservas de vuelo por fecha de salida.
   *
   * @param from Fecha inicial
   * @param to Fecha final
   * @return Lista de reservas de vuelo
   */
  List<FlightBooking> findByDepartureDateBetween(String from, String to);
}