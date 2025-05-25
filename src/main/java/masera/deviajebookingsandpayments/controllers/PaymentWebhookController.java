package masera.deviajebookingsandpayments.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import masera.deviajebookingsandpayments.entities.Payment;
import masera.deviajebookingsandpayments.repositories.PaymentRepository;
import masera.deviajebookingsandpayments.services.interfaces.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * Controlador para webhooks de procesadores de pago.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments/webhook")
@Slf4j
public class PaymentWebhookController {

  private final PaymentRepository paymentRepository;
  private final PaymentService paymentService;

  /**
   * Recibe notificaciones de Mercado Pago sobre cambios en los pagos.
   *
   * @param request datos de la notificación
   * @return respuesta al servicio de notificaciones
   */
  @PostMapping
  public ResponseEntity<Map<String, String>> handleMercadoPagoWebhook(@RequestBody Map<String, Object> request) {
    log.info("Recibida notificación de Mercado Pago: {}", request);

    try {
      // Verificar tipo de notificación
      if (request.containsKey("type") && "payment".equals(request.get("type"))) {
        // Obtener datos del pago
        if (request.containsKey("data") && ((Map)request.get("data")).containsKey("id")) {
          String paymentId = ((Map)request.get("data")).get("id").toString();

          // Actualizar el estado del pago en nuestra base de datos
          updatePaymentStatus(paymentId);
        }
      }

      return ResponseEntity.ok(Map.of("status", "OK"));
    } catch (Exception e) {
      log.error("Error al procesar webhook de Mercado Pago", e);
      return ResponseEntity.ok(Map.of("status", "ERROR", "message", e.getMessage()));
    }
  }

  /**
   * Maneja la redirección de Mercado Pago tras completar un pago.
   *
   * @param paymentId ID del pago en Mercado Pago
   * @param status Estado del pago
   * @return respuesta que será procesada por el frontend
   */
  @GetMapping("/success")
  public ResponseEntity<Map<String, Object>> handlePaymentSuccess(
          @RequestParam("payment_id") String paymentId,
          @RequestParam("status") String status) {

    log.info("Redirección exitosa de Mercado Pago - Payment ID: {}, Status: {}", paymentId, status);

    // Actualizar estado en nuestra BD
    updatePaymentStatus(paymentId);

    return ResponseEntity.ok(Map.of(
            "success", true,
            "payment_id", paymentId,
            "status", status
    ));
  }

  /**
   * Maneja la redirección de Mercado Pago tras un pago fallido.
   *
   * @param paymentId ID del pago en Mercado Pago (puede ser null)
   * @param status Estado del pago
   * @return respuesta que será procesada por el frontend
   */
  @GetMapping("/failure")
  public ResponseEntity<Map<String, Object>> handlePaymentFailure(
          @RequestParam(value = "payment_id", required = false) String paymentId,
          @RequestParam("status") String status) {

    log.info("Redirección fallida de Mercado Pago - Payment ID: {}, Status: {}", paymentId, status);

    if (paymentId != null) {
      updatePaymentStatus(paymentId);
    }

    return ResponseEntity.ok(Map.of(
            "success", false,
            "payment_id", paymentId != null ? paymentId : "",
            "status", status
    ));
  }

  /**
   * Maneja la redirección de Mercado Pago para pagos pendientes.
   *
   * @param paymentId ID del pago en Mercado Pago
   * @param status Estado del pago
   * @return respuesta que será procesada por el frontend
   */
  @GetMapping("/pending")
  public ResponseEntity<Map<String, Object>> handlePaymentPending(
          @RequestParam("payment_id") String paymentId,
          @RequestParam("status") String status) {

    log.info("Redirección pendiente de Mercado Pago - Payment ID: {}, Status: {}", paymentId, status);

    // Actualizar estado en nuestra BD
    updatePaymentStatus(paymentId);

    return ResponseEntity.ok(Map.of(
            "success", true,
            "payment_id", paymentId,
            "status", status,
            "pending", true
    ));
  }

  /**
   * Actualiza el estado de un pago en nuestra base de datos.
   *
   * @param mercadoPagoPaymentId ID del pago en Mercado Pago
   */
  private void updatePaymentStatus(String mercadoPagoPaymentId) {
    try {
      // Verificar si el pago ya existe en nuestra BD
      Optional<Payment> paymentOpt = paymentRepository.findByExternalPaymentId(mercadoPagoPaymentId);

      if (paymentOpt.isPresent()) {
        // Actualizar estado consultando a Mercado Pago
        paymentService.checkExternalPaymentStatus(mercadoPagoPaymentId);
      } else {
        log.warn("Pago con ID externo {} no encontrado en nuestra base de datos", mercadoPagoPaymentId);
      }
    } catch (Exception e) {
      log.error("Error al actualizar estado del pago {}", mercadoPagoPaymentId, e);
    }
  }
}