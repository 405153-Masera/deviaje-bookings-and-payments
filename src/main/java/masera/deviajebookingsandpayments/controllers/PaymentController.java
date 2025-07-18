package masera.deviajebookingsandpayments.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import masera.deviajebookingsandpayments.configs.PagoConfig;
import masera.deviajebookingsandpayments.dtos.payments.PaymentRequestDto;
import masera.deviajebookingsandpayments.dtos.responses.PaymentResponseDto;
import masera.deviajebookingsandpayments.services.interfaces.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador para operaciones de pago directas.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
@Slf4j
public class PaymentController {

  private final PaymentService paymentService;

  private final PagoConfig pagoConfig;

  /**
   * Procesa un pago directamente.
   *
   * @param paymentRequest datos del pago
   * @return resultado del procesamiento
   */
  @PostMapping("/process/payment")
  public ResponseEntity<PaymentResponseDto> processPayment(
          @Valid @RequestBody PaymentRequestDto paymentRequest) {

    log.info("Procesando pago por {} {}", paymentRequest.getAmount(), paymentRequest.getCurrency());

    try {
      PaymentResponseDto response = paymentService.processPayment(paymentRequest);

      if ("APPROVED".equals(response.getStatus())) {
        return ResponseEntity.ok(response);
      } else {
        return ResponseEntity.badRequest().body(response);
      }
    } catch (Exception e) {
      log.error("Error al procesar pago", e);
      PaymentResponseDto errorResponse = PaymentResponseDto.rejected(
              "PROCESSING_ERROR",
              "Error al procesar el pago: " + e.getMessage()
      );
      return ResponseEntity.internalServerError().body(errorResponse);
    }
  }

  /**
   * Verifica el estado de un pago.
   *
   * @param paymentId id del pago
   * @return estado actualizado del pago
   */
  @GetMapping("/{paymentId}")
  public ResponseEntity<PaymentResponseDto> checkPaymentStatus(@PathVariable Long paymentId) {
    log.info("Verificando estado de pago: {}", paymentId);

    try {
      PaymentResponseDto response = paymentService.checkPaymentStatus(paymentId);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Error al verificar estado de pago", e);
      PaymentResponseDto errorResponse = PaymentResponseDto.builder()
              .status("ERROR")
              .errorCode("CHECK_ERROR")
              .errorMessage("Error al verificar estado: " + e.getMessage())
              .build();
      return ResponseEntity.internalServerError().body(errorResponse);
    }
  }

  /**
   * Verifica el estado de un pago externo por su ID.
   *
   * @param externalPaymentId id externo del pago
   * @return estado actualizado del pago
   */
  @GetMapping("/external/{externalPaymentId}")
  public ResponseEntity<PaymentResponseDto> checkExternalPaymentStatus(
          @PathVariable String externalPaymentId) {

    log.info("Verificando estado de pago externo: {}", externalPaymentId);

    try {
      PaymentResponseDto response = paymentService.checkExternalPaymentStatus(externalPaymentId);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Error al verificar estado de pago externo", e);
      PaymentResponseDto errorResponse = PaymentResponseDto.builder()
              .status("ERROR")
              .errorCode("CHECK_ERROR")
              .errorMessage("Error al verificar estado: " + e.getMessage())
              .build();
      return ResponseEntity.internalServerError().body(errorResponse);
    }
  }

  /**
   * Procesa un reembolso para un pago.
   *
   * @param paymentId ID del pago a reembolsar
   * @return resultado del reembolso
   */
  @PostMapping("/{paymentId}/refund")
  public ResponseEntity<PaymentResponseDto> refundPayment(@PathVariable Long paymentId) {
    log.info("Procesando reembolso para pago: {}", paymentId);

    try {
      PaymentResponseDto response = paymentService.refundPayment(paymentId);

      if ("REFUNDED".equals(response.getStatus())) {
        return ResponseEntity.ok(response);
      } else {
        return ResponseEntity.badRequest().body(response);
      }
    } catch (Exception e) {
      log.error("Error al procesar reembolso", e);
      PaymentResponseDto errorResponse = PaymentResponseDto.builder()
              .status("ERROR")
              .errorCode("REFUND_ERROR")
              .errorMessage("Error al procesar reembolso: " + e.getMessage())
              .build();
      return ResponseEntity.internalServerError().body(errorResponse);
    }
  }

  /**
   * Obtiene la configuración pública de Mercado Pago.
   *
   * @return configuración pública
   */
  @GetMapping("/config")
  public ResponseEntity<Object> getPaymentConfig() {
    try {
      // Esta información es necesaria para que el frontend genere el token
      return ResponseEntity.ok(java.util.Map.of(
              "publicKey", pagoConfig.getPublicKey(),
              "countryCode", "AR",
              "preferredPaymentMethods", java.util.List.of("credit_card", "debit_card")
      ));
    } catch (Exception e) {
      log.error("Error al obtener configuración de pagos", e);
      return ResponseEntity.internalServerError().body(
              java.util.Map.of("error", "Error al obtener configuración de pagos")
      );
    }
  }
}