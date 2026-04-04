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
import { MatDialog } from '@angular/material/dialog';
import { FormBuilder, FormGroup } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil, catchError } from 'rxjs/operators';
import { of } from 'rxjs';
import { DebitNoteService } from '../../../_service/invoice/debit-note.service';
import { DebitNoteDto, DebitNoteListResponseDto } from '../../../_model/invoice/debit-note-dto';
import { LoggerService } from '../../../_service/logger/logger.service';
import { ConfirmDialogComponent } from '../../../_shared/confirm-dialog/confirm-dialog.component';
import { SelectInvoiceDialogComponent } from '../credit-notes/select-invoice-dialog/select-invoice-dialog.component';
import {
  CreateDebitNoteComponent,
  CreateDebitNoteDialogData,
} from './create-debit-note/create-debit-note.component';
import { InvoiceDto } from '../../../_model/invoice/invoice-dto';
import { environment } from '../../../../environments/environment';

/**
 * Custom Paginator Intl para personalizar el texto
 */
@Injectable()
export class DebitNotesPaginatorIntl extends MatPaginatorIntl {
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
      return `0 de ${length} notas de débito`;
    }
    const startIndex = page * pageSize;
    const endIndex =
      startIndex < length
        ? Math.min(startIndex + pageSize, length)
        : startIndex + pageSize;
    return `Mostrando ${
      startIndex + 1
    }-${endIndex} de ${length} notas de débito`;
  };
}

/**
 * DebitNotesComponent - Lista y gestión de notas de débito
 *
 * Optimizado con OnPush Change Detection para mejor performance
 */
