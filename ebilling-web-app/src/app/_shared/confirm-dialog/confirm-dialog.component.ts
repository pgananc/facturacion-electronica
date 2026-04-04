import { Component, Inject, ViewEncapsulation } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

export interface ConfirmDialogData {
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  type?: 'warning' | 'danger' | 'info';
  showPasswordInput?: boolean;
  passwordLabel?: string;
}

export interface ConfirmDialogResult {
  confirmed: boolean;
  password?: string;
}

@Component({
  selector: 'app-confirm-dialog',
  templateUrl: './confirm-dialog.component.html',
  styleUrls: ['./confirm-dialog.component.css'],
  encapsulation: ViewEncapsulation.None,
})
export class ConfirmDialogComponent {
  password = '';

  constructor(
    public dialogRef: MatDialogRef<ConfirmDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ConfirmDialogData
  ) {
    // Valores por defecto
    this.data.confirmText = this.data.confirmText || 'Confirmar';
    this.data.cancelText = this.data.cancelText || 'Cancelar';
    this.data.type = this.data.type || 'warning';
    this.data.passwordLabel = this.data.passwordLabel || 'Contraseña';
  }

  onCancel(): void {
    this.dialogRef.close(
      this.data.showPasswordInput ? { confirmed: false } : false
    );
  }

  onConfirm(): void {
    if (this.data.showPasswordInput) {
      this.dialogRef.close({
        confirmed: true,
        password: this.password,
      } as ConfirmDialogResult);
    } else {
      this.dialogRef.close(true);
    }
  }

  canConfirm(): boolean {
    if (this.data.showPasswordInput) {
      return this.password.trim().length > 0;
    }
    return true;
  }
}
