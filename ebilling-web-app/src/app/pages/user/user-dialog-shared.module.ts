import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UpdateUserComponent } from './update-user/update-user.component';
import { MaterialModule } from '../../material/material.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { FlexLayoutModule } from '@angular/flex-layout';

/**
 * UserDialogSharedModule - Módulo compartido SOLO para el diálogo de usuario
 * 
 * Declara UpdateUserComponent para uso en modales de otros módulos
 * SIN incluir rutas para evitar conflictos con UserModule
 * 
 * IMPORTANTE: Este módulo declara UpdateUserComponent independientemente de UserModule
 * para evitar conflictos de routing cuando se importa en otros módulos.
 */
@NgModule({
  declarations: [UpdateUserComponent],
  imports: [
    CommonModule,
    MaterialModule,
    FormsModule,
    ReactiveFormsModule,
    FlexLayoutModule,
  ],
  exports: [UpdateUserComponent], // Exportar para uso en modales
})
export class UserDialogSharedModule {}

