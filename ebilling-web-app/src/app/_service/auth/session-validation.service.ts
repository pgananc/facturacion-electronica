import { Injectable, Injector } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, BehaviorSubject } from 'rxjs';
import { tap, catchError, map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { LoggerService } from '../logger/logger.service';
import { PermissionService } from '../permission/permission.service';

/**
 * Respuesta del endpoint de validación de sesión
 */
export interface SessionValidationResponse {
  valid: boolean;
  reason?:
    | 'ip_mismatch'
    | 'browser_mismatch'
    | 'token_expired'
    | 'invoice_point_released'
    | 'session_not_found'
    | 'multiple_sessions'
    | 'network_error';
  user?: {
    userName: string;
    name: string;
    roles: string[];
  };
  company?: {
    idCompany: number;
    name: string;
    identification: string;
  };
  establishment?: {
    idEstablishment: number;
    name: string;
    code: string;
  };
  invoicePoint?: {
    idInvoicePoint: number;
    code: string;
  };
}

/**
 * SessionValidationService
 *
 * Servicio encargado de validar la sesión del usuario contra el backend.
 * Verifica que la sesión actual coincida con el fingerprint guardado (IP + Browser).
 *
 * Características:
 * - Caché de validación para evitar llamadas repetidas al backend
 * - Integración con el sistema de fingerprinting (IP + User-Agent)
 * - Restauración automática de datos de sesión
 */
@Injectable({
  providedIn: 'root',
})
export class SessionValidationService {
  private readonly baseUrl = `${environment.HOST_AUTH}/api/session`;

  // Caché de la última validación
  private lastValidation: SessionValidationResponse | null = null;
  private lastValidationTime: number = 0;

  // Tiempo de vida del caché (5 minutos)
  private readonly CACHE_TTL_MS = 5 * 60 * 1000;

  // Estado de validación observable
  private validationState$ = new BehaviorSubject<boolean | null>(null);

  constructor(
    private readonly http: HttpClient,
    private readonly logger: LoggerService,
    private readonly injector: Injector
  ) {}

  /**
   * Valida la sesión actual contra el backend
   * Verifica: IP + Browser + Token + Punto de emisión
   *
   * @param forceRefresh Si es true, ignora el caché y fuerza una nueva validación
   */
  validateSession(
    forceRefresh: boolean = false
  ): Observable<SessionValidationResponse> {
    // Verificar si hay token antes de llamar al backend
    const token = localStorage.getItem(environment.TOKEN);
    if (!token) {
      this.logger.debug(
        '[SessionValidation] No hay token en localStorage, sesión inválida'
      );
      return of({ valid: false, reason: 'token_expired' });
    }

    // Verificar caché
    if (!forceRefresh && this.isCacheValid()) {
      this.logger.debug(
        '[SessionValidation] Retornando validación desde caché',
        this.lastValidation
      );
      const cachedResponse = this.lastValidation!;
      // ⚠️ IMPORTANTE: Restaurar datos de sesión incluso cuando se usa caché
      // Esto asegura que el localStorage esté actualizado cuando el WelcomeComponent se inicializa
      if (cachedResponse.valid) {
        this.restoreSessionData(cachedResponse);
      }
      return of(cachedResponse);
    }

    this.logger.debug('[SessionValidation] Validando sesión con backend...');

    return this.http
      .get<SessionValidationResponse | SessionValidationResponse[]>(
        `${this.baseUrl}/validate`
      )
      .pipe(
        map((response) => {
          // ⭐ CASO 1: Backend retorna una lista (múltiples sesiones activas)
          if (Array.isArray(response)) {
            this.logger.warn(
              `[SessionValidation] ⚠️ Múltiples sesiones detectadas: ${response.length}`
            );

            // Filtrar solo las válidas
            const validSessions = response.filter((s) => s.valid);

            if (validSessions.length === 0) {
              this.logger.error(
                '[SessionValidation] No hay sesiones válidas en la lista'
              );
              return { valid: false, reason: 'session_not_found' as const };
            }

            // Usar la primera sesión válida (el backend debería ordenarlas por fecha)
            const selectedSession = validSessions[0];

            this.logger.warn(
              `[SessionValidation] 🎯 Usando sesión: Punto ${
                selectedSession.invoicePoint?.code || 'N/A'
              }`
            );

            return selectedSession;
          }

          // ⭐ CASO 2: Backend retorna un objeto único (normal)
          return response;
        }),
        tap((response) => {
          // Guardar en caché
          this.lastValidation = response;
          this.lastValidationTime = Date.now();
          this.validationState$.next(response.valid);

          this.logger.debug('[SessionValidation] Respuesta procesada:', {
            valid: response.valid,
            reason: response.reason,
            hasUser: !!response.user,
            hasCompany: !!response.company,
            invoicePoint: response.invoicePoint?.code || 'N/A',
          });

          // Si es válida, restaurar datos en localStorage
          if (response.valid) {
            this.restoreSessionData(response);
          }
        }),
        catchError((error) => {
          this.logger.error(
            '[SessionValidation] Error al validar sesión:',
            error
          );

          // Si el error es 401, la sesión expiró
          if (error.status === 401) {
            return of({ valid: false, reason: 'token_expired' as const });
          }

          // Si el error es 500, probablemente múltiples sesiones
          if (error.status === 500) {
            this.logger.error(
              '[SessionValidation] Error 500: Posiblemente múltiples sesiones activas'
            );
            return of({
              valid: false,
              reason: 'session_not_found' as const,
            });
          }

          // Otros errores (red, servidor)
          return of({ valid: false, reason: 'network_error' as const });
        })
      );
  }

  /**
   * Verifica si el caché de validación aún es válido
   */
  private isCacheValid(): boolean {
    if (!this.lastValidation) {
      return false;
    }

    const now = Date.now();
    const elapsed = now - this.lastValidationTime;

    return elapsed < this.CACHE_TTL_MS;
  }

  /**
   * Restaura los datos de sesión en localStorage desde la respuesta del backend
   */
  private restoreSessionData(response: SessionValidationResponse): void {
    this.logger.debug('[SessionValidation] Restaurando datos de sesión:', {
      hasCompany: !!response.company,
      companyId: response.company?.idCompany,
      companyName: response.company?.name,
      hasEstablishment: !!response.establishment,
      hasInvoicePoint: !!response.invoicePoint,
    });

    if (response.company) {
      // ✅ NO sobrescribir idCompany si ya existe uno válido en localStorage
      // Esto evita que se cambie automáticamente la compañía cuando el usuario
      // tiene múltiples roles en múltiples compañías
      const currentIdCompany = localStorage.getItem(environment.ID_COMPANY);
      const responseIdCompany = response.company.idCompany.toString();
      
      // Solo actualizar si no hay idCompany actual o si es diferente (pero solo si el usuario lo solicita explícitamente)
      // Por ahora, NO actualizamos automáticamente para evitar cambios no deseados
      if (!currentIdCompany || currentIdCompany === '0' || currentIdCompany === '') {
        // Solo actualizar si no hay compañía seleccionada
      localStorage.setItem(
        environment.ID_COMPANY,
          responseIdCompany
      );
      localStorage.setItem(environment.NAME_COMPANY, response.company.name);
      localStorage.setItem(
        environment.IDENTIFICATION,
        response.company.identification
      );
      this.logger.debug(
          '[SessionValidation] idCompany guardado en localStorage (no había compañía seleccionada):',
        {
            idCompany: responseIdCompany,
          companyName: response.company.name,
        }
      );
      } else {
        // Ya hay una compañía seleccionada, NO sobrescribir
        this.logger.debug(
          '[SessionValidation] idCompany NO actualizado (ya hay compañía seleccionada):',
          {
            currentIdCompany: currentIdCompany,
            responseIdCompany: responseIdCompany,
            companyName: response.company.name,
            note: 'Manteniendo compañía actual para evitar cambios no deseados',
          }
        );
      }

      // 🔄 Refrescar suscripción después de restaurar idCompany
      // Esto asegura que PermissionService tenga la suscripción cargada
      try {
        const permissionService = this.injector.get(PermissionService);
        permissionService.refreshSubscription();
        this.logger.debug(
          '[SessionValidation] Suscripción refrescada después de restaurar sesión'
        );
      } catch (error) {
        this.logger.warn(
          '[SessionValidation] No se pudo refrescar suscripción (puede ser normal en inicialización):',
          error
        );
      }
    } else {
      this.logger.warn(
        '[SessionValidation] No hay información de compañía en la respuesta del backend'
      );
    }

    if (response.establishment) {
      localStorage.setItem(
        environment.ID_ESTABLISHMENT,
        response.establishment.idEstablishment.toString()
      );
    }

    if (response.invoicePoint) {
      localStorage.setItem(
        environment.ID_INVOICE_POINT,
        response.invoicePoint.idInvoicePoint.toString()
      );
      localStorage.setItem(
        'INVOICE_POINT_DATA',
        JSON.stringify({
          id: response.invoicePoint.idInvoicePoint,
          code: response.invoicePoint.code,
          establishmentId: response.establishment?.idEstablishment,
        })
      );
    }

    this.logger.debug('[SessionValidation] Datos de sesión restaurados');
  }

  /**
   * Limpia el caché de validación
   * Llamar cuando el usuario hace logout o cuando se necesita forzar revalidación
   */
  clearCache(): void {
    this.lastValidation = null;
    this.lastValidationTime = 0;
    this.validationState$.next(null);
    this.logger.debug('[SessionValidation] Caché limpiado');
  }

  /**
   * Limpia toda la sesión (localStorage + caché)
   */
  clearSession(): void {
    this.clearCache();
    localStorage.clear();
    this.logger.debug('[SessionValidation] Sesión completa limpiada');
  }

  /**
   * Observable del estado de validación
   * null = no validado, true = válido, false = inválido
   */
  getValidationState$(): Observable<boolean | null> {
    return this.validationState$.asObservable();
  }

  /**
   * Verifica si hay una sesión guardada (token en localStorage)
   * No valida contra el backend, solo verifica existencia local
   */
  hasStoredSession(): boolean {
    return !!localStorage.getItem(environment.TOKEN);
  }

  /**
   * Obtiene la última respuesta de validación (desde caché)
   */
  getLastValidation(): SessionValidationResponse | null {
    return this.lastValidation;
  }
}
