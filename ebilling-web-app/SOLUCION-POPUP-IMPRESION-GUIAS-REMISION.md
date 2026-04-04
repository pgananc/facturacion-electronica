# Solución: Popup de Impresión para Guías de Remisión

## 📋 Descripción del Problema

Cuando se firma e imprime una guía de remisión, si el response tiene `guideNumber`, se debe mostrar un popup similar al de facturación que permita imprimir el **ticket** y el **RIDE**.

## 🎯 Objetivo

Crear un diálogo de éxito personalizado para guías de remisión que:
1. Se muestre después de crear/enviar una guía exitosamente
2. Solo se muestre si el response contiene `guideNumber`
3. Permita imprimir el **ticket** (recibo)
4. Permita imprimir el **RIDE** (PDF)

## 📁 Archivos a Crear/Modificar

### 1. Nuevo Componente: `SuccessGuideDialogComponent`

**Ubicación:** `src/app/pages/guides/success-guide-dialog/`

#### `success-guide-dialog.component.ts`

```typescript
import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { LoggerService } from '../../../_service/logger/logger.service';
import { GuideService } from '../../../_service/guide/guide.service';
import { MatSnackBar } from '@angular/material/snack-bar';

export interface SuccessGuideDialogData {
  guideId: number;
  guideNumber?: string;
  recipientName?: string;
  startDate?: string;
  endDate?: string;
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
    private readonly snackBar: MatSnackBar
  ) {}

  /**
   * Imprime el recibo/ticket de la guía de remisión
   * Abre una nueva ventana con la página de impresión
   */
  printTicket(): void {
    this.logger.debug('🖨️ Imprimiendo ticket de guía de remisión desde diálogo de éxito', {
      guideId: this.data.guideId,
    });

    // Ruta para el ticket de guía (similar a /invoices/ticket/:id)
    // NOTA: Esta ruta debe ser creada en el routing module
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
          this.snackBar.open(
            'No se pudo abrir la ventana de impresión. Verifique que los popups estén permitidos.',
            'Cerrar',
            { duration: 3000 }
          );
        }
      }
    } catch (error) {
      this.logger.error('Error al abrir ventana de impresión', error);
      this.snackBar.open(
        'Error al abrir la ventana de impresión. Intente nuevamente.',
        'Cerrar',
        { duration: 3000 }
      );
    }
  }

  /**
   * Descarga e imprime el PDF de la guía de remisión (RIDE)
   * GET /api/guides/{id}/pdf
   */
  printRide(): void {
    if (!this.data.guideNumber) {
      this.snackBar.open(
        'No hay número de guía disponible para descargar',
        'Cerrar',
        { duration: 3000 }
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
              this.snackBar.open(
                error.message || 'Error al descargar el PDF',
                'Cerrar',
                { duration: 3000 }
              );
            } catch (e) {
              this.snackBar.open(
                'Error al descargar el PDF. Intente nuevamente.',
                'Cerrar',
                { duration: 3000 }
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
          this.snackBar.open(
            'PDF descargado. Ábralo para visualizar e imprimir.',
            'Cerrar',
            {
              duration: 3000,
            }
          );
          // Limpiar URL después de descargar
          setTimeout(() => {
            window.URL.revokeObjectURL(url);
          }, 1000);
        } else {
          // El PDF se abrirá en una nueva ventana donde el usuario puede imprimirlo
          this.snackBar.open(
            'PDF abierto. Use Ctrl+P o el botón de imprimir del navegador.',
            'Cerrar',
            {
              duration: 4000,
            }
          );
          // Limpiar URL después de un tiempo (cuando la ventana ya no la necesite)
          setTimeout(() => {
            window.URL.revokeObjectURL(url);
          }, 60000); // 60 segundos para dar tiempo a imprimir
        }
      },
      error: (error) => {
        this.logger.error('Error al descargar PDF de guía de remisión', error);
        this.snackBar.open(
          'Error al descargar el PDF. Intente nuevamente.',
          'Cerrar',
          { duration: 3000 }
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
```

#### `success-guide-dialog.component.html`

