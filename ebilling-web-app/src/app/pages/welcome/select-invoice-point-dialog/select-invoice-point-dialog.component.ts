import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { InvoiceIssuancePoint } from '../../../_model/customer/invoice-issuance-point';
import { LoggerService } from '../../../_service/logger/logger.service';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-select-invoice-point-dialog',
  templateUrl: './select-invoice-point-dialog.component.html',
  styleUrls: ['./select-invoice-point-dialog.component.css'],
})
export class SelectInvoicePointDialogComponent {
  selectedPointArray: InvoiceIssuancePoint[] = [];

  constructor(
    private readonly dialogRef: MatDialogRef<SelectInvoicePointDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { points: InvoiceIssuancePoint[] },
    private readonly logger: LoggerService
  ) {}

  /**
   * Obtiene los puntos ordenados por código (numéricamente si es posible)
   */
  get sortedPoints(): InvoiceIssuancePoint[] {
    if (!this.data?.points) {
      return [];
    }

    return [...this.data.points].sort((a, b) => {
      const codeA = a.code || '';
      const codeB = b.code || '';

      // Intentar ordenar numéricamente si ambos son números
      const numA = parseInt(codeA, 10);
      const numB = parseInt(codeB, 10);

      if (!isNaN(numA) && !isNaN(numB)) {
        return numA - numB;
      }

      // Si no son números, ordenar alfabéticamente
      return codeA.localeCompare(codeB, undefined, {
        numeric: true,
        sensitivity: 'base',
      });
    });
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  onSelect(): void {
    const selected = this.selectedPointArray[0];
    if (selected) {
      // Guardar inmediatamente en localStorage ANTES de cerrar el diálogo
      // Esto asegura que el punto se guarde incluso si el componente welcome se destruye
      const invoicePointData = {
        id: selected.idInvoiceIssuancePoint,
        code: selected.code,
        establishmentId: selected.establishmentId,
      };
      
      localStorage.setItem(
        environment.ID_INVOICE_POINT,
        selected.idInvoiceIssuancePoint.toString()
      );
      localStorage.setItem(
        'INVOICE_POINT_DATA',
        JSON.stringify(invoicePointData)
      );
      
      this.dialogRef.close(selected);
    } else {
      this.logger.warn('No hay punto seleccionado, no se puede cerrar el diálogo');
    }
  }
}
