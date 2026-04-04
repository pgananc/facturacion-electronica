import { Injectable, Injector } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, of, BehaviorSubject } from 'rxjs';
import { map, catchError, tap, switchMap, shareReplay } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { decodeToken, isTokenValid } from '../../_functions/functions';
import { LoggerService } from '../logger/logger.service';
import { CompanySubscription } from '../../_model/subscription/company-subscription';
import { SubscriptionPlan } from '../../_model/subscription/subscription-plan';
import {
  FeatureValidationResponse,
  FeatureValidationRequest,
  CanEmitDocumentRequest,
  CanEmitDocumentResponse,
} from '../../_model/subscription/feature-validation-response';
import { SubscriptionUsageResponse } from '../../_model/subscription/subscription-usage';
import { UserService } from '../user/user.service';
import { UserRoleService } from '../auth/user-role.service';
import { PlanFeaturesResponse } from '../../_model/permission/plan-features-response';

/**
 * PermissionService
 *
 * Gestiona permisos basados en ROL + SUSCRIPCIÓN
 * - Validación de features (coordinada con backend)
 * - Caché de suscripción
 * - Verificación de límites de documentos
 * - Bypass para super_admin
 */
@Injectable({
  providedIn: 'root',
})
export class PermissionService {
  private readonly authUrl = `${environment.HOST_AUTH}/api/permission`;
  private readonly billingUrl = `${environment.HOST_EBILLING}/api/subscription`;
  private readonly billingPermissionsUrl = `${environment.HOST_EBILLING}/api/permissions`;

  // Cache de suscripción para evitar llamadas repetidas
  private subscriptionCache$ = new BehaviorSubject<CompanySubscription | null>(
    null,
  );

  // Guard para evitar llamadas duplicadas simultáneas a loadSubscription
  private isLoadingSubscription = false;

  // Cache de idUser para evitar llamadas repetidas al backend
  private idUserCache: number | null = null;

  // Observable compartido para idUser (evita múltiples llamadas paralelas)
  private idUser$: Observable<number> | null = null;

  // Cache de features permitidas del usuario (cargadas una vez al inicio)
  private allowedFeaturesCache: string[] = [];
  private featuresLoaded: boolean = false;

  // Cache de permisos de planes para validación local
  private planPermissionsCache: Map<string, string[]> = new Map(); // planCode -> featureCodes[]

  // Cache de validaciones de features con TTL de 5 minutos
  private featureValidationCache = new Map<
    string,
    { result: boolean; timestamp: number; ttl: number }
  >();
  private readonly CACHE_TTL = 300000; // 5 minutos en milisegundos
  private readonly CACHE_STORAGE_PREFIX = 'PERMISSION_CACHE_';

  // Cache de llamadas a getUsage para compartir entre múltiples suscriptores
  private usageCache$: Observable<SubscriptionUsageResponse | null> | null =
    null;
  private usageCacheCompanyId: number | null = null;

  constructor(
    private readonly http: HttpClient,
    private readonly logger: LoggerService,
    private readonly userService: UserService,
    private readonly injector: Injector,
  ) {
    // Solo cargar datos si hay un token válido y no expirado
    // Esto evita errores 401 cuando el usuario no está autenticado o el token está expirado
    if (isTokenValid()) {
      // Cargar suscripción al iniciar el servicio
      this.loadSubscription();
      // Cargar permisos de planes solo si hay token válido (usuario autenticado)
      this.loadPlanPermissions();
      // Pre-cargar idUser al iniciar el servicio (si hay token)
      this.preloadIdUser();
      // Cargar features permitidas del usuario al iniciar
      this.loadUserFeatures();
    } else {
      this.logger.debug(
        '[PermissionService] No hay token válido o token expirado, omitiendo carga inicial de datos',
      );
    }
  }

  /**
   * Pre-cargar idUser al inicio para evitar múltiples llamadas paralelas
   * @private
   */
  private preloadIdUser(): void {
    const token = decodeToken();
    if (!token) {
      return;
    }

    // Si ya tenemos el idUser en caché, no hacer nada
    if (this.idUserCache !== null) {
      return;
    }

    // Intentar obtener del token primero (prioridad: 'sub' estándar JWT)
    const idUserFromToken =
      (token as any).sub || // ✅ Estándar JWT (prioridad)
      (token as any).idUser ||
      (token as any).userId ||
      null;

    // El 'sub' puede venir como string o number, convertir a number si es necesario
    let idUserNumber: number | null = null;
    if (idUserFromToken) {
      if (typeof idUserFromToken === 'number') {
        idUserNumber = idUserFromToken;
      } else if (typeof idUserFromToken === 'string') {
        const parsed = parseInt(idUserFromToken, 10);
        if (!isNaN(parsed)) {
          idUserNumber = parsed;
        }
      }
    }

    if (idUserNumber && idUserNumber > 0) {
      this.idUserCache = idUserNumber;
      this.logger.debug(
        `[PermissionService] ✅ idUser pre-cargado del token (sub): ${idUserNumber}`,
      );
      return;
    }

    // Si no está en el token y tenemos userName, pre-cargar del backend
    if (token.userName) {
      this.logger.debug(
        `[PermissionService] 🔄 Pre-cargando idUser del backend para userName: ${token.userName}`,
      );
      // Iniciar la carga pero no bloquear (se cacheará cuando termine)
      this.userService.findUserByUserName(token.userName).subscribe({
        next: (response) => {
          const idUser = response.userDto?.idUser || 0;
          if (idUser > 0) {
            this.idUserCache = idUser;
            this.logger.debug(
              `[PermissionService] ✅ idUser pre-cargado del backend: ${idUser}`,
            );
          }
        },
        error: (error) => {
          this.logger.warn(
            '[PermissionService] Error al pre-cargar idUser (se intentará de nuevo cuando sea necesario):',
            error,
          );
        },
      });
    }
  }

