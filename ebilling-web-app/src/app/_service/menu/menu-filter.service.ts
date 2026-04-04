import { Injectable } from '@angular/core';
import { Menu } from '../../_model/auth/menu';
import { PermissionService } from '../permission/permission.service';
import { Observable, forkJoin, of } from 'rxjs';
import { map, catchError, switchMap, filter, timeout } from 'rxjs/operators';
import { CompanySubscription } from '../../_model/subscription/company-subscription';
import { LoggerService } from '../logger/logger.service';
import { decodeToken } from '../../_functions/functions';
import { environment } from '../../../environments/environment';
import { UserRoleService } from '../auth/user-role.service';

/**
 * MenuFilterService
 *
 * Filtra items del menú basándose en permisos de suscripción de forma dinámica.
 *
 * Comportamiento:
 * - super_admin (bypass completo):
 *   Ven todos los menús sin validar permisos
 * - Cualquier otro rol:
 *   - Si tiene suscripción activa: Filtra menús según permisos de suscripción
 *   - Si NO tiene suscripción activa: No muestra menús (acceso bloqueado hasta tener plan asignado)
 *
 * El sistema es completamente dinámico - no requiere modificar código al agregar nuevos roles.
 * Solo agrega el rol a la lista de bypass en PermissionService si necesita acceso completo.
 */
@Injectable({
  providedIn: 'root',
})
export class MenuFilterService {
  // Mapeo de URLs del menú a feature codes (fallback si el backend no envía featureCode)
  // NOTA: El backend ahora envía featureCode en cada menú hoja, este mapeo es solo fallback
  private readonly urlToFeatureMap: { [key: string]: string } = {
    // Reportes operativos (granulares)
    'sales-by-period': 'REPORT_SALES_BY_PERIOD',
    'sales-by-client': 'REPORT_SALES_BY_CLIENT',
    'sales-by-product': 'REPORT_SALES_BY_PRODUCT',
    'credit-notes': 'REPORT_CREDIT_NOTES',
    'pending-invoices': 'REPORT_PENDING_INVOICES',

    // Reportes comerciales/avanzados (granulares)
    'sales-dashboard': 'REPORT_SALES_DASHBOARD',
    'branch-comparison': 'REPORT_BRANCH_COMPARISON',
    'payment-methods': 'REPORT_PAYMENT_METHODS',

    // Facturación
    '/invoices': 'INVOICE_CREATE',
    '/proforma': 'PROFORMA_CREATE',
    '/credit-note': 'CREDIT_NOTE_CREATE',
    '/drafts': 'DRAFT_MANAGEMENT',
    '/delivery-guides': 'DELIVERY_GUIDE_CREATE',
    '/debit-note': 'DEBIT_NOTE_CREATE',

    // Clientes
    '/clients': 'CLIENT_CREATE',

    // Transportistas
    '/carriers': 'CARRIER_CREATE',

    // Productos
    '/products': 'PRODUCT_MANAGEMENT',

    // Usuarios
    '/users': 'USER_MANAGEMENT',
    '/company-users': 'COMPANY_USER_MANAGEMENT',

    // Compañías
    '/companies': 'COMPANY_SETTINGS',
    '/setting': 'COMPANY_SETTINGS', // Configuración de firma digital (solo admin y super_admin)

    // Establecimientos
    '/establishments': 'ESTABLISHMENT_MANAGEMENT',
    '/invoice-points': 'INVOICE_POINT_MANAGEMENT',

    // Administración Global (Super Admin)
    '/admin/subscriptions': 'SUBSCRIPTION_MANAGEMENT',
    '/admin/plan-management': 'SUBSCRIPTION_MANAGEMENT', // Gestión de planes de suscripción
    '/admin/role-permissions': 'ROLE_PERMISSION_MANAGEMENT',
    '/admin/menus': 'MENU_MANAGEMENT',
    '/admin/establishments': 'GLOBAL_ESTABLISHMENT_MANAGEMENT',
    '/admin/invoice-points': 'GLOBAL_INVOICE_POINT_MANAGEMENT',
  };

