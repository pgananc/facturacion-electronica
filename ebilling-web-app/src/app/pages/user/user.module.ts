import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { UserComponent } from './user.component';
import { UpdateUserComponent } from './update-user/update-user.component';
import { ChangePasswordComponent } from './change-password/change-password.component';
import { MaterialModule } from '../../material/material.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { FlexLayoutModule } from '@angular/flex-layout';
import { CanActiveUserGuard } from '../../_service/canActivate/can-active-user.guard';
import { PermissionGuard } from '../../_guard/permission.guard';
import { UserDialogSharedModule } from './user-dialog-shared.module';

const routes: Routes = [
  {
    path: '',
    component: UserComponent,
    children: [
      { path: 'new', component: UpdateUserComponent },
      { path: 'edit/:idUser', component: UpdateUserComponent },
    ],
    canActivate: [CanActiveUserGuard, PermissionGuard],
    data: { permission: 'USER_MANAGEMENT' },
  },
  {
    path: 'change-password',
    component: ChangePasswordComponent,
    canActivate: [CanActiveUserGuard],
    // No requiere validación de feature - todos pueden cambiar su contraseña
  },
];

/**
 * Módulo de Usuarios (Lazy Loaded)
 *
 * Carga solo cuando se navega a /user
 * Incluye componentes de gestión de usuarios y cambio de contraseña
 */
@NgModule({
  declarations: [UserComponent, ChangePasswordComponent],
  imports: [
    CommonModule,
    RouterModule.forChild(routes),
    MaterialModule,
    FormsModule,
    ReactiveFormsModule,
    FlexLayoutModule,
    UserDialogSharedModule, // Importar el módulo compartido que declara UpdateUserComponent
  ],
})
export class UserModule {}