@Component({
  selector: 'app-debit-notes',
  templateUrl: './debit-notes.component.html',
  styleUrls: ['./debit-notes.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [{ provide: MatPaginatorIntl, useClass: DebitNotesPaginatorIntl }],
})
export class DebitNotesComponent implements OnInit, AfterViewInit, OnDestroy {
  searchForm: FormGroup;
  displayedColumns = [
    'debitNoteNumber',
    'originalInvoiceNumber',
    'clientIdentification',
    'clientName',
    'totalAmount',
    'status',
    'creationDate',
    'actions',
  ];
  dataSource = new MatTableDataSource<DebitNoteDto>([]);
  pageSize = 10;
  totalElements = 0;
  currentPage = 0;
  isLoading = false;

  private destroy$ = new Subject<void>();

  @ViewChild(MatPaginator, { static: true }) paginator: MatPaginator;
  @ViewChild(MatSort, { static: true }) sort: MatSort;

  constructor(
    private readonly debitNoteService: DebitNoteService,
    private readonly toast: ToastService,
    private readonly cdr: ChangeDetectorRef,
    private readonly dialog: MatDialog,
    private readonly fb: FormBuilder,
    private readonly logger: LoggerService
  ) {
    this.initForm();
  }

  ngOnInit(): void {
    this.logger.debug('DebitNotesComponent initialized');

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
    this.searchForm = this.fb.group({
      debitNoteNumber: [''],
      originalInvoiceNumber: [''],
    });
  }

  /**
   * Busca notas de débito con filtros usando el endpoint search-by-company
   */
  search(): void {
    // Obtener idCompany desde localStorage
    const idCompanyStr = localStorage.getItem(environment.ID_COMPANY);
    if (!idCompanyStr) {
      this.logger.error('No se encontró ID de compañía en localStorage');
      this.toast.error(
        'Error: No se encontró información de la compañía. Por favor, inicie sesión nuevamente.'
      );
      return;
    }

    const idCompany = Number(idCompanyStr);
    if (!idCompany || isNaN(idCompany)) {
      this.logger.error('ID de compañía inválido:', idCompanyStr);
      this.toast.error(
        'Error: ID de compañía inválido. Por favor, inicie sesión nuevamente.'
      );
      return;
    }

    this.isLoading = true;
    this.cdr.markForCheck();

    // Construir el DTO de búsqueda
    const searchDto = {
      idCompany: idCompany,
      debitNoteNumber: this.searchForm.get('debitNoteNumber')?.value?.trim() || undefined,
      originalInvoiceNumber: this.searchForm.get('originalInvoiceNumber')?.value?.trim() || undefined,
    };

    this.debitNoteService
      .searchByCompany(searchDto, this.currentPage, this.pageSize)
      .pipe(
        takeUntil(this.destroy$),
        catchError((error) => {
          this.logger.error('Error searching debit notes', error);
          this.isLoading = false;
          this.dataSource.data = [];
          this.totalElements = 0;
          this.cdr.markForCheck();
          this.toast.error(
            'Error al buscar notas de débito. Intente nuevamente.'
          );
          return of({ content: [], totalElements: 0, totalPages: 0, number: 0, size: this.pageSize });
        })
      )
      .subscribe((response) => {
        this.isLoading = false;

        this.logger.debug('Response from searchByCompany:', response);

        if (response && response.content && Array.isArray(response.content)) {
          // Mapear DebitNoteListResponseDto a DebitNoteDto para mantener compatibilidad
          const mappedData: DebitNoteDto[] = response.content.map((item: DebitNoteListResponseDto) => ({
            idDebitNote: item.idDebitNote,
            debitNoteNumber: item.debitNoteNumber,
            originalInvoiceNumber: item.originalInvoiceNumber,
            buyerIdentification: item.buyerIdentification,
            buyerName: item.buyerName,
            totalAmount: item.totalAmount,
            valorTotal: item.totalAmount,
            debitNoteStatus: item.debitNoteStatus,
            status: item.debitNoteStatus,
            creationDate: item.createDate || item.creationDate,
          }));

          this.dataSource.data = mappedData;
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
    this.searchForm.reset({
      debitNoteNumber: '',
      originalInvoiceNumber: '',
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
   * Abre el diálogo para crear una nueva nota de débito
   */
  openCreateDialog(): void {
    // Paso 1: Seleccionar factura original
    const selectDialogRef = this.dialog.open(SelectInvoiceDialogComponent, {
      width: '650px',
      maxWidth: '90vw',
      maxHeight: '90vh',
      disableClose: false,
      autoFocus: false,
      panelClass: 'custom-dialog-container',
      data: {
        hideAvailableAmount: true, // Ocultar saldo disponible para notas de débito
      },
    });

    selectDialogRef
      .afterClosed()
      .subscribe((selectedInvoice: InvoiceDto | null) => {
        if (selectedInvoice) {
          this.logger.debug(
            'Factura seleccionada para nota de débito:',
            selectedInvoice
          );

          // Abrir diálogo de creación de ND con la factura seleccionada
          this.openCreateDebitNoteDialog(selectedInvoice);
        }
      });
  }

  /**
   * Abre el diálogo de creación de nota de débito
   */
  private openCreateDebitNoteDialog(invoice?: InvoiceDto, draftId?: number): void {
    const createDialogRef = this.dialog.open(CreateDebitNoteComponent, {
      width: '900px',
      maxWidth: '95vw',
      maxHeight: '95vh',
      disableClose: true,
      autoFocus: false,
      panelClass: 'custom-dialog-container',
      data: { invoice, draftId },
    });

    createDialogRef.afterClosed().subscribe((result) => {
      if (result?.success) {
        // Resetear a la primera página y refrescar la lista
        this.currentPage = 0;
        if (this.paginator) {
          this.paginator.pageIndex = 0;
        }
        this.search(); // Refrescar la lista

        const message = result.draft
          ? 'Nota de débito guardada como borrador'
          : `Nota de débito ${
              result.response?.debitNoteNumber || ''
            } autorizada exitosamente`;

        this.toast.success(message);
      }
    });
  }

  /**
   * Edita un borrador de nota de débito
   */
  editDraft(debitNote: DebitNoteDto): void {
    if (!debitNote.idDebitNote) {
      this.toast.error('ID de nota de débito inválido');
      return;
    }

    if (!this.isDraft(debitNote)) {
      this.toast.warning('Solo se pueden editar borradores');
      return;
    }

    this.logger.debug('Editando borrador de nota de débito:', debitNote.idDebitNote);

    // Abrir el diálogo de creación/edición con el draftId
    this.openCreateDebitNoteDialog(undefined, debitNote.idDebitNote);
  }

  /**
   * Envía un borrador al SRI
   */
  sendDraftToSri(debitNote: DebitNoteDto): void {
    if (!debitNote.idDebitNote) {
      this.toast.error('ID de nota de débito inválido');
      return;
    }

    if (!this.isDraft(debitNote)) {
      this.toast.warning('Solo se pueden enviar borradores al SRI');
      return;
    }

    // Pedir contraseña de firma
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '450px',
      maxWidth: '90vw',
      data: {
        title: 'Enviar al SRI',
        message:
          'Ingrese la contraseña de su firma electrónica para enviar la nota de débito al SRI.',
        confirmText: 'Enviar',
        cancelText: 'Cancelar',
        type: 'info',
        showPasswordInput: true,
        passwordLabel: 'Contraseña de firma',
      },
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result?.confirmed && result?.password) {
        this.isLoading = true;
        this.cdr.markForCheck();

        this.debitNoteService
          .sendToSri(debitNote.idDebitNote!, result.password)
          .pipe(
            takeUntil(this.destroy$),
            catchError((error) => {
              this.logger.error('Error sending draft to SRI', error);
              this.isLoading = false;
              this.cdr.markForCheck();
              this.toast.error(
                error.error?.message ||
                  'Error al enviar al SRI. Intente nuevamente.'
              );
              return of(null);
            })
          )
          .subscribe((response) => {
            this.isLoading = false;
            this.cdr.markForCheck();

            if (response?.code === 200) {
              // Resetear a la primera página y refrescar la lista
              this.currentPage = 0;
              if (this.paginator) {
                this.paginator.pageIndex = 0;
              }
              this.search();
              this.toast.success(
                `Nota de débito ${
                  response.debitNoteNumber || ''
                } autorizada exitosamente`
              );
            } else if (response) {
              const errorMsg = response.message || 'Error al procesar';
              this.toast.error(errorMsg);
            }
          });
      }
    });
  }

  /**
   * Elimina una nota de débito (solo borradores)
   */
  deleteDebitNote(debitNote: DebitNoteDto): void {
    if (!debitNote.idDebitNote) {
      this.toast.error('ID de nota de débito inválido');
      return;
    }

    if (!this.isDraft(debitNote)) {
      this.toast.warning(
        'Solo se pueden eliminar notas de débito en estado BORRADOR'
      );
      return;
    }

    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '500px',
      maxWidth: '90vw',
      data: {
        title: 'Confirmar eliminación',
        message:
          '¿Está seguro de que desea eliminar esta nota de débito? Esta acción no se puede deshacer.',
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
          `🗑️ Eliminando nota de débito: ${debitNote.idDebitNote}`
        );

        this.debitNoteService
          .delete(debitNote.idDebitNote!)
          .pipe(
            takeUntil(this.destroy$),
            catchError((error) => {
              this.logger.error('Error deleting debit note', error);
              this.isLoading = false;
              this.cdr.markForCheck();
              this.toast.error(
                error.error?.message || 'Error al eliminar la nota de débito. Intente nuevamente.'
              );
              return of(null);
            })
          )
          .subscribe({
            next: () => {
              // La eliminación fue exitosa (Observable<void> se completa sin valor)
              // Resetear a la primera página y refrescar la lista
              this.currentPage = 0;
              if (this.paginator) {
                this.paginator.pageIndex = 0;
              }
              this.search();

              this.toast.success('Nota de débito eliminada exitosamente');
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

  /**
   * Descarga el PDF de una nota de débito
   */
  downloadPdf(idDebitNote: number, debitNoteNumber: string): void {
    this.debitNoteService.downloadPdf(idDebitNote).subscribe({
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
        const cleanFileName = debitNoteNumber.replace(/[^a-zA-Z0-9-]/g, '_');
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
   * Descarga el XML de una nota de débito
   */
  downloadXml(idDebitNote: number, debitNoteNumber: string): void {
    this.debitNoteService.downloadXml(idDebitNote).subscribe({
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
        const cleanFileName = debitNoteNumber.replace(/[^a-zA-Z0-9-]/g, '_');
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


  // ========== HELPERS ==========

  /**
   * Verifica si una nota de débito es un borrador
   */
  isDraft(debitNote: DebitNoteDto): boolean {
    // Si es DEVUELTA, no es borrador (y no se mostrarán acciones)
    if (debitNote.debitNoteStatus && typeof debitNote.debitNoteStatus === 'string' && debitNote.debitNoteStatus.toUpperCase() === 'DEVUELTA') {
      return false;
    }

    const status = debitNote.status as any;
    const hasNumber = !!(debitNote.debitNoteNumber || debitNote.sequential);
    
    // Si está marcado como autorizado pero no tiene número, es un borrador
    const isAuthorized = status === true || status === 'true' || 
                         (typeof status === 'string' && status.toUpperCase() === 'AUTORIZADO') ||
                         debitNote.debitNoteStatus === 'AUTORIZADO' ||
                         debitNote.authorizationStatusSri === 'AUTORIZADO';
    if (isAuthorized && !hasNumber) {
      return true; // Es un borrador aunque esté marcado como autorizado
    }
    
    // Si tiene número y está autorizado, NO es borrador
    if (isAuthorized && hasNumber) {
      return false;
    }
    
    // Booleano false = borrador
    if (status === false || status === 'false') return true;
    if (status === true || status === 'true') return false;
    if (!status) return true;
    if (typeof status !== 'string') return true;
    const upperStatus = status.toUpperCase();
    return upperStatus === 'BORRADOR' || upperStatus === 'DRAFT';
  }

  /**
   * Verifica si una nota de débito está autorizada
   * Una nota solo está realmente autorizada si tiene número asignado
   * Si es DEVUELTA, no está autorizada (no se mostrarán acciones)
   */
  isAuthorized(debitNote: DebitNoteDto): boolean {
    // Si es DEVUELTA, no está autorizada (no se mostrarán acciones)
    if (debitNote.debitNoteStatus && typeof debitNote.debitNoteStatus === 'string' && debitNote.debitNoteStatus.toUpperCase() === 'DEVUELTA') {
      return false;
    }

    const status = debitNote.status as any;
    const hasNumber = !!(debitNote.debitNoteNumber || debitNote.sequential);
    
    // Si no tiene número, no puede estar autorizada
    if (!hasNumber) return false;
    
    // Verificar si está autorizado
    const isAuthorized = status === true || status === 'true' || 
                         (typeof status === 'string' && status.toUpperCase() === 'AUTORIZADO') ||
                         debitNote.debitNoteStatus === 'AUTORIZADO' ||
                         debitNote.authorizationStatusSri === 'AUTORIZADO';
    
    return isAuthorized;
  }

  /**
   * Obtiene el número de nota de débito
   */
  getDebitNoteNumber(debitNote: DebitNoteDto): string {
    return debitNote.debitNoteNumber || '-';
  }

  /**
   * Obtiene el número de factura original
   */
  getOriginalInvoiceNumber(debitNote: DebitNoteDto): string {
    return debitNote.originalInvoiceNumber || '-';
  }

  /**
   * Obtiene la identificación del cliente
   */
  getClientIdentification(debitNote: DebitNoteDto): string {
    return (
      debitNote.buyerIdentification ||
      debitNote.clientDto?.identification ||
      '-'
    );
  }

  /**
   * Obtiene el nombre del cliente
   */
  getClientName(debitNote: DebitNoteDto): string {
    return debitNote.buyerName || debitNote.clientDto?.name || '-';
  }

  /**
   * Obtiene el total
   */
  getTotal(debitNote: DebitNoteDto): number {
    return debitNote.valorTotal || debitNote.totalAmount || 0;
  }

  /**
   * Formatea moneda
   */
  formatCurrency(value: number): string {
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
   * Obtiene el texto del estado
   */
  /**
   * Obtiene el texto de estado legible
   * Valida que una nota autorizada tenga número asignado
   */
  getStatusText(debitNote: DebitNoteDto): string {
    // Priorizar debitNoteStatus si existe (específicamente para DEVUELTA)
    if (debitNote.debitNoteStatus && typeof debitNote.debitNoteStatus === 'string') {
      const upperDebitNoteStatus = debitNote.debitNoteStatus.toUpperCase();
      if (upperDebitNoteStatus === 'DEVUELTA') {
        return 'Devuelta';
      }
    }

    const status = debitNote.status as any;
    const hasNumber = !!(debitNote.debitNoteNumber || debitNote.sequential);
    
    // Si está marcado como autorizado pero no tiene número, mostrar como "Borrador"
    const isAuthorized = status === true || status === 'true' || 
                         (typeof status === 'string' && status.toUpperCase() === 'AUTORIZADO');
    if (isAuthorized && !hasNumber) {
      return 'Borrador'; // No puede estar autorizada sin número
    }

    // Manejar caso de booleano (false = borrador)
    if (status === false || status === 'false') return 'Borrador';
    if (status === true || status === 'true') return 'Autorizado';
    if (!status) return 'Borrador';

    // Manejar caso de string
    if (typeof status !== 'string') return 'Desconocido';

    const upperStatus = status.toUpperCase();
    switch (upperStatus) {
      case 'AUTORIZADO':
      case 'AUTHORIZED':
        return 'Autorizado';
      case 'BORRADOR':
      case 'DRAFT':
        return 'Borrador';
      case 'RECHAZADO':
      case 'REJECTED':
        return 'Rechazado';
      case 'DEVUELTA':
        return 'Devuelta';
      case 'PENDIENTE':
      case 'PENDING':
        return 'Pendiente';
      case 'EN_PROCESO':
      case 'EN PROCESO':
        return 'En Proceso';
      default:
        return String(status);
    }
  }

  /**
   * Obtiene la clase CSS del estado
   * Valida que una nota autorizada tenga número asignado
   */
  getStatusClass(debitNote: DebitNoteDto): string {
    // Priorizar debitNoteStatus si existe (específicamente para DEVUELTA)
    if (debitNote.debitNoteStatus && typeof debitNote.debitNoteStatus === 'string') {
      const upperDebitNoteStatus = debitNote.debitNoteStatus.toUpperCase();
      if (upperDebitNoteStatus === 'DEVUELTA') {
        return 'badge-danger';
      }
    }

    const status = debitNote.status as any;
    const hasNumber = !!(debitNote.debitNoteNumber || debitNote.sequential);
    
    // Si está marcado como autorizado pero no tiene número, mostrar como borrador
    const isAuthorized = status === true || status === 'true' || 
                         (typeof status === 'string' && status.toUpperCase() === 'AUTORIZADO');
    if (isAuthorized && !hasNumber) {
      return 'badge-warning'; // Borrador
    }

    // Manejar caso de booleano (false = borrador)
    if (status === false || status === 'false') return 'badge-warning';
    if (status === true || status === 'true') return 'badge-success';
    if (!status) return 'badge-warning';

    // Manejar caso de string
    if (typeof status !== 'string') return 'badge-secondary';

    const upperStatus = status.toUpperCase();
    switch (upperStatus) {
      case 'AUTORIZADO':
      case 'AUTHORIZED':
        return hasNumber ? 'badge-success' : 'badge-warning'; // Si es autorizado pero sin número, es borrador
      case 'BORRADOR':
      case 'DRAFT':
        return 'badge-warning';
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
}
