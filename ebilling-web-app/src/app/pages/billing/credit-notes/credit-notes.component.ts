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
import { MatPaginator } from '@angular/material/paginator';
import { MatPaginatorIntl } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { ToastService } from '../../../_service/notification/toast.service';
import { MatDialog } from '@angular/material/dialog';
import { FormBuilder, FormGroup } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil, catchError } from 'rxjs/operators';
import { of } from 'rxjs';
import { CreditNoteService } from '../../../_service/invoice/credit-note.service';
import { CreditNoteDto, CreditNoteSearchByCompanyDto } from '../../../_model/invoice/credit-note-dto';
import { LoggerService } from '../../../_service/logger/logger.service';
import { environment } from '../../../../environments/environment';
import { ConfirmDialogComponent } from '../../../_shared/confirm-dialog/confirm-dialog.component';
import { SelectInvoiceDialogComponent } from './select-invoice-dialog/select-invoice-dialog.component';
import {
  CreateCreditNoteComponent,
  CreateCreditNoteDialogData,
} from './create-credit-note/create-credit-note.component';
import { InvoiceDto } from '../../../_model/invoice/invoice-dto';

/**
 * Custom Paginator Intl para personalizar el texto
 */
@Injectable()
export class CreditNotesPaginatorIntl extends MatPaginatorIntl {
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
      return `0 de ${length} notas de crédito`;
    }
    const startIndex = page * pageSize;
    const endIndex =
      startIndex < length
        ? Math.min(startIndex + pageSize, length)
        : startIndex + pageSize;
    return `Mostrando ${
      startIndex + 1
    }-${endIndex} de ${length} notas de crédito`;
  };
}

/**
 * CreditNotesComponent - Lista y gestión de notas de crédito
 *
 * Optimizado con OnPush Change Detection para mejor performance
 */
@Component({
  selector: 'app-credit-notes',
  templateUrl: './credit-notes.component.html',
  styleUrls: ['./credit-notes.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    { provide: MatPaginatorIntl, useClass: CreditNotesPaginatorIntl },
  ],
})
export class CreditNotesComponent implements OnInit, AfterViewInit, OnDestroy {
  searchForm: FormGroup;
  displayedColumns = [
    'creditNoteNumber',
    'originalInvoiceNumber',
    'clientIdentification',
    'clientName',
    'totalAmount',
    'status',
    'creationDate',
    'actions',
  ];
  dataSource = new MatTableDataSource<CreditNoteDto>([]);
  pageSize = 10;
  totalElements = 0;
  currentPage = 0;
  isLoading = false;

  private destroy$ = new Subject<void>();

  @ViewChild(MatPaginator, { static: true }) paginator: MatPaginator;
  @ViewChild(MatSort, { static: true }) sort: MatSort;

  constructor(
    private readonly creditNoteService: CreditNoteService,
    private readonly toast: ToastService,
    private readonly cdr: ChangeDetectorRef,
    private readonly dialog: MatDialog,
    private readonly fb: FormBuilder,
    private readonly logger: LoggerService
  ) {
    this.initForm();
  }

