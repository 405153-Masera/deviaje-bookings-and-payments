package masera.deviajebookingsandpayments.services.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import masera.deviajebookingsandpayments.clients.HotelClient;
import masera.deviajebookingsandpayments.dtos.cancellations.CancelBookingRequestDto;
import masera.deviajebookingsandpayments.dtos.cancellations.CancelBookingResponseDto;
import masera.deviajebookingsandpayments.entities.BookingEntity;
import masera.deviajebookingsandpayments.entities.HotelBookingEntity;
import masera.deviajebookingsandpayments.repositories.BookingRepository;
import masera.deviajebookingsandpayments.repositories.HotelBookingRepository;
import masera.deviajebookingsandpayments.services.interfaces.CancellationService;
import masera.deviajebookingsandpayments.services.interfaces.EmailService;
import masera.deviajebookingsandpayments.services.interfaces.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Servicio para gestionar cancelaciones de reservas.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CancellationServiceImpl implements CancellationService {

  private final BookingRepository bookingRepository;

  private final HotelBookingRepository hotelBookingRepository;

  private final PaymentService paymentService;

  private final EmailService emailService;

  private final HotelClient hotelClient;

  /**
   * Cancela una reserva y procesa el reembolso si corresponde.
   *
   * @param bookingId ID de la reserva
   * @param request datos de la cancelación (incluye refundAmount calculado por el frontend)
   * @return resultado de la cancelación
   */
  @Override
  @Transactional
  public CancelBookingResponseDto cancelBooking(Long bookingId, CancelBookingRequestDto request) {
    log.info("Cancelando reserva ID: {} con reembolso de: {}",
            bookingId, request.getRefundAmount());

    BookingEntity booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Reserva no encontrada"
            ));

    if (BookingEntity.BookingStatus.CANCELLED.equals(booking.getStatus())) {
      throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST,
              "Esta reserva ya se encuentra cancelada"
      );
    }

    if (!BookingEntity.BookingStatus.CONFIRMED.equals(booking.getStatus())) {
      throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST,
              "Solo se pueden cancelar reservas confirmadas."
                      + " Las reservas completadas no pueden cancelarse"
      );
    }

    return switch (booking.getType()) {
      case FLIGHT -> cancelFlightBooking(booking, request);
      case HOTEL -> cancelHotelBooking(booking, request);
      case PACKAGE -> cancelPackageBooking(booking, request);
    };
  }

  /**
   * Cancela una reserva de vuelo.
   */
  private CancelBookingResponseDto cancelFlightBooking(
          BookingEntity booking,
          CancelBookingRequestDto request
  ) {
    log.info("Cancelando reserva de vuelo: {}", booking.getBookingReference());

    String fullReason = buildCancellationReason(request);
    booking.setStatus(BookingEntity.BookingStatus.CANCELLED);
    booking.setCancellationReason(fullReason);
    booking.setCancelledAt(LocalDateTime.now());
    bookingRepository.save(booking);

    BigDecimal refundAmount = request.getRefundAmount();
    if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
      log.info("Procesando reembolso de vuelo: {} {}", refundAmount, booking.getCurrency());
      paymentService.processRefundForBooking(booking.getId(), refundAmount);
    } else {
      try {
        emailService.sendCancellationEmail(
                booking.getEmail(),
                booking.getBookingReference(),
                booking.getHolderName(),
                "FLIGHT",
                BigDecimal.ZERO,
                booking.getCurrency(),
                booking.getCancelledAt()
        );
      } catch (Exception e) {
        log.error(e.getMessage());
      }
    }

    return CancelBookingResponseDto.builder()
            .bookingId(booking.getId())
            .bookingReference(booking.getBookingReference())
            .status("CANCELLED")
            .bookingType("FLIGHT")
            .cancelledAt(booking.getCancelledAt())
            .message("Reserva de vuelo cancelada exitosamente")
            .flightRefundAmount(refundAmount)
            .hotelRefundAmount(BigDecimal.ZERO)
            .totalRefundAmount(refundAmount)
            .currency(booking.getCurrency())
            .build();
  }

  /**
   * Cancela una reserva de hotel.
   */
  private CancelBookingResponseDto cancelHotelBooking(
          BookingEntity booking,
          CancelBookingRequestDto request
  ) {
    log.info("Cancelando reserva de hotel: {}", booking.getBookingReference());

    HotelBookingEntity hotelBooking = hotelBookingRepository.findByBookingEntity(booking)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Detalles de hotel no encontrados"
            ));

    try {
      hotelClient.cancelBooking(hotelBooking.getExternalId()).block();
      log.info("Reserva cancelada en HotelBeds: {}", hotelBooking.getExternalId());
    } catch (Exception e) {
      log.error("Error al cancelar en HotelBeds, continuando con cancelación local", e);
    }

    String fullReason = buildCancellationReason(request);
    booking.setStatus(BookingEntity.BookingStatus.CANCELLED);
    booking.setCancellationReason(fullReason);
    booking.setCancelledAt(LocalDateTime.now());
    bookingRepository.save(booking);

    BigDecimal refundAmount = request.getRefundAmount();
    if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
      log.info("Procesando reembolso de hotel: {} {}", refundAmount, booking.getCurrency());
      paymentService.processRefundForBooking(booking.getId(), refundAmount);
    } else {
      try {
        emailService.sendCancellationEmail(
                booking.getEmail(),
                booking.getBookingReference(),
                booking.getHolderName(),
                "HOTEL",
                BigDecimal.ZERO,
                booking.getCurrency(),
                booking.getCancelledAt()
        );
      } catch (Exception e) {
        log.error(e.getMessage());
      }
    }

    return CancelBookingResponseDto.builder()
            .bookingId(booking.getId())
            .bookingReference(booking.getBookingReference())
            .status("CANCELLED")
            .bookingType("HOTEL")
            .cancelledAt(booking.getCancelledAt())
            .message("Reserva de hotel cancelada exitosamente")
            .flightRefundAmount(BigDecimal.ZERO)
            .hotelRefundAmount(refundAmount)
            .totalRefundAmount(refundAmount)
            .currency(booking.getCurrency())
            .build();
  }

  /**
   * Cancela una reserva de paquete.
   */
  private CancelBookingResponseDto cancelPackageBooking(
          BookingEntity booking,
          CancelBookingRequestDto request
  ) {
    log.info("Cancelando paquete: {}", booking.getBookingReference());

    HotelBookingEntity hotelBooking = hotelBookingRepository.findByBookingEntity(booking)
            .orElse(null);

    if (hotelBooking != null) {
      try {
        hotelClient.cancelBooking(hotelBooking.getExternalId()).block();
        log.info("Hotel cancelado en HotelBeds: {}", hotelBooking.getExternalId());
      } catch (Exception e) {
        log.error("Error al cancelar hotel en HotelBeds", e);
      }
    }

    String fullReason = buildCancellationReason(request);
    booking.setStatus(BookingEntity.BookingStatus.CANCELLED);
    booking.setCancellationReason(fullReason);
    booking.setCancelledAt(LocalDateTime.now());
    bookingRepository.save(booking);

    BigDecimal totalRefund = request.getRefundAmount();
    if (totalRefund.compareTo(BigDecimal.ZERO) > 0) {
      log.info("Procesando reembolso de paquete: {} {}", totalRefund, booking.getCurrency());
      paymentService.processRefundForBooking(booking.getId(), totalRefund);
    } else {
      try {
        emailService.sendCancellationEmail(
                booking.getEmail(),
                booking.getBookingReference(),
                booking.getHolderName(),
                "PACKAGE",
                BigDecimal.ZERO,
                booking.getCurrency(),
                booking.getCancelledAt()
        );
      } catch (Exception e) {
        log.error(e.getMessage());
      }
    }

    return CancelBookingResponseDto.builder()
            .bookingId(booking.getId())
            .bookingReference(booking.getBookingReference())
            .status("CANCELLED")
            .bookingType("PACKAGE")
            .cancelledAt(booking.getCancelledAt())
            .message("Paquete cancelado exitosamente")
            .flightRefundAmount(BigDecimal.ZERO)
            .hotelRefundAmount(BigDecimal.ZERO)
            .totalRefundAmount(totalRefund)
            .currency(booking.getCurrency())
            .build();
  }

  /**
   * Construye el motivo completo de cancelación.
   */
  private String buildCancellationReason(CancelBookingRequestDto request) {
    if (request.getCancellationReason() == null || request.getCancellationReason().isEmpty()) {
      return "Sin motivo especificado";
    }

    String reason = request.getCancellationReason();

    if ("otro".equals(reason) && request.getAdditionalDetails() != null
            && !request.getAdditionalDetails().isEmpty()) {
      return "Otro: " + request.getAdditionalDetails();
    }

    return switch (reason) {
      case "cambio_planes" -> "Cambio de planes";
      case "salud" -> "Problemas de salud";
      case "emergencia" -> "Emergencia familiar";
      case "economico" -> "Problemas económicos";
      case "mejor_precio" -> "Encontré mejor precio";
      case "cambio_fechas" -> "Cambio de fechas";
      case "destino_no_interesa" -> "Destino ya no es de interés";
      case "otro" -> "Otro motivo";
      default -> reason;
    };
  }
}
