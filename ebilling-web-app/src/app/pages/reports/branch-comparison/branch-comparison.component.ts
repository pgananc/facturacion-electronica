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
  BranchComparisonDto,
  BranchComparisonResponse,
} from '../../../_model/report/branch-comparison-dto';
import { CompanyService } from '../../../_service/customer/company.service';
import { Company } from '../../../_model/customer/company-establishment';
import { LoggerService } from '../../../_service/logger/logger.service';
import { ToastService } from '../../../_service/notification/toast.service';
import { REPORT_PAGE_SIZE_OPTIONS, REPORT_DEFAULT_PAGE_SIZE } from '../../../_constants/report-constants';
import { environment } from '../../../../environments/environment';
import { decodeToken } from '../../../_functions/functions';
import { SUPER_ADMIN_ROLE } from '../../../_constants/constants';
import { catchError } from 'rxjs/operators';
import { Observable, of } from 'rxjs';
import { UserRoleService } from '../../../_service/auth/user-role.service';

@Component({
  selector: 'app-branch-comparison',
  templateUrl: './branch-comparison.component.html',
  styleUrls: ['./branch-comparison.component.css'],
})
export class BranchComparisonComponent implements OnInit {
  filterForm: FormGroup;
  dataSource: MatTableDataSource<BranchComparisonDto> =
    new MatTableDataSource<BranchComparisonDto>([]);
  displayedColumns: string[] = [
    'ranking',
    'establishmentName',
    'issuancePointName',
    'totalInvoiced',
    'totalInvoices',
    'averageTicket',
  ];

  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;

