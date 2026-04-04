import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { ClientComponent } from './client/client.component';
import { UpdateClientComponent } from './client/update-client/update-client.component';
import { MaterialModule } from '../../material/material.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { FlexLayoutModule } from '@angular/flex-layout';
import { CanActiveUserGuard } from '../../_service/canActivate/can-active-user.guard';
import { PermissionGuard } from '../../_guard/permission.guard';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { SharedModule } from '../../_shared/shared.module';

const routes: Routes = [
  {
    path: '',
    component: ClientComponent,
    canActivate: [CanActiveUserGuard, PermissionGuard],
    data: { permission: 'CLIENT_CREATE' },
  },
];

/**
 * Módulo de Clientes (Lazy Loaded)
 *
 * Carga solo cuando se navega a /client
 */
@NgModule({
  declarations: [ClientComponent, UpdateClientComponent],
  imports: [
    CommonModule,
    RouterModule.forChild(routes),
    MaterialModule,
    FormsModule,
    ReactiveFormsModule,
    FlexLayoutModule,
    MatDatepickerModule,
    MatNativeDateModule,
    SharedModule,
  ],
})
export class CustomerModule {}
