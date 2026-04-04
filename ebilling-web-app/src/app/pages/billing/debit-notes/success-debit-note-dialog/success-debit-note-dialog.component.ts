import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { LoggerService } from '../../../../_service/logger/logger.service';
import { DebitNoteService } from '../../../../_service/invoice/debit-note.service';
import { ToastService } from '../../../../_service/notification/toast.service';

export interface SuccessDebitNoteDialogData {
  debitNoteId: number;
  debitNoteNumber?: string;
  clientName?: string;
  total?: number;
  authorizationStatusSri?: string;
}

/**
 * SuccessDebitNoteDialogComponent - Diálogo de éxito después de crear nota de débito
 *
 * Muestra un mensaje de éxito y permite descargar el recibo (PDF) y el RIDE (XML).
 */
@Component({
  selector: 'app-success-debit-note-dialog',
  templateUrl: './success-debit-note-dialog.component.html',
  styleUrls: ['./success-debit-note-dialog.component.css'],
})
export class SuccessDebitNoteDialogComponent {
  // Exponer Math para usar en el template
  Math = Math;

  constructor(
    private readonly dialogRef: MatDialogRef<SuccessDebitNoteDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: SuccessDebitNoteDialogData,
    private readonly logger: LoggerService,
    private readonly debitNoteService: DebitNoteService,
    private readonly toast: ToastService
  ) {}

  /**
   * Imprime el recibo/ticket de la nota de débito
   * Abre una nueva ventana con la página de impresión
   */
  printTicket(): void {
    this.logger.debug('🖨️ Imprimiendo ticket de nota de débito desde diálogo de éxito', {
      debitNoteId: this.data.debitNoteId,
    });

    const relativeUrl = `/invoices/debit-note-ticket/${this.data.debitNoteId}`;
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
        // Si falla, intentar con URL completa
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
          this.logger.warn(
            '⚠️ No se pudo abrir la ventana de impresión (posible bloqueador de popups)'
          );
          this.toast.warning(
            'No se pudo abrir la ventana de impresión. Verifique que los popups estén permitidos.'
          );
        }
      }
    } catch (error) {
      this.logger.error('Error al abrir ventana de impresión', error);
      this.toast.error(
        'Error al abrir la ventana de impresión. Intente nuevamente.'
      );
    }
  }

  /**
   * Descarga el PDF de la nota de débito (RIDE)
   * GET /api/debit-notes/{id}/pdf
   */
  printRide(): void {
    if (!this.data.debitNoteNumber) {
      this.toast.warning(
        'No hay número de nota de débito disponible para descargar'
      );
      return;
    }

    this.logger.debug('📄 Descargando PDF de nota de débito (RIDE)', {
      debitNoteId: this.data.debitNoteId,
      debitNoteNumber: this.data.debitNoteNumber,
    });

    this.debitNoteService.downloadPdf(this.data.debitNoteId).subscribe({
      next: (blob: Blob) => {
        // Verificar si el blob es realmente un PDF o un error JSON
        if (blob.type === 'application/json' || blob.size < 100) {
          // Es probablemente un error, leer el contenido
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

        // Crear URL del blob y abrir en nueva ventana para visualizar/imprimir
        const url = window.URL.createObjectURL(blob);
        const printWindow = window.open(url, '_blank');
        
        if (!printWindow) {
          // Si no se puede abrir ventana (bloqueador de popups), descargar directamente
          const link = document.createElement('a');
          link.href = url;
          const cleanFileName = this.data.debitNoteNumber!.replace(
            /[^a-zA-Z0-9-]/g,
            '_'
          );
          link.download = `${cleanFileName}.pdf`;
          link.style.display = 'none';
          document.body.appendChild(link);
          link.click();
          document.body.removeChild(link);
          this.toast.success(
            'PDF descargado. Ábralo para visualizar e imprimir.'
          );
          // Limpiar URL después de descargar
          setTimeout(() => {
            window.URL.revokeObjectURL(url);
          }, 1000);
        } else {
          // El PDF se abrirá en una nueva ventana donde el usuario puede imprimirlo
          this.toast.info(
            'PDF abierto. Use Ctrl+P o el botón de imprimir del navegador.'
          );
          // Limpiar URL después de un tiempo (cuando la ventana ya no la necesite)
          setTimeout(() => {
            window.URL.revokeObjectURL(url);
          }, 60000); // 60 segundos para dar tiempo a imprimir
        }
      },
      error: (error) => {
        this.logger.error('Error al descargar PDF de nota de débito', error);
        this.toast.error(
          'Error al descargar el PDF. Intente nuevamente.'
        );
      },
    });
  }

  /**
   * Cierra el diálogo
   */
  onClose(): void {
    this.dialogRef.close();
  }
}

