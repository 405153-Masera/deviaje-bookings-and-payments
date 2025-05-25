package masera.deviajebookingsandpayments.repositories;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import masera.deviajebookingsandpayments.entities.HotelBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


/**
 * Repositorio para acceder a los datos de HotelBooking.
 */
@Repository
public interface HotelBookingRepository extends JpaRepository<HotelBooking, Long> {

  /**
   * Encuentra una reserva de hotel por su ID externo.
   *
   * @param externalId ID externo (de HotelBeds)
   * @return Reserva de hotel
   */
  Optional<HotelBooking> findByExternalId(String externalId);

  /**
   * Encuentra reservas de hotel por código de hotel.
   *
   * @param hotelCode Código del hotel
   * @return Lista de reservas del hotel
   */
  List<HotelBooking> findByHotelCode(String hotelCode);

  /**
   * Encuentra reservas de hotel por ID de reserva principal.
   *
   * @param bookingId ID de la reserva principal
   * @return Lista de reservas de hotel
   */
  List<HotelBooking> findByBookingId(Long bookingId);

  /**
   * Encuentra reservas de hotel por código de destino.
   *
   * @param destinationCode Código del destino
   * @return Lista de reservas para el destino
   */
  List<HotelBooking> findByDestinationCode(String destinationCode);

  /**
   * Encuentra reservas de hotel por fecha de check-in.
   *
   * @param from Fecha inicial
   * @param to Fecha final
   * @return Lista de reservas de hotel
   */
  List<HotelBooking> findByCheckInDateBetween(LocalDate from, LocalDate to);
}