```html
<div class="dialog-container">
  <!-- Cabecera -->
  <mat-toolbar class="toolbar-primary-sm">
    <span>✅ Guía de Remisión Guardada</span>
    <span class="spacer"></span>
    <button
      type="button"
      mat-icon-button
      (click)="onClose()"
      aria-label="Cerrar"
    >
      <mat-icon>close</mat-icon>
    </button>
  </mat-toolbar>

  <mat-dialog-content class="dialog-content">
    <div class="success-content">
      <!-- Icono de éxito -->
      <div class="success-icon">
        <mat-icon class="success-icon-large">check_circle</mat-icon>
      </div>

      <!-- Mensaje principal -->
      <h2 class="success-title">¡Guía de remisión guardada correctamente!</h2>
      <p class="success-message">
        La guía de remisión ha sido guardada y enviada al SRI exitosamente.
      </p>

      <!-- Información de la guía -->
      <!-- Si hay número de guía, mostrar toda la información -->
      <div *ngIf="data.guideNumber">
        <div class="guide-info">
          <div class="info-row">
            <span class="info-label">Número de guía:</span>
            <span class="info-value">{{ data.guideNumber }}</span>
          </div>
        </div>

        <div class="guide-info" *ngIf="data.recipientName">
          <div class="info-row">
            <span class="info-label">Destinatario:</span>
            <span class="info-value">{{ data.recipientName }}</span>
          </div>
        </div>

        <div class="guide-info" *ngIf="data.startDate && data.endDate">
          <div class="info-row">
            <span class="info-label">Período de transporte:</span>
            <span class="info-value">
              {{ data.startDate }} - {{ data.endDate }}
            </span>
          </div>
        </div>
      </div>

      <!-- Si NO hay número de guía, solo mostrar el nombre del destinatario -->
      <div class="guide-info" *ngIf="!data.guideNumber && data.recipientName">
        <div class="info-row">
          <span class="info-label">Destinatario:</span>
          <span class="info-value">{{ data.recipientName }}</span>
        </div>
      </div>
    </div>
  </mat-dialog-content>

  <mat-dialog-actions class="dialog-actions">
    <button
      type="button"
      mat-raised-button
      color="primary"
      (click)="printTicket()"
      class="print-button"
    >
      <mat-icon>print</mat-icon>
      Recibo
    </button>
    <button
      *ngIf="data.guideNumber"
      type="button"
      mat-raised-button
      color="accent"
      (click)="printRide()"
      class="print-ride-button"
    >
      <mat-icon>description</mat-icon>
      RIDE
    </button>
  </mat-dialog-actions>
</div>
```

#### `success-guide-dialog.component.css`

```css
/* Reutilizar estilos similares a success-invoice-dialog */
@import '../../../_shared/shared-styles.css';

.dialog-container {
  display: flex;
  flex-direction: column;
  min-width: 400px;
  max-width: 500px;
}

.toolbar-primary-sm {
  background: linear-gradient(135deg, #0ea5e9 0%, #0284c7 100%);
  color: white;
  padding: 12px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.spacer {
  flex: 1 1 auto;
}

.dialog-content {
  padding: 24px;
  max-height: 60vh;
  overflow-y: auto;
}

.success-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.success-icon {
  margin-bottom: 16px;
}

.success-icon-large {
  font-size: 64px;
  width: 64px;
  height: 64px;
  color: #4caf50;
}

.success-title {
  font-size: 20px;
  font-weight: 600;
  color: #424242;
  margin: 0 0 8px 0;
}

.success-message {
  font-size: 14px;
  color: #757575;
  margin: 0 0 24px 0;
}

.guide-info {
  width: 100%;
  margin-bottom: 12px;
  padding: 12px;
  background-color: #f5f5f5;
  border-radius: 8px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.info-label {
  font-weight: 500;
  color: #666;
  font-size: 14px;
}

.info-value {
  font-weight: 600;
  color: #424242;
  font-size: 14px;
  text-align: right;
}

.dialog-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid #e0e0e0;
}

.print-button,
.print-ride-button {
  min-width: 120px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.print-button mat-icon,
.print-ride-button mat-icon {
  font-size: 20px;
  width: 20px;
  height: 20px;
}
```

### 2. Modificar `guide-form.component.ts`

#### Cambios en `createAndSendGuide()`:

