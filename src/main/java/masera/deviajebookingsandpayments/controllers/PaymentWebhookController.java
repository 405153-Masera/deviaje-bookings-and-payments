package masera.deviajebookingsandpayments.controllers;

import java.math.BigDecimal;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import masera.deviajebookingsandpayments.entities.BookingEntity;
import masera.deviajebookingsandpayments.entities.PaymentEntity;
import masera.deviajebookingsandpayments.entities.RefundEntity;
import masera.deviajebookingsandpayments.repositories.BookingRepository;
import masera.deviajebookingsandpayments.repositories.PaymentRepository;
import masera.deviajebookingsandpayments.repositories.RefundRepository;
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
 * Procesa notificaciones de MercadoPago para pagos y reembolsos.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments/webhook")
@Slf4j
public class PaymentWebhookController {

  private final PaymentRepository paymentRepository;

  private final BookingRepository bookingRepository;

  private final RefundRepository refundRepository;

  private final PaymentService paymentService;

  private final VoucherService voucherService;

  private final EmailService emailService;

  /**
   * Recibe notificaciones de MercadoPago sobre cambios en pagos y reembolsos.
   * Este es el ÚNICO endpoint que debe configurarse en el panel de MercadoPago.
   *
   * @param request datos de la notificación
   * @return respuesta al servicio de notificaciones
   */
  @PostMapping
  public ResponseEntity<Map<String, String>> handleMercadoPagoWebhook(
          @RequestBody Map<String, Object> request) {

    log.info("Recibida notificación de MercadoPago: {}", request);

    try {
      String type = (String) request.get("type");
      String action = (String) request.get("action");

      log.info("Webhook - type: {}, action: {}", type, action);

      // Obtener datos del pago
      if (!request.containsKey("data")) {
        log.warn("Notificación sin campo 'data'");
        return ResponseEntity.ok(Map.of("status", "OK", "message", "No data field"));
      }

      @SuppressWarnings("unchecked")
      Map<String, Object> data = (Map<String, Object>) request.get("data");

      if (!data.containsKey("id")) {
        log.warn("Notificación sin ID");
        return ResponseEntity.ok(Map.of("status", "OK", "message", "No ID"));
      }

      String paymentId = data.get("id").toString();

      // Diferenciar según el tipo de evento
      if ("payment".equals(type) && action == null) {
        // Pago nuevo aprobado → generar voucher y enviar email
        log.info("Procesando webhook de pago para ID: {}", paymentId);
        updatePaymentStatusAndGenerateVoucher(paymentId);
      } else if ("payment.updated".equals(action) || "refund".equals(type)) {
        // Refund confirmado → enviar email de cancelación
        log.info("Procesando webhook de refund para pago ID: {}", paymentId);
        processRefundConfirmation(paymentId);
      } else {
        log.info("Tipo de notificación ignorado: type={}, action={}", type, action);
      }

      return ResponseEntity.ok(Map.of("status", "OK"));

    } catch (Exception e) {
      log.error("Error al procesar webhook de MercadoPago", e);
      // Siempre devolver 200 OK para que MercadoPago no reintente
      return ResponseEntity.ok(Map.of("status", "ERROR", "message", e.getMessage()));
    }
  }

  /**
   * Actualiza el estado de un pago y genera el voucher si está aprobado.
   *
   * @param mercadoPagoPaymentId id del pago en MercadoPago
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
        log.warn("Pago con ID externo {} no encontrado en nuestra BD", mercadoPagoPaymentId);
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

  /**
   * Procesa la confirmación de un reembolso desde MercadoPago.
   *
   * @param mercadoPagoPaymentId ID del pago en MercadoPago
   */
  private void processRefundConfirmation(String mercadoPagoPaymentId) {
    try {
      log.info("Procesando confirmación de refund para pago: {}", mercadoPagoPaymentId);

      // 1. Consultar el estado actual del pago en MercadoPago
      var paymentResponse = paymentService.checkExternalPaymentStatus(mercadoPagoPaymentId);
      log.info("Estado del pago en MercadoPago: {}", paymentResponse.getStatus());

      // 2. Verificar si el pago fue reembolsado
      if (!"REFUNDED".equals(paymentResponse.getStatus())) {
        log.info("Pago no está reembolsado, estado: {}. No se envía email de cancelación.",
                paymentResponse.getStatus());
        return;
      }

      // 3. Buscar el pago en nuestra BD
      var paymentOpt = paymentRepository.findByExternalPaymentId(mercadoPagoPaymentId);

      if (paymentOpt.isEmpty()) {
        log.warn("Pago con ID externo {} no encontrado en nuestra BD", mercadoPagoPaymentId);
        return;
      }

      PaymentEntity payment = paymentOpt.get();

      // 4. Obtener el booking asociado
      BookingEntity booking = payment.getBookingEntity();

      if (booking == null) {
        log.error("El pago {} no tiene un booking asociado", payment.getId());
        return;
      }

      // 5. Verificar que el booking esté cancelado
      if (!BookingEntity.BookingStatus.CANCELLED.equals(booking.getStatus())) {
        log.warn("El booking {} no está cancelado, estado: {}",
                booking.getBookingReference(), booking.getStatus());
        return;
      }

      // 6. Enviar email de confirmación de cancelación
      sendCancellationEmail(booking, payment);

    } catch (Exception e) {
      log.error("Error al procesar confirmación de refund {}: {}",
              mercadoPagoPaymentId, e.getMessage(), e);
    }
  }

  /**
   * Envía el email de confirmación de cancelación con el monto real del reembolso.
   *
   * @param booking entidad de reserva cancelada
   * @param payment entidad de pago reembolsado
   */
  private void sendCancellationEmail(BookingEntity booking, PaymentEntity payment) {
    try {
      log.info("Enviando email de cancelación para booking: {}", booking.getBookingReference());

      // Buscar el refund asociado para obtener el monto real reembolsado
      RefundEntity refund = refundRepository.findByBookingEntityId(booking.getId())
              .orElseThrow();

      emailService.sendCancellationEmail(
              booking.getEmail(),
              booking.getBookingReference(),
              booking.getHolderName(),
              booking.getType().name(),
              refund.getAmount(),
              booking.getCurrency(),
              booking.getCancelledAt()
      );

      log.info("Email de cancelación enviado exitosamente a: {}", booking.getEmail());

    } catch (Exception e) {
      log.error("Error al enviar email de cancelación para booking {}: {}",
              booking.getBookingReference(), e.getMessage(), e);
    }
  }
}