  /**
   * Cargar todas las features permitidas del usuario desde el backend
   * Se ejecuta una vez al inicio y cachea el resultado
   * @private
   */
  private loadUserFeatures(): void {
    // Verificar token válido y no expirado antes de hacer peticiones
    if (!isTokenValid()) {
      this.logger.debug(
        '[PermissionService] No hay token válido o token expirado, no se pueden cargar features del usuario',
      );
      return;
    }

    // Verificar que tenemos idCompany (necesario para validar features)
    const idCompanyRaw = localStorage.getItem(environment.ID_COMPANY);
    if (!idCompanyRaw) {
      this.logger.debug(
        '[PermissionService] No hay idCompany, no se pueden cargar features del usuario',
      );
      return;
    }

    this.logger.debug(
      '[PermissionService] 🔄 Cargando features permitidas del usuario...',
    );

    this.http.get<string[]>(`${this.authUrl}/user/features`).subscribe({
      next: (features) => {
        this.allowedFeaturesCache = features || [];
        this.featuresLoaded = true;

        this.logger.debug(
          `[PermissionService] ✅ Features permitidas cargadas: ${this.allowedFeaturesCache.length} features`,
          {
            features: this.allowedFeaturesCache,
            total: this.allowedFeaturesCache.length,
          },
        );
      },
      error: (error) => {
        // No mostrar errores 401 (token expirado) ya que ya validamos antes
        if (error.status === 401) {
          this.logger.debug(
            '[PermissionService] Token expirado al cargar features del usuario (se cargarán cuando sea necesario)',
          );
        } else {
          this.logger.error(
            '[PermissionService] ❌ Error al cargar features del usuario:',
            error,
          );
        }
        // En caso de error, usar array vacío (ninguna feature permitida)
        this.allowedFeaturesCache = [];
        this.featuresLoaded = true; // Marcar como cargado para evitar reintentos infinitos
      },
    });
  }

  /**
   * Invalidar cache de features permitidas (útil después de cambios de rol/plan)
   */
  invalidateFeaturesCache(): void {
    this.allowedFeaturesCache = [];
    this.featuresLoaded = false;
    this.logger.debug(
      '[PermissionService] Cache de features invalidado, se recargará en la próxima validación',
    );
    // Recargar features si hay token
    if (decodeToken()) {
      this.loadUserFeatures();
    }
  }

  /**
   * Invalidar TODO el caché de permisos (features, validaciones, planes)
   * Útil al iniciar sesión para asegurar validación fresca contra el backend
   */
  invalidateAllCaches(): void {
    // Invalidar caché de features permitidas
    this.allowedFeaturesCache = [];
    this.featuresLoaded = false;

    // Invalidar caché de validaciones de features
    this.invalidateFeatureCache();

    // Invalidar caché de permisos de planes
    this.planPermissionsCache.clear();

    // Invalidar caché de idUser
    this.idUserCache = null;

    this.logger.debug(
      '[PermissionService] ✅ TODO el caché de permisos invalidado (features, validaciones, planes, idUser)',
    );
  }

  /**
   * Cargar suscripción actual desde backend y cachear
   * @private
   */
  private loadSubscription(): void {
    // Verificar token válido y no expirado antes de hacer peticiones
    if (!isTokenValid()) {
      this.logger.debug(
        '[PermissionService] No hay token válido o token expirado, no se puede cargar suscripción',
      );
      return;
    }

    // Usar idCompany de localStorage (más confiable que el token)
    const idCompanyRaw = localStorage.getItem(environment.ID_COMPANY);
    if (!idCompanyRaw) {
      this.logger.debug(
        '[PermissionService] No hay idCompany en localStorage, no se puede cargar suscripción',
      );
      return;
    }

    const idCompany = Number(idCompanyRaw);
    if (!idCompany || isNaN(idCompany)) {
      this.logger.warn(
        '[PermissionService] idCompany inválido en localStorage:',
        idCompanyRaw,
      );
      return;
    }

    // Evitar llamadas duplicadas simultáneas
    if (this.isLoadingSubscription) {
      this.logger.debug(
        '[PermissionService] Ya hay una carga de suscripción en curso, omitiendo llamada duplicada',
      );
      return;
    }

    this.isLoadingSubscription = true;
    this.logger.debug(
      '[PermissionService] Cargando suscripción para companyId:',
      idCompany,
    );

    this.http.get<any>(`${this.billingUrl}/${idCompany}/current`).subscribe({
      next: (response) => {
        // Mapear la respuesta del backend a CompanySubscription
        // El backend puede retornar 'status' o 'subscriptionStatus'
        const subscription: CompanySubscription = {
          ...response,
          status: response.status || response.subscriptionStatus || 'UNKNOWN',
        };

        this.logger.debug(
          '[PermissionService] Suscripción cargada exitosamente:',
          {
            companyId: idCompany,
            planCode: subscription.plan?.code,
            status: subscription.status,
            originalStatus: response.status,
            originalSubscriptionStatus: response.subscriptionStatus,
          },
        );
        this.subscriptionCache$.next(subscription);
        // Guardar en localStorage para uso offline
        localStorage.setItem('SUBSCRIPTION_DATA', JSON.stringify(subscription));
        this.isLoadingSubscription = false;
      },
      error: (err) => {
        this.isLoadingSubscription = false;
        // No mostrar errores 401 (token expirado) ya que ya validamos antes
        if (err.status === 401) {
          this.logger.debug(
            '[PermissionService] Token expirado al cargar suscripción (se cargará cuando sea necesario)',
          );
        } else if (err.status === 404) {
          this.logger.warn(
            '[PermissionService] No hay suscripción activa para la compañía:',
            { companyId: idCompany },
          );
          // No usar fallback: sin suscripción no se permite acceso al sistema
          localStorage.removeItem('SUBSCRIPTION_DATA');
        } else if (err.status !== 0) {
          this.logger.error(
            '[PermissionService] Error al cargar suscripción:',
            {
              companyId: idCompany,
              status: err.status,
              message: err.error?.message || err.message,
            },
          );
        } else {
          this.logger.debug(
            '[PermissionService] Servicio de suscripción no disponible',
          );
        }
        // Sin fallback a caché: si el backend no devuelve suscripción activa, la compañía no tiene acceso
        this.subscriptionCache$.next(null);
      },
    });
  }

