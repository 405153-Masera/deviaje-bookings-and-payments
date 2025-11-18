package masera.deviajebookingsandpayments.controllers;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import masera.deviajebookingsandpayments.entities.BookingEntity;
import masera.deviajebookingsandpayments.entities.PaymentEntity;
import masera.deviajebookingsandpayments.repositories.BookingRepository;
import masera.deviajebookingsandpayments.repositories.PaymentRepository;
import masera.deviajebookingsandpayments.services.interfaces.EmailService;
import masera.deviajebookingsandpayments.services.interfaces.PaymentService;
import masera.deviajebookingsandpayments.services.interfaces.VoucherService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador para webhooks de procesadores de pago.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments/webhook")
@Slf4j
public class PaymentWebhookController {

  private final PaymentRepository paymentRepository;
  private final BookingRepository bookingRepository;
  private final PaymentService paymentService;
  private final VoucherService voucherService;
  private final EmailService emailService;

  /**
   * Recibe notificaciones de Mercado Pago sobre cambios en los pagos.
   *
   * @param request datos de la notificación
   * @return respuesta al servicio de notificaciones
   */
  @PostMapping
  public ResponseEntity<Map<String, String>> handleMercadoPagoWebhook(
          @RequestBody Map<String, Object> request) {

    log.info("Recibida notificación de Mercado Pago: {}", request);

    try {
      // Verificar tipo de notificación
      if (!"payment".equals(request.get("type"))) {
        log.info("Tipo de notificación ignorado: {}", request.get("type"));
        return ResponseEntity.ok(Map.of("status", "OK", "message", "Ignored notification type"));
      }

      // Obtener datos del pago
      if (!request.containsKey("data")) {
        log.warn("Notificación sin campo 'data'");
        return ResponseEntity.ok(Map.of("status", "OK", "message", "No data field"));
      }

      @SuppressWarnings("unchecked")
      Map<String, Object> data = (Map<String, Object>) request.get("data");

      if (!data.containsKey("id")) {
        log.warn("Notificación sin ID de pago");
        return ResponseEntity.ok(Map.of("status", "OK", "message", "No payment ID"));
      }

      String paymentId = data.get("id").toString();
      log.info("Procesando webhook para pago ID: {}", paymentId);

      // Actualizar el estado del pago en nuestra base de datos
      updatePaymentStatusAndGenerateVoucher(paymentId);

      return ResponseEntity.ok(Map.of("status", "OK"));

    } catch (Exception e) {
      log.error("Error al procesar webhook de Mercado Pago", e);
      // Siempre devolver 200 OK para que MercadoPago no reintente
      return ResponseEntity.ok(Map.of("status", "ERROR", "message", e.getMessage()));
    }
  }

  /**
   * Actualiza el estado de un pago y genera el voucher si está aprobado.
   *
   * @param mercadoPagoPaymentId id del pago en Mercado Pago
   */
  private void updatePaymentStatusAndGenerateVoucher(String mercadoPagoPaymentId) {
    try {
      log.info("Actualizando estado del pago: {}", mercadoPagoPaymentId);

      // 1. Consultar el estado actual del pago en MercadoPago
      var paymentResponse = paymentService.checkExternalPaymentStatus(mercadoPagoPaymentId);

      log.info("Estado del pago en MercadoPago: {}", paymentResponse.getStatus());

      // 2. Buscar el pago en nuestra BD
      var paymentOpt = paymentRepository.findByExternalPaymentId(mercadoPagoPaymentId);

      if (paymentOpt.isEmpty()) {
        log.warn("Pago con ID externo {} no encontrado en nuestra base de datos",
                mercadoPagoPaymentId);
        return;
      }

      PaymentEntity payment = paymentOpt.get();

      // 3. Verificar si el pago fue aprobado
      if (!"APPROVED".equals(paymentResponse.getStatus())) {
        log.info("Pago no aprobado, estado: {}. No se genera voucher.",
                paymentResponse.getStatus());
        return;
      }

      // 4. Obtener el booking asociado
      BookingEntity booking = payment.getBookingEntity();

      if (booking == null) {
        log.error("El pago {} no tiene un booking asociado", payment.getId());
        return;
      }

      // 5. Verificar si ya tiene voucher
      if (booking.getVoucher() != null) {
        log.info("El booking {} ya tiene voucher generado", booking.getBookingReference());

        // Si tiene voucher, pero no se envió, intentar enviar
        if (Boolean.FALSE.equals(booking.getIsSent())) {
          log.info("Voucher existe pero no fue enviado. Intentando envío...");
          sendVoucherEmail(booking);
        }
        return;
      }

      // 6. Generar el voucher
      log.info("Generando voucher para booking: {}", booking.getBookingReference());
      byte[] voucherPdf = voucherService.generateVoucher(booking);

      // 7. Guardar el voucher en la BD
      booking.setVoucher(voucherPdf);
      bookingRepository.save(booking);
      log.info("Voucher guardado en BD para booking: {}", booking.getBookingReference());

      // 8. Enviar el voucher por email
      sendVoucherEmail(booking);

    } catch (Exception e) {
      log.error("Error al actualizar estado del pago {}: {}",
              mercadoPagoPaymentId, e.getMessage(), e);
    }
  }

  /**
   * Envía el voucher por email al cliente.
   *
   * @param booking entidad de reserva con el voucher
   */
  private void sendVoucherEmail(BookingEntity booking) {
    try {
      log.info("Enviando voucher por email para booking: {}", booking.getBookingReference());

      emailService.sendBookingVoucher(
              booking.getEmail(),
              booking.getBookingReference(),
              booking.getHolderName(),
              booking.getVoucher()
      );

      // Marcar como enviado
      booking.setIsSent(true);
      bookingRepository.save(booking);

      log.info("Voucher enviado exitosamente a: {}", booking.getEmail());

    } catch (Exception e) {
      log.error("Error al enviar voucher por email para booking {}: {}",
              booking.getBookingReference(), e.getMessage(), e);

      // No marcar como enviado para que el proceso automático lo reintente
      booking.setIsSent(false);
      bookingRepository.save(booking);
    }
  }
}