import { Component, OnInit, OnDestroy, ViewChild, ChangeDetectorRef } from '@angular/core';
import { MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';

import { FormBuilder, FormGroup } from '@angular/forms';
import jsPDF from 'jspdf';
import * as XLSX from 'xlsx';
import { ReportService } from '../../../_service/report/report.service';
import { ReportFilterDto } from '../../../_model/report/report-filter-dto';
import {
  CreditNoteReportDto,
  CreditNoteReportResponse,
} from '../../../_model/report/credit-note-report-dto';
import { CompanyService } from '../../../_service/customer/company.service';
import { EstablishmentService } from '../../../_service/customer/establishment.service';
import { Company } from '../../../_model/customer/company-establishment';
import { LoggerService } from '../../../_service/logger/logger.service';
import { ToastService } from '../../../_service/notification/toast.service';
import { REPORT_PAGE_SIZE_OPTIONS, REPORT_DEFAULT_PAGE_SIZE } from '../../../_constants/report-constants';
import { environment } from '../../../../environments/environment';
import { decodeToken } from '../../../_functions/functions';
import { SUPER_ADMIN_ROLE } from '../../../_constants/constants';
import { catchError, takeUntil } from 'rxjs/operators';
import { Observable, of, Subject } from 'rxjs';
import { UserRoleService } from '../../../_service/auth/user-role.service';

@Component({
  selector: 'app-credit-notes-report',
  templateUrl: './credit-notes.component.html',
  styleUrls: ['./credit-notes.component.css'],
})
export class CreditNotesReportComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  
  filterForm: FormGroup;
  dataSource: MatTableDataSource<CreditNoteReportDto> =
    new MatTableDataSource<CreditNoteReportDto>([]);
  displayedColumns: string[] = [
    'creditNoteNumber',
    'issueDate',
    'originalInvoiceNumber',
    'clientName',
    'clientIdentification',
    'reason',
    'totalAmount',
  ];

  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;

  companies: Company[] = [];
  branches: any[] = [];
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
      // Suscribirse a cambios de ordenamiento
      this.sort.sortChange.pipe(takeUntil(this.destroy$)).subscribe(() => {
        this.pageIndex = 0; // Resetear a primera página al cambiar ordenamiento
        this.search();
      });
    }
  }

  ngOnDestroy(): void {
    // Cancelar todas las suscripciones HTTP pendientes
    this.destroy$.next();
    this.destroy$.complete();
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
    this.companyService
      .findAll()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
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
    this.establishmentService.findByCompany(companyId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
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
    // Limpiar la sucursal seleccionada cuando cambia la compañía
    this.filterForm.patchValue({ branchId: null });
    this.branches = [];
    
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

    // Obtener parámetros de ordenamiento
    let sortField = 'createDate';
    let sortDirection = 'DESC';
    
    if (this.sort && this.sort.active) {
      sortField = this.sort.active;
      sortDirection = this.sort.direction.toUpperCase() || 'DESC';
    }

    this.reportService
      .getCreditNoteReport(filter, this.pageIndex, this.pageSize, sortField, sortDirection)
      .pipe(
        takeUntil(this.destroy$),
        catchError((err: any) => {
          this.logger.error('Error al cargar reporte', err);
          this.toastService.httpError(err);
          return of({
            items: [],
            totalReturned: 0,
            totalCreditNotes: 0,
            pagination: {
              currentPage: 0,
              pageSize: 10,
              totalElements: 0,
              totalPages: 0,
              hasNext: false,
              hasPrevious: false,
              isFirst: true,
              isLast: true,
            },
          } as CreditNoteReportResponse);
        })
      )
      .subscribe({
        next: (result: CreditNoteReportResponse) => {
          this.dataSource.data = result.items || [];
          this.totalElements = result.pagination?.totalElements || 0;
          this.isLoading = false;
          this.cdr.markForCheck();
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
   * Obtiene el valor numérico de totalAmount (puede ser number o objeto)
   */
  private getTotalAmountValue(totalAmount: number | { source: string; parsedValue: number } | undefined): number {
    if (totalAmount === undefined || totalAmount === null) return 0;
    if (typeof totalAmount === 'number') return totalAmount;
    if (typeof totalAmount === 'object' && 'parsedValue' in totalAmount) {
      return totalAmount.parsedValue || 0;
    }
    return 0;
  }

  /**
   * Obtiene todos los registros del reporte (sin paginación)
   */
  private getAllReportData(): Observable<CreditNoteReportResponse> {
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

    let sortField = 'createDate';
    let sortDirection = 'DESC';
    
    if (this.sort && this.sort.active) {
      sortField = this.sort.active;
      sortDirection = this.sort.direction.toUpperCase() || 'DESC';
    }

    return this.reportService.getCreditNoteReport(filter, 0, 10000, sortField, sortDirection);
  }

  downloadPdf(): void {
    if (this.isDownloadingPdf) return;
    this.isDownloadingPdf = true;
    this.cdr.markForCheck();

    this.getAllReportData()
      .pipe(
        takeUntil(this.destroy$),
        catchError((err) => {
          this.logger.error('Error al obtener datos para PDF', err);
          this.toastService.httpError(err, 'Error al obtener los datos del reporte');
          this.isDownloadingPdf = false;
          this.cdr.markForCheck();
          return of({ items: [], totalReturned: 0, totalCreditNotes: 0, pagination: {} as any } as CreditNoteReportResponse);
        })
      )
      .subscribe({
        next: (result: CreditNoteReportResponse) => {
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
            doc.text('Reporte de Notas de Crédito', margin, yPos);
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
            doc.text('Notas de Crédito', margin, yPos);
            yPos += 8;

            doc.setFontSize(9);
            const colWidths = [35, 25, 35, 50, 30, 50, 30];
            const headers = ['Número NC', 'Fecha', 'Factura Orig.', 'Cliente', 'RUC/CI', 'Motivo', 'Total'];
            let xPos = margin;
            headers.forEach((header, index) => {
              doc.text(header, xPos, yPos);
              xPos += colWidths[index];
            });

            yPos += 7;
            doc.setLineWidth(0.5);
            doc.line(margin, yPos - 3, pageWidth - margin, yPos - 3);
            doc.setFont('helvetica', 'normal');

            const totalAmount = allItems.reduce((sum, row) => sum + this.getTotalAmountValue(row.totalAmount), 0);
            const rowHeight = 8;

            allItems.forEach((row) => {
              if (yPos + rowHeight > pageHeight - margin) {
                doc.addPage();
                yPos = margin;
              }
              xPos = margin;
              const rowData = [
                (row.creditNoteNumber || '-').substring(0, 15),
                this.formatDate(row.creationDate),
                (row.originalInvoiceNumber || '-').substring(0, 15),
                (row.clientName || '-').substring(0, 25),
                (row.clientIdentification || '-').substring(0, 12),
                (row.reason || '-').substring(0, 25),
                this.formatCurrency(this.getTotalAmountValue(row.totalAmount)),
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
            doc.text('TOTAL', margin, yPos);
            doc.text(this.formatCurrency(totalAmount), margin + colWidths[0] + colWidths[1] + colWidths[2] + colWidths[3] + colWidths[4] + colWidths[5], yPos);

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
            const fileName = `reporte-notas-credito-${dateFromStr}-${dateToStr}.pdf`;
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
        takeUntil(this.destroy$),
        catchError((err) => {
          this.logger.error('Error al obtener datos para Excel', err);
          this.toastService.httpError(err, 'Error al obtener los datos del reporte');
          this.isDownloadingExcel = false;
          this.cdr.markForCheck();
          return of({ items: [], totalReturned: 0, totalCreditNotes: 0, pagination: {} as any } as CreditNoteReportResponse);
        })
      )
      .subscribe({
        next: (result: CreditNoteReportResponse) => {
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
              ['Reporte de Notas de Crédito'],
              [''],
              [`Período: ${this.formatFormDate(this.filterForm.value.dateFrom)} - ${this.formatFormDate(this.filterForm.value.dateTo)}`],
            ];

            if (this.filterForm.value.companyId) {
              const company = this.companies.find((c) => c.idCompany === this.filterForm.value.companyId);
              if (company) headerData.push([`Empresa: ${company.name}`]);
            }

            headerData.push(['']);
            const tableHeaders = [['Número NC', 'Fecha', 'Factura Original', 'Cliente', 'RUC/CI', 'Motivo', 'Total']];
            const tableData = allItems.map((row) => [
              row.creditNoteNumber || '-',
              this.formatDate(row.creationDate),
              row.originalInvoiceNumber || '-',
              row.clientName || '-',
              row.clientIdentification || '-',
              row.reason || '-',
              this.getTotalAmountValue(row.totalAmount),
            ]);

            const totalAmount = allItems.reduce((sum, row) => sum + this.getTotalAmountValue(row.totalAmount), 0);

            const allData = [...headerData, ['Notas de Crédito'], ...tableHeaders, ...tableData, [''], ['TOTAL', '', '', '', '', '', totalAmount]];
            const ws = XLSX.utils.aoa_to_sheet(allData);
            ws['!cols'] = [{ wch: 20 }, { wch: 12 }, { wch: 20 }, { wch: 30 }, { wch: 15 }, { wch: 40 }, { wch: 15 }];
            XLSX.utils.book_append_sheet(wb, ws, 'Notas de Crédito');

            const dateFromValue = this.filterForm.value.dateFrom;
            const dateToValue = this.filterForm.value.dateTo;
            const dateFromStr = dateFromValue instanceof Date
              ? dateFromValue.toISOString().split('T')[0]
              : dateFromValue || 'desde';
            const dateToStr = dateToValue instanceof Date
              ? dateToValue.toISOString().split('T')[0]
              : dateToValue || 'hasta';
            const fileName = `reporte-notas-credito-${dateFromStr}-${dateToStr}.xlsx`;
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