  /**
   * Obtener suscripción actual (Observable)
   * @returns Observable con la suscripción o null
   */
  getSubscription(): Observable<CompanySubscription | null> {
    return this.subscriptionCache$.asObservable();
  }

  /**
   * Obtener suscripción actual (valor sincrónico)
   * @returns Suscripción actual o null
   */
  getSubscriptionValue(): CompanySubscription | null {
    return this.subscriptionCache$.value;
  }

  /**
   * Obtener suscripción de una compañía (consulta puntual, no actualiza el caché global).
   * Útil para decidir el mensaje cuando no hay puntos de emisión (suscripción cancelada vs sin puntos).
   */
  getSubscriptionForCompany(
    companyId: number
  ): Observable<CompanySubscription | null> {
    const headers = new HttpHeaders().set('X-Skip-Interceptor', 'true');
    return this.http
      .get<any>(`${this.billingUrl}/${companyId}/current`, { headers })
      .pipe(
        map(
          (response): CompanySubscription => ({
            ...response,
            status:
              response.status || response.subscriptionStatus || 'UNKNOWN',
          })
        ),
        catchError(() => of(null))
      );
  }

  /**
   * Refrescar suscripción desde backend
   */
  refreshSubscription(): void {
    this.loadSubscription();
    // También refrescar permisos de planes cuando se refresca la suscripción
    this.loadPlanPermissions();
    // Invalidar y recargar features cuando se refresca la suscripción
    // (el plan puede haber cambiado)
    this.invalidateFeaturesCache();
    // Invalidar cache de validaciones porque el plan puede haber cambiado
    this.invalidateFeatureCache();
    // Limpiar caché de usage para forzar recarga
    this.usageCache$ = null;
    this.usageCacheCompanyId = null;
  }

  /**
   * Cargar permisos de planes desde backend para validación local
   * Solo se carga si el usuario está autenticado (tiene token válido y no expirado)
   * @private
   */
  private loadPlanPermissions(): void {
    // Verificar que el usuario esté autenticado y el token no esté expirado
    if (!isTokenValid()) {
      this.logger.debug(
        '[PermissionService] No hay token válido o token expirado, omitiendo carga de permisos de planes',
      );
      return;
    }

    // Usar ebill-authentication que tiene la tabla feature_permission con required_plans
    // Endpoint: GET /api/permission/permissions/features
    // Retorna todas las features con sus required_plans (array de planes permitidos)
    // Nota: requiredPlans puede venir como array o como string PostgreSQL array
    this.http
      .get<
        Array<{
          featureCode: string;
          featureName: string;
          description: string;
          module: string;
          allowedRoles: number[];
          requiredPlans: string[] | string; // Array de planes o string PostgreSQL array: ['FREE', 'BASIC'] o "{FREE,BASIC}"
          isActive: boolean;
        }>
      >(`${this.authUrl}/permissions/features`)
      .pipe(
        catchError((err) => {
          // Manejar diferentes tipos de errores
          if (err.status === 401) {
            // Token expirado - ya validamos antes, solo loggear como debug
            this.logger.debug(
              '[PermissionService] Token expirado al cargar permisos de planes (se cargarán cuando sea necesario)',
            );
          } else if (err.status === 0) {
            // Servicio no disponible - normal si el servicio está caído
            this.logger.debug(
              '[PermissionService] Servicio de permisos no disponible (puede ser normal si el servicio no está corriendo)',
            );
          } else {
            // Error real del servidor
            this.logger.warn(
              '[PermissionService] Error al cargar permisos de planes:',
              err,
            );
          }
          return of([]);
        }),
      )
      .subscribe((featuresResponse) => {
        // Construir mapa: planCode -> [featureCodes habilitados]
        const planMap = new Map<string, string[]>();

        this.logger.debug(
          '[PermissionService] 🔄 Construyendo mapa de planes desde feature_permission:',
          {
            totalFeatures: featuresResponse.length,
            activeFeatures: featuresResponse.filter((f) => f.isActive).length,
          },
        );

        // Función helper para normalizar requiredPlans
        const normalizeRequiredPlans = (
          requiredPlans: string[] | string,
        ): string[] => {
          if (Array.isArray(requiredPlans)) {
            return requiredPlans;
          } else if (typeof requiredPlans === 'string') {
            // Si viene como string PostgreSQL array: "{FREE,BASIC}" -> ["FREE", "BASIC"]
            return requiredPlans
              .replace(/[{}]/g, '')
              .split(',')
              .map((p: string) => p.trim())
              .filter((p: string) => p.length > 0);
          }
          return [];
        };

        // Obtener todos los planes únicos de todas las features
        const allPlans = new Set<string>();
        featuresResponse.forEach((feature) => {
          const plans = normalizeRequiredPlans(feature.requiredPlans);
          plans.forEach((plan) => {
            if (plan) {
              allPlans.add(plan);
            }
          });
        });

        // Para cada plan, construir la lista de feature codes permitidos
        allPlans.forEach((planCode) => {
          const featureCodes: string[] = [];

          // Iterar sobre todas las features y agregar las que incluyen este plan
          featuresResponse.forEach((feature) => {
            if (!feature.isActive) {
              return; // Saltar features inactivas
            }

            const plans = normalizeRequiredPlans(feature.requiredPlans);
            if (plans.includes(planCode)) {
              featureCodes.push(feature.featureCode);
            }
          });

          planMap.set(planCode, featureCodes);

          this.logger.debug(
            `[PermissionService] Plan ${planCode} mapeado desde feature_permission:`,
            {
              planCode,
              featureCodes,
              totalFeatures: featureCodes.length,
            },
          );
        });

        this.planPermissionsCache = planMap;
        this.logger.debug(
          '[PermissionService] Permisos de planes cargados en cache (dinámico desde feature_permission):',
          {
            plansCount: planMap.size,
            plans: Array.from(planMap.keys()),
            featuresPerPlan: Object.fromEntries(planMap),
            FREE_features: planMap.get('FREE') || [],
            FREE_count: (planMap.get('FREE') || []).length,
          },
        );
      });
  }

