package masera.deviajebookingsandpayments.controllers;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import masera.deviajebookingsandpayments.dtos.responses.BookingResponseDto;
import masera.deviajebookingsandpayments.entities.Booking;
import masera.deviajebookingsandpayments.repositories.BookingRepository;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador para operaciones generales sobre reservas.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/bookings")
@Slf4j
public class BookingController {

  private final BookingRepository bookingRepository;
  private final ModelMapper modelMapper;

  /**
   * Obtiene todas las reservas de un cliente.
   *
   * @param clientId ID del cliente
   * @return lista de reservas
   */
  @GetMapping("/client/{clientId}")
  public ResponseEntity<List<BookingResponseDto>> getClientBookings(
          @PathVariable Integer clientId) {
    log.info("Obteniendo reservas para el cliente: {}", clientId);

    try {
      List<Booking> bookings = bookingRepository.findByClientId(clientId);
      List<BookingResponseDto> response = bookings.stream()
              .map(booking -> modelMapper.map(booking, BookingResponseDto.class))
              .collect(Collectors.toList());

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Error al obtener reservas del cliente", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /**
   * Obtiene una reserva específica.
   *
   * @param id ID de la reserva
   * @return detalles de la reserva
   */
  @GetMapping("/{id}")
  public ResponseEntity<BookingResponseDto> getBooking(@PathVariable Long id) {
    log.info("Obteniendo detalles de la reserva: {}", id);

    try {
      Optional<Booking> bookingOpt = bookingRepository.findById(id);
      if (bookingOpt.isEmpty()) {
        return ResponseEntity.notFound().build();
      }

      BookingResponseDto response = modelMapper.map(bookingOpt.get(), BookingResponseDto.class);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Error al obtener reserva", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  // Agregar este método al BookingController existente

  /**
   * Obtiene todas las reservas (solo para administradores).
   *
   * @return lista de todas las reservas
   */
  @GetMapping("/admin/all")
  public ResponseEntity<List<BookingResponseDto>> getAllBookings() {
    log.info("Obteniendo todas las reservas (administrador)");

    try {
      List<Booking> bookings = bookingRepository.findAll();
      List<BookingResponseDto> response = bookings.stream()
              .map(booking -> modelMapper.map(booking, BookingResponseDto.class))
              .collect(Collectors.toList());

      log.info("Se encontraron {} reservas", response.size());
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Error al obtener todas las reservas", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /**
   * Obtiene reservas filtradas por estado.
   *
   * @param status estado de la reserva (PENDING, CONFIRMED, CANCELLED, COMPLETED)
   * @return lista de reservas
   */
  @GetMapping("/admin/status/{status}")
  public ResponseEntity<List<BookingResponseDto>> getBookingsByStatus(@PathVariable String status) {
    log.info("Obteniendo reservas con estado: {}", status);

    try {
      Booking.BookingStatus bookingStatus = Booking.BookingStatus.valueOf(status.toUpperCase());
      List<Booking> bookings = bookingRepository.findByStatus(bookingStatus);
      List<BookingResponseDto> response = bookings.stream()
              .map(booking -> modelMapper.map(booking, BookingResponseDto.class))
              .collect(Collectors.toList());

      return ResponseEntity.ok(response);
    } catch (IllegalArgumentException e) {
      log.error("Estado de reserva inválido: {}", status);
      return ResponseEntity.badRequest().build();
    } catch (Exception e) {
      log.error("Error al obtener reservas por estado", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /**
   * Obtiene el historial de reservas de un agente.
   *
   * @param agentId ID del agente
   * @return lista de reservas
   */
  @GetMapping("/agent/{agentId}")
  public ResponseEntity<List<BookingResponseDto>> getAgentBookings(@PathVariable Integer agentId) {
    log.info("Obteniendo reservas para el agente: {}", agentId);

    try {
      List<Booking> bookings = bookingRepository.findByAgentId(agentId);
      List<BookingResponseDto> response = bookings.stream()
              .map(booking -> modelMapper.map(booking, BookingResponseDto.class))
              .collect(Collectors.toList());

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Error al obtener reservas del agente", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /**
   * Obtiene reservas filtradas por tipo.
   *
   * @param clientId ID del cliente
   * @param type tipo de reserva (FLIGHT, HOTEL, PACKAGE)
   * @return lista de reservas
   */
  @GetMapping("/client/{clientId}/type/{type}")
  public ResponseEntity<List<BookingResponseDto>> getClientBookingsByType(
          @PathVariable Integer clientId,
          @PathVariable String type) {

    log.info("Obteniendo reservas de tipo {} para el cliente: {}", type, clientId);

    try {
      Booking.BookingType bookingType = Booking.BookingType.valueOf(type.toUpperCase());
      List<Booking> bookings = bookingRepository.findByClientIdAndType(clientId, bookingType);
      List<BookingResponseDto> response = bookings.stream()
              .map(booking -> modelMapper.map(booking, BookingResponseDto.class))
              .collect(Collectors.toList());

      return ResponseEntity.ok(response);
    } catch (IllegalArgumentException e) {
      log.error("Tipo de reserva inválido: {}", type);
      return ResponseEntity.badRequest().build();
    } catch (Exception e) {
      log.error("Error al obtener reservas del cliente por tipo", e);
      return ResponseEntity.internalServerError().build();
    }
  }
}