package masera.deviajebookingsandpayments.controllers;

import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import masera.deviajebookingsandpayments.dtos.bookings.hotels.BookHotelAndPayRequest;
import masera.deviajebookingsandpayments.dtos.responses.BaseResponse;
import masera.deviajebookingsandpayments.dtos.responses.HotelBookingResponseDto;
import masera.deviajebookingsandpayments.services.interfaces.HotelBookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador para reservas de hoteles.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hotels")
@Slf4j
public class HotelBookingController {

  private final HotelBookingService hotelBookingService;

  /**
   * Endpoint unificado: Reservar hotel y procesar pago.
   *
   * @param request datos de la reserva de hotel
   * @return respuesta unificada con reserva y pago
   */
  @PostMapping("/book-and-pay")
  public ResponseEntity<BaseResponse> bookHotelAndPay(
          @Valid @RequestBody BookHotelAndPayRequest request) {

    log.info("Iniciando reserva y pago de hotel para cliente: {}",
            request.getBookingRequest().getClientId());

    try {
      BaseResponse response = hotelBookingService.bookAndPay(
              request.getBookingRequest(),
              request.getPaymentRequest(),
              request.getPrices()
              );

      return ResponseEntity.ok(response);

    } catch (Exception e) {
      log.error("Error inesperado al procesar reserva de hotel", e);
      BaseResponse errorResponse = BaseResponse.builder()
              .success(false)
              .message("Error interno del servidor")
              .failureReason("INTERNAL_ERROR")
              .detailedError(e.getMessage())
              .build();
      return ResponseEntity.ok(errorResponse);
    }
  }

  /**
   * Obtiene los datos básicos de una reserva de hotel.
   *
   * @param id ID de la reserva de hotel
   * @return datos básicos del hotel reservado
   *
   */
  @GetMapping("/bookings/{id}")
  public ResponseEntity<HotelBookingResponseDto> getHotelBooking(@PathVariable Long id) {

    log.info("Obteniendo reserva de hotel básica: {}", id);

    try {
      HotelBookingResponseDto booking = hotelBookingService.getBasicBookingInfo(id);
      return ResponseEntity.ok(booking);
    } catch (Exception e) {
      log.error("Error al obtener reserva de hotel: {}", id, e);
      return ResponseEntity.notFound().build();
    }
  }

  /**
   * Obtiene los detalles completos de una reserva (llama a HotelBeds API).
   *
   * @param id ID de la reserva de hotel
   * @return detalles completos desde HotelBeds
   */
  @GetMapping("/bookings/{id}/details")
  public ResponseEntity<Object> getHotelBookingDetails(@PathVariable Long id) {

    log.info("Obteniendo detalles completos de reserva de hotel: {}", id);

    try {
      Object fullDetails = hotelBookingService.getFullBookingDetails(id);
      return ResponseEntity.ok(fullDetails);
    } catch (Exception e) {
      log.error("Error al obtener detalles de reserva de hotel: {}", id, e);
      return ResponseEntity.notFound().build();
    }
  }

  /**
   * Verifica disponibilidad y precio de una tarifa de hotel.
   *
   * @param rateKey clave de la tarifa a verificar
   * @return información actualizada de la tarifa
   *
   */
  @GetMapping("/checkrates")
  public ResponseEntity<Object> checkRates(@RequestParam String rateKey) {

    log.info("Verificando tarifa de hotel: {}", rateKey);

    try {
      Object rateInfo = hotelBookingService.checkRates(rateKey);
      return ResponseEntity.ok(rateInfo);
    } catch (Exception e) {
      log.error("Error al verificar tarifa: {}", rateKey, e);
      return ResponseEntity.badRequest().body(
              Map.of("error", "Tarifa no disponible", "message", e.getMessage())
      );
    }
  }
}