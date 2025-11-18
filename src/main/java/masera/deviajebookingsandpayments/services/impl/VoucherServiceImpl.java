package masera.deviajebookingsandpayments.services.impl;

import com.itextpdf.html2pdf.HtmlConverter;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

  private static final String PRIMARY_COLOR = "#8B5CF6";
  private static final String PRIMARY_LIGHT = "#A78BFA";

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
            + "</div>"

            // Flight details
            + "<div class='info-section'>"
            + "<h3>Código de Confirmación: " + flight.getExternalId() + "</h3>"
            + "<div class='flight-info'>"
            + "<div class='flight-column'>"
            + "<p class='label'>Salida</p>"
            + "<p class='value'>" + flight.getDepartureDate() + "</p>"
            + "<p class='location'>" + flight.getOrigin() + "</p>"
            + "</div>"
            + "<div class='flight-column'>"
            + "<p class='label'>Llegada</p>"
            + "<p class='value'>" + (flight.getReturnDate() != null
            ? flight.getReturnDate() : "N/A") + "</p>"
            + "<p class='location'>" + flight.getDestination() + "</p>"
            + "</div>"
            + "</div>"
            + "</div>"

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

            // Hotel details
            + "<div class='info-section'>"
            + "<h3>Código Alojamiento: " + hotel.getExternalId() + "</h3>"
            + "<h2>" + hotel.getHotelName() + "</h2>"
            + "<p>" + hotel.getDestinationName() + ", " + hotel.getCountryName() + "</p>"
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
  private String buildPackageVoucherHtml(BookingEntity booking,
                                         FlightBookingEntity flight,
                                         HotelBookingEntity hotel) {
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
            + "<h3>Código de Confirmación: " + flight.getExternalId() + "</h3>"
            + "<div class='flight-info'>"
            + "<div class='flight-column'>"
            + "<p class='label'>Salida</p>"
            + "<p class='value'>" + flight.getDepartureDate() + "</p>"
            + "<p class='location'>" + flight.getOrigin() + "</p>"
            + "</div>"
            + "<div class='flight-column'>"
            + "<p class='label'>Llegada</p>"
            + "<p class='value'>" + (flight.getReturnDate() != null
            ? flight.getReturnDate() : "N/A") + "</p>"
            + "<p class='location'>" + flight.getDestination() + "</p>"
            + "</div>"
            + "</div>"
            + "</div>"

            // Hotel section
            + "<div class='section-divider'>DETALLES DEL ALOJAMIENTO</div>"
            + "<div class='info-section'>"
            + "<h3>Código Alojamiento: " + hotel.getExternalId() + "</h3>"
            + "<h2>" + hotel.getHotelName() + "</h2>"
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
}