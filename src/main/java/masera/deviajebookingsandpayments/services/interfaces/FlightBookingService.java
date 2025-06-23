package masera.deviajebookingsandpayments.services.interfaces;

import masera.deviajebookingsandpayments.dtos.bookings.flights.CreateFlightBookingRequestDto;
import masera.deviajebookingsandpayments.dtos.payments.PaymentRequestDto;
import masera.deviajebookingsandpayments.dtos.payments.PricesDto;
import masera.deviajebookingsandpayments.dtos.responses.BookAndPayResponseDto;
import masera.deviajebookingsandpayments.dtos.responses.FlightBookingResponseDto;
import org.springframework.stereotype.Service;

/**
 * Interfaz para el servicio de reservas de vuelos.
 */
@Service
public interface FlightBookingService {

  /**
   * Procesa una reserva de vuelo y su pago de forma unificada.
   *
   * @param bookingRequest datos de la reserva
   * @param paymentRequest datos del pago
   * @return respuesta unificada con resultado de la operación
   */
  BookAndPayResponseDto bookAndPay(CreateFlightBookingRequestDto bookingRequest,
                                   PaymentRequestDto paymentRequest, PricesDto prices);

  /**
   * Obtiene información básica de una reserva de vuelo desde la BD.
   *
   * @param bookingId ID de la reserva
   * @return datos básicos de la reserva
   */
  FlightBookingResponseDto getBasicBookingInfo(Long bookingId);

  /**
   * Obtiene detalles completos de una reserva desde Amadeus API.
   *
   * @param bookingId ID de la reserva
   * @return detalles completos desde la API externa
   */
  Object getFullBookingDetails(Long bookingId);

  /**
   * Verifica disponibilidad y precio de una oferta de vuelo.
   *
   * @param flightOfferData Datos de la oferta a verificar
   * @return información actualizada de la oferta
   */
  Object verifyFlightOfferPrice(Object flightOfferData);
}