  ngOnInit(): void {
    this.logger.debug('CreditNotesComponent initialized');

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

  private initForm(): void {
    this.searchForm = this.fb.group({
      creditNoteNumber: [''],
      originalInvoiceNumber: [''],
    });
  }

  /**
   * Busca notas de crédito con filtros usando el endpoint search-by-company
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
    const searchDto: CreditNoteSearchByCompanyDto = {
      idCompany: idCompany,
      creditNoteNumber: this.searchForm.get('creditNoteNumber')?.value?.trim() || undefined,
      originalInvoiceNumber: this.searchForm.get('originalInvoiceNumber')?.value?.trim() || undefined,
    };

    this.creditNoteService
      .searchByCompany(searchDto, this.currentPage, this.pageSize)
      .pipe(
        takeUntil(this.destroy$),
        catchError((error) => {
          this.logger.error('Error searching credit notes', error);
          this.isLoading = false;
          this.dataSource.data = [];
          this.totalElements = 0;
          this.cdr.markForCheck();
          this.toast.error(
            'Error al buscar notas de crédito. Intente nuevamente.'
          );
          return of({ content: [], totalElements: 0, totalPages: 0, number: 0, size: this.pageSize });
        })
      )
      .subscribe((response) => {
        this.isLoading = false;

        this.logger.debug('Response from searchByCompany:', response);

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
    this.searchForm.reset({
      creditNoteNumber: '',
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
   * Abre el diálogo para crear una nueva nota de crédito
   * Las notas de crédito se crean a partir de una factura autorizada
   */
  openCreateDialog(): void {
    // Paso 1: Seleccionar factura
    const selectDialogRef = this.dialog.open(SelectInvoiceDialogComponent, {
      width: '650px',
      maxWidth: '90vw',
      maxHeight: '90vh',
      disableClose: false,
      autoFocus: false,
      panelClass: 'custom-dialog-container',
    });

    selectDialogRef
      .afterClosed()
      .subscribe((selectedInvoice: InvoiceDto | null) => {
        if (selectedInvoice) {
          this.logger.debug(
            'Factura seleccionada para nota de crédito:',
            selectedInvoice
          );

          // Paso 2: Abrir diálogo de creación de NC con la factura seleccionada
          this.openCreateCreditNoteDialog(selectedInvoice);
        }
      });
  }

  /**
   * Abre el diálogo de creación de nota de crédito
   */
  private openCreateCreditNoteDialog(invoice?: InvoiceDto, draftId?: number): void {
    const createDialogRef = this.dialog.open(CreateCreditNoteComponent, {
      width: '800px',
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
          ? 'Nota de crédito guardada como borrador'
          : `Nota de crédito ${
              result.response?.creditNoteNumber || ''
            } autorizada exitosamente`;

        this.toast.success(message);
      }
    });
  }

  /**
   * Edita un borrador de nota de crédito
   */
  editDraft(creditNote: CreditNoteDto): void {
    if (!creditNote.idCreditNote) {
      this.toast.error('ID de nota de crédito inválido');
      return;
    }

    if (!this.isDraft(creditNote)) {
      this.toast.warning('Solo se pueden editar borradores');
      return;
    }

    this.logger.debug('Editando borrador de nota de crédito:', creditNote.idCreditNote);

    // Abrir el diálogo de creación/edición con el draftId
    this.openCreateCreditNoteDialog(undefined, creditNote.idCreditNote);
  }


