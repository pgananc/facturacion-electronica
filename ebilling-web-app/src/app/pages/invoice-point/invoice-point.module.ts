import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { IssuancePointListComponent } from '../company/issuance-point-list/issuance-point-list.component';
import { MaterialModule } from '../../material/material.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { CanActiveUserGuard } from '../../_service/canActivate/can-active-user.guard';
import { PermissionGuard } from '../../_guard/permission.guard';
import { SharedModule } from '../../_shared/shared.module';

const routes: Routes = [
  {
    path: '',
    component: IssuancePointListComponent,
    canActivate: [CanActiveUserGuard, PermissionGuard],
    data: { permission: 'ESTABLISHMENT_MANAGEMENT' },
  },
];

@NgModule({
  imports: [
    CommonModule,
    RouterModule.forChild(routes),
    MaterialModule,
    FormsModule,
    ReactiveFormsModule,
    SharedModule,
  ],
})
export class InvoicePointModule {}
