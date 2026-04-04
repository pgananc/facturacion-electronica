import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { LoggerService } from '../logger/logger.service';
import { ReportFilterDto } from '../../_model/report/report-filter-dto';
import {
  SalesByPeriodResponse,
  SalesByPeriodMetrics,
} from '../../_model/report/sales-by-period-dto';
import { SalesByClientResponse } from '../../_model/report/sales-by-client-dto';
import { SalesByProductResponse } from '../../_model/report/sales-by-product-dto';
import { CreditNoteReportResponse } from '../../_model/report/credit-note-report-dto';
import { PendingInvoiceResponse } from '../../_model/report/pending-invoices-dto';
import { SalesDashboardDto } from '../../_model/report/sales-dashboard-dto';
import { BranchComparisonResponse } from '../../_model/report/branch-comparison-dto';
import { PaymentMethodReportResponse } from '../../_model/report/payment-method-report-dto';
import { WelcomeDashboardResponse } from '../../_model/report/welcome-dashboard-dto';

/**
 * ReportService - Servicio para reportes operativos y gerenciales
 *
 * El token JWT es manejado automáticamente por JwtModule
 */
@Injectable({ providedIn: 'root' })
export class ReportService {
  private readonly baseUrl = `${environment.HOST_REPORT}/api/report`;

  constructor(
    private readonly http: HttpClient,
    private readonly logger: LoggerService,
  ) {}

  /**
   * Formatea una fecha para dateFrom (agrega T00:00:00)
   * Extrae solo la parte de fecha (YYYY-MM-DD) si viene con hora
   * Acepta string, Date, null o undefined
   */
  private formatDateFrom(dateStr: string | Date | null | undefined): string | null {
    if (!dateStr) return null;
    
    // Si es un objeto Date, convertirlo a string ISO
    let dateString: string;
    if (dateStr instanceof Date) {
      dateString = dateStr.toISOString();
    } else {
      dateString = dateStr;
    }
    
    if (!dateString || typeof dateString !== 'string') return null;
    
    // Si ya tiene formato completo (YYYY-MM-DDTHH:mm:ss), extraer solo la fecha
    const dateOnly = dateString.split('T')[0];
    if (!dateOnly || dateOnly.length !== 10) return null;
    
    return `${dateOnly}T00:00:00`;
  }

  /**
   * Formatea una fecha para dateTo (agrega T23:59:59)
   * Extrae solo la parte de fecha (YYYY-MM-DD) si viene con hora
   * Acepta string, Date, null o undefined
   */
  private formatDateTo(dateStr: string | Date | null | undefined): string | null {
    if (!dateStr) return null;
    
    // Si es un objeto Date, convertirlo a string ISO
    let dateString: string;
    if (dateStr instanceof Date) {
      dateString = dateStr.toISOString();
    } else {
      dateString = dateStr;
    }
    
    if (!dateString || typeof dateString !== 'string') return null;
    
    // Si ya tiene formato completo (YYYY-MM-DDTHH:mm:ss), extraer solo la fecha
    const dateOnly = dateString.split('T')[0];
    if (!dateOnly || dateOnly.length !== 10) return null;
    
    return `${dateOnly}T23:59:59`;
  }

  /**
   * Reporte de ventas por período
   * POST /api/report/sales/by-period
   *
   * El backend ahora acepta Pageable como query params:
   * - page: número de página (default: 0)
   * - size: tamaño de página (default: 20)
   * - sort: campo de ordenamiento (default: "issueDate")
   * - direction: dirección del ordenamiento (default: "DESC")
   */
  getSalesByPeriod(
    filter: ReportFilterDto,
    page: number = 0,
    size: number = 20,
    sort: string = 'issueDate',
    direction: string = 'DESC'
  ): Observable<SalesByPeriodResponse> {
    const body: any = {
      dateFrom: this.formatDateFrom(filter.dateFrom),
      dateTo: this.formatDateTo(filter.dateTo),
      idCompany: filter.companyId || null,
      idEstablishment: filter.branchId || null,
      idIssuancePoint: filter.emissionPointId || null,
      authorizationStatusSri: filter.status || null,
      receptionStatusSri: null,
    };

    // Enviar parámetros de paginación como query params
    // Spring Boot espera: sort=field,direction (ej: sort=issueDate,DESC)
    const params: any = {
      page: page.toString(),
      size: size.toString(),
      sort: direction && direction.trim() ? `${sort},${direction}` : sort,
    };

    return this.http.post<SalesByPeriodResponse>(
      `${this.baseUrl}/sales/by-period`,
      body,
      { params }
    );
  }

