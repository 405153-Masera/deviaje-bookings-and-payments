package masera.deviajebookingsandpayments.services.interfaces;

import java.time.LocalDateTime;
import masera.deviajebookingsandpayments.dtos.dashboards.DashboardDtos;
import org.springframework.stereotype.Service;

/**
 * Interfaz para el servicio de Dashboard con métodos separados por gráfico.
 */
@Service
public interface DashboardService {

  /**
   * Obtiene el resumen general del dashboard (para vista principal).
   *
   * @param startDate fecha de inicio
   * @param endDate fecha de fin
   * @return resumen con KPIs globales y datos mini
   */
  DashboardDtos.DashboardSummaryDto getDashboardSummary(
          LocalDateTime startDate, LocalDateTime endDate,
          String bookingStatus,
          String bookingType
  );

  /**
   * Obtiene reservas agrupadas por tipo (Flight, Hotel, Package).
   *
   * @param startDate fecha de inicio
   * @param endDate fecha de fin
   * @param bookingType filtro por tipo específico (opcional)
   * @param bookingStatus filtro por estado (opcional)
   * @return datos del gráfico + KPI
   */
  DashboardDtos.BookingsByTypeDto getBookingsByType(LocalDateTime startDate,
                                                    LocalDateTime endDate,
                                                    String bookingType,
                                                    String bookingStatus,
                                                    Integer agentId,
                                                    Integer clientId
  );

  /**
   * Obtiene revenue en el tiempo con granularidad configurable.
   *
   * @param startDate fecha de inicio
   * @param endDate fecha de fin
   * @param granularity DAILY, MONTHLY, YEARLY
   * @param bookingType filtro por tipo (opcional)
   * @return series temporal + KPIs
   */
  DashboardDtos.RevenueOverTimeDto getRevenueOverTime(LocalDateTime startDate,
                                                      LocalDateTime endDate,
                                                      String granularity,
                                                      String bookingType,
                                                      Integer agentId
  );

  /**
   * Obtiene top destinos más reservados (hoteles).
   *
   * @param startDate fecha de inicio
   * @param endDate fecha de fin
   * @param limit cantidad de destinos a mostrar
   * @param bookingStatus filtro por estado (opcional)
   * @param type tipo de reserva
   * @return top destinos + KPIs
   */
  DashboardDtos.TopDestinationsDto getTopDestinations(LocalDateTime startDate,
                                                      LocalDateTime endDate,
                                                      Integer limit,
                                                      String bookingStatus,
                                                      String type);

  /**
   * Obtiene top aerolíneas más reservadas (vuelos).
   *
   * @param startDate fecha de inicio
   * @param endDate fecha de fin
   * @param limit cantidad de aerolíneas a mostrar
   * @param bookingStatus filtro por estado (opcional)
   * @return top aerolíneas + KPIs
   */
  DashboardDtos.TopCarriersDto getTopCarriers(LocalDateTime startDate,
                                              LocalDateTime endDate,
                                              Integer limit,
                                              String bookingStatus);

  /**
   * Obtiene pagos agrupados por estado.
   *
   * @param startDate fecha de inicio
   * @param endDate fecha de fin
   * @param paymentMethod filtro por metodo de pago (opcional)
   * @return distribución de pagos + KPIs
   */
  DashboardDtos.PaymentsByStatusDto getPaymentsByStatus(LocalDateTime startDate,
                                                        LocalDateTime endDate,
                                                        String paymentMethod);
}