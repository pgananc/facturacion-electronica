import { Component, Inject, ViewEncapsulation } from '@angular/core';
import {
  MAT_SNACK_BAR_DATA,
  MatSnackBarRef,
} from '@angular/material/snack-bar';

export interface ValidationSnackbarData {
  message: string;
  type: 'error' | 'warning' | 'info' | 'success';
  icon?: string;
}

@Component({
  selector: 'app-validation-snackbar',
  templateUrl: './validation-snackbar.component.html',
  styleUrls: ['./validation-snackbar.component.css'],
  encapsulation: ViewEncapsulation.None, // ✅ Desactivar encapsulation para que los estilos se apliquen
})
export class ValidationSnackbarComponent {
  constructor(
    public snackBarRef: MatSnackBarRef<ValidationSnackbarComponent>,
    @Inject(MAT_SNACK_BAR_DATA) public data: ValidationSnackbarData
  ) {
    // Auto-cerrar después de 5 segundos
    setTimeout(() => {
      this.dismiss();
    }, 5000);
  }

  get icon(): string {
    if (this.data.icon) {
      return this.data.icon;
    }

    switch (this.data.type) {
      case 'error':
        return 'error';
      case 'warning':
        return 'warning';
      case 'info':
        return 'info';
      case 'success':
        return 'check_circle';
      default:
        return 'info';
    }
  }

  dismiss(): void {
    this.snackBarRef.dismiss();
  }
}
