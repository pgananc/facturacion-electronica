import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { RolePermissionsContainerComponent } from './role-permissions-container/role-permissions-container.component';
import { SessionValidationGuard } from '../../../_service/canActivate/session-validation.guard';
import { PermissionGuard } from '../../../_guard/permission.guard';

const routes: Routes = [
  {
    path: '',
    component: RolePermissionsContainerComponent,
    canActivate: [SessionValidationGuard, PermissionGuard],
    data: { permission: 'ROLE_PERMISSION_MANAGEMENT' },
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class RolePermissionsRoutingModule {}
