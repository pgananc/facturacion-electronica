import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { PlanListComponent } from './plan-list/plan-list.component';
import { CanActiveUserGuard } from '../../../_service/canActivate/can-active-user.guard';
import { AdminGuard } from '../../../_guard/admin.guard';
import { SessionValidationGuard } from '../../../_service/canActivate/session-validation.guard';
import { PermissionGuard } from '../../../_guard/permission.guard';

const routes: Routes = [
  {
    path: '',
    component: PlanListComponent,
    canActivate: [SessionValidationGuard, PermissionGuard],
    data: { permission: 'SUBSCRIPTION_MANAGEMENT' }, // Mismo permiso que gestión de suscripciones
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class PlanManagementRoutingModule {}
