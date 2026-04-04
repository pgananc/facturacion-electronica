import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import {
  SubscriptionAdmin,
  SubscriptionCancelRequest,
} from '../../../../../_model/subscription/subscription-admin';
import { SubscriptionAdminService } from '../../../../../_service/subscription/subscription-admin.service';
import { ToastService } from '../../../../../_service/notification/toast.service';
import { LoggerService } from '../../../../../_service/logger/logger.service';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-cancel-subscription-dialog',
  templateUrl: './cancel-subscription-dialog.component.html',
  styleUrls: ['./cancel-subscription-dialog.component.css'],
})
export class CancelSubscriptionDialogComponent {
  form: FormGroup;
  isLoading = false;

  // Razones predefinidas para cancelación
  cancelReasons = [
    { value: 'PAYMENT_FAILURE', label: 'Fallo en el pago' },
    { value: 'CUSTOMER_REQUEST', label: 'Solicitud del cliente' },
    { value: 'CONTRACT_TERMINATION', label: 'Terminación de contrato' },
    { value: 'NON_COMPLIANCE', label: 'Incumplimiento' },
    { value: 'OTHER', label: 'Otra razón' },
  ];

  constructor(
    public dialogRef: MatDialogRef<CancelSubscriptionDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: SubscriptionAdmin,
    private readonly fb: FormBuilder,
    private readonly subscriptionService: SubscriptionAdminService,
    private readonly toast: ToastService,
    private readonly logger: LoggerService
  ) {
    this.form = this.fb.group({
      reason: ['', Validators.required],
      cancelImmediately: [true, Validators.required],
      notes: [''],
    });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.markFormGroupTouched(this.form);
      this.toast.warning('Por favor completa todos los campos requeridos');
      return;
    }

    const request: SubscriptionCancelRequest = {
      reason: this.form.get('reason')?.value,
      cancelImmediately: this.form.get('cancelImmediately')?.value,
      notes: this.form.get('notes')?.value || undefined,
    };

    this.isLoading = true;
    this.logger.info('📤 Cancelando suscripción:', {
      empresa: this.data.businessName,
      razón: request.reason,
      cancelarInmediatamente: request.cancelImmediately,
    });

    this.subscriptionService
      .cancelSubscription(this.data.idCompany, request)
      .subscribe({
        next: () => {
          this.toast.success('Suscripción cancelada exitosamente');
          this.dialogRef.close(true);
        },
        error: (error: HttpErrorResponse) => {
          // Log completo del error para debugging (solo en consola)
          this.logger.error('Error al cancelar suscripción', {
            status: error?.status,
            url: error?.url,
            message: error?.error?.message,
            error: error,
          });
          this.isLoading = false;
          // Mostrar mensaje amigable al usuario usando httpError
          this.toast.httpError(error, 'cancelar la suscripción');
        },
      });
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }

  /**
   * Marca todos los campos del formulario como touched para mostrar errores
   */
  private markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach((key) => {
      const control = formGroup.get(key);
      control?.markAsTouched();
      if (control instanceof FormGroup) {
        this.markFormGroupTouched(control);
      }
    });
  }
}
