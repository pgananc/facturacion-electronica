import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { GuideFormComponent } from './guide-form/guide-form.component';
import { GuideListComponent } from './guide-list/guide-list.component';
import { GuideTicketPrintComponent } from './guide-ticket-print/guide-ticket-print.component';
import { CanActiveUserGuard } from '../../_service/canActivate/can-active-user.guard';
import { PermissionGuard } from '../../_guard/permission.guard';

const routes: Routes = [
  {
    path: '',
    component: GuideListComponent,
    canActivate: [CanActiveUserGuard, PermissionGuard],
    data: { permission: 'DELIVERY_GUIDE_CREATE' },
  },
  {
    path: 'new',
    component: GuideFormComponent,
    canActivate: [CanActiveUserGuard, PermissionGuard],
    data: { permission: 'DELIVERY_GUIDE_CREATE' },
  },
  {
    path: 'edit/:id',
    component: GuideFormComponent,
    canActivate: [CanActiveUserGuard, PermissionGuard],
    data: { permission: 'DELIVERY_GUIDE_CREATE' },
  },
  {
    path: 'ticket/:id',
    component: GuideTicketPrintComponent,
    canActivate: [CanActiveUserGuard],
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class GuidesRoutingModule {}
