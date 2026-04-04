import {
  Component,
  OnInit,
  OnDestroy,
  AfterViewInit,
  ViewChild,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Injectable,
} from '@angular/core';
import { MatPaginator, MatPaginatorIntl } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { ToastService } from '../../../_service/notification/toast.service';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil, catchError } from 'rxjs/operators';
import { of } from 'rxjs';
import { InvoiceService } from '../../../_service/invoice/invoice.service';
import { InvoiceListResponseDto } from '../../../_model/invoice/invoice-list-response-dto';
import { InvoiceSearchByCompanyAndDateDto } from '../../../_model/invoice/invoice-search-by-company-dto';
import { LoggerService } from '../../../_service/logger/logger.service';
import { Router } from '@angular/router';
import { environment } from '../../../../environments/environment';
import { DraftInvoiceService } from '../../../_service/invoice/draft-invoice.service';
import { MatDialog } from '@angular/material/dialog';
import { ConfirmDialogComponent } from '../../../_shared/confirm-dialog/confirm-dialog.component';

/**
 * Custom Paginator Intl para personalizar el texto
 */
@Injectable()
export class InvoiceListPaginatorIntl extends MatPaginatorIntl {
  override itemsPerPageLabel = 'Items por página:';
  override nextPageLabel = 'Siguiente';
  override previousPageLabel = 'Anterior';
  override firstPageLabel = 'Primera página';
  override lastPageLabel = 'Última página';

  override getRangeLabel = (
    page: number,
    pageSize: number,
    length: number
  ): string => {
    if (length === 0 || pageSize === 0) {
      return `0 de ${length} facturas`;
    }
    const startIndex = page * pageSize;
    const endIndex =
      startIndex < length
        ? Math.min(startIndex + pageSize, length)
        : startIndex + pageSize;
    return `Mostrando ${
      startIndex + 1
    }-${endIndex} de ${length} facturas`;
  };
}

/**
 * InvoiceListComponent - Lista y gestión de facturas
 *
 * Optimizado con OnPush Change Detection para mejor performance
 */
