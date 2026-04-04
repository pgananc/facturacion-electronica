import { environment } from './../../environments/environment';
import { Injectable } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor,
  HttpResponse,
  HttpErrorResponse,
} from '@angular/common/http';
import { catchError, Observable, retry, tap, throwError } from 'rxjs';
import { Router } from '@angular/router';
import { DURATION_TIME_MESSAGE } from '../_constants/constants';
import {
  MESSAGE_ERROR_SERVER,
  ERRORS,
  HEADER_MESSAGE,
} from '../_constants/constants';
import { LoggerService } from '../_service/logger/logger.service';
import { ToastService } from '../_service/notification/toast.service';
import { extractUserFriendlyErrorMessage } from '../_utils/error-message.util';
import { decodeToken } from '../_functions/functions';

@Injectable()
export class ServerErrorsInterceptor implements HttpInterceptor {
  constructor(
    private router: Router,
    private logger: LoggerService,
    private toast: ToastService,
  ) {}

  intercept(
    request: HttpRequest<any>,
    next: HttpHandler,
  ): Observable<HttpEvent<any>> {
    // Verificar si el componente quiere manejar sus propios errores
    const skipInterceptor = request.headers.has('X-Skip-Interceptor');

    // Lista de URLs que NO deben cerrar la sesión automáticamente en 401
    const draftEndpoints = ['/api/invoice/draft/'];
    const shouldSkipAutoLogout = draftEndpoints.some((endpoint) =>
      request.url.includes(endpoint),
    );

    return next
      .handle(request)
      .pipe(retry(environment.REINTENTOS))
      .pipe(
        tap((event) => {
          if (event instanceof HttpResponse) {
            if (
              event.body &&
              event.body.error === true &&
              event.body.errorMessage
            ) {
              throw new Error(event.body.errorMessage);
            }
          }
        }),
      )
      .pipe(
        catchError((err: HttpErrorResponse) => {
          // Si el componente quiere manejar el error, solo lo propagamos sin logging
          if (skipInterceptor) {
            return throwError(() => err);
          }

          const isLogoEndpoint =
            err.url?.includes('/logo/image') || err.url?.endsWith('/logo');
          if (isLogoEndpoint && err.status === 404) {
            // No loguear ni mostrar error, solo propagar silenciosamente
            return throwError(() => err);
          }

          // Ignorar 404 de suscripción actual (compañía sin suscripción activa); el mensaje se muestra en el diálogo de compañías
          const isSubscriptionCurrent =
            err.url?.includes('/api/subscription/') &&
            err.url?.endsWith('/current');
          if (isSubscriptionCurrent && err.status === 404) {
            return throwError(() => err);
          }

          // Manejar error 500 de "Index out of bounds" en user-company/pageable
          // Esto ocurre cuando una compañía no tiene usuarios (caso válido, pero informar al usuario)
          const isUserCompanyPageable = err.url?.includes(
            '/api/user-company/pageable',
          );
          const errorMessage = err?.error?.message || err?.message || '';
          const isIndexOutOfBounds =
            err.status === 500 &&
            errorMessage.includes('Index') &&
            errorMessage.includes('out of bounds');

          if (isUserCompanyPageable && isIndexOutOfBounds) {
            // Caso válido: compañía sin usuarios - el componente mostrará el mensaje con el nombre
            // No mostrar mensaje aquí, el componente tiene acceso al nombre de la compañía
            // Propagar el error para que el componente lo maneje con el nombre de la compañía
            return throwError(() => err);
          }

          // Ignorar errores 403 y 404 de validación de permisos (son casos esperados)
          const isPermissionValidation = err.url?.includes(
            '/api/permission/validate-feature',
          );
          if (
            isPermissionValidation &&
            (err.status === 403 || err.status === 404)
          ) {
            // No loguear como error:
            // - 403: Usuario no tiene permisos (caso esperado)
            // - 404: Feature no existe o está inactiva en el backend (caso esperado durante desarrollo)
            return throwError(() => err);
          }

          // Ignorar errores del endpoint de carga de features del usuario
          // Este es un servicio de fondo que se carga automáticamente
          // El PermissionService ya maneja el error silenciosamente (solo loguea y usa array vacío)
          const isUserFeaturesEndpoint = err.url?.includes(
            '/api/permission/user/features',
          );
          if (isUserFeaturesEndpoint) {
            // No mostrar mensaje de error - el PermissionService ya lo maneja
            return throwError(() => err);
          }

          // Ignorar errores 401 de endpoints de carga inicial cuando el token está expirado
          // Estos endpoints se llaman automáticamente al iniciar la app y ya validamos el token antes
          // Si el token expira entre la validación y la petición, no mostrar error (se cargarán cuando sea necesario)
          const isInitialLoadEndpoint =
            err.status === 401 &&
            (err.url?.includes('/api/payment-method') ||
              err.url?.includes('/api/subscription/') ||
              err.url?.includes('/api/permission/permissions/features'));
          if (isInitialLoadEndpoint) {
            // No mostrar mensaje de error - los servicios ya validan el token antes de hacer la petición
            // Si llega aquí, el token expiró entre la validación y la petición (caso raro)
            return throwError(() => err);
          }

          // Ignorar errores del endpoint check-feature cuando el feature no existe
          // Esto es un caso normal (validación), no un error del sistema
          const isCheckFeatureEndpoint = err.url?.includes(
            '/api/admin/menu/check-feature/',
          );
          if (
            isCheckFeatureEndpoint &&
            (err.status === 404 ||
              (err.status === 500 &&
                (err.error?.message?.includes('not found') ||
                  err.error?.message?.includes('Feature not found'))))
          ) {
            // No mostrar mensaje de error - el componente ya maneja esto como caso normal
            return throwError(() => err);
          }

          // Ignorar errores de conexión (status 0) - los servicios los manejan como DEBUG
          // Solo loguear errores reales del servidor
          if (err.status !== 0) {
            this.logger.error('HTTP Error intercepted', err);
          } else {
            // Status 0 = servicio no disponible - los servicios individuales lo manejan
            // No loguear aquí para evitar ruido en consola durante pruebas
          }

          // Si es un endpoint de draft o ticket con 401, no cerrar sesión automáticamente
          const isTicketEndpoint =
            (request.url.includes('/api/invoice/') ||
              request.url.includes('/api/debit-notes/') ||
              request.url.includes('/api/credit-notes/')) &&
            !request.url.includes('/api/invoice/draft/') &&
            !request.url.includes('/api/debit-notes/draft/') &&
            !request.url.includes('/api/credit-notes/draft/');

          if (
            (shouldSkipAutoLogout || isTicketEndpoint) &&
            err.status === 401
          ) {
            this.logger.warn(
              `⚠️ 401 en ${
                isTicketEndpoint ? 'endpoint de ticket' : 'endpoint de draft'
              } - no cerrando sesión automáticamente`,
            );
            return throwError(() => err);
          }

          // Si no, manejamos el error normalmente y lo propagamos
          this.validateError(err);
          return throwError(() => err);
        }),
      );
  }

