import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { CompanyUserComponent } from './company-user.component';
import { MaterialModule } from '../../../material/material.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { FlexLayoutModule } from '@angular/flex-layout';
import { SharedModule } from '../../../_shared/shared.module';
import { CanActiveUserGuard } from '../../../_service/canActivate/can-active-user.guard';
import { PermissionGuard } from '../../../_guard/permission.guard';
import { UserDialogSharedModule } from '../../user/user-dialog-shared.module';

const routes: Routes = [
  {
    path: '',
    component: CompanyUserComponent,
    canActivate: [CanActiveUserGuard, PermissionGuard],
    data: { permission: 'COMPANY_USER_MANAGEMENT' },
  },
];

/**
 * Módulo de Usuarios por Compañía (Lazy Loaded)
 *
 * Carga solo cuando se navega a /company-users
 */
@NgModule({
  declarations: [CompanyUserComponent],
  imports: [
    CommonModule,
    RouterModule.forChild(routes),
    MaterialModule,
    FormsModule,
    ReactiveFormsModule,
    FlexLayoutModule,
    SharedModule,
    UserDialogSharedModule, // Módulo compartido solo para el diálogo (sin rutas)
  ],
})
export class CompanyUserModule {}
