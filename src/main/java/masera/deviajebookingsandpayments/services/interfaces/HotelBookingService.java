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
   * @param prices representa los detalles del precio
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

  /**
   * Metodo que prepara la request para hotelbeds.
   *
   * @param request representa la previa de la request
   * @return la request
   */
  Map<String, Object> prepareHotelBedsBookingRequest(CreateHotelBookingRequestDto request);

  /**
   * Metodo que crea la reserva en hotelbeds.
   *
   * @param hotelBedsRequest request del endpoint
   * @return la reserva ya creada
   */
  HotelBookingResponse callHotelBedsCreateBooking(Map<String, Object> hotelBedsRequest);

  /**
   * Metodo que crea la entidad de la reserva.
   *
   * @param request petición de la api hotelbeds
   * @param booking reserva ya creada
   * @param externalId referencia de hotelbeds
   * @param prices detalles del precio
   * @param hotelDetails detalles de la reserva
   */
  void createHotelBookingEntity(CreateHotelBookingRequestDto request,
                                Booking booking,
                                String externalId,
                                PricesDto prices,
                                HotelBookingApi hotelDetails);


  /**
   * Metodo que crea guarda la reserva en la base de datos.
   *
   * @param request petición de hotelbeds
   * @param payment detalles del pago
   * @param externalId referencia de hotelbeds
   * @param hotelDetails detalles de la reserva
   * @return la entidad guardada
   */
  Booking saveBookingInDatabase(CreateHotelBookingRequestDto request,
                                PricesDto payment,
                                String externalId,
                                HotelBookingApi hotelDetails);

  /**
   * Metodo que cuenta la cantidad de adultos.
   *
   * @param request petición de hotelbeds
   * @return la cantidad de adultos
   */
  Integer countAdults(CreateHotelBookingRequestDto request);

  /**
   * Metodo que cuenta la cantidad de chicos.
   *
   * @param request petición de hotelbeds
   * @return la cantidad de chicos
   */
  Integer countChildren(CreateHotelBookingRequestDto request);

  /**
   *  Metodo que facilita el mapeo entre la entidad y el DTO.
   *
   * @param hotelBooking la entidad de la reserva
   * @return el DTO de respuesta
   */
  HotelBookingDetailsDto convertToHotelBookingResponse(HotelBooking hotelBooking);
}
