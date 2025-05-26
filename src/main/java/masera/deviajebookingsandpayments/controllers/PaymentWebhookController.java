package masera.deviajebookingsandpayments.controllers;

import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import masera.deviajebookingsandpayments.entities.Payment;
import masera.deviajebookingsandpayments.repositories.PaymentRepository;
import masera.deviajebookingsandpayments.services.interfaces.PaymentService;
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
  private final PaymentService paymentService;

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
      if (request.containsKey("type") && "payment".equals(request.get("type"))) {
        // Obtener datos del pago
        if (request.containsKey("data") && ((Map) request.get("data")).containsKey("id")) {
          String paymentId = ((Map) request.get("data")).get("id").toString();

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
   * Actualiza el estado de un pago en nuestra base de datos.
   *
   * @param mercadoPagoPaymentId id del pago en Mercado Pago
   */
  private void updatePaymentStatus(String mercadoPagoPaymentId) {
    try {
      // Verificar si el pago ya existe en nuestra BD
      Optional<Payment> paymentOpt = paymentRepository
              .findByExternalPaymentId(mercadoPagoPaymentId);

      if (paymentOpt.isPresent()) {
        // Actualizar estado consultando a Mercado Pago
        paymentService.checkExternalPaymentStatus(mercadoPagoPaymentId);
      } else {
        log.warn("Pago con ID externo {} no encontrado en nuestra base de datos",
                mercadoPagoPaymentId);
      }
    } catch (Exception e) {
      log.error("Error al actualizar estado del pago {}", mercadoPagoPaymentId, e);
    }
  }
}