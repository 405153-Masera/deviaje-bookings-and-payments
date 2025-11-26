package masera.deviajebookingsandpayments.services.interfaces;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

/**
 * Interfaz para el servicio de envío de correos electrónicos.
 */
@Service
public interface EmailService {

  /**
   * Envía un correo electrónico de forma asíncrona.
   *
   * @param to destinatario del correo electrónico
   * @param subject asunto del correo electrónico
   * @param content contenido del correo electrónico (puede ser HTML)
   * @throws Exception si ocurre un error al enviar el correo electrónico
   */
  void sendEmail(String to, String subject, String content) throws Exception;

  /**
   * Envía un correo electrónico con un archivo adjunto (voucher PDF).
   *
   * @param to destinatario del correo electrónico
   * @param subject asunto del correo electrónico
   * @param content contenido del correo electrónico en HTML
   * @param attachmentData datos del archivo adjunto en bytes
   * @param attachmentName nombre del archivo adjunto
   * @throws Exception si ocurre un error al enviar el correo electrónico
   */
  void sendEmailWithAttachment(String to, String subject, String content,
                               byte[] attachmentData, String attachmentName) throws Exception;

  /**
   * Envía el voucher de reserva por email.
   *
   * @param to email del destinatario
   * @param bookingReference referencia de la reserva
   * @param holderName nombre del titular
   * @param voucherPdf PDF del voucher en bytes
   * @throws Exception si ocurre un error al enviar el email
   */
  void sendBookingVoucher(String to, String bookingReference,
                          String holderName, byte[] voucherPdf) throws Exception;

  /**
   * Envía un email de confirmación de cancelación de reserva.
   *
   * @param to email del destinatario
   * @param bookingReference referencia de la reserva
   * @param holderName nombre del titular
   * @param bookingType tipo de reserva (FLIGHT, HOTEL, PACKAGE)
   * @param refundAmount monto reembolsado
   * @param currency moneda del reembolso
   * @param cancelledAt fecha y hora de cancelación
   */
  void sendCancellationEmail(
          String to,
          String bookingReference,
          String holderName,
          String bookingType,
          BigDecimal refundAmount,
          String currency,
          LocalDateTime cancelledAt
  );
}
