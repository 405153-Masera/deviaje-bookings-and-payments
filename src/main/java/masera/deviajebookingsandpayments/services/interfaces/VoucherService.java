package masera.deviajebookingsandpayments.services.interfaces;

import masera.deviajebookingsandpayments.entities.BookingEntity;
import org.springframework.stereotype.Service;

/**
 * Servicio para generación de vouchers en PDF.
 */
@Service
public interface VoucherService {

  /**
   * Genera un voucher en PDF según el tipo de reserva.
   *
   * @param booking entidad de reserva completa
   * @return PDF generado en bytes
   * @throws Exception si ocurre un error al generar el PDF
   */
  byte[] generateVoucher(BookingEntity booking) throws Exception;

  /**
   * Genera un voucher de vuelo.
   *
   * @param booking entidad de reserva de vuelo
   * @return PDF generado en bytes
   * @throws Exception si ocurre un error al generar el PDF
   */
  byte[] generateFlightVoucher(BookingEntity booking) throws Exception;

  /**
   * Genera un voucher de hotel.
   *
   * @param booking entidad de reserva de hotel
   * @return PDF generado en bytes
   * @throws Exception si ocurre un error al generar el PDF
   */
  byte[] generateHotelVoucher(BookingEntity booking) throws Exception;

  /**
   * Genera un voucher de paquete.
   *
   * @param booking entidad de reserva de paquete
   * @return PDF generado en bytes
   * @throws Exception si ocurre un error al generar el PDF
   */
  byte[] generatePackageVoucher(BookingEntity booking) throws Exception;
}