@Component({
  selector: 'app-invoice-list',
  templateUrl: './invoice-list.component.html',
  styleUrls: ['./invoice-list.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [{ provide: MatPaginatorIntl, useClass: InvoiceListPaginatorIntl }],
})
export class InvoiceListComponent implements OnInit, AfterViewInit, OnDestroy {
  searchForm: FormGroup;
  displayedColumns = [
    'invoiceNumber',
    'clientIdentification',
    'clientName',
    'totalAmount',
    'status',
    'issueDate',
    'actions',
  ];
  dataSource = new MatTableDataSource<InvoiceListResponseDto>([]);
  pageSize = 10;
  totalElements = 0;
  currentPage = 0;
  isLoading = false;

  private destroy$ = new Subject<void>();

  @ViewChild(MatPaginator, { static: true }) paginator: MatPaginator;
  @ViewChild(MatSort, { static: true }) sort: MatSort;

  constructor(
    private readonly invoiceService: InvoiceService,
    private readonly draftInvoiceService: DraftInvoiceService,
    private readonly toast: ToastService,
    private readonly cdr: ChangeDetectorRef,
    private readonly fb: FormBuilder,
    private readonly logger: LoggerService,
    private readonly router: Router,
    private readonly dialog: MatDialog
  ) {
    this.initForm();
  }

  ngOnInit(): void {
    this.logger.debug('InvoiceListComponent initialized');

    // Initial load
    this.search();
  }

  ngAfterViewInit(): void {
    // Configurar ordenamiento local
    this.dataSource.sort = this.sort;
    
    // Configurar paginación del servidor (sin asignar al dataSource.paginator)
    // Escuchar eventos de paginación
    if (this.paginator) {
      this.paginator.page
        .pipe(takeUntil(this.destroy$))
        .subscribe((pageEvent) => {
          this.currentPage = pageEvent.pageIndex;
          this.pageSize = pageEvent.pageSize;
          this.search();
        });
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initForm(): void {
    // Establecer fecha actual por defecto
    const today = new Date();
    
    this.searchForm = this.fb.group({
      numeroFactura: [''],
      numeroAutorizacion: [''],
      identificacionCliente: [''],
      creationDateFrom: [today, [Validators.required]], // Obligatorio con fecha actual por defecto
      creationDateTo: [today, [Validators.required]], // Obligatorio con fecha actual por defecto
      receptionStatusSri: [''],
      authorizationStatusSri: [''],
    });
  }

  /**
   * Busca facturas con filtros usando el endpoint search
   */
  search(): void {
    // Obtener idCompany desde localStorage
    const idCompanyStr = localStorage.getItem(environment.ID_COMPANY);
    if (!idCompanyStr) {
      this.logger.error('No se encontró ID de compañía en localStorage');
      this.toast.error(
        'No se encontró información de la compañía. Por favor, inicie sesión nuevamente.'
      );
      return;
    }

    const idCompany = Number(idCompanyStr);
    if (!idCompany || isNaN(idCompany)) {
      this.logger.error('ID de compañía inválido:', idCompanyStr);
      this.toast.error(
        'ID de compañía inválido. Por favor, inicie sesión nuevamente.'
      );
      return;
    }

    this.isLoading = true;
    this.cdr.markForCheck();

    // Construir el DTO de búsqueda
    const formValue = this.searchForm.value;
    const searchDto: InvoiceSearchByCompanyAndDateDto = {
      idCompany: idCompany,
      numeroFactura: formValue.numeroFactura?.trim() || undefined,
      numeroAutorizacion: formValue.numeroAutorizacion?.trim() || undefined,
      identificacionCliente: formValue.identificacionCliente?.trim() || undefined,
      // Las fechas son obligatorias
      creationDateFrom: this.formatDateForApi(formValue.creationDateFrom)!,
      creationDateTo: this.formatDateForApi(formValue.creationDateTo)!,
      receptionStatusSri: formValue.receptionStatusSri?.trim() || undefined,
      authorizationStatusSri: formValue.authorizationStatusSri?.trim() || undefined,
    };

    this.invoiceService
      .search(searchDto, this.currentPage, this.pageSize)
      .pipe(
        takeUntil(this.destroy$),
        catchError((error) => {
          this.logger.error('Error searching invoices', error);
          this.isLoading = false;
          this.dataSource.data = [];
          this.totalElements = 0;
          this.cdr.markForCheck();
          this.toast.error('Error al buscar facturas. Intente nuevamente.');
          return of({ content: [], totalElements: 0, totalPages: 0, number: 0, size: this.pageSize });
        })
      )
      .subscribe((response) => {
        this.isLoading = false;

        this.logger.debug('Response from search:', response);

        if (response && response.content && Array.isArray(response.content)) {
          this.dataSource.data = response.content;
          this.totalElements = response.totalElements || 0;
          
          // Actualizar el paginador si está disponible
          if (this.paginator) {
            this.paginator.length = this.totalElements;
            this.paginator.pageIndex = response.number || 0;
          }
        } else {
          this.dataSource.data = [];
          this.totalElements = 0;
        }

        this.cdr.markForCheck();
      });
  }

  /**
   * Limpia los filtros de búsqueda
   */
  clearFilters(): void {
    // Establecer fecha actual por defecto
    const today = new Date();
    
    this.searchForm.reset({
      numeroFactura: '',
      numeroAutorizacion: '',
      identificacionCliente: '',
      creationDateFrom: today,
      creationDateTo: today,
      receptionStatusSri: '',
      authorizationStatusSri: '',
    });
    // Resetear a la primera página
    this.currentPage = 0;
    if (this.paginator) {
      this.paginator.pageIndex = 0;
    }
    this.cdr.markForCheck();
    this.search();
  }

  /**
   * Navega a la pantalla de creación de factura
   */
  openCreateInvoice(): void {
    // Limpiar cualquier draftId del localStorage para asegurar que es una factura nueva
    localStorage.removeItem('CURRENT_DRAFT_ID');
    this.router.navigate(['/invoices/create'], {
      queryParams: {}, // Asegurar que no hay query params
      replaceUrl: true, // Reemplazar la URL para limpiar cualquier query param anterior
    });
  }

  /**
   * Descarga el PDF de una factura
   */
  downloadPdf(idInvoice: number, invoiceNumber: string): void {
    this.invoiceService.downloadPdf(idInvoice).subscribe({
      next: (blob: Blob) => {
        if (blob.type === 'application/json' || blob.size < 100) {
          blob.text().then((text) => {
            try {
              const error = JSON.parse(text);
              this.toast.error(
                error.message || 'Error al descargar el PDF'
              );
            } catch (e) {
              this.toast.error(
                'Error al descargar el PDF. Intente nuevamente.'
              );
            }
          });
          return;
        }

        const downloadUrl = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = downloadUrl;
        const cleanFileName = invoiceNumber.replace(/[^a-zA-Z0-9-]/g, '_');
        link.download = `${cleanFileName}.pdf`;
        link.style.display = 'none';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(downloadUrl);
        this.toast.success('PDF descargado exitosamente');
      },
      error: async (error) => {
        this.logger.error('Error downloading PDF', error);
        const errorMessage =
          error.error?.message ||
          'Error al descargar el PDF. Intente nuevamente.';
        this.toast.error(errorMessage);
      },
    });
  }

  /**
   * Descarga el XML de una factura
   */
  downloadXml(idInvoice: number, invoiceNumber: string): void {
    this.invoiceService.downloadXml(idInvoice).subscribe({
      next: (blob: Blob) => {
        if (blob.type === 'application/json' || blob.size < 100) {
          blob.text().then((text) => {
            try {
              const error = JSON.parse(text);
              this.toast.error(
                error.message || 'Error al descargar el XML'
              );
            } catch (e) {
              this.toast.error(
                'Error al descargar el XML. Intente nuevamente.'
              );
            }
          });
          return;
        }

        const downloadUrl = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = downloadUrl;
        const cleanFileName = invoiceNumber.replace(/[^a-zA-Z0-9-]/g, '_');
        link.download = `${cleanFileName}.xml`;
        link.style.display = 'none';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(downloadUrl);
        this.toast.success('XML descargado exitosamente');
      },
      error: async (error) => {
        this.logger.error('Error downloading XML', error);
        const errorMessage =
          error.error?.message ||
          'Error al descargar el XML. Intente nuevamente.';
        this.toast.error(errorMessage);
      },
    });
  }

  /**
   * Imprime el recibo de la factura
   */
  printTicket(idInvoice: number): void {
    this.logger.debug('🖨️ Imprimiendo recibo de factura:', idInvoice);
    
    // Abrir ventana de impresión del recibo
    const relativeUrl = `/invoices/ticket/${idInvoice}`;
    const fullUrl = `${window.location.origin}${relativeUrl}`;
    
    try {
      const printWindow = window.open(
        relativeUrl,
        '_blank',
        'width=400,height=600'
      );

      if (
        !printWindow ||
        printWindow.closed ||
        typeof printWindow.closed === 'undefined'
      ) {
        // Si window.open falla (bloqueador de popups)
        this.logger.warn(
          '⚠️ window.open falló (posible bloqueador de popups), intentando con URL completa...'
        );

        // Intentar una segunda vez con URL completa
        const printWindow2 = window.open(
          fullUrl,
          '_blank',
          'width=400,height=600'
        );

        if (
          !printWindow2 ||
          printWindow2.closed ||
          typeof printWindow2.closed === 'undefined'
        ) {
          this.logger.error(
            '❌ No se pudo abrir ventana de impresión después de múltiples intentos'
          );
          this.toast.error(
            'No se pudo abrir la ventana de impresión. Verifique el bloqueador de ventanas emergentes.'
          );
        } else {
          this.logger.debug('✅ Ventana de impresión abierta exitosamente (segundo intento)');
        }
      } else {
        this.logger.debug('✅ Ventana de impresión abierta exitosamente');
      }
    } catch (error) {
      this.logger.error('❌ Error al abrir ventana de impresión', error);
      this.toast.error(
        'Error al abrir la ventana de impresión. Intente nuevamente.'
      );
    }
  }

  // ========== HELPERS ==========

  /**
   * Formatea moneda
   */
  formatCurrency(value: number | undefined): string {
    if (!value) return '$0.00';
    return new Intl.NumberFormat('es-EC', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(value);
  }

  /**
   * Formatea fecha
   */
  formatDate(dateString?: string): string {
    if (!dateString) {
      return '-';
    }
    try {
      const date = new Date(dateString);
      return date.toLocaleDateString('es-EC', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
      });
    } catch (error) {
      return dateString;
    }
  }

  /**
   * Formatea fecha para el API (ISO 8601)
   */
  private formatDateForApi(date: Date | string | null): string | undefined {
    if (!date) {
      return undefined;
    }
    if (typeof date === 'string') {
      return date;
    }
    // Si es un objeto Date de Angular Material
    if (date instanceof Date) {
      return date.toISOString().split('T')[0];
    }
    return undefined;
  }

  /**
   * Obtiene el texto de estado legible
   */
  getStatusText(invoice: InvoiceListResponseDto): string {
    // Priorizar receptionStatusSri para detectar DEVUELTA (puede estar en el objeto raíz o en authorizationSri)
    const receptionStatus = invoice.receptionStatusSri || 
                           invoice.authorizationSri?.receptionStatusSri;
    
    if (receptionStatus && typeof receptionStatus === 'string') {
      const upperReceptionStatus = receptionStatus.toUpperCase();
      if (upperReceptionStatus === 'DEVUELTA' || upperReceptionStatus === 'RETURNED') {
        return 'Devuelta';
      }
    }

    // Intentar obtener el estado desde el campo directo o desde el objeto authorizationSri
    const statusStr = invoice.authorizationStatusSri || 
                      invoice.authorizationSri?.authorizationStatusSri;
    
    if (!statusStr) {
      return 'Borrador';
    }

    const status = statusStr.toUpperCase();
    switch (status) {
      case 'AUTORIZADO':
      case 'AUTHORIZED':
        return 'Autorizado';
      case 'RECHAZADO':
      case 'REJECTED':
        return 'Rechazado';
      case 'DEVUELTA':
      case 'RETURNED':
        return 'Devuelta';
      case 'PENDIENTE':
      case 'PENDING':
        return 'Pendiente';
      case 'EN_PROCESO':
      case 'EN PROCESO':
        return 'En Proceso';
      default:
        return status;
    }
  }

  /**
   * Obtiene la clase CSS del estado
   */
  getStatusClass(invoice: InvoiceListResponseDto): string {
    // Priorizar receptionStatusSri para detectar DEVUELTA (puede estar en el objeto raíz o en authorizationSri)
    const receptionStatus = invoice.receptionStatusSri || 
                           invoice.authorizationSri?.receptionStatusSri;
    
    if (receptionStatus && typeof receptionStatus === 'string') {
      const upperReceptionStatus = receptionStatus.toUpperCase();
      if (upperReceptionStatus === 'DEVUELTA' || upperReceptionStatus === 'RETURNED') {
        return 'badge-danger';
      }
    }

    // Intentar obtener el estado desde el campo directo o desde el objeto authorizationSri
    const statusStr = invoice.authorizationStatusSri || 
                      invoice.authorizationSri?.authorizationStatusSri;
    
    if (!statusStr) {
      return 'badge-warning';
    }

    const status = statusStr.toUpperCase();
    switch (status) {
      case 'AUTORIZADO':
      case 'AUTHORIZED':
        return 'badge-success';
      case 'RECHAZADO':
      case 'REJECTED':
      case 'DEVUELTA':
        return 'badge-danger';
      case 'PENDIENTE':
      case 'PENDING':
        return 'badge-info';
      case 'EN_PROCESO':
      case 'EN PROCESO':
        return 'badge-warning';
      default:
        return 'badge-secondary';
    }
  }

  /**
   * Verifica si una factura está autorizada
   */
  isAuthorized(invoice: InvoiceListResponseDto): boolean {
    // Si está devuelta, no está autorizada
    if (this.isReturned(invoice)) {
      return false;
    }

    // Intentar obtener el estado desde el campo directo o desde el objeto authorizationSri
    const statusStr = invoice.authorizationStatusSri || 
                      invoice.authorizationSri?.authorizationStatusSri;
    
    if (!statusStr) {
      return false;
    }
    
    const upperStatus = statusStr.toUpperCase();
    return upperStatus === 'AUTORIZADO' || upperStatus === 'AUTHORIZED';
  }

  /**
   * Verifica si una factura está devuelta
   */
  isReturned(invoice: InvoiceListResponseDto): boolean {
    // Priorizar receptionStatusSri para detectar DEVUELTA (puede estar en el objeto raíz o en authorizationSri)
    const receptionStatus = invoice.receptionStatusSri || 
                           invoice.authorizationSri?.receptionStatusSri;
    
    if (receptionStatus && typeof receptionStatus === 'string') {
      const upperReceptionStatus = receptionStatus.toUpperCase();
      if (upperReceptionStatus === 'DEVUELTA' || upperReceptionStatus === 'RETURNED') {
        return true;
      }
    }

    // También verificar en authorizationStatusSri por si acaso
    const statusStr = invoice.authorizationStatusSri || 
                      invoice.authorizationSri?.authorizationStatusSri;
    
    if (statusStr) {
      const upperStatus = statusStr.toUpperCase();
      if (upperStatus === 'DEVUELTA' || upperStatus === 'RETURNED') {
        return true;
      }
    }
    
    return false;
  }

  /**
   * Verifica si una factura es un borrador
   */
  isDraft(invoice: InvoiceListResponseDto): boolean {
    // Si está devuelta, NO es borrador
    if (this.isReturned(invoice)) {
      return false;
    }

    // Una factura es borrador si no tiene authorizationSri o si el estado es "BORRADOR"
    const statusStr = invoice.authorizationStatusSri || 
                      invoice.authorizationSri?.authorizationStatusSri;
    
    if (!statusStr) {
      return true; // Sin estado = borrador
    }
    
    const upperStatus = statusStr.toUpperCase();
    return upperStatus === 'BORRADOR' || upperStatus === 'DRAFT';
  }

  /**
   * Edita un borrador de factura
   */
  editDraft(invoice: InvoiceListResponseDto): void {
    if (!invoice.idInvoice) {
      this.toast.error('ID de factura inválido');
      return;
    }

    if (!this.isDraft(invoice)) {
      this.toast.warning('Solo se pueden editar borradores');
      return;
    }

    this.logger.debug('Editando borrador de factura:', invoice.idInvoice);

    // Navegar al componente de billing con el ID del borrador como query param
    this.router.navigate(['/invoices/create'], {
      queryParams: { draftId: invoice.idInvoice },
    });
  }

  /**
   * Elimina un borrador de factura
   */
  deleteDraft(invoice: InvoiceListResponseDto): void {
    if (!invoice.idInvoice) {
      this.toast.error('ID de factura inválido');
      return;
    }

    if (!this.isDraft(invoice)) {
      this.toast.warning(
        'Solo se pueden eliminar facturas en estado BORRADOR'
      );
      return;
    }

    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '500px',
      maxWidth: '90vw',
      data: {
        title: 'Confirmar eliminación',
        message:
          '¿Está seguro de que desea eliminar este borrador? Esta acción no se puede deshacer.',
        confirmText: 'Eliminar',
        cancelText: 'Cancelar',
        type: 'danger',
      },
    });

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.isLoading = true;
        this.cdr.markForCheck();

        this.logger.debug(
          `🗑️ Eliminando borrador de factura: ${invoice.idInvoice}`
        );

        this.draftInvoiceService
          .deleteDraft(invoice.idInvoice!)
          .pipe(
            takeUntil(this.destroy$),
            catchError((error) => {
              this.logger.error('Error deleting draft invoice', error);
              this.isLoading = false;
              this.cdr.markForCheck();
              this.toast.error(
                error.error?.message || 'Error al eliminar el borrador. Intente nuevamente.'
              );
              return of(null);
            })
          )
          .subscribe({
            next: () => {
              // La eliminación fue exitosa
              // Resetear a la primera página y refrescar la lista
              this.currentPage = 0;
              if (this.paginator) {
                this.paginator.pageIndex = 0;
              }
              this.search();

              this.toast.success('Borrador eliminado exitosamente');
              this.isLoading = false;
              this.cdr.markForCheck();
            },
            error: () => {
              // El error ya fue manejado en catchError
              this.isLoading = false;
              this.cdr.markForCheck();
            }
          });
      }
    });
  }
}

