import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { PlanResponse } from '../../../../../_model/subscription/subscription-admin';
import {
  CreatePlanRequest,
  UpdatePlanRequest,
} from '../../../../../_model/subscription/plan-management';
import { SubscriptionAdminService } from '../../../../../_service/subscription/subscription-admin.service';
import { ToastService } from '../../../../../_service/notification/toast.service';

@Component({
  selector: 'app-plan-form-dialog',
  templateUrl: './plan-form-dialog.component.html',
  styleUrls: ['./plan-form-dialog.component.css'],
})
export class PlanFormDialogComponent implements OnInit {
  form: FormGroup;
  isLoading = false;
  isEditMode = false;

  constructor(
    public dialogRef: MatDialogRef<PlanFormDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: PlanResponse | null,
    private readonly fb: FormBuilder,
    private readonly subscriptionService: SubscriptionAdminService,
    private readonly toast: ToastService
  ) {
    this.isEditMode = !!data;
    this.form = this.createForm();
  }

  ngOnInit(): void {
    if (this.data) {
      this.initForm();
    }
  }

  private createForm(): FormGroup {
    return this.fb.group({
      code: ['', [Validators.required, Validators.pattern(/^[A-Z_]+$/)]],
      name: ['', Validators.required],
      description: [''],
      priceAnnual: [0, [Validators.required, Validators.min(0)]],
      priceMonthly: [0, [Validators.required, Validators.min(0)]],
      documentLimitYearly: [null],
      userLimit: [null],
      establishmentLimit: [null],
      issuancePointLimit: [null],
      // Propiedades que existen en PlanResponse
      hasApiAccess: [false],
      hasMultiUser: [false],
      hasAdvancedReports: [false],
      hasElectronicInvoice: [false],
      hasCreditNote: [false],
      hasDebitNote: [false],
      hasRetention: [false],
      hasGuide: [false], // PlanResponse usa hasGuide, no hasGuides
      hasPrioritySupport: [false],
      hasCustomBranding: [false],
      hasAutomatedBackups: [false],
      isActive: [true],
      displayOrder: [0, [Validators.required, Validators.min(0)]],
    });
  }

  private initForm(): void {
    if (!this.data) return;

    // Convertir -1 a null para límites ilimitados
    const documentLimit =
      this.data.documentLimitYearly === -1
        ? null
        : this.data.documentLimitYearly;
    const userLimit = this.data.userLimit === -1 ? null : this.data.userLimit;
    const establishmentLimit =
      this.data.establishmentLimit === -1 ? null : this.data.establishmentLimit;
    const issuancePointLimit =
      this.data.issuancePointLimit === -1 ? null : this.data.issuancePointLimit;

    this.form.patchValue({
      code: this.data.code,
      name: this.data.name,
      description: this.data.description,
      priceAnnual: this.data.priceAnnual,
      priceMonthly: this.data.priceMonthly,
      documentLimitYearly: documentLimit,
      userLimit: userLimit,
      establishmentLimit: establishmentLimit,
      issuancePointLimit: issuancePointLimit,
      // Mapear propiedades que existen en PlanResponse
      hasApiAccess: this.data.hasApiAccess,
      hasMultiUser: this.data.hasMultiUser,
      hasAdvancedReports: this.data.hasAdvancedReports,
      hasElectronicInvoice: this.data.hasElectronicInvoice,
      hasCreditNote: this.data.hasCreditNote,
      hasDebitNote: this.data.hasDebitNote,
      hasRetention: this.data.hasRetention,
      hasGuide: this.data.hasGuide,
      hasPrioritySupport: this.data.hasPrioritySupport,
      hasCustomBranding: this.data.hasCustomBranding,
      hasAutomatedBackups: this.data.hasAutomatedBackups,
      isActive: this.data.isActive,
      displayOrder: this.data.displayOrder,
    });

    // Deshabilitar código en modo edición
    this.form.get('code')?.disable();
  }

  validateSave(): void {
    if (this.form.invalid) {
      this.markFormGroupTouched(this.form);
      this.toast.warning('Por favor completa todos los campos requeridos');
      return;
    }

    this.save();
  }

  private save(): void {
    if (this.isEditMode && this.data) {
      this.updatePlan();
    } else {
      this.createPlan();
    }
  }

