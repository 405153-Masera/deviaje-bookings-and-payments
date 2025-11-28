package masera.deviajebookingsandpayments.controllers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import masera.deviajebookingsandpayments.dtos.dashboards.DashboardDtos;
import masera.deviajebookingsandpayments.services.interfaces.DashboardService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para el Dashboard con endpoints separados por gráfico.
 */
@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

  private final DashboardService dashboardService;

  /**
   * Endpoint para obtener el resumen general del dashboard (vista principal).
   *
   * @param startDate fecha de inicio
   * @param endDate fecha de fin
   * @return resumen con KPI globales y datos mini de todos los gráficos
   */
  @GetMapping("/summary")
  public ResponseEntity<DashboardDtos.DashboardSummaryDto> getDashboardSummary(
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime startDate,
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime endDate) {

    log.info("GET /api/dashboard/summary - startDate: {}, endDate: {}", startDate, endDate);

    DashboardDtos.DashboardSummaryDto summary = dashboardService.getDashboardSummary(startDate, endDate);
    return ResponseEntity.ok(summary);
  }

  /**
   * Endpoint para obtener reservas por tipo (Flight, Hotel, Package).
   *
   * @param startDate fecha de inicio
   * @param endDate fecha de fin
   * @param bookingType filtro por tipo específico (opcional)
   * @param bookingStatus filtro por estado (opcional)
   * @return datos del gráfico + KPIs
   */
  @GetMapping("/bookings-by-type")
  public ResponseEntity<DashboardDtos.BookingsByTypeDto> getBookingsByType(
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
          @RequestParam(required = false) String bookingType,
          @RequestParam(required = false) String bookingStatus,
          @RequestParam(required = false) Integer agentId,
          @RequestParam(required = false) Integer clientId) {

    LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
    LocalDateTime end = endDate != null ? endDate.atTime(23, 59, 59) : null;

    DashboardDtos.BookingsByTypeDto result = dashboardService.getBookingsByType(
            start, end, bookingType, bookingStatus, agentId, clientId);
    return ResponseEntity.ok(result);
  }

  /**
   * Endpoint para obtener revenue en el tiempo.
   *
   * @param startDate fecha de inicio
   * @param endDate fecha de fin
   * @param granularity granularidad temporal: DAILY, MONTHLY, YEARLY (default: MONTHLY)
   * @param bookingType filtro por tipo (opcional)
   * @return series temporal + KPIs
   */
  @GetMapping("/revenue-over-time")
  public ResponseEntity<DashboardDtos.RevenueOverTimeDto> getRevenueOverTime(
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime startDate,
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime endDate,
          @RequestParam(defaultValue = "MONTHLY") String granularity,
          @RequestParam(required = false) String bookingType) {

    log.info("GET /api/dashboard/revenue-over-time - startDate: {}, endDate: {}, granularity: {}, type: {}",
            startDate, endDate, granularity, bookingType);

    DashboardDtos.RevenueOverTimeDto data = dashboardService.getRevenueOverTime(
            startDate, endDate, granularity, bookingType);
    return ResponseEntity.ok(data);
  }

  /**
   * Endpoint para obtener top destinos (hoteles).
   *
   * @param startDate fecha de inicio
   * @param endDate fecha de fin
   * @param limit cantidad de destinos a mostrar (default: 10)
   * @param bookingStatus filtro por estado (opcional)
   * @return top destinos + KPIs
   */
  @GetMapping("/top-destinations")
  public ResponseEntity<DashboardDtos.TopDestinationsDto> getTopDestinations(
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime startDate,
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime endDate,
          @RequestParam(defaultValue = "10") Integer limit,
          @RequestParam(required = false) String bookingStatus) {

    log.info("GET /api/dashboard/top-destinations - startDate: {}, endDate: {}, limit: {}, status: {}",
            startDate, endDate, limit, bookingStatus);

    DashboardDtos.TopDestinationsDto data = dashboardService.getTopDestinations(
            startDate, endDate, limit, bookingStatus);
    return ResponseEntity.ok(data);
  }

  /**
   * Endpoint para obtener top aerolíneas (vuelos).
   *
   * @param startDate fecha de inicio
   * @param endDate fecha de fin
   * @param limit cantidad de aerolíneas a mostrar (default: 10)
   * @param bookingStatus filtro por estado (opcional)
   * @return top aerolíneas + KPIs
   */
  @GetMapping("/top-carriers")
  public ResponseEntity<DashboardDtos.TopCarriersDto> getTopCarriers(
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime startDate,
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime endDate,
          @RequestParam(defaultValue = "10") Integer limit,
          @RequestParam(required = false) String bookingStatus) {

    log.info("GET /api/dashboard/top-carriers - startDate: {}, endDate: {}, limit: {}, status: {}",
            startDate, endDate, limit, bookingStatus);

    DashboardDtos.TopCarriersDto data = dashboardService.getTopCarriers(
            startDate, endDate, limit, bookingStatus);
    return ResponseEntity.ok(data);
  }

  /**
   * Endpoint para obtener pagos por estado.
   *
   * @param startDate fecha de inicio
   * @param endDate fecha de fin
   * @param paymentMethod filtro por método de pago (opcional)
   * @return distribución de pagos + KPIs
   */
  @GetMapping("/payments-by-status")
  public ResponseEntity<DashboardDtos.PaymentsByStatusDto> getPaymentsByStatus(
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime startDate,
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime endDate,
          @RequestParam(required = false) String paymentMethod) {

    log.info("GET /api/dashboard/payments-by-status - startDate: {}, endDate: {}, method: {}",
            startDate, endDate, paymentMethod);

    DashboardDtos.PaymentsByStatusDto data = dashboardService.getPaymentsByStatus(
            startDate, endDate, paymentMethod);
    return ResponseEntity.ok(data);
  }

  /**
   * Endpoint para obtener reservas por estado.
   *
   * @param startDate fecha de inicio
   * @param endDate fecha de fin
   * @param bookingType filtro por tipo (opcional)
   * @return distribución de reservas + KPIs
   */
  @GetMapping("/bookings-by-status")
  public ResponseEntity<DashboardDtos.BookingsByStatusDto> getBookingsByStatus(
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime startDate,
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime endDate,
          @RequestParam(required = false) String bookingType) {

    log.info("GET /api/dashboard/bookings-by-status - startDate: {}, endDate: {}, type: {}",
            startDate, endDate, bookingType);

    DashboardDtos.BookingsByStatusDto data = dashboardService.getBookingsByStatus(
            startDate, endDate, bookingType);
    return ResponseEntity.ok(data);
  }
}