package masera.deviajebookingsandpayments.services.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
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

  /**
   * Construye el contenido HTML del email del voucher.
   */
  private String buildVoucherEmailContent(String bookingReference, String holderName) {
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
            + "<p><strong>Importante:</strong></p>"
            + "<ul>"
            + "<li>Guarda este voucher para presentarlo en tu check-in</li>"
            + "<li>Verifica que todos los datos sean correctos</li>"
            + "<li>Revisa las políticas de cancelación adjuntas</li>"
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