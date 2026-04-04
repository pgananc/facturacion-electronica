package com.wallparisoft.ebillbilling.service;

import com.wallparisoft.ebillbilling.dto.dashboard.WelcomeDashboardDto;

public interface IDashboardService {

  /**
   * Obtiene el dashboard completo de bienvenida para la pantalla Welcome
   *
   * @param companyId ID de la compañía
   * @return WelcomeDashboardDto con todos los datos (KPIs, tendencias, alertas, etc.)
   */
  WelcomeDashboardDto getWelcomeDashboard(Long companyId);
}
