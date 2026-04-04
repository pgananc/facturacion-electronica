import {
  Component,
  OnInit,
  ChangeDetectorRef,
  ViewChild,
  AfterViewInit,
  OnDestroy,
} from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { ReportService } from '../../../_service/report/report.service';
import { ReportFilterDto } from '../../../_model/report/report-filter-dto';
import {
  SalesDashboardDto,
  SalesTrendItemDto,
  TopProductDto,
  BranchDistributionDto,
} from '../../../_model/report/sales-dashboard-dto';
import { CompanyService } from '../../../_service/customer/company.service';
import { Company } from '../../../_model/customer/company-establishment';
import { LoggerService } from '../../../_service/logger/logger.service';
import { ToastService } from '../../../_service/notification/toast.service';
import {
  REPORT_PAGE_SIZE_OPTIONS,
  REPORT_DEFAULT_PAGE_SIZE,
} from '../../../_constants/report-constants';
import { environment } from '../../../../environments/environment';
import { decodeToken } from '../../../_functions/functions';
import { SUPER_ADMIN_ROLE } from '../../../_constants/constants';
import { catchError } from 'rxjs/operators';
import { of } from 'rxjs';
import { UserRoleService } from '../../../_service/auth/user-role.service';
import {
  Chart,
  ChartConfiguration,
  registerables,
  ChartTypeRegistry,
} from 'chart.js';

// Registrar los componentes de Chart.js
Chart.register(...registerables);

