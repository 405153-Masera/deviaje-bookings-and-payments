package masera.deviajebookingsandpayments.dtos.bookings.hotels;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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

  @Data
  public static class ModificationPolicies {

    private boolean cancellation;

    private boolean modification;
  }

  @Data
  public static class Holder {

    private String name;

    private String surname;
  }

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

  @Data
  public static class Room {

    private String status;

    private int id;

    private String code;

    private String name;

    private List<Pax> paxes;

    private List<Rate> rates;
  }

  @Data
  public static class Pax {

    private int roomId;

    private String type;

    private String name;

    private String surname;
  }

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

  @Data
  public static class CancellationPolicy {

    private BigDecimal amount;

    private String from;
  }

  @Data
  public static class RateBreakDown {

    private List<RateDiscount> rateDiscounts;
  }

  @Data
  public static class RateDiscount {

    private String code;

    private String name;

    private BigDecimal amount;
  }

  @Data
  public static class Supplier {

    private String name;

    private String vatNumber;
  }

  @Data
  public static class InvoiceCompany {

    private String code;

    private String company;

    private String registrationNumber;
  }
}