  /**
   * Verificar si un plan tiene habilitada una feature según la matriz de permisos
   * @private
   */
  private isFeatureEnabledForPlan(
    featureCode: string,
    planCode: string,
  ): boolean {
    const features = this.planPermissionsCache.get(planCode);
    return features ? features.includes(featureCode) : false;
  }

  /**
   * Obtener uso de documentos (mensual/anual)
   * Usa shareReplay para compartir la misma llamada HTTP entre múltiples suscriptores
   * (evita llamadas duplicadas cuando hay múltiples instancias de subscription-badge)
   * @param idCompany ID de la compañía
   * @returns Observable con el uso de documentos
   */
  getUsage(idCompany?: number): Observable<SubscriptionUsageResponse | null> {
    const token = decodeToken();
    const companyId = idCompany || token?.idCompany;

    if (!companyId) {
      return of(null);
    }

    // Si hay una llamada en caché para la misma compañía, reutilizarla
    if (this.usageCache$ && this.usageCacheCompanyId === companyId) {
      return this.usageCache$;
    }

    // Crear nueva llamada y cachearla
    this.usageCache$ = this.http
      .get<SubscriptionUsageResponse>(`${this.billingUrl}/${companyId}/usage`)
      .pipe(
        shareReplay(1), // Compartir el resultado entre múltiples suscriptores
        catchError(() => {
          // Limpiar caché en caso de error
          this.usageCache$ = null;
          this.usageCacheCompanyId = null;
          return of(null);
        }),
      );
    this.usageCacheCompanyId = companyId;

    return this.usageCache$;
  }

  /**
   * Obtener clave de cache para una validación de feature
   * @private
   */
  private getCacheKey(featureCode: string, idCompany: number): string {
    return `${featureCode}-${idCompany}`;
  }

  /**
   * Verificar si hay cache válido para una feature
   * @private
   */
  private getCachedValidation(
    cacheKey: string,
  ): { result: boolean; source: 'memory' | 'localStorage' } | null {
    // 1. Verificar cache en memoria primero
    const memoryCache = this.featureValidationCache.get(cacheKey);
    if (memoryCache && Date.now() - memoryCache.timestamp < memoryCache.ttl) {
      this.logger.debug(
        `[PermissionService] ✅ Cache hit (memoria) para ${cacheKey}`,
      );
      return { result: memoryCache.result, source: 'memory' };
    }

    // 2. Si no hay en memoria o expiró, verificar localStorage
    try {
      const storageKey = `${this.CACHE_STORAGE_PREFIX}${cacheKey}`;
      const cached = localStorage.getItem(storageKey);
      if (cached) {
        const data = JSON.parse(cached);
        if (Date.now() - data.timestamp < data.ttl) {
          // Cache válido en localStorage, restaurar en memoria también
          this.featureValidationCache.set(cacheKey, {
            result: data.result,
            timestamp: data.timestamp,
            ttl: data.ttl,
          });
          this.logger.debug(
            `[PermissionService] ✅ Cache hit (localStorage) para ${cacheKey}`,
          );
          return { result: data.result, source: 'localStorage' };
        } else {
          // Cache expirado, limpiar
          localStorage.removeItem(storageKey);
        }
      }
    } catch (error) {
      this.logger.debug(
        `[PermissionService] Error leyendo cache de localStorage:`,
        error,
      );
    }

    return null;
  }

  /**
   * Guardar resultado de validación en cache
   * @private
   */
  private saveToCache(
    cacheKey: string,
    result: boolean,
    ttl: number = this.CACHE_TTL,
  ): void {
    const cacheData = {
      result,
      timestamp: Date.now(),
      ttl,
    };

    // Guardar en memoria
    this.featureValidationCache.set(cacheKey, cacheData);

    // Guardar en localStorage como fallback
    try {
      const storageKey = `${this.CACHE_STORAGE_PREFIX}${cacheKey}`;
      localStorage.setItem(storageKey, JSON.stringify(cacheData));
    } catch (error) {
      this.logger.debug(
        `[PermissionService] Error guardando cache en localStorage:`,
        error,
      );
    }
  }

  /**
   * Invalidar cache de validaciones de features
   * @param featureCode Feature específica a invalidar (opcional, si no se proporciona invalida todo)
   */
  invalidateFeatureCache(featureCode?: string): void {
    const idCompanyRaw = localStorage.getItem(environment.ID_COMPANY);
    if (!idCompanyRaw) {
      return;
    }

    const idCompany = Number(idCompanyRaw);
    if (featureCode) {
      // Invalidar solo una feature específica
      const cacheKey = this.getCacheKey(featureCode, idCompany);
      this.featureValidationCache.delete(cacheKey);
      try {
        localStorage.removeItem(`${this.CACHE_STORAGE_PREFIX}${cacheKey}`);
      } catch (error) {
        // Ignorar errores de localStorage
      }
      this.logger.debug(
        `[PermissionService] Cache invalidado para feature: ${featureCode}`,
      );
    } else {
      // Invalidar todo el cache
      this.featureValidationCache.clear();
      try {
        // Limpiar todas las entradas de cache del localStorage
        const keysToRemove: string[] = [];
        for (let i = 0; i < localStorage.length; i++) {
          const key = localStorage.key(i);
          if (key?.startsWith(this.CACHE_STORAGE_PREFIX)) {
            keysToRemove.push(key);
          }
        }
        keysToRemove.forEach((key) => localStorage.removeItem(key));
      } catch (error) {
        // Ignorar errores de localStorage
      }
      this.logger.debug(
        '[PermissionService] Todo el cache de validaciones invalidado',
      );
    }
  }

