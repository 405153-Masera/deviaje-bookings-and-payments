package masera.deviajebookingsandpayments.controllers;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import masera.deviajebookingsandpayments.dtos.responses.BookingDetailsResponseDto;
import masera.deviajebookingsandpayments.dtos.responses.BookingResponseDto;
import masera.deviajebookingsandpayments.dtos.responses.CancellationResponseDto;
import masera.deviajebookingsandpayments.services.interfaces.BookingService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador para operaciones generales sobre reservas.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/bookings")
@Slf4j
public class BookingController {

  private final BookingService bookingService;

  /**
   * Obtiene todas las reservas de un cliente con filtros opcionales.
   */
  @GetMapping("/client/{clientId}")
  public ResponseEntity<List<BookingResponseDto>> getClientBookings(
          @PathVariable Integer clientId,
          @RequestParam(required = false) String email,
          @RequestParam(required = false) String holderName) {

    List<BookingResponseDto> bookings = bookingService.getClientBookings(
            clientId, email, holderName);

    return ResponseEntity.ok(bookings);
  }

  /**
   * Obtiene el historial de reservas de un agente con filtros opcionales.
   */
  @GetMapping("/agent/{agentId}")
  public ResponseEntity<List<BookingResponseDto>> getAgentBookings(
          @PathVariable Integer agentId,
          @RequestParam(required = false) Integer clientId,
          @RequestParam(required = false) String email,
          @RequestParam(required = false) String holderName) {

    List<BookingResponseDto> bookings = bookingService.getAgentBookings(
            agentId, clientId, email, holderName);

    return ResponseEntity.ok(bookings);
  }

  /**
   * Obtiene todas las reservas (solo administradores) con filtros opcionales.
   */
  @GetMapping("/admin/all")
  public ResponseEntity<List<BookingResponseDto>> getAllBookings(
          @RequestParam(required = false) Integer agentId,
          @RequestParam(required = false) Integer clientId,
          @RequestParam(required = false) String email,
          @RequestParam(required = false) String holderName) {

    List<BookingResponseDto> bookings = bookingService.getAllBookings(
            agentId, clientId, email, holderName);

    return ResponseEntity.ok(bookings);
  }

  /**
   * Obtiene una reserva específica (resumen).
   */
  @GetMapping("/{id}")
  public ResponseEntity<BookingResponseDto> getBooking(@PathVariable Long id) {
    log.info("GET /bookings/{}", id);

    BookingResponseDto booking = bookingService.getBookingById(id);
    return ResponseEntity.ok(booking);
  }

 /*
   * Obtiene los detalles completos de una reserva (incluyendo datos del JSON).
   *
  @GetMapping("/{bookingId}/details")
  public ResponseEntity<BookingDetailsResponseDto> getBookingDetails(
          @PathVariable Long bookingId) {

    BookingDetailsResponseDto details = bookingService.getBookingDetails(bookingId);
    return ResponseEntity.ok(details);
  }*/

  /**
   * Descarga el voucher de una reserva en formato PDF.
   */
  @GetMapping("/{bookingId}/voucher/download")
  public ResponseEntity<byte[]> downloadVoucher(@PathVariable Long bookingId) {
    log.info("GET /bookings/{}/voucher/download", bookingId);

    byte[] voucherPdf = bookingService.downloadVoucher(bookingId);
    String bookingReference = bookingService.getBookingReference(bookingId);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);
    headers.setContentDispositionFormData("attachment",
            "voucher-" + bookingReference + ".pdf");

    return ResponseEntity.ok()
            .headers(headers)
            .body(voucherPdf);
  }

  /**
   * Reenvía el voucher por email.
   */
  @PostMapping("/{bookingId}/voucher/resend")
  public ResponseEntity<String> resendVoucher(@PathVariable Long bookingId) {
    log.info("POST /bookings/{}/voucher/resend", bookingId);

    String result = bookingService.resendVoucher(bookingId);

    return ResponseEntity.ok(result);
  }

  /**
   * Obtiene la información de cancelación (política y monto de reembolso).
   */
  @GetMapping("/{bookingId}/cancellation-info")
  public ResponseEntity<CancellationResponseDto> getCancellationInfo(
          @PathVariable Long bookingId) {

    log.info("GET /bookings/{}/cancellation-info", bookingId);

    CancellationResponseDto info = bookingService.getCancellationInfo(bookingId);

    return ResponseEntity.ok(info);
  }

  /**
   * Cancela una reserva y procesa el reembolso si corresponde.
   */
  @PostMapping("/{bookingId}/cancel")
  public ResponseEntity<CancellationResponseDto> cancelBooking(
          @PathVariable Long bookingId,
          @RequestBody CancelBookingRequestDto request) {

    log.info("POST /bookings/{}/cancel", bookingId);

    CancellationResponseDto response = bookingService.cancelBooking(bookingId, request);

    return ResponseEntity.ok(response);
  }
}
