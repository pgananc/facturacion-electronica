import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { trigger, transition, style, animate } from '@angular/animations';
import { SubscriptionAdminService } from '../../../../../_service/subscription/subscription-admin.service';
import { AvailablePlan } from '../../../../../_model/subscription/subscription-admin';
import { LoggerService } from '../../../../../_service/logger/logger.service';
import { ToastService } from '../../../../../_service/notification/toast.service';
import { HttpErrorResponse } from '@angular/common/http';

/**
 * Dialog para asignar una nueva suscripción a una empresa
 */
@Component({
  selector: 'app-assign-subscription-dialog',
  templateUrl: './assign-subscription-dialog.component.html',
  styleUrls: ['./assign-subscription-dialog.component.css'],
  animations: [
    trigger('fadeIn', [
      transition(':enter', [
        style({ opacity: 0, transform: 'translateY(-10px)' }),
        animate(
          '300ms ease-out',
          style({ opacity: 1, transform: 'translateY(0)' })
        ),
      ]),
    ]),
  ],
})
export class AssignSubscriptionDialogComponent implements OnInit {
  assignForm: FormGroup;
  availablePlans: AvailablePlan[] = [];
  companies: Array<{ idCompany: number; businessName: string; ruc: string }> =
    [];
  isLoading = false;
  isLoadingCompanies = false;
  minDate: string = ''; // Fecha mínima para el date picker

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<AssignSubscriptionDialogComponent>,
    private subscriptionService: SubscriptionAdminService,
    private toast: ToastService,
    private logger: LoggerService
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.loadAvailablePlans();
    this.loadCompaniesWithoutSubscription();
  }

  /**
   * Inicializa el formulario
   */
  private initForm(): void {
    const today = new Date();
    // Calcular la fecha mínima (hoy)
    this.minDate = today.toISOString().split('T')[0];

    this.assignForm = this.fb.group({
      idCompany: [null, Validators.required],
      planCode: ['BASIC', Validators.required],
      startDate: [today.toISOString().split('T')[0], Validators.required],
      durationMonths: [
        12,
        [Validators.required, Validators.min(1), Validators.max(60)],
      ],
    });

    // Escuchar cambios en duración y fecha para recalcular
    this.assignForm.get('durationMonths')?.valueChanges.subscribe(() => {
      this.onDurationChange();
    });
    this.assignForm.get('startDate')?.valueChanges.subscribe(() => {
      this.onDateChange();
    });
  }

  /**
   * Carga los planes disponibles
   */
  private loadAvailablePlans(): void {
    this.isLoading = true;
    this.subscriptionService.getAvailablePlans().subscribe({
      next: (plans) => {
        this.availablePlans = plans;
        this.isLoading = false;
        this.logger.info('Planes disponibles cargados', plans);
      },
      error: (error: HttpErrorResponse) => {
        this.logger.error('Error al cargar planes disponibles', error);
        this.toast.httpError(error, 'cargar los planes disponibles');
        this.isLoading = false;
      },
    });
  }

  /**
   * Carga empresas sin suscripción (disponibles para asignar)
   */
  private loadCompaniesWithoutSubscription(): void {
    this.isLoadingCompanies = true;
    // Deshabilitar campo mientras carga
    this.assignForm.get('idCompany')?.disable();

    this.subscriptionService.getCompaniesWithoutSubscription().subscribe({
      next: (companies) => {
        this.companies = companies;
        this.isLoadingCompanies = false;
        // Habilitar campo después de cargar
        this.assignForm.get('idCompany')?.enable();

        this.logger.info(
          `Empresas sin suscripción cargadas: ${companies.length}`
        );

        // Si no hay empresas, mostrar mensaje
        if (companies.length === 0) {
          this.logger.warn(
            'No hay empresas disponibles para asignar suscripción'
          );
        }
      },
      error: (error: HttpErrorResponse) => {
        this.logger.error('Error al cargar empresas disponibles', error);
        this.toast.httpError(error, 'cargar las empresas disponibles');
        this.isLoadingCompanies = false;
        // Habilitar campo en caso de error
        this.assignForm.get('idCompany')?.enable();
        // Array vacío para que el dropdown muestre que no hay empresas
        this.companies = [];
      },
    });
  }

  /**
   * Obtiene el plan seleccionado
   */
  get selectedPlan(): AvailablePlan | undefined {
    const planCode = this.assignForm.get('planCode')?.value;
    return this.availablePlans.find((p) => p.code === planCode);
  }

  /**
   * Obtiene la empresa seleccionada
   */
  get selectedCompany():
    | { idCompany: number; businessName: string; ruc: string }
    | undefined {
    const idCompany = this.assignForm.get('idCompany')?.value;
    return this.companies.find((c) => c.idCompany === idCompany);
  }

  /**
   * Calcula la fecha de finalización basada en fecha de inicio y duración
   *
   * Lógica: Si empieza el día X del mes M, termina el día X del mes M+durationMonths
   * Ejemplos:
   * - Inicio: 23/12/2025, Duración: 1 mes → Fin: 23/01/2026
   * - Inicio: 23/12/2025, Duración: 12 meses → Fin: 23/12/2026
   * - Inicio: 31/01/2025, Duración: 1 mes → Fin: 28/02/2025 (ajuste automático de JS)
   */
  get calculatedEndDate(): Date | null {
    const startDate = this.assignForm.get('startDate')?.value;
    const durationMonths = this.assignForm.get('durationMonths')?.value;

    if (!startDate || !durationMonths) {
      return null;
    }

    // Crear fecha de inicio
    const start = new Date(startDate);

    // Crear fecha de fin sumando los meses (mantiene el mismo día del mes)
    const endDate = new Date(start);
    endDate.setMonth(endDate.getMonth() + durationMonths);

    // JavaScript maneja automáticamente los casos donde el día no existe
    // (ej: 31 de enero → 28/29 de febrero)

    return endDate;
  }

  /**
   * Calcula el costo total del período
   */
  get totalCost(): number {
    const plan = this.selectedPlan;
    const durationMonths = this.assignForm.get('durationMonths')?.value || 0;

    if (!plan || !durationMonths) {
      return 0;
    }

    return plan.price * durationMonths;
  }

  /**
   * Manejador de cambio de duración
   */
  onDurationChange(): void {
    // Trigger recalculation of computed properties
    const duration = this.assignForm.get('durationMonths')?.value;
    this.logger.debug('Duración actualizada:', duration, 'meses');
  }

  /**
   * Manejador de cambio de fecha
   */
  onDateChange(): void {
    // Trigger recalculation of computed properties
    const date = this.assignForm.get('startDate')?.value;
    this.logger.debug('Fecha actualizada:', date);
  }

  /**
   * Establece una duración predefinida (atajo rápido)
   */
  setDuration(months: number): void {
    this.assignForm.patchValue({ durationMonths: months });
    this.logger.info(
      `⚡ Atajo rápido: ${months} meses (${months / 12} año${
        months === 12 ? '' : 's'
      })`
    );
  }

  /**
   * Obtiene la duración en formato "X años Y meses"
   */
  getDurationYearsMonths(): string {
    const months = this.assignForm.get('durationMonths')?.value || 0;

    if (months === 0) return '';

    const years = Math.floor(months / 12);
    const remainingMonths = months % 12;

    if (years === 0) {
      return `${months} mes${months !== 1 ? 'es' : ''}`;
    } else if (remainingMonths === 0) {
      return `${years} año${years !== 1 ? 's' : ''}`;
    } else {
      return `${years} año${years !== 1 ? 's' : ''} y ${remainingMonths} mes${
        remainingMonths !== 1 ? 'es' : ''
      }`;
    }
  }

  /**
   * Formatea el RUC para mostrar en el dropdown
   * Sin espacios, solo con prefijo "RUC: "
   */
  formatRuc(ruc: string): string {
    if (!ruc) return '';
    return `RUC: ${ruc}`;
  }

  /**
   * Obtiene el texto a mostrar cuando la empresa está seleccionada (colapsado)
   * Solo muestra el nombre de la empresa sin el RUC para mejor legibilidad
   */
  getSelectedCompanyDisplay(): string {
    const idCompany = this.assignForm.get('idCompany')?.value;
    if (!idCompany) return '';

    const company = this.companies.find((c) => c.idCompany === idCompany);
    return company ? company.businessName : '';
  }

  /**
   * Obtiene el texto formateado del plan seleccionado para mostrar en el select (colapsado)
   * Formato: "Plan Básico • $29.99/mes • 500 doc/año"
   */
  getSelectedPlanDisplay(): string {
    const plan = this.selectedPlan;
    if (!plan) return '';

    const parts: string[] = [];

    // Nombre del plan
    parts.push(plan.name);

    // Precio mensual
    parts.push(`$${plan.price.toFixed(2)}/mes`);

    // Límite de documentos
    if (plan.documentLimit === -1) {
      parts.push('Ilimitado');
    } else {
      parts.push(`${plan.documentLimit.toLocaleString()} doc/año`);
    }

    // Unir con separador " • " (bullet point con espacios)
    return parts.join(' • ');
  }

  /**
   * Asigna la suscripción
   */
  onAssign(): void {
    if (this.assignForm.invalid) {
      this.markFormGroupTouched(this.assignForm);
      this.toast.warning('Por favor completa todos los campos correctamente');
      return;
    }

    if (this.companies.length === 0) {
      this.toast.warning(
        'No hay empresas disponibles para asignar suscripción'
      );
      return;
    }

    const formData = this.assignForm.value;
    this.isLoading = true;

    this.logger.info('📤 Asignando suscripción:', {
      empresa: this.selectedCompany?.businessName,
      plan: this.selectedPlan?.name,
      período: `${formData.durationMonths} meses`,
      costo: `$${this.totalCost.toFixed(2)}`,
    });

    // Preparar request según interface AssignSubscriptionRequest
    const request = {
      idCompany: formData.idCompany,
      planCode: formData.planCode,
      startDate: formData.startDate,
      durationMonths: formData.durationMonths,
    };

    this.subscriptionService.assignSubscription(request).subscribe({
      next: (response) => {
        this.logger.info('Suscripción asignada exitosamente', response);
        this.toast.success(
          `Suscripción "${this.selectedPlan?.name}" asignada exitosamente a "${this.selectedCompany?.businessName}"`
        );
        this.dialogRef.close(true);
      },
      error: (error: HttpErrorResponse) => {
        // Log completo del error para debugging (solo en consola)
        this.logger.error('Error al asignar suscripción', {
          status: error?.status,
          url: error?.url,
          message: error?.error?.message,
          error: error,
        });
        this.isLoading = false;
        this.toast.httpError(error, 'asignar la suscripción');
      },
    });
  }

  /**
   * Cancela y cierra el dialog
   */
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
