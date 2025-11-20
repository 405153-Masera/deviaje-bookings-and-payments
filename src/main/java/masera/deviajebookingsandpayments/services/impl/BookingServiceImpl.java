package masera.deviajebookingsandpayments.services.impl;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import masera.deviajebookingsandpayments.dtos.responses.BookingResponseDto;
import masera.deviajebookingsandpayments.entities.BookingEntity;
import masera.deviajebookingsandpayments.entities.PaymentEntity;
import masera.deviajebookingsandpayments.repositories.BookingRepository;
import masera.deviajebookingsandpayments.repositories.PaymentRepository;
import masera.deviajebookingsandpayments.services.interfaces.BookingService;
import masera.deviajebookingsandpayments.services.interfaces.EmailService;
import masera.deviajebookingsandpayments.services.interfaces.VoucherService;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Implementación del servicio de reservas.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

  private final BookingRepository bookingRepository;

  private final PaymentRepository paymentRepository;

  private final VoucherService voucherService;

  private final EmailService emailService;

  private final ModelMapper modelMapper;

  @Override
  public List<BookingResponseDto> getClientBookings(Integer clientId,
                                                    String email,
                                                    String holderName) {

    log.info("Obteniendo reservas del cliente: {} con filtros", clientId);

    List<BookingEntity> bookings = bookingRepository.findByClientId(clientId);
    bookings = applyEmailAndNameFilters(bookings, email, holderName);

    return bookings.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());

  }

  @Override
  public List<BookingResponseDto> getAgentBookings(Integer agentId,
                                                   Integer clientId,
                                                   String email,
                                                   String holderName) {

    log.info("Obteniendo reservas del agente: {} con filtros", agentId);

    List<BookingEntity> bookings;
    if (clientId != null) {
      bookings = bookingRepository.findByAgentIdAndClientId(agentId, clientId);
    } else {
      bookings = bookingRepository.findByAgentId(agentId);
    }

    bookings = applyEmailAndNameFilters(bookings, email, holderName);

    return bookings.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
  }

  @Override
  public List<BookingResponseDto> getAllBookings(Integer agentId,
                                                 Integer clientId,
                                                 String email,
                                                 String holderName) {

    log.info("Obteniendo todas las reservas con filtros");

    List<BookingEntity> bookings = bookingRepository.findAll();
    if (agentId != null) {
      bookings = bookings.stream()
              .filter(b -> agentId.equals(b.getAgentId()))
              .collect(Collectors.toList());
    }

    if (clientId != null) {
      bookings = bookings.stream()
              .filter(b -> clientId.equals(b.getClientId()))
              .collect(Collectors.toList());
    }

    bookings = applyEmailAndNameFilters(bookings, email, holderName);
    log.info("Se encontraron {} reservas", bookings.size());
    return bookings.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
  }

  @Override
  public BookingResponseDto getBookingById(Long bookingId) {
    log.info("Obteniendo reserva: {}", bookingId);

    BookingEntity booking = findBookingById(bookingId);
    return convertToDto(booking);
  }

  /*@Override
  public BookingDetailsResponseDto getBookingDetails(Long bookingId) {
    log.info("Obteniendo detalles completos de reserva: {}", bookingId);

    BookingEntity booking = findBookingById(bookingId);
    return getBookingDetails(booking);
  }*/

  @Override
  public byte[] downloadVoucher(Long bookingId) {
    log.info("Descargando voucher de reserva: {}", bookingId);

    try {
      BookingEntity booking = findBookingById(bookingId);

      if (booking.getVoucher() != null && booking.getVoucher().length > 0) {
        log.info("Devolviendo voucher existente");
        return booking.getVoucher();
      }

      log.info("Generando nuevo voucher");
      return voucherService.generateVoucher(booking);

    } catch (Exception e) {
      log.error("Error al descargar voucher de reserva: {}", bookingId, e);
      throw new ResponseStatusException(
              HttpStatus.INTERNAL_SERVER_ERROR,
              "Error al descargar voucher");
    }
  }

  /*@Override
  public String resendVoucher(Long bookingId) {
    log.info("Reenviando voucher de reserva: {}", bookingId);

    try {
      BookingEntity booking = findBookingById(bookingId);
      emailService.sendVoucherEmail(booking);

      String message = "Voucher enviado exitosamente a " + booking.getEmail();
      log.info(message);
      return message;

    } catch (Exception e) {
      log.error("Error al reenviar voucher de reserva: {}", bookingId, e);
      throw new ResponseStatusException(
              HttpStatus.INTERNAL_SERVER_ERROR,
              "Error al enviar el voucher: " + e.getMessage());
    }
  }*/

  @Override
  public String generateBookingReference(Long bookingId,
                                         BookingEntity.BookingType type) {

    String prefix = switch (type) {
      case FLIGHT -> "FLT";
      case HOTEL -> "HTL";
      case PACKAGE -> "PKG";
    };

    return prefix + "-" + String.format("%08d", bookingId);
  }

  @Override
  public void updatePaymentWithBookingId(Long paymentId, Long bookingId) {
    log.info("Actualizando payment {} con bookingId {}", paymentId, bookingId);

    try {
      PaymentEntity payment = paymentRepository.findById(paymentId)
              .orElseThrow(() -> new ResponseStatusException(
                      HttpStatus.NOT_FOUND,
                      "Pago no encontrado: " + paymentId));

      BookingEntity booking = findBookingById(bookingId);
      payment.setBookingEntity(booking);
      paymentRepository.save(payment);

      log.info("Payment actualizado exitosamente");

    } catch (Exception e) {
      log.error("Error al actualizar payment con bookingId", e);
      throw new ResponseStatusException(
              HttpStatus.INTERNAL_SERVER_ERROR,
              "Error al actualizar pago");
    }
  }

  /**
   * Busca una reserva por ID y lanza excepción si no existe.
   */
  private BookingEntity findBookingById(Long bookingId) {
    return bookingRepository.findById(bookingId)
            .orElseThrow(() -> {
              log.warn("Reserva no encontrada: {}", bookingId);
              return new ResponseStatusException(
                      HttpStatus.NOT_FOUND,
                      "Reserva no encontrada: " + bookingId);
            });
  }

  /**
   * Aplica filtros de email y nombre a una lista de reservas.
   */
  private List<BookingEntity> applyEmailAndNameFilters(List<BookingEntity> bookings,
                                                       String email,
                                                       String holderName) {

    if (email != null && !email.isEmpty()) {
      bookings = bookings.stream()
              .filter(b -> b.getEmail() != null
                      && b.getEmail().toLowerCase().contains(email.toLowerCase()))
              .collect(Collectors.toList());
    }

    if (holderName != null && !holderName.isEmpty()) {
      bookings = bookings.stream()
              .filter(b -> b.getHolderName() != null
                      && b.getHolderName().toLowerCase().contains(holderName.toLowerCase()))
              .collect(Collectors.toList());
    }

    return bookings;
  }

  /**
   * Convierte una entidad a DTO.
   */
  private BookingResponseDto convertToDto(BookingEntity booking) {
    return modelMapper.map(booking, BookingResponseDto.class);
  }
}
