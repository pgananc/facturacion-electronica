import { Injectable, OnDestroy, Injector } from '@angular/core';
import { InvoicePointService } from './invoice-point.service';
import { LoggerService } from '../logger/logger.service';
import { ToastService } from '../notification/toast.service';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';

/**
 * Servicio global para gestionar sesiones de punto de emisión con heartbeat automático.
 *
 * Este servicio mantiene activa la sesión del punto de emisión enviando heartbeats
 * periódicos al servidor (cada 2 minutos). Si el servidor no recibe heartbeat por
 * más de 10 minutos, libera automáticamente el punto de emisión.
 *
 * IMPORTANTE: El heartbeat se envía SOLO si el usuario ha estado activo. Se detecta
 * actividad mediante eventos de mouse, teclado y navegación.
 *
 * Uso:
 * 1. Importar en el componente donde se asigna el punto de emisión (welcome)
 * 2. Llamar a startHeartbeat() inmediatamente después de asignar el punto
 * 3. Llamar a stopHeartbeat() cuando se libere el punto o se cierre sesión
 *
 * El servicio es singleton (providedIn: 'root'), por lo que solo hay una instancia
 * en toda la aplicación.
 */
@Injectable({
  providedIn: 'root',
})
export class InvoicePointSessionService implements OnDestroy {
  private heartbeatInterval: any = null;
  private readonly HEARTBEAT_INTERVAL_MS = 2 * 60 * 1000; // 2 minutos
  private isHeartbeatActive = false;

  // Detección de actividad del usuario
  private lastActivity: number = Date.now();
  private activityListeners: Array<() => void> = [];
  private readonly INACTIVITY_THRESHOLD_MS = 5 * 60 * 1000; // 5 minutos (debe ser menor que el timeout del backend de 10 minutos)

  // Contador de heartbeats para debugging
  private heartbeatCount = 0;
  private lastHeartbeatSent: Date | null = null;

  constructor(
    private readonly invoicePointService: InvoicePointService,
    private readonly injector: Injector,
    private readonly logger: LoggerService,
    private readonly toast: ToastService,
    private readonly router: Router
  ) {
    this.logger.debug('💓 InvoicePointSessionService inicializado');
  }

  /**
   * Lifecycle hook de Angular - Se ejecuta cuando el servicio se destruye
   */
  ngOnDestroy(): void {
    this.stopHeartbeat();
    this.removeActivityListeners();
  }

  /**
   * Registra los event listeners para detectar actividad del usuario
   * Se detecta: clicks, teclas presionadas, scroll, y movimiento del mouse
   */
  private registerActivityListeners(): void {
    // Si ya hay listeners registrados, no duplicar
    if (this.activityListeners.length > 0) {
      return;
    }

    const updateActivity = () => {
      this.lastActivity = Date.now();
      this.logger.debug(
        `👆 Actividad detectada. Última actividad: ${new Date(
          this.lastActivity
        ).toLocaleTimeString()}`
      );
    };

    // Eventos de actividad del usuario
    const events = ['mousedown', 'keydown', 'scroll', 'touchstart'];

    events.forEach((eventName) => {
      const listener = updateActivity.bind(this);
      window.addEventListener(eventName, listener, true); // true = capture phase
      this.activityListeners.push(() => {
        window.removeEventListener(eventName, listener, true);
      });
    });

    this.logger.debug(
      '✅ Activity listeners registrados para detectar actividad del usuario'
    );
  }

  /**
   * Remueve todos los event listeners de actividad
   */
  private removeActivityListeners(): void {
    this.activityListeners.forEach((removeListener) => removeListener());
    this.activityListeners = [];
    this.logger.debug('🗑️ Activity listeners removidos');
  }

  /**
   * Verifica si el usuario ha estado activo recientemente
   * @returns true si el usuario ha estado activo en los últimos INACTIVITY_THRESHOLD_MS
   */
  private isUserActive(): boolean {
    const timeSinceLastActivity = Date.now() - this.lastActivity;
    const isActive = timeSinceLastActivity < this.INACTIVITY_THRESHOLD_MS;

    if (!isActive) {
      this.logger.warn(
        `⏰ Usuario inactivo por ${Math.floor(
          timeSinceLastActivity / 60000
        )} minutos (umbral: ${Math.floor(
          this.INACTIVITY_THRESHOLD_MS / 60000
        )} minutos)`
      );
    }

    return isActive;
  }

