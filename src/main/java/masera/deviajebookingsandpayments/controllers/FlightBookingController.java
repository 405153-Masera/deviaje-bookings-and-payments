package masera.deviajebookingsandpayments.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import masera.deviajebookingsandpayments.dtos.BookFlightAndPayRequest;
import masera.deviajebookingsandpayments.dtos.responses.FlightBookingDetailsDto;
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
  public ResponseEntity<String> bookFlightAndPay(
          @Valid @RequestBody BookFlightAndPayRequest request) {

    String response = flightBookingService.bookAndPay(request.getBookingRequest(),
              request.getPaymentRequest(), request.getPrices());

    return ResponseEntity.ok(response);
  }

  /**
   * Obtiene los datos básicos de una reserva de vuelo.
   *
   * @param id ID de la reserva de vuelo
   * @return datos básicos del vuelo reservado
   */
  @GetMapping("/bookings/{id}")
  public ResponseEntity<FlightBookingDetailsDto> getFlightBooking(@PathVariable Long id) {
    log.info("Obteniendo reserva de vuelo básica: {}", id);
    FlightBookingDetailsDto booking = flightBookingService.getBasicBookingInfo(id);
    return ResponseEntity.ok(booking);
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
    Object verifiedOffer = flightBookingService.verifyFlightOfferPrice(flightOfferData);
    return ResponseEntity.ok(verifiedOffer);
  }
}