  private createPlan(): void {
    const formValue = this.form.value;
    const request: CreatePlanRequest = {
      code: formValue.code.toUpperCase(),
      name: formValue.name,
      description: formValue.description || '',
      priceAnnual: formValue.priceAnnual,
      priceMonthly: formValue.priceMonthly,
      documentLimitYearly:
        formValue.documentLimitYearly === '' ||
        formValue.documentLimitYearly === null
          ? null
          : formValue.documentLimitYearly,
      userLimit:
        formValue.userLimit === '' || formValue.userLimit === null
          ? null
          : formValue.userLimit,
      establishmentLimit:
        formValue.establishmentLimit === '' ||
        formValue.establishmentLimit === null
          ? null
          : formValue.establishmentLimit,
      issuancePointLimit:
        formValue.issuancePointLimit === '' ||
        formValue.issuancePointLimit === null
          ? null
          : formValue.issuancePointLimit,
      // Propiedades que existen en PlanResponse
      hasApiAccess: formValue.hasApiAccess || false,
      hasMultiUser: formValue.hasMultiUser || false,
      hasAdvancedReports: formValue.hasAdvancedReports || false,
      hasElectronicInvoice: formValue.hasElectronicInvoice || false,
      hasCreditNote: formValue.hasCreditNote || false,
      hasDebitNote: formValue.hasDebitNote || false,
      hasRetention: formValue.hasRetention || false,
      hasGuide: formValue.hasGuide || false,
      hasPrioritySupport: formValue.hasPrioritySupport || false,
      hasCustomBranding: formValue.hasCustomBranding || false,
      hasAutomatedBackups: formValue.hasAutomatedBackups || false,
      isActive: formValue.isActive !== undefined ? formValue.isActive : true,
      displayOrder: formValue.displayOrder || 0,
    };

    this.isLoading = true;
    this.subscriptionService.createPlan(request).subscribe({
      next: () => {
        this.isLoading = false;
        this.toast.success('Plan creado exitosamente');
        this.dialogRef.close(true);
      },
      error: (error) => {
        this.isLoading = false;
        const errorMessage =
          error.error?.message || error.message || 'Error al crear el plan';
        this.toast.error(errorMessage);
      },
    });
  }

  private updatePlan(): void {
    if (!this.data) return;

    const formValue = this.form.value;
    const request: UpdatePlanRequest = {
      name: formValue.name,
      description: formValue.description || '',
      priceAnnual: formValue.priceAnnual,
      priceMonthly: formValue.priceMonthly,
      documentLimitYearly:
        formValue.documentLimitYearly === '' ||
        formValue.documentLimitYearly === null
          ? null
          : formValue.documentLimitYearly,
      userLimit:
        formValue.userLimit === '' || formValue.userLimit === null
          ? null
          : formValue.userLimit,
      establishmentLimit:
        formValue.establishmentLimit === '' ||
        formValue.establishmentLimit === null
          ? null
          : formValue.establishmentLimit,
      issuancePointLimit:
        formValue.issuancePointLimit === '' ||
        formValue.issuancePointLimit === null
          ? null
          : formValue.issuancePointLimit,
      // Propiedades que existen en PlanResponse
      hasApiAccess: formValue.hasApiAccess,
      hasMultiUser: formValue.hasMultiUser,
      hasAdvancedReports: formValue.hasAdvancedReports,
      hasElectronicInvoice: formValue.hasElectronicInvoice,
      hasCreditNote: formValue.hasCreditNote,
      hasDebitNote: formValue.hasDebitNote,
      hasRetention: formValue.hasRetention,
      hasGuide: formValue.hasGuide,
      hasPrioritySupport: formValue.hasPrioritySupport,
      hasCustomBranding: formValue.hasCustomBranding,
      hasAutomatedBackups: formValue.hasAutomatedBackups,
      isActive: formValue.isActive,
      displayOrder: formValue.displayOrder,
    };

    this.isLoading = true;
    this.subscriptionService.updatePlan(this.data.idPlan, request).subscribe({
      next: () => {
        this.isLoading = false;
        this.toast.success('Plan actualizado exitosamente');
        this.dialogRef.close(true);
      },
      error: (error) => {
        this.isLoading = false;
        const errorMessage =
          error.error?.message ||
          error.message ||
          'Error al actualizar el plan';
        this.toast.error(errorMessage);
      },
    });
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

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
