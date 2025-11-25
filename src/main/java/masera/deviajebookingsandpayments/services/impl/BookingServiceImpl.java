package masera.deviajebookingsandpayments.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import masera.deviajebookingsandpayments.clients.UserClient;
import masera.deviajebookingsandpayments.dtos.additional.UserBasicInfoDto;
import masera.deviajebookingsandpayments.dtos.bookings.flights.ItineraryDto;
import masera.deviajebookingsandpayments.dtos.bookings.hotels.HotelBookingApi;
import masera.deviajebookingsandpayments.dtos.bookings.travelers.TravelerDto;
import masera.deviajebookingsandpayments.dtos.responses.BookingDetailsResponseDto;
import masera.deviajebookingsandpayments.dtos.responses.BookingResponseDto;
import masera.deviajebookingsandpayments.entities.BookingEntity;
import masera.deviajebookingsandpayments.entities.FlightBookingEntity;
import masera.deviajebookingsandpayments.entities.HotelBookingEntity;
import masera.deviajebookingsandpayments.entities.PaymentEntity;
import masera.deviajebookingsandpayments.repositories.BookingRepository;
import masera.deviajebookingsandpayments.repositories.PaymentRepository;
import masera.deviajebookingsandpayments.services.interfaces.BookingService;
import masera.deviajebookingsandpayments.services.interfaces.EmailService;
import masera.deviajebookingsandpayments.services.interfaces.VoucherService;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

  private final UserClient userClient;

  private final VoucherService voucherService;

  private final EmailService emailService;

  private final ModelMapper modelMapper;

  private final ObjectMapper objectMapper;

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

  @Override
  @Transactional(readOnly = true)
  public BookingDetailsResponseDto getBookingDetails(Long bookingId) {
    log.info("Obteniendo detalles completos de la reserva con ID: {}", bookingId);

    BookingEntity booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new EntityNotFoundException(
                    "Reserva no encontrada con ID: " + bookingId));

    BookingDetailsResponseDto response = BookingDetailsResponseDto.builder()
            .id(booking.getId())
            .bookingReference(booking.getBookingReference())
            .clientId(booking.getClientId())
            .agentId(booking.getAgentId())
            .status(booking.getStatus().name())
            .type(booking.getType().name())
            .totalAmount(booking.getTotalAmount())
            .commission(booking.getCommission())
            .discount(booking.getDiscount())
            .taxes(booking.getTaxes())
            .currency(booking.getCurrency())
            .holderName(booking.getHolderName())
            .countryCallingCode(booking.getCountryCallingCode())
            .phone(booking.getPhone())
            .email(booking.getEmail())
            .createdDatetime(booking.getCreatedDatetime())
            .build();

    // Agregar detalles específicos según el tipo
    switch (booking.getType()) {
      case FLIGHT:
        response.setFlightDetails(buildFlightDetails(booking));
        break;
      case HOTEL:
        response.setHotelDetails(buildHotelDetails(booking));
        break;
      case PACKAGE:
        response.setFlightDetails(buildFlightDetails(booking));
        response.setHotelDetails(buildHotelDetails(booking));
        break;
      default:
        throw new IllegalStateException("Tipo de reserva no soportado: " + booking.getType());
    }

    log.info("Detalles de la reserva {} obtenidos exitosamente", bookingId);
    return response;
  }

  /**
   * Construye los detalles de una reserva de vuelo.
   */
  private BookingDetailsResponseDto.FlightBookingDetails buildFlightDetails(BookingEntity booking) {
    FlightBookingEntity flightBooking = booking.getFlightBookingEntities().getFirst();

    BookingDetailsResponseDto.FlightBookingDetails flightDetails =
            BookingDetailsResponseDto.FlightBookingDetails.builder()
            .externalId(flightBooking.getExternalId())
            .origin(flightBooking.getOrigin())
            .destination(flightBooking.getDestination())
            .carrier(flightBooking.getCarrier())
            .departureDate(flightBooking.getDepartureDate())
            .arrivalDate(flightBooking.getReturnDate())
            .adults(flightBooking.getAdults())
            .children(flightBooking.getChildren())
            .infants(flightBooking.getInfants())
            .totalPrice(flightBooking.getTotalPrice())
            .currency(flightBooking.getCurrency())
            .build();

    try {
      if (flightBooking.getItineraries() != null) {
        ItineraryDto itineraryDto = objectMapper.readValue(
                flightBooking.getItineraries(),
                ItineraryDto.class
        );
        flightDetails.setItineraries(itineraryDto);
      }

      if (flightBooking.getTravelers() != null) {
        List<TravelerDto> travelersDto = objectMapper.readValue(
                flightBooking.getTravelers(),
                objectMapper.getTypeFactory().constructCollectionType(
                        List.class,
                        TravelerDto.class
                )
        );
        flightDetails.setTravelers(travelersDto);
      }
    } catch (JsonProcessingException e) {
      log.error("Error al deserializar itineraries para booking {}: {}",
              booking.getId(), e.getMessage());
    }
    return flightDetails;
  }

  /**
   * Construye los detalles de una reserva de hotel.
   */
  private BookingDetailsResponseDto.HotelBookingDetails buildHotelDetails(BookingEntity booking) {
    HotelBookingEntity hotelBooking = booking.getHotelBookingEntities().getFirst();

    BookingDetailsResponseDto.HotelBookingDetails hotelDetails =
            BookingDetailsResponseDto.HotelBookingDetails.builder()
            .externalId(hotelBooking.getExternalId())
            .hotelName(hotelBooking.getHotelName())
            .destinationName(hotelBooking.getDestinationName())
            .countryName(hotelBooking.getCountryName())
            .roomName(hotelBooking.getRoomName())
            .boardName(hotelBooking.getBoardName())
            .checkInDate(hotelBooking.getCheckInDate())
            .checkOutDate(hotelBooking.getCheckOutDate())
            .numberOfNights(hotelBooking.getNumberOfNights())
            .numberOfRooms(hotelBooking.getNumberOfRooms())
            .adults(hotelBooking.getAdults())
            .children(hotelBooking.getChildren())
            .totalPrice(hotelBooking.getTotalPrice())
            .taxes(hotelBooking.getTaxes())
            .currency(hotelBooking.getCurrency())
            .build();

    if (hotelBooking.getHotelBooking() != null) {
      try {
        HotelBookingApi hotelBookingApi = objectMapper.readValue(
                hotelBooking.getHotelBooking(),
                HotelBookingApi.class
        );
        hotelDetails.setHotelBooking(hotelBookingApi);
      } catch (JsonProcessingException e) {
        log.error("Error al deserializar hotelBooking para booking {}: {}",
                booking.getId(), e.getMessage());
      }
    }
    return hotelDetails;
  }

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

  @Override
  public String getBookingReference(Long bookingId) {

    BookingEntity booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new EntityNotFoundException(
                    "Reserva no encontrada con ID: " + bookingId));
    return booking.getBookingReference();
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
   * Convierte BookingEntity a BookingResponseDto con información de usuarios.
   *
   * @param booking entidad de reserva
   * @return DTO de respuesta con nombres de usuario
   */
  private BookingResponseDto convertToDto(BookingEntity booking) {
    BookingResponseDto.BookingResponseDtoBuilder builder = BookingResponseDto.builder()
            .id(booking.getId())
            .bookingReference(booking.getBookingReference())
            .clientId(booking.getClientId())
            .agentId(booking.getAgentId())
            .status(booking.getStatus().name())
            .type(booking.getType().name())
            .totalAmount(booking.getTotalAmount())
            .commission(booking.getCommission())
            .discount(booking.getDiscount())
            .taxes(booking.getTaxes())
            .currency(booking.getCurrency())
            .holderName(booking.getHolderName())
            .phone(booking.getPhone())
            .email(booking.getEmail())
            .createdDatetime(booking.getCreatedDatetime());

    if (booking.getClientId() != null) {
      try {
        UserBasicInfoDto clientInfo = userClient.getUserBasicInfo(booking.getClientId())
                .block();
        if (clientInfo != null) {
          builder.clientUserName(clientInfo.getUserName());
        }
      } catch (Exception e) {
        log.warn("No se pudo obtener información del cliente {}: {}",
                booking.getClientId(), e.getMessage());
        builder.clientUserName("Cliente #" + booking.getClientId());
      }
    }

    if (booking.getAgentId() != null) {
      try {
        UserBasicInfoDto agentInfo = userClient.getUserBasicInfo(booking.getAgentId())
                .block();
        if (agentInfo != null) {
          builder.agentUserName(agentInfo.getUserName());
        }
      } catch (Exception e) {
        log.warn("No se pudo obtener información del agente {}: {}",
                booking.getAgentId(), e.getMessage());
        builder.agentUserName("Agente #" + booking.getAgentId());
      }
    }

    return builder.build();
  }
}
