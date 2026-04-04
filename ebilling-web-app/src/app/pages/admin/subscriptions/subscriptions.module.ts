import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { FlexLayoutModule } from '@angular/flex-layout';

// Angular CDK
import { CdkTableModule } from '@angular/cdk/table';

// Material Module
import { MaterialModule } from '../../../material/material.module';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';

// Shared Module
import { SharedModule } from '../../../_shared/shared.module';

// Guards
import { SessionValidationGuard } from '../../../_service/canActivate/session-validation.guard';
import { PermissionGuard } from '../../../_guard/permission.guard';

// Components
import { SubscriptionListComponent } from './subscription-list/subscription-list.component';
import { UpgradePlanDialogComponent } from './dialogs/upgrade-plan-dialog/upgrade-plan-dialog.component';
import { ExtendSubscriptionDialogComponent } from './dialogs/extend-subscription-dialog/extend-subscription-dialog.component';
import { AssignSubscriptionDialogComponent } from './dialogs/assign-subscription-dialog/assign-subscription-dialog.component';
import { SubscriptionDetailDialogComponent } from './dialogs/subscription-detail-dialog/subscription-detail-dialog.component';
import { CancelSubscriptionDialogComponent } from './dialogs/cancel-subscription-dialog/cancel-subscription-dialog.component';

const routes: Routes = [
  {
    path: '',
    component: SubscriptionListComponent,
    canActivate: [SessionValidationGuard, PermissionGuard],
    data: { permission: 'SUBSCRIPTION_MANAGEMENT' },
  },
];

/**
 * Módulo de Gestión de Suscripciones (Lazy Loaded)
 *
 * Carga solo cuando se navega a /admin/subscriptions
 * Configuración idéntica a CustomerModule que funciona correctamente
 */
@NgModule({
  declarations: [
    SubscriptionListComponent,
    UpgradePlanDialogComponent,
    ExtendSubscriptionDialogComponent,
    AssignSubscriptionDialogComponent,
    SubscriptionDetailDialogComponent,
    CancelSubscriptionDialogComponent,
  ],
  imports: [
    CommonModule,
    RouterModule.forChild(routes),
    CdkTableModule, // ← Explícito ANTES de MaterialModule
    MaterialModule,
    FormsModule,
    ReactiveFormsModule,
    FlexLayoutModule,
    MatDatepickerModule,
    MatNativeDateModule,
    SharedModule,
  ],
})
export class SubscriptionsModule {}