```typescript
private createAndSendGuide(guideDto: GuideDto): void {
  this.guideService.createGuide(guideDto).subscribe({
    next: (response: GuideResponse) => {
      this.isProcessingGuide = false;
      if (response.code === 200) {
        // ✅ VERIFICAR: Respuesta exitosa y tiene guideNumber
        const guideId = response.idGuide || response.guideDto?.idGuide;
        const guideNumber = response.guideNumber || response.guideDto?.guideNumber;

        if (guideId && guideNumber) {
          // ✅ MOSTRAR DIALOG DE ÉXITO con opción de imprimir
          const recipient = this.recipientForm.getRawValue();
          const transfer = this.transferForm.getRawValue();

          const dialogData: SuccessGuideDialogData = {
            guideId: guideId,
            guideNumber: guideNumber,
            recipientName: recipient.name,
            startDate: transfer.startDate ? this.formatDate(transfer.startDate) : undefined,
            endDate: transfer.endDate ? this.formatDate(transfer.endDate) : undefined,
          };

          const dialogRef = this.dialog.open(SuccessGuideDialogComponent, {
            width: '450px',
            disableClose: false, // Permitir cerrar con ESC o clic fuera
            data: dialogData,
          });

          // Limpiar el formulario cuando se cierre el diálogo
          dialogRef.afterClosed().subscribe(() => {
            this.logger.debug(
              '🔄 Diálogo de éxito cerrado, navegando a lista de guías'
            );
            this.router.navigate(['/delivery-guides']);
          });

          this.logger.debug('✅ Diálogo de éxito mostrado', {
            guideId,
            guideNumber,
          });
        } else {
          // Respuesta exitosa pero sin guideNumber (caso raro)
          this.showSuccess('Guía guardada correctamente');
          this.router.navigate(['/delivery-guides']);
        }
      } else {
        this.errorMessage = response.message || 'Error al crear guía';
      }
      this.cdRef.detectChanges();
    },
    error: (error) => {
      this.isProcessingGuide = false;
      this.errorMessage = error.error?.message || 'Error al crear guía';
      this.cdRef.detectChanges();
    },
  });
}
```

#### Cambios en `updateAndSendDraft()`:

```typescript
private updateAndSendDraft(guideDto: GuideDto): void {
  if (!this.currentDraftId) {
    this.showError('ID de guía inválido');
    return;
  }

  // Primero actualizar el borrador
  this.guideService.updateGuide(this.currentDraftId, guideDto).subscribe({
    next: (updateResponse: GuideResponse) => {
      if (updateResponse.code === 200) {
        // Luego pedir contraseña y enviar al SRI
        const dialogRef = this.dialog.open(ConfirmDialogComponent, {
          width: '450px',
          maxWidth: '90vw',
          data: {
            title: 'Enviar al SRI',
            message:
              'Ingrese la contraseña de su firma electrónica para enviar la guía de remisión al SRI.',
            confirmText: 'Enviar',
            cancelText: 'Cancelar',
            type: 'info',
            showPasswordInput: true,
            passwordLabel: 'Contraseña de firma',
          },
        });

        dialogRef.afterClosed().subscribe((result: any) => {
          if (result?.confirmed && result?.password) {
            this.isProcessingGuide = true;
            this.errorMessage = '';
            this.cdRef.detectChanges();

            this.guideService
              .sendToSri(this.currentDraftId!, result.password)
              .pipe(
                catchError((error) => {
                  this.logger.error('Error sending draft to SRI', error);
                  this.isProcessingGuide = false;
                  this.showError(
                    error.error?.message ||
                      'Error al enviar al SRI. Intente nuevamente.'
                  );
                  this.cdRef.detectChanges();
                  return of(null);
                })
              )
              .subscribe((sendResponse: GuideResponse | null) => {
                this.isProcessingGuide = false;
                if (sendResponse && sendResponse.code === 200) {
                  // ✅ VERIFICAR: Respuesta exitosa y tiene guideNumber
                  const guideId = sendResponse.idGuide || sendResponse.guideDto?.idGuide;
                  const guideNumber = sendResponse.guideNumber || sendResponse.guideDto?.guideNumber;

                  if (guideId && guideNumber) {
                    // ✅ MOSTRAR DIALOG DE ÉXITO con opción de imprimir
                    const recipient = this.recipientForm.getRawValue();
                    const transfer = this.transferForm.getRawValue();

                    const dialogData: SuccessGuideDialogData = {
                      guideId: guideId,
                      guideNumber: guideNumber,
                      recipientName: recipient.name,
                      startDate: transfer.startDate ? this.formatDate(transfer.startDate) : undefined,
                      endDate: transfer.endDate ? this.formatDate(transfer.endDate) : undefined,
                    };

                    const dialogRef = this.dialog.open(SuccessGuideDialogComponent, {
                      width: '450px',
                      disableClose: false,
                      data: dialogData,
                    });

                    // Limpiar el formulario cuando se cierre el diálogo
                    dialogRef.afterClosed().subscribe(() => {
                      this.logger.debug(
                        '🔄 Diálogo de éxito cerrado, navegando a lista de guías'
                      );
                      this.router.navigate(['/delivery-guides']);
                    });

                    this.logger.debug('✅ Diálogo de éxito mostrado', {
                      guideId,
                      guideNumber,
                    });
                  } else {
                    // Respuesta exitosa pero sin guideNumber
                    this.showSuccess('Guía enviada exitosamente');
                    this.router.navigate(['/delivery-guides']);
                  }
                } else if (sendResponse) {
                  this.errorMessage =
                    sendResponse.message || 'Error al enviar al SRI';
                }
                this.cdRef.detectChanges();
              });
          } else {
            // Usuario canceló el envío
            this.isProcessingGuide = false;
            this.cdRef.detectChanges();
          }
        });
      } else {
        this.isProcessingGuide = false;
        this.errorMessage =
          updateResponse.message || 'Error al actualizar borrador';
        this.cdRef.detectChanges();
      }
    },
    error: (error) => {
      this.isProcessingGuide = false;
      this.errorMessage =
        error.error?.message || 'Error al actualizar borrador';
      this.cdRef.detectChanges();
    },
  });
}
```