  companies: Company[] = [];
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
      companyId: this.filterForm.value.companyId, // Mantener la empresa seleccionada
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
      },
      error: (err) => {
        this.logger.error('Error al cargar compañías', err);
      },
    });
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
    };

    this.reportService
      .getBranchComparison(filter, this.pageIndex, this.pageSize)
      .pipe(
        catchError((err: any) => {
          this.logger.error('Error al cargar reporte', err);
          this.toastService.httpError(err);
          return of({
            items: [],
          } as BranchComparisonResponse);
        })
      )
      .subscribe({
        next: (result: BranchComparisonResponse) => {
          this.logger.debug('Respuesta del backend:', result);
          
          // El backend devuelve { items: [...] }
          const items = result.items || [];
          
          this.dataSource.data = [...items];
          this.totalElements = items.length; // Si el backend no proporciona paginación, usar length
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

  /**
   * Formatea una fecha del formulario (YYYY-MM-DD) sin conversión de zona horaria
   * Acepta string o Date
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
      return dateStr;
    } catch {
      return '-';
    }
  }

  /**
   * Obtiene todos los registros del reporte (sin paginación)
   */
  private getAllReportData(): Observable<BranchComparisonResponse> {
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
    };

    // Solicitar un tamaño grande para obtener todos los registros
    return this.reportService.getBranchComparison(filter, 0, 10000);
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
        catchError((err: any) => {
          this.logger.error('Error al obtener datos para PDF', err);
          this.toastService.httpError(err, 'Error al obtener los datos del reporte');
          this.isDownloadingPdf = false;
          this.cdr.markForCheck();
          return of({ items: [] } as BranchComparisonResponse);
        })
      )
      .subscribe({
        next: (result: BranchComparisonResponse) => {
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
            doc.text('Reporte Comparativo por Sucursal', margin, yPos);
            yPos += 10;

            doc.setFontSize(10);
            doc.setFont('helvetica', 'normal');
            doc.text(
              `Período: ${this.formatFormDate(this.filterForm.value.dateFrom)} - ${this.formatFormDate(this.filterForm.value.dateTo)}`,
              margin,
              yPos
            );
            yPos += 7;

            if (this.filterForm.value.companyId) {
              const company = this.companies.find(
                (c) => c.idCompany === this.filterForm.value.companyId
              );
              if (company) {
                doc.text(`Empresa: ${company.name}`, margin, yPos);
                yPos += 7;
              }
            }

            yPos += 10;
            doc.setFontSize(12);
            doc.setFont('helvetica', 'bold');
            doc.text('Ranking de Sucursales', margin, yPos);
            yPos += 8;

            doc.setFontSize(9);
            const colWidths = [25, 60, 60, 40, 35, 40];
            const headers = [
              'Ranking',
              'Sucursal',
              'Punto de Emisión',
              'Total Facturado',
              'N° Facturas',
              'Ticket Promedio',
            ];
            let xPos = margin;

            headers.forEach((header, index) => {
              doc.setFont('helvetica', 'bold');
              doc.text(header, xPos, yPos);
              xPos += colWidths[index];
            });

            yPos += 7;
            doc.setLineWidth(0.5);
            doc.line(margin, yPos - 3, pageWidth - margin, yPos - 3);
            doc.setFont('helvetica', 'normal');

            const rowHeight = 8;

            // Calcular totales
            let totalInvoiced = 0;
            let totalInvoices = 0;
            let totalAverageTicket = 0;

            allItems.forEach((row) => {
              if (yPos + rowHeight > pageHeight - margin) {
                doc.addPage();
                yPos = margin;
              }
              xPos = margin;
              const rowData = [
                String(row.ranking || '-'),
                (row.establishmentName || '-').substring(0, 30),
                (row.issuancePointName || '-').substring(0, 30),
                this.formatCurrency(row.totalInvoiced || 0),
                String(row.totalInvoices || 0),
                this.formatCurrency(row.averageTicket || 0),
              ];
              rowData.forEach((cell, colIndex) => {
                doc.text(String(cell), xPos, yPos);
                xPos += colWidths[colIndex];
              });
              yPos += rowHeight;

              // Acumular totales
              totalInvoiced += row.totalInvoiced || 0;
              totalInvoices += row.totalInvoices || 0;
              totalAverageTicket += row.averageTicket || 0;
            });

            // Calcular promedio del ticket promedio
            const avgTicket = allItems.length > 0 ? totalAverageTicket / allItems.length : 0;

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
            // Calcular la posición X para los totales (después de Ranking, Sucursal, Punto de Emisión)
            const totalXPos = margin + colWidths[0] + colWidths[1] + colWidths[2];
            doc.text('TOTALES', margin, yPos);
            doc.text(this.formatCurrency(totalInvoiced), totalXPos, yPos);
            doc.text(String(totalInvoices), totalXPos + colWidths[3], yPos);
            doc.text(this.formatCurrency(avgTicket), totalXPos + colWidths[3] + colWidths[4], yPos);

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

            const dateFromValue = this.filterForm.value.dateFrom;
            const dateToValue = this.filterForm.value.dateTo;
            const dateFromStr = dateFromValue instanceof Date
              ? dateFromValue.toISOString().split('T')[0]
              : dateFromValue || 'desde';
            const dateToStr = dateToValue instanceof Date
              ? dateToValue.toISOString().split('T')[0]
              : dateToValue || 'hasta';
            const fileName = `reporte-comparativo-sucursales-${dateFromStr}-${dateToStr}.pdf`;
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
        catchError((err: any) => {
          this.logger.error('Error al obtener datos para Excel', err);
          this.toastService.httpError(err, 'Error al obtener los datos del reporte');
          this.isDownloadingExcel = false;
          this.cdr.markForCheck();
          return of({ items: [] } as BranchComparisonResponse);
        })
      )
      .subscribe({
        next: (result: BranchComparisonResponse) => {
          const allItems = result.items || [];
          if (allItems.length === 0) {
            this.toastService.warning('No hay datos para exportar');
            this.isDownloadingExcel = false;
            this.cdr.markForCheck();
            return;
          }

          try {
            const wb = XLSX.utils.book_new();
            const headerData = [['Reporte Comparativo por Sucursal'], [''], [`Período: ${this.formatFormDate(this.filterForm.value.dateFrom)} - ${this.formatFormDate(this.filterForm.value.dateTo)}`]];

            if (this.filterForm.value.companyId) {
              const company = this.companies.find(
                (c) => c.idCompany === this.filterForm.value.companyId
              );
              if (company) headerData.push([`Empresa: ${company.name}`]);
            }

            headerData.push(['']);
            const tableHeaders = [
              ['Ranking', 'Sucursal', 'Punto de Emisión', 'Total Facturado', 'N° Facturas', 'Ticket Promedio'],
            ];
            
            // Calcular totales
            let totalInvoiced = 0;
            let totalInvoices = 0;
            let totalAverageTicket = 0;

            const tableData = allItems.map((row) => {
              totalInvoiced += row.totalInvoiced || 0;
              totalInvoices += row.totalInvoices || 0;
              totalAverageTicket += row.averageTicket || 0;
              return [
                row.ranking || '-',
                row.establishmentName || '-',
                row.issuancePointName || '-',
                row.totalInvoiced || 0,
                row.totalInvoices || 0,
                row.averageTicket || 0,
              ];
            });

            // Calcular promedio del ticket promedio
            const avgTicket = allItems.length > 0 ? totalAverageTicket / allItems.length : 0;

            // Agregar fila de totales
            const totalRow = ['TOTALES', '', '', totalInvoiced, totalInvoices, avgTicket];

            const allData = [...headerData, ['Ranking de Sucursales'], ...tableHeaders, ...tableData, [], totalRow];
            const ws = XLSX.utils.aoa_to_sheet(allData);
            ws['!cols'] = [
              { wch: 10 },
              { wch: 30 },
              { wch: 30 },
              { wch: 20 },
              { wch: 15 },
              { wch: 20 },
            ];
            XLSX.utils.book_append_sheet(wb, ws, 'Comparativo Sucursales');

            const dateFromValue = this.filterForm.value.dateFrom;
            const dateToValue = this.filterForm.value.dateTo;
            const dateFromStr = dateFromValue instanceof Date
              ? dateFromValue.toISOString().split('T')[0]
              : dateFromValue || 'desde';
            const dateToStr = dateToValue instanceof Date
              ? dateToValue.toISOString().split('T')[0]
              : dateToValue || 'hasta';
            const fileName = `reporte-comparativo-sucursales-${dateFromStr}-${dateToStr}.xlsx`;
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
