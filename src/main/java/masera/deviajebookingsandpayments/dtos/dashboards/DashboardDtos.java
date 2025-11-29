package masera.deviajebookingsandpayments.dtos.dashboards;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTOs específicos para cada endpoint del dashboard.
 */
public class DashboardDtos {

  /**
   * Dtos para el grafíco de reservas por tipo.
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class BookingsByTypeDto {

    private List<TypeCount> data;

    private KpisDto kpis;

    /**
     * Cuenta la cantidad
     * de reservas con sus comisiones por tipo.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TypeCount {
      private String bookingType;
      private long count;
      private BigDecimal totalRevenue;
      private BigDecimal totalCommission;
      private BigDecimal averageRevenue;
    }

    /**
     * Estadísticas del gráfico reservas por tipo.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KpisDto {
      private Long totalBookings;
      private BigDecimal totalRevenue;
      private BigDecimal totalCommissions;
      private BigDecimal averageBookingValue;
    }
  }

  /**
   * Datos para el gráfico ingresos a lo largo del tiempo.
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class RevenueOverTimeDto {
    private List<TimeSeriesPoint> data;
    private KpisDto kpis;
    private String granularity; // "DAILY", "MONTHLY", "YEARLY"

    /**
     * Suma por periodo de las ventas y comisiones.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TimeSeriesPoint {
      private String period; // "2025-01", "2025-01-15", "2025"
      private Long bookingsCount;
      private BigDecimal revenue;
      private BigDecimal commission;
    }

    /**
     * Estadísticas del gráfico de ingresos a lo largo del tiempo.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KpisDto {
      private BigDecimal totalRevenue;
      private BigDecimal totalCommission;
      private BigDecimal averageRevenuePerPeriod;
      private BigDecimal highestRevenue;
      private String highestRevenuePeriod;
    }
  }

  /**
   * Datos del gráfico Top Destinos.
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class TopDestinationsDto {
    private List<DestinationData> data;
    private KpisDto kpis;
    private Integer limit; // Top 5, 10, 20, etc.

    /**
     * Datos estadísticos por destino.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DestinationData {
      private String destination;
      private Long bookingsCount;
      private BigDecimal revenue;
      private Integer averageNights;
      private BigDecimal averagePrice;
    }

    /**
     * Estadísticas del gráfico Top Destinos.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KpisDto {
      private Long totalBookings;
      private Integer uniqueDestinations;
      private String topDestination;
      private BigDecimal totalRevenue;
    }
  }

  /**
   * Datos del gráfico de top aerolíneas.
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class TopCarriersDto {
    private List<CarrierData> data;
    private KpisDto kpis;
    private Integer limit;

    /**
     * Datos de la aerolínea.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CarrierData {
      private String carrierName;
      private Long bookingsCount;
      private BigDecimal totalRevenue;
      private Integer averagePassengers;
      private BigDecimal averagePrice;
    }

    /**
     * Estadísticas para el gráfico top aerolíneas.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KpisDto {
      private Long totalFlightBookings;
      private Integer uniqueCarriers;
      private String topCarrier;
      private BigDecimal totalFlightRevenue;
    }
  }

  /**
   *  Datos para el gráfico de pagos por estado.
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class PaymentsByStatusDto {
    private List<StatusData> data;
    private KpisDto kpis;

    /**
     * Datos del estado de pago.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StatusData {
      private String status;
      private Long count;
      private BigDecimal amount;
      private Double percentage;
    }

    /**
     * Estadísticas para el gráfico de pagos por estado.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KpisDto {
      private Long totalPayments;
      private BigDecimal totalAmount;
      private Long approvedPayments;
      private BigDecimal approvedAmount;
      private Long refundedPayments;
      private Double approvalRate;
    }
  }

  // ==================== SUMMARY (para vista principal) ====================
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class DashboardSummaryDto {
    private GlobalKpis globalKpis;
    private List<MiniChartData> miniCharts;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GlobalKpis {
      private Long totalBookings;
      private BigDecimal totalRevenue;
      private BigDecimal totalCommissions;
      private BigDecimal averageBookingValue;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MiniChartData {
      private String chartType; // "BOOKINGS_BY_TYPE", "REVENUE_OVER_TIME", etc.
      private String title;
      private Object previewData; // Datos simplificados para vista mini
    }
  }
}