  /**
   * Inicia el envío periódico de heartbeat al servidor
   * Se debe llamar inmediatamente después de que el usuario asigne un punto de emisión
   *
   * @returns true si el heartbeat se inició exitosamente, false si ya estaba activo
   */
  startHeartbeat(): boolean {
    // Verificar que haya un punto de emisión asignado
    const invoicePointId = localStorage.getItem(environment.ID_INVOICE_POINT);

    if (!invoicePointId) {
      this.logger.warn(
        '⚠️  No hay punto de emisión asignado, no se puede iniciar heartbeat'
      );
      return false;
    }

    // Si ya hay un intervalo activo, no crear otro (evitar múltiples intervalos)
    if (this.heartbeatInterval || this.isHeartbeatActive) {
      this.logger.debug('⚠️  Heartbeat ya está activo, no se inicia otro');
      return false;
    }

    this.isHeartbeatActive = true;

    // Resetear el contador de actividad
    this.lastActivity = Date.now();

    // Registrar los listeners para detectar actividad del usuario
    this.registerActivityListeners();

    // Enviar el primer heartbeat inmediatamente
    this.sendHeartbeat();

    // Configurar envío periódico cada 2 minutos
    this.heartbeatInterval = setInterval(() => {
      const timeSinceLastActivity = Date.now() - this.lastActivity;
      const minutesSinceActivity = Math.floor(timeSinceLastActivity / 60000);

      this.logger.debug(
        `⏱️  Verificando actividad - Últimos ${minutesSinceActivity} minutos desde última interacción`
      );

      // SOLO enviar heartbeat si el usuario ha estado activo
      if (this.isUserActive()) {
        this.sendHeartbeat();
      } else {
        this.logger.warn(
          `⏰ Usuario inactivo por ${minutesSinceActivity} minutos, NO se envía heartbeat. El backend liberará la sesión si no hay actividad.`
        );
      }
    }, this.HEARTBEAT_INTERVAL_MS);

    this.logger.debug(
      `💓 Heartbeat iniciado (cada ${
        this.HEARTBEAT_INTERVAL_MS / 60000
      } minutos). Detectando actividad del usuario...`
    );

    return true;
  }