  constructor(
    private readonly permissionService: PermissionService,
    private readonly logger: LoggerService,
    private readonly userRoleService: UserRoleService,
  ) {}

  /**
   * Filtra un array de menús según los permisos del usuario
   * Solo super_admin ve todos los menús sin filtrar
   * Admin y otros roles deben validar permisos de suscripción
   */
  filterMenus(menus: Menu[]): Observable<Menu[]> {
    this.logger.debug(
      `[MenuFilterService] Filtrando ${
        menus?.length || 0
      } menús recibidos del backend`,
    );

    // 🔍 LOG: Imprimir menús completos recibidos para filtrar
    this.logger.debug('📋 [MenuFilterService] Menús completos recibidos:', {
      totalMenus: menus?.length || 0,
      menus: menus?.map((m) => ({
        id: m.id,
        name: m.name,
        url: m.url,
        icon: m.icon,
        featureCode: m.featureCode,
        subMenusCount: m.subMenus?.length || 0,
        subMenus: m.subMenus?.map((sm) => ({
          id: sm.id,
          name: sm.name,
          url: sm.url,
          icon: sm.icon,
          featureCode: sm.featureCode,
        })),
      })),
      fullMenus: menus,
    });

    // Log detallado de todos los menús recibidos para debugging
    if (menus && menus.length > 0) {
      // Buscar específicamente el menú de permisos (en menús principales y submenús)
      let permisosMenu: Menu | undefined;
      const buscarEnSubMenus = (menuList: Menu[]): Menu | undefined => {
        for (const m of menuList) {
          if (
            m.url === '/admin/role-permissions' ||
            m.name?.toLowerCase().includes('permisos')
          ) {
            return m;
          }
          if (m.subMenus && m.subMenus.length > 0) {
            const encontrado = buscarEnSubMenus(m.subMenus);
            if (encontrado) return encontrado;
          }
        }
        return undefined;
      };
      permisosMenu = buscarEnSubMenus(menus);

      // Log detallado de todos los menús y sus submenús
      const logMenus = (menuList: Menu[], nivel = 0): any[] => {
        return menuList.map((m) => ({
          id: m.id,
          name: m.name,
          url: m.url,
          featureCode: m.featureCode,
          nivel,
          hasSubMenus: !!(m.subMenus && m.subMenus.length > 0),
          subMenusCount: m.subMenus?.length || 0,
          subMenus: m.subMenus ? logMenus(m.subMenus, nivel + 1) : undefined,
        }));
      };

      // Buscar el menú "Administración Global" y verificar sus submenús
      const adminGlobalMenu = menus.find(
        (m) =>
          m.name?.toLowerCase().includes('administración') ||
          m.name?.toLowerCase().includes('admin') ||
          m.id === 16,
      );

      this.logger.debug('[MenuFilterService] Menús recibidos del backend:', {
        totalMenus: menus.length,
        menus: logMenus(menus),
        menuPermisosEncontrado: permisosMenu
          ? {
              id: permisosMenu.id,
              name: permisosMenu.name,
              url: permisosMenu.url,
              featureCode: permisosMenu.featureCode,
              nivel: permisosMenu.subMenus ? 'grupo' : 'item',
            }
          : 'NO ENCONTRADO - El backend no está enviando este menú',
        menuAdministracionGlobal: adminGlobalMenu
          ? {
              id: adminGlobalMenu.id,
              name: adminGlobalMenu.name,
              url: adminGlobalMenu.url,
              subMenusCount: adminGlobalMenu.subMenus?.length || 0,
              subMenus: adminGlobalMenu.subMenus?.map((sm) => ({
                id: sm.id,
                name: sm.name,
                url: sm.url,
                featureCode: sm.featureCode,
                icon: sm.icon,
              })),
              tieneMenuPermisos: adminGlobalMenu.subMenus?.some(
                (sm) =>
                  sm.url === '/admin/role-permissions' ||
                  sm.name?.toLowerCase().includes('permisos'),
              ),
              menuPermisosEnSubMenus: adminGlobalMenu.subMenus?.find(
                (sm) =>
                  sm.url === '/admin/role-permissions' ||
                  sm.name?.toLowerCase().includes('permisos'),
              ),
            }
          : 'NO ENCONTRADO',
        advertencia: !permisosMenu
          ? '⚠️ El menú "Gestión de Permisos" no está en la respuesta del backend. Verificar: 1) Si está asignado al rol en role_menu, 2) Si el backend está filtrando menús antes de enviar'
          : adminGlobalMenu &&
              !adminGlobalMenu.subMenus?.some(
                (sm) =>
                  sm.url === '/admin/role-permissions' ||
                  sm.name?.toLowerCase().includes('permisos'),
              )
            ? '⚠️ El menú "Gestión de Permisos" existe pero NO está dentro de "Administración Global". Verificar parent_id_menu en la BD.'
            : null,
      });
    }

    if (!menus || menus.length === 0) {
      this.logger.warn('[MenuFilterService] No hay menús para filtrar', {
        menus,
      });
      return of([]);
    }

    // PRIORIDAD 1: Verificar si el usuario es super_admin globalmente (desde el token)
    // super_admin es un rol global del sistema, no depende de la compañía
    const isSuperAdminGlobal = this.userRoleService.isSuperAdminGlobal();

    if (isSuperAdminGlobal) {
      this.logger.debug(
        '[MenuFilterService] Usuario es super_admin (rol global), mostrando todos los menús',
      );
      return of(this.sortMenusByOrder(menus));
    }

    // PRIORIDAD 2: Verificar roles por compañía (fallback)
    const roles = this.userRoleService.getCurrentRolesSync();
    const isSuperAdminByCompany =
      roles && roles.length > 0 && roles.includes('super_admin');

    // Si los roles aún no se han cargado, esperar a que se carguen
    if (roles.length === 0) {
      this.logger.debug(
        '[MenuFilterService] Roles aún no cargados, esperando a que se carguen...',
        {
          idCompany: localStorage.getItem(environment.ID_COMPANY),
        },
      );
      // Esperar a que los roles se carguen (esperar hasta que haya roles o timeout)
      return this.userRoleService.getCurrentCompanyRoles().pipe(
        timeout(3000), // Timeout de 3 segundos
        switchMap((loadedRoles) => {
          // Verificar nuevamente si es super_admin global después de cargar
          if (this.userRoleService.isSuperAdminGlobal()) {
            this.logger.debug(
              '[MenuFilterService] Usuario es super_admin (rol global después de cargar), mostrando todos los menús',
            );
            return of(this.sortMenusByOrder(menus));
          }

          const isSuperAdminAfterLoad =
            loadedRoles &&
            loadedRoles.length > 0 &&
            loadedRoles.includes('super_admin');
          if (isSuperAdminAfterLoad || this.permissionService.isSuperAdmin()) {
            this.logger.debug(
              `[MenuFilterService] Usuario es super_admin (roles: ${
                loadedRoles?.join(', ') || 'N/A'
              }), mostrando todos los menús`,
            );
            return of(this.sortMenusByOrder(menus));
          }
          // Continuar con el filtrado normal
          return this.continueWithFiltering(menus);
        }),
        catchError(() => {
          // En caso de error o timeout, verificar con permissionService como fallback
          this.logger.warn(
            '[MenuFilterService] Error o timeout al cargar roles, usando fallback',
          );
          if (
            this.userRoleService.isSuperAdminGlobal() ||
            this.permissionService.isSuperAdmin()
          ) {
            this.logger.debug(
              '[MenuFilterService] Usuario es super_admin (fallback), mostrando todos los menús',
            );
            return of(this.sortMenusByOrder(menus));
          }
          // Continuar con el filtrado normal
          return this.continueWithFiltering(menus);
        }),
      );
    }

    if (isSuperAdminByCompany || this.permissionService.isSuperAdmin()) {
      this.logger.debug(
        `[MenuFilterService] Usuario es super_admin (roles: ${
          roles?.join(', ') || 'N/A'
        }), mostrando todos los menús`,
      );
      // Ordenar menús por campo order antes de retornar
      return of(this.sortMenusByOrder(menus));
    }

    // Continuar con el filtrado normal (verificar suscripción)
    return this.continueWithFiltering(menus);
  }

