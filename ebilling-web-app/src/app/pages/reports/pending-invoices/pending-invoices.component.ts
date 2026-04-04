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
  PendingInvoiceDto,
  PendingInvoiceResponse,
} from '../../../_model/report/pending-invoices-dto';
import { CompanyService } from '../../../_service/customer/company.service';
import { EstablishmentService } from '../../../_service/customer/establishment.service';
import { Company } from '../../../_model/customer/company-establishment';
import { LoggerService } from '../../../_service/logger/logger.service';
import { ToastService } from '../../../_service/notification/toast.service';
import {
  REPORT_PAGE_SIZE_OPTIONS,
  REPORT_DEFAULT_PAGE_SIZE,
  PendingInvoiceStatus,
  PENDING_INVOICE_STATUS_LABELS,
  PENDING_INVOICE_STATUS_CODES,
} from '../../../_constants/report-constants';
import { environment } from '../../../../environments/environment';
import { decodeToken } from '../../../_functions/functions';
import { SUPER_ADMIN_ROLE } from '../../../_constants/constants';
import { catchError } from 'rxjs/operators';
import { Observable, of } from 'rxjs';
import { UserRoleService } from '../../../_service/auth/user-role.service';

@Component({
  selector: 'app-pending-invoices',
  templateUrl: './pending-invoices.component.html',
  styleUrls: ['./pending-invoices.component.css'],
})
export class PendingInvoicesComponent implements OnInit {
  filterForm: FormGroup;
  dataSource: MatTableDataSource<PendingInvoiceDto> =
    new MatTableDataSource<PendingInvoiceDto>([]);
  displayedColumns: string[] = [
    'invoiceNumber',
    'issueDate',
    'clientName',
    'receptionStatusSri',
    'authorizationStatusSri',
    'receptionError',
    'authorizationError',
    'retryCount',
  ];

  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;

