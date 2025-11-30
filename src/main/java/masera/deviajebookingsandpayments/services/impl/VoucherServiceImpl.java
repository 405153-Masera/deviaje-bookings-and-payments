package masera.deviajebookingsandpayments.services.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.html2pdf.HtmlConverter;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import masera.deviajebookingsandpayments.dtos.bookings.flights.DepartureArrivalDto;
import masera.deviajebookingsandpayments.dtos.bookings.flights.ItineraryDto;
import masera.deviajebookingsandpayments.dtos.bookings.flights.SegmentDto;
import masera.deviajebookingsandpayments.entities.BookingEntity;
import masera.deviajebookingsandpayments.entities.FlightBookingEntity;
import masera.deviajebookingsandpayments.entities.HotelBookingEntity;
import masera.deviajebookingsandpayments.repositories.BookingRepository;
import masera.deviajebookingsandpayments.services.interfaces.VoucherService;
import org.springframework.stereotype.Service;

/**
 * Implementación del servicio de generación de vouchers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VoucherServiceImpl implements VoucherService {

  private final BookingRepository bookingRepository;

  private final ObjectMapper objectMapper;

  private static final String PRIMARY_COLOR = "#8B5CF6";

  @Override
  public byte[] generateVoucher(BookingEntity booking) throws Exception {
    log.info("Generando voucher para reserva: {}", booking.getBookingReference());

    return switch (booking.getType()) {
      case FLIGHT -> generateFlightVoucher(booking);
      case HOTEL -> generateHotelVoucher(booking);
      case PACKAGE -> generatePackageVoucher(booking);
    };
  }

  @Override
  public byte[] generateFlightVoucher(BookingEntity booking) throws Exception {
    log.info("Generando voucher de vuelo para: {}", booking.getBookingReference());

    // Cargar la entidad completa con las relaciones
    BookingEntity fullBooking = bookingRepository.findById(booking.getId())
            .orElseThrow(() -> new Exception("Reserva no encontrada"));

    if (fullBooking.getFlightBookingEntities() == null
            || fullBooking.getFlightBookingEntities().isEmpty()) {
      throw new Exception("No se encontraron detalles del vuelo");
    }

    FlightBookingEntity flightBooking = fullBooking.getFlightBookingEntities().getFirst();

    String htmlContent = buildFlightVoucherHtml(fullBooking, flightBooking);

    return convertHtmlToPdf(htmlContent);
  }

  @Override
  public byte[] generateHotelVoucher(BookingEntity booking) throws Exception {
    log.info("Generando voucher de hotel para: {}", booking.getBookingReference());

    // Cargar la entidad completa con las relaciones
    BookingEntity fullBooking = bookingRepository.findById(booking.getId())
            .orElseThrow(() -> new Exception("Reserva no encontrada"));

    if (fullBooking.getHotelBookingEntities() == null
            || fullBooking.getHotelBookingEntities().isEmpty()) {
      throw new Exception("No se encontraron detalles del hotel");
    }

    HotelBookingEntity hotelBooking = fullBooking.getHotelBookingEntities().getFirst();

    String htmlContent = buildHotelVoucherHtml(fullBooking, hotelBooking);

    return convertHtmlToPdf(htmlContent);
  }

  @Override
  public byte[] generatePackageVoucher(BookingEntity booking) throws Exception {
    log.info("Generando voucher de paquete para: {}", booking.getBookingReference());

    // Cargar la entidad completa con las relaciones
    BookingEntity fullBooking = bookingRepository.findById(booking.getId())
            .orElseThrow(() -> new Exception("Reserva no encontrada"));

    if (fullBooking.getFlightBookingEntities() == null
            || fullBooking.getFlightBookingEntities().isEmpty()
            || fullBooking.getHotelBookingEntities() == null
            || fullBooking.getHotelBookingEntities().isEmpty()) {
      throw new Exception("No se encontraron detalles completos del paquete");
    }

    FlightBookingEntity flightBooking = fullBooking.getFlightBookingEntities().getFirst();
    HotelBookingEntity hotelBooking = fullBooking.getHotelBookingEntities().getFirst();

    String htmlContent = buildPackageVoucherHtml(fullBooking, flightBooking, hotelBooking);

    return convertHtmlToPdf(htmlContent);
  }

  /**
   * Construye el HTML para el voucher de vuelo.
   */
  private String buildFlightVoucherHtml(BookingEntity booking, FlightBookingEntity flight) {
    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    boolean isRoundTrip = flight.getReturnDate() != null;

    String formattedDepartureDate = formatFlightDateTime(flight.getDepartureDate());
    String formattedReturnDate = isRoundTrip
            ? formatFlightDateTime(flight.getReturnDate())
            : null;
    String itineraryDetailsHtml = buildItineraryDetailsHtml(flight.getItineraries());

    return "<!DOCTYPE html>"
            + "<html>"
            + "<head>"
            + "<meta charset='UTF-8'/>"
            + "<style>" + getCommonStyles() + "</style>"
            + "</head>"
            + "<body>"
            + "<div class='container'>"

            // Header
            + "<div class='header'>"
            + "<h1>DeViaje</h1>"
            + "</div>"

            + "<div class='voucher-title'>Voucher de Vuelo</div>"
            + "<div class='code-section'>CÓDIGO DEVIAJE " + booking.getBookingReference() + "</div>"

            // Confirmation bar
            + "<div class='confirmation-bar'>"
            + "Confirmado: " + flight.getOrigin() + " → " + flight.getDestination()
            + "</div>"

            // Booking details
            + "<div class='info-section'>"
            + "<p><strong>Reservado por:</strong> " + booking.getHolderName() + "</p>"
            + "<p><strong>Fecha de reserva:</strong> "
            + booking.getCreatedDatetime().format(dateFormatter) + "</p>"
            + "<p><strong>Fecha de salida:</strong> " + formattedDepartureDate + "</p>"
            + (isRoundTrip
            ? "<p><strong>Fecha de regreso:</strong> " + formattedReturnDate + "</p>"
            : "")
            + "</div>"

            // Itinerarios detallados
            + itineraryDetailsHtml

            // Passenger details
            + "<div class='info-section'>"
            + "<div class='details-row'>"
            + "<div class='details-column'><strong>Pasajeros</strong><br/>"
            + flight.getAdults() + " Adultos"
            + (flight.getChildren() != null && flight.getChildren() > 0
            ? ", " + flight.getChildren() + " Niños" : "")
            + (flight.getInfants() != null && flight.getInfants() > 0
            ? ", " + flight.getInfants() + " Infantes" : "")
            + "</div>"
            + "<div class='details-column'><strong>Aerolínea</strong><br/>"
            + (flight.getCarrier() != null ? flight.getCarrier() : "N/A") + "</div>"
            + "</div>"
            + "</div>"

            // Price details
            + "<div class='price-section'>"
            + "<h3>Detalles del Precio</h3>"
            + "<div class='price-row'>"
            + "<span>Total:</span>"
            + "<span class='price-value'>" + booking.getCurrency()
            + " " + booking.getTotalAmount() + "</span>"
            + "</div>"
            + "<div class='price-row'>"
            + "<span>Impuestos incluidos:</span>"
            + "<span>" + booking.getCurrency() + " " + booking.getTaxes() + "</span>"
            + "</div>"
            + "</div>"

            // Important information
            + "<div class='important-section'>"
            + "<h3>Información importante</h3>"
            + "<p>Presentar este voucher junto con un documento "
            + "de identidad válido en el check-in. "
            + "Se recomienda llegar al aeropuerto con al menos 2 horas de anticipación para vuelos "
            + "nacionales y 3 horas para vuelos internacionales.</p>"
            + "</div>"

            // Footer
            + getFooter()

            + "</div>"
            + "</body>"
            + "</html>";
  }

  /**
   * Construye el HTML para el voucher de hotel.
   */
  private String buildHotelVoucherHtml(BookingEntity booking, HotelBookingEntity hotel) {
    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    return "<!DOCTYPE html>"
            + "<html>"
            + "<head>"
            + "<meta charset='UTF-8'/>"
            + "<style>" + getCommonStyles() + "</style>"
            + "</head>"
            + "<body>"
            + "<div class='container'>"

            // Header
            + "<div class='header'>"
            + "<h1>DeViaje</h1>"
            + "</div>"

            + "<div class='voucher-title'>Voucher de Alojamiento</div>"
            + "<div class='code-section'>CÓDIGO DEVIAJE " + booking.getBookingReference() + "</div>"

            // Confirmation bar
            + "<div class='confirmation-bar'>"
            + "Confirmado: " + hotel.getNumberOfNights()
            + " noches en " + hotel.getDestinationName()
            + "</div>"

            // Booking details
            + "<div class='info-section'>"
            + "<p><strong>Reservado por:</strong> " + booking.getHolderName() + "</p>"
            + "<p><strong>Fecha de reserva:</strong> "
            + booking.getCreatedDatetime().format(dateFormatter) + "</p>"
            + "</div>"

            // Check-in/out dates
            + "<div class='info-section'>"
            + "<div class='hotel-dates'>"
            + "<div class='date-column'>"
            + "<p class='label'>Check in</p>"
            + "<p class='value'>" + hotel.getCheckInDate().format(dateFormatter) + "</p>"
            + "<p class='time'>15:00HS</p>"
            + "</div>"
            + "<div class='date-column'>"
            + "<p class='label'>Check out</p>"
            + "<p class='value'>" + hotel.getCheckOutDate().format(dateFormatter) + "</p>"
            + "<p class='time'>11:00HS</p>"
            + "</div>"
            + "</div>"
            + "</div>"

            // Room details
            + "<div class='info-section'>"
            + "<div class='details-row'>"
            + "<div class='details-column'><strong>Habitaciones</strong><br/>"
            + hotel.getNumberOfRooms() + " " + hotel.getRoomName() + "</div>"
            + "<div class='details-column'><strong>Pasajeros</strong><br/>"
            + hotel.getAdults() + " Adultos"
            + (hotel.getChildren() > 0 ? ", " + hotel.getChildren() + " Niños" : "")
            + "</div>"
            + "<div class='details-column'><strong>Régimen de comida</strong><br/>"
            + hotel.getBoardName() + "</div>"
            + "</div>"
            + "</div>"

            // Price details
            + "<div class='price-section'>"
            + "<h3>Detalles del Precio</h3>"
            + "<div class='price-row'>"
            + "<span>Total:</span>"
            + "<span class='price-value'>" + booking.getCurrency()
            + " " + booking.getTotalAmount() + "</span>"
            + "</div>"
            + "<div class='price-row'>"
            + "<span>Impuestos incluidos:</span>"
            + "<span>" + booking.getCurrency() + " " + booking.getTaxes() + "</span>"
            + "</div>"
            + "</div>"

            // Important information
            + "<div class='important-section'>"
            + "<h3>Información importante</h3>"
            + "<p>Presentar este voucher en el check-in del hotel junto con un "
            + "documento de identidad válido. Verificar las políticas específicas del "
            + "hotel respecto a horarios de entrada y servicios incluidos.</p>"
            + "</div>"

            // Footer
            + getFooter()

            + "</div>"
            + "</body>"
            + "</html>";
  }

  /**
   * Construye el HTML para el voucher de paquete.
   */
  /**
   * Construye el HTML para el voucher de paquete.
   */
  private String buildPackageVoucherHtml(BookingEntity booking,
                                         FlightBookingEntity flight,
                                         HotelBookingEntity hotel) {
    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Determinar si es vuelo de ida y vuelta
    boolean isRoundTrip = flight.getReturnDate() != null;

    // Formatear fechas de vuelo
    String formattedDepartureDate = formatFlightDateTime(flight.getDepartureDate());
    String formattedReturnDate = isRoundTrip
            ? formatFlightDateTime(flight.getReturnDate())
            : null;

    // Parsear itinerarios para mostrar detalles del vuelo
    String itineraryDetailsHtml = buildItineraryDetailsHtml(flight.getItineraries());

    return "<!DOCTYPE html>"
            + "<html>"
            + "<head>"
            + "<meta charset='UTF-8'/>"
            + "<style>" + getCommonStyles() + "</style>"
            + "</head>"
            + "<body>"
            + "<div class='container'>"

            // Header
            + "<div class='header'>"
            + "<h1>DeViaje</h1>"
            + "</div>"

            + "<div class='voucher-title'>Voucher de Paquete</div>"
            + "<div class='code-section'>CÓDIGO DEVIAJE " + booking.getBookingReference() + "</div>"

            // Confirmation bar
            + "<div class='confirmation-bar'>"
            + "Confirmado: Paquete Vuelo + Hotel"
            + "</div>"

            // Booking details
            + "<div class='info-section'>"
            + "<p><strong>Reservado por:</strong> " + booking.getHolderName() + "</p>"
            + "<p><strong>Fecha de reserva:</strong> "
            + booking.getCreatedDatetime().format(dateFormatter) + "</p>"
            + "</div>"

            // Flight section
            + "<div class='section-divider'>DETALLES DEL VUELO</div>"
            + "<div class='info-section'>"
            + "<p><strong>Origen:</strong> " + flight.getOrigin() + "</p>"
            + "<p><strong>Destino:</strong> " + flight.getDestination() + "</p>"
            + "<p><strong>Fecha de salida:</strong> " + formattedDepartureDate + "</p>"
            + (isRoundTrip
            ? "<p><strong>Fecha de regreso:</strong> " + formattedReturnDate + "</p>"
            : "")
            + "</div>"

            // Itinerarios detallados del vuelo
            + itineraryDetailsHtml

            // Passenger details
            + "<div class='info-section'>"
            + "<div class='details-row'>"
            + "<div class='details-column'><strong>Pasajeros</strong><br/>"
            + flight.getAdults() + " Adultos"
            + (flight.getChildren() != null && flight.getChildren() > 0
            ? ", " + flight.getChildren() + " Niños" : "")
            + (flight.getInfants() != null && flight.getInfants() > 0
            ? ", " + flight.getInfants() + " Infantes" : "")
            + "</div>"
            + "<div class='details-column'><strong>Aerolínea</strong><br/>"
            + (flight.getCarrier() != null ? flight.getCarrier() : "N/A") + "</div>"
            + "</div>"
            + "</div>"

            // Hotel section
            + "<div class='section-divider'>DETALLES DEL ALOJAMIENTO</div>"
            + "<div class='info-section'>"
            + "<h2>" + hotel.getHotelName() + "</h2>"
            + "<p>" + hotel.getDestinationName() + ", " + hotel.getCountryName() + "</p>"
            + "</div>"

            // Hotel dates
            + "<div class='info-section'>"
            + "<div class='hotel-dates'>"
            + "<div class='date-column'>"
            + "<p class='label'>Check in</p>"
            + "<p class='value'>" + hotel.getCheckInDate().format(dateFormatter) + "</p>"
            + "<p class='time'>15:00HS</p>"
            + "</div>"
            + "<div class='date-column'>"
            + "<p class='label'>Check out</p>"
            + "<p class='value'>" + hotel.getCheckOutDate().format(dateFormatter) + "</p>"
            + "<p class='time'>11:00HS</p>"
            + "</div>"
            + "</div>"
            + "</div>"

            // Hotel room details
            + "<div class='info-section'>"
            + "<div class='details-row'>"
            + "<div class='details-column'><strong>Habitaciones</strong><br/>"
            + hotel.getNumberOfRooms() + " " + hotel.getRoomName() + "</div>"
            + "<div class='details-column'><strong>Pasajeros</strong><br/>"
            + hotel.getAdults() + " Adultos"
            + (hotel.getChildren() > 0 ? ", " + hotel.getChildren() + " Niños" : "")
            + "</div>"
            + "<div class='details-column'><strong>Régimen de comida</strong><br/>"
            + hotel.getBoardName() + "</div>"
            + "</div>"
            + "</div>"

            // Price details
            + "<div class='price-section'>"
            + "<h3>Detalles del Precio</h3>"
            + "<div class='price-row'>"
            + "<span>Total del Paquete:</span>"
            + "<span class='price-value'>" + booking.getCurrency() + " "
            + booking.getTotalAmount() + "</span>"
            + "</div>"
            + "<div class='price-row'>"
            + "<span>Impuestos incluidos:</span>"
            + "<span>" + booking.getCurrency() + " " + booking.getTaxes() + "</span>"
            + "</div>"
            + (booking.getDiscount().compareTo(java.math.BigDecimal.ZERO) > 0
            ? "<div class='price-row discount'>"
            + "<span>Descuento aplicado:</span>"
            + "<span>- " + booking.getCurrency() + " " + booking.getDiscount() + "</span>"
            + "</div>"
            : "")
            + "</div>"

            // Important information
            + "<div class='important-section'>"
            + "<h3>Información importante</h3>"
            + "<p>Este es un paquete combinado. Presentar este voucher tanto en el "
            + "check-in del vuelo como en el check-in del hotel. Verificar todos los "
            + "horarios y políticas de cada servicio.</p>"
            + "</div>"

            // Footer
            + getFooter()

            + "</div>"
            + "</body>"
            + "</html>";
  }

  /**
   * Estilos CSS comunes para todos los vouchers.
   */
  private String getCommonStyles() {
    return "body { font-family: Arial, sans-serif; margin: 0; padding: 20px; color: #333; }"
            + ".container { max-width: 800px; margin: 0 auto; }"
            + ".header { background-color: " + PRIMARY_COLOR + "; padding: 20px; "
            + "text-align: center; border-radius: 8px 8px 0 0; }"
            + ".header h1 { color: white; margin: 0; font-size: 32px; }"
            + ".voucher-title { font-size: 24px; font-weight: bold; margin: 20px 0 10px; }"
            + ".code-section { text-align: right; font-size: "
            + "12px; color: #666; margin-bottom: 20px; }"
            + ".confirmation-bar { background-color: #4B5563; color: white; padding: 15px; "
            + "border-radius: 20px; font-weight: bold; margin: 20px 0; }"
            + ".info-section { margin: 20px 0; padding: 15px; border: 1px solid #ddd; "
            + "border-radius: 5px; }"
            + ".info-section h2 { color: " + PRIMARY_COLOR + "; margin: 10px 0; }"
            + ".info-section h3 { color: #333; margin: 10px 0; font-size: 14px; }"
            + ".flight-info, .hotel-dates { display: flex; justify-content: space-around; "
            + "margin: 20px 0; }"
            + ".flight-column, .date-column { text-align: center; flex: 1; }"
            + ".label { font-size: 12px; color: #666; margin: 5px 0; }"
            + ".value { font-size: 16px; font-weight: bold; margin: 5px 0; }"
            + ".time { font-size: 14px; margin: 5px 0; }"
            + ".location { font-size: 14px; color: " + PRIMARY_COLOR + "; font-weight: bold; }"
            + ".details-row { display: flex; justify-content: space-between; gap: 20px; }"
            + ".details-column { flex: 1; }"
            + ".price-section { background-color: #f3f4f6; padding: 15px; "
            + "border-radius: 5px; margin: 20px 0; border-left: 4px solid " + PRIMARY_COLOR + "; }"
            + ".price-section h3 { margin-top: 0; color: " + PRIMARY_COLOR + "; }"
            + ".price-row { display: flex; justify-content: space-between; "
            + "padding: 8px 0; border-bottom: 1px solid #ddd; }"
            + ".price-row:last-child { border-bottom: none; }"
            + ".price-value { font-weight: bold; font-size: 18px; color: " + PRIMARY_COLOR + "; }"
            + ".discount { color: #10b981; }"
            + ".important-section { background-color: #fef3c7; padding: 15px; "
            + "border-radius: 5px; margin: 20px 0; border-left: 4px solid #f59e0b; }"
            + ".section-divider { background-color: #e5e7eb; padding: 10px; font-weight: bold; "
            + "margin: 20px 0; border-radius: 5px; }"
            + ".footer { margin-top: 30px; padding: 20px; background-color: #f9fafb; "
            + "border-radius: 5px; text-align: center; }"
            + ".footer p { margin: 5px 0; font-size: 12px; color: #666; }";
  }

  /**
   * Footer común para todos los vouchers.
   */
  private String getFooter() {
    return "<div class='footer'>"
            + "<p><strong>¡Que tengas un excelente viaje!</strong></p>"
            + "<p>Te acompañamos antes, durante y después de tu viaje</p>"
            + "<p style='margin-top: 15px;'>Este es un correo "
            + "automático, por favor no responder.</p>"
            + "<p>© " + java.time.Year.now().getValue() + " DeViaje. "
            + "Todos los derechos reservados.</p>"
            + "</div>";
  }

  /**
   * Convierte HTML a PDF usando iText.
   */
  private byte[] convertHtmlToPdf(String htmlContent) throws Exception {
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      HtmlConverter.convertToPdf(htmlContent, outputStream);
      return outputStream.toByteArray();
    } catch (Exception e) {
      log.error("Error al convertir HTML a PDF: {}", e.getMessage(), e);
      throw new Exception("Error al generar el PDF del voucher: " + e.getMessage());
    }
  }

  /**
   * Formatea una fecha ISO a formato legible en español.
   */
  private String formatFlightDateTime(String isoDateTime) {
    try {
      LocalDateTime dateTime = LocalDateTime.parse(isoDateTime);
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm",
              new java.util.Locale("es", "AR"));
      return dateTime.format(formatter);
    } catch (Exception e) {
      return isoDateTime;
    }
  }

  /**
   * Construye el HTML de detalles del itinerario desde el JSON.
   */
  private String buildItineraryDetailsHtml(String itinerariesJson) {
    if (itinerariesJson == null || itinerariesJson.isEmpty()) {
      return "<div class='info-section'><p>Detalles del itinerario no disponibles</p></div>";
    }

    try {
      List<ItineraryDto> itineraries = objectMapper.readValue(
              itinerariesJson,
              new TypeReference<>() {}
      );

      StringBuilder html = new StringBuilder();
      html.append("<div class='info-section'><h3>Itinerarios</h3>");

      for (int i = 0; i < itineraries.size(); i++) {
        ItineraryDto itinerary = itineraries.get(i);
        List<SegmentDto> segments = itinerary.getSegments();

        // Calcular duración total del itinerario sumando los segmentos
        String totalDuration = calculateTotalDuration(segments);

        // Título del itinerario (Ida o Vuelta)
        html.append("<h4>").append(i == 0 ? "Vuelo de Ida" : "Vuelo de Regreso").append("</h4>");
        html.append("<p><strong>Duración total:</strong> ").append(totalDuration).append("</p>");

        // Detalles de cada segmento
        if (segments != null && !segments.isEmpty()) {
          html.append("<table style='width: 100%; border-collapse: collapse; margin-top: 10px;'>");
          html.append("<tr style='background-color: #f3f4f6;'>");
          html.append("<th style='padding: 8px; text-align: left; "
                  + "border: 1px solid #ddd;'>Salida</th>");
          html.append("<th style='padding: 8px; text-align: left; "
                  + "border: 1px solid #ddd;'>Llegada</th>");
          html.append("<th style='padding: 8px; text-align: left; "
                  + "border: 1px solid #ddd;'>Duración</th>");
          html.append("</tr>");

          for (SegmentDto segment : segments) {

            html.append("<tr>");
            DepartureArrivalDto departure = segment.getDeparture();
            String departureTime = formatFlightDateTime(departure.getAt());
            String departureCode = departure.getIataCode();
            html.append("<td style='padding: 8px; border: 1px solid #ddd;'>")
                    .append(departureCode).append("<br>").append(departureTime).append("</td>");

            DepartureArrivalDto arrival = segment.getArrival();
            String arrivalTime = formatFlightDateTime(arrival.getAt());
            String arrivalCode = arrival.getIataCode();
            html.append("<td style='padding: 8px; border: 1px solid #ddd;'>")
                    .append(arrivalCode).append("<br>").append(arrivalTime).append("</td>");

            String segmentDuration = segment.getDuration();
            html.append("<td style='padding: 8px; border: 1px solid #ddd;'>")
                    .append(formatDuration(segmentDuration)).append("</td>");

            html.append("</tr>");
          }
          html.append("</table>");
        }
      }

      html.append("</div>");
      return html.toString();

    } catch (Exception e) {
      log.error("Error al parsear itinerarios: {}", e.getMessage(), e);
      return "<div class='info-section'><p>Error al cargar detalles del itinerario</p></div>";
    }
  }

  /**
   * Calcula la duración total de un itinerario sumando las duraciones de todos los segmentos.
   */
  private String calculateTotalDuration(List<SegmentDto> segments) {
    if (segments == null || segments.isEmpty()) {
      return "N/A";
    }

    try {
      long totalMinutes = 0;

      for (SegmentDto segment : segments) {
        if (segment.getDuration() != null) {
          java.time.Duration d = java.time.Duration.parse(segment.getDuration());
          totalMinutes += d.toMinutes();
        }
      }

      long hours = totalMinutes / 60;
      long minutes = totalMinutes % 60;

      if (hours > 0 && minutes > 0) {
        return hours + "h " + minutes + "m";
      } else if (hours > 0) {
        return hours + "h";
      } else {
        return minutes + "m";
      }
    } catch (Exception e) {
      log.error("Error al calcular duración total: {}", e.getMessage());
      return "N/A";
    }
  }

  /**
   * Formatea una duración ISO 8601 (PT2H30M) a formato legible (2 h 30 m).
   */
  private String formatDuration(String duration) {
    if (duration == null || duration.isEmpty()) {
      return "N/A";
    }

    try {
      // Parsear formato ISO 8601 duration (PT2H30M)
      java.time.Duration d = java.time.Duration.parse(duration);
      long hours = d.toHours();
      long minutes = d.toMinutes() % 60;

      if (hours > 0 && minutes > 0) {
        return hours + "h " + minutes + "m";
      } else if (hours > 0) {
        return hours + "h";
      } else {
        return minutes + "m";
      }
    } catch (Exception e) {
      return duration; // Fallback
    }
  }
}