  /**
   * Obtiene el idUser del token o del backend (con caché y Observable compartido)
   * Usa shareReplay(1) para que múltiples llamadas paralelas compartan el mismo Observable
   * @private
   */
  private getIdUser(): Observable<number> {
    // Si ya tenemos el idUser en caché, retornarlo directamente
    if (this.idUserCache !== null) {
      this.logger.debug(
        `[PermissionService] ✅ idUser obtenido desde caché: ${this.idUserCache}`,
      );
      return of(this.idUserCache);
    }

    // Si ya hay un Observable compartido en curso, reutilizarlo
    if (this.idUser$) {
      this.logger.debug(
        `[PermissionService] 🔄 Reutilizando Observable compartido de idUser`,
      );
      return this.idUser$;
    }

    const token = decodeToken();
    if (!token) {
      this.logger.warn(
        '[PermissionService] No hay token disponible para obtener idUser',
      );
      return of(0); // Retornar 0 si no hay token (el backend manejará el error)
    }

    // Intentar obtener idUser del token (prioridad: 'sub' estándar JWT, luego campos personalizados)
    const idUserFromToken =
      (token as any).sub || // ✅ Estándar JWT (prioridad)
      (token as any).idUser ||
      (token as any).userId ||
      null;

    this.logger.debug(
      `[PermissionService] 🔍 Intentando obtener idUser del token:`,
      {
        sub: (token as any).sub, // ✅ Estándar JWT (prioridad)
        idUser: (token as any).idUser,
        userId: (token as any).userId,
        userName: token.userName,
        roles: token.roles,
      },
    );

    // El 'sub' puede venir como string o number, convertir a number si es necesario
    let idUserNumber: number | null = null;
    if (idUserFromToken) {
      if (typeof idUserFromToken === 'number') {
        idUserNumber = idUserFromToken;
      } else if (typeof idUserFromToken === 'string') {
        const parsed = parseInt(idUserFromToken, 10);
        if (!isNaN(parsed)) {
          idUserNumber = parsed;
        }
      }
    }

    if (idUserNumber && idUserNumber > 0) {
      this.idUserCache = idUserNumber;
      this.logger.debug(
        `[PermissionService] ✅ idUser obtenido del token (sub): ${idUserNumber}`,
      );
      return of(idUserNumber);
    }

    // Si no está en el token, obtenerlo del backend usando userName
    if (!token.userName) {
      this.logger.warn(
        '[PermissionService] No se pudo obtener idUser: no hay userName en el token',
      );
      return of(0);
    }

    this.logger.debug(
      `[PermissionService] 🔍 Obteniendo idUser del backend usando userName: ${token.userName}`,
    );

    // Crear Observable compartido para que múltiples llamadas paralelas compartan el mismo resultado
    this.idUser$ = this.userService.findUserByUserName(token.userName).pipe(
      map((response) => {
        const idUser = response.userDto?.idUser || 0;
        this.logger.debug(
          `[PermissionService] 🔍 Respuesta del backend para obtener idUser:`,
          {
            userName: token.userName,
            idUser: idUser,
            userDto: response.userDto,
          },
        );
        if (idUser > 0) {
          this.idUserCache = idUser; // Cachear para próximas llamadas
          this.logger.debug(
            `[PermissionService] ✅ idUser obtenido del backend y cacheado: ${idUser}`,
          );
        } else {
          this.logger.warn(
            `[PermissionService] ⚠️ idUser obtenido del backend es 0 o inválido`,
          );
        }
        // Limpiar el Observable compartido después de obtener el resultado
        this.idUser$ = null;
        return idUser;
      }),
      catchError((error) => {
        this.logger.error(
          '[PermissionService] ❌ Error al obtener idUser del backend:',
          error,
        );
        // Limpiar el Observable compartido en caso de error
        this.idUser$ = null;
        return of(0);
      }),
      shareReplay(1), // ✅ Compartir el resultado entre múltiples suscripciones paralelas
    );

    return this.idUser$;
  }

  /**
   * Verificar si el usuario puede acceder a una feature
   * Usa el cache de features permitidas cargado al inicio (más eficiente)
   *
   * @param featureCode Código de la feature (ej: 'INVOICE_CREATE')
   * @returns Observable<boolean> true si tiene acceso
   */
  canAccess(featureCode: string): Observable<boolean> {
    const token = decodeToken();

    if (!token) {
      return of(false);
    }

    // Super admin siempre tiene acceso
    if (this.isSuperAdmin()) {
      return of(true);
    }

    // Si las features ya están cargadas, usar el cache (validación instantánea)
    if (this.featuresLoaded) {
      const hasAccess = this.allowedFeaturesCache.includes(featureCode);

      this.logger.debug(
        `[PermissionService] ✅ Validación de ${featureCode} desde cache: ${
          hasAccess ? 'PERMITIDO' : 'DENEGADO'
        }`,
        {
          featureCode,
          hasAccess,
          totalFeaturesPermitidas: this.allowedFeaturesCache.length,
        },
      );
      return of(hasAccess);
    }

    // Si las features aún no se han cargado, cargarlas primero
    // Esto puede pasar si se llama a canAccess() antes de que termine loadUserFeatures()
    this.logger.debug(
      `[PermissionService] ⏳ Features aún no cargadas, cargando ahora para validar ${featureCode}...`,
    );
    return this.http.get<string[]>(`${this.authUrl}/user/features`).pipe(
      tap((features) => {
        this.allowedFeaturesCache = features || [];
        this.featuresLoaded = true;
        this.logger.debug(
          `[PermissionService] ✅ Features cargadas: ${this.allowedFeaturesCache.length} features`,
        );
      }),
      map((features) => {
        const hasAccess = (features || []).includes(featureCode);
        this.logger.debug(
          `[PermissionService] ✅ Validación de ${featureCode}: ${
            hasAccess ? 'PERMITIDO' : 'DENEGADO'
          }`,
        );
        return hasAccess;
      }),
      catchError((error) => {
        this.logger.error(
          `[PermissionService] ❌ Error al cargar features para validar ${featureCode}:`,
          error,
        );
        // En caso de error, denegar acceso
        this.allowedFeaturesCache = [];
        this.featuresLoaded = true; // Marcar como cargado para evitar reintentos infinitos
        return of(false);
      }),
    );
  }

