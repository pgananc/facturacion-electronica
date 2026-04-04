import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ConfirmDialogComponent } from './confirm-dialog/confirm-dialog.component';
import { ValidationSnackbarComponent } from './validation-snackbar/validation-snackbar.component';
import { MaterialModule } from '../material/material.module';
import { HasPermissionDirective } from '../_directives/has-permission.directive';

/**
 * SharedModule - Módulo para componentes y directivas compartidas
 *
 * Contiene componentes y directivas reutilizables en toda la aplicación
 */
@NgModule({
  declarations: [
    ConfirmDialogComponent,
    ValidationSnackbarComponent,
    HasPermissionDirective,
  ],
  imports: [CommonModule, MaterialModule, FormsModule],
  exports: [
    ConfirmDialogComponent,
    ValidationSnackbarComponent,
    HasPermissionDirective,
  ],
})
export class SharedModule {}
