import {
  Component,
  OnInit,
  AfterViewInit,
  ViewChild,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
} from '@angular/core';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { ToastService } from '../../../_service/notification/toast.service';
import { MatDialog } from '@angular/material/dialog';
import { DraftInvoiceService } from '../../../_service/invoice/draft-invoice.service';
import { InvoiceDto } from '../../../_model/invoice/invoice-dto';
import { LoggerService } from '../../../_service/logger/logger.service';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { ConfirmDialogComponent } from '../../../_shared/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-draft-invoices',
  templateUrl: './draft-invoices.component.html',
  styleUrls: ['./draft-invoices.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DraftInvoicesComponent implements OnInit, AfterViewInit {
  dataSource: MatTableDataSource<InvoiceDto> =
    new MatTableDataSource<InvoiceDto>([]);

  displayedColumns: string[] = [
    'clientIdentification',
    'clientName',
    'totalAmount',
    'creationDate',
    'actions',
  ];

  @ViewChild(MatPaginator, { static: false }) paginator: MatPaginator;
  @ViewChild(MatSort, { static: false }) sort: MatSort;

  isLoading = false;

  constructor(
    private readonly draftInvoiceService: DraftInvoiceService,
    private readonly toast: ToastService,
    private readonly logger: LoggerService,
    private readonly cdRef: ChangeDetectorRef,
    private readonly dialog: MatDialog,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.logger.debug('DraftInvoicesComponent initialized');
    this.loadDrafts();
  }

  ngAfterViewInit(): void {
    if (this.paginator && this.sort) {
      this.dataSource.paginator = this.paginator;
      this.dataSource.sort = this.sort;

      // Configurar sorting personalizado
      this.dataSource.sortingDataAccessor = (item, property) => {
        switch (property) {
          case 'clientName':
            return (
              item.buyerName ||
              item.clientDto?.name ||
              'Cliente no especificado'
            );
          case 'clientIdentification':
            return (
              item.buyerIdentification ||
              item.clientDto?.identification ||
              'Sin identificación'
            );
          case 'creationDate':
            return new Date(item.creationDate || '').getTime();
          default:
            return (item as any)[property];
        }
      };
    }
  }

  /**
   * Carga todos los borradores desde el backend
   */
  loadDrafts(): void {
    this.isLoading = true;
    this.cdRef.markForCheck();

    this.draftInvoiceService.getDrafts().subscribe({
      next: (drafts: InvoiceDto[]) => {
        this.logger.debug(`✅ ${drafts.length} borradores cargados`);

        // 🔍 LOGS DETALLADOS PARA DEBUGGING
        this.logger.debug('═══════════════════════════════════════════════');
        this.logger.debug('🔍 DATOS DE LOS BORRADORES RECIBIDOS DEL BACKEND:');
        this.logger.debug('═══════════════════════════════════════════════');
        drafts.forEach((draft, index) => {
          this.logger.debug(`\nBorrador ${index + 1}:`);
          this.logger.debug('  idInvoice:', draft.idInvoice);
          this.logger.debug('  idClient:', draft.idClient);
          this.logger.debug(
            '  buyerIdentification:',
            draft.buyerIdentification
          );
          this.logger.debug('  buyerName:', draft.buyerName);
          this.logger.debug('  clientDto:', draft.clientDto);
          this.logger.debug('  totalAmount:', draft.totalAmount);
        });
        this.logger.debug('═══════════════════════════════════════════════');

        this.dataSource.data = drafts;
        this.isLoading = false;
        this.cdRef.markForCheck();
      },
      error: (err: HttpErrorResponse) => {
        this.logger.error('Error al cargar borradores', err);
        this.isLoading = false;
        this.cdRef.markForCheck();

        this.toast.error('Error al cargar borradores. Intente nuevamente.');
      },
    });
  }

  /**
   * Elimina un borrador permanentemente (eliminación física)
   */
  deleteDraft(draft: InvoiceDto): void {
    if (!draft.idInvoice) {
      this.toast.error('ID de factura inválido');
      return;
    }

    // Abrir diálogo de confirmación bonito
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '500px',
      maxWidth: '90vw',
      data: {
        title: '¿Eliminar borrador?',
        message:
          '¿Está seguro de eliminar este borrador? Esta acción no se puede deshacer.',
        confirmText: 'Eliminar',
        cancelText: 'Cancelar',
        type: 'danger',
      },
    });

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (!confirmed) {
        return;
      }

      this.isLoading = true;
      this.cdRef.markForCheck();

      this.logger.debug(`🗑️ Eliminando borrador: ${draft.idInvoice}`);

      this.draftInvoiceService.deleteDraft(draft.idInvoice!).subscribe({
        next: () => {
          this.logger.debug('✅ Borrador eliminado exitosamente');
          this.isLoading = false;

          this.toast.success('Borrador eliminado exitosamente');

          // Recargar lista de borradores
          this.loadDrafts();
        },
        error: (err: HttpErrorResponse) => {
          this.logger.error('❌ Error al eliminar borrador', err);
          this.isLoading = false;
          this.cdRef.markForCheck();

          this.toast.error('Error al eliminar el borrador. Intente nuevamente.');
        },
      });
    });
  }

  /**
   * Navega a la página de facturación para continuar editando el borrador
   */
  editDraft(draft: InvoiceDto): void {
    this.logger.debug('Editando borrador:', draft.idInvoice);

    // Navegar al componente de billing con el ID del borrador como query param
    // La ruta correcta es /invoices/create para acceder al formulario de facturación
    this.router.navigate(['/invoices/create'], {
      queryParams: { draftId: draft.idInvoice },
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
    } catch (error) {
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
   */
  getClientName(draft: InvoiceDto): string {
    return (
      draft.buyerName || draft.clientDto?.name || 'Cliente no especificado'
    );
  }

  /**
   * Obtiene la identificación del cliente
   */
  getClientIdentification(draft: InvoiceDto): string {
    return (
      draft.buyerIdentification ||
      draft.clientDto?.identification ||
      'Sin identificación'
    );
  }
}
