import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { CarrierComponent } from './carrier.component';
import { UpdateCarrierComponent } from './update-carrier/update-carrier.component';
import { MaterialModule } from '../../material/material.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { FlexLayoutModule } from '@angular/flex-layout';
import { CanActiveUserGuard } from '../../_service/canActivate/can-active-user.guard';
import { PermissionGuard } from '../../_guard/permission.guard';
import { SharedModule } from '../../_shared/shared.module';

const routes: Routes = [
  {
    path: '',
    component: CarrierComponent,
    canActivate: [CanActiveUserGuard, PermissionGuard],
    data: { permission: 'CARRIER_CREATE' },
  },
];

/**
 * Módulo de Transportistas (Carriers) (Lazy Loaded)
 *
 * Carga solo cuando se navega a /carrier
 */
@NgModule({
  declarations: [CarrierComponent, UpdateCarrierComponent],
  imports: [
    CommonModule,
    RouterModule.forChild(routes),
    MaterialModule,
    FormsModule,
    ReactiveFormsModule,
    FlexLayoutModule,
    SharedModule,
  ],
})
export class CarrierModule {}