  /**
   * Elimina una nota de crédito (solo borradores)
   */
  deleteCreditNote(creditNote: CreditNoteDto): void {
    if (!creditNote.idCreditNote) {
      this.toast.error('ID de nota de crédito inválido');
      return;
    }

    // Verificar que sea un borrador
    if (!this.isDraft(creditNote)) {
      this.toast.warning(
        'Solo se pueden eliminar notas de crédito en estado BORRADOR'
      );
      return;
    }

    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '500px',
      maxWidth: '90vw',
      data: {
        title: 'Confirmar eliminación',
        message:
          '¿Está seguro de que desea eliminar esta nota de crédito? Esta acción no se puede deshacer.',
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
          `🗑️ Eliminando nota de crédito: ${creditNote.idCreditNote}`
        );

        this.creditNoteService
          .delete(creditNote.idCreditNote!)
          .pipe(
            takeUntil(this.destroy$),
            catchError((error) => {
              this.logger.error('Error deleting credit note', error);
              this.isLoading = false;
              this.cdr.markForCheck();
              this.toast.error(
                error.error?.message || 'Error al eliminar la nota de crédito. Intente nuevamente.'
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

              this.toast.success('Nota de crédito eliminada exitosamente');
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
   * Descarga el PDF de la nota de crédito
   */
  downloadPdf(creditNote: CreditNoteDto): void {
    if (!creditNote.idCreditNote) {
      this.toast.error('ID de nota de crédito inválido');
      return;
    }

    this.isLoading = true;
    this.cdr.markForCheck();

    this.creditNoteService
      .downloadPdf(creditNote.idCreditNote)
      .pipe(
        takeUntil(this.destroy$),
        catchError((error) => {
          this.logger.error('Error downloading PDF', error);
          this.isLoading = false;
          this.cdr.markForCheck();
          this.toast.error(
            'Error al descargar el PDF. Intente nuevamente.'
          );
          return of(null);
        })
      )
      .subscribe((blob) => {
        this.isLoading = false;
        this.cdr.markForCheck();

        if (blob) {
          const url = window.URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = `nota-credito-${
            creditNote.creditNoteNumber || creditNote.idCreditNote
          }.pdf`;
          document.body.appendChild(a);
          a.click();
          document.body.removeChild(a);
          window.URL.revokeObjectURL(url);

          this.toast.success('PDF descargado exitosamente');
        }
      });
  }

  /**
   * Descarga el XML de la nota de crédito
   */
  downloadXml(creditNote: CreditNoteDto): void {
    if (!creditNote.idCreditNote) {
      this.toast.error('ID de nota de crédito inválido');
      return;
    }

    this.isLoading = true;
    this.cdr.markForCheck();

    this.creditNoteService
      .downloadXml(creditNote.idCreditNote)
      .pipe(
        takeUntil(this.destroy$),
        catchError((error) => {
          this.logger.error('Error downloading XML', error);
          this.isLoading = false;
          this.cdr.markForCheck();
          this.toast.error(
            'Error al descargar el XML. Intente nuevamente.'
          );
          return of(null);
        })
      )
      .subscribe((blob) => {
        this.isLoading = false;
        this.cdr.markForCheck();

        if (blob) {
          const url = window.URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = `nota-credito-${
            creditNote.creditNoteNumber || creditNote.idCreditNote
          }.xml`;
          document.body.appendChild(a);
          a.click();
          document.body.removeChild(a);
          window.URL.revokeObjectURL(url);

          this.toast.success('XML descargado exitosamente');
        }
      });
  }

  /**
   * Formatea una fecha en formato legible
   */
  formatDate(date: string | undefined): string {
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
   * Formatea un monto en formato de moneda
   */
  formatCurrency(amount: number | undefined): string {
    if (amount === undefined || amount === null) return '$0.00';
    return `$${amount.toFixed(2)}`;
  }

  /**
   * Obtiene el nombre del cliente
   * Busca en originalInvoice.clientDto primero (viene del backend)
   */
  getClientName(creditNote: CreditNoteDto): string {
    const cn = creditNote as any;
    return (
      cn.originalInvoice?.clientDto?.name ||
      cn.originalInvoice?.buyerName ||
      cn.buyerName ||
      cn.clientDto?.name ||
      (cn.idClient ? `Cliente #${cn.idClient}` : 'Cliente no especificado')
    );
  }

  /**
   * Obtiene la identificación del cliente
   * Busca en originalInvoice.clientDto primero
   */
  getClientIdentification(creditNote: CreditNoteDto): string {
    const cn = creditNote as any;
    return (
      cn.originalInvoice?.clientDto?.identification ||
      cn.originalInvoice?.buyerIdentification ||
      cn.buyerIdentification ||
      cn.clientDto?.identification ||
      'Sin identificación'
    );
  }

  /**
   * Obtiene el número de nota de crédito
   * Construye desde sequential: 001-001-000000XXX
   */
  getCreditNoteNumber(creditNote: CreditNoteDto): string {
    const cn = creditNote as any;

    // Si ya tenemos el número formateado
    if (cn.creditNoteNumber) {
      return cn.creditNoteNumber;
    }

    // Construir desde sequential
    if (cn.sequential) {
      const seq = cn.sequential.toString().padStart(9, '0');
      // TODO: Obtener establecimiento y punto de emisión reales
      return `001-001-${seq}`;
    }

    return '-';
  }

  /**
   * Obtiene el número de factura original
   * Busca en originalInvoice o construye desde originalInvoiceNumber
   */
  getOriginalInvoiceNumber(creditNote: CreditNoteDto): string {
    const cn = creditNote as any;

    // 1. Si viene el número formateado directamente
    if (cn.originalInvoiceNumber) {
      return cn.originalInvoiceNumber;
    }

    // 2. Desde el objeto originalInvoice
    if (cn.originalInvoice?.sequential) {
      const seq = cn.originalInvoice.sequential.toString().padStart(9, '0');
      return `001-001-${seq}`;
    }

    if (cn.originalInvoice?.invoiceNumber) {
      return cn.originalInvoice.invoiceNumber;
    }

    // 3. Fallback: mostrar ID con formato amigable
    return cn.originalInvoiceId ? `FAC-${cn.originalInvoiceId}` : '-';
  }

  /**
   * Obtiene el total de la nota de crédito
   * Calcula desde creditNoteDetails si no hay totalAmount
   */
  getTotal(creditNote: CreditNoteDto): number {
    const cn = creditNote as any;

    // Si tiene totalAmount directo
    if (cn.totalAmount && cn.totalAmount > 0) {
      return cn.totalAmount;
    }

    // Calcular desde detalles
    if (cn.creditNoteDetails && cn.creditNoteDetails.length > 0) {
      let subtotal = 0;
      cn.creditNoteDetails.forEach((detail: any) => {
        const qty = detail.quantity || 0;
        const price = detail.unitPrice || 0;
        const disc = detail.discount || 0;
        subtotal += qty * price - disc;
      });
      // Agregar IVA (15%)
      return subtotal * 1.15;
    }

    return cn.valorModificacion || 0;
  }

  /**
   * Determina la clase CSS para el badge de estado
   */
  getStatusClass(creditNote: CreditNoteDto): string {
    // Priorizar creditNoteStatus si existe (específicamente para DEVUELTA)
    const creditNoteStatus = (creditNote as any).creditNoteStatus;
    if (creditNoteStatus && typeof creditNoteStatus === 'string') {
      const upperCreditNoteStatus = creditNoteStatus.toUpperCase();
      if (upperCreditNoteStatus === 'DEVUELTA') {
        return 'badge-danger';
      }
    }

    const status = creditNote.status as any;
    const hasNumber = !!(creditNote.creditNoteNumber || creditNote.sequential);
    
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
        return 'badge-success';
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
      default:
        return 'badge-secondary';
    }
  }

  /**
   * Obtiene el texto de estado legible
   * Valida que una nota autorizada tenga número asignado
   */
  getStatusText(creditNote: CreditNoteDto): string {
    // Priorizar creditNoteStatus si existe (específicamente para DEVUELTA)
    const creditNoteStatus = (creditNote as any).creditNoteStatus;
    if (creditNoteStatus && typeof creditNoteStatus === 'string') {
      const upperCreditNoteStatus = creditNoteStatus.toUpperCase();
      if (upperCreditNoteStatus === 'DEVUELTA') {
        return 'Devuelta';
      }
    }

    const status = creditNote.status as any;
    const hasNumber = !!(creditNote.creditNoteNumber || creditNote.sequential);
    
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
      default:
        return String(status);
    }
  }

  /**
   * Verifica si la nota de crédito es un borrador
   * Una nota marcada como autorizada pero sin número también se considera borrador
   */
  isDraft(creditNote: CreditNoteDto): boolean {
    // Si es DEVUELTA, no es borrador (y no se mostrarán acciones)
    const creditNoteStatus = (creditNote as any).creditNoteStatus;
    if (creditNoteStatus && typeof creditNoteStatus === 'string' && creditNoteStatus.toUpperCase() === 'DEVUELTA') {
      return false;
    }

    const status = creditNote.status as any;
    const hasNumber = !!(creditNote.creditNoteNumber || creditNote.sequential);
    
    // Si está marcado como autorizado pero no tiene número, es un borrador
    const isAuthorized = status === true || status === 'true' || 
                         (typeof status === 'string' && status.toUpperCase() === 'AUTORIZADO');
    if (isAuthorized && !hasNumber) {
      return true; // Es un borrador aunque esté marcado como autorizado
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
   * Verifica si la nota de crédito está autorizada
   * Una nota solo está realmente autorizada si tiene número asignado
   * Si es DEVUELTA, no está autorizada (no se mostrarán acciones)
   */
  isAuthorized(creditNote: CreditNoteDto): boolean {
    // Si es DEVUELTA, no está autorizada (no se mostrarán acciones)
    const creditNoteStatus = (creditNote as any).creditNoteStatus;
    if (creditNoteStatus && typeof creditNoteStatus === 'string' && creditNoteStatus.toUpperCase() === 'DEVUELTA') {
      return false;
    }

    const status = creditNote.status as any;
    const hasNumber = !!(creditNote.creditNoteNumber || creditNote.sequential);
    
    // Si no tiene número, no puede estar autorizada
    if (!hasNumber) return false;
    
    // Booleano true = autorizado
    if (status === true || status === 'true') return true;
    if (status === false || status === 'false') return false;
    if (!status) return false;
    if (typeof status !== 'string') return false;
    const upperStatus = status.toUpperCase();
    return upperStatus === 'AUTORIZADO' || upperStatus === 'AUTHORIZED';
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
