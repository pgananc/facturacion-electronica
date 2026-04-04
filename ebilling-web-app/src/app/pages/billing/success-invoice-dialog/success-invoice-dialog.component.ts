import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { LoggerService } from '../../../_service/logger/logger.service';
import { InvoiceService } from '../../../_service/invoice/invoice.service';
import { ToastService } from '../../../_service/notification/toast.service';

export interface SuccessInvoiceDialogData {
  invoiceId: number;
  invoiceNumber?: string;
  clientName?: string;
  total?: number;
}

/**
 * SuccessInvoiceDialogComponent - Diálogo de éxito después de crear factura
 *
 * Muestra un mensaje de éxito y permite imprimir el recibo.
 * El diálogo permanece abierto hasta que el usuario lo cierre,
 * permitiendo múltiples intentos de impresión si cancela.
 */
@Component({
  selector: 'app-success-invoice-dialog',
  templateUrl: './success-invoice-dialog.component.html',
  styleUrls: ['./success-invoice-dialog.component.css'],
})
export class SuccessInvoiceDialogComponent {
  constructor(
    private readonly dialogRef: MatDialogRef<SuccessInvoiceDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: SuccessInvoiceDialogData,
    private readonly logger: LoggerService,
    private readonly invoiceService: InvoiceService,
    private readonly toast: ToastService
  ) {}

  /**
   * Imprime el recibo/ticket de la factura
   * Abre una nueva ventana con la página de impresión
   */
  printTicket(): void {
    this.logger.debug('🖨️ Imprimiendo ticket desde diálogo de éxito', {
      invoiceId: this.data.invoiceId,
    });

    const relativeUrl = `/invoices/ticket/${this.data.invoiceId}`;
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
          // Podrías mostrar un snackbar aquí si tienes acceso al servicio
        }
      }
    } catch (error) {
      this.logger.error('Error al abrir ventana de impresión', error);
    }
  }

  /**
   * Descarga e imprime el PDF de la factura
   * GET /api/invoice/{id}/pdf
   */
  printInvoice(): void {
    if (!this.data.invoiceNumber) {
      this.toast.warning(
        'No hay número de factura disponible para imprimir'
      );
      return;
    }

    this.logger.debug('📄 Descargando PDF de factura', {
      invoiceId: this.data.invoiceId,
      invoiceNumber: this.data.invoiceNumber,
    });

    this.invoiceService.downloadPdf(this.data.invoiceId).subscribe({
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
          const cleanFileName = this.data.invoiceNumber!.replace(
            /[^a-zA-Z0-9-]/g,
            '_'
          );
          link.download = `${cleanFileName}.pdf`;
          link.style.display = 'none';
          document.body.appendChild(link);
          link.click();
          document.body.removeChild(link);
          this.toast.info(
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
        this.logger.error('Error al descargar PDF de factura', error);
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

