import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { SettingComponent } from './setting.component';
import { MaterialModule } from '../../material/material.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { FlexLayoutModule } from '@angular/flex-layout';
import { CanActiveUserGuard } from '../../_service/canActivate/can-active-user.guard';
import { PermissionGuard } from '../../_guard/permission.guard';

const routes: Routes = [
  {
    path: '',
    component: SettingComponent,
    canActivate: [CanActiveUserGuard, PermissionGuard],
    data: { permission: 'COMPANY_SETTINGS' },
  },
];

/**
 * Módulo de Configuración (Lazy Loaded)
 *
 * Carga solo cuando se navega a /setting
 * Maneja configuración de firma digital y certificados
 */
@NgModule({
  declarations: [SettingComponent],
  imports: [
    CommonModule,
    RouterModule.forChild(routes),
    MaterialModule,
    FormsModule,
    ReactiveFormsModule,
    FlexLayoutModule,
  ],
})
export class SettingModule {}
