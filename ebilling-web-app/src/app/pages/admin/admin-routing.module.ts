import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { CanActiveUserGuard } from '../../_service/canActivate/can-active-user.guard';
import { AdminGuard } from '../../_guard/admin.guard';
import { SessionValidationGuard } from '../../_service/canActivate/session-validation.guard';
import { PermissionGuard } from '../../_guard/permission.guard';

const routes: Routes = [
  {
    path: 'establishments',
    loadChildren: () =>
      import('./admin-establishment-list/admin-establishment-list.module').then(
        (m) => m.AdminEstablishmentListModule
      ),
    canActivate: [CanActiveUserGuard, AdminGuard],
  },
  {
    path: 'invoice-points',
    loadChildren: () =>
      import(
        './admin-issuance-point-list/admin-issuance-point-list.module'
      ).then((m) => m.AdminIssuancePointListModule),
    canActivate: [CanActiveUserGuard, AdminGuard],
  },
  {
    path: 'subscriptions',
    loadChildren: () =>
      import('./subscriptions/subscriptions.module').then(
        (m) => m.SubscriptionsModule
      ),
    canActivate: [SessionValidationGuard, PermissionGuard],
    data: { permission: 'SUBSCRIPTION_MANAGEMENT' },
  },
  {
    path: 'role-permissions',
    loadChildren: () =>
      import('./role-permissions/role-permissions.module').then(
        (m) => m.RolePermissionsModule
      ),
    canActivate: [SessionValidationGuard, PermissionGuard],
    data: { permission: 'ROLE_PERMISSION_MANAGEMENT' },
  },
  {
    path: 'menus',
    loadChildren: () =>
      import('./menus/menus.module').then((m) => m.MenusModule),
    canActivate: [SessionValidationGuard, PermissionGuard],
    data: { permission: 'MENU_MANAGEMENT' },
  },
  {
    path: 'plan-management',
    loadChildren: () =>
      import('./plan-management/plan-management.module').then(
        (m) => m.PlanManagementModule
      ),
    canActivate: [SessionValidationGuard, PermissionGuard],
    data: { permission: 'SUBSCRIPTION_MANAGEMENT' },
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class AdminRoutingModule {}
