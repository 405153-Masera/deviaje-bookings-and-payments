package masera.deviajebookingsandpayments.services.interfaces;

import java.math.BigDecimal;
import masera.deviajebookingsandpayments.dtos.payments.PaymentRequestDto;
import masera.deviajebookingsandpayments.dtos.responses.PaymentResponseDto;
import org.springframework.stereotype.Service;

/**
 * Interfaz para el servicio de pagos.
 */
@Service
public interface PaymentService {

  /**
   * Procesa un nuevo pago.
   *
   * @param paymentRequest datos del pago a procesar
   * @return resultado del procesamiento del pago
   */
  PaymentResponseDto processPayment(PaymentRequestDto paymentRequest);

  /**
   * Reembolsa un pago existente.
   *
   * @param paymentId ID del pago a reembolsar
   * @return resultado del reembolso
   */
  PaymentResponseDto refundPayment(Long paymentId);

  /**
   * Procesa un reembolso para un booking.
   *
   * @param bookingId ID del booking
   * @param refundAmount Monto a reembolsar (calculado por CancellationService)
   */
  void processRefundForBooking(Long bookingId, BigDecimal refundAmount);

  /**
   * Verifica el estado de un pago.
   *
   * @param paymentId id del pago
   * @return estado actualizado del pago
   */
  PaymentResponseDto checkPaymentStatus(Long paymentId);

  /**
   * Verifica el estado de un pago externo por su ID.
   *
   * @param externalPaymentId id externo del pago
   * @return estado actualizado del pago
   */
  PaymentResponseDto checkExternalPaymentStatus(String externalPaymentId);

  /**
   * Verifica si un pago tiene refunds aprobados consultando directamente a MercadoPago.
   *
   * @param externalPaymentId ID del pago en MercadoPago
   * @return true si tiene al menos un refund aprobado, false caso contrario
   */
  boolean hasApprovedRefund(String externalPaymentId);
}