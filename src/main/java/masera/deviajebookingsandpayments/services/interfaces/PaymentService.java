package masera.deviajebookingsandpayments.services.interfaces;

import masera.deviajebookingsandpayments.dtos.payments.PaymentRequestDto;
import masera.deviajebookingsandpayments.dtos.responses.PaymentResponseDto;

import java.util.UUID;

/**
 * Interfaz para el servicio de pagos.
 */
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
   * Procesa reembolsos para una reserva.
   *
   * @param bookingId ID de la reserva
   * @return resultado del reembolso
   */
  PaymentResponseDto processRefundForBooking(UUID bookingId);

  /**
   * Verifica el estado de un pago.
   *
   * @param paymentId ID del pago
   * @return estado actualizado del pago
   */
  PaymentResponseDto checkPaymentStatus(Long paymentId);

  /**
   * Verifica el estado de un pago externo por su ID.
   *
   * @param externalPaymentId ID externo del pago
   * @return estado actualizado del pago
   */
  PaymentResponseDto checkExternalPaymentStatus(String externalPaymentId);
}