  /**
   * Verificar permiso con detalles (para mostrar mensajes de upgrade)
   * Usa cache para evitar llamadas repetidas al backend
   *
   * @param featureCode Código de la feature
   * @returns Observable con detalles de la validación
   */
  canAccessWithDetails(
    featureCode: string,
  ): Observable<FeatureValidationResponse> {
    const token = decodeToken();
    if (!token) {
      return of<FeatureValidationResponse>({
        allowed: false,
        reason: 'NO_ROLE',
        message: 'Usuario no autenticado',
      });
    }

    if (this.isSuperAdmin()) {
      return of<FeatureValidationResponse>({
        allowed: true,
        message: 'Acceso de super administrador',
      });
    }

    // Obtener idCompany desde localStorage (usar environment.ID_COMPANY para consistencia)
    const idCompanyRaw = localStorage.getItem(environment.ID_COMPANY);
    const idCompany = idCompanyRaw
      ? Number(idCompanyRaw)
      : token.idCompany || undefined;

    // Validar que tenemos los datos necesarios
    if (!idCompany) {
      this.logger.warn(
        '[PermissionService] No se pudo obtener idCompany para validar permisos con detalles',
        {
          hasLocalStorage: !!idCompanyRaw,
          tokenIdCompany: token.idCompany,
        },
      );
      return of<FeatureValidationResponse>({
        allowed: false,
        reason: 'NO_ROLE',
        message: 'Datos de sesión incompletos',
      });
    }

    // ⚠️ IMPORTANTE: SIEMPRE validar contra el backend sin usar caché
    // Esto asegura que los permisos se validen correctamente al momento de loguear
    // El backend es la fuente de verdad para permisos de rol + plan
    this.logger.debug(
      `[PermissionService] 🔄 Validando ${featureCode} contra el backend (sin caché)`,
      {
        featureCode,
        idCompany,
        tokenRoles: token?.roles,
        note: 'Siempre validando contra backend para asegurar permisos correctos',
      },
    );

    // Obtener idUser y luego hacer la validación
    return this.getIdUser().pipe(
      switchMap((idUser) => {
        const request: FeatureValidationRequest = {
          featureCode,
          idCompany,
          idUser,
        };

        this.logger.debug(
          `[PermissionService] 📤 Enviando request de validación de permisos (con detalles):`,
          {
            featureCode,
            idCompany,
            idUser,
            requestBody: request,
            tokenRoles: token?.roles,
          },
        );

        return this.http
          .post<FeatureValidationResponse>(
            `${this.authUrl}/validate-feature`,
            request,
          )
          .pipe(
            tap((response) => {
              // Log detallado de la respuesta del backend
              this.logger.debug(
                `[PermissionService] 📥 Respuesta del backend para ${featureCode}:`,
                {
                  allowed: response.allowed,
                  reason: response.reason,
                  message: response.message,
                  idUserEnviado: idUser,
                  idCompanyEnviado: idCompany,
                },
              );
            }),
            map((response) => {
              const subscription = this.getSubscriptionValue();
              const userRoles = token?.roles || [];

              return response;
            }),
            catchError((error) => {
              // Proporcionar una respuesta más específica según el error
              const errorReason = error.error?.reason;
              const errorMessage = error.error?.message || error.message;

              if (error.status === 400) {
                return of<FeatureValidationResponse>({
                  allowed: false,
                  reason: 'NO_ROLE',
                  message:
                    'Solicitud inválida. Verifique la configuración del sistema.',
                });
              } else if (error.status === 404) {
                return of<FeatureValidationResponse>({
                  allowed: false,
                  reason: 'NO_ROLE',
                  message: 'Funcionalidad no encontrada o no disponible.',
                });
              } else if (error.status === 403) {
                // Diferenciar entre NO_ROLE y NO_SUBSCRIPTION
                if (errorReason === 'NO_SUBSCRIPTION') {
                  return of<FeatureValidationResponse>({
                    allowed: false,
                    reason: 'NO_SUBSCRIPTION',
                    message:
                      'No hay una suscripción activa. Por favor, contacte al administrador para activar su suscripción.',
                  });
                } else if (
                  errorReason === 'NO_ROLE' ||
                  errorMessage?.includes('role')
                ) {
                  return of<FeatureValidationResponse>({
                    allowed: false,
                    reason: 'NO_ROLE',
                    message:
                      'Tu rol no tiene permisos para acceder a esta funcionalidad. Contacta al administrador si necesitas acceso.',
                  });
                } else {
                  return of<FeatureValidationResponse>({
                    allowed: false,
                    reason: errorReason || 'NO_ROLE',
                    message:
                      errorMessage ||
                      'No tienes permisos para acceder a esta funcionalidad.',
                  });
                }
              } else {
                return of<FeatureValidationResponse>({
                  allowed: false,
                  reason: 'NO_ROLE',
                  message:
                    'Error al verificar permisos. Por favor, intente nuevamente.',
                });
              }
            }),
          );
      }),
    );
  }

