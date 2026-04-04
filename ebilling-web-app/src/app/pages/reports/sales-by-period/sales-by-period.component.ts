import { Component, OnInit, ViewChild, ChangeDetectorRef } from '@angular/core';
import { MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { FormBuilder, FormGroup } from '@angular/forms';
import jsPDF from 'jspdf';
import * as XLSX from 'xlsx';
import { ReportService } from '../../../_service/report/report.service';
import { ReportFilterDto } from '../../../_model/report/report-filter-dto';
import {
  SalesByPeriodDto,
  SalesByPeriodItemDto,
  SalesByPeriodMetrics,
  SalesByPeriodResponse,
} from '../../../_model/report/sales-by-period-dto';
import { CompanyService } from '../../../_service/customer/company.service';
import { EstablishmentService } from '../../../_service/customer/establishment.service';
import { IssuancePointService } from '../../../_service/customer/issuance-point.service';
import { Company } from '../../../_model/customer/company-establishment';
import { LoggerService } from '../../../_service/logger/logger.service';
import { ToastService } from '../../../_service/notification/toast.service';
import { environment } from '../../../../environments/environment';
import { decodeToken } from '../../../_functions/functions';
import { SUPER_ADMIN_ROLE } from '../../../_constants/constants';
import { UserRoleService } from '../../../_service/auth/user-role.service';
import {
  REPORT_PAGE_SIZE_OPTIONS,
  REPORT_DEFAULT_PAGE_SIZE,
  SriAuthorizationStatus,
  SRI_STATUS_LABELS,
  SRI_STATUS_CLASSES,
  SRI_STATUS_CODES,
} from '../../../_constants/report-constants';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Component({
  selector: 'app-sales-by-period',
  templateUrl: './sales-by-period.component.html',
  styleUrls: ['./sales-by-period.component.css'],
})
export class SalesByPeriodComponent implements OnInit {
  filterForm: FormGroup;
  dataSource: MatTableDataSource<SalesByPeriodDto> =
    new MatTableDataSource<SalesByPeriodDto>([]);
  displayedColumns: string[] = [
    'invoiceNumber',
    'issueDate',
    'clientName',
    'clientIdentification',
    'subtotal',
    'iva',
    'total',
    'authorizationStatusSri',
  ];

  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;

  companies: Company[] = [];
  branches: any[] = [];
  emissionPoints: any[] = [];

  // ✅ Usar enums y constantes en lugar de arrays hardcodeados
  statuses = Object.values(SriAuthorizationStatus);
  statusLabels = SRI_STATUS_LABELS;

  isLoading = false;
  totalElements = 0;
  pageSize = REPORT_DEFAULT_PAGE_SIZE;
  pageIndex = 0;
  pageSizeOptions = REPORT_PAGE_SIZE_OPTIONS;

  metrics: SalesByPeriodMetrics = {
    totalInvoiced: 0,
    totalComprobantes: 0,
    averageTicket: 0,
  };

  isAdmin = false;
  isDownloadingPdf = false;
  isDownloadingExcel = false;

  constructor(
    private fb: FormBuilder,
    private reportService: ReportService,
    private companyService: CompanyService,
    private establishmentService: EstablishmentService,
    private issuancePointService: IssuancePointService,
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
      emissionPointId: [null],
      status: [null],
    });
  }

  ngOnInit(): void {
    // Cargar roles de la compañía actual usando el servicio centralizado
    this.userRoleService.getCurrentCompanyRoles().subscribe({
      next: (roles) => {
        this.isAdmin = roles.includes(SUPER_ADMIN_ROLE.name);

        // Establecer la compañía del usuario logueado desde localStorage
        const storedCompanyId = localStorage.getItem(environment.ID_COMPANY);
        if (storedCompanyId) {
          const companyId = parseInt(storedCompanyId);
          this.filterForm.patchValue({ companyId });
        }

        this.loadCompanies();
        this.search();
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.logger.error('Error al cargar roles:', err);
        // Fallback: asumir que no es admin
        this.isAdmin = false;
        const storedCompanyId = localStorage.getItem(environment.ID_COMPANY);
        if (storedCompanyId) {
          const companyId = parseInt(storedCompanyId);
          this.filterForm.patchValue({ companyId });
        }
        this.loadCompanies();
        this.search();
        this.cdr.markForCheck();
      },
    });
  }

  ngAfterViewInit(): void {
    if (this.sort) {
      this.dataSource.sort = this.sort;
    }
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
        const currentCompanyId = this.filterForm.value.companyId;
        let companyIdToLoad: number | null = null;

        // Si ya hay una compañía seleccionada, verificar que existe en la lista
        if (currentCompanyId) {
          const companyExists = this.companies.some(
            (c) => c.idCompany === currentCompanyId
          );
          if (companyExists) {
            companyIdToLoad = currentCompanyId;
          } else {
            // Si la compañía seleccionada no existe en la lista, buscar la del localStorage
            const storedCompanyId = localStorage.getItem(
              environment.ID_COMPANY
            );
            if (storedCompanyId) {
              const companyId = parseInt(storedCompanyId);
              const foundCompany = this.companies.find(
                (c) => c.idCompany === companyId
              );
              if (foundCompany) {
                companyIdToLoad = companyId;
                this.filterForm.patchValue({ companyId: companyIdToLoad });
              }
            }
          }
        } else if (this.companies.length > 0) {
          // Si no hay ninguna seleccionada, intentar primero con la del localStorage
          const storedCompanyId = localStorage.getItem(environment.ID_COMPANY);
          if (storedCompanyId) {
            const companyId = parseInt(storedCompanyId);
            const foundCompany = this.companies.find(
              (c) => c.idCompany === companyId
            );
            if (foundCompany) {
              companyIdToLoad = companyId;
              this.filterForm.patchValue({ companyId: companyIdToLoad });
            } else {
              // Si no se encontró la del localStorage, tomar la primera
              companyIdToLoad = this.companies[0].idCompany;
              this.filterForm.patchValue({ companyId: companyIdToLoad });
            }
          } else {
            // Si no hay localStorage, tomar la primera
            companyIdToLoad = this.companies[0].idCompany;
            this.filterForm.patchValue({ companyId: companyIdToLoad });
          }
        }

        // Cargar sucursales si hay una compañía seleccionada
        if (companyIdToLoad) {
          this.loadBranchesForCompany(companyIdToLoad);
        }
      },
      error: (err) => {
        this.logger.error('Error al cargar compañías', err);
      },
    });
  }

  /**
   * Carga las sucursales de una compañía específica
   * Usa el servicio /api/establishments/company/{idCompany}
   */
  private loadBranchesForCompany(companyId: number): void {
    this.establishmentService.findByCompany(companyId).subscribe({
      next: (establishments) => {
        if (establishments && establishments.length > 0) {
          this.branches = establishments.map((est) => ({
            id: est.idEstablishment,
            name: est.name,
            code: est.code,
          }));
          this.logger.debug(
            `Cargadas ${this.branches.length} sucursales para la compañía ${companyId}`
          );
        } else {
          this.branches = [];
          this.logger.debug(
            `No se encontraron sucursales para la compañía ${companyId}`
          );
        }
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.logger.error('Error al cargar establecimientos', err);
        this.branches = [];
        this.cdr.markForCheck();
      },
    });
  }

  onCompanyChange(): void {
    const companyId = this.filterForm.value.companyId;
    // Limpiar sucursales y puntos de emisión cuando cambia la empresa
    this.branches = [];
    this.emissionPoints = [];
    this.filterForm.patchValue({
      branchId: null,
      emissionPointId: null,
    });

    if (companyId) {
      this.loadBranchesForCompany(companyId);
    }
  }

  onBranchChange(): void {
    const branchId = this.filterForm.value.branchId;

    // Limpiar puntos de emisión cuando cambia la sucursal
    this.emissionPoints = [];
    this.filterForm.patchValue({
      emissionPointId: null,
    });

    if (branchId) {
      // Usar el servicio /api/invoice-issuance-points/establishment/{establishmentId}
      this.issuancePointService.findByEstablishment(branchId).subscribe({
        next: (points) => {
          if (points && points.length > 0) {
            this.emissionPoints = points.map((point) => ({
              id: point.idInvoiceIssuancePoint,
              code: point.code,
            }));
            this.logger.debug(
              `Cargados ${this.emissionPoints.length} puntos de emisión para la sucursal ${branchId}`
            );
          } else {
            this.emissionPoints = [];
            this.logger.debug(
              `No se encontraron puntos de emisión para la sucursal ${branchId}`
            );
          }
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.logger.error('Error al cargar puntos de emisión', err);
          this.emissionPoints = [];
          this.cdr.markForCheck();
        },
      });
    }
  }

  clearFilters(): void {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    
    this.filterForm.reset({
      dateFrom: this.getFirstDayOfMonth(),
      dateTo: today,
      companyId: null,
      branchId: null,
      emissionPointId: null,
      status: null,
    });
    this.search();
  }

  search(): void {
    if (this.filterForm.invalid) {
      this.toastService.warning('Por favor complete los campos requeridos');
      return;
    }

    this.isLoading = true;
    this.cdr.markForCheck();

    // Convertir el estado seleccionado al código que espera el backend
    const selectedStatus = this.filterForm.value.status;
    const statusCode = selectedStatus
      ? SRI_STATUS_CODES[selectedStatus as SriAuthorizationStatus]
      : undefined;

    // Convertir fechas Date a strings en formato ISO (YYYY-MM-DD)
    const dateFrom = this.filterForm.value.dateFrom instanceof Date
      ? this.filterForm.value.dateFrom.toISOString().split('T')[0]
      : this.filterForm.value.dateFrom;
    const dateTo = this.filterForm.value.dateTo instanceof Date
      ? this.filterForm.value.dateTo.toISOString().split('T')[0]
      : this.filterForm.value.dateTo;

    const filter: ReportFilterDto = {
      dateFrom: dateFrom,
      dateTo: dateTo,
      companyId: this.filterForm.value.companyId,
      branchId: this.filterForm.value.branchId,
      emissionPointId: this.filterForm.value.emissionPointId,
      status: statusCode,
    };

    this.reportService
      .getSalesByPeriod(filter, this.pageIndex, this.pageSize)
      .pipe(
        catchError((err) => {
          this.logger.error('Error al cargar reporte', err);
          this.toastService.httpError(err, 'Error al cargar el reporte');
          return of({
            items: [],
            metrics: {
              totalInvoiced: 0,
              totalComprobantes: 0,
              averageTicket: 0,
            },
          } as SalesByPeriodResponse);
        })
      )
      .subscribe({
        next: (result: SalesByPeriodResponse) => {
          this.logger.debug('Respuesta del backend:', result);

          // El backend devuelve { items: [...], metrics: {...} }
          const items = result.items || [];
          const metrics = result.metrics || {
            totalInvoiced: 0,
            totalComprobantes: 0,
            averageTicket: 0,
          };

          this.dataSource.data = [...items];
          // Usar totalElements del backend si está disponible, sino usar el length de items
          this.totalElements = result.totalElements ?? items.length;
          this.metrics = metrics;
          this.isLoading = false;

          if (items.length === 0) {
            this.toastService.info(
              'No se encontraron resultados para los filtros seleccionados'
            );
          } else {
            this.toastService.success(
              `Se encontraron ${items.length} registro(s)`
            );
          }

          this.cdr.detectChanges();
        },
        error: (err) => {
          this.logger.error('Error en la búsqueda', err);
          this.toastService.httpError(err);
          this.isLoading = false;
          this.cdr.markForCheck();
        },
      });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.search();
  }

  formatCurrency(value: number): string {
    if (value === null || value === undefined) return '$0.00';
    return `$${value.toFixed(2)}`;
  }

  formatDate(date: string): string {
    if (!date) return '-';
    try {
      return new Date(date).toLocaleDateString('es-EC', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
      });
    } catch {
      return '-';
    }
  }

  /**
   * Formatea una fecha del formulario (YYYY-MM-DD) sin conversión de zona horaria
   */
  formatFormDate(dateString: string | Date): string {
    if (!dateString) return '-';
    
    // Si es un objeto Date, convertirlo a string ISO
    let dateStr: string;
    if (dateString instanceof Date) {
      dateStr = dateString.toISOString().split('T')[0];
    } else {
      dateStr = dateString;
    }
    
    try {
      // Parsear fecha en formato YYYY-MM-DD directamente sin conversión de zona horaria
      const parts = dateStr.split('-');
      if (parts.length === 3) {
        const year = parts[0];
        const month = parts[1];
        const day = parts[2];
        return `${day}/${month}/${year}`;
      }
      // Si no está en el formato esperado, intentar con formatDate (que acepta string)
      return this.formatDate(dateStr);
    } catch {
      return '-';
    }
  }

  /**
   * ✅ Retorna la clase CSS del estado basada en constantes globales
   * @param status Estado del SRI
   * @returns Clase CSS para aplicar al badge
   */
  getStatusClass(status: string): string {
    const statusEnum = status as SriAuthorizationStatus;
    return SRI_STATUS_CLASSES[statusEnum] || 'status-inactive';
  }

  /**
   * ✅ Retorna el label amigable del estado
   * @param status Estado del SRI
   * @returns Label traducido
   */
  getStatusLabel(status: string): string {
    const statusEnum = status as SriAuthorizationStatus;
    return SRI_STATUS_LABELS[statusEnum] || status;
  }

  /**
   * Obtiene todos los registros del reporte (sin paginación)
   */
  private getAllReportData(): Observable<SalesByPeriodResponse> {
    // Convertir el estado seleccionado al código que espera el backend
    const selectedStatus = this.filterForm.value.status;
    const statusCode = selectedStatus
      ? SRI_STATUS_CODES[selectedStatus as SriAuthorizationStatus]
      : undefined;

    // Convertir fechas Date a strings en formato ISO (YYYY-MM-DD)
    const dateFrom = this.filterForm.value.dateFrom instanceof Date
      ? this.filterForm.value.dateFrom.toISOString().split('T')[0]
      : this.filterForm.value.dateFrom;
    const dateTo = this.filterForm.value.dateTo instanceof Date
      ? this.filterForm.value.dateTo.toISOString().split('T')[0]
      : this.filterForm.value.dateTo;

    const filter: ReportFilterDto = {
      dateFrom: dateFrom,
      dateTo: dateTo,
      companyId: this.filterForm.value.companyId,
      branchId: this.filterForm.value.branchId,
      emissionPointId: this.filterForm.value.emissionPointId,
      status: statusCode,
    };

    // Obtener parámetros de ordenamiento
    let sortField = 'issueDate';
    let sortDirection = 'DESC';

    if (this.sort && this.sort.active) {
      sortField = this.sort.active;
      sortDirection = this.sort.direction.toUpperCase() || 'DESC';
    }

    // Solicitar todos los registros con un tamaño muy grande
    return this.reportService.getSalesByPeriod(
      filter,
      0,
      10000, // Tamaño muy grande para obtener todos los registros
      sortField,
      sortDirection
    );
  }

  /**
   * Genera y descarga el reporte en formato PDF
   */
  downloadPdf(): void {
    if (this.isDownloadingPdf) {
      return;
    }

    this.isDownloadingPdf = true;
    this.cdr.markForCheck();

    // Obtener todos los registros
    this.getAllReportData()
      .pipe(
        catchError((err) => {
          this.logger.error('Error al obtener datos para PDF', err);
          this.toastService.httpError(
            err,
            'Error al obtener los datos del reporte'
          );
          this.isDownloadingPdf = false;
          this.cdr.markForCheck();
          return of({
            items: [],
            metrics: {
              totalInvoiced: 0,
              totalComprobantes: 0,
              averageTicket: 0,
            },
          } as SalesByPeriodResponse);
        })
      )
      .subscribe({
        next: (result: SalesByPeriodResponse) => {
          const allItems = result.items || [];
          const metrics = result.metrics || this.metrics;

          if (allItems.length === 0) {
            this.toastService.warning('No hay datos para exportar');
            this.isDownloadingPdf = false;
            this.cdr.markForCheck();
            return;
          }

          try {
            const doc = new jsPDF('l', 'mm', 'a4'); // landscape, milímetros, tamaño A4

            // Configuración
            const pageWidth = doc.internal.pageSize.getWidth();
            const pageHeight = doc.internal.pageSize.getHeight();
            const margin = 15;
            const contentWidth = pageWidth - margin * 2;
            let yPos = margin;

            // Título
            doc.setFontSize(18);
            doc.setFont('helvetica', 'bold');
            doc.text('Reporte de Ventas por Período', margin, yPos);
            yPos += 10;

            // Información del filtro
            doc.setFontSize(10);
            doc.setFont('helvetica', 'normal');
            const dateFrom = this.formatFormDate(
              this.filterForm.value.dateFrom
            );
            const dateTo = this.formatFormDate(this.filterForm.value.dateTo);
            doc.text(`Período: ${dateFrom} - ${dateTo}`, margin, yPos);
            yPos += 7;

            // Obtener nombre de empresa si está seleccionada
            if (this.filterForm.value.companyId) {
              const company = this.companies.find(
                (c) => c.idCompany === this.filterForm.value.companyId
              );
              if (company) {
                doc.text(`Empresa: ${company.name}`, margin, yPos);
                yPos += 7;
              }
            }

            yPos += 5;

            // Métricas
            doc.setFontSize(12);
            doc.setFont('helvetica', 'bold');
            doc.text('Resumen', margin, yPos);
            yPos += 8;

            doc.setFontSize(10);
            doc.setFont('helvetica', 'normal');
            doc.text(
              `Total Facturado: ${this.formatCurrency(metrics.totalInvoiced)}`,
              margin,
              yPos
            );
            yPos += 6;
            doc.text(
              `Número de Comprobantes: ${metrics.totalComprobantes}`,
              margin,
              yPos
            );
            yPos += 6;
            doc.text(
              `Ticket Promedio: ${this.formatCurrency(metrics.averageTicket)}`,
              margin,
              yPos
            );
            yPos += 10;

            // Tabla
            doc.setFontSize(12);
            doc.setFont('helvetica', 'bold');
            doc.text('Detalle de Facturas', margin, yPos);
            yPos += 8;

            // Encabezados de la tabla
            doc.setFontSize(9);
            doc.setFont('helvetica', 'bold');
            const colWidths = [35, 25, 50, 35, 25, 25, 25, 30];
            const headers = [
              'Factura',
              'Fecha',
              'Cliente',
              'RUC/CI',
              'Subtotal',
              'IVA',
              'Total',
              'Estado',
            ];
            let xPos = margin;

            headers.forEach((header, index) => {
              doc.text(header, xPos, yPos);
              xPos += colWidths[index];
            });

            yPos += 7;
            doc.setLineWidth(0.5);
            doc.line(margin, yPos - 3, pageWidth - margin, yPos - 3);

            // Datos de la tabla
            doc.setFont('helvetica', 'normal');
            const data = allItems; // Usar todos los registros obtenidos
            const rowHeight = 8;
            const maxRowsPerPage = Math.floor(
              (pageHeight - yPos - margin) / rowHeight
            );

            // Calcular totales
            const totalSubtotal = data.reduce(
              (sum, row) => sum + (row.subtotal || 0),
              0
            );
            const totalIva = data.reduce((sum, row) => sum + (row.iva || 0), 0);
            const totalTotal = data.reduce(
              (sum, row) => sum + (row.total || 0),
              0
            );

            data.forEach((row, index) => {
              // Verificar si necesitamos una nueva página
              if (yPos + rowHeight > pageHeight - margin) {
                doc.addPage();
                yPos = margin;
              }

              xPos = margin;
              const rowData = [
                row.invoiceNumber || '-',
                this.formatDate(row.issueDate),
                (row.clientName || '-').substring(0, 25),
                (row.clientIdentification || '-').substring(0, 12),
                this.formatCurrency(row.subtotal),
                this.formatCurrency(row.iva),
                this.formatCurrency(row.total),
                this.getStatusLabel(row.authorizationStatusSri || '').substring(
                  0,
                  15
                ),
              ];

              rowData.forEach((cell, colIndex) => {
                doc.text(String(cell), xPos, yPos);
                xPos += colWidths[colIndex];
              });

              yPos += rowHeight;
            });

            // Agregar totales al final
            if (yPos + 15 > pageHeight - margin) {
              doc.addPage();
              yPos = margin;
            }

            yPos += 5;
            doc.setLineWidth(0.3);
            doc.line(margin, yPos, pageWidth - margin, yPos);
            yPos += 5;

            doc.setFont('helvetica', 'bold');
            // Calcular la posición X para Subtotal (después de Factura, Fecha, Cliente, RUC/CI)
            const subtotalXPos =
              margin +
              colWidths[0] +
              colWidths[1] +
              colWidths[2] +
              colWidths[3];
            doc.text('TOTALES', margin, yPos);
            doc.text(this.formatCurrency(totalSubtotal), subtotalXPos, yPos);
            doc.text(
              this.formatCurrency(totalIva),
              subtotalXPos + colWidths[4],
              yPos
            );
            doc.text(
              this.formatCurrency(totalTotal),
              subtotalXPos + colWidths[4] + colWidths[5],
              yPos
            );

            // Pie de página
            const totalPages = doc.getNumberOfPages();
            for (let i = 1; i <= totalPages; i++) {
              doc.setPage(i);
              doc.setFontSize(8);
              doc.setFont('helvetica', 'italic');
              doc.text(
                `Página ${i} de ${totalPages}`,
                pageWidth - margin - 30,
                pageHeight - 10
              );
              doc.text(
                `Generado el: ${new Date().toLocaleDateString('es-EC')}`,
                margin,
                pageHeight - 10
              );
            }

            // Descargar el PDF
            const dateFromValue = this.filterForm.value.dateFrom;
            const dateToValue = this.filterForm.value.dateTo;
            const dateFromStr = dateFromValue instanceof Date
              ? dateFromValue.toISOString().split('T')[0]
              : dateFromValue || 'desde';
            const dateToStr = dateToValue instanceof Date
              ? dateToValue.toISOString().split('T')[0]
              : dateToValue || 'hasta';
            const fileName = `reporte-ventas-periodo-${dateFromStr}-${dateToStr}.pdf`;
            doc.save(fileName);

            this.isDownloadingPdf = false;
            this.toastService.success('Reporte PDF descargado correctamente');
            this.cdr.markForCheck();
          } catch (error) {
            this.logger.error('Error al generar PDF', error);
            this.toastService.error('Error al generar el PDF');
            this.isDownloadingPdf = false;
            this.cdr.markForCheck();
          }
        },
        error: (err: any) => {
          this.logger.error('Error al generar PDF', err);
          this.isDownloadingPdf = false;
          this.cdr.markForCheck();
        },
      });
  }

  /**
   * Genera y descarga el reporte en formato Excel
   */
  downloadExcel(): void {
    if (this.isDownloadingExcel) {
      return;
    }

    this.isDownloadingExcel = true;
    this.cdr.markForCheck();

    // Obtener todos los registros
    this.getAllReportData()
      .pipe(
        catchError((err) => {
          this.logger.error('Error al obtener datos para Excel', err);
          this.toastService.httpError(
            err,
            'Error al obtener los datos del reporte'
          );
          this.isDownloadingExcel = false;
          this.cdr.markForCheck();
          return of({
            items: [],
            metrics: {
              totalInvoiced: 0,
              totalComprobantes: 0,
              averageTicket: 0,
            },
          } as SalesByPeriodResponse);
        })
      )
      .subscribe({
        next: (result: SalesByPeriodResponse) => {
          const allItems = result.items || [];
          const metrics = result.metrics || this.metrics;

          if (allItems.length === 0) {
            this.toastService.warning('No hay datos para exportar');
            this.isDownloadingExcel = false;
            this.cdr.markForCheck();
            return;
          }

          try {
            // Crear un nuevo workbook
            const wb = XLSX.utils.book_new();

            // Datos del encabezado del reporte
            const headerData = [
              ['Reporte de Ventas por Período'],
              [''],
              [
                `Período: ${this.formatFormDate(
                  this.filterForm.value.dateFrom
                )} - ${this.formatFormDate(this.filterForm.value.dateTo)}`,
              ],
            ];

            // Agregar información de empresa si está seleccionada
            if (this.filterForm.value.companyId) {
              const company = this.companies.find(
                (c) => c.idCompany === this.filterForm.value.companyId
              );
              if (company) {
                headerData.push([`Empresa: ${company.name}`]);
              }
            }

            headerData.push(['']);

            // Datos del resumen
            const summaryData = [
              ['Resumen'],
              ['Total Facturado', this.formatCurrency(metrics.totalInvoiced)],
              ['Número de Comprobantes', metrics.totalComprobantes],
              ['Ticket Promedio', this.formatCurrency(metrics.averageTicket)],
              [''],
            ];

            // Encabezados de la tabla
            const tableHeaders = [
              [
                'Factura',
                'Fecha',
                'Cliente',
                'RUC/CI',
                'Subtotal',
                'IVA',
                'Total',
                'Estado',
              ],
            ];

            // Datos de la tabla
            const tableData = allItems.map((row) => [
              row.invoiceNumber || '-',
              this.formatDate(row.issueDate),
              row.clientName || '-',
              row.clientIdentification || '-',
              row.subtotal || 0,
              row.iva || 0,
              row.total || 0,
              this.getStatusLabel(row.authorizationStatusSri || ''),
            ]);

            // Calcular totales
            const totalSubtotal = allItems.reduce(
              (sum, row) => sum + (row.subtotal || 0),
              0
            );
            const totalIva = allItems.reduce(
              (sum, row) => sum + (row.iva || 0),
              0
            );
            const totalTotal = allItems.reduce(
              (sum, row) => sum + (row.total || 0),
              0
            );

            // Combinar todos los datos
            const allData = [
              ...headerData,
              ...summaryData,
              ['Detalle de Facturas'],
              ...tableHeaders,
              ...tableData,
              [''],
              ['TOTALES', '', '', '', totalSubtotal, totalIva, totalTotal, ''],
            ];

            // Crear worksheet
            const ws = XLSX.utils.aoa_to_sheet(allData);

            // Establecer ancho de columnas
            const colWidths = [
              { wch: 20 }, // Factura
              { wch: 12 }, // Fecha
              { wch: 30 }, // Cliente
              { wch: 15 }, // RUC/CI
              { wch: 12 }, // Subtotal
              { wch: 12 }, // IVA
              { wch: 12 }, // Total
              { wch: 20 }, // Estado
            ];
            ws['!cols'] = colWidths;

            // Agregar el worksheet al workbook
            XLSX.utils.book_append_sheet(wb, ws, 'Ventas por Período');

            // Generar el archivo Excel
            const dateFromValue = this.filterForm.value.dateFrom;
            const dateToValue = this.filterForm.value.dateTo;
            const dateFromStr = dateFromValue instanceof Date
              ? dateFromValue.toISOString().split('T')[0]
              : dateFromValue || 'desde';
            const dateToStr = dateToValue instanceof Date
              ? dateToValue.toISOString().split('T')[0]
              : dateToValue || 'hasta';
            const fileName = `reporte-ventas-periodo-${dateFromStr}-${dateToStr}.xlsx`;
            XLSX.writeFile(wb, fileName);

            this.isDownloadingExcel = false;
            this.toastService.success('Reporte Excel descargado correctamente');
            this.cdr.markForCheck();
          } catch (error) {
            this.logger.error('Error al generar Excel', error);
            this.toastService.error('Error al generar el Excel');
            this.isDownloadingExcel = false;
            this.cdr.markForCheck();
          }
        },
        error: (err: any) => {
          this.logger.error('Error al generar Excel', err);
          this.isDownloadingExcel = false;
          this.cdr.markForCheck();
        },
      });
  }
}
