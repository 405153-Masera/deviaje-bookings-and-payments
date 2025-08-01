package masera.deviajebookingsandpayments.services.interfaces;

import masera.deviajebookingsandpayments.dtos.bookings.hotels.CreateHotelBookingRequestDto;
import masera.deviajebookingsandpayments.dtos.payments.PaymentRequestDto;
import masera.deviajebookingsandpayments.dtos.payments.PricesDto;
import masera.deviajebookingsandpayments.dtos.responses.BaseResponse;
import masera.deviajebookingsandpayments.dtos.responses.BookingResponseDto;
import masera.deviajebookingsandpayments.dtos.responses.HotelBookingResponseDto;
import masera.deviajebookingsandpayments.entities.Booking;
import masera.deviajebookingsandpayments.entities.HotelBooking;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

/**
 * Interfaz para el servicio de reservas de hoteles.
 */
@Service
public interface HotelBookingService {

  /**
   * Procesa una reserva de hotel y su pago de forma unificada.
   *
   * @param bookingRequest datos de la reserva
   * @param paymentRequest datos del pago
   * @return respuesta unificada con resultado de la operación
   */
  BaseResponse bookAndPay(CreateHotelBookingRequestDto bookingRequest,
                          PaymentRequestDto paymentRequest, PricesDto prices);

  /**
   * Obtiene información básica de una reserva de hotel desde la BD.
   *
   * @param bookingId ID de la reserva
   * @return datos básicos de la reserva
   */
  HotelBookingResponseDto getBasicBookingInfo(Long bookingId);

  /**
   * Obtiene detalles completos de una reserva desde HotelBeds API.
   *
   * @param bookingId ID de la reserva
   * @return detalles completos desde la API externa
   */
  Object getFullBookingDetails(Long bookingId);

  /**
   * Verifica disponibilidad y precio de una tarifa.
   *
   * @param rateKey clave de la tarifa
   * @return información actualizada de la tarifa
   */
  Object checkRates(String rateKey);

  Map<String, Object> prepareHotelBedsBookingRequest(CreateHotelBookingRequestDto request);

  void updatePaymentWithBookingId(Long paymentId, Long bookingId);

  String extractExternalId(Object hotelBedsResponse);

  Map<String, Object> extractHotelDetails(Object hotelBedsResponse);

  @Transactional
  Booking saveBookingInDatabase(CreateHotelBookingRequestDto request,
                                PricesDto payment,
                                String externalId,
                                Map<String, Object> hotelDetails);

  LocalDate extractCheckInDate(String rateKey);

  LocalDate extractCheckOutDate(String rateKey);

  String extractHotelName(Map<String, Object> hotelDetails);

  String extractDestinationName(Map<String, Object> hotelDetails);

  Integer countAdults(CreateHotelBookingRequestDto request);

  Integer countChildren(CreateHotelBookingRequestDto request);

  HotelBookingResponseDto convertToHotelBookingResponse(HotelBooking hotelBooking);

  BookingResponseDto convertToBookingResponse(Booking booking);
}