  /**
   * Métricas de ventas por período
   * POST /api/report/sales/by-period (mismo endpoint, pero se calculan métricas)
   * Nota: Si el backend tiene un endpoint separado para métricas, ajustar la URL
   */
  getSalesByPeriodMetrics(
    filter: ReportFilterDto
  ): Observable<SalesByPeriodMetrics> {
    const body = {
      dateFrom: this.formatDateFrom(filter.dateFrom),
      dateTo: this.formatDateTo(filter.dateTo),
      idCompany: filter.companyId || null,
      idEstablishment: filter.branchId || null,
      idIssuancePoint: filter.emissionPointId || null,
      documentType: filter.documentType || null,
      authorizationStatusSri: filter.status || null,
      receptionStatusSri: null,
    };

    // Si el backend tiene un endpoint separado para métricas, usar:
    // return this.http.post<SalesByPeriodMetrics>(`${this.baseUrl}/sales/by-period/metrics`, body);
    // Por ahora, asumimos que las métricas vienen en la misma respuesta
    return this.http.post<SalesByPeriodMetrics>(
      `${this.baseUrl}/sales/by-period`,
      body
    );
  }

  /**
   * Reporte de ventas por cliente
   * POST /api/report/sales/by-client
   *
   * El backend ahora acepta Pageable como query params:
   * - page: número de página (default: 0)
   * - size: tamaño de página (default: 20)
   * - sort: campo de ordenamiento (default: "totalSales")
   * - direction: dirección del ordenamiento (default: "DESC")
   */
  getSalesByClient(
    filter: ReportFilterDto,
    page: number = 0,
    size: number = 20,
    sort: string = 'totalSales',
    direction: string = 'DESC'
  ): Observable<SalesByClientResponse> {
    // El backend espera: dateFrom, dateTo, idCompany, idEstablishment
    const body: any = {
      dateFrom: this.formatDateFrom(filter.dateFrom),
      dateTo: this.formatDateTo(filter.dateTo),
      idCompany: filter.companyId || null,
      idEstablishment: filter.branchId || null,
    };

    // Enviar parámetros de paginación como query params
    // Spring Boot espera: sort=field,direction (ej: sort=totalSales,DESC)
    const params: any = {
      page: page.toString(),
      size: size.toString(),
      sort: direction ? `${sort},${direction}` : sort,
    };

    return this.http
      .post<SalesByClientResponse>(`${this.baseUrl}/sales/by-client`, body, {
        params,
      })
;
  }

  /**
   * Reporte de ventas por producto/servicio
   * POST /api/report/sales/by-product
   *
   * El backend ahora acepta Pageable como query params:
   * - page: número de página (default: 0)
   * - size: tamaño de página (default: 20)
   * - sort: campo de ordenamiento (default: "totalInvoiced")
   * - direction: dirección del ordenamiento (default: "DESC")
   */
  getSalesByProduct(
    filter: ReportFilterDto,
    page: number = 0,
    size: number = 20,
    sort: string = 'totalInvoiced',
    direction: string = 'DESC'
  ): Observable<SalesByProductResponse> {
    // El backend espera: dateFrom, dateTo, idCompany, idEstablishment, idProduct (opcional)
    const body: any = {
      dateFrom: this.formatDateFrom(filter.dateFrom),
      dateTo: this.formatDateTo(filter.dateTo),
      idCompany: filter.companyId || null,
      idEstablishment: filter.branchId || null,
    };

    // Solo agregar idProduct si se proporciona
    if (filter.productId) {
      body.idProduct = filter.productId;
    }

    // Enviar parámetros de paginación como query params
    // Spring Boot espera: sort=field,direction (ej: sort=totalInvoiced,DESC)
    const params: any = {
      page: page.toString(),
      size: size.toString(),
      sort: direction ? `${sort},${direction}` : sort,
    };

    return this.http.post<SalesByProductResponse>(
      `${this.baseUrl}/sales/by-product`,
      body,
      { params }
    );
  }

