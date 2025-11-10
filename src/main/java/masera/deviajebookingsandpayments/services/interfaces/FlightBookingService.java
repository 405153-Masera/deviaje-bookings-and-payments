package masera.deviajebookingsandpayments.services.interfaces;

import masera.deviajebookingsandpayments.dtos.bookings.flights.CreateFlightBookingRequestDto;
import masera.deviajebookingsandpayments.dtos.bookings.flights.FlightOfferDto;
import masera.deviajebookingsandpayments.dtos.payments.PaymentRequestDto;
import masera.deviajebookingsandpayments.dtos.payments.PricesDto;
import masera.deviajebookingsandpayments.dtos.responses.BookingReferenceResponse;
import masera.deviajebookingsandpayments.dtos.responses.BookingResponseDto;
import masera.deviajebookingsandpayments.dtos.responses.FlightBookingDetailsDto;
import masera.deviajebookingsandpayments.entities.Booking;
import masera.deviajebookingsandpayments.entities.FlightBooking;
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
  BookingReferenceResponse bookAndPay(CreateFlightBookingRequestDto bookingRequest,
                                      PaymentRequestDto paymentRequest, PricesDto prices);

  /**
   * Obtiene información básica de una reserva de vuelo desde la BD.
   *
   * @param bookingId ID de la reserva
   * @return datos básicos de la reserva
   */
  FlightBookingDetailsDto getBasicBookingInfo(Long bookingId);


  /**
   * Verifica disponibilidad y precio de una oferta de vuelo.
   *
   * @param flightOfferData Datos de la oferta a verificar
   * @return información actualizada de la oferta
   */
  Object verifyFlightOfferPrice(Object flightOfferData);

  // =============== MÉTODOS PÚBLICOS PARA REUTILIZACIÓN ===============

  /**
   * Prepara los datos de la reserva en el formato requerido por Amadeus.
   *
   * @param bookingRequest datos de la reserva
   * @return datos formateados para Amadeus
   */
  Object prepareAmadeusBookingData(CreateFlightBookingRequestDto bookingRequest);

  /**
   * Extrae el ID externo de la respuesta de Amadeus.
   *
   * @param amadeusResponse respuesta de Amadeus
   * @return ID externo o temporal
   */
  String extractExternalId(Object amadeusResponse);

  void createFlightBookingEntity(CreateFlightBookingRequestDto request,
                                 FlightOfferDto flightOffer,
                                 Booking booking,
                                 String externalId,
                                 PricesDto prices);

  /**
   * Guarda la reserva de vuelo en la base de datos.
   *
   * @param request datos de la reserva
   * @param flightOffer datos de la oferta de vuelo
   * @param prices datos de precios
   * @param externalId ID externo de la reserva en Amadeus
   * @return la reserva guardada
   */
  Booking saveBookingInDatabase(CreateFlightBookingRequestDto request,
                                FlightOfferDto flightOffer,
                                PricesDto prices,
                                String externalId);

  Object callAmadeusCreateOrder(Object amadeusBookingData);

  /**
   * Extrae la fecha de salida del primer segmento.
   *
   * @param offer la oferta de vuelo
   * @return fecha de salida
   */
  String extractDepartureDate(FlightOfferDto offer);

  /**
   * Extrae la fecha de retorno si existe.
   *
   * @param offer la oferta de vuelo
   * @return fecha de retorno o null
   */
  String extractReturnDate(FlightOfferDto offer);

  /**
   * Extrae la aerolínea principal del primer segmento.
   *
   * @param offer la oferta de vuelo
   * @return código de aerolínea
   */
  String extractCarrier(FlightOfferDto offer);

  /**
   * Cuenta adultos en la lista de viajeros.
   *
   * @param request datos de la reserva
   * @return número de adultos
   */
  Integer countAdults(CreateFlightBookingRequestDto request);

  /**
   * Cuenta niños en la lista de viajeros.
   *
   * @param request datos de la reserva
   * @return número de niños
   */
  Integer countChildren(CreateFlightBookingRequestDto request);

  /**
   * Cuenta infantes en la lista de viajeros.
   *
   * @param request datos de la reserva
   * @return número de infantes
   */
  Integer countInfants(CreateFlightBookingRequestDto request);

  /**
   * Convierte FlightBooking a FlightBookingResponseDto.
   *
   * @param flightBooking entidad de reserva de vuelo
   * @return DTO de respuesta
   */
  FlightBookingDetailsDto convertToFlightBookingResponse(FlightBooking flightBooking);

  /**
   * Convierte Booking a BookingResponseDto.
   *
   * @param booking entidad de reserva
   * @return DTO de respuesta
   */
  BookingResponseDto convertToBookingResponse(Booking booking);
}