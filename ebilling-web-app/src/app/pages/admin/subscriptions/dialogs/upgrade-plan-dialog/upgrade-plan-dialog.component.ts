import { Component, Inject, OnInit, OnDestroy } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import {
  SubscriptionAdmin,
  AvailablePlan,
  PlanChangeRequest,
} from '../../../../../_model/subscription/subscription-admin';
import { SubscriptionAdminService } from '../../../../../_service/subscription/subscription-admin.service';
import { ToastService } from '../../../../../_service/notification/toast.service';
import { HttpErrorResponse } from '@angular/common/http';
import { Subscription } from 'rxjs';
import { decodeToken } from '../../../../../_functions/functions';
import { LoggerService } from '../../../../../_service/logger/logger.service';
import { UserRoleService } from '../../../../../_service/auth/user-role.service';

@Component({
  selector: 'app-upgrade-plan-dialog',
  templateUrl: './upgrade-plan-dialog.component.html',
  styleUrls: ['./upgrade-plan-dialog.component.css'],
})
export class UpgradePlanDialogComponent implements OnInit, OnDestroy {
  form: FormGroup;
  availablePlans: AvailablePlan[] = [];
  isLoading = false;
  isLoadingPlans = false;
  private subscription?: Subscription;

  constructor(
    public dialogRef: MatDialogRef<UpgradePlanDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: SubscriptionAdmin,
    private readonly fb: FormBuilder,
    private readonly subscriptionService: SubscriptionAdminService,
    private readonly toast: ToastService,
    private readonly logger: LoggerService,
    private readonly userRoleService: UserRoleService
  ) {
    this.form = this.fb.group({
      newPlanCode: ['', Validators.required],
      notes: [''],
    });
  }

  ngOnInit(): void {
    this.loadAvailablePlans();
  }

  loadAvailablePlans(): void {
    this.isLoadingPlans = true;
    // Deshabilitar el control mientras se cargan los planes
    this.form.get('newPlanCode')?.disable();

    this.subscriptionService.getAvailablePlans().subscribe({
      next: (plans) => {
        // Filtrar para mostrar solo planes diferentes al actual
        this.availablePlans = plans.filter(
          (p) => p.code !== this.data.plan.code
        );
        this.isLoadingPlans = false;
        // Habilitar el control cuando los planes estén cargados
        this.form.get('newPlanCode')?.enable();
      },
      error: (error) => {
        console.error('Error cargando planes:', error);
        this.toast.error('Error al cargar los planes disponibles');
        this.isLoadingPlans = false;
        // Habilitar el control incluso si hay error para permitir reintento
        this.form.get('newPlanCode')?.enable();
      },
    });
  }

  getSelectedPlan(): AvailablePlan | null {
    const code = this.form.get('newPlanCode')?.value;
    return this.availablePlans.find((p) => p.code === code) || null;
  }

  /**
   * Formatea una fecha al formato ISO LocalDateTime requerido por el backend
   * Formato: "YYYY-MM-DDTHH:mm:ss"
   */
  formatDateTimeForBackend(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    const seconds = String(date.getSeconds()).padStart(2, '0');
    return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`;
  }


  onSubmit(): void {
    if (this.form.invalid) {
      this.markFormGroupTouched(this.form);
      this.toast.warning('Por favor completa todos los campos requeridos');
      return;
    }

    const formValue = this.form.value;
    
    // paymentDate se envía automáticamente con la fecha actual (requerido por backend)
    const now = new Date();
    const paymentDate = this.formatDateTimeForBackend(now);
    
    const request: PlanChangeRequest = {
      newPlanCode: formValue.newPlanCode,
      paymentDate: paymentDate, // Fecha actual automática
      notes: formValue.notes?.trim() || undefined,
    };

    this.isLoading = true;
    
    // Verificar roles antes de enviar la petición (usando servicio centralizado)
    const roles = this.userRoleService.getCurrentRolesSync();
    const isSuperAdmin = roles.includes('super_admin');
    this.logger.debug('🔍 [UpgradePlanDialog] Verificando permisos antes de upgrade:', {
      roles: roles,
      isSuperAdmin: isSuperAdmin,
      idCompany: this.data.idCompany,
      newPlanCode: request.newPlanCode,
    });
    
    // Cancelar suscripción anterior si existe
    if (this.subscription) {
      this.subscription.unsubscribe();
    }

    this.subscription = this.subscriptionService
      .changePlan(this.data.idCompany, request)
      .subscribe({
        next: () => {
          this.isLoading = false;
          this.toast.success('Plan actualizado exitosamente');
          this.dialogRef.close(true);
        },
        error: (error: HttpErrorResponse | Error) => {
          this.isLoading = false;
          
          // Manejar diferentes tipos de errores
          if (error instanceof Error && error.message.includes('tardó demasiado')) {
            this.toast.error(
              'La petición tardó demasiado. Por favor verifica la conexión con el servidor.'
            );
          } else if (error instanceof HttpErrorResponse) {
            // Usar toast.httpError para manejar errores HTTP de forma consistente
            // El logger ya registra el error automáticamente
            this.toast.httpError(error, 'cambiar el plan');
          } else {
            this.toast.error('Error al cambiar el plan');
          }
        },
      });
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

  onCancel(): void {
    // Cancelar suscripción si está en progreso
    if (this.subscription) {
      this.subscription.unsubscribe();
      this.isLoading = false;
    }
    this.dialogRef.close(false);
  }

  ngOnDestroy(): void {
    // Limpiar suscripción al destruir el componente
    if (this.subscription) {
      this.subscription.unsubscribe();
    }
  }
}
