package masera.deviajebookingsandpayments.services.impl;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import masera.deviajebookingsandpayments.entities.BookingEntity;
import masera.deviajebookingsandpayments.entities.PaymentEntity;
import masera.deviajebookingsandpayments.repositories.BookingRepository;
import masera.deviajebookingsandpayments.services.interfaces.EmailService;
import masera.deviajebookingsandpayments.services.interfaces.VoucherService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio para procesar automáticamente vouchers pendientes.
 * Se ejecuta periódicamente para garantizar que todos los vouchers se generen y envíen.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VoucherScheduledService {

  private final BookingRepository bookingRepository;
  private final VoucherService voucherService;
  private final EmailService emailService;

  /**
   * Procesa vouchers pendientes cada 30 minutos.
   * Busca bookings con pagos aprobados que no tienen voucher o no se enviaron.
   */
  @Scheduled(fixedDelayString = "${deviaje.voucher.scheduler.fixed-delay:1800000}") // 30 min
  @Transactional
  public void processPendingVouchers() {
    log.info("Iniciando proceso automático de vouchers pendientes...");

    try {
      // 1. Buscar bookings confirmados sin voucher o no enviados
      List<BookingEntity> pendingBookings = findPendingBookings();

      if (pendingBookings.isEmpty()) {
        log.info("No hay vouchers pendientes para procesar");
        return;
      }

      log.info("Encontrados {} bookings pendientes de procesar", pendingBookings.size());

      int successCount = 0;
      int failCount = 0;

      // 2. Procesar cada booking
      for (BookingEntity booking : pendingBookings) {
        try {
          processBookingVoucher(booking);
          successCount++;
        } catch (Exception e) {
          log.error("Error al procesar booking {}: {}",
                  booking.getBookingReference(), e.getMessage(), e);
          failCount++;
        }
      }

      log.info("Proceso completado. Exitosos: {}, Fallidos: {}", successCount, failCount);

    } catch (Exception e) {
      log.error("Error en el proceso automático de vouchers: {}", e.getMessage(), e);
    }
  }

  /**
   * Busca bookings que necesitan procesamiento de voucher.
   * Incluye bookings con pagos aprobados pero sin voucher o sin enviar.
   */
  private List<BookingEntity> findPendingBookings() {
    // Buscar bookings confirmados
    List<BookingEntity> confirmedBookings =
            bookingRepository.findByStatus(BookingEntity.BookingStatus.CONFIRMED);

    // Filtrar los que necesitan procesamiento
    return confirmedBookings.stream()
            .filter(this::needsVoucherProcessing)
            .toList();
  }

  /**
   * Verifica si un booking necesita procesamiento de voucher.
   */
  private boolean needsVoucherProcessing(BookingEntity booking) {
    // 1. Verificar si tiene pagos aprobados
    boolean hasApprovedPayment = booking.getPaymentEntities() != null
            && booking.getPaymentEntities().stream()
            .anyMatch(p -> PaymentEntity.PaymentStatus.APPROVED.equals(p.getStatus()));

    if (!hasApprovedPayment) {
      return false;
    }

    // 2. Verificar si necesita voucher
    boolean needsVoucher = booking.getVoucher() == null;

    // 3. Verificar si necesita envío
    boolean needsSending = Boolean.FALSE.equals(booking.getIsSent());

    return needsVoucher || needsSending;
  }

  /**
   * Procesa el voucher de un booking específico.
   */
  private void processBookingVoucher(BookingEntity booking) throws Exception {
    log.info("Procesando booking: {}", booking.getBookingReference());

    // 1. Generar voucher si no existe
    if (booking.getVoucher() == null) {
      log.info("Generando voucher para booking: {}", booking.getBookingReference());

      byte[] voucherPdf = voucherService.generateVoucher(booking);
      booking.setVoucher(voucherPdf);
      bookingRepository.save(booking);

      log.info("Voucher generado y guardado para: {}", booking.getBookingReference());
    }

    // 2. Enviar por email si no se ha enviado
    if (Boolean.FALSE.equals(booking.getIsSent())) {
      log.info("Enviando voucher por email para: {}", booking.getBookingReference());

      try {
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
        log.error("Error al enviar voucher por email: {}", e.getMessage(), e);
        // No marcar como enviado para que se reintente en la próxima ejecución
        throw e;
      }
    }
  }

  /**
   * Método manual para reprocesar un booking específico.
   * Útil para debugging o forzar reenvío.
   */
  @Transactional
  public void reprocessBooking(Long bookingId) throws Exception {
    log.info("Reprocesando manualmente booking ID: {}", bookingId);

    BookingEntity booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new Exception("Booking no encontrado con ID: " + bookingId));

    // Forzar regeneración del voucher
    booking.setVoucher(null);
    booking.setIsSent(false);
    bookingRepository.save(booking);

    // Procesar
    processBookingVoucher(booking);

    log.info("Booking reprocesado exitosamente: {}", booking.getBookingReference());
  }
}