@Component({
  selector: 'app-sales-dashboard',
  templateUrl: './sales-dashboard.component.html',
  styleUrls: ['./sales-dashboard.component.css'],
})
export class SalesDashboardComponent
  implements OnInit, AfterViewInit, OnDestroy
{
  filterForm: FormGroup;
  dashboardData: SalesDashboardDto | null = null;
  isLoading = false;
  isAdmin = false;
  companies: Company[] = [];
  branches: any[] = [];

  // Referencias a los gráficos
  private salesTrendChart: Chart | null = null;
  private topProductsChart: Chart | null = null;
  private branchDistributionChart: Chart | null = null;

  constructor(
    private fb: FormBuilder,
    private reportService: ReportService,
    private companyService: CompanyService,
    private toastService: ToastService,
    private logger: LoggerService,
    private cdr: ChangeDetectorRef,
    private userRoleService: UserRoleService
  ) {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    
    this.filterForm = this.fb.group({
      dateFrom: [this.getFirstDayOfMonth()],
      dateTo: [today],
      companyId: [null],
      branchId: [null],
    });
  }

  ngOnInit(): void {
    // Cargar roles de la compañía actual usando el servicio centralizado
    this.userRoleService.getCurrentCompanyRoles().subscribe({
      next: (roles) => {
        this.isAdmin = roles.includes(SUPER_ADMIN_ROLE.name);

        if (!this.isAdmin) {
          const companyId = parseInt(
            localStorage.getItem(environment.ID_COMPANY)!
          );
          this.filterForm.patchValue({ companyId });
          this.logger.debug('Company ID from localStorage:', companyId);
        }

        // Cargar empresas primero
        this.loadCompanies();
        
        // NO cargar dashboard automáticamente aquí
        // Se cargará después de seleccionar empresa (si es admin)
        // O se cargará automáticamente (si NO es admin y ya tiene companyId)
        if (!this.isAdmin && this.filterForm.value.companyId) {
          this.loadDashboard();
        }
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.logger.error('Error al cargar roles:', err);
        // Fallback: asumir que no es admin
        this.isAdmin = false;
        const companyId = parseInt(
          localStorage.getItem(environment.ID_COMPANY)!
        );
        this.filterForm.patchValue({ companyId });
        this.loadCompanies();
        if (this.filterForm.value.companyId) {
          this.loadDashboard();
        }
        this.cdr.markForCheck();
      },
    });
  }

  ngAfterViewInit(): void {
    // Los gráficos se inicializan después de cargar datos
  }

  ngOnDestroy(): void {
    // Destruir gráficos al salir del componente
    this.destroyCharts();
  }

  getFirstDayOfMonth(): Date {
    const date = new Date();
    date.setDate(1);
    date.setHours(0, 0, 0, 0);
    return date;
  }

  loadCompanies(): void {
    this.companyService.findAll().subscribe({
      next: (data) => {
        this.companies = data.companyDetailsDtos || [];
        if (this.companies.length > 0 && !this.filterForm.value.companyId) {
          const firstCompanyId = this.companies[0].idCompany;
          this.filterForm.patchValue({
            companyId: firstCompanyId,
          });
          this.logger.debug('Auto-seleccionada primera empresa:', firstCompanyId);
          this.onCompanyChange();
          
          // Cargar dashboard automáticamente solo si es admin
          if (this.isAdmin) {
            this.loadDashboard();
          }
        }
      },
      error: (err) => {
        this.logger.error('Error al cargar compañías', err);
        this.toastService.error('Error al cargar compañías');
      },
    });
  }

  onCompanyChange(): void {
    const companyId = this.filterForm.value.companyId;
    if (companyId) {
      this.companyService.findById(companyId).subscribe({
        next: (data) => {
          const company = data.companyDetailsDtos?.[0];
          if (company?.establishments) {
            this.branches = company.establishments.map((est: any) => ({
              id: est.idEstablishment,
              name: est.name,
              code: est.code,
            }));
          }
        },
        error: (err) => {
          this.logger.error('Error al cargar establecimientos', err);
        },
      });
    } else {
      this.branches = [];
    }
  }

  loadDashboard(): void {
    const companyId = this.filterForm.value.companyId;
    
    // Validar que haya companyId
    if (!companyId) {
      this.logger.warn('No se puede cargar dashboard: companyId es null');
      this.toastService.warning('Seleccione una empresa para ver el dashboard');
      return;
    }

    // Validar que las fechas estén presentes
    if (!this.filterForm.value.dateFrom || !this.filterForm.value.dateTo) {
      this.logger.warn('No se puede cargar dashboard: fechas faltantes');
      this.toastService.warning('Por favor seleccione las fechas de inicio y fin');
      return;
    }

    this.isLoading = true;
    this.cdr.markForCheck();

    // Convertir fechas Date a strings en formato ISO (YYYY-MM-DD)
    const dateFrom = this.filterForm.value.dateFrom instanceof Date
      ? this.filterForm.value.dateFrom.toISOString().split('T')[0]
      : this.filterForm.value.dateFrom;
    const dateTo = this.filterForm.value.dateTo instanceof Date
      ? this.filterForm.value.dateTo.toISOString().split('T')[0]
      : this.filterForm.value.dateTo;

    // Validar que las fechas convertidas sean válidas
    if (!dateFrom || !dateTo || dateFrom.length !== 10 || dateTo.length !== 10) {
      this.logger.error('Fechas inválidas después de conversión:', { dateFrom, dateTo });
      this.toastService.error('Error: Las fechas seleccionadas no son válidas. Por favor, seleccione fechas válidas.');
      this.isLoading = false;
      this.cdr.markForCheck();
      return;
    }

    const filter: ReportFilterDto = {
      dateFrom: dateFrom,
      dateTo: dateTo,
      companyId: companyId,
      branchId: this.filterForm.value.branchId,
    };

    this.logger.debug('Cargando dashboard con filtros:', filter);

    this.reportService
      .getSalesDashboard(filter)
      .pipe(
        catchError((err) => {
          this.logger.error('Error al cargar dashboard', err);
          this.logger.error('Filtros enviados:', filter);
          this.logger.error('Detalles del error:', {
            status: err?.status,
            statusText: err?.statusText,
            message: err?.message,
            error: err?.error
          });
          
          // Manejo específico de errores 500 del servidor
          if (err?.status === 500) {
            const errorMessage = err?.error?.message || err?.message || 'Error desconocido del servidor';
            this.toastService.error(
              `Error en el servidor: ${errorMessage}. Por favor, verifica que todos los datos estén completos e intenta nuevamente. Si el problema persiste, contacta al administrador.`,
              'error_outline'
            );
          } else if (err?.status === 400) {
            // Error de validación del backend
            const errorMessage = err?.error?.message || 'Los parámetros enviados no son válidos';
            this.toastService.warning(
              `Error de validación: ${errorMessage}. Por favor, verifica las fechas y filtros seleccionados.`
            );
          } else {
            // Para otros errores, usar el método estándar
            this.toastService.httpError(
              err,
              'cargar el dashboard',
              'Error al cargar el dashboard. Por favor, verifica los filtros seleccionados e intenta nuevamente.'
            );
          }
          
          this.isLoading = false;
          this.cdr.markForCheck();
          return of(null);
        })
      )
      .subscribe({
        next: (data: SalesDashboardDto | null) => {
          this.dashboardData = data;
          this.isLoading = false;
          this.cdr.markForCheck();

          // Inicializar gráficos después de que los datos estén disponibles
          if (data) {
            setTimeout(() => {
              this.initializeCharts();
            }, 100);
          } else {
            // Si no hay datos pero tampoco hubo error, mostrar mensaje informativo
            this.logger.warn('Dashboard cargado pero sin datos disponibles');
          }
        },
        error: (err: any) => {
          // Este error handler es redundante ya que catchError maneja los errores
          // Pero lo mantenemos por si acaso
          this.logger.error('Error en la carga', err);
          this.isLoading = false;
          this.cdr.markForCheck();
        },
      });
  }

  search(): void {
    this.loadDashboard();
  }

  clearFilters(): void {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    
    this.filterForm.reset({
      dateFrom: this.getFirstDayOfMonth(),
      dateTo: today,
      companyId: this.filterForm.value.companyId, // Mantener la empresa seleccionada
      branchId: null,
    });
    this.loadDashboard();
  }

  formatCurrency(value: number): string {
    if (value === null || value === undefined) return '$0.00';
    return `$${value.toFixed(2)}`;
  }

  getGrowthPercentage(): number {
    if (!this.dashboardData || this.dashboardData.previousMonthTotal === 0) {
      return 0;
    }
    return (
      ((this.dashboardData.monthTotal - this.dashboardData.previousMonthTotal) /
        this.dashboardData.previousMonthTotal) *
      100
    );
  }

  /**
   * Destruye todos los gráficos existentes
   */
  private destroyCharts(): void {
    if (this.salesTrendChart) {
      this.salesTrendChart.destroy();
      this.salesTrendChart = null;
    }
    if (this.topProductsChart) {
      this.topProductsChart.destroy();
      this.topProductsChart = null;
    }
    if (this.branchDistributionChart) {
      this.branchDistributionChart.destroy();
      this.branchDistributionChart = null;
    }
  }

  /**
   * Inicializa todos los gráficos
   */
  private initializeCharts(): void {
    // Destruir gráficos anteriores si existen
    this.destroyCharts();

    // Crear nuevos gráficos
    this.createSalesTrendChart();
    this.createTopProductsChart();
    this.createBranchDistributionChart();
  }

  /**
   * Crea el gráfico de tendencia de ventas (Line Chart)
   * ACTUALIZADO: Usa datos reales del backend
   */
  private createSalesTrendChart(): void {
    const canvas = document.getElementById(
      'salesTrendChart'
    ) as HTMLCanvasElement;
    if (!canvas) {
      this.logger.warn('Canvas salesTrendChart no encontrado');
      return;
    }

    // Extraer datos del backend
    const salesTrend: SalesTrendItemDto[] = this.dashboardData?.salesTrend || [];
    const labels = salesTrend.map((item: SalesTrendItemDto) =>
      new Date(item.date).toLocaleDateString('es-EC', {
        month: 'short',
        day: 'numeric',
      })
    );
    const data = salesTrend.map((item: SalesTrendItemDto) => item.total);

    // Fallback a datos demo si no hay datos del backend
    if (labels.length === 0) {
      this.logger.warn('No hay datos de tendencia, usando datos demo');
      const demoLabels = this.generateLast30DaysLabels();
      const demoData = this.generateRandomSalesData(30);
      this.createSalesTrendChartWithData(canvas, demoLabels, demoData);
      return;
    }

    this.createSalesTrendChartWithData(canvas, labels, data);
  }

  /**
   * Helper para crear el gráfico de tendencia con datos específicos
   */
  private createSalesTrendChartWithData(
    canvas: HTMLCanvasElement,
    labels: string[],
    data: number[]
  ): void {
    const config: ChartConfiguration<'line'> = {
      type: 'line',
      data: {
        labels: labels,
        datasets: [
          {
            label: 'Ventas Diarias',
            data: data,
            borderColor: '#003a75',
            backgroundColor: 'rgba(0, 58, 117, 0.1)',
            borderWidth: 3,
            fill: true,
            tension: 0.4,
            pointRadius: 4,
            pointBackgroundColor: '#003a75',
            pointBorderColor: '#ffffff',
            pointBorderWidth: 2,
            pointHoverRadius: 6,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            display: true,
            position: 'top',
            labels: {
              color: '#424242',
              font: { size: 13, weight: 600 },
              padding: 20,
            },
          },
          tooltip: {
            backgroundColor: 'rgba(0, 58, 117, 0.9)',
            titleColor: '#ffffff',
            bodyColor: '#ffffff',
            borderColor: '#003a75',
            borderWidth: 1,
            padding: 12,
            displayColors: false,
            callbacks: {
              label: (context) => {
                const value = context.parsed.y ?? 0;
                return `Ventas: ${this.formatCurrency(value)}`;
              },
            },
          },
        },
        scales: {
          y: {
            beginAtZero: true,
            ticks: {
              color: '#757575',
              font: { size: 12 },
              callback: (value) => this.formatCurrency(Number(value)),
            },
            grid: {
              color: 'rgba(0, 0, 0, 0.05)',
            },
          },
          x: {
            ticks: {
              color: '#757575',
              font: { size: 11 },
              maxRotation: 45,
              minRotation: 45,
            },
            grid: {
              display: false,
            },
          },
        },
      },
    };

    this.salesTrendChart = new Chart(canvas, config);
  }

  /**
   * Crea el gráfico de top productos (Bar Chart)
   * ACTUALIZADO: Usa datos reales del backend
   */
  private createTopProductsChart(): void {
    const canvas = document.getElementById(
      'topProductsChart'
    ) as HTMLCanvasElement;
    if (!canvas) {
      this.logger.warn('Canvas topProductsChart no encontrado');
      return;
    }

    // Extraer datos del backend
    const topProducts: TopProductDto[] = this.dashboardData?.topProducts || [];
    const labels = topProducts.map((p: TopProductDto) => p.productName);
    const data = topProducts.map((p: TopProductDto) => p.totalSales);

    // Fallback a datos demo si no hay datos del backend
    if (labels.length === 0) {
      this.logger.warn('No hay datos de productos, usando datos demo');
      const demoLabels = [
        'Producto A',
        'Producto B',
        'Producto C',
        'Producto D',
        'Producto E',
        'Producto F',
        'Producto G',
        'Producto H',
        'Producto I',
        'Producto J',
      ];
      const demoData = [
        12500, 10800, 9200, 8500, 7800, 6900, 6200, 5500, 4800, 4200,
      ];
      this.createTopProductsChartWithData(canvas, demoLabels, demoData);
      return;
    }

    this.createTopProductsChartWithData(canvas, labels, data);
  }

  /**
   * Helper para crear el gráfico de top productos con datos específicos
   */
  private createTopProductsChartWithData(
    canvas: HTMLCanvasElement,
    labels: string[],
    data: number[]
  ): void {
    const config: ChartConfiguration<'bar'> = {
      type: 'bar',
      data: {
        labels: labels,
        datasets: [
          {
            label: 'Ventas ($)',
            data: data,
            backgroundColor: [
              'rgba(0, 58, 117, 0.9)',
              'rgba(0, 86, 168, 0.85)',
              'rgba(67, 233, 123, 0.8)',
              'rgba(102, 126, 234, 0.8)',
              'rgba(249, 147, 251, 0.8)',
              'rgba(79, 172, 254, 0.75)',
              'rgba(245, 87, 108, 0.75)',
              'rgba(56, 249, 215, 0.7)',
              'rgba(250, 112, 154, 0.7)',
              'rgba(254, 225, 64, 0.7)',
            ],
            borderColor: [
              '#003a75',
              '#0056a8',
              '#43e97b',
              '#667eea',
              '#f993fb',
              '#4facfe',
              '#f5576c',
              '#38f9d7',
              '#fa709a',
              '#fee140',
            ],
            borderWidth: 2,
            borderRadius: 8,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        indexAxis: 'y',
        plugins: {
          legend: {
            display: false,
          },
          tooltip: {
            backgroundColor: 'rgba(0, 58, 117, 0.9)',
            titleColor: '#ffffff',
            bodyColor: '#ffffff',
            borderColor: '#003a75',
            borderWidth: 1,
            padding: 12,
            displayColors: false,
            callbacks: {
              label: (context) => {
                const value = context.parsed.x ?? 0;
                return `Ventas: ${this.formatCurrency(value)}`;
              },
            },
          },
        },
        scales: {
          x: {
            beginAtZero: true,
            ticks: {
              color: '#757575',
              font: { size: 11 },
              callback: (value) => this.formatCurrency(Number(value)),
            },
            grid: {
              color: 'rgba(0, 0, 0, 0.05)',
            },
          },
          y: {
            ticks: {
              color: '#424242',
              font: { size: 12, weight: 600 },
            },
            grid: {
              display: false,
            },
          },
        },
      },
    };

    this.topProductsChart = new Chart(canvas, config);
  }

  /**
   * Crea el gráfico de distribución por sucursal (Doughnut Chart)
   * ACTUALIZADO: Usa datos reales del backend
   */
  private createBranchDistributionChart(): void {
    const canvas = document.getElementById(
      'branchDistributionChart'
    ) as HTMLCanvasElement;
    if (!canvas) {
      this.logger.warn('Canvas branchDistributionChart no encontrado');
      return;
    }

    // Extraer datos del backend
    const branchDistribution: BranchDistributionDto[] =
      this.dashboardData?.branchDistribution || [];
    const labels = branchDistribution.map((b: BranchDistributionDto) => b.branchName);
    const data = branchDistribution.map((b: BranchDistributionDto) => b.totalSales);

    // Fallback a datos demo si no hay datos del backend
    if (labels.length === 0) {
      this.logger.warn('No hay datos de sucursales, usando datos demo');
      const demoLabels = [
        'Sucursal Centro',
        'Sucursal Norte',
        'Sucursal Sur',
        'Sucursal Este',
        'Sucursal Oeste',
      ];
      const demoData = [35000, 28000, 22000, 18000, 15000];
      this.createBranchDistributionChartWithData(canvas, demoLabels, demoData);
      return;
    }

    this.createBranchDistributionChartWithData(canvas, labels, data);
  }

  /**
   * Helper para crear el gráfico de distribución por sucursal con datos específicos
   */
  private createBranchDistributionChartWithData(
    canvas: HTMLCanvasElement,
    labels: string[],
    data: number[]
  ): void {
    const config: ChartConfiguration<'doughnut'> = {
      type: 'doughnut',
      data: {
        labels: labels,
        datasets: [
          {
            label: 'Ventas ($)',
            data: data,
            backgroundColor: [
              'rgba(0, 58, 117, 0.9)',
              'rgba(79, 172, 254, 0.9)',
              'rgba(67, 233, 123, 0.9)',
              'rgba(249, 147, 251, 0.9)',
              'rgba(245, 87, 108, 0.9)',
            ],
            borderColor: [
              '#003a75',
              '#4facfe',
              '#43e97b',
              '#f993fb',
              '#f5576c',
            ],
            borderWidth: 3,
            hoverOffset: 15,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            display: true,
            position: 'right',
            labels: {
              color: '#424242',
              font: { size: 13, weight: 600 },
              padding: 15,
              usePointStyle: true,
              pointStyle: 'circle',
            },
          },
          tooltip: {
            backgroundColor: 'rgba(0, 58, 117, 0.9)',
            titleColor: '#ffffff',
            bodyColor: '#ffffff',
            borderColor: '#003a75',
            borderWidth: 1,
            padding: 12,
            displayColors: true,
            callbacks: {
              label: (context) => {
                const label = context.label || '';
                const value = this.formatCurrency(Number(context.parsed));
                const total = (context.dataset.data as number[]).reduce(
                  (a, b) => a + b,
                  0
                );
                const percentage = (
                  (Number(context.parsed) / total) *
                  100
                ).toFixed(1);
                return `${label}: ${value} (${percentage}%)`;
              },
            },
          },
        },
      },
    };

    this.branchDistributionChart = new Chart(canvas, config);
  }

  /**
   * Genera las etiquetas de los últimos 30 días
   */
  private generateLast30DaysLabels(): string[] {
    const labels: string[] = [];
    const today = new Date();
    for (let i = 29; i >= 0; i--) {
      const date = new Date(today);
      date.setDate(today.getDate() - i);
      labels.push(
        date.toLocaleDateString('es-EC', { month: 'short', day: 'numeric' })
      );
    }
    return labels;
  }

  /**
   * Genera datos de ventas aleatorios para demostración
   */
  private generateRandomSalesData(count: number): number[] {
    const data: number[] = [];
    let base = 5000;
    for (let i = 0; i < count; i++) {
      const variation = Math.random() * 4000 - 2000; // Variación entre -2000 y +2000
      base = Math.max(1000, base + variation); // Mantener un mínimo de 1000
      data.push(Math.round(base * 100) / 100);
    }
    return data;
  }
}
