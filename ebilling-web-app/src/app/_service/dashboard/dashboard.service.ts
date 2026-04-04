import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { WelcomeDashboardResponse } from '../../_model/report/welcome-dashboard-dto';

/**
 * DashboardService - Servicio para el dashboard de bienvenida
 *
 * Este servicio usa HOST_EBILLING (puerto 8480) porque el endpoint
 * /api/dashboard/welcome fue implementado en ebill-billing, no en ebill-report
 */
@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly baseUrl = `${environment.HOST_EBILLING}/api/dashboard`;

  constructor(private readonly http: HttpClient) {}

  /**
   * Dashboard de bienvenida (Welcome Screen)
   * GET /api/dashboard/welcome?companyId={id}
   *
   * Retorna un resumen ejecutivo optimizado con:
   * - KPIs (hoy, semana, mes, pendientes)
   * - Tendencia de ventas (últimos 7 días)
   * - Últimas 5 facturas emitidas
   * - Alertas dinámicas
   */
  getWelcomeDashboard(companyId: number): Observable<WelcomeDashboardResponse> {
    const params = {
      companyId: companyId.toString(),
    };

    return this.http.get<WelcomeDashboardResponse>(`${this.baseUrl}/welcome`, {
      params,
    });
  }
}