  /**
   * Continúa con el filtrado normal (verificar suscripción y permisos)
   */
  private continueWithFiltering(menus: Menu[]): Observable<Menu[]> {
    // Verificar si hay suscripción activa para validar permisos
    // Cualquier rol (excepto super_admin) necesita suscripción activa para filtrar menús
    // Si no hay suscripción activa, mostrar todos los menús temporalmente
    // Esto permite que cualquier rol funcione sin necesidad de modificar código
    const subscription = this.permissionService.getSubscriptionValue();

    // Si no hay suscripción cargada aún, esperar un momento y reintentar
    if (!subscription) {
      this.logger.debug(
        '[MenuFilterService] Suscripción no cargada aún, esperando...',
        { idCompany: localStorage.getItem(environment.ID_COMPANY) },
      );
      return this.permissionService.getSubscription().pipe(
        filter((sub) => sub !== null),
        timeout(3000),
        switchMap((sub) => {
          const hasActiveSubscription =
            sub?.status === 'ACTIVE' ||
            (sub?.status === undefined && sub?.plan?.code);
          if (!sub || !hasActiveSubscription) {
            this.logger.warn(
              '[MenuFilterService] Sin suscripción activa, no se muestran menús',
              {
                hasSubscription: !!sub,
                status: sub?.status,
                planCode: sub?.plan?.code,
              },
            );
            return of([]);
          }
          return this.continueFiltering(menus, sub);
        }),
        catchError(() => {
          this.logger.warn(
            '[MenuFilterService] Error o timeout al obtener suscripción, no se muestran menús',
          );
          return of([]);
        }),
      );
    }

    const hasActiveSubscription =
      subscription.status === 'ACTIVE' ||
      (subscription.status === undefined && subscription.plan?.code);

    // Sin suscripción activa no se muestran menús (no hay fallback)
    if (!subscription.plan?.code || !hasActiveSubscription) {
      this.logger.warn(
        '[MenuFilterService] Usuario sin plan/suscripción activa, no se muestran menús',
        {
          status: subscription?.status,
          planCode: subscription?.plan?.code,
        },
      );
      return of([]);
    }

    // Si tiene planCode, filtrar según permisos (incluso si status es undefined)
    this.logger.debug(
      '[MenuFilterService] Usuario con plan asignado, filtrando menús según permisos',
      {
        planCode: subscription.plan.code,
        status: subscription.status,
        hasActiveSubscription,
      },
    );
    return this.continueFiltering(menus, subscription);
  }

