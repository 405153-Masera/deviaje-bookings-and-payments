package masera.deviajebookingsandpayments.services.interfaces;

import java.util.Map;
import masera.deviajebookingsandpayments.dtos.bookings.hotels.CreateHotelBookingRequestDto;
import masera.deviajebookingsandpayments.dtos.bookings.hotels.HotelBookingApi;
import masera.deviajebookingsandpayments.dtos.bookings.hotels.HotelBookingResponse;
import masera.deviajebookingsandpayments.dtos.payments.PaymentRequestDto;
import masera.deviajebookingsandpayments.dtos.payments.PricesDto;
import masera.deviajebookingsandpayments.dtos.responses.BookingReferenceResponse;
import masera.deviajebookingsandpayments.dtos.responses.HotelBookingDetailsDto;
import masera.deviajebookingsandpayments.entities.Booking;
import masera.deviajebookingsandpayments.entities.HotelBooking;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
  BookingReferenceResponse bookAndPay(CreateHotelBookingRequestDto bookingRequest,
                                      PaymentRequestDto paymentRequest, PricesDto prices);

  /**
   * Obtiene información básica de una reserva de hotel desde la BD.
   *
   * @param bookingId ID de la reserva
   * @return datos básicos de la reserva
   */
  HotelBookingDetailsDto getBasicBookingInfo(Long bookingId);

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

  HotelBookingResponse callHotelBedsCreateBooking(Map<String, Object> hotelBedsRequest);

  void createHotelBookingEntity(CreateHotelBookingRequestDto request,
                                Booking booking,
                                String externalId,
                                PricesDto prices,
                                HotelBookingApi hotelDetails);

  @Transactional
  Booking saveBookingInDatabase(CreateHotelBookingRequestDto request,
                                PricesDto payment,
                                String externalId,
                                HotelBookingApi hotelDetails);

  Integer countAdults(CreateHotelBookingRequestDto request);

  Integer countChildren(CreateHotelBookingRequestDto request);

  HotelBookingDetailsDto convertToHotelBookingResponse(HotelBooking hotelBooking);
}