  /**
   * Verificar si puede emitir un documento
   *
   * @param documentType Tipo de documento (default: 'INVOICE')
   * @returns Observable<boolean> true si puede emitir
   */
  canEmitDocument(
    documentType:
      | 'INVOICE'
      | 'CREDIT_NOTE'
      | 'DEBIT_NOTE'
      | 'RETENTION'
      | 'GUIDE' = 'INVOICE',
  ): Observable<boolean> {
    const token = decodeToken();
    if (!token) {
      this.logger.warn('[PermissionService] canEmitDocument: No token');
      return of(false);
    }

    // Obtener idCompany desde localStorage (más confiable) o del token como fallback
    const idCompanyRaw = localStorage.getItem(environment.ID_COMPANY);
    const idCompany = idCompanyRaw
      ? Number(idCompanyRaw)
      : token.idCompany || undefined;

    if (!idCompany || isNaN(idCompany)) {
      this.logger.warn('[PermissionService] canEmitDocument: No idCompany', {
        hasLocalStorage: !!idCompanyRaw,
        tokenIdCompany: token.idCompany,
      });
      return of(false);
    }

    if (this.isSuperAdmin()) {
      return of(true);
    }

    const request: CanEmitDocumentRequest = { documentType };
    return this.http
      .post<CanEmitDocumentResponse>(
        `${this.billingUrl}/${idCompany}/can-emit`,
        request,
      )
      .pipe(
        map((response) => {
          this.logger.debug(
            `[PermissionService] canEmitDocument response: allowed=${response.allowed}, usage=${response.currentUsage}, limit=${response.limit}, remaining=${response.remaining}, message=${response.message}`,
          );
          return response.allowed;
        }),
        catchError((error) => {
          // Si es un HttpErrorResponse con status 403, intentar leer el body
          if (error.status === 403 && error.error) {
            const errorResponse = error.error as CanEmitDocumentResponse;
            this.logger.warn(
              `[PermissionService] canEmitDocument 403: allowed=${errorResponse.allowed}, usage=${errorResponse.currentUsage}, limit=${errorResponse.limit}, message=${errorResponse.message}`,
            );
            return of(errorResponse.allowed || false);
          }

          // Para otros errores, loguear y retornar false
          this.logger.error(
            `[PermissionService] canEmitDocument error: status=${error.status}, message=${error.message}`,
            error,
          );
          return of(false);
        }),
      );
  }

  /**
   * Verificar si puede emitir un documento con detalles
   *
   * @param documentType Tipo de documento
   * @returns Observable con detalles de la capacidad de emisión
   */
  canEmitDocumentWithDetails(
    documentType:
      | 'INVOICE'
      | 'CREDIT_NOTE'
      | 'DEBIT_NOTE'
      | 'RETENTION'
      | 'GUIDE' = 'INVOICE',
  ): Observable<CanEmitDocumentResponse> {
    const token = decodeToken();
    if (!token) {
      this.logger.warn(
        '[PermissionService] canEmitDocumentWithDetails: No token',
      );
      return of<CanEmitDocumentResponse>({
        allowed: false,
        currentUsage: 0,
        limit: null,
        remaining: 0,
        message: 'Usuario no autenticado',
      });
    }

    // Obtener idCompany desde localStorage (más confiable) o del token como fallback
    const idCompanyRaw = localStorage.getItem(environment.ID_COMPANY);
    const idCompany = idCompanyRaw
      ? Number(idCompanyRaw)
      : token.idCompany || undefined;

    if (!idCompany || isNaN(idCompany)) {
      this.logger.warn(
        '[PermissionService] canEmitDocumentWithDetails: No idCompany',
        {
          hasLocalStorage: !!idCompanyRaw,
          tokenIdCompany: token.idCompany,
        },
      );
      return of<CanEmitDocumentResponse>({
        allowed: false,
        currentUsage: 0,
        limit: null,
        remaining: 0,
        message: 'No se pudo obtener la compañía',
      });
    }

    if (this.isSuperAdmin()) {
      return of<CanEmitDocumentResponse>({
        allowed: true,
        currentUsage: 0,
        limit: null,
        remaining: 999999,
        message: 'Super administrador: sin límites',
      });
    }

    const request: CanEmitDocumentRequest = { documentType };
    return this.http
      .post<CanEmitDocumentResponse>(
        `${this.billingUrl}/${idCompany}/can-emit`,
        request,
      )
      .pipe(
        catchError(() =>
          of<CanEmitDocumentResponse>({
            allowed: false,
            currentUsage: 0,
            limit: null,
            remaining: 0,
            message: 'Error al verificar límites',
          }),
        ),
      );
  }

  /**
   * Verificar si el usuario es super_admin
   * PRIORIDAD 1: Verificar si es super_admin globalmente (desde el token)
   * super_admin es un rol global del sistema, no depende de la compañía
   * PRIORIDAD 2: Verificar roles por compañía (fallback)
   * @returns true si es super_admin
   */
  isSuperAdmin(): boolean {
    // PRIORIDAD 1: Verificar si es super_admin globalmente (desde el token)
    try {
      const userRoleService = this.injector.get(UserRoleService, null);
      if (userRoleService) {
        const isSuperAdminGlobal = userRoleService.isSuperAdminGlobal();
        if (isSuperAdminGlobal) {
          return true;
        }
      }
    } catch (error) {
      // Si UserRoleService no está disponible, continuar con fallback
    }

    // PRIORIDAD 2: Verificar roles por compañía (fallback)
    try {
      const userRoleService = this.injector.get(UserRoleService, null);
      if (userRoleService) {
        const roles = userRoleService.getCurrentRolesSync();
        if (roles && roles.length > 0) {
          return roles.includes('super_admin');
        }
      }
    } catch (error) {
      // Si UserRoleService no está disponible, usar token como fallback
    }

    // Fallback final: usar token directamente
    const token = decodeToken();
    if (!token || !token.roles) {
      return false;
    }
    return token.roles.includes('super_admin');
  }

