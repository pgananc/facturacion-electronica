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
  SalesByClientDto,
  SalesByClientResponse,
} from '../../../_model/report/sales-by-client-dto';
import { CompanyService } from '../../../_service/customer/company.service';
import { EstablishmentService } from '../../../_service/customer/establishment.service';
import { Company } from '../../../_model/customer/company-establishment';
import { LoggerService } from '../../../_service/logger/logger.service';
import { ToastService } from '../../../_service/notification/toast.service';
import { environment } from '../../../../environments/environment';
import { decodeToken } from '../../../_functions/functions';
import { SUPER_ADMIN_ROLE } from '../../../_constants/constants';
import {
  REPORT_PAGE_SIZE_OPTIONS,
  REPORT_DEFAULT_PAGE_SIZE,
} from '../../../_constants/report-constants';
import { catchError } from 'rxjs/operators';
import { Observable, of } from 'rxjs';
import { UserRoleService } from '../../../_service/auth/user-role.service';

@Component({
  selector: 'app-sales-by-client',
  templateUrl: './sales-by-client.component.html',
  styleUrls: ['./sales-by-client.component.css'],
})
export class SalesByClientComponent implements OnInit {
  filterForm: FormGroup;
  dataSource: MatTableDataSource<SalesByClientDto> =
    new MatTableDataSource<SalesByClientDto>([]);
  displayedColumns: string[] = [
    'clientName',
    'clientIdentification',
    'totalSales',
    'totalInvoices',
    'averageTicket',
  ];

  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;

  companies: Company[] = [];
  branches: any[] = [];
  isLoading = false;
  totalElements = 0;
  pageSize = REPORT_DEFAULT_PAGE_SIZE;
  pageIndex = 0;
  pageSizeOptions = REPORT_PAGE_SIZE_OPTIONS;
  isAdmin = false;
  isDownloadingPdf = false;
  isDownloadingExcel = false;

  constructor(
    private fb: FormBuilder,
    private reportService: ReportService,
    private companyService: CompanyService,
    private establishmentService: EstablishmentService,
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
    if (this.paginator) {
      this.dataSource.paginator = this.paginator;
    }
  }

  getFirstDayOfMonth(): Date {
    const date = new Date();
    date.setDate(1);
    date.setHours(0, 0, 0, 0);
    return date;
  }

  clearFilters(): void {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    
    this.filterForm.reset({
      dateFrom: this.getFirstDayOfMonth(),
      dateTo: today,
      companyId: null,
      branchId: null,
    });
    this.search();
  }