  /**
   * Reporte de notas de crédito
   * POST /api/report/credit-notes
   */
  getCreditNoteReport(
    filter: ReportFilterDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'createDate',
    direction: string = 'DESC'
  ): Observable<CreditNoteReportResponse> {
    const body: any = {
      dateFrom: this.formatDateFrom(filter.dateFrom),
      dateTo: this.formatDateTo(filter.dateTo),
      idCompany: filter.companyId || null,
      idClient: filter.clientId || null,
      originalInvoiceId: null,
    };

    const params: any = {
      page: page.toString(),
      size: size.toString(),
      sort: direction && direction.trim() ? `${sort},${direction}` : sort,
    };

    return this.http
      .post<CreditNoteReportResponse>(`${this.baseUrl}/credit-notes`, body, {
        params,
      })
      .pipe(
        map((response: CreditNoteReportResponse) => {
          // Mapear la respuesta del backend al formato esperado por el componente
          return {
            ...response,
            // Normalizar totalAmount para que siempre sea número
            items:
              response.items?.map((item) => ({
                ...item,
                totalAmount:
                  typeof item.totalAmount === 'object'
                    ? item.totalAmount.parsedValue
                    : item.totalAmount,
              })) || [],
          };
        })
      );
  }

  /**
   * Reporte de facturas pendientes/errores SRI
   * POST /api/report/invoices/pending
   */
  getPendingInvoices(
    filter: ReportFilterDto,
    page: number = 0,
    size: number = 10
  ): Observable<PendingInvoiceResponse> {
    // El backend espera: dateFrom, dateTo, idCompany, idEstablishment, statusFilter
    // No incluye paginación en el request según el DTO del backend
    const body: any = {
      dateFrom: this.formatDateFrom(filter.dateFrom),
      dateTo: this.formatDateTo(filter.dateTo),
      idCompany: filter.companyId || null,
      idEstablishment: filter.branchId || null,
      statusFilter: filter.status || null,
    };

    return this.http.post<PendingInvoiceResponse>(
      `${this.baseUrl}/invoices/pending`,
      body
    );
  }

  /**
   * Dashboard de ventas
   * GET /api/report/dashboard/sales
   *
   * Según especificación backend (diciembre 2024):
   * - Query params: companyId, dateFrom, dateTo, branchId (opcional)
   * - Response: ApiResponse<SalesDashboardDto> con estructura optimizada
   */
  getSalesDashboard(filter: ReportFilterDto): Observable<SalesDashboardDto> {
    // Validar que companyId esté presente
    if (!filter.companyId) {
      throw new Error('companyId es requerido para cargar el dashboard');
    }

    // Validar que las fechas estén presentes
    const dateFrom = this.formatDateFrom(filter.dateFrom);
    const dateTo = this.formatDateTo(filter.dateTo);
    
    if (!dateFrom || !dateTo) {
      throw new Error('dateFrom y dateTo son requeridos para cargar el dashboard');
    }

    const params: any = {
      companyId: filter.companyId.toString(), // Convertir a string explícitamente
      dateFrom: dateFrom,
      dateTo: dateTo,
    };

    // Agregar branchId solo si está presente
    if (filter.branchId) {
      params.branchId = filter.branchId.toString();
    }

    return this.http
      .get<{ status: string; message: string; data: SalesDashboardDto }>(
        `${this.baseUrl}/dashboard/sales`,
        { params }
      )
      .pipe(
        tap((response) => {
          this.logger.debug('[Dashboard] Response status:', { status: response.status });
        }),
        map((response) => response.data)
      );
  }

  /**
   * Reporte comparativo por sucursal
   * POST /api/report/dashboard/comparison
   */
  getBranchComparison(
    filter: ReportFilterDto,
    page: number = 0,
    size: number = 10
  ): Observable<BranchComparisonResponse> {
    // El backend espera: dateFrom, dateTo, idCompany
    // No incluye paginación en el request según el DTO del backend
    const body: any = {
      dateFrom: this.formatDateFrom(filter.dateFrom),
      dateTo: this.formatDateTo(filter.dateTo),
      idCompany: filter.companyId || null,
    };

    return this.http.post<BranchComparisonResponse>(
      `${this.baseUrl}/dashboard/comparison`,
      body
    );
  }

  /**
   * Reporte de formas de pago
   * POST /api/report/payment-methods
   */
  getPaymentMethodReport(
    filter: ReportFilterDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'totalAmount',
    direction: string = 'DESC'
  ): Observable<PaymentMethodReportResponse> {
    const body: any = {
      dateFrom: this.formatDateFrom(filter.dateFrom),
      dateTo: this.formatDateTo(filter.dateTo),
      idCompany: filter.companyId || null,
      idEstablishment: filter.branchId || null,
    };

    const params: any = {
      page: page.toString(),
      size: size.toString(),
      sort: direction && direction.trim() ? `${sort},${direction}` : sort,
    };

    return this.http.post<PaymentMethodReportResponse>(
      `${this.baseUrl}/dashboard/payment-methods`,
      body,
      { params }
    );
  }

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

    return this.http.get<WelcomeDashboardResponse>(
      `${this.baseUrl}/dashboard/welcome`,
      { params }
    );
  }
}
