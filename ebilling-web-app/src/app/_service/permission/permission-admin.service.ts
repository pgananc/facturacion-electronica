import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, forkJoin, of } from 'rxjs';
import { map, catchError, switchMap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import {
  FeaturePermissionAdmin,
  Role,
  RolePermissionUpdate,
  BulkPermissionUpdateRequest,
} from '../../_model/permission/permission-admin';
import {
  SubscriptionPlan,
  FeaturePlanPermission,
  PlanPermissionUpdate,
  BulkPlanPermissionUpdateRequest,
} from '../../_model/permission/plan-permission-admin';
import {
  PlanFeatures,
  PlanFeaturesResponse,
  BulkPlanFeaturesUpdateRequest,
  BulkUpdateResponse,
  SystemFeature,
} from '../../_model/permission/plan-features-response';
import { Menu } from '../../_model/auth/menu';
import { decodeToken } from '../../_functions/functions';
import { RoleResponse } from '../../_model/auth/role-response';
import { LoggerService } from '../logger/logger.service';

@Injectable({
  providedIn: 'root',
})
export class PermissionAdminService {
  private readonly apiUrl = `${environment.HOST_AUTH}/api/permission/permissions`; // 🔧 Estructura correcta: /api/permission/permissions
  private readonly billingPermissionsUrl = `${environment.HOST_EBILLING}/api/permissions`; // 🔧 Nueva API de permisos en billing (8480)
  private readonly menuUrl = `${environment.HOST_AUTH}/api/menu`;
  private readonly roleUrl = `${environment.HOST_AUTH}/api/role`; // 🔧 Endpoint alternativo para listar roles

  /**
   * Mapeo dinámico de feature codes a nombres de features booleanos del backend
   * 
   * IMPORTANTE: Este mapeo solo aplica para features que están en el formato booleano
   * del backend (PlanFeatures). Features que no están aquí se manejarán como sin permisos de planes.
   * 
   * Estrategia completamente dinámica:
   * 1. Intenta mapeo directo (para compatibilidad con features existentes)
   * 2. Genera dinámicamente el nombre del campo booleano basándose en el feature code
   * 3. Verifica si el campo existe en PlanFeatures antes de retornarlo
   */
  private getBooleanKeyFromFeatureCode(featureCode: string): keyof PlanFeatures | null {
    // Mapeo directo para casos especiales/alias (mínimo necesario)
    const directMap: { [key: string]: keyof PlanFeatures } = {
      INVOICE_CREATE: 'hasElectronicInvoice', // Alias común
      DELIVERY_GUIDE_CREATE: 'hasGuide', // Alias común
    };

    // Si hay mapeo directo, usarlo
    if (directMap[featureCode]) {
      return directMap[featureCode];
    }

    // Generar dinámicamente el nombre del campo booleano
    // Convención: "CREDIT_NOTE_CREATE" -> "hasCreditNote"
    // Pasos:
    // 1. Normalizar a mayúsculas
    // 2. Remover sufijos comunes (_CREATE, _VOID, _EXPORT, etc.)
    // 3. Convertir a camelCase con prefijo "has"
    const normalized = featureCode.toUpperCase();
    
    // Remover sufijos comunes que no son parte del nombre base
    let baseName = normalized
      .replace(/_CREATE$/, '')
      .replace(/_VOID$/, '')
      .replace(/_EXPORT$/, '')
      .replace(/_MANAGEMENT$/, '')
      .replace(/_ACCESS$/, '')
      .replace(/_CONFIG$/, '');

    // Convertir a camelCase: "CREDIT_NOTE" -> "creditNote" -> "hasCreditNote"
    const camelCase = baseName
      .toLowerCase()
      .split('_')
      .map((word, index) => 
        index === 0 ? word : word.charAt(0).toUpperCase() + word.slice(1)
      )
      .join('');

    const booleanKey = `has${camelCase.charAt(0).toUpperCase() + camelCase.slice(1)}` as keyof PlanFeatures;

    // Verificar que el campo existe en PlanFeatures (validación en tiempo de ejecución)
    // Lista de campos válidos en PlanFeatures (basado en la interfaz)
    const validBooleanKeys: (keyof PlanFeatures)[] = [
      'hasApiAccess',
      'hasMultiUser',
      'hasAdvancedReports',
      'hasElectronicInvoice',
      'hasCreditNote',
      'hasDebitNote',
      'hasRetention',
      'hasGuide',
      'hasPrioritySupport',
      'hasCustomBranding',
      'hasAutomatedBackups',
    ];

    // Si el campo generado existe en la lista de válidos, retornarlo
    if (validBooleanKeys.includes(booleanKey)) {
      return booleanKey;
    }

    // No hay mapeo disponible (el feature no está en formato booleano del backend)
    return null;
  }

  constructor(
    private readonly http: HttpClient,
    private readonly logger: LoggerService
  ) {}

  /**
   * Obtener el menú completo del backend (sin filtrar)
   * Usado para extraer funcionalidades reales del menú
   */
  private getAllMenus(): Observable<Menu[]> {
    const token = decodeToken();
    if (!token || !token.userName) {
      return of([]);
    }
    return this.http.get<Menu[]>(`${this.menuUrl}/${token.userName}`);
  }

  /**
   * Extraer funcionalidades del menú del backend
   * IMPORTANTE: Cada menú hoja debe tener su propio featureCode único.
   * Si el backend asigna el mismo featureCode a múltiples menús hoja,
   * se generará un featureCode único basado en la URL del menú.
   * 
   * El módulo se determina por el nombre del menú padre (NO hardcodeado)
   */
  private extractFeaturesFromMenu(menus: Menu[]): FeaturePermissionAdmin[] {
    const features: FeaturePermissionAdmin[] = [];
    const featureMap = new Map<string, FeaturePermissionAdmin>();

    const processMenu = (menu: Menu, parentMenuName?: string) => {
      // El módulo es el nombre del menú padre (sin mapeos hardcodeados)
      // Si no hay padre, usar el nombre del menú actual
      const module = parentMenuName || menu.name;

      // Si tiene submenús, procesarlos recursivamente
      // Pasar el nombre del menú actual como padre para los hijos
      if (menu.subMenus && menu.subMenus.length > 0) {
        menu.subMenus.forEach((subMenu) => {
          // Si el menú actual tiene URL válida, es un padre intermedio, usar su nombre
          // Si no tiene URL, es un padre raíz, usar su nombre
          const currentParentName = menu.url && menu.url !== 'null' 
            ? parentMenuName || menu.name  // Si tiene URL, mantener el padre anterior
            : menu.name; // Si no tiene URL, es el nuevo padre
          processMenu(subMenu, currentParentName);
        });
      }

      // Si es un menú hoja (tiene URL válida)
      if (menu.url && menu.url !== 'null') {
        // Usar featureCode del backend si existe (el backend es la fuente de verdad)
        let featureCode = menu.featureCode;

        // Si no tiene featureCode del backend, generar uno dinámico basado en el nombre del menú
        // NO usar prefijos hardcodeados, solo convertir el nombre a formato válido
        if (!featureCode) {
          featureCode = this.generateFeatureCodeFromMenuName(menu.name, menu.url);
        } else if (featureMap.has(featureCode)) {
          // Si el featureCode ya existe, generar uno único para este menú específico
          // usando el nombre del menú + URL para garantizar unicidad
          featureCode = this.generateFeatureCodeFromMenuName(menu.name, menu.url);
        }

        // Crear feature para este menú hoja específico
        // El backend siempre envía menu.name, usarlo directamente
        // La descripción vendrá del backend en getAllFeatures(), no hardcodeada
        const feature: FeaturePermissionAdmin = {
          featureCode,
          name: menu.name, // Usar directamente el nombre del menú del backend
          module: module, // Usar el nombre del menú padre (sin mapeos)
          allowedRoles: [], // Se obtendrá del backend
          requiredPlans: [], // Se obtendrá del backend
          description: `Acceso a ${menu.name}`, // Descripción genérica temporal, será reemplazada por la del backend
        };
        featureMap.set(featureCode, feature);
      }
    };

    menus.forEach((menu) => processMenu(menu));
    return Array.from(featureMap.values());
  }

  /**
   * Generar un featureCode único dinámicamente basado en el nombre del menú y URL
   * NO usa prefijos hardcodeados - completamente dinámico
   * 
   * @param menuName Nombre del menú (ej: "Nota de crédito")
   * @param url URL del menú (ej: "/credit-note")
   * @returns FeatureCode generado (ej: "NOTA_DE_CREDITO" o "CREDIT_NOTE" si la URL es más descriptiva)
   */
  private generateFeatureCodeFromMenuName(menuName: string, url: string): string {
    // Intentar usar la URL primero (más descriptiva y única)
    if (url && url !== 'null' && url.length > 1) {
      // Limpiar URL: remover barras iniciales y caracteres especiales
      let cleanUrl = url
        .replace(/^\//, '') // Remover barra inicial
        .replace(/\//g, '_') // Reemplazar barras con guiones bajos
        .replace(/-/g, '_') // Reemplazar guiones con guiones bajos
        .replace(/[^A-Z0-9_]/gi, '') // Remover caracteres especiales
        .toUpperCase();

      // Si la URL limpia es válida (más de 2 caracteres), usarla
      if (cleanUrl && cleanUrl.length > 2) {
        return cleanUrl;
      }
    }

    // Si la URL no es válida, usar el nombre del menú
    // Convertir nombre a formato válido: "Nota de crédito" -> "NOTA_DE_CREDITO"
    const cleanName = menuName
      .toUpperCase()
      .replace(/[^A-Z0-9\s]/g, '') // Remover caracteres especiales (mantener espacios)
      .replace(/\s+/g, '_') // Reemplazar espacios con guiones bajos
      .replace(/_+/g, '_') // Reemplazar múltiples guiones bajos con uno solo
      .replace(/^_|_$/g, ''); // Remover guiones bajos al inicio y final

    // Si el nombre limpio es válido, usarlo
    if (cleanName && cleanName.length > 2) {
      return cleanName;
    }

    // Fallback: usar "FEATURE_" + hash simple basado en nombre + URL
    const hash = (menuName + url).split('').reduce((acc, char) => {
      return ((acc << 5) - acc) + char.charCodeAt(0);
    }, 0);
    return `FEATURE_${Math.abs(hash).toString(36).toUpperCase()}`;
  }

  /**
   * Obtener todos los features disponibles
   * Combina la estructura de menús (para módulos y nombres correctos) con permisos del backend
   * Esto asegura que:
   * 1. Los módulos coincidan con la estructura real de menús (padre del menú)
   * 2. Los nombres coincidan con los nombres de los menús
   * 3. Los permisos vengan del backend (feature_permission)
   */
  getAllFeatures(): Observable<FeaturePermissionAdmin[]> {
    // Paso 1: Obtener menús del backend para determinar estructura y módulos correctos
    return this.getAllMenus().pipe(
      switchMap((menus) => {
        // Paso 2: Extraer features de los menús (esto determina módulos basándose en el menú padre)
        const menuFeatures = this.extractFeaturesFromMenu(menus);
        
        // Si no hay features del menú (por ejemplo, sin token), retornar array vacío sin hacer petición HTTP
        if (menuFeatures.length === 0) {
          return of([]);
        }
        
        // Paso 3: Obtener permisos del backend para cada feature
        return this.http.get<any[]>(`${this.apiUrl}/features`).pipe(
          map((backendFeatures) => {
            // Crear mapa de features del backend (solo para permisos)
            const backendMap = new Map<string, any>();
            backendFeatures.forEach((f) => {
              backendMap.set(f.featureCode, f);
            });

            // Combinar: usar features del menú (con módulos y nombres correctos) pero con permisos del backend
            return menuFeatures.map((menuFeature) => {
              const backendFeature = backendMap.get(menuFeature.featureCode);
              if (backendFeature) {
                // Usar módulo y nombre del menú (correcto), pero permisos del backend
                return {
                  featureCode: menuFeature.featureCode,
                  name: menuFeature.name, // Nombre del menú (correcto)
                  module: menuFeature.module, // Módulo basado en menú padre (correcto)
                  allowedRoles: backendFeature.allowedRoles || [],
                  requiredPlans: backendFeature.requiredPlans || [],
                  description: backendFeature.description || menuFeature.description,
                };
              }
              // Si no hay backend feature, retornar el feature del menú tal cual
              return menuFeature;
            });
          }),
          catchError((error) => {
            this.logger.error(
              '[PermissionAdminService] Error al obtener features del backend:',
              error
            );
            // En caso de error, retornar features del menú sin permisos
            return of(menuFeatures);
          })
        );
      }),
      catchError((error) => {
        this.logger.error(
          '[PermissionAdminService] Error al obtener menús:',
          error
        );
        // Si falla obtener menús, retornar array vacío (NO usar fallback al backend)
        // El componente debe manejar el caso de features vacías
        return of([]);
      })
    );
  }


  /**
   * Obtener permisos de un rol específico
   * Endpoint: GET /api/permission/permissions/role/{idRole}
   * ⚠️ El backend espera {idRole}, no {id}
   */
  getRolePermissions(roleId: number): Observable<FeaturePermissionAdmin[]> {
    // El backend espera idRole en el path, usar roleId directamente
    return this.http.get<FeaturePermissionAdmin[]>(
      `${this.apiUrl}/role/${roleId}`
    );
  }

  /**
   * Obtener todos los roles disponibles
   * Usa GET /api/role como alternativa temporal (el endpoint /api/permission/permissions/roles no existe)
   */
  getAllRoles(): Observable<Role[]> {
    return this.http.get<RoleResponse>(this.roleUrl).pipe(
      map((response) => {
        // Mapear RoleResponse (con idRole) a Role (con id) para el componente
        return (response.roleDtos || []).map((role) => ({
          id: role.idRole, // Mapear idRole -> id
          name: role.name,
          description: role.description || '',
        }));
      }),
      catchError((error) => {
        console.error(
          '[PermissionAdminService] Error al obtener roles del backend:',
          error
        );
        // Retornar array vacío si el backend falla (NO usar datos hardcodeados)
        // El componente debe manejar el caso de roles vacíos
        return of([]);
      })
    );
  }

  /**
   * Actualizar roles permitidos para un feature
   */
  updateFeatureRoles(
    featureCode: string,
    allowedRoles: number[]
  ): Observable<void> {
    // 🔧 Endpoint correcto según backend: PUT /api/permission/permissions/feature/{featureCode}
    return this.http.put<void>(`${this.apiUrl}/feature/${featureCode}`, {
      allowedRoles,
    });
  }

  /**
   * Sincronizar múltiples permisos de un rol (no implementado en backend)
   */
  syncRolePermissions(roleId: number, features: string[]): Observable<void> {
    // 🔧 El backend NO tiene este endpoint
    // Por ahora, retornamos un Observable vacío
    this.logger.warn(
      '[PermissionAdminService] syncRolePermissions no implementado en backend'
    );
    return new Observable((observer) => {
      observer.next();
      observer.complete();
    });
  }

  /**
   * Actualizar múltiples permisos en lote
   * Endpoint: PUT /api/permission/permissions/bulk
   * Formato del backend: { "featureRoles": { "FEATURE_CODE": [roleIds] } }
   */
  bulkUpdatePermissions(
    request: BulkPermissionUpdateRequest
  ): Observable<FeaturePermissionAdmin[]> {
    // El backend espera el formato: { "featureRoles": { "FEATURE_CODE": [roleIds] } }
    return this.http.put<FeaturePermissionAdmin[]>(
      `${this.apiUrl}/bulk`,
      request
    );
  }

  // ============================================
  // MÉTODOS PARA GESTIÓN DE PERMISOS DE PLANES
  // ============================================
  // NOTA: getAllPlans() fue movido a SubscriptionAdminService
  // porque los planes pertenecen al dominio de billing/subscription
  // Usar: subscriptionAdminService.getAllPlans()

  /**
   * Obtener todos los features con sus permisos de planes
   * Usa los menús como fuente de verdad (NO fallbacks hardcodeados)
   */
  getAllFeaturesWithPlanPermissions(): Observable<FeaturePlanPermission[]> {
    // Obtener features desde menús (fuente de verdad)
    return this.getAllMenus().pipe(
      switchMap((menus) => {
        const menuFeatures = this.extractFeaturesFromMenu(menus);
        if (menuFeatures.length === 0) {
          // Si no hay features del menú, retornar array vacío (NO usar endpoint alternativo)
          this.logger.warn(
            '[PermissionAdminService] No se encontraron features en los menús'
          );
          return of([]);
        }
        // Convertir a FeaturePlanPermission y obtener permisos de planes
        const planFeatures: FeaturePlanPermission[] = menuFeatures.map(
          (f) => ({
            featureCode: f.featureCode,
            name: f.name,
            module: f.module,
            allowedPlans: f.requiredPlans || [], // Los planes requeridos son los permitidos
            description: f.description,
          })
        );
        return this.enrichPlanFeaturesWithPermissions(planFeatures);
      }),
      catchError((error) => {
        this.logger.error(
          '[PermissionAdminService] Error al obtener features con permisos de planes:',
          error
        );
        // Si falla, retornar array vacío (NO usar endpoint alternativo)
        return of([]);
      })
    );
  }

  /**
   * Enriquecer features del menú con permisos de planes del backend
   */
  private enrichPlanFeaturesWithPermissions(
    menuFeatures: FeaturePlanPermission[]
  ): Observable<FeaturePlanPermission[]> {
    return this.http
      .get<FeaturePlanPermission[]>(`${this.apiUrl}/features/plans`)
      .pipe(
        map((backendFeatures) => {
          // Crear mapa de features del backend (solo para permisos de planes, no para nombres)
          // Los nombres siempre vienen del menú (menu.name)
          const backendMap = new Map<string, FeaturePlanPermission>();
          backendFeatures.forEach((f) => {
            const feature: FeaturePlanPermission = {
              ...f,
              // El nombre se ignorará, siempre usamos menu.name
              name: f.name, // Solo para completar el objeto, no se usará
              // El backend usa requiredPlans, pero nuestro modelo usa allowedPlans
              allowedPlans: (f as any).requiredPlans || f.allowedPlans,
            };
            backendMap.set(f.featureCode, feature);
          });

          // Combinar: usar features del menú (con nombres) pero con permisos de planes del backend
          // El nombre siempre viene de menu.name (del endpoint /api/menu/{userName})
          return menuFeatures.map((menuFeature) => {
            const backendFeature = backendMap.get(menuFeature.featureCode);
            if (backendFeature) {
              return {
                ...menuFeature,
                // Siempre usar menu.name del endpoint /api/menu/{userName}
                name: menuFeature.name,
                allowedPlans: backendFeature.allowedPlans,
                // Priorizar descripción del backend si existe y es mejor, sino usar la del menú
                description:
                  backendFeature.description || menuFeature.description,
              };
            }
            // Si no hay backend feature, retornar el feature del menú tal cual
            return menuFeature;
          });
        }),
        catchError(() => {
          // Si falla, retornar solo features del menú sin permisos
          return of(menuFeatures);
        })
      );
  }

  // ============================================
  // MÉTODOS NUEVOS PARA GESTIÓN DE PERMISOS DE PLANES (billing 8480)
  // Estos métodos usan los nuevos endpoints SOLO para la pantalla de administración
  // ============================================

  /**
   * Obtener features y planes para gestión de permisos de planes
   * Usa la nueva API en billing (8480): GET /api/permissions/features/plans
   * SOLO para la pantalla de administración de permisos de planes
   */
  getPlanFeaturesForManagement(): Observable<{
    systemFeatures: SystemFeature[];
    planFeatures: PlanFeaturesResponse[];
  }> {
    return forkJoin({
      systemFeatures: this.http.get<SystemFeature[]>(
        `${this.billingPermissionsUrl}/features`
      ),
      planFeatures: this.http.get<PlanFeaturesResponse[]>(
        `${this.billingPermissionsUrl}/features/plans`
      ),
    });
  }

  /**
   * Actualización masiva de permisos de planes usando la nueva API
   * Usa la nueva API en billing (8480): PUT /api/permissions/plans/bulk
   * SOLO para la pantalla de administración de permisos de planes
   */
  bulkUpdatePlanFeatures(
    request: BulkPlanFeaturesUpdateRequest
  ): Observable<BulkUpdateResponse> {
    return this.http.put<BulkUpdateResponse>(
      `${this.billingPermissionsUrl}/plans/bulk`,
      request
    );
  }

  /**
   * Transformar datos de la nueva API a FeaturePlanPermission[] para el componente
   * Helper para convertir PlanFeaturesResponse[] + SystemFeature[] a FeaturePlanPermission[]
   * 
   * IMPORTANTE: Usa los menús para obtener el módulo dinámicamente (NO hardcodeado)
   */
  transformPlanFeaturesToFeaturePlanPermissions(
    systemFeatures: SystemFeature[],
    planFeatures: PlanFeaturesResponse[]
  ): Observable<FeaturePlanPermission[]> {
    // Obtener menús para determinar módulos dinámicamente (NO usar fallbacks hardcodeados)
    return this.getAllMenus().pipe(
      map((menus) => {
        // Extraer módulos desde los menús (igual que getAllFeatures)
        const menuFeatures = this.extractFeaturesFromMenu(menus);
        const moduleMap = new Map<string, string>();
        menuFeatures.forEach((feature) => {
          moduleMap.set(feature.featureCode, feature.module);
        });

        const featureMap = new Map<string, FeaturePlanPermission>();

        systemFeatures.forEach((systemFeature) => {
          const booleanKey = this.getBooleanKeyFromFeatureCode(systemFeature.code);
          
          // Obtener módulo desde menús (dinámico) - NO usar fallbacks
          const module = moduleMap.get(systemFeature.code);
          
          // Si no hay módulo en los menús, omitir esta feature o lanzar error
          if (!module) {
            this.logger.warn(
              `[PermissionAdminService] Feature ${systemFeature.code} no encontrada en menús, omitiendo...`
            );
            return; // Omitir features que no están en los menús
          }

          if (!booleanKey) {
            // Si no hay mapeo, crear feature sin permisos de planes
            featureMap.set(systemFeature.code, {
              featureCode: systemFeature.code,
              name: systemFeature.name,
              module: module, // Módulo desde menús (dinámico)
              allowedPlans: [],
              description: systemFeature.description,
            });
            return;
          }

          // Encontrar qué planes tienen esta feature habilitada
          const allowedPlans: string[] = [];
          planFeatures.forEach((plan) => {
            if (plan.features[booleanKey]) {
              allowedPlans.push(plan.planCode);
            }
          });

          featureMap.set(systemFeature.code, {
            featureCode: systemFeature.code,
            name: systemFeature.name,
            module: module, // Módulo desde menús (dinámico)
            allowedPlans,
            description: systemFeature.description,
          });
        });

        return Array.from(featureMap.values());
      }),
      catchError((error) => {
        this.logger.error(
          '[PermissionAdminService] Error al obtener menús para módulos:',
          error
        );
        // Si falla obtener menús, retornar array vacío (NO usar fallbacks hardcodeados)
        // El componente debe manejar el caso de features vacías
        return of([]);
      })
    );
  }

  /**
   * Transformar cambios del componente al formato de la nueva API
   * Helper para convertir { "featurePlans": { "FEATURE_CODE": [planCodes] } }
   * a { "updates": [{ "planCode": "...", "features": {...} }] }
   */
  transformChangesToBulkRequest(
    changes: Map<string, string[]>,
    currentPlans: PlanFeaturesResponse[]
  ): BulkPlanFeaturesUpdateRequest {
    // Crear mapa de planCode -> PlanFeatures actuales
    const planFeaturesMap = new Map<string, PlanFeatures>();
    currentPlans.forEach((plan) => {
      planFeaturesMap.set(plan.planCode, plan.features);
    });

    // Construir el nuevo formato de request
    const updates: Array<{
      planCode: string;
      features: Partial<PlanFeatures>;
    }> = [];

    // Para cada feature code en los cambios
    changes.forEach((allowedPlans, featureCode) => {
      const booleanKey = this.getBooleanKeyFromFeatureCode(featureCode);

      if (!booleanKey) {
        this.logger.warn(
          `[PermissionAdminService] Feature code ${featureCode} no tiene mapeo a formato booleano`
        );
        return;
      }

      // Para cada plan, actualizar el feature correspondiente
      currentPlans.forEach((plan) => {
        const shouldHaveFeature = allowedPlans.includes(plan.planCode);
        const currentFeatures =
          planFeaturesMap.get(plan.planCode) || plan.features;

        // Buscar o crear el update para este plan
        let planUpdate = updates.find((u) => u.planCode === plan.planCode);
        if (!planUpdate) {
          planUpdate = {
            planCode: plan.planCode,
            features: {},
          };
          updates.push(planUpdate);
        }

        // Solo agregar al update si el valor cambió
        if (currentFeatures[booleanKey] !== shouldHaveFeature) {
          planUpdate.features[booleanKey] = shouldHaveFeature;
        }
      });
    });

    return { updates };
  }


  /**
   * Obtener permisos de planes para un feature específico
   * Endpoint: GET /api/permission/permissions/feature/{featureCode}/plans
   */
  getFeaturePlanPermissions(
    featureCode: string
  ): Observable<FeaturePlanPermission> {
    return this.http
      .get<any>(`${this.apiUrl}/feature/${featureCode}/plans`)
      .pipe(
        map((response) => {
          // Mapear formato del backend a nuestro modelo
          return {
            featureCode: response.featureCode,
            name: response.featureName || response.name,
            module: response.module,
            allowedPlans: response.requiredPlans || response.allowedPlans,
            description: response.description,
          } as FeaturePlanPermission;
        })
      );
  }

  /**
   * Actualizar permisos de planes para un feature
   * Endpoint: PUT /api/permission/permissions/feature/{featureCode}/plans
   * Formato del backend: { "requiredPlans": [planCodes] }
   */
  updateFeaturePlans(
    featureCode: string,
    allowedPlans: string[]
  ): Observable<FeaturePlanPermission> {
    // El backend espera el formato: { "requiredPlans": [planCodes] }
    return this.http.put<FeaturePlanPermission>(
      `${this.apiUrl}/feature/${featureCode}/plans`,
      {
        requiredPlans: allowedPlans,
      }
    );
  }

  /**
   * Actualización masiva de permisos de planes
   * Usa la nueva API en billing (8480): PUT /api/permissions/plans/bulk
   * Transforma el formato de request desde FeaturePlanPermission[] al nuevo formato
   */
  bulkUpdatePlanPermissions(
    request: BulkPlanPermissionUpdateRequest
  ): Observable<BulkUpdateResponse> {
    // Transformar desde { "featurePlans": { "FEATURE_CODE": [planCodes] } }
    // al nuevo formato { "updates": [{ "planCode": "...", "features": {...} }] }

    // Primero obtener los planes actuales para saber qué features actualizar
    return this.http
      .get<PlanFeaturesResponse[]>(
        `${this.billingPermissionsUrl}/features/plans`
      )
      .pipe(
        switchMap((currentPlans) => {
          // Crear mapa de planCode -> PlanFeatures actuales
          const planFeaturesMap = new Map<string, PlanFeatures>();
          currentPlans.forEach((plan) => {
            planFeaturesMap.set(plan.planCode, plan.features);
          });

          // Construir el nuevo formato de request
          const updates: Array<{
            planCode: string;
            features: Partial<PlanFeatures>;
          }> = [];

          // Para cada feature code en el request
          Object.keys(request.featurePlans).forEach((featureCode) => {
            const allowedPlans = request.featurePlans[featureCode];
            const booleanKey = this.getBooleanKeyFromFeatureCode(featureCode);

            if (!booleanKey) {
              this.logger.warn(
                `[PermissionAdminService] Feature code ${featureCode} no tiene mapeo a formato booleano`
              );
              return;
            }

            // Para cada plan, actualizar el feature correspondiente
            currentPlans.forEach((plan) => {
              const shouldHaveFeature = allowedPlans.includes(plan.planCode);
              const currentFeatures =
                planFeaturesMap.get(plan.planCode) || plan.features;

              // Buscar o crear el update para este plan
              let planUpdate = updates.find(
                (u) => u.planCode === plan.planCode
              );
              if (!planUpdate) {
                planUpdate = {
                  planCode: plan.planCode,
                  features: {},
                };
                updates.push(planUpdate);
              }

              // Solo agregar al update si el valor cambió
              if (currentFeatures[booleanKey] !== shouldHaveFeature) {
                planUpdate.features[booleanKey] = shouldHaveFeature;
              }
            });
          });

          const bulkRequest: BulkPlanFeaturesUpdateRequest = { updates };

          // Enviar request al nuevo endpoint
          return this.http.put<BulkUpdateResponse>(
            `${this.billingPermissionsUrl}/plans/bulk`,
            bulkRequest
          );
        }),
        catchError((error) => {
          this.logger.error(
            '[PermissionAdminService] Error en bulk update de permisos de planes:',
            error
          );
          throw error;
        })
      );
  }
}
