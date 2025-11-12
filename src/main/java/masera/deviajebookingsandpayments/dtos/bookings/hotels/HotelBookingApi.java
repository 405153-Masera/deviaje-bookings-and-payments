package masera.deviajebookingsandpayments.dtos.bookings.hotels;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa la reserva de hotelbeds.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class HotelBookingApi {

  private String reference;

  private String clientReference;

  private String creationDate;

  private String status;

  private ModificationPolicies modificationPolicies;

  private String creationUser;

  private Holder holder;

  private Hotel hotel;

  private String remark;

  private InvoiceCompany invoiceCompany;

  private BigDecimal totalNet;

  private BigDecimal pendingAmount;

  private String currency;

  /**
   * DTO que representa las políticas de la reserva.
   */
  @Data
  public static class ModificationPolicies {

    private boolean cancellation;

    private boolean modification;
  }

  /**
   * DtO que representa al titular de la reserva.
   */
  @Data
  public static class Holder {

    private String name;

    private String surname;
  }

  /**
   * DTO que representa un hotel.
   */
  @Data
  public static class Hotel {

    private String checkOut;

    private String checkIn;

    private int code;

    private String name;

    private String categoryCode;

    private String categoryName;

    private String destinationCode;

    private String destinationName;

    private int zoneCode;

    private String zoneName;

    private String latitude;

    private String longitude;

    private List<Room> rooms;

    private BigDecimal totalNet;

    private String currency;

    private Supplier supplier;
  }

  /**
   * DTO que representa una habitación de hotel.
   */
  @Data
  public static class Room {

    private String status;

    private int id;

    private String code;

    private String name;

    private List<Pax> paxes;

    private List<Rate> rates;
  }

  /**
   * DTO que representa un pasajero.
   */
  @Data
  public static class Pax {

    private int roomId;

    private String type;

    private String name;

    private String surname;
  }

  /**
   * DTO que representa una tarifa de hotel.
   */
  @Data
  public static class Rate {

    private String rateClass;

    private BigDecimal net;

    private String rateComments;

    private String paymentType;

    private boolean packaging;

    private String boardCode;

    private String boardName;

    private List<CancellationPolicy> cancellationPolicies;

    private RateBreakDown rateBreakDown;

    private int rooms;

    private int adults;

    private int children;
  }

  /**
   * DTO que representa la política de cancelación del hotel.
   */
  @Data
  public static class CancellationPolicy {

    private BigDecimal amount;

    private String from;
  }

  /**
   * DTO que representa una lista de descuentos aplicados.
   */
  @Data
  public static class RateBreakDown {

    private List<RateDiscount> rateDiscounts;
  }

  /**
   * DTO que representa un descuento.
   */
  @Data
  public static class RateDiscount {

    private String code;

    private String name;

    private BigDecimal amount;
  }

  /**
   * DTO que representa al proveedor.
   */
  @Data
  public static class Supplier {

    private String name;

    private String vatNumber;
  }

  /**
   * DTO que representa ula factura.
   */
  @Data
  public static class InvoiceCompany {

    private String code;

    private String company;

    private String registrationNumber;
  }
}