  loadCompanies(): void {
    this.companyService.findAll().subscribe({
      next: (data) => {
        this.companies = data.companyDetailsDtos || [];
        const currentCompanyId = this.filterForm.value.companyId;
        
        // Si ya hay una compañía seleccionada, verificar que existe en la lista
        if (currentCompanyId) {
          const companyExists = this.companies.some(
            (c) => c.idCompany === currentCompanyId
          );
          if (companyExists) {
            // Cargar sucursales de la compañía ya seleccionada
            this.loadBranchesForCompany(currentCompanyId);
          } else {
            // Si la compañía seleccionada no existe en la lista, buscar la del localStorage
            const storedCompanyId = localStorage.getItem(environment.ID_COMPANY);
            if (storedCompanyId) {
              const companyId = parseInt(storedCompanyId);
              const foundCompany = this.companies.find(
                (c) => c.idCompany === companyId
              );
              if (foundCompany) {
                this.filterForm.patchValue({ companyId });
                // Cargar sucursales de la compañía encontrada
                this.loadBranchesForCompany(companyId);
              }
            }
          }
        } else if (this.companies.length > 0) {
          // Solo tomar la primera si no hay ninguna seleccionada
          // Intentar primero con la del localStorage
          const storedCompanyId = localStorage.getItem(environment.ID_COMPANY);
          if (storedCompanyId) {
            const companyId = parseInt(storedCompanyId);
            const foundCompany = this.companies.find(
              (c) => c.idCompany === companyId
            );
            if (foundCompany) {
              this.filterForm.patchValue({ companyId });
              // Cargar sucursales de la compañía seleccionada
              this.loadBranchesForCompany(companyId);
              return;
            }
          }
          // Si no se encontró la del localStorage, tomar la primera
          const firstCompanyId = this.companies[0].idCompany;
          this.filterForm.patchValue({
            companyId: firstCompanyId,
          });
          // Cargar sucursales de la primera compañía
          this.loadBranchesForCompany(firstCompanyId);
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
          this.logger.debug(`Cargadas ${this.branches.length} sucursales para la compañía ${companyId}`);
        } else {
          this.branches = [];
          this.logger.debug(`No se encontraron sucursales para la compañía ${companyId}`);
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
    // Limpiar sucursales cuando cambia la empresa
    this.branches = [];
    this.filterForm.patchValue({
      branchId: null,
    });

    if (companyId) {
      this.loadBranchesForCompany(companyId);
    }
  }

  search(): void {
    this.isLoading = true;
    this.cdr.markForCheck();

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
    };

    this.reportService
      .getSalesByClient(filter, this.pageIndex, this.pageSize)
      .pipe(
        catchError((err) => {
          this.logger.error('Error al cargar reporte', err);
          this.toastService.httpError(err, 'Error al cargar el reporte');
          return of({
            items: [],
            totalElements: 0,
            totalPages: 0,
            size: 10,
            number: 0,
          } as SalesByClientResponse);
        })
      )
      .subscribe({
        next: (result: SalesByClientResponse) => {
          this.logger.debug('Respuesta del backend:', result);
          this.logger.debug('Tipo de resultado:', typeof result);
          this.logger.debug('¿Es array?', Array.isArray(result));

          // Verificar si la respuesta es un array directo o un objeto con items/content
          let data: SalesByClientDto[] = [];
          if (Array.isArray(result)) {
            // Si la respuesta es un array directo
            data = result;
            this.totalElements = result.length;
            this.logger.debug(
              'Respuesta es un array directo, elementos:',
              data.length
            );
          } else if (result && result.items) {
            // El backend usa 'items' en lugar de 'content'
            data = result.items || [];
            this.totalElements = result.totalElements || data.length;
            this.logger.debug(
              'Respuesta tiene estructura items, elementos:',
              data.length
            );
          } else if (result && (result as any).content) {
            // Compatibilidad con estructura 'content' si se usa en el futuro
            data = (result as any).content || [];
            this.totalElements = result.totalElements || 0;
            this.logger.debug(
              'Respuesta tiene estructura content, elementos:',
              data.length
            );
          } else {
            // Si la respuesta tiene otra estructura, intentar acceder directamente
            this.logger.warn('Estructura de respuesta inesperada:', result);
            data = [];
            this.totalElements = 0;
          }

          this.logger.debug('Datos a asignar al dataSource:', data);
          // Crear una nueva instancia del array para forzar la detección de cambios
          this.dataSource.data = [...data];
          this.logger.debug(
            'dataSource.data después de asignar:',
            this.dataSource.data
          );
          this.logger.debug(
            'dataSource.data.length:',
            this.dataSource.data.length
          );

          // Forzar actualización del paginador si existe
          if (this.paginator) {
            this.paginator.length = this.totalElements;
          }

          this.isLoading = false;

          if (data.length === 0) {
            this.toastService.info(
              'No se encontraron resultados para los filtros seleccionados'
            );
          } else {
            this.toastService.success(
              `Se encontraron ${data.length} cliente(s)`
            );
          }

          this.cdr.detectChanges();
        },
        error: (err: any) => {
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
   * Obtiene todos los registros del reporte (sin paginación)
   */
  private getAllReportData(): Observable<SalesByClientResponse> {
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
    };

    let sortField = 'totalSales';
    let sortDirection = 'DESC';
    
    if (this.sort && this.sort.active) {
      sortField = this.sort.active;
      sortDirection = this.sort.direction.toUpperCase() || 'DESC';
    }

    return this.reportService.getSalesByClient(filter, 0, 10000, sortField, sortDirection);
  }

  downloadPdf(): void {
    if (this.isDownloadingPdf) return;
    this.isDownloadingPdf = true;
    this.cdr.markForCheck();

    this.getAllReportData()
      .pipe(
        catchError((err) => {
          this.logger.error('Error al obtener datos para PDF', err);
          this.toastService.httpError(err, 'Error al obtener los datos del reporte');
          this.isDownloadingPdf = false;
          this.cdr.markForCheck();
          return of({ items: [] } as SalesByClientResponse);
        })
      )
      .subscribe({
        next: (result: SalesByClientResponse) => {
          const allItems = result.items || [];
          if (allItems.length === 0) {
            this.toastService.warning('No hay datos para exportar');
            this.isDownloadingPdf = false;
            this.cdr.markForCheck();
            return;
          }

          try {
            const doc = new jsPDF('l', 'mm', 'a4');
            const pageWidth = doc.internal.pageSize.getWidth();
            const pageHeight = doc.internal.pageSize.getHeight();
            const margin = 15;
            let yPos = margin;

            doc.setFontSize(18);
            doc.setFont('helvetica', 'bold');
            doc.text('Reporte de Ventas por Cliente', margin, yPos);
            yPos += 10;

            doc.setFontSize(10);
            doc.setFont('helvetica', 'normal');
            doc.text(`Período: ${this.formatFormDate(this.filterForm.value.dateFrom)} - ${this.formatFormDate(this.filterForm.value.dateTo)}`, margin, yPos);
            yPos += 7;

            if (this.filterForm.value.companyId) {
              const company = this.companies.find((c) => c.idCompany === this.filterForm.value.companyId);
              if (company) {
                doc.text(`Empresa: ${company.name}`, margin, yPos);
                yPos += 7;
              }
            }

            yPos += 10;
            doc.setFontSize(12);
            doc.setFont('helvetica', 'bold');
            doc.text('Clientes por Monto', margin, yPos);
            yPos += 8;

            doc.setFontSize(9);
            const colWidths = [70, 45, 35, 35, 35];
            const headers = ['Cliente', 'RUC/CI', 'Total Ventas', 'N° Facturas', 'Ticket Promedio'];
            let xPos = margin;
            headers.forEach((header, index) => {
              doc.text(header, xPos, yPos);
              xPos += colWidths[index];
            });

            yPos += 7;
            doc.setLineWidth(0.5);
            doc.line(margin, yPos - 3, pageWidth - margin, yPos - 3);
            doc.setFont('helvetica', 'normal');

            const totalSales = allItems.reduce((sum, row) => sum + (row.totalSales || 0), 0);
            const totalInvoices = allItems.reduce((sum, row) => sum + (row.totalInvoices || 0), 0);
            const avgTicket = totalInvoices > 0 ? totalSales / totalInvoices : 0;
            const rowHeight = 8;

            allItems.forEach((row) => {
              if (yPos + rowHeight > pageHeight - margin) {
                doc.addPage();
                yPos = margin;
              }
              xPos = margin;
              const rowData = [
                (row.clientName || '-').substring(0, 35),
                (row.clientIdentification || '-').substring(0, 15),
                this.formatCurrency(row.totalSales),
                String(row.totalInvoices || 0),
                this.formatCurrency(row.averageTicket),
              ];
              rowData.forEach((cell, colIndex) => {
                doc.text(String(cell), xPos, yPos);
                xPos += colWidths[colIndex];
              });
              yPos += rowHeight;
            });

            if (yPos + 15 > pageHeight - margin) {
              doc.addPage();
              yPos = margin;
            }
            yPos += 5;
            doc.setLineWidth(0.3);
            doc.line(margin, yPos, pageWidth - margin, yPos);
            yPos += 5;
            doc.setFont('helvetica', 'bold');
            doc.text('TOTALES', margin, yPos);
            doc.text(this.formatCurrency(totalSales), margin + colWidths[0] + colWidths[1], yPos);
            doc.text(String(totalInvoices), margin + colWidths[0] + colWidths[1] + colWidths[2], yPos);
            doc.text(this.formatCurrency(avgTicket), margin + colWidths[0] + colWidths[1] + colWidths[2] + colWidths[3], yPos);

            const totalPages = doc.getNumberOfPages();
            for (let i = 1; i <= totalPages; i++) {
              doc.setPage(i);
              doc.setFontSize(8);
              doc.setFont('helvetica', 'italic');
              doc.text(`Página ${i} de ${totalPages}`, pageWidth - margin - 30, pageHeight - 10);
              doc.text(`Generado el: ${new Date().toLocaleDateString('es-EC')}`, margin, pageHeight - 10);
            }

            const dateFromValue = this.filterForm.value.dateFrom;
            const dateToValue = this.filterForm.value.dateTo;
            const dateFromStr = dateFromValue instanceof Date
              ? dateFromValue.toISOString().split('T')[0]
              : dateFromValue || 'desde';
            const dateToStr = dateToValue instanceof Date
              ? dateToValue.toISOString().split('T')[0]
              : dateToValue || 'hasta';
            const fileName = `reporte-ventas-cliente-${dateFromStr}-${dateToStr}.pdf`;
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

  downloadExcel(): void {
    if (this.isDownloadingExcel) return;
    this.isDownloadingExcel = true;
    this.cdr.markForCheck();

    this.getAllReportData()
      .pipe(
        catchError((err) => {
          this.logger.error('Error al obtener datos para Excel', err);
          this.toastService.httpError(err, 'Error al obtener los datos del reporte');
          this.isDownloadingExcel = false;
          this.cdr.markForCheck();
          return of({ items: [] } as SalesByClientResponse);
        })
      )
      .subscribe({
        next: (result: SalesByClientResponse) => {
          const allItems = result.items || [];
          if (allItems.length === 0) {
            this.toastService.warning('No hay datos para exportar');
            this.isDownloadingExcel = false;
            this.cdr.markForCheck();
            return;
          }

          try {
            const wb = XLSX.utils.book_new();
            const headerData = [
              ['Reporte de Ventas por Cliente'],
              [''],
              [`Período: ${this.formatFormDate(this.filterForm.value.dateFrom)} - ${this.formatFormDate(this.filterForm.value.dateTo)}`],
            ];

            if (this.filterForm.value.companyId) {
              const company = this.companies.find((c) => c.idCompany === this.filterForm.value.companyId);
              if (company) headerData.push([`Empresa: ${company.name}`]);
            }

            headerData.push(['']);
            const tableHeaders = [['Cliente', 'RUC/CI', 'Total Ventas', 'N° Facturas', 'Ticket Promedio']];
            const tableData = allItems.map((row) => [
              row.clientName || '-',
              row.clientIdentification || '-',
              row.totalSales || 0,
              row.totalInvoices || 0,
              row.averageTicket || 0,
            ]);

            const totalSales = allItems.reduce((sum, row) => sum + (row.totalSales || 0), 0);
            const totalInvoices = allItems.reduce((sum, row) => sum + (row.totalInvoices || 0), 0);
            const avgTicket = totalInvoices > 0 ? totalSales / totalInvoices : 0;

            const allData = [...headerData, ['Clientes por Monto'], ...tableHeaders, ...tableData, [''], ['TOTALES', '', totalSales, totalInvoices, avgTicket]];
            const ws = XLSX.utils.aoa_to_sheet(allData);
            ws['!cols'] = [{ wch: 40 }, { wch: 15 }, { wch: 15 }, { wch: 15 }, { wch: 15 }];
            XLSX.utils.book_append_sheet(wb, ws, 'Ventas por Cliente');

            const dateFromValue = this.filterForm.value.dateFrom;
            const dateToValue = this.filterForm.value.dateTo;
            const dateFromStr = dateFromValue instanceof Date
              ? dateFromValue.toISOString().split('T')[0]
              : dateFromValue || 'desde';
            const dateToStr = dateToValue instanceof Date
              ? dateToValue.toISOString().split('T')[0]
              : dateToValue || 'hasta';
            const fileName = `reporte-ventas-cliente-${dateFromStr}-${dateToStr}.xlsx`;
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