  /**
   * Verificar si el usuario tiene un rol específico
   * Usa UserRoleService para obtener roles de la compañía actual
   *
   * @param roleName Nombre del rol (ej: 'admin', 'member')
   * @returns true si tiene el rol
   */
  hasRole(roleName: string): boolean {
    // Intentar usar UserRoleService si está disponible
    try {
      const userRoleService = this.injector.get(UserRoleService, null);
      if (userRoleService) {
        const roles = userRoleService.getCurrentRolesSync();
        if (roles && roles.length > 0) {
          return roles.includes(roleName);
        }
      }
    } catch (error) {
      // Si UserRoleService no está disponible, usar token como fallback
    }

    // Fallback: usar token (puede incluir roles de todas las compañías)
    const token = decodeToken();
    if (!token || !token.roles) {
      return false;
    }
    return token.roles.includes(roleName);
  }

  /**
   * Obtener código del plan actual (desde caché)
   * @returns Código del plan (FREE, BASIC, PRO, etc.)
   */
  getCurrentPlanCode(): string {
    const subscription = this.subscriptionCache$.value;
    return subscription?.plan?.code || 'FREE';
  }

  /**
   * Obtener plan actual (desde caché)
   * @returns Plan actual o null
   */
  getCurrentPlan(): SubscriptionPlan | null {
    const subscription = this.subscriptionCache$.value;
    return subscription?.plan || null;
  }

  /**
   * Determina si un permiso requiere solo validación por ROL (sin validar plan)
   *
   * Lógica: Si el backend retorna NO_ROLE cuando el usuario no tiene el rol,
   * pero NO retorna NO_SUBSCRIPTION cuando el usuario tiene el rol pero no el plan,
   * entonces el permiso solo requiere validación por rol.
   *
   * Alternativamente, podemos usar una heurística: si el permiso tiene requiredPlans
   * vacío o con todos los planes, entonces solo requiere validación por rol.
   *
   * @param featureCode Código de la feature
   * @returns Observable<boolean> true si solo requiere validación por rol
   */
  requiresOnlyRoleValidation(featureCode: string): Observable<boolean> {
    // Si es super_admin, no requiere validación
    if (this.isSuperAdmin()) {
      return of(false);
    }

    // Intentar usar UserRoleService para validar directamente por rol
    try {
      const userRoleService = this.injector.get(UserRoleService, null);
      if (userRoleService) {
        // Si podemos validar por rol directamente, hacerlo
        // Por ahora, retornamos false para usar validación normal
        // Esto se puede mejorar consultando al backend sobre requiredPlans
        return of(false);
      }
    } catch (error) {
      // Si UserRoleService no está disponible, usar validación normal
    }

    // Por defecto, asumir que requiere validación por rol + plan
    return of(false);
  }

  /**
   * Valida un permiso SOLO por rol (sin validar plan)
   * Usa UserRoleService para obtener roles y valida contra los roles permitidos del backend
   *
   * @param featureCode Código de la feature
   * @returns Observable<boolean> true si tiene permiso por rol
   */
  canAccessByRoleOnly(featureCode: string): Observable<boolean> {
    const token = decodeToken();
    if (!token) {
      return of(false);
    }

    // Super admin siempre tiene acceso
    if (this.isSuperAdmin()) {
      return of(true);
    }

    // Intentar usar UserRoleService para obtener roles de la compañía actual
    try {
      const userRoleService = this.injector.get(UserRoleService, null);
      if (userRoleService) {
        const roles = userRoleService.getCurrentRolesSync();

        // Si no hay roles cargados, intentar cargarlos
        if (!roles || roles.length === 0) {
          return userRoleService.getCurrentCompanyRoles().pipe(
            switchMap((loadedRoles) => {
              // Validar con el backend si el rol tiene permiso
              // Por ahora, retornamos false y dejamos que canAccessWithDetails valide
              // Esto se puede mejorar consultando al backend sobre allowedRoles para este featureCode
              return of(false);
            }),
          );
        }

        // Por ahora, retornamos false y usamos canAccessWithDetails
        // Esto se puede mejorar consultando al backend sobre allowedRoles
        return of(false);
      }
    } catch (error) {
      // Si UserRoleService no está disponible, usar validación normal
    }

    // Por defecto, usar validación normal (rol + plan)
    return of(false);
  }

  /**
   * Verificar si el plan actual incluye una feature (caché local)
   *
   * @param feature Nombre de la feature (ej: 'hasDashboard')
   * @returns true si el plan incluye la feature
   */
  planHasFeature(feature: keyof SubscriptionPlan): boolean {
    const subscription = this.subscriptionCache$.value;
    if (!subscription) {
      return false;
    }
    return subscription.plan[feature] as boolean;
  }

  /**
   * Verificar si la suscripción está activa
   * @returns true si está activa
   */
  isSubscriptionActive(): boolean {
    const subscription = this.subscriptionCache$.value;
    if (!subscription) {
      return false;
    }
    return subscription.status === 'ACTIVE' || subscription.status === 'TRIAL';
  }

  /**
   * Verificar si la suscripción está expirada
   * @returns true si está expirada
   */
  isSubscriptionExpired(): boolean {
    const subscription = this.subscriptionCache$.value;
    if (!subscription) {
      return false;
    }
    return subscription.status === 'EXPIRED';
  }

  /**
   * Obtener porcentaje de uso de documentos
   * @returns Porcentaje de uso (0-100)
   */
  getUsagePercent(): number {
    const subscription = this.subscriptionCache$.value;
    return subscription?.percentUsed || 0;
  }

  /**
   * Obtener documentos restantes
   * @returns Número de documentos restantes
   */
  getDocumentsRemaining(): number {
    const subscription = this.subscriptionCache$.value;
    return subscription?.documentsRemaining || 0;
  }
}