  /**
   * Continúa con el filtrado de menús cuando ya se tiene la suscripción
   */
  private continueFiltering(
    menus: Menu[],
    subscription: CompanySubscription,
  ): Observable<Menu[]> {
    // Si hay suscripción activa, filtrar según permisos de suscripción
    // Esto funciona dinámicamente para cualquier rol
    this.logger.debug(
      '[MenuFilterService] Usuario con suscripción activa, filtrando menús según permisos',
      {
        subscriptionStatus: subscription.status,
        planCode: subscription.plan?.code,
      },
    );

    // Verificar si hay idCompany disponible antes de filtrar
    const token = decodeToken();
    const idCompanyRaw = localStorage.getItem(environment.ID_COMPANY);
    const idCompany = idCompanyRaw
      ? Number(idCompanyRaw)
      : token?.idCompany || undefined;

    this.logger.debug('[MenuFilterService] Verificando idCompany:', {
      idCompanyRaw,
      idCompany,
      tokenIdCompany: token?.idCompany,
      roles: token?.roles,
    });

    if (!idCompany) {
      this.logger.warn(
        '[MenuFilterService] No hay idCompany disponible, mostrando solo menús sin feature code',
        {
          hasLocalStorage: !!idCompanyRaw,
          tokenIdCompany: token?.idCompany,
        },
      );
      // Si no hay idCompany, mostrar solo menús que no requieren feature code
      const menusWithoutFeature = menus.filter((menu) => {
        const featureCode =
          menu.featureCode || this.getFeatureCodeFromUrl(menu.url);
        return !featureCode; // Solo menús sin feature code
      });
      this.logger.debug(
        `[MenuFilterService] Menús sin feature code: ${menusWithoutFeature.length}`,
      );
      return of(this.sortMenusByOrder(menusWithoutFeature));
    }

    // Filtrar cada menú de nivel superior según permisos de suscripción
    this.logger.debug(
      `[MenuFilterService] Filtrando ${menus.length} menús con idCompany: ${idCompany}`,
    );

    const filteredMenus$ = menus.map((menu) => this.filterMenu(menu));

    return forkJoin(filteredMenus$).pipe(
      map((filteredMenus) => {
        const validMenus = filteredMenus.filter(
          (menu) => menu !== null,
        ) as Menu[];

        // Ordenar menús por campo order antes de retornar
        const sortedMenus = this.sortMenusByOrder(validMenus);

        this.logger.debug(
          `[MenuFilterService] Menús después de filtrar: ${sortedMenus.length} de ${menus.length}`,
        );
        return sortedMenus;
      }),
      catchError((error) => {
        this.logger.error('[MenuFilterService] Error al filtrar menús:', error);
        // En caso de error, mostrar solo menús sin feature code
        const menusWithoutFeature = menus.filter((menu) => {
          const featureCode =
            menu.featureCode || this.getFeatureCodeFromUrl(menu.url);
          return !featureCode;
        });
        this.logger.debug(
          `[MenuFilterService] En caso de error, mostrando ${menusWithoutFeature.length} menús sin feature code`,
        );
        return of(this.sortMenusByOrder(menusWithoutFeature));
      }),
    );
  }

  /**
   * Filtra un menú individual recursivamente
   */
  private filterMenu(menu: Menu): Observable<Menu | null> {
    // 🔒 Validación especial para menús de Administración Global
    // Estos menús solo deben mostrarse a super_admin
    if (this.isAdminGlobalMenu(menu)) {
      const roles = this.userRoleService.getCurrentRolesSync();
      const isSuperAdmin =
        roles && roles.length > 0 && roles.includes('super_admin');

      if (!isSuperAdmin && !this.permissionService.isSuperAdmin()) {
        this.logger.warn(
          `[MenuFilterService] Menú "${menu.name}" oculto - solo para super_admin`,
          {
            menuName: menu.name,
            menuUrl: menu.url,
            userRoles: roles,
            isSuperAdmin: false,
          },
        );
        return of(null); // ❌ Ocultar menú de administración global
      }
    }

    // Si tiene submenús, filtrarlos recursivamente (MENÚ PADRE)
    if (menu.subMenus && menu.subMenus.length > 0) {
      return this.filterSubMenus(menu);
    }

    // Si es un item final, verificar permisos (MENÚ HOJA)
    // 🔧 Priorizar featureCode del backend si existe
    const featureCode =
      menu.featureCode || this.getFeatureCodeFromUrl(menu.url);

    this.logger.debug(
      `[MenuFilterService] Filtrando menú: ${menu.name}, URL: ${menu.url}, featureCode: ${featureCode}`,
    );

    if (!featureCode) {
      // 🔒 Validación especial: Si es un menú de admin sin featureCode, verificar permisos
      if (this.isAdminGlobalMenu(menu)) {
        const roles = this.userRoleService.getCurrentRolesSync();
        const isSuperAdmin =
          roles && roles.length > 0 && roles.includes('super_admin');

        if (!isSuperAdmin && !this.permissionService.isSuperAdmin()) {
          this.logger.warn(
            `[MenuFilterService] Menú "${menu.name}" oculto - sin featureCode pero es de admin`,
            {
              menuName: menu.name,
              menuUrl: menu.url,
              userRoles: roles,
            },
          );
          return of(null);
        }
      }

      // Si no tiene feature code y no es de admin, permitir (es una ruta pública)
      this.logger.debug(
        `[MenuFilterService] Menú "${menu.name}" no requiere feature code, permitiendo`,
      );
      return of(menu);
    }

    // 🔍 Validar usando canAccessWithDetails para determinar dinámicamente
    // si el permiso requiere solo rol o rol+plan basándose en la respuesta del backend

    // Para otros permisos, usar validación normal (ROL + PLAN)
    const subscription = this.permissionService.getSubscriptionValue();
    this.logger.debug(
      `[MenuFilterService] Validando menú "${menu.name}" (${featureCode}) - ROL + PLAN:`,
      {
        menuUrl: menu.url,
        featureCode,
        planCode: subscription?.plan?.code,
        subscriptionStatus: subscription?.status,
        companyId: localStorage.getItem(environment.ID_COMPANY),
      },
    );

    // 🔒 Validación adicional: Si el featureCode es de administración global, verificar super_admin
    const isAdminFeature = this.isAdminGlobalFeatureCode(featureCode);
    if (isAdminFeature) {
      const roles = this.userRoleService.getCurrentRolesSync();
      const isSuperAdmin =
        roles && roles.length > 0 && roles.includes('super_admin');

      if (!isSuperAdmin && !this.permissionService.isSuperAdmin()) {
        this.logger.warn(
          `[MenuFilterService] Menú "${menu.name}" oculto - featureCode de admin sin permiso`,
          {
            menuName: menu.name,
            menuUrl: menu.url,
            featureCode,
            userRoles: roles,
          },
        );
        return of(null);
      }
    }

    // Usar canAccessWithDetails() para validar permisos
    // La respuesta del backend nos indica si requiere solo rol o rol+plan
    return this.permissionService.canAccessWithDetails(featureCode).pipe(
      map((response) => {
        const roles = this.userRoleService.getCurrentRolesSync();
        const subscription = this.permissionService.getSubscriptionValue();

        this.logger.debug(
          `[MenuFilterService] Validando "${menu.name}" (${featureCode}):`,
          {
            allowed: response.allowed,
            reason: response.reason,
          },
        );

        if (response.allowed) {
          this.logger.debug(
            `[MenuFilterService] Menú "${menu.name}" (${featureCode}): ✅ PERMITIDO`,
            {
              menuUrl: menu.url,
              planCode: subscription?.plan?.code,
              subscriptionStatus: subscription?.status,
              roles: roles,
            },
          );
          return menu;
        }

        // ❌ Menú denegado - El backend es la fuente de verdad
        // Si el backend retorna allowed: false, ocultar el menú
        // El motivo (reason) puede ser NO_ROLE, PLAN_NOT_ALLOWED, etc.
        // No debemos asumir que un permiso es "administrativo" basándonos en patrones del nombre
        // Solo confiar en la respuesta del backend
        this.logger.warn(
          `[MenuFilterService] ❌ Menú "${menu.name}" DENEGADO por falta de permiso ${featureCode}`,
          {
            menuName: menu.name,
            menuUrl: menu.url,
            menuId: menu.id,
            featureCode,
            planCode: subscription?.plan?.code,
            subscriptionStatus: subscription?.status,
            reason: response.reason,
            message: response.message,
            hasFeatureCodeInMenu: !!menu.featureCode,
            mappedFeatureCode: menu.featureCode
              ? null
              : this.getFeatureCodeFromUrl(menu.url),
            userRoles: roles,
          },
        );
        return null;
      }),
      catchError((error) => {
        this.logger.error(
          `[MenuFilterService] Error al validar permiso para "${menu.name}" (${featureCode}):`,
          {
            error,
            menuName: menu.name,
            menuUrl: menu.url,
            featureCode,
          },
        );
        // En caso de error, denegar acceso por seguridad
        return of(null);
      }),
    );
  }