  /**
   * Valida y muestra mensajes de error según el código de estado HTTP
   * Solo navega automáticamente para errores de autenticación (401)
   * Usa extractUserFriendlyErrorMessage para sanitizar mensajes técnicos
   */
  public validateError(err: HttpErrorResponse): void {
    // No loguear errores de conexión (status 0) - los servicios los manejan como DEBUG
    // Solo loguear errores reales del servidor
    if (err.status !== 0) {
      this.logger.error('🔴 [ServerErrorsInterceptor] Error HTTP:', {
        status: err.status,
        url: err.url,
        message: err.error?.message || err.message,
        error: err,
      });
    }

    // Extraer mensaje amigable usando la función de sanitización
    const userFriendlyMessage = extractUserFriendlyErrorMessage(
      err,
      'realizar la operación',
    );

    switch (err.status) {
      case MESSAGE_ERROR_SERVER.MESSAGE_ERROR_SERVER_400.code: {
        this.showMessageError(userFriendlyMessage, 'Error de validación', '');
        break;
      }
      case MESSAGE_ERROR_SERVER.MESSAGE_ERROR_SERVER_401.code: {
        // 401: No autenticado
        const url = err.url || '';

        // ✅ Excluir rutas que manejan sus propios errores o donde el 401 es esperado
        if (
          url.includes('/ticket') ||
          url.includes('/api/invoice/') ||
          url.includes('/api/debit-notes/') ||
          url.includes('/api/credit-notes/') ||
          url.includes('/api/login/logout') || // ✅ Logout: el 401 es esperado (sesión ya cerrada)
          url.includes('/api/payment-method') || // ✅ Carga inicial: ya validamos token antes
          url.includes('/api/subscription/') || // ✅ Carga inicial: ya validamos token antes
          url.includes('/api/permission/permissions/features') || // ✅ Carga inicial: ya validamos token antes
          url.includes('/api/permission/user/features') // ✅ Carga inicial: ya validamos token antes
        ) {
          // No mostrar mensaje de error ni redirigir - el componente/servicio manejará el error
          break;
        }

        // Para otras rutas, redirigir al login con mensaje amigable
        this.showMessageError(
          'Tu sesión ha expirado. Por favor, inicia sesión nuevamente.',
          MESSAGE_ERROR_SERVER.MESSAGE_ERROR_SERVER_401.message,
          '/login',
        );
        break;
      }
      case MESSAGE_ERROR_SERVER.MESSAGE_ERROR_SERVER_404.code: {
        // 404: No encontrado - NO navegamos automáticamente
        // El componente puede manejar este caso específicamente
        this.showMessageError(userFriendlyMessage, 'Recurso no encontrado', '');
        break;
      }
      case MESSAGE_ERROR_SERVER.MESSAGE_ERROR_SERVER_429.code: {
        // 429: Too Many Requests - Rate Limiting
        // Nota: Si hay error de CORS, el body del error puede no estar disponible
        // Intentar extraer el mensaje del backend, con fallback a mensaje genérico
        let message = userFriendlyMessage;

        // Si el mensaje extraído es genérico y tenemos acceso al error.error, intentar obtener más info
        if (message.includes('Error al realizar la operación') && err?.error) {
          // Intentar extraer mensaje de diferentes ubicaciones
          const backendMessage =
            err.error?.message ||
            err.error?.error ||
            (typeof err.error === 'string' ? err.error : null);

          if (backendMessage && !backendMessage.includes('Http failure')) {
            message = backendMessage;
          }
        }

        this.showMessageError(message, 'Límite de solicitudes excedido', '');
        break;
      }
      case MESSAGE_ERROR_SERVER.MESSAGE_ERROR_SERVER_500.code: {
        this.showMessageError(userFriendlyMessage, 'Error del servidor', '');
        break;
      }
      case MESSAGE_ERROR_SERVER.MESSAGE_ERROR_SERVER_0.code: {
        // Error de conexión (servicio no disponible)
        // Si hay sesión válida, los servicios individuales manejan estos errores silenciosamente
        // Solo mostrar mensaje si no hay sesión válida
        const hasValidSession = decodeToken() !== null;
        if (!hasValidSession) {
          // No hay sesión válida - mostrar error y redirigir a login
          this.showMessageError(
            userFriendlyMessage,
            'Error de conexión',
            '/login',
          );
        }
        // Si hay sesión válida, no mostrar mensaje (los servicios lo manejan como DEBUG)
        break;
      }
      default: {
        this.showMessageError(userFriendlyMessage, 'Error', '');
      }
    }
  }

  showMessageError(message: string, header: string, navegation: string) {
    // Mostrar mensaje usando ToastService (unificado con el resto de la aplicación)
    this.toast.error(message);
    if (navegation) {
      localStorage.clear();
      this.router.navigate([navegation]);
    }
  }
}
