package com.wallparisoft.ebillbilling.controller;

import com.wallparisoft.ebillbilling.dto.dashboard.DashboardResponseDto;
import com.wallparisoft.ebillbilling.dto.dashboard.WelcomeDashboardDto;
import com.wallparisoft.ebillbilling.service.IDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

  private final IDashboardService dashboardService;

  /**
   * GET /api/dashboard/welcome?companyId={id}
   *
   * <p>Retorna un resumen ejecutivo con KPIs, últimas facturas, tendencias y alertas para la
   * pantalla Welcome. Incluye: ventas de hoy/semana/mes, pendientes de cobro, tendencia de últimos
   * 7 días, últimas 5 facturas y alertas (rechazos, puntos de emisión bajos, récords)
   *
   * @param companyId ID de la empresa (required)
   * @return DashboardResponseDto con status, message y data
   */
  @GetMapping("/welcome")
  public ResponseEntity<DashboardResponseDto> getWelcomeDashboard(@RequestParam Long companyId) {

    log.info("📊 GET /api/dashboard/welcome?companyId={}", companyId);

    try {
      WelcomeDashboardDto dashboard = dashboardService.getWelcomeDashboard(companyId);

      DashboardResponseDto response =
          DashboardResponseDto.builder()
              .status("OK")
              .message("Dashboard de bienvenida cargado exitosamente")
              .data(dashboard)
              .build();

      return ResponseEntity.ok(response);

    } catch (Exception e) {
      log.error("❌ Error generando dashboard para companyId {}: {}", companyId, e.getMessage(), e);

      DashboardResponseDto errorResponse =
          DashboardResponseDto.builder()
              .status("ERROR")
              .message("Error al cargar el dashboard: " + e.getMessage())
              .data(null)
              .build();

      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
  }
}
