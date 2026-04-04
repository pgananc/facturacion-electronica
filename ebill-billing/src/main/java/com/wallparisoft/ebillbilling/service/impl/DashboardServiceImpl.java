package com.wallparisoft.ebillbilling.service.impl;

import com.wallparisoft.ebillbilling.dto.ClientDto;
import com.wallparisoft.ebillbilling.dto.ClientDtoResponse;
import com.wallparisoft.ebillbilling.dto.dashboard.AlertDto;
import com.wallparisoft.ebillbilling.dto.dashboard.AlertPriority;
import com.wallparisoft.ebillbilling.dto.dashboard.AlertType;
import com.wallparisoft.ebillbilling.dto.dashboard.KpiDto;
import com.wallparisoft.ebillbilling.dto.dashboard.KpiSummaryDto;
import com.wallparisoft.ebillbilling.dto.dashboard.PendingCollectionDto;
import com.wallparisoft.ebillbilling.dto.dashboard.RecentInvoiceDto;
import com.wallparisoft.ebillbilling.dto.dashboard.SalesTrendItemDto;
import com.wallparisoft.ebillbilling.dto.dashboard.TrendSummaryDto;
import com.wallparisoft.ebillbilling.dto.dashboard.WelcomeDashboardDto;
import com.wallparisoft.ebillbilling.repository.IInvoiceRepo;
import com.wallparisoft.ebillbilling.service.CustomerClient;
import com.wallparisoft.ebillbilling.service.IDashboardService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardServiceImpl implements IDashboardService {

  private final IInvoiceRepo invoiceRepo;
  private final CustomerClient customerClient;

  @Override
  public WelcomeDashboardDto getWelcomeDashboard(Long companyId) {
    log.info("🎯 Generando dashboard de bienvenida para companyId: {}", companyId);

    long startTime = System.currentTimeMillis();

    // Ejecutar todas las consultas
    KpiSummaryDto kpis = calculateKpis(companyId);
    List<SalesTrendItemDto> salesTrend = calculateSalesTrend(companyId);
    TrendSummaryDto trendSummary = calculateTrendSummary(salesTrend);
    List<RecentInvoiceDto> recentInvoices = getRecentInvoices(companyId);
    List<AlertDto> alerts = generateAlerts(companyId);

    long elapsedTime = System.currentTimeMillis() - startTime;
    log.info("✅ Dashboard generado en {}ms", elapsedTime);

    return WelcomeDashboardDto.builder()
        .kpis(kpis)
        .salesTrend(salesTrend)
        .trendSummary(trendSummary)
        .recentInvoices(recentInvoices)
        .alerts(alerts)
        .build();
  }

  private KpiSummaryDto calculateKpis(Long companyId) {
    log.debug("📊 Calculando KPIs para companyId: {}", companyId);

    // KPI: Ventas de hoy
    KpiDto todaySales = calculateTodaySales(companyId);

    // KPI: Ventas de la semana
    KpiDto weekSales = calculateWeekSales(companyId);

    // KPI: Ventas del mes
    KpiDto monthSales = calculateMonthSales(companyId);

    // KPI: Pendientes de cobro
    PendingCollectionDto pendingCollection = calculatePendingCollection(companyId);

    return KpiSummaryDto.builder()
        .todaySales(todaySales)
        .weekSales(weekSales)
        .monthSales(monthSales)
        .pendingCollection(pendingCollection)
        .build();
  }

  private KpiDto calculateTodaySales(Long companyId) {
    BigDecimal todayAmount = invoiceRepo.getTodaySalesAmount(companyId);
    Long todayCount = invoiceRepo.getTodaySalesCount(companyId);
    if (todayAmount == null) {
      todayAmount = BigDecimal.ZERO;
    }
    if (todayCount == null) {
      todayCount = 0L;
    }

    // Calcular % cambio vs ayer
    BigDecimal yesterdayAmount = invoiceRepo.getYesterdaySales(companyId);
    if (yesterdayAmount == null) {
      yesterdayAmount = BigDecimal.ZERO;
    }

    Double percentageChange = calculatePercentageChange(todayAmount, yesterdayAmount);

    return KpiDto.builder()
        .amount(todayAmount)
        .invoiceCount(todayCount.intValue())
        .percentageChange(percentageChange)
        .build();
  }

  private KpiDto calculateWeekSales(Long companyId) {
    BigDecimal weekAmount = invoiceRepo.getWeekSalesAmount(companyId);
    Long weekCount = invoiceRepo.getWeekSalesCount(companyId);
    if (weekAmount == null) {
      weekAmount = BigDecimal.ZERO;
    }
    if (weekCount == null) {
      weekCount = 0L;
    }

    return KpiDto.builder().amount(weekAmount).invoiceCount(weekCount.intValue()).build();
  }

  private KpiDto calculateMonthSales(Long companyId) {
    BigDecimal monthAmount = invoiceRepo.getMonthSalesAmount(companyId);
    Long monthCount = invoiceRepo.getMonthSalesCount(companyId);
    if (monthAmount == null) {
      monthAmount = BigDecimal.ZERO;
    }
    if (monthCount == null) {
      monthCount = 0L;
    }

    // Calcular % cambio vs mes anterior
    BigDecimal previousMonthAmount = invoiceRepo.getPreviousMonthSales(companyId);
    if (previousMonthAmount == null) {
      previousMonthAmount = BigDecimal.ZERO;
    }

    Double percentageChange = calculatePercentageChange(monthAmount, previousMonthAmount);

    return KpiDto.builder()
        .amount(monthAmount)
        .invoiceCount(monthCount.intValue())
        .percentageChange(percentageChange)
        .build();
  }

  private PendingCollectionDto calculatePendingCollection(Long companyId) {
    BigDecimal pendingAmount = invoiceRepo.getPendingCollectionAmount(companyId);
    Long invoiceCount = invoiceRepo.getPendingCollectionCount(companyId);
    Long overdueCount = invoiceRepo.getOverdueCollectionCount(companyId);

    if (pendingAmount == null) {
      pendingAmount = BigDecimal.ZERO;
    }
    if (invoiceCount == null) {
      invoiceCount = 0L;
    }
    if (overdueCount == null) {
      overdueCount = 0L;
    }

    return PendingCollectionDto.builder()
        .amount(pendingAmount)
        .invoiceCount(invoiceCount.intValue())
        .overdueCount(overdueCount.intValue())
        .build();
  }

  private List<SalesTrendItemDto> calculateSalesTrend(Long companyId) {
    log.debug("📈 Calculando tendencia de ventas (últimos 7 días)");

    List<Object[]> trendData = invoiceRepo.getSalesTrendLast7Days(companyId);

    // Asegurar que tengamos datos para todos los 7 días (llenar con ceros si faltan)
    Map<LocalDate, SalesTrendItemDto> trendMap = new HashMap<>();

    for (Object[] row : trendData) {
      LocalDate date = convertToLocalDate(row[0]);
      BigDecimal amount = convertToBigDecimal(row[1]);
      Integer invoiceCount = convertToInteger(row[2]);

      String dayOfWeek = date.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("es", "ES"));

      trendMap.put(
          date,
          SalesTrendItemDto.builder()
              .date(date)
              .dayOfWeek(dayOfWeek)
              .amount(amount)
              .invoiceCount(invoiceCount)
              .build());
    }

    // Rellenar días faltantes con ceros
    List<SalesTrendItemDto> result = new ArrayList<>();
    LocalDate today = LocalDate.now();

    for (int i = 6; i >= 0; i--) {
      LocalDate date = today.minusDays(i);
      if (trendMap.containsKey(date)) {
        result.add(trendMap.get(date));
      } else {
        String dayOfWeek =
            date.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("es", "ES"));
        result.add(
            SalesTrendItemDto.builder()
                .date(date)
                .dayOfWeek(dayOfWeek)
                .amount(BigDecimal.ZERO)
                .invoiceCount(0)
                .build());
      }
    }

    return result;
  }

  private TrendSummaryDto calculateTrendSummary(List<SalesTrendItemDto> salesTrend) {
    if (salesTrend.isEmpty()) {
      return TrendSummaryDto.builder().total(BigDecimal.ZERO).average(BigDecimal.ZERO).build();
    }

    BigDecimal total =
        salesTrend.stream()
            .map(SalesTrendItemDto::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal average = total.divide(new BigDecimal(7), 2, RoundingMode.HALF_UP);

    return TrendSummaryDto.builder().total(total).average(average).build();
  }

  private List<RecentInvoiceDto> getRecentInvoices(Long companyId) {
    log.debug("📋 Obteniendo últimas 5 facturas");

    List<Object[]> invoiceData = invoiceRepo.getRecentInvoices(companyId);

    // Pre-cargar datos de clientes únicos
    Set<Long> clientIds =
        invoiceData.stream()
            .map(row -> convertToLong(row[5]))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    Map<Long, ClientDto> clientCache = new HashMap<>();
    for (Long clientId : clientIds) {
      try {
        ClientDtoResponse response = customerClient.getClientById(clientId);
        if (response != null
            && response.getClientDtos() != null
            && !response.getClientDtos().isEmpty()) {
          clientCache.put(clientId, response.getClientDtos().get(0));
        }
      } catch (Exception e) {
        log.warn("Error cargando cliente {}: {}", clientId, e.getMessage());
      }
    }

    // Mapear a DTOs
    return invoiceData.stream()
        .map(
            row -> {
              Long clientId = convertToLong(row[5]);
              ClientDto client = clientCache.get(clientId);

              return RecentInvoiceDto.builder()
                  .idInvoice(convertToLong(row[0]))
                  .invoiceNumber((String) row[1])
                  .totalAmount(convertToBigDecimal(row[2]))
                  .issueDate(convertToLocalDateTime(row[3]))
                  .authorizationStatus((String) row[4])
                  .paymentStatus("N/A") // No disponible en la BD
                  .clientName(client != null ? client.getName() : "Cliente no disponible")
                  .clientIdentification(client != null ? client.getIdentification() : "N/A")
                  .build();
            })
        .collect(Collectors.toList());
  }

  private List<AlertDto> generateAlerts(Long companyId) {
    log.debug("🚨 Generando alertas");

    List<AlertDto> alerts = new ArrayList<>();

    // Alerta 1: Facturas rechazadas
    Long rejectedCount = invoiceRepo.getRejectedInvoicesCount(companyId);
    if (rejectedCount != null && rejectedCount > 0) {
      alerts.add(
          AlertDto.builder()
              .id("rejected-invoices-" + System.currentTimeMillis())
              .type(AlertType.ERROR)
              .priority(AlertPriority.HIGH)
              .title("Facturas rechazadas por el SRI")
              .message(rejectedCount + " facturas requieren atención inmediata")
              .count(rejectedCount.intValue())
              .actionUrl("/viewing-invoices?status=RECHAZADA")
              .createdAt(LocalDateTime.now())
              .build());
    }

    // Alerta 2: Récord de ventas
    BigDecimal thisWeekSales = invoiceRepo.getCurrentWeekSales(companyId);
    BigDecimal previousBestWeek = invoiceRepo.getPreviousBestWeekSales(companyId);

    if (thisWeekSales != null
        && previousBestWeek != null
        && thisWeekSales.compareTo(previousBestWeek) > 0
        && previousBestWeek.compareTo(BigDecimal.ZERO) > 0) {

      Map<String, Object> metadata = new HashMap<>();
      metadata.put("amount", thisWeekSales);
      metadata.put("previousRecord", previousBestWeek);

      alerts.add(
          AlertDto.builder()
              .id("record-sales-week-" + System.currentTimeMillis())
              .type(AlertType.SUCCESS)
              .priority(AlertPriority.LOW)
              .title("¡Récord de ventas esta semana!")
              .message(
                  String.format("Has alcanzado $%,.2f - Tu mejor semana del mes", thisWeekSales))
              .actionUrl(null)
              .metadata(metadata)
              .createdAt(LocalDateTime.now())
              .build());
    }

    // Nota: La alerta de punto de emisión bajo requiere acceso a ebill-customer
    // Se puede agregar si el cliente provee un endpoint para ello

    return alerts;
  }

  // ================================================================================
  // UTILIDADES DE CONVERSIÓN
  // ================================================================================

  private BigDecimal convertToBigDecimal(Object obj) {
    if (obj == null) return BigDecimal.ZERO;
    if (obj instanceof BigDecimal) return (BigDecimal) obj;
    if (obj instanceof Number) return new BigDecimal(obj.toString());
    return BigDecimal.ZERO;
  }

  private Integer convertToInteger(Object obj) {
    if (obj == null) return 0;
    if (obj instanceof Integer) return (Integer) obj;
    if (obj instanceof Long) return ((Long) obj).intValue();
    if (obj instanceof Number) return ((Number) obj).intValue();
    return 0;
  }

  private Long convertToLong(Object obj) {
    if (obj == null) return null;
    if (obj instanceof Long) return (Long) obj;
    if (obj instanceof Integer) return ((Integer) obj).longValue();
    if (obj instanceof Number) return ((Number) obj).longValue();
    return null;
  }

  private LocalDate convertToLocalDate(Object obj) {
    if (obj == null) return null;
    if (obj instanceof LocalDate) return (LocalDate) obj;
    if (obj instanceof Date) return ((Date) obj).toLocalDate();
    if (obj instanceof java.util.Date) {
      return new Date(((java.util.Date) obj).getTime()).toLocalDate();
    }
    return null;
  }

  private LocalDateTime convertToLocalDateTime(Object obj) {
    if (obj == null) return null;
    if (obj instanceof LocalDateTime) return (LocalDateTime) obj;
    if (obj instanceof Timestamp) return ((Timestamp) obj).toLocalDateTime();
    if (obj instanceof java.util.Date) {
      return new Timestamp(((java.util.Date) obj).getTime()).toLocalDateTime();
    }
    return null;
  }

  private Double calculatePercentageChange(BigDecimal current, BigDecimal previous) {
    if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
      return 0.0;
    }

    BigDecimal change = current.subtract(previous);
    BigDecimal percentage =
        change.divide(previous, 4, RoundingMode.HALF_UP).multiply(new BigDecimal(100));

    return percentage.doubleValue();
  }
}
