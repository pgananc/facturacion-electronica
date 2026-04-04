import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { MenuListComponent } from './menu-list/menu-list.component';
import { SessionValidationGuard } from '../../../_service/canActivate/session-validation.guard';
import { PermissionGuard } from '../../../_guard/permission.guard';

const routes: Routes = [
  {
    path: '',
    component: MenuListComponent,
    canActivate: [SessionValidationGuard, PermissionGuard],
    data: { permission: 'MENU_MANAGEMENT' },
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class MenusRoutingModule {}
