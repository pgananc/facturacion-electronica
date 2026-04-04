import { Injectable } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  ValidationSnackbarComponent,
  ValidationSnackbarData,
} from '../../_shared/validation-snackbar/validation-snackbar.component';
import { LoggerService } from '../logger/logger.service';
import {
  extractUserFriendlyErrorMessage,
  sanitizeErrorMessage,
} from '../../_utils/error-message.util';

/**
 * ToastService - Servicio centralizado para mostrar notificaciones toast
 *
 * Usa el ValidationSnackbarComponent para mostrar toasts con estilo consistente
 * en todas las pantallas de la aplicación.
 *
 * Tipos disponibles:
 * - success: Verde, icono check_circle
 * - error: Rojo, icono error
 * - warning: Naranja, icono warning
 * - info: Azul, icono info
 *
 * Uso:
 * ```typescript
 * constructor(private toast: ToastService) {}
 *
 * this.toast.success('Operación exitosa');
 * this.toast.error('Error al procesar');
 * this.toast.warning('Atención requerida');
 * this.toast.info('Información importante');
 * ```
 */
@Injectable({
  providedIn: 'root',
})
export class ToastService {
  constructor(
    private readonly snackBar: MatSnackBar,
    private readonly logger: LoggerService
  ) {}

  /**
   * Muestra un toast de éxito (verde)
   */
  success(message: string, icon?: string): void {
    this.show(message, 'success', icon);
  }

  /**
   * Muestra un toast de error (rojo)
   */
  error(message: string, icon?: string): void {
    this.show(message, 'error', icon);
  }

  /**
   * Muestra un toast de advertencia (naranja)
   */
  warning(message: string, icon?: string): void {
    this.show(message, 'warning', icon);
  }

  /**
   * Muestra un toast informativo (azul)
   */
  info(message: string, icon?: string): void {
    this.show(message, 'info', icon);
  }

  /**
   * Muestra un toast personalizado
   */
  show(
    message: string,
    type: 'error' | 'warning' | 'info' | 'success' = 'info',
    icon?: string
  ): void {
    this.logger.debug(`🔔 Toast [${type.toUpperCase()}]:`, message);

    try {
      const data: ValidationSnackbarData = { message, type, icon };

      this.snackBar.openFromComponent(ValidationSnackbarComponent, {
        data,
        duration: 5000,
        horizontalPosition: 'end',
        verticalPosition: 'top',
        panelClass: ['validation-snackbar-container'],
      });
    } catch (error) {
      this.logger.error('Error mostrando toast:', error);

      // Fallback: snackbar básico de Material
      const panelClass =
        type === 'error'
          ? ['error-snackbar']
          : type === 'success'
          ? ['success-snackbar']
          : type === 'warning'
          ? ['warning-snackbar']
          : [];

      this.snackBar.open(message, 'Cerrar', {
        duration: 5000,
        horizontalPosition: 'end',
        verticalPosition: 'top',
        panelClass,
      });
    }
  }

  /**
   * Muestra un mensaje de error HTTP con formato amigable
   * Filtra automáticamente mensajes técnicos (excepciones Java, stack traces, etc.)
   *
   * @param error - Objeto HttpErrorResponse o error genérico
   * @param context - Contexto de la operación (ej: "crear nota de crédito", "guardar factura")
   * @param defaultMessage - Mensaje por defecto si no se puede extraer uno del error
   */
  httpError(
    error: any,
    context: string = 'realizar la operación',
    defaultMessage?: string
  ): void {
    // Log del error completo para debugging (solo en consola, no al usuario)
    this.logger.error('🔴 Error HTTP detectado:', {
      status: error?.status,
      url: error?.url,
      message: error?.error?.message || error?.message,
      error: error,
    });

    // Extraer mensaje amigable usando la utilidad
    const userMessage = extractUserFriendlyErrorMessage(error, context);

    // Si hay un mensaje por defecto y el mensaje extraído es genérico, usar el default
    const finalMessage =
      defaultMessage && userMessage.includes('contacta al administrador')
        ? defaultMessage
        : userMessage;

    this.error(finalMessage);
  }
}
