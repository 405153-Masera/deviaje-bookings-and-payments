package masera.deviajebookingsandpayments.controllers;

import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import masera.deviajebookingsandpayments.dtos.BookFlightAndPayRequest;
import masera.deviajebookingsandpayments.dtos.responses.BookAndPayResponseDto;
import masera.deviajebookingsandpayments.dtos.responses.FlightBookingResponseDto;
import masera.deviajebookingsandpayments.services.interfaces.FlightBookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador para reservas de vuelos.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/flights")
@Slf4j
public class FlightBookingController {

  private final FlightBookingService flightBookingService;

  /**
   * Endpoint unificado: Reservar vuelo y procesar pago.
   *
   * @param request datos de la reserva de vuelo
   * @return respuesta unificada con reserva y pago
   */
  @PostMapping("/book-and-pay")
  public ResponseEntity<BookAndPayResponseDto> bookFlightAndPay(
          @Valid @RequestBody BookFlightAndPayRequest request) {

    log.info("Iniciando reserva y pago de vuelo para cliente: {}",
            request.getBookingRequest().getClientId());

    try {
      BookAndPayResponseDto response = flightBookingService.bookAndPay(request.getBookingRequest(),
              request.getPaymentRequest(), request.getPrices());

      // Siempre devolver HTTP 200 (OK) y dejar que el campo success indique el resultado
      return ResponseEntity.ok(response);

    } catch (Exception e) {
      log.error("Error inesperado al procesar reserva de vuelo", e);
      BookAndPayResponseDto errorResponse = BookAndPayResponseDto.builder()
              .success(false)
              .message("Error interno del servidor")
              .failureReason("INTERNAL_ERROR")
              .detailedError(e.getMessage())
              .build();

      // También devolver HTTP 200 para errores no controlados
      return ResponseEntity.ok(errorResponse);
    }
  }

  /**
   * Obtiene los datos básicos de una reserva de vuelo.
   *
   * @param id ID de la reserva de vuelo
   * @return datos básicos del vuelo reservado
   */
  @GetMapping("/bookings/{id}")
  public ResponseEntity<FlightBookingResponseDto> getFlightBooking(@PathVariable Long id) {

    log.info("Obteniendo reserva de vuelo básica: {}", id);

    try {
      FlightBookingResponseDto booking = flightBookingService.getBasicBookingInfo(id);
      return ResponseEntity.ok(booking);
    } catch (Exception e) {
      log.error("Error al obtener reserva de vuelo: {}", id, e);
      return ResponseEntity.notFound().build();
    }
  }

  /**
   * Obtiene los detalles completos de una reserva (llama a Amadeus API).
   *
   * @param id ID de la reserva de vuelo
   * @return detalles completos desde Amadeus
   */
  @GetMapping("/bookings/{id}/details")
  public ResponseEntity<Object> getFlightBookingDetails(@PathVariable Long id) {

    log.info("Obteniendo detalles completos de reserva de vuelo: {}", id);

    try {
      Object fullDetails = flightBookingService.getFullBookingDetails(id);
      return ResponseEntity.ok(fullDetails);
    } catch (Exception e) {
      log.error("Error al obtener detalles de reserva de vuelo: {}", id, e);
      return ResponseEntity.notFound().build();
    }
  }

  /**
   * Verifica disponibilidad y precio de una oferta de vuelo.
   *
   * @param flightOfferData Datos de la oferta a verificar
   * @return información actualizada de la oferta
   */
  @PostMapping("/verify-price")
  public ResponseEntity<Object> verifyPrice(@RequestBody Object flightOfferData) {

    log.info("Verificando precio de oferta de vuelo");

    try {
      Object verifiedOffer = flightBookingService.verifyFlightOfferPrice(flightOfferData);
      return ResponseEntity.ok(verifiedOffer);
    } catch (Exception e) {
      log.error("Error al verificar precio: {}", e.getMessage());
      return ResponseEntity.badRequest().body(
              Map.of("error", "Oferta no disponible", "message", e.getMessage())
      );
    }
  }
}
