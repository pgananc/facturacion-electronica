import { Injectable } from '@angular/core';
import { CanActivate, Router, UrlTree } from '@angular/router';
import { Observable, of } from 'rxjs';
import { map, catchError, take } from 'rxjs/operators';
import {
  SessionValidationService,
  SessionValidationResponse,
} from '../auth/session-validation.service';
import { LoggerService } from '../logger/logger.service';
import { environment } from '../../../environments/environment';
import { ToastService } from '../notification/toast.service';

/**
 * SessionValidationGuard
 *
 * Guard que protege las rutas verificando que la sesión sea válida.
 * Valida contra el backend: IP + Browser + Token + Punto de emisión.
 *
 * Características:
 * - Usa caché para evitar llamadas repetidas al backend en cada navegación
 * - Muestra mensajes informativos al usuario cuando la sesión es inválida
 * - Redirige a login automáticamente si la sesión no es válida
 *
 * Uso en routing:
 * ```typescript
 * {
 *   path: 'billing',
 *   canActivate: [SessionValidationGuard],
 *   loadChildren: () => import('./billing/billing.module')
 * }
 * ```
 */
@Injectable({
  providedIn: 'root',
})
export class SessionValidationGuard implements CanActivate {
  constructor(
    private readonly sessionValidationService: SessionValidationService,
    private readonly router: Router,
    private readonly logger: LoggerService,
    private readonly toast: ToastService
  ) {}

  canActivate(): Observable<boolean | UrlTree> {
    // Verificación rápida: ¿hay token en localStorage?
    const token = localStorage.getItem(environment.TOKEN);

    if (!token) {
      this.logger.debug(
        '[SessionGuard] No hay token en localStorage, redirigiendo a login'
      );
      return of(this.router.createUrlTree(['/login']));
    }

    // Validar sesión con el backend (usa caché si está disponible)
    return this.sessionValidationService.validateSession().pipe(
      take(1),
      map((response: SessionValidationResponse) => {
        if (response.valid) {
          this.logger.debug(
            '[SessionGuard] ✅ Sesión válida, permitiendo acceso'
          );
          return true;
        } else {
          this.logger.warn(
            '[SessionGuard] ❌ Sesión inválida, redirigiendo a login',
            { reason: response.reason }
          );
          this.handleInvalidSession(response.reason);
          return this.router.createUrlTree(['/login']);
        }
      }),
      catchError((error) => {
        this.logger.error('[SessionGuard] Error al validar sesión:', error);
        this.sessionValidationService.clearSession();
        this.showMessage(
          'Error de conexión. Por favor inicie sesión nuevamente.'
        );
        return of(this.router.createUrlTree(['/login']));
      })
    );
  }

  /**
   * Maneja los diferentes casos de sesión inválida
   * Muestra un mensaje apropiado y limpia la sesión
   */
  private handleInvalidSession(
    reason?: SessionValidationResponse['reason']
  ): void {
    // Limpiar sesión
    this.sessionValidationService.clearSession();

    // Mostrar mensaje según la razón
    let message = 'Su sesión ha expirado. Por favor inicie sesión nuevamente.';

    switch (reason) {
      case 'ip_mismatch':
        message =
          'Se detectó un cambio de red. Por seguridad, debe iniciar sesión nuevamente.';
        break;
      case 'browser_mismatch':
        message =
          'Se detectó un cambio de navegador. Por seguridad, debe iniciar sesión nuevamente.';
        break;
      case 'token_expired':
        message = 'Su sesión ha expirado. Por favor inicie sesión nuevamente.';
        break;
      case 'invoice_point_released':
        message =
          'Su punto de emisión fue liberado por inactividad. Por favor inicie sesión nuevamente.';
        break;
      case 'session_not_found':
        message = 'No se encontró una sesión activa. Por favor inicie sesión.';
        break;
      case 'multiple_sessions':
        message =
          'Se detectaron múltiples sesiones activas. Por favor inicie sesión nuevamente.';
        break;
      case 'network_error':
        message =
          'Error de conexión con el servidor. Por favor intente nuevamente.';
        break;
    }

    this.showMessage(message);
  }

  /**
   * Muestra un mensaje al usuario usando ToastService
   */
  private showMessage(message: string): void {
    this.toast.warning(message);
  }
}
