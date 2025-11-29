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
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

    LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
    LocalDateTime end = endDate != null ? endDate.atTime(23, 59, 59) : null;

    DashboardDtos.DashboardSummaryDto summary = dashboardService.getDashboardSummary(start, end);
    return ResponseEntity.ok(summary);
  }

  /**
   * Endpoint para obtener reservas por tipo (Flight, Hotel, Package).
   *
   * @param startDate fecha de inicio
   * @param endDate fecha de fin
   * @param bookingType filtro por tipo específico (opcional)
   * @param bookingStatus filtro por estado (opcional)
   * @return datos del gráfico + KPI
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
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
          @RequestParam(required = false) String granularity,
          @RequestParam(required = false) String bookingType,
          @RequestParam(required = false) Integer agentId) {

    LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
    LocalDateTime end = endDate != null ? endDate.atTime(23, 59, 59) : null;

    DashboardDtos.RevenueOverTimeDto result = dashboardService.getRevenueOverTime(
            start, end, granularity, bookingType, agentId);
    return ResponseEntity.ok(result);
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
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
          @RequestParam(defaultValue = "10") Integer limit,
          @RequestParam(required = false) String bookingStatus,
          @RequestParam(required = false, defaultValue = "HOTEL") String type) {

    LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
    LocalDateTime end = endDate != null ? endDate.atTime(23, 59, 59) : null;
    DashboardDtos.TopDestinationsDto result = dashboardService.getTopDestinations(
            start, end, limit, bookingStatus, type);
    return ResponseEntity.ok(result);
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
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
          @RequestParam(defaultValue = "10") Integer limit,
          @RequestParam(required = false) String bookingStatus) {

    LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
    LocalDateTime end = endDate != null ? endDate.atTime(23, 59, 59) : null;
    DashboardDtos.TopCarriersDto data = dashboardService.getTopCarriers(
            start, end, limit, bookingStatus);
    return ResponseEntity.ok(data);
  }

  /**
   * Endpoint para obtener pagos por estado.
   *
   * @param startDate fecha de inicio
   * @param endDate fecha de fin
   * @param paymentMethod filtro por metodo de pago (opcional)
   * @return distribución de pagos + KPIs
   */
  @GetMapping("/payments-by-status")
  public ResponseEntity<DashboardDtos.PaymentsByStatusDto> getPaymentsByStatus(
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
          @RequestParam(required = false) String paymentMethod) {

    LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
    LocalDateTime end = endDate != null ? endDate.atTime(23, 59, 59) : null;

    DashboardDtos.PaymentsByStatusDto data = dashboardService.getPaymentsByStatus(
            start, end, paymentMethod);
    return ResponseEntity.ok(data);
  }
}