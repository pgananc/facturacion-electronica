import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { LoggerService } from '../../../_service/logger/logger.service';
import { GuideService } from '../../../_service/guide/guide.service';
import { ToastService } from '../../../_service/notification/toast.service';

export interface SuccessGuideDialogData {
  guideId: number;
  guideNumber?: string;
  recipientName?: string;
  startDate?: string;
  endDate?: string;
  authorizationStatusSri?: string;
}

/**
 * SuccessGuideDialogComponent - Diálogo de éxito después de crear/enviar guía de remisión
 *
 * Muestra un mensaje de éxito y permite imprimir el ticket y el RIDE.
 */
@Component({
  selector: 'app-success-guide-dialog',
  templateUrl: './success-guide-dialog.component.html',
  styleUrls: ['./success-guide-dialog.component.css'],
})
export class SuccessGuideDialogComponent {
  constructor(
    private readonly dialogRef: MatDialogRef<SuccessGuideDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: SuccessGuideDialogData,
    private readonly logger: LoggerService,
    private readonly guideService: GuideService,
    private readonly toast: ToastService
  ) {}

  /**
   * Imprime el recibo/ticket de la guía de remisión
   * Abre una nueva ventana con la página de impresión
   */
  printTicket(): void {
    this.logger.debug('🖨️ Imprimiendo ticket de guía de remisión desde diálogo de éxito', {
      guideId: this.data.guideId,
    });

    // Ruta para el ticket de guía
    const relativeUrl = `/delivery-guides/ticket/${this.data.guideId}`;
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
   * Descarga e imprime el PDF de la guía de remisión (RIDE)
   * GET /api/guides/{id}/pdf
   */
  printRide(): void {
    if (!this.data.guideNumber) {
      this.toast.warning(
        'No hay número de guía disponible para descargar'
      );
      return;
    }

    this.logger.debug('📄 Descargando PDF de guía de remisión (RIDE)', {
      guideId: this.data.guideId,
      guideNumber: this.data.guideNumber,
    });

    this.guideService.downloadPdf(this.data.guideId).subscribe({
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
          const cleanFileName = this.data.guideNumber!.replace(
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
        this.logger.error('Error al descargar PDF de guía de remisión', error);
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

