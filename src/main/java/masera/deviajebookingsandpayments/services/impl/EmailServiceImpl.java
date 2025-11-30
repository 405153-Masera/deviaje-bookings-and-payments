package masera.deviajebookingsandpayments.services.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import masera.deviajebookingsandpayments.services.interfaces.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


/**
 * Implementación del servicio de envío de correos electrónicos.
 */
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

  private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

  @Value("${deviaje.app.email.enabled:true}")
  private boolean emailEnabled;

  @Value("${spring.mail.username}")
  private String fromEmail;

  @Value("${deviaje.app.frontend-url}")
  private String frontendUrl;

  private final JavaMailSender mailSender;

  @Override
  @Async
  public void sendEmail(String to, String subject, String content) throws Exception {
    if (!emailEnabled) {
      logger.info("Email sending is disabled. "
              + "Would have sent email to {} with subject: {}", to, subject);
      logger.debug("Email content: {}", content);
      return;
    }

    logger.info("Sending email to {} with subject: {}", to, subject);

    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

      helper.setFrom(fromEmail);
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(content, true); // true indica que el contenido es HTML

      mailSender.send(message);
      logger.info("Email enviado exitosamente a: {}", to);
    } catch (MessagingException e) {
      logger.error("Error al enviar email: {}", e.getMessage(), e);
      throw new Exception("Error al enviar email: " + e.getMessage());
    }
  }

  @Override
  @Async
  public void sendEmailWithAttachment(String to, String subject, String content,
                                      byte[] attachmentData, String attachmentName)
          throws Exception {
    if (!emailEnabled) {
      logger.info("Email sending is disabled. "
              + "Would have sent email with attachment to {} with subject: {}", to, subject);
      return;
    }

    logger.info("Sending email with attachment to {} with subject: {}", to, subject);

    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

      helper.setFrom(fromEmail);
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(content, true); // true indica que el contenido es HTML

      // Agregar el archivo adjunto
      ByteArrayDataSource dataSource = new ByteArrayDataSource(attachmentData, "application/pdf");
      helper.addAttachment(attachmentName, dataSource);

      mailSender.send(message);
      logger.info("Email con adjunto enviado exitosamente a: {}", to);
    } catch (MessagingException e) {
      logger.error("Error al enviar email con adjunto: {}", e.getMessage(), e);
      throw new Exception("Error al enviar email con adjunto: " + e.getMessage());
    }
  }

  @Override
  public void sendBookingVoucher(String to, String bookingReference,
                                 String holderName, byte[] voucherPdf) throws Exception {
    String subject = "Tu Voucher de Reserva - DeViaje | Código: " + bookingReference;

    String content = buildVoucherEmailContent(bookingReference, holderName);

    String attachmentName = "Voucher_" + bookingReference + ".pdf";

    sendEmailWithAttachment(to, subject, content, voucherPdf, attachmentName);
  }

  @Override
  public void sendCancellationEmail(
          String to,
          String bookingReference,
          String holderName,
          String bookingType,
          BigDecimal refundAmount,
          String currency,
          LocalDateTime cancelledAt) {

    try {

      // Formatear fecha
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
      String formattedDate = cancelledAt.format(formatter);

      // Formatear monto
      String formattedAmount = String.format("%,.2f %s", refundAmount, currency);

      // Traducir tipo de reserva
      String bookingTypeSpanish = switch (bookingType) {
        case "FLIGHT" -> "Vuelo";
        case "HOTEL" -> "Hotel";
        case "PACKAGE" -> "Paquete";
        default -> bookingType;
      };

      // Construir el contenido HTML del email
      String htmlContent = buildCancellationEmailHtml(
              holderName,
              bookingReference,
              bookingTypeSpanish,
              formattedDate,
              formattedAmount
      );

      // Enviar el email
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

      helper.setFrom(fromEmail);
      helper.setTo(to);
      helper.setSubject("Confirmación de Cancelación - " + bookingReference);
      helper.setText(htmlContent, true);

      mailSender.send(message);

    } catch (Exception e) {
      throw new RuntimeException("Error al enviar email de cancelación", e);
    }
  }

  /**
   * Construye el HTML del email de cancelación.
   */
  private String buildCancellationEmailHtml(
          String holderName,
          String bookingReference,
          String bookingType,
          String cancelledDate,
          String refundAmount) {

    return "<!DOCTYPE html>"
            + "<html>"
            + "<head>"
            + "<meta charset='UTF-8'/>"
            + "<style>"
            + "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }"
            + ".container { max-width: 600px; margin: 0 auto; padding: 20px; }"
            + ".header { background-color: #8B5CF6; color: white; "
            + "padding: 20px; text-align: center; }"
            + ".header h1 { margin: 0; font-size: 24px; }"
            + ".content { background-color: #f9f9f9; padding: 30px; border: 1px solid #ddd; }"
            + ".info-box { background-color: white; padding: 20px; "
            + "margin: 20px 0; border-left: 4px solid #8B5CF6; }"
            + ".info-row { margin: 10px 0; }"
            + ".info-label { font-weight: bold; color: #666; }"
            + ".info-value { color: #333; }"
            + ".highlight { color: #8B5CF6; font-weight: bold; font-size: 18px; }"
            + ".footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }"
            + ".alert { background-color: #fff3cd; border: 1px solid #ffc107; "
            + "padding: 15px; margin: 20px 0; border-radius: 5px; }"
            + "</style>"
            + "</head>"
            + "<body>"
            + "<div class='container'>"
            +
            "<div class='header'>"
            + "<h1>DeViaje</h1>"
            + "<p style='margin: 5px 0;'>Confirmación de Cancelación</p>"
            + "</div>"
            +
            "<div class='content'>"
            + "<p>Estimado/a <strong>" + holderName + "</strong>,</p>"
            + "<p>Le confirmamos que su reserva ha sido cancelada exitosamente.</p>"

            + "<div class='info-box'>"
            + "<div class='info-row'>"
            + "<span class='info-label'>Código de Reserva:</span> "
            + "<span class='highlight'>" + bookingReference + "</span>"
            + "</div>"
            + "<div class='info-row'>"
            + "<span class='info-label'>Tipo:</span> "
            + "<span class='info-value'>" + bookingType + "</span>"
            + "</div>"
            + "<div class='info-row'>"
            + "<span class='info-label'>Fecha de Cancelación:</span> "
            + "<span class='info-value'>" + cancelledDate + "</span>"
            + "</div>"
            + "<div class='info-row'>"
            + "<span class='info-label'>Monto a Reembolsar:</span> "
            + "<span class='highlight'>" + refundAmount + "</span>"
            + "</div>"
            + "</div>"
            + "<div class='alert'>"
            + "<strong>⏱️ Tiempo de Procesamiento:</strong><br/>"
            + "El reembolso será acreditado en su medio de pago "
            + "original en un plazo de 5 a 10 días hábiles, "
            + "dependiendo de su entidad bancaria."
            + "</div>"
            + "<p><strong>Nota importante:</strong> La comisión de agencia no es reembolsable.</p>"
            + "<p>Si tiene alguna consulta, no dude en contactarnos.</p>"
            + "<p>Saludos cordiales,<br/><strong>Equipo DeViaje</strong></p>"
            + "</div>"
            + "<div class='footer'>"
            + "<p>Este es un correo automático, por favor no responda a este mensaje.</p>"
            + "<p>&copy; 2024 DeViaje. Todos los derechos reservados.</p>"
            + "</div>"
            + "</div>"
            + "</body>"
            + "</html>";
  }


  /**
   * Construye el contenido HTML del email del voucher.
   */
  private String buildVoucherEmailContent(String bookingReference, String holderName) {
    // URL para que invitados puedan ver/cancelar la reserva
    String bookingUrl = "https://localhost:4200/bookings/" + bookingReference + "/details";

    return "<html>"
            + "<head>"
            + "<style>"
            + "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }"
            + ".container { max-width: 600px; margin: 0 auto; }"
            + ".header { background-color: #8B5CF6; padding: 20px; text-align: center; }"
            + ".header h1 { color: white; margin: 0; }"
            + ".content { padding: 20px; border: 1px solid #ddd; border-top: none; }"
            + ".highlight { background-color: #f3f4f6; padding: 15px; "
            + "border-radius: 5px; margin: 20px 0; }"
            + ".booking-code { font-size: 24px; font-weight: bold; "
            + "color: #8B5CF6; text-align: center; }"
            + ".booking-link { display: block; text-align: center; margin: 20px 0; }"
            + ".booking-link a { color: #8B5CF6; text-decoration: none; font-weight: bold; }"
            + ".footer { margin-top: 30px; font-size: 12px; "
            + "color: #666; text-align: center; }"
            + "</style>"
            + "</head>"
            + "<body>"
            + "<div class='container'>"
            + "<div class='header'>"
            + "<h1>DeViaje</h1>"
            + "</div>"
            + "<div class='content'>"
            + "<h2>¡Tu reserva está confirmada!</h2>"
            + "<p>Hola <strong>" + holderName + "</strong>,</p>"
            + "<p>Nos complace confirmar tu reserva. Encontrarás todos los detalles "
            + "en el voucher adjunto a este correo.</p>"
            + "<div class='highlight'>"
            + "<p style='margin: 0; text-align: center;'>Código de Reserva</p>"
            + "<p class='booking-code'>" + bookingReference + "</p>"
            + "</div>"
            + "<div class='booking-link'>"
            + "<p>Para ver los detalles de tu reserva o gestionar cambios, accede a:</p>"
            + "<a href='" + bookingUrl + "'>" + bookingUrl + "</a>"
            + "</div>"
            + "<p><strong>Importante:</strong></p>"
            + "<ul>"
            + "<li>Guarda este voucher para presentarlo en tu check-in</li>"
            + "<li>Verifica que todos los datos sean correctos</li>"
            + "<li>Revisa las políticas de cancelación en la web</li>"
            + "</ul>"
            + "<p>Si tienes alguna consulta, no dudes en contactarnos.</p>"
            + "<p>¡Que tengas un excelente viaje!</p>"
            + "<p style='margin-top: 30px;'>"
            + "<strong>Equipo DeViaje</strong><br>"
            + "Te acompañamos antes, durante y después de tu viaje"
            + "</p>"
            + "<div class='footer'>"
            + "<p>Este es un correo automático, por favor no responder.<br>"
            + "© " + java.time.Year.now().getValue() + " DeViaje. "
            + "Todos los derechos reservados.</p>"
            + "</div>"
            + "</div>"
            + "</div>"
            + "</body>"
            + "</html>";
  }
}