  /**
   * Detiene el envío de heartbeat
   * Se debe llamar cuando:
   * - El usuario libera el punto de emisión manualmente
   * - El usuario cierra sesión
   * - La aplicación detecta que la sesión ya no existe (404 del servidor)
   */
  stopHeartbeat(): void {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
      this.heartbeatInterval = null;
      this.isHeartbeatActive = false;
      this.removeActivityListeners();
      this.logger.debug('🛑 Heartbeat detenido y activity listeners removidos');
    }
  }

  /**
   * Verifica si el heartbeat está activo
   */
  isActive(): boolean {
    return this.isHeartbeatActive && this.heartbeatInterval !== null;
  }

  /**
   * Envía un heartbeat al servidor para mantener activa la sesión
   * Si la sesión no existe (404), detiene el heartbeat automáticamente
   *
   * Este método es privado y se llama automáticamente por el intervalo
   */
  private sendHeartbeat(): void {
    this.heartbeatCount++;
    const now = new Date();
    this.lastHeartbeatSent = now;

    const timeSinceLastActivity = Date.now() - this.lastActivity;
    const secondsSinceActivity = Math.floor(timeSinceLastActivity / 1000);

    this.logger.debug(
      `💓 Enviando heartbeat #${
        this.heartbeatCount
      } a las ${now.toLocaleTimeString()} (última actividad hace ${secondsSinceActivity}s)`
    );

    this.invoicePointService.sendHeartbeat().subscribe({
      next: () => {
        this.logger.debug(
          `✅ Heartbeat #${this.heartbeatCount} recibido exitosamente por el backend`
        );
      },
      error: (err: HttpErrorResponse) => {
        if (err.status === 404) {
          // No hay sesión activa en el backend
          this.logger.error(
            `❌ SESIÓN PERDIDA - Heartbeat #${this.heartbeatCount} falló con 404. ` +
              `Total heartbeats enviados: ${this.heartbeatCount}. ` +
              `Último heartbeat exitoso: ${
                this.lastHeartbeatSent
                  ? this.lastHeartbeatSent.toLocaleTimeString()
                  : 'N/A'
              }. ` +
              `Última actividad del usuario: ${new Date(
                this.lastActivity
              ).toLocaleTimeString()}`
          );

          // Verificar si el usuario ha estado activo recientemente (últimos 2 minutos)
          const timeSinceLastActivity = Date.now() - this.lastActivity;
          const wasRecentlyActive = timeSinceLastActivity < 2 * 60 * 1000; // 2 minutos

          if (wasRecentlyActive) {
            // Usuario activo pero sesión perdida - probablemente el backend reinició
            this.logger.warn(
              '⚠️ Usuario estaba activo pero sesión perdida. Posible reinicio del backend.'
            );
            this.logger.warn(
              '⚠️ NO cerrando sesión del usuario. Deteniendo solo el heartbeat.'
            );

            // Solo detener el heartbeat, NO cerrar sesión completa
            this.stopHeartbeat();

            // Notificar al usuario pero NO cerrar sesión
            this.toast.warning(
              'La sesión del punto de emisión se perdió (posible reinicio del servidor). ' +
                'Por favor, seleccione nuevamente el punto de emisión desde el menú.'
            );

            // Limpiar SOLO los datos del punto de emisión, NO el token de login
            this.logger.warn('🧹 [InvoicePointSessionService] Limpiando INVOICE_POINT_DATA del localStorage porque sesión se perdió');
            localStorage.removeItem(environment.ID_INVOICE_POINT);
            localStorage.removeItem('INVOICE_POINT_DATA');
            
            // ✅ Verificar que se limpió
            const afterCleanup = localStorage.getItem('INVOICE_POINT_DATA');
            this.logger.debug('🔍 [InvoicePointSessionService] Después de limpiar:', {
              stillExists: !!afterCleanup,
              value: afterCleanup,
            });

            return; // NO cerrar sesión completa
          }

          // Si el usuario NO estaba activo, entonces sí cerrar sesión completa
          this.logger.warn(
            '⚠️ Usuario inactivo y sesión perdida. Cerrando sesión completa.'
          );
          this.stopHeartbeat();

          // Notificar al usuario que su sesión ha expirado
          this.logger.debug('📢 Mostrando notificación al usuario...');
          this.toast.warning(
            'Su sesión ha expirado por inactividad. Cerrando sesión...'
          );

          // Limpiar sesión inmediatamente y redirigir
          this.logger.debug('🚪 Cerrando sesión inmediatamente...');

          // Actualizar estado de login PRIMERO (antes de limpiar localStorage)
          // Esto evita que componentes intermedios intenten renderizarse
          import('./login.service').then((module) => {
            const loginService = this.injector.get(module.LoginService);
            loginService.setLoginStatus(false);
          });

          import('./menu.service').then((module) => {
            const menuService = this.injector.get(module.MenuService);
            menuService.isLoginChange.next(false);
            menuService.menuChange.next([]);
          });

          // Limpiar todo el localStorage después de actualizar el estado
          localStorage.clear();

          // Redirigir directamente al login sin delay
          this.logger.debug('🔒 Redirigiendo a login...');
          this.router
            .navigate(['/login'], {
              replaceUrl: true, // Reemplaza la URL actual en el historial
              skipLocationChange: false,
            })
            .then(() => {
              this.logger.debug('✅ Redireccionado a login exitosamente');
            });
        } else if (err.status === 401) {
          // Token inválido o expirado - redirigir al login
          this.logger.warn(
            '⚠️  Token inválido o expirado, deteniendo heartbeat'
          );
          this.stopHeartbeat();

          this.toast.warning(
            'Sesión expirada. Por favor, inicie sesión nuevamente.'
          );

          // Actualizar estado de login PRIMERO
          import('./login.service').then((module) => {
            const loginService = this.injector.get(module.LoginService);
            loginService.setLoginStatus(false);
          });

          import('./menu.service').then((module) => {
            const menuService = this.injector.get(module.MenuService);
            menuService.isLoginChange.next(false);
            menuService.menuChange.next([]);
          });

          // Limpiar todo el localStorage y redirigir al login
          localStorage.clear();
          setTimeout(() => {
            this.router.navigate(['/login'], { replaceUrl: true });
          }, 1500);
        } else {
          // Error de red u otro error - NO detener el heartbeat
          // Puede ser un problema temporal de red
          this.logger.error(
            `❌ Error al enviar heartbeat (problema temporal): ${err.status}`,
            err
          );

          // Si hay muchos errores consecutivos, podríamos implementar un contador
          // Por ahora, solo logueamos y continuamos intentando
        }
      },
    });
  }

  /**
   * Asigna un punto de emisión e inicia el heartbeat automáticamente
   * Este método simplifica el flujo: asignar + iniciar heartbeat en una sola llamada
   *
   * @param idInvoiceIssuancePoint - ID del punto de emisión a asignar
   * @returns Observable que emite cuando la asignación es exitosa
   */
  assignInvoicePointWithHeartbeat(idInvoiceIssuancePoint: number) {
    return new Promise<void>((resolve, reject) => {
      this.invoicePointService
        .assignInvoicePoint(idInvoiceIssuancePoint)
        .subscribe({
          next: () => {
            this.logger.debug('✅ Punto de emisión asignado exitosamente');

            // Iniciar heartbeat automáticamente
            const started = this.startHeartbeat();
            if (started) {
              this.logger.debug(
                '💓 Heartbeat iniciado automáticamente después de asignar punto'
              );
            }

            resolve();
          },
          error: (err: HttpErrorResponse) => {
            this.logger.error('❌ Error al asignar punto de emisión', err);
            reject(err);
          },
        });
    });
  }

  /**
   * Libera el punto de emisión y detiene el heartbeat automáticamente
   * Este método simplifica el flujo: detener heartbeat + liberar en una sola llamada
   *
   * @returns Observable que emite cuando la liberación es exitosa
   */
  releaseInvoicePointWithHeartbeat() {
    return new Promise<void>((resolve, reject) => {
      // Primero detener heartbeat
      this.stopHeartbeat();

      // Luego liberar el punto en el servidor
      this.invoicePointService.releaseInvoicePoint().subscribe({
        next: () => {
          this.logger.debug('✅ Punto de emisión liberado exitosamente');

          // Limpiar localStorage
          localStorage.removeItem(environment.ID_INVOICE_POINT);
          localStorage.removeItem('INVOICE_POINT_DATA');

          resolve();
        },
        error: (err: HttpErrorResponse) => {
          this.logger.error('❌ Error al liberar punto de emisión', err);
          reject(err);
        },
      });
    });
  }
}
