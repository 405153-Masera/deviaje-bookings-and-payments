package masera.deviajebookingsandpayments.services.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
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

  //region Métodos para la vista principal de los gráficos
  @Override
  public DashboardDtos.DashboardSummaryDto getDashboardSummary(LocalDateTime startDate,
                                                               LocalDateTime endDate,
                                                               String bookingStatus,
                                                               String bookingType) {
    log.info("Generando resumen del dashboard");

    List<BookingEntity> bookings = filterBookings(startDate, endDate, bookingType, bookingStatus);
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
    LocalDateTime end = LocalDate.now().atStartOfDay();
    LocalDateTime start = LocalDate.now().minusDays(6).atTime(23, 59, 59);
    List<DashboardDtos.RevenueOverTimeDto.TimeSeriesPoint> revenueLast7Days =
            calculateDailyRevenue(bookings, start, end);
    miniCharts.add(DashboardDtos.DashboardSummaryDto.MiniChartData.builder()
            .chartType("REVENUE_OVER_TIME")
            .title("Ventas por Día")
            .previewData(revenueLast7Days)
            .build());

    // Mini chart 3: Top 5 Destinations (HOTELS)
    List<Long> hotelBookingIds = bookings.stream()
            .map(BookingEntity::getId)
            .toList();

    Map<String, Long> destinations = hotelBookingRepository.findAll().stream()
            .filter(hb -> hotelBookingIds.contains(hb.getBookingEntity().getId()))
            .filter(hb -> hb.getDestinationName() != null)
            .collect(Collectors.groupingBy(
                    hb -> hb.getDestinationName() + ", "
                            + (hb.getCountryName() != null ? hb.getCountryName() : ""),
                    Collectors.counting()
            ));

    long uniqueDestinations = destinations.size();
    Map<String, Long> topDestinations = destinations.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(5)
            .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (e1, e2) -> e1,
                    LinkedHashMap::new
            ));

    miniCharts.add(DashboardDtos.DashboardSummaryDto.MiniChartData.builder()
            .chartType("TOP_DESTINATIONS")
            .title("Top 5 Destinos")
            .previewData(topDestinations)
            .build());

    // Mini chart 4: Top 5 Carriers (FLIGHTS)
    List<Long> flightBookingIds = bookings.stream()
            .filter(b -> "FLIGHT".equals(b.getType().name()))
            .map(BookingEntity::getId)
            .toList();

    Map<String, Long> carrierCount = flightBookingRepository.findAll().stream()
            .filter(fb -> flightBookingIds.contains(fb.getBookingEntity().getId()))
            .filter(fb -> fb.getCarrier() != null)
            .collect(Collectors.groupingBy(
                    FlightBookingEntity::getCarrier,
                    Collectors.counting()
            ));
    long totalUniqueCarriers = carrierCount.size();

    Map<String, Long> topCarriers = carrierCount.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(5)
            .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (e1, e2) -> e1,
                    LinkedHashMap::new
            ));

    miniCharts.add(DashboardDtos.DashboardSummaryDto.MiniChartData.builder()
            .chartType("TOP_CARRIERS")
            .title("Top 5 Aerolíneas")
            .previewData(topCarriers)
            .build());

    // Mini chart 5: Payments by Status
    List<PaymentEntity> payments = paymentRepository.findAll().stream()
            .filter(p -> filterPaymentByDateRange(p, startDate, endDate))
            .toList();

    Map<String, Long> paymentsByStatus = payments.stream()
            .collect(Collectors.groupingBy(
                    p -> p.getStatus().name(),
                    Collectors.counting()
            ));

    miniCharts.add(DashboardDtos.DashboardSummaryDto.MiniChartData.builder()
            .chartType("PAYMENTS_BY_STATUS")
            .title("Pagos por Estado")
            .previewData(paymentsByStatus)
            .build());

    DashboardDtos.DashboardSummaryDto.GlobalKpis globalKpis =
            DashboardDtos.DashboardSummaryDto.GlobalKpis.builder()
                    .totalBookings(totalBookings)
                    .totalRevenue(totalRevenue)
                    .totalCommissions(totalCommissions)
                    .averageBookingValue(averageBookingValue)
                    .uniqueDestinations(uniqueDestinations)
                    .uniqueCarriers(totalUniqueCarriers)
                    .build();
    return DashboardDtos.DashboardSummaryDto.builder()
            .globalKpis(globalKpis)
            .miniCharts(miniCharts)
            .build();
  }
  //endregion

  //region Métodos para el gráfico de BOOKINGS BY TYPE
  @Override
  public DashboardDtos.BookingsByTypeDto getBookingsByType(LocalDateTime startDate,
                                                           LocalDateTime endDate,
                                                           String bookingType,
                                                           String bookingStatus,
                                                           Integer agentId,
                                                           Integer clientId) {
    List<BookingEntity> bookings = filterBookings(startDate, endDate, bookingType, bookingStatus);

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
  //endregion

  //region Métodos para el gráfico de REVENUE OVER TIME
  @Override
  public DashboardDtos.RevenueOverTimeDto getRevenueOverTime(LocalDateTime startDate,
                                                             LocalDateTime endDate,
                                                             String granularity,
                                                             String bookingType,
                                                             Integer agentId) {
    List<BookingEntity> bookings = filterBookings(startDate, endDate, bookingType, null);

    if (agentId != null) {
      bookings = bookings.stream()
              .filter(b -> b.getAgentId() != null && b.getAgentId().equals(agentId))
              .collect(Collectors.toList());
    }

    List<DashboardDtos.RevenueOverTimeDto.TimeSeriesPoint> data
            = switch (granularity.toUpperCase()) {
      case "DAILY" -> calculateDailyRevenue(bookings, startDate, endDate);
      case "YEARLY" -> calculateYearlyRevenue(bookings);
      default -> calculateMonthlyRevenue(bookings, startDate, endDate);
    };

    // KPIs
    BigDecimal totalRevenue = data.stream()
            .map(DashboardDtos.RevenueOverTimeDto.TimeSeriesPoint::getRevenue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalCommission = data.stream()
            .map(DashboardDtos.RevenueOverTimeDto.TimeSeriesPoint::getCommission)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal averageRevenuePerPeriod = !data.isEmpty()
            ? totalRevenue.divide(BigDecimal.valueOf(data.size()), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

    DashboardDtos.RevenueOverTimeDto.TimeSeriesPoint highest = data.stream()
            .max(Comparator.comparing(DashboardDtos.RevenueOverTimeDto.TimeSeriesPoint::getRevenue))
            .orElse(null);

    BigDecimal highestRevenue = highest != null ? highest.getRevenue() : BigDecimal.ZERO;
    String highestRevenuePeriod = highest != null ? highest.getPeriod() : "";

    DashboardDtos.RevenueOverTimeDto.KpisDto kpis = DashboardDtos.RevenueOverTimeDto.KpisDto
            .builder()
              .totalRevenue(totalRevenue)
              .totalCommission(totalCommission)
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

    Map<LocalDate, DashboardDtos.RevenueOverTimeDto.TimeSeriesPoint> dataMap =
            new LinkedHashMap<>();

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
        dataMap.computeIfPresent(bookingDate, (k, existing)
                -> DashboardDtos.RevenueOverTimeDto.TimeSeriesPoint.builder()
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
        dataMap.computeIfPresent(monthKey, (k, existing)
                -> DashboardDtos.RevenueOverTimeDto.TimeSeriesPoint.builder()
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
  //endregion

  //region para el gráfico TOP DESTINATIONS
  @Override
  public DashboardDtos.TopDestinationsDto getTopDestinations(LocalDateTime startDate,
                                                             LocalDateTime endDate,
                                                             Integer limit,
                                                             String bookingStatus,
                                                             String type) {
    log.info("Obteniendo top {} destinos de tipo {}", limit, type);

    List<BookingEntity> bookings = filterBookings(startDate, endDate, null, bookingStatus);

    List<Long> bookingIds = bookings.stream()
            .map(BookingEntity::getId)
            .toList();

    List<DashboardDtos.TopDestinationsDto.DestinationData> destinationDataList = new ArrayList<>();
    long totalBookings = 0;
    int uniqueDestinations = 0;
    String topDestination = "";
    BigDecimal totalRevenue = BigDecimal.ZERO;

    if ("HOTEL".equals(type)) {
      List<HotelBookingEntity> hotelBookings = hotelBookingRepository.findAll().stream()
              .filter(hb -> bookingIds.contains(hb.getBookingEntity().getId()))
              .filter(hb -> hb.getDestinationName() != null)
              .toList();

      // Agrupar por destino (destination_name + country)
      Map<String, List<HotelBookingEntity>> groupedByDestination = hotelBookings.stream()
              .collect(Collectors.groupingBy(hb ->
                      hb.getDestinationName() + ", "
                              + (hb.getCountryName() != null ? hb.getCountryName() : "")
              ));

      destinationDataList = groupedByDestination.entrySet().stream()
              .map(entry -> {
                String destination = entry.getKey();
                List<HotelBookingEntity> destBookings = entry.getValue();

                long count = destBookings.size();
                BigDecimal revenue = destBookings.stream()
                        .map(HotelBookingEntity::getTotalPrice)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Calcular promedio de noches
                double averageNights = destBookings.stream()
                        .mapToInt(HotelBookingEntity::getNumberOfNights)
                        .average()
                        .orElse(0.0);

                // Calcular precio promedio
                BigDecimal averagePrice = count > 0
                        ? revenue.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;

                return DashboardDtos.TopDestinationsDto.DestinationData.builder()
                        .destination(destination)
                        .bookingsCount(count)
                        .revenue(revenue)
                        .averageNights((int) averageNights)
                        .averagePrice(averagePrice)
                        .build();
              })
              .sorted((a, b) -> Long.compare(b.getBookingsCount(), a.getBookingsCount()))
              .limit(limit != null ? limit : 10)
              .collect(Collectors.toList());

      totalBookings = hotelBookings.size();
      uniqueDestinations = groupedByDestination.size();
      topDestination = destinationDataList.isEmpty() ? "" : destinationDataList
              .getFirst().getDestination();
      totalRevenue = hotelBookings.stream()
              .map(HotelBookingEntity::getTotalPrice)
              .reduce(BigDecimal.ZERO, BigDecimal::add);

    } else if ("FLIGHT".equals(type)) {
      // Obtener todos los flight bookings que corresponden a los bookings filtrados
      List<FlightBookingEntity> flightBookings = flightBookingRepository.findAll().stream()
              .filter(fb -> bookingIds.contains(fb.getBookingEntity().getId()))
              .filter(fb -> fb.getDestination() != null)
              .toList();

      // Agrupar por destino
      Map<String, List<FlightBookingEntity>> groupedByDestination = flightBookings.stream()
              .collect(Collectors.groupingBy(FlightBookingEntity::getDestination));

      destinationDataList = groupedByDestination.entrySet().stream()
              .map(entry -> {
                String destination = entry.getKey();
                List<FlightBookingEntity> destBookings = entry.getValue();

                long count = destBookings.size();
                BigDecimal revenue = destBookings.stream()
                        .map(FlightBookingEntity::getTotalPrice)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Calcular precio promedio
                BigDecimal averagePrice = count > 0
                        ? revenue.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;

                return DashboardDtos.TopDestinationsDto.DestinationData.builder()
                        .destination(destination)
                        .bookingsCount(count)
                        .revenue(revenue)
                        .averageNights(0)
                        .averagePrice(averagePrice)
                        .build();
              })
              .sorted((a, b) -> Long.compare(b.getBookingsCount(), a.getBookingsCount()))
              .limit(limit != null ? limit : 10)
              .collect(Collectors.toList());

      totalBookings = flightBookings.size();
      uniqueDestinations = groupedByDestination.size();
      topDestination = destinationDataList.isEmpty() ? "" : destinationDataList
              .getFirst().getDestination();
      totalRevenue = flightBookings.stream()
              .map(FlightBookingEntity::getTotalPrice)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    DashboardDtos.TopDestinationsDto.KpisDto kpis =
            DashboardDtos.TopDestinationsDto.KpisDto.builder()
                    .totalBookings(totalBookings)
                    .uniqueDestinations(uniqueDestinations)
                    .topDestination(topDestination)
                    .totalRevenue(totalRevenue)
                    .build();

    return DashboardDtos.TopDestinationsDto.builder()
            .data(destinationDataList)
            .kpis(kpis)
            .build();
  }
  //endregion

  //region para el gráfico TOP CARRIERS
  @Override
  public DashboardDtos.TopCarriersDto getTopCarriers(LocalDateTime startDate,
                                                     LocalDateTime endDate,
                                                     Integer limit,
                                                     String bookingStatus) {
    log.info("Obteniendo top {} aerolíneas", limit);

    List<BookingEntity> bookings = filterBookings(startDate, endDate, null, bookingStatus);
    List<Long> bookingIds = bookings.stream()
            .map(BookingEntity::getId)
            .toList();

    List<FlightBookingEntity> flightBookings = flightBookingRepository.findAll().stream()
            .filter(fb -> bookingIds.contains(fb.getBookingEntity().getId()))
            .filter(fb -> fb.getCarrier() != null)
            .toList();

    // Agrupar por aerolínea
    Map<String, List<FlightBookingEntity>> groupedByCarrier = flightBookings.stream()
            .collect(Collectors.groupingBy(FlightBookingEntity::getCarrier));

    List<DashboardDtos.TopCarriersDto.CarrierData> data = new ArrayList<>();
    groupedByCarrier.forEach((carrierName, flightList) -> {
      long bookingsCount = flightList.size();
      BigDecimal totalRevenue = flightList.stream()
              .map(FlightBookingEntity::getTotalPrice)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
      double averagePassengers = flightList.stream()
              .mapToInt(fb -> fb.getAdults() + fb.getChildren() + fb.getInfants())
              .average()
              .orElse(0.0);

      BigDecimal averagePrice = bookingsCount > 0
              ? totalRevenue.divide(BigDecimal.valueOf(bookingsCount), 2, RoundingMode.HALF_UP)
              : BigDecimal.ZERO;

      data.add(DashboardDtos.TopCarriersDto.CarrierData.builder()
              .carrierName(carrierName)
              .bookingsCount(bookingsCount)
              .totalRevenue(totalRevenue)
              .averagePassengers((int) averagePassengers)
              .averagePrice(averagePrice)
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
    String topCarrier = !limitedData.isEmpty() ? limitedData.getFirst().getCarrierName() : "";
    BigDecimal totalFlightRevenue = data.stream()
            .map(DashboardDtos.TopCarriersDto.CarrierData::getTotalRevenue)
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
  //endregion

  //region métodos para el gráfico PAYMENTS BY STATUS
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
    long totalPayments = payments.size();
    long approvedPayments = groupedByStatus.getOrDefault("APPROVED", List.of()).size();
    BigDecimal approvedAmount = groupedByStatus.getOrDefault("APPROVED", List.of()).stream()
            .map(PaymentEntity::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    long refundedPayments = groupedByStatus.getOrDefault("REFUNDED", List.of()).size();
    Double approvalRate = totalPayments > 0
            ? ((double) approvedPayments / totalPayments) * 100
            : 0.0;

    DashboardDtos.PaymentsByStatusDto.KpisDto kpis = DashboardDtos.PaymentsByStatusDto.KpisDto
            .builder()
              .totalPayments(totalPayments)
              .totalAmount(totalAmount)
              .approvedPayments(approvedPayments)
              .approvedAmount(approvedAmount)
              .refundedPayments(refundedPayments)
              .approvalRate(approvalRate)
              .build();

    return DashboardDtos.PaymentsByStatusDto.builder()
            .data(data)
            .kpis(kpis)
            .build();
  }
  //endregion
}