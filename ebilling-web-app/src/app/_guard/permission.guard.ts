import { Injectable } from '@angular/core';
import {
  CanActivate,
  ActivatedRouteSnapshot,
  RouterStateSnapshot,
  Router,
  UrlTree,
} from '@angular/router';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { PermissionService } from '../_service/permission/permission.service';
import { ToastService } from '../_service/notification/toast.service';
import { MenuService } from '../_service/auth/menu.service';
import { MenuFilterService } from '../_service/menu/menu-filter.service';
import { UserRoleService } from '../_service/auth/user-role.service';
import { Menu } from '../_model/auth/menu';

/**
 * PermissionGuard
 *
 * Guard para proteger rutas basado en permisos (ROL + SUSCRIPCIÓN)
 *
 * Uso en routing:
 * ```typescript
 * {
 *   path: 'reports/advanced',
 *   canActivate: [SessionValidationGuard, PermissionGuard],
 *   data: { permission: 'reports.advanced' },
 *   component: SalesDashboardComponent
 * }
 * ```
 *
 * Super admin siempre tiene acceso.
 * Si el usuario no tiene permiso, redirige a /welcome o /subscription según el motivo.
 */
@Injectable({
  providedIn: 'root',
})
export class PermissionGuard implements CanActivate {
  // Cache de errores mostrados para evitar spam de toasts
  private readonly shownErrors = new Set<string>();
  private readonly ERROR_CACHE_TTL = 60000; // 1 minuto

  constructor(
    private readonly permissionService: PermissionService,
    private readonly router: Router,
    private readonly toastService: ToastService,
    private readonly menuService: MenuService,
    private readonly menuFilterService: MenuFilterService,
    private readonly userRoleService: UserRoleService
  ) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean | UrlTree> | boolean | UrlTree {
    const requiredPermission = route.data['permission'] as string;

    if (!requiredPermission) {
      return true;
    }

    // Verificar si el usuario es super_admin
    // super_admin puede acceder sin validar permisos de suscripción
    if (this.permissionService.isSuperAdmin()) {
      return true;
    }

    // 🔍 Validar usando canAccessWithDetails para determinar dinámicamente
    // si el permiso requiere solo rol o rol+plan basándose en la respuesta del backend

    // Para otros permisos, usar validación normal (ROL + PLAN)
    const subscription = this.permissionService.getSubscriptionValue();
    const hasActiveSubscription =
      subscription && subscription.status === 'ACTIVE';

    // Sin suscripción activa no se permite acceso: redirigir a welcome con mensaje
    if (!hasActiveSubscription) {
      this.toastService.warning(
        'No tiene suscripción activa para esta compañía. Contacte al administrador.',
      );
      return this.router.createUrlTree(['/welcome']);
    }

    // Verificar permiso con detalles (valida ROL + PLAN)
    return this.permissionService.canAccessWithDetails(requiredPermission).pipe(
      map((response) => {
        if (response.allowed) {
          return true;
        }

        // 🔍 PRIORIDAD 1: Analizar dinámicamente si el permiso requiere solo rol o rol+plan
        // Si el motivo es relacionado con plan (NO_SUBSCRIPTION, PLAN_DOES_NOT_INCLUDE_FEATURE, PLAN_NOT_ALLOWED),
        // y es un permiso administrativo (solo requiere rol), verificar rol directamente
        let finalReason = response.reason;
        const isPlanRelatedReason =
          response.reason === 'NO_SUBSCRIPTION' ||
          response.reason === 'PLAN_DOES_NOT_INCLUDE_FEATURE' ||
          response.reason === 'PLAN_NOT_ALLOWED';

        if (
          isPlanRelatedReason &&
          this.isAdminFeatureCode(requiredPermission)
        ) {
          // Es un permiso administrativo que solo requiere rol
          const roles = this.userRoleService.getCurrentRolesSync();
          const isAdmin =
            roles.includes('admin') || roles.includes('super_admin');

          if (isAdmin) {
            // Tiene el rol adecuado, permitir acceso
            console.debug(
              '[PermissionGuard] ✅ Permiso concedido - Permiso administrativo (solo requiere rol)',
              {
                permission: requiredPermission,
                userRoles: roles,
              }
            );
            return true;
          } else {
            // Es un permiso administrativo pero el usuario NO tiene el rol adecuado
            // Cambiar el motivo a NO_ROLE para mostrar el mensaje correcto
            console.debug(
              '[PermissionGuard] Permiso administrativo - Cambiando motivo de PLAN a NO_ROLE',
              {
                originalReason: response.reason,
                permission: requiredPermission,
                userRoles: roles,
              }
            );
            finalReason = 'NO_ROLE';
          }
        }

        // 🔍 PRIORIDAD 2: Si la razón final es NO_ROLE, SIEMPRE mostrar el mensaje
        // Esto cubre casos donde:
        // - El motivo original ya era NO_ROLE
        // - El motivo fue cambiado a NO_ROLE porque es un permiso administrativo sin rol adecuado
        const isNoRoleReason = finalReason === 'NO_ROLE';
        if (isNoRoleReason) {
          // NO verificar si el menú existe en el listado, directamente mostrar el mensaje
          // Continuar con el flujo para mostrar el mensaje (saltarse la verificación del menú)
        } else {
          // 🔍 PRIORIDAD 3: Verificar si el menú existe en el listado filtrado
          // EXCEPCIÓN: /setting está en el dropdown del menú de usuario, no en el sidebar
          // Usar state.url (URL de destino) en lugar de router.url (URL actual)
          const targetUrl = state.url;
          const isSettingRoute = targetUrl.startsWith('/setting');

          // Si NO es la ruta /setting, verificar si el menú existe en el listado filtrado
          if (!isSettingRoute) {
            const menus = this.menuService.menuChange.getValue();
            const menuExists = this.menuExistsInFilteredList(
              targetUrl,
              menus,
              requiredPermission
            );

            if (!menuExists) {
              // El menú no existe en el listado filtrado, redirigir silenciosamente
              // Esto significa que MenuFilterService ya lo excluyó correctamente
              // NO mostrar mensaje porque el menú ya está oculto en el sidebar
              return this.router.createUrlTree(['/welcome']);
            }
          }
        }

        // El menú existe en el listado filtrado pero el usuario no tiene permiso
        // O el motivo es NO_ROLE (ya sea original o convertido desde un motivo de plan)
        // Esto puede pasar si:
        // 1. El usuario accedió directamente por URL sin tener permisos
        // 2. Hay una desincronización entre el filtrado y la protección de rutas
        // 3. El usuario tiene un permiso administrativo pero no el rol adecuado


        // Crear una clave única para este error
        const errorKey = `${requiredPermission}-${finalReason}`;

        // Si es NO_ROLE, siempre mostrar el mensaje (no usar caché para estos casos críticos)
        // Para otros motivos, usar caché para evitar spam
        const shouldShowMessage =
          isNoRoleReason || !this.shownErrors.has(errorKey);

        if (shouldShowMessage) {
          // Si no es NO_ROLE, agregar al caché para evitar spam
          if (!isNoRoleReason) {
            this.shownErrors.add(errorKey);

            // Limpiar el error del cache después del TTL
            setTimeout(() => {
              this.shownErrors.delete(errorKey);
            }, this.ERROR_CACHE_TTL);
          }

          // Mostrar mensaje según el motivo final (corregido si es necesario)
          switch (finalReason) {
            case 'PLAN_DOES_NOT_INCLUDE_FEATURE':
            case 'NO_SUBSCRIPTION':
            case 'PLAN_NOT_ALLOWED':
              this.toastService.warning(
                'Esta funcionalidad requiere un plan superior. Actualiza tu suscripción.'
              );
              break;

            case 'NO_ACTIVE_SUBSCRIPTION':
            case 'EXPIRED':
              this.toastService.error(
                'Tu suscripción ha expirado. Renueva tu plan.'
              );
              break;

            case 'LIMIT_EXCEEDED':
              this.toastService.warning(
                'Has alcanzado el límite de tu plan. Actualiza tu suscripción.'
              );
              break;

            case 'NO_ROLE':
            case 'INSUFFICIENT_PERMISSIONS':
            default:
              this.toastService.error(
                'No tienes permisos para acceder a esta sección.'
              );
              break;
          }
        }

        // ✅ Redirigir a welcome (dashboard)
        return this.router.createUrlTree(['/welcome']);
      })
    );
  }

  /**
   * Verifica si un menú existe en el listado filtrado
   * Busca recursivamente en menús y submenús
   * También verifica por featureCode si está disponible
   * @param url URL a buscar
   * @param menus Lista de menús filtrados
   * @param featureCode Feature code a buscar (opcional, para verificación adicional)
   * @returns true si el menú existe en el listado
   */
  private menuExistsInFilteredList(
    url: string,
    menus: Menu[],
    featureCode?: string
  ): boolean {
    if (!menus || menus.length === 0) {
      return false;
    }

    // Normalizar la URL (remover query params y fragmentos)
    const normalizedUrl = url.split('?')[0].split('#')[0].toLowerCase();

    // Buscar recursivamente en menús y submenús
    for (const menu of menus) {
      // Verificar si la URL del menú coincide
      if (menu.url && menu.url !== 'null') {
        const menuUrl = menu.url.split('?')[0].split('#')[0].toLowerCase();
        // ✅ Obtener featureCode del menú (viene del backend) o usar MenuFilterService como fallback
        const menuFeatureCode =
          menu.featureCode ||
          this.menuFilterService.getFeatureCodeFromUrl(menu.url);


        // Comparación exacta o parcial de URL
        const urlMatches =
          normalizedUrl === menuUrl ||
          normalizedUrl.startsWith(menuUrl + '/') ||
          menuUrl.startsWith(normalizedUrl + '/');

        // Si hay featureCode, también verificar por featureCode
        if (urlMatches) {
          if (featureCode && menuFeatureCode) {
            // Si ambos tienen featureCode, deben coincidir
            if (featureCode === menuFeatureCode) {
              return true;
            }
          } else {
            // Si no hay featureCode para comparar, solo verificar URL
            return true;
          }
        }
      }

      // Buscar en submenús recursivamente
      if (menu.subMenus && menu.subMenus.length > 0) {
        if (
          this.menuExistsInFilteredList(
            normalizedUrl,
            menu.subMenus,
            featureCode
          )
        ) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Determina dinámicamente si un featureCode es administrativo (solo requiere validación por rol)
   * Basado en patrones del nombre del featureCode, sin hardcodear valores específicos
   *
   * @param featureCode Código de la feature
   * @returns true si es un permiso administrativo que solo requiere rol
   */
  private isAdminFeatureCode(featureCode: string): boolean {
    if (!featureCode) {
      return false;
    }

    // Patrones que indican permisos administrativos (solo requieren rol, no plan)
    const adminPatterns = [
      'SETTINGS', // Configuración (COMPANY_SETTINGS, etc.)
      'MANAGEMENT', // Gestión (USER_MANAGEMENT, COMPANY_USER_MANAGEMENT, etc.)
      'ADMIN', // Administración
      'GLOBAL', // Administración global
    ];

    // Verificar si el featureCode contiene alguno de los patrones
    return adminPatterns.some((pattern) => featureCode.includes(pattern));
  }
}
