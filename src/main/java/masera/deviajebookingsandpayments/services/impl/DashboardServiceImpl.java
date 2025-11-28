package masera.deviajebookingsandpayments.services.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import masera.deviajebookingsandpayments.dtos.dashboards.DashboardDtos;
import masera.deviajebookingsandpayments.entities.BookingEntity;
import masera.deviajebookingsandpayments.entities.FlightBookingEntity;
import masera.deviajebookingsandpayments.entities.HotelBookingEntity;
import masera.deviajebookingsandpayments.entities.PaymentEntity;
import masera.deviajebookingsandpayments.repositories.BookingRepository;
import masera.deviajebookingsandpayments.repositories.FlightBookingRepository;
import masera.deviajebookingsandpayments.repositories.HotelBookingRepository;
import masera.deviajebookingsandpayments.repositories.PaymentRepository;
import masera.deviajebookingsandpayments.services.interfaces.DashboardService;
import org.springframework.stereotype.Service;

/**
 * Implementación del servicio de Dashboard con métodos separados.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {

  private final BookingRepository bookingRepository;

  private final PaymentRepository paymentRepository;

  private final FlightBookingRepository flightBookingRepository;

  private final HotelBookingRepository hotelBookingRepository;


  private List<BookingEntity> filterBookings(LocalDateTime startDate,
                                             LocalDateTime endDate,
                                             String bookingType,
                                             String bookingStatus) {
    List<BookingEntity> bookings = bookingRepository.findAll();

    return bookings.stream()
            .filter(b -> filterByDateRange(b, startDate, endDate))
            .filter(b -> filterByType(b, bookingType))
            .filter(b -> filterByStatus(b, bookingStatus))
            .collect(Collectors.toList());
  }

  private List<PaymentEntity> filterPayments(LocalDateTime startDate,
                                             LocalDateTime endDate,
                                             String paymentMethod) {
    List<PaymentEntity> payments = paymentRepository.findAll();

    return payments.stream()
            .filter(p -> filterPaymentByDateRange(p, startDate, endDate))
            .filter(p -> filterByPaymentMethod(p, paymentMethod))
            .collect(Collectors.toList());
  }

  private boolean filterByDateRange(BookingEntity booking,
                                    LocalDateTime startDate,
                                    LocalDateTime endDate) {
    if (startDate == null || endDate == null) {
      return true;
    }
    return booking.getCreatedDatetime() != null
            && !booking.getCreatedDatetime().isBefore(startDate)
            && !booking.getCreatedDatetime().isAfter(endDate);
  }

  private boolean filterPaymentByDateRange(PaymentEntity payment,
                                           LocalDateTime startDate,
                                           LocalDateTime endDate) {
    if (startDate == null || endDate == null) {
      return true;
    }
    return payment.getDate() != null
            && !payment.getDate().isBefore(startDate)
            && !payment.getDate().isAfter(endDate);
  }

  private boolean filterByType(BookingEntity booking, String type) {
    if (type == null || type.isEmpty()) {
      return true;
    }
    return booking.getType().name().equalsIgnoreCase(type);
  }

  private boolean filterByStatus(BookingEntity booking, String status) {
    if (status == null || status.isEmpty()) {
      return true;
    }
    return booking.getStatus().name().equalsIgnoreCase(status);
  }

  private boolean filterByPaymentMethod(PaymentEntity payment, String method) {
    if (method == null || method.isEmpty()) {
      return true;
    }
    return payment.getMethod() != null
            && payment.getMethod().equalsIgnoreCase(method);
  }

  private String getCarrierName(String iataCode) {
    Map<String, String> carriers = Map.of(
            "AA", "American Airlines",
            "AR", "Aerolíneas Argentinas",
            "LA", "LATAM Airlines",
            "UA", "United Airlines",
            "DL", "Delta Airlines",
            "BA", "British Airways",
            "IB", "Iberia",
            "AF", "Air France",
            "LH", "Lufthansa"
    );
    return carriers.getOrDefault(iataCode, iataCode);
  }

  // ==================== SUMMARY (Vista Principal) ====================

  @Override
  public DashboardDtos.DashboardSummaryDto getDashboardSummary(LocalDateTime startDate,
                                                               LocalDateTime endDate) {
    log.info("Generando resumen del dashboard");

    List<BookingEntity> bookings = filterBookings(startDate, endDate, null, null);

    // KPIs globales
    long totalBookings = bookings.size();
    BigDecimal totalRevenue = bookings.stream()
            .map(BookingEntity::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalCommissions = bookings.stream()
            .map(BookingEntity::getCommission)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal averageBookingValue = totalBookings > 0
            ? totalRevenue.divide(BigDecimal.valueOf(totalBookings), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

    DashboardDtos.DashboardSummaryDto.GlobalKpis globalKpis =
            DashboardDtos.DashboardSummaryDto.GlobalKpis.builder()
                    .totalBookings(totalBookings)
                    .totalRevenue(totalRevenue)
                    .totalCommissions(totalCommissions)
                    .averageBookingValue(averageBookingValue)
                    .build();

    // Mini charts (datos simplificados para vista previa)
    List<DashboardDtos.DashboardSummaryDto.MiniChartData> miniCharts = new ArrayList<>();

    // Mini chart 1: Bookings by type
    Map<String, Long> bookingsByType = bookings.stream()
            .collect(Collectors.groupingBy(
                    b -> b.getType().name(),
                    Collectors.counting()
            ));
    miniCharts.add(DashboardDtos.DashboardSummaryDto.MiniChartData.builder()
            .chartType("BOOKINGS_BY_TYPE")
            .title("Reservas por Tipo")
            .previewData(bookingsByType)
            .build());

    // Mini chart 2: Revenue last 7 days
    Map<String, BigDecimal> revenueLast7Days = calculateRevenueLast7Days(bookings);
    miniCharts.add(DashboardDtos.DashboardSummaryDto.MiniChartData.builder()
            .chartType("REVENUE_OVER_TIME")
            .title("Revenue Últimos 7 Días")
            .previewData(revenueLast7Days)
            .build());

    return DashboardDtos.DashboardSummaryDto.builder()
            .globalKpis(globalKpis)
            .miniCharts(miniCharts)
            .build();
  }

  private Map<String, BigDecimal> calculateRevenueLast7Days(List<BookingEntity> bookings) {
    LocalDate endDate = LocalDate.now();
    LocalDate startDate = endDate.minusDays(6);
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");

    Map<LocalDate, BigDecimal> dataMap = new LinkedHashMap<>();

    // Inicializar todos los días con 0
    for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
      dataMap.put(date, BigDecimal.ZERO);
    }

    // Llenar con datos reales
    bookings.forEach(booking -> {
      LocalDate bookingDate = booking.getCreatedDatetime().toLocalDate();
      if (!bookingDate.isBefore(startDate) && !bookingDate.isAfter(endDate)) {
        dataMap.put(bookingDate,
                dataMap.get(bookingDate).add(booking.getTotalAmount()));
      }
    });

    // Convertir a Map<String, BigDecimal> con formato de fecha
    Map<String, BigDecimal> result = new LinkedHashMap<>();
    dataMap.forEach((date, amount) -> result.put(date.format(formatter), amount));
    return result;
  }

  // BOOKINGS BY TYPE

  @Override
  public DashboardDtos.BookingsByTypeDto getBookingsByType(LocalDateTime startDate,
                                                           LocalDateTime endDate,
                                                           String bookingType,
                                                           String bookingStatus,
                                                           Integer agentId,
                                                           Integer clientId) {
    List<BookingEntity> bookings = filterBookings(startDate, endDate, bookingType, bookingStatus);

    log.info("agent id", agentId);
    if (agentId != null) {
      bookings = bookings.stream()
              .filter(b -> b.getAgentId() != null && b.getAgentId().equals(agentId))
              .collect(Collectors.toList());
    }

    if (clientId != null) {
      bookings = bookings.stream()
              .filter(b -> b.getClientId() != null && b.getClientId().equals(clientId))
              .collect(Collectors.toList());
    }

    // Agrupar por tipo
    Map<String, List<BookingEntity>> groupedByType = bookings.stream()
            .collect(Collectors.groupingBy(b -> b.getType().name()));

    List<DashboardDtos.BookingsByTypeDto.TypeCount> data = new ArrayList<>();
    groupedByType.forEach((type, bookingList) -> {
      long count = bookingList.size();
      BigDecimal totalRevenue = bookingList.stream()
              .map(BookingEntity::getTotalAmount)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
      BigDecimal totalCommission = bookingList.stream()
              .map(BookingEntity::getCommission)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
      BigDecimal averageRevenue = count > 0
              ? totalRevenue.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
              : BigDecimal.ZERO;

      data.add(DashboardDtos.BookingsByTypeDto.TypeCount.builder()
              .bookingType(type)
              .count(count)
              .totalRevenue(totalRevenue)
              .totalCommission(totalCommission)
              .averageRevenue(averageRevenue)
              .build());
    });

    // KPIs
    long totalBookings = bookings.size();
    BigDecimal totalRevenue = data.stream()
            .map(DashboardDtos.BookingsByTypeDto.TypeCount::getTotalRevenue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalCommissions = data.stream()
            .map(DashboardDtos.BookingsByTypeDto.TypeCount::getTotalCommission)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal averageBookingValue = totalBookings > 0
            ? totalRevenue.divide(BigDecimal.valueOf(totalBookings), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

    DashboardDtos.BookingsByTypeDto.KpisDto kpis = DashboardDtos.BookingsByTypeDto.KpisDto.builder()
            .totalBookings(totalBookings)
            .totalRevenue(totalRevenue)
            .totalCommissions(totalCommissions)
            .averageBookingValue(averageBookingValue)
            .build();

    return DashboardDtos.BookingsByTypeDto.builder()
            .data(data)
            .kpis(kpis)
            .build();
  }

  // ==================== REVENUE OVER TIME ====================

  @Override
  public DashboardDtos.RevenueOverTimeDto getRevenueOverTime(LocalDateTime startDate,
                                                             LocalDateTime endDate,
                                                             String granularity,
                                                             String bookingType) {
    log.info("Obteniendo revenue en el tiempo con granularidad: {}", granularity);

    List<BookingEntity> bookings = filterBookings(startDate, endDate, bookingType, null);

    List<DashboardDtos.RevenueOverTimeDto.TimeSeriesPoint> data = switch (granularity.toUpperCase()) {
      case "DAILY" -> calculateDailyRevenue(bookings, startDate, endDate);
      case "YEARLY" -> calculateYearlyRevenue(bookings);
      default -> calculateMonthlyRevenue(bookings, startDate, endDate);
    };

    // KPIs
    BigDecimal totalRevenue = data.stream()
            .map(DashboardDtos.RevenueOverTimeDto.TimeSeriesPoint::getRevenue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal averageRevenuePerPeriod = !data.isEmpty()
            ? totalRevenue.divide(BigDecimal.valueOf(data.size()), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

    DashboardDtos.RevenueOverTimeDto.TimeSeriesPoint highest = data.stream()
            .max((a, b) -> a.getRevenue().compareTo(b.getRevenue()))
            .orElse(null);

    BigDecimal highestRevenue = highest != null ? highest.getRevenue() : BigDecimal.ZERO;
    String highestRevenuePeriod = highest != null ? highest.getPeriod() : "";

    DashboardDtos.RevenueOverTimeDto.KpisDto kpis = DashboardDtos.RevenueOverTimeDto.KpisDto.builder()
            .totalRevenue(totalRevenue)
            .averageRevenuePerPeriod(averageRevenuePerPeriod)
            .highestRevenue(highestRevenue)
            .highestRevenuePeriod(highestRevenuePeriod)
            .build();

    return DashboardDtos.RevenueOverTimeDto.builder()
            .data(data)
            .kpis(kpis)
            .granularity(granularity.toUpperCase())
            .build();
  }

  private List<DashboardDtos.RevenueOverTimeDto.TimeSeriesPoint> calculateDailyRevenue(
          List<BookingEntity> bookings,
          LocalDateTime startDate,
          LocalDateTime endDate) {

    LocalDate start = startDate != null ? startDate.toLocalDate() : LocalDate.now().minusDays(30);
    LocalDate end = endDate != null ? endDate.toLocalDate() : LocalDate.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    Map<LocalDate, DashboardDtos.RevenueOverTimeDto.TimeSeriesPoint> dataMap = new LinkedHashMap<>();

    // Inicializar todos los días
    for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
      dataMap.put(date, DashboardDtos.RevenueOverTimeDto.TimeSeriesPoint.builder()
              .period(date.format(formatter))
              .bookingsCount(0L)
              .revenue(BigDecimal.ZERO)
              .commission(BigDecimal.ZERO)
              .build());
    }

    // Llenar con datos reales
    bookings.forEach(booking -> {
      LocalDate bookingDate = booking.getCreatedDatetime().toLocalDate();
      if (!bookingDate.isBefore(start) && !bookingDate.isAfter(end)) {
        DashboardDtos.RevenueOverTimeDto.TimeSeriesPoint existing = dataMap.get(bookingDate);
        dataMap.put(bookingDate, DashboardDtos.RevenueOverTimeDto.TimeSeriesPoint.builder()
                .period(existing.getPeriod())
                .bookingsCount(existing.getBookingsCount() + 1)
                .revenue(existing.getRevenue().add(booking.getTotalAmount()))
                .commission(existing.getCommission().add(booking.getCommission()))
                .build());
      }
    });

    return new ArrayList<>(dataMap.values());
  }

  private List<DashboardDtos.RevenueOverTimeDto.TimeSeriesPoint> calculateMonthlyRevenue(
          List<BookingEntity> bookings,
          LocalDateTime startDate,
          LocalDateTime endDate) {

    LocalDate start = startDate != null ? startDate.toLocalDate().withDayOfMonth(1)
            : LocalDate.now().minusMonths(11).withDayOfMonth(1);
    LocalDate end = endDate != null ? endDate.toLocalDate().with(TemporalAdjusters.lastDayOfMonth())
            : LocalDate.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy");

    Map<String, DashboardDtos.RevenueOverTimeDto.TimeSeriesPoint> dataMap = new LinkedHashMap<>();

    // Inicializar todos los meses
    LocalDate currentMonth = start;
    while (!currentMonth.isAfter(end)) {
      String periodKey = currentMonth.format(formatter);
      dataMap.put(periodKey, DashboardDtos.RevenueOverTimeDto.TimeSeriesPoint.builder()
              .period(periodKey)
              .bookingsCount(0L)
              .revenue(BigDecimal.ZERO)
              .commission(BigDecimal.ZERO)
              .build());
      currentMonth = currentMonth.plusMonths(1);
    }

    // Llenar con datos reales
    bookings.forEach(booking -> {
      String monthKey = booking.getCreatedDatetime().format(formatter);
      if (dataMap.containsKey(monthKey)) {
        DashboardDtos.RevenueOverTimeDto.TimeSeriesPoint existing = dataMap.get(monthKey);
        dataMap.put(monthKey, DashboardDtos.RevenueOverTimeDto.TimeSeriesPoint.builder()
                .period(existing.getPeriod())
                .bookingsCount(existing.getBookingsCount() + 1)
                .revenue(existing.getRevenue().add(booking.getTotalAmount()))
                .commission(existing.getCommission().add(booking.getCommission()))
                .build());
      }
    });

    return new ArrayList<>(dataMap.values());
  }

  private List<DashboardDtos.RevenueOverTimeDto.TimeSeriesPoint> calculateYearlyRevenue(
          List<BookingEntity> bookings) {

    Map<String, DashboardDtos.RevenueOverTimeDto.TimeSeriesPoint> dataMap = new LinkedHashMap<>();

    bookings.forEach(booking -> {
      String year = booking.getCreatedDatetime().getYear() + "";
      DashboardDtos.RevenueOverTimeDto.TimeSeriesPoint existing = dataMap.getOrDefault(year,
              DashboardDtos.RevenueOverTimeDto.TimeSeriesPoint.builder()
                      .period(year)
                      .bookingsCount(0L)
                      .revenue(BigDecimal.ZERO)
                      .commission(BigDecimal.ZERO)
                      .build());

      dataMap.put(year, DashboardDtos.RevenueOverTimeDto.TimeSeriesPoint.builder()
              .period(year)
              .bookingsCount(existing.getBookingsCount() + 1)
              .revenue(existing.getRevenue().add(booking.getTotalAmount()))
              .commission(existing.getCommission().add(booking.getCommission()))
              .build());
    });

    return new ArrayList<>(dataMap.values());
  }

  // ==================== TOP DESTINATIONS ====================

  @Override
  public DashboardDtos.TopDestinationsDto getTopDestinations(LocalDateTime startDate,
                                                             LocalDateTime endDate,
                                                             Integer limit,
                                                             String bookingStatus) {
    log.info("Obteniendo top {} destinos", limit);

    List<BookingEntity> bookings = filterBookings(startDate, endDate, "HOTEL", bookingStatus);
    List<Long> bookingIds = bookings.stream()
            .map(BookingEntity::getId)
            .collect(Collectors.toList());

    List<HotelBookingEntity> hotelBookings = hotelBookingRepository.findAll().stream()
            .filter(hb -> bookingIds.contains(hb.getBookingEntity().getId()))
            .filter(hb -> hb.getDestinationName() != null)
            .collect(Collectors.toList());

    // Agrupar por destino
    Map<String, List<HotelBookingEntity>> groupedByDestination = hotelBookings.stream()
            .collect(Collectors.groupingBy(HotelBookingEntity::getDestinationName));

    List<DashboardDtos.TopDestinationsDto.DestinationData> data = new ArrayList<>();
    groupedByDestination.forEach((destination, hotelList) -> {
      Long bookingsCount = (long) hotelList.size();
      BigDecimal revenue = hotelList.stream()
              .map(HotelBookingEntity::getTotalPrice)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
      Double averageNights = hotelList.stream()
              .mapToInt(HotelBookingEntity::getNumberOfNights)
              .average()
              .orElse(0.0);
      BigDecimal averagePrice = bookingsCount > 0
              ? revenue.divide(BigDecimal.valueOf(bookingsCount), 2, RoundingMode.HALF_UP)
              : BigDecimal.ZERO;

      data.add(DashboardDtos.TopDestinationsDto.DestinationData.builder()
              .destination(destination)
              .bookingsCount(bookingsCount)
              .revenue(revenue)
              .averageNights(averageNights.intValue())
              .averagePrice(averagePrice)
              .build());
    });

    // Ordenar por bookingsCount descendente y limitar
    data.sort((a, b) -> b.getBookingsCount().compareTo(a.getBookingsCount()));
    List<DashboardDtos.TopDestinationsDto.DestinationData> limitedData = data.stream()
            .limit(limit)
            .collect(Collectors.toList());

    // KPIs
    Long totalHotelBookings = (long) hotelBookings.size();
    Integer uniqueDestinations = groupedByDestination.size();
    String topDestination = !limitedData.isEmpty() ? limitedData.get(0).getDestination() : "";
    BigDecimal totalHotelRevenue = data.stream()
            .map(DashboardDtos.TopDestinationsDto.DestinationData::getRevenue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    DashboardDtos.TopDestinationsDto.KpisDto kpis = DashboardDtos.TopDestinationsDto.KpisDto.builder()
            .totalHotelBookings(totalHotelBookings)
            .uniqueDestinations(uniqueDestinations)
            .topDestination(topDestination)
            .totalHotelRevenue(totalHotelRevenue)
            .build();

    return DashboardDtos.TopDestinationsDto.builder()
            .data(limitedData)
            .kpis(kpis)
            .limit(limit)
            .build();
  }

  // ==================== TOP CARRIERS ====================

  @Override
  public DashboardDtos.TopCarriersDto getTopCarriers(LocalDateTime startDate,
                                                     LocalDateTime endDate,
                                                     Integer limit,
                                                     String bookingStatus) {
    log.info("Obteniendo top {} aerolíneas", limit);

    List<BookingEntity> bookings = filterBookings(startDate, endDate, "FLIGHT", bookingStatus);
    List<Long> bookingIds = bookings.stream()
            .map(BookingEntity::getId)
            .collect(Collectors.toList());

    List<FlightBookingEntity> flightBookings = flightBookingRepository.findAll().stream()
            .filter(fb -> bookingIds.contains(fb.getBookingEntity().getId()))
            .filter(fb -> fb.getCarrier() != null)
            .collect(Collectors.toList());

    // Agrupar por aerolínea
    Map<String, List<FlightBookingEntity>> groupedByCarrier = flightBookings.stream()
            .collect(Collectors.groupingBy(FlightBookingEntity::getCarrier));

    List<DashboardDtos.TopCarriersDto.CarrierData> data = new ArrayList<>();
    groupedByCarrier.forEach((carrierCode, flightList) -> {
      Long bookingsCount = (long) flightList.size();
      BigDecimal revenue = flightList.stream()
              .map(FlightBookingEntity::getTotalPrice)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
      Double averagePassengers = flightList.stream()
              .mapToInt(fb -> fb.getAdults() + fb.getChildren() + fb.getInfants())
              .average()
              .orElse(0.0);

      data.add(DashboardDtos.TopCarriersDto.CarrierData.builder()
              .carrierCode(carrierCode)
              .carrierName(getCarrierName(carrierCode))
              .bookingsCount(bookingsCount)
              .revenue(revenue)
              .averagePassengers(averagePassengers.intValue())
              .build());
    });

    // Ordenar por bookingsCount descendente y limitar
    data.sort((a, b) -> b.getBookingsCount().compareTo(a.getBookingsCount()));
    List<DashboardDtos.TopCarriersDto.CarrierData> limitedData = data.stream()
            .limit(limit)
            .collect(Collectors.toList());

    // KPIs
    Long totalFlightBookings = (long) flightBookings.size();
    Integer uniqueCarriers = groupedByCarrier.size();
    String topCarrier = !limitedData.isEmpty() ? limitedData.get(0).getCarrierName() : "";
    BigDecimal totalFlightRevenue = data.stream()
            .map(DashboardDtos.TopCarriersDto.CarrierData::getRevenue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    DashboardDtos.TopCarriersDto.KpisDto kpis = DashboardDtos.TopCarriersDto.KpisDto.builder()
            .totalFlightBookings(totalFlightBookings)
            .uniqueCarriers(uniqueCarriers)
            .topCarrier(topCarrier)
            .totalFlightRevenue(totalFlightRevenue)
            .build();

    return DashboardDtos.TopCarriersDto.builder()
            .data(limitedData)
            .kpis(kpis)
            .limit(limit)
            .build();
  }

  // ==================== PAYMENTS BY STATUS ====================

  @Override
  public DashboardDtos.PaymentsByStatusDto getPaymentsByStatus(LocalDateTime startDate,
                                                               LocalDateTime endDate,
                                                               String paymentMethod) {
    log.info("Obteniendo pagos por estado");

    List<PaymentEntity> payments = filterPayments(startDate, endDate, paymentMethod);

    // Agrupar por estado
    Map<String, List<PaymentEntity>> groupedByStatus = payments.stream()
            .collect(Collectors.groupingBy(p -> p.getStatus().name()));

    List<DashboardDtos.PaymentsByStatusDto.StatusData> data = new ArrayList<>();
    BigDecimal totalAmount = payments.stream()
            .map(PaymentEntity::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    groupedByStatus.forEach((status, paymentList) -> {
      Long count = (long) paymentList.size();
      BigDecimal amount = paymentList.stream()
              .map(PaymentEntity::getAmount)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
      Double percentage = totalAmount.compareTo(BigDecimal.ZERO) > 0
              ? amount.divide(totalAmount, 4, RoundingMode.HALF_UP)
              .multiply(BigDecimal.valueOf(100)).doubleValue()
              : 0.0;

      data.add(DashboardDtos.PaymentsByStatusDto.StatusData.builder()
              .status(status)
              .count(count)
              .amount(amount)
              .percentage(percentage)
              .build());
    });

    // KPIs
    Long totalPayments = (long) payments.size();
    Long approvedPayments = Long.valueOf(groupedByStatus.getOrDefault("APPROVED", List.of()).size());
    BigDecimal approvedAmount = groupedByStatus.getOrDefault("APPROVED", List.of()).stream()
            .map(PaymentEntity::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    Long rejectedPayments = Long.valueOf(groupedByStatus.getOrDefault("REJECTED", List.of()).size());
    Double approvalRate = totalPayments > 0
            ? (approvedPayments.doubleValue() / totalPayments) * 100
            : 0.0;

    DashboardDtos.PaymentsByStatusDto.KpisDto kpis = DashboardDtos.PaymentsByStatusDto.KpisDto.builder()
            .totalPayments(totalPayments)
            .totalAmount(totalAmount)
            .approvedPayments((long) approvedPayments)
            .approvedAmount(approvedAmount)
            .rejectedPayments((long) rejectedPayments)
            .approvalRate(approvalRate)
            .build();

    return DashboardDtos.PaymentsByStatusDto.builder()
            .data(data)
            .kpis(kpis)
            .build();
  }

  // ==================== BOOKINGS BY STATUS ====================

  @Override
  public DashboardDtos.BookingsByStatusDto getBookingsByStatus(LocalDateTime startDate,
                                                               LocalDateTime endDate,
                                                               String bookingType) {
    log.info("Obteniendo reservas por estado");

    List<BookingEntity> bookings = filterBookings(startDate, endDate, bookingType, null);

    // Agrupar por estado
    Map<String, List<BookingEntity>> groupedByStatus = bookings.stream()
            .collect(Collectors.groupingBy(b -> b.getStatus().name()));

    List<DashboardDtos.BookingsByStatusDto.StatusData> data = new ArrayList<>();
    Long totalBookings = (long) bookings.size();

    groupedByStatus.forEach((status, bookingList) -> {
      Long count = (long) bookingList.size();
      BigDecimal revenue = bookingList.stream()
              .map(BookingEntity::getTotalAmount)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
      Double percentage = totalBookings > 0
              ? (count.doubleValue() / totalBookings) * 100
              : 0.0;

      data.add(DashboardDtos.BookingsByStatusDto.StatusData.builder()
              .status(status)
              .count(count)
              .revenue(revenue)
              .percentage(percentage)
              .build());
    });

    // KPIs
    Long confirmedBookings = (long) groupedByStatus.getOrDefault("CONFIRMED", List.of()).size();
    Long cancelledBookings = (long) groupedByStatus.getOrDefault("CANCELLED", List.of()).size();
    Double cancellationRate = totalBookings > 0
            ? (cancelledBookings.doubleValue() / totalBookings) * 100
            : 0.0;

    DashboardDtos.BookingsByStatusDto.KpisDto kpis = DashboardDtos.BookingsByStatusDto.KpisDto.builder()
            .totalBookings(totalBookings)
            .confirmedBookings(confirmedBookings)
            .cancelledBookings(cancelledBookings)
            .cancellationRate(cancellationRate)
            .build();

    return DashboardDtos.BookingsByStatusDto.builder()
            .data(data)
            .kpis(kpis)
            .build();
  }
}