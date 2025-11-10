package masera.deviajebookingsandpayments.controllers;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import masera.deviajebookingsandpayments.dtos.bookings.BookPackageAndPayRequest;
import masera.deviajebookingsandpayments.dtos.responses.BookingReferenceResponse;
import masera.deviajebookingsandpayments.dtos.responses.BookingResponseDto;
import masera.deviajebookingsandpayments.services.interfaces.PackageBookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador para reservas de paquetes (vuelo + hotel).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/packages")
@Slf4j
public class PackageBookingController {

  private final PackageBookingService packageBookingService;

  /**
   * Endpoint unificado: Reservar paquete y procesar pago.
   *
   * @param request datos de la reserva de paquete
   * @return respuesta unificada con reserva y pago
   */
  @PostMapping("/book-and-pay")
  public ResponseEntity<BookingReferenceResponse> bookPackageAndPay(
          @Valid @RequestBody BookPackageAndPayRequest request) {

    BookingReferenceResponse response = packageBookingService.bookAndPay(
              request.getPackageBookingRequest(),
              request.getPaymentRequest(),
              request.getPrices()
    );
    return ResponseEntity.ok(response);
  }

  /**
   * Obtiene las reservas de paquetes de un cliente.
   *
   * @param clientId ID del cliente
   * @return lista de reservas
   */
  @GetMapping("/client/{clientId}")
  public ResponseEntity<List<BookingResponseDto>> getClientPackageBookings(
          @PathVariable Integer clientId) {
    log.info("Obteniendo reservas de paquetes para el cliente: {}", clientId);
    List<BookingResponseDto> bookings = packageBookingService.getClientPackageBookings(clientId);
    return ResponseEntity.ok(bookings);
  }

  /**
   * Obtiene los detalles de una reserva de paquete.
   *
   * @param id ID de la reserva
   * @return detalles de la reserva
   */
  @GetMapping("/{id}")
  public ResponseEntity<BookingResponseDto> getPackageBooking(@PathVariable Long id) {
    log.info("Obteniendo detalles de reserva de paquete: {}", id);
    BookingResponseDto booking = packageBookingService.getPackageBookingDetails(id);
    return ResponseEntity.ok(booking);
  }
}
