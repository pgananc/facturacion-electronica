import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { SubscriptionAdmin } from '../../../../../_model/subscription/subscription-admin';

@Component({
  selector: 'app-subscription-detail-dialog',
  templateUrl: './subscription-detail-dialog.component.html',
  styleUrls: ['./subscription-detail-dialog.component.css'],
})
export class SubscriptionDetailDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<SubscriptionDetailDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: SubscriptionAdmin
  ) {}

  /**
   * Calcula los días restantes hasta la expiración
   * Si la suscripción está cancelada y tiene cancelledAt, retorna 0 (no hay días restantes)
   */
  getDaysUntilExpiration(): number {
    // Si está cancelada y tiene fecha de cancelación, no hay días restantes
    if (
      this.data.subscriptionStatus === 'CANCELLED' &&
      this.data.cancelledAt
    ) {
      return 0;
    }

    const expiration = new Date(this.data.expiresAt);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    expiration.setHours(0, 0, 0, 0);

    const diffTime = expiration.getTime() - today.getTime();
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

    return diffDays;
  }

  /**
   * Obtiene la clase CSS para el estado de expiración
   * Si está cancelada con cancelledAt, retorna 'expired' (rojo)
   */
  getExpirationStatusClass(): string {
    // Si está cancelada y tiene fecha de cancelación, mostrar como expirada
    if (
      this.data.subscriptionStatus === 'CANCELLED' &&
      this.data.cancelledAt
    ) {
      return 'expired';
    }

    const days = this.getDaysUntilExpiration();
    if (days < 0) return 'expired';
    if (days <= 30) return 'warning';
    if (days <= 90) return 'caution';
    return 'ok';
  }

  /**
   * Obtiene el texto del estado de expiración
   * Si está cancelada con cancelledAt, muestra la fecha de cancelación
   */
  getExpirationStatusText(): string {
    // Si está cancelada y tiene fecha de cancelación, mostrar fecha de cancelación
    if (
      this.data.subscriptionStatus === 'CANCELLED' &&
      this.data.cancelledAt
    ) {
      const cancelledDate = new Date(this.data.cancelledAt);
      return `Cancelada el ${cancelledDate.toLocaleDateString('es-ES', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
      })}`;
    }

    const days = this.getDaysUntilExpiration();
    if (days < 0) return 'Expirada';
    if (days === 0) return 'Expira hoy';
    if (days === 1) return 'Expira mañana';
    if (days <= 30) return `Expira en ${days} días`;
    return `${days} días restantes`;
  }

  /**
   * Obtiene la clase CSS para el estado de uso
   */
  getUsageStatusClass(): string {
    const percent = this.data.percentUsed;
    if (percent >= 90) return 'critical';
    if (percent >= 75) return 'warning';
    return 'normal';
  }

  /**
   * Formatea el límite de documentos
   */
  getDocumentLimitText(): string {
    if (this.data.plan.documentLimitYearly === -1) {
      return 'Ilimitado';
    }
    return this.data.plan.documentLimitYearly.toLocaleString();
  }

  /**
   * Obtiene la clase CSS para el estado de la suscripción
   */
  getStatusClass(status: string): string {
    switch (status) {
      case 'ACTIVE':
        return 'status-active';
      case 'TRIAL':
        return 'status-trial';
      case 'EXPIRED':
        return 'status-expired';
      case 'CANCELLED':
        return 'status-cancelled';
      default:
        return '';
    }
  }

  onClose(): void {
    this.dialogRef.close();
  }
}
