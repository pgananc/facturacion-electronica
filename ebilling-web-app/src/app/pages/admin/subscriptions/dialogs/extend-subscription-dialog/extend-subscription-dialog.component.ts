import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import {
  SubscriptionAdmin,
  SubscriptionExtendRequest,
} from '../../../../../_model/subscription/subscription-admin';
import { SubscriptionAdminService } from '../../../../../_service/subscription/subscription-admin.service';
import { ToastService } from '../../../../../_service/notification/toast.service';

@Component({
  selector: 'app-extend-subscription-dialog',
  templateUrl: './extend-subscription-dialog.component.html',
  styleUrls: ['./extend-subscription-dialog.component.css'],
})
export class ExtendSubscriptionDialogComponent {
  form: FormGroup;
  isLoading = false;

  monthOptions = [1, 3, 6, 12, 24];

  constructor(
    public dialogRef: MatDialogRef<ExtendSubscriptionDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: SubscriptionAdmin,
    private readonly fb: FormBuilder,
    private readonly subscriptionService: SubscriptionAdminService,
    private readonly toast: ToastService
  ) {
    this.form = this.fb.group({
      extensionMonths: [6, [Validators.required, Validators.min(1)]], // 🔧 Campo correcto
      reason: [''],
    });
  }

  /**
   * Calcula la nueva fecha de expiración basada en la fecha de inicio
   * Mantiene el mismo día del mes (mismo principio que asignar suscripción)
   * 
   * IMPORTANTE: El backend calcula desde la fecha de inicio original + duración total,
   * manteniendo el mismo día del mes. Por eso siempre usamos la fecha de inicio como base.
   * 
   * Ejemplo:
   * - Inicio: 23/12/2025
   * - Duración original: 1 mes → Debería expirar: 23/01/2026
   * - Extensión: 1 mes → Nueva duración total: 2 meses → Nueva expiración: 23/02/2026
   * 
   * Si la fecha de expiración actual está mal (ej: 22/01/2026), el backend la corregirá
   * al calcular desde la fecha de inicio con la nueva duración total.
   */
  getNewExpirationDate(): Date {
    const extensionMonths = this.form.get('extensionMonths')?.value || 0;
    
    // Siempre usar la fecha de inicio como base (no la fecha de expiración actual)
    const startDate = new Date(this.data.startDate);
    
    // Extraer el día del mes de inicio para mantenerlo
    const startDay = startDate.getDate();
    
    // Calcular la duración original aproximada desde inicio hasta expiración actual
    const currentExpiration = new Date(this.data.expiresAt);
    const currentStart = new Date(this.data.startDate);
    
    // Calcular diferencia en meses (años * 12 + meses)
    const yearsDiff = currentExpiration.getFullYear() - currentStart.getFullYear();
    const monthsDiff = currentExpiration.getMonth() - currentStart.getMonth();
    let originalMonths = yearsDiff * 12 + monthsDiff;
    
    // Si la diferencia de días es significativa, ajustar
    // (esto maneja casos donde la fecha de expiración está mal calculada)
    const daysDiff = currentExpiration.getDate() - currentStart.getDate();
    if (daysDiff < -15) {
      // Si la fecha de expiración es mucho antes del día de inicio, probablemente está mal
      // Asumir que debería ser el mismo día del mes siguiente
      originalMonths = Math.max(1, originalMonths);
    }
    
    // Nueva duración total = meses originales + extensión
    const totalMonths = originalMonths + extensionMonths;
    
    // Calcular nueva fecha desde el inicio con la nueva duración total
    const newDate = new Date(startDate);
    newDate.setMonth(newDate.getMonth() + totalMonths);
    
    // JavaScript setMonth() mantiene el día automáticamente, pero si el mes destino
    // no tiene ese día (ej: 31 de enero → febrero), ajusta al último día del mes
    
    return newDate;
  }

  onSubmit(): void {
    if (this.form.invalid) {
      return;
    }

    const request: SubscriptionExtendRequest = this.form.value;

    this.isLoading = true;
    this.subscriptionService
      .extendSubscription(this.data.idCompany, request)
      .subscribe({
        next: () => {
          this.toast.success('Suscripción extendida exitosamente');
          this.dialogRef.close(true);
        },
        error: (error) => {
          console.error('Error extendiendo suscripción:', error);
          this.toast.error('Error al extender la suscripción');
          this.isLoading = false;
        },
      });
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }
}