#### Agregar imports necesarios:

```typescript
import { SuccessGuideDialogComponent, SuccessGuideDialogData } from '../success-guide-dialog/success-guide-dialog.component';
```

### 3. Registrar el componente en el módulo

**Archivo:** `src/app/pages/guides/guides.module.ts`

```typescript
import { SuccessGuideDialogComponent } from './success-guide-dialog/success-guide-dialog.component';

@NgModule({
  declarations: [
    // ... otros componentes
    SuccessGuideDialogComponent,
  ],
  // ...
})
export class GuidesModule {}
```

### 4. ✅ Ruta para el ticket de guía (YA CREADA)

**Archivo:** `src/app/pages/guides/guides-routing.module.ts`

```typescript
{
  path: 'ticket/:id',
  component: GuideTicketPrintComponent,
  canActivate: [CanActiveUserGuard],
}
```

**Componente creado:** `GuideTicketPrintComponent`
- **Ubicación:** `src/app/pages/guides/guide-ticket-print/`
- **Funcionalidad:** Similar a `TicketPrintComponent` pero adaptado para guías de remisión
- **Características:**
  - Muestra datos de la empresa
  - Muestra información de la guía (número, fecha, clave de acceso)
  - Muestra datos del destinatario
  - Muestra datos del transportista
  - Muestra datos del traslado (origen, destino, fechas, tipo de transporte, placa, motivo)
  - Muestra detalle de productos
  - Muestra referencias a documentos (facturas, notas de crédito/débito)
  - Auto-impresión al cargar
  - Cierre automático después de imprimir

## 📝 Notas Importantes

1. ✅ **Ruta del Ticket**: La ruta `/delivery-guides/ticket/:id` ya está creada y funcional.
   - Componente: `GuideTicketPrintComponent`
   - Ruta: `ticket/:id` en `guides-routing.module.ts`
   - Acceso: `/delivery-guides/ticket/:id`

2. **Método `downloadPdf()`**: Ya existe en `GuideService`, así que solo se necesita llamarlo desde el diálogo.

3. **Formato de Fechas**: El método `formatDate()` ya existe en `guide-form.component.ts` y se puede reutilizar.

4. **Estilos**: Los estilos del diálogo pueden reutilizar los de `success-invoice-dialog.component.css` o `success-debit-note-dialog.component.css`.

## ✅ Resumen de Cambios

1. ✅ Crear componente `SuccessGuideDialogComponent` con métodos `printTicket()` y `printRide()`
2. ✅ Modificar `createAndSendGuide()` para mostrar el diálogo cuando hay `guideNumber`
3. ✅ Modificar `updateAndSendDraft()` para mostrar el diálogo cuando hay `guideNumber`
4. ✅ Registrar el componente en `GuidesModule`
5. ✅ Crear componente `GuideTicketPrintComponent` para imprimir tickets de guías
6. ✅ Crear ruta `ticket/:id` en `guides-routing.module.ts`

## 🎨 Comportamiento Esperado

1. Usuario crea/envía una guía de remisión
2. Si la respuesta tiene `guideNumber`, se muestra el diálogo de éxito
3. El diálogo muestra:
   - Icono de éxito
   - Mensaje de confirmación
   - Información de la guía (número, destinatario, período)
   - Botón "Recibo" (siempre visible)
   - Botón "RIDE" (solo si hay `guideNumber`)
4. Al hacer clic en "Recibo", se abre una ventana con el ticket para imprimir
5. Al hacer clic en "RIDE", se descarga/abre el PDF de la guía