  /**
   * Filtra submenús recursivamente
   */
  private filterSubMenus(menu: Menu): Observable<Menu | null> {
    if (!menu.subMenus || menu.subMenus.length === 0) {
      return of(menu);
    }

    const filteredSubMenus$ = menu.subMenus.map((subMenu) =>
      this.filterMenu(subMenu),
    );

    return forkJoin(filteredSubMenus$).pipe(
      map((filteredSubMenus) => {
        const validSubMenus = filteredSubMenus.filter(
          (subMenu) => subMenu !== null,
        ) as Menu[];

        if (validSubMenus.length === 0) {
          this.logger.debug(
            `[MenuFilterService] Menú padre "${menu.name}" oculto - no quedan submenús válidos`,
            {
              menuName: menu.name,
              menuUrl: menu.url,
              totalSubMenus: menu.subMenus.length,
              validSubMenus: 0,
              subMenusDetails: menu.subMenus.map((sm) => ({
                name: sm.name,
                url: sm.url,
                featureCode: sm.featureCode,
              })),
            },
          );
          return null;
        }

        this.logger.debug(
          `[MenuFilterService] Menú padre "${menu.name}" PERMITIDO - ${validSubMenus.length}/${menu.subMenus.length} submenús`,
        );

        return {
          ...menu,
          subMenus: validSubMenus,
        };
      }),
      catchError((error) => {
        this.logger.error(
          `[MenuFilterService] Error al filtrar submenús de "${menu.name}":`,
          error,
        );
        // En caso de error, ocultar el menú padre por seguridad
        return of(null);
      }),
    );
  }