  companies: Company[] = [];
  branches: any[] = [];
  statuses = Object.values(PendingInvoiceStatus);
  statusLabels = PENDING_INVOICE_STATUS_LABELS;
  isLoading = false;
  totalElements = 0;
  pageSize = REPORT_DEFAULT_PAGE_SIZE;
  pageSizeOptions = REPORT_PAGE_SIZE_OPTIONS;
  pageIndex = 0;
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
      statusFilter: [null],
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
      statusFilter: null,
    });
    this.search();
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
            const storedCompanyId = localStorage.getItem(environment.ID_COMPANY);
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

    // Convertir el estado seleccionado al código que espera el backend
    const selectedStatus = this.filterForm.value.statusFilter;
    const statusCode = selectedStatus
      ? PENDING_INVOICE_STATUS_CODES[selectedStatus as PendingInvoiceStatus]
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
      status: statusCode,
    };

    this.reportService
      .getPendingInvoices(filter, this.pageIndex, this.pageSize)
      .pipe(
        catchError((err: any) => {
          this.logger.error('Error al cargar reporte', err);
          this.toastService.httpError(err);
          return of({
            items: [],
            totalPending: 0,
          } as PendingInvoiceResponse);
        })
      )
      .subscribe({
        next: (result: PendingInvoiceResponse) => {
          this.logger.debug('Respuesta del backend:', result);
          
          // El backend devuelve { items: [...], totalPending: number }
          const items = result.items || [];
          
          this.dataSource.data = [...items];
          this.totalElements = result.totalPending || items.length;
          this.isLoading = false;
          this.cdr.detectChanges();
        },
        error: (err: any) => {
          this.logger.error('Error en la búsqueda', err);
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
   * ✅ Retorna el label amigable del estado
   * @param status Estado de factura pendiente
   * @returns Label traducido
   */
  getStatusLabel(status: string): string {
    const statusEnum = status as PendingInvoiceStatus;
    return PENDING_INVOICE_STATUS_LABELS[statusEnum] || status;
  }

  getStatusColor(status: string): string {
    switch (status?.toUpperCase()) {
      case 'PENDIENTE':
        return 'blue';
      case 'RECHAZADA':
      case 'RECHAZADO':
        return 'red';
      case 'CADUCADA':
        return 'gray';
      default:
        return 'black';
    }
  }

  /**
   * Obtiene todos los registros del reporte (sin paginación)
   */
  private getAllReportData(): Observable<PendingInvoiceResponse> {
    // Convertir el estado seleccionado al código que espera el backend
    const selectedStatus = this.filterForm.value.statusFilter;
    const statusCode = selectedStatus
      ? PENDING_INVOICE_STATUS_CODES[selectedStatus as PendingInvoiceStatus]
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
      status: statusCode,
    };

    // El backend de facturas pendientes no usa paginación, devuelve todos los resultados
    return this.reportService.getPendingInvoices(filter, 0, 10000);
  }

  /**
   * Genera y descarga el reporte en formato PDF
   */
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
          return of({ items: [], totalPending: 0 } as PendingInvoiceResponse);
        })
      )
      .subscribe({
        next: (result: PendingInvoiceResponse) => {
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
            doc.text('Reporte de Facturas Pendientes de Envío', margin, yPos);
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
            doc.text('Facturas Pendientes', margin, yPos);
            yPos += 8;

            doc.setFontSize(9);
            // Ajustar anchos: reducir algunas columnas para dar más espacio a "Reintentos"
            const colWidths = [32, 22, 45, 28, 28, 45, 45, 25];
            // Headers con texto cortado para que no se superpongan
            const headers = [
              'Número\nFactura',
              'Fecha',
              'Cliente',
              'Estado\nRecepción',
              'Estado\nAutorización',
              'Error\nRecepción',
              'Error\nAutorización',
              'Reintentos'
            ];
            let xPos = margin;
            
            // Dibujar headers con soporte para múltiples líneas
            headers.forEach((header, index) => {
              const lines = header.split('\n');
              let currentY = yPos;
              lines.forEach((line) => {
                doc.text(line, xPos, currentY);
                currentY += 4; // Espacio entre líneas
              });
              xPos += colWidths[index];
            });

            yPos += 10; // Aumentar espacio para headers de múltiples líneas
            doc.setLineWidth(0.5);
            doc.line(margin, yPos - 3, pageWidth - margin, yPos - 3);
            doc.setFont('helvetica', 'normal');

            const rowHeight = 8;

            allItems.forEach((row) => {
              if (yPos + rowHeight > pageHeight - margin) {
                doc.addPage();
                yPos = margin;
              }
              xPos = margin;
              const rowData = [
                (row.invoiceNumber || '-').substring(0, 14),
                this.formatDate(row.issueDate),
                (row.clientName || '-').substring(0, 22),
                (row.receptionStatusSri || '-').substring(0, 12),
                this.getStatusLabel(row.authorizationStatusSri || '').substring(0, 12),
                (row.receptionError || '-').substring(0, 22),
                (row.authorizationError || '-').substring(0, 22),
                String(row.retryCount || 0),
              ];
              rowData.forEach((cell, colIndex) => {
                doc.text(String(cell), xPos, yPos);
                xPos += colWidths[colIndex];
              });
              yPos += rowHeight;
            });

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
            const fileName = `reporte-facturas-pendientes-${dateFromStr}-${dateToStr}.pdf`;
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
          return of({ items: [], totalPending: 0 } as PendingInvoiceResponse);
        })
      )
      .subscribe({
        next: (result: PendingInvoiceResponse) => {
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
              ['Reporte de Facturas Pendientes de Envío'],
              [''],
              [`Período: ${this.formatFormDate(this.filterForm.value.dateFrom)} - ${this.formatFormDate(this.filterForm.value.dateTo)}`],
            ];

            if (this.filterForm.value.companyId) {
              const company = this.companies.find((c) => c.idCompany === this.filterForm.value.companyId);
              if (company) headerData.push([`Empresa: ${company.name}`]);
            }

            headerData.push(['']);
            const tableHeaders = [['Número Factura', 'Fecha', 'Cliente', 'Estado Recepción', 'Estado Autorización', 'Error Recepción', 'Error Autorización', 'Reintentos']];
            const tableData = allItems.map((row) => [
              row.invoiceNumber || '-',
              this.formatDate(row.issueDate),
              row.clientName || '-',
              row.receptionStatusSri || '-',
              this.getStatusLabel(row.authorizationStatusSri || ''),
              row.receptionError || '-',
              row.authorizationError || '-',
              row.retryCount || 0,
            ]);

            const allData = [...headerData, ['Facturas Pendientes'], ...tableHeaders, ...tableData];
            const ws = XLSX.utils.aoa_to_sheet(allData);
            ws['!cols'] = [{ wch: 20 }, { wch: 12 }, { wch: 30 }, { wch: 20 }, { wch: 20 }, { wch: 40 }, { wch: 40 }, { wch: 12 }];
            XLSX.utils.book_append_sheet(wb, ws, 'Facturas Pendientes');

            const dateFromValue = this.filterForm.value.dateFrom;
            const dateToValue = this.filterForm.value.dateTo;
            const dateFromStr = dateFromValue instanceof Date
              ? dateFromValue.toISOString().split('T')[0]
              : dateFromValue || 'desde';
            const dateToStr = dateToValue instanceof Date
              ? dateToValue.toISOString().split('T')[0]
              : dateToValue || 'hasta';
            const fileName = `reporte-facturas-pendientes-${dateFromStr}-${dateToStr}.xlsx`;
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
