package masera.deviajebookingsandpayments.controllers;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import masera.deviajebookingsandpayments.dtos.bookings.CreatePackageBookingRequestDto;
import masera.deviajebookingsandpayments.dtos.payments.PaymentRequestDto;
import masera.deviajebookingsandpayments.dtos.responses.BookAndPayResponseDto;
import masera.deviajebookingsandpayments.dtos.responses.BookingResponseDto;
import masera.deviajebookingsandpayments.services.interfaces.PackageBookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
   * @param paymentRequest datos del pago
   * @return respuesta unificada con reserva y pago
   */
  @PostMapping("/book-and-pay")
  public ResponseEntity<BookAndPayResponseDto> bookPackageAndPay(
          @Valid @RequestBody CreatePackageBookingRequestDto request,
          @Valid @RequestBody PaymentRequestDto paymentRequest) {

    log.info("Iniciando reserva y pago de paquete para cliente: {}", request.getClientId());

    try {
      BookAndPayResponseDto response = packageBookingService.bookAndPay(request, paymentRequest);

      if (response.getSuccess()) {
        log.info("Reserva de paquete exitosa. ID: {}", response.getBooking().getId());
        return ResponseEntity.ok(response);
      } else {
        log.warn("Fallo en reserva de paquete: {}", response.getDetailedError());
        return ResponseEntity.badRequest().body(response);
      }

    } catch (Exception e) {
      log.error("Error inesperado al procesar reserva de paquete", e);
      BookAndPayResponseDto errorResponse = BookAndPayResponseDto.builder()
              .success(false)
              .message("Error interno del servidor")
              .failureReason("INTERNAL_ERROR")
              .detailedError(e.getMessage())
              .build();
      return ResponseEntity.internalServerError().body(errorResponse);
    }
  }

  /**
   * Obtiene las reservas de paquetes de un cliente.
   *
   * @param clientId ID del cliente
   * @return lista de reservas
   */
  @GetMapping("/client/{clientId}")
  public ResponseEntity<List<BookingResponseDto>> getClientPackageBookings(
          @PathVariable Long clientId) {

    log.info("Obteniendo reservas de paquetes para el cliente: {}", clientId);

    try {
      List<BookingResponseDto> bookings = packageBookingService.getClientPackageBookings(clientId);
      return ResponseEntity.ok(bookings);
    } catch (Exception e) {
      log.error("Error al obtener reservas de paquetes del cliente: {}", clientId, e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /**
   * Obtiene los detalles de una reserva de paquete.
   *
   * @param id ID de la reserva
   * @return detalles de la reserva
   */
  @GetMapping("/{id}")
  public ResponseEntity<BookingResponseDto> getPackageBooking(@PathVariable UUID id) {
    log.info("Obteniendo detalles de reserva de paquete: {}", id);

    try {
      BookingResponseDto booking = packageBookingService.getPackageBookingDetails(id);
      return ResponseEntity.ok(booking);
    } catch (Exception e) {
      log.error("Error al obtener detalles de reserva de paquete: {}", id, e);
      return ResponseEntity.notFound().build();
    }
  }

  /**
   * Cancela una reserva de paquete.
   *
   * @param id ID de la reserva
   * @return respuesta de cancelación
   */
  @PutMapping("/{id}/cancel")
  public ResponseEntity<BookAndPayResponseDto> cancelPackageBooking(@PathVariable UUID id) {
    log.info("Cancelando reserva de paquete: {}", id);

    try {
      BookAndPayResponseDto response = packageBookingService.cancelBooking(id);

      if (response.getSuccess()) {
        return ResponseEntity.ok(response);
      } else {
        return ResponseEntity.badRequest().body(response);
      }

    } catch (Exception e) {
      log.error("Error al cancelar reserva de paquete: {}", id, e);
      BookAndPayResponseDto errorResponse = BookAndPayResponseDto.builder()
              .success(false)
              .message("Error al cancelar la reserva")
              .failureReason("CANCELLATION_ERROR")
              .detailedError(e.getMessage())
              .build();
      return ResponseEntity.internalServerError().body(errorResponse);
    }
  }

  /**
   * Verifica disponibilidad y precio de un paquete.
   *
   * @param packageDetails detalles del paquete a verificar
   * @return información actualizada del paquete
   */
  @PostMapping("/verify-price")
  public ResponseEntity<Map<String, Object>> verifyPackagePrice(
          @RequestBody Map<String, Object> packageDetails) {

    log.info("Verificando precio y disponibilidad de paquete");

    try {
      Map<String, Object> verifiedPackage =
              packageBookingService.verifyPackagePrice(packageDetails);

      return ResponseEntity.ok(verifiedPackage);
    } catch (Exception e) {
      log.error("Error al verificar precio de paquete: {}", e.getMessage());
      return ResponseEntity.badRequest().body(
              Map.of("error", "Paquete no disponible", "message", e.getMessage())
      );
    }
  }
}