  /**
   * Obtiene el feature code a partir de una URL del menú
   * Método público para ser usado por otros servicios (ej: PermissionGuard)
   */
  public getFeatureCodeFromUrl(url: string): string | null {
    if (!url || url === 'null') {
      return null;
    }

    // Buscar coincidencia exacta
    if (this.urlToFeatureMap[url]) {
      return this.urlToFeatureMap[url];
    }

    // Buscar coincidencia parcial
    for (const [urlPattern, featureCode] of Object.entries(
      this.urlToFeatureMap,
    )) {
      if (url.includes(urlPattern) || urlPattern.includes(url)) {
        return featureCode;
      }
    }

    // No requiere validación de feature
    return null;
  }

  /**
   * Verifica si un menú es de Administración Global
   * Los menús de administración global solo deben mostrarse a super_admin
   */
  private isAdminGlobalMenu(menu: Menu): boolean {
    // Verificar por nombre
    if (
      menu.name &&
      (menu.name.toLowerCase().includes('administración global') ||
        menu.name.toLowerCase().includes('administracion global') ||
        menu.name.toLowerCase().includes('admin global'))
    ) {
      return true;
    }

    // Verificar por URL (menús que contienen /admin/)
    if (menu.url && menu.url.includes('/admin/')) {
      return true;
    }

    // Verificar submenús (si algún submenú es de admin, el padre también lo es)
    if (menu.subMenus && menu.subMenus.length > 0) {
      return menu.subMenus.some((subMenu) => this.isAdminGlobalMenu(subMenu));
    }

    return false;
  }

  /**
   * Verifica si un featureCode es de Administración Global
   * Los featureCodes de admin solo deben mostrarse a super_admin
   */
  private isAdminGlobalFeatureCode(featureCode: string): boolean {
    if (!featureCode) {
      return false;
    }

    const adminFeatureCodes = [
      'ROLE_PERMISSION_MANAGEMENT',
      'SUBSCRIPTION_MANAGEMENT',
      'MENU_MANAGEMENT',
      'GLOBAL_ESTABLISHMENT_MANAGEMENT',
      'GLOBAL_INVOICE_POINT_MANAGEMENT',
    ];

    return adminFeatureCodes.includes(featureCode);
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

  /**
   * Ordena los menús por el campo order (menor = primero)
   * También ordena los submenús recursivamente
   *
   * @param menus Array de menús a ordenar
   * @returns Array de menús ordenados
   */
  private sortMenusByOrder(menus: Menu[]): Menu[] {
    if (!menus || menus.length === 0) {
      return menus;
    }

    // Ordenar menús principales por order (menor = primero)
    // Si no tienen order, se ordenan por id como fallback
    const sorted = [...menus].sort((a, b) => {
      const orderA = a.order ?? a.id ?? 999;
      const orderB = b.order ?? b.id ?? 999;
      return orderA - orderB;
    });

    // Ordenar submenús recursivamente
    return sorted.map((menu) => ({
      ...menu,
      subMenus:
        menu.subMenus && menu.subMenus.length > 0
          ? this.sortMenusByOrder(menu.subMenus)
          : menu.subMenus,
    }));
  }
}
