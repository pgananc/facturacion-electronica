import { Injectable } from '@angular/core';
import { Observable, BehaviorSubject, of } from 'rxjs';
import { map, catchError, tap } from 'rxjs/operators';
import { CompanyUserService } from './company-user.service';
import { environment } from '../../../environments/environment';
import { decodeToken } from '../../_functions/functions';
import { LoggerService } from '../logger/logger.service';

/**
 * UserRoleService - Servicio centralizado para obtener roles del usuario por compañía
 *
 * Este servicio centraliza la lógica de obtener los roles del usuario en el contexto
 * de una compañía específica, evitando usar los roles del token que contienen roles
 * de todas las compañías.
 *
 * Uso:
 * ```typescript
 * constructor(private userRoleService: UserRoleService) {}
 *
 * // Obtener roles de la compañía actual
 * this.userRoleService.getCurrentCompanyRoles().subscribe(roles => {
 *   console.log('Roles:', roles);
 * });
 *
 * // Obtener roles de una compañía específica
 * this.userRoleService.getRolesByCompany(idCompany).subscribe(roles => {
 *   console.log('Roles:', roles);
 * });
 * ```
 */
@Injectable({
  providedIn: 'root',
})
export class UserRoleService {
  private rolesCache: Map<number, string[]> = new Map();
  private currentRoles$ = new BehaviorSubject<string[]>([]);

  constructor(
    private companyUserService: CompanyUserService,
    private logger: LoggerService,
  ) {}

  /**
   * Obtiene los roles del usuario en la compañía actual (desde localStorage)
   * Usa caché para evitar llamadas repetidas al backend
   */
  getCurrentCompanyRoles(): Observable<string[]> {
    const idCompany = Number(localStorage.getItem(environment.ID_COMPANY));
    if (!idCompany || idCompany <= 0 || isNaN(idCompany)) {
      this.logger.warn(
        '[UserRoleService] No hay idCompany válido en localStorage, retornando array vacío',
      );
      this.currentRoles$.next([]);
      return of([]);
    }

    return this.getRolesByCompany(idCompany);
  }

  /**
   * Obtiene los roles del usuario en una compañía específica
   * @param idCompany - ID de la compañía
   * @param useCache - Si es true, usa caché si está disponible (default: true)
   */
  getRolesByCompany(
    idCompany: number,
    useCache: boolean = true,
  ): Observable<string[]> {
    if (!idCompany || idCompany <= 0 || isNaN(idCompany)) {
      this.logger.warn(
        '[UserRoleService] idCompany inválido, retornando array vacío',
      );
      this.currentRoles$.next([]);
      return of([]);
    }

    // Verificar caché
    if (useCache && this.rolesCache.has(idCompany)) {
      const cachedRoles = this.rolesCache.get(idCompany)!;
      this.logger.debug(
        `[UserRoleService] Roles obtenidos desde caché para compañía ${idCompany}:`,
        cachedRoles,
      );
      this.currentRoles$.next(cachedRoles);
      return of(cachedRoles);
    }

    // Obtener idUser del token
    const token = decodeToken();

    // Intentar obtener idUser del token (prioridad: 'sub' estándar JWT, luego campos personalizados)
    const idUserFromToken =
      (token as any)?.sub || // ✅ Estándar JWT (prioridad)
      (token as any)?.idUser ||
      (token as any)?.userId ||
      null;

    // El 'sub' puede venir como string o number, convertir a number si es necesario
    let idUser: number | null = null;
    if (idUserFromToken) {
      if (typeof idUserFromToken === 'number') {
        idUser = idUserFromToken;
      } else if (typeof idUserFromToken === 'string') {
        idUser = Number(idUserFromToken);
        if (isNaN(idUser)) {
          idUser = null;
        }
      }
    }

    if (!token || !idUser || idUser <= 0) {
      console.error(
        '❌ [UserRoleService] ⚠️ PROBLEMA: No se puede obtener idUser válido del token',
      );
      console.error('❌ [UserRoleService] Token:', token);
      console.error('❌ [UserRoleService] idUserFromToken:', idUserFromToken);
      console.error('❌ [UserRoleService] idUser (convertido):', idUser);
      console.error(
        '❌ [UserRoleService] Retornando array vacío - NO se llamará al backend',
      );
      this.logger.warn(
        '[UserRoleService] No se puede obtener idUser del token, retornando array vacío',
      );
      this.currentRoles$.next([]);
      return of([]);
    }

    // Obtener roles del backend
    return this.companyUserService
      .findByIdCompanyAndIdUser(idCompany, idUser)
      .pipe(
        map((response) => {
          let roles: string[] = [];

          if (response?.userDtos && response.userDtos.length > 0) {
            const user = response.userDtos[0];

            if (user.roleDtos && user.roleDtos.length > 0) {
              roles = user.roleDtos
                .map((role) => role.name || '')
                .filter((name) => name !== '');
            }
          }

          // Guardar en caché
          this.rolesCache.set(idCompany, roles);
          this.currentRoles$.next(roles);

          this.logger.debug(
            `[UserRoleService] Roles cargados para compañía ${idCompany}:`,
            roles,
          );

          return roles;
        }),
        catchError((error) => {
          this.logger.error(
            `[UserRoleService] Error al cargar roles para compañía ${idCompany}:`,
            error,
          );
          // Retornar array vacío en lugar de usar roles del token
          this.currentRoles$.next([]);
          return of([]);
        }),
      );
  }

  /**
   * Obtiene los roles del token (fallback cuando no hay compañía seleccionada)
   * ⚠️ ADVERTENCIA: Estos roles incluyen roles de TODAS las compañías
   * ⚠️ IMPORTANTE: Este método solo se debe usar como último recurso
   * En la mayoría de los casos, es mejor retornar un array vacío para evitar mostrar roles incorrectos
   */
  private getRolesFromToken(): Observable<string[]> {
    const token = decodeToken();
    if (!token || !token.roles) {
      this.logger.warn('[UserRoleService] No hay roles en el token');
      this.currentRoles$.next([]);
      return of([]);
    }

    // ⚠️ NO usar roles del token como fallback porque incluyen roles de todas las compañías
    // Es mejor retornar un array vacío y que el componente maneje la ausencia de roles
    this.logger.warn(
      '[UserRoleService] No se puede obtener roles por compañía. Retornando array vacío para evitar mostrar roles incorrectos.',
    );

    this.currentRoles$.next([]);
    return of([]);
  }

  /**
   * Limpia el caché de roles (útil cuando se cambia de compañía)
   * También limpia el BehaviorSubject para que los componentes se actualicen
   * @param idCompany - ID de la compañía a limpiar (opcional, si no se proporciona limpia todo)
   */
  clearCache(idCompany?: number): void {
    if (idCompany) {
      this.rolesCache.delete(idCompany);
      this.logger.debug(
        `[UserRoleService] Caché limpiado para compañía ${idCompany}`,
      );
      // Limpiar currentRoles$ solo si es la compañía actual
      const currentIdCompany = Number(
        localStorage.getItem(environment.ID_COMPANY),
      );
      if (currentIdCompany === idCompany) {
        this.currentRoles$.next([]);
        this.logger.debug(
          `[UserRoleService] currentRoles$ limpiado para compañía ${idCompany}`,
        );
      }
    } else {
      this.rolesCache.clear();
      this.currentRoles$.next([]); // Limpiar el BehaviorSubject
      this.logger.debug(
        '[UserRoleService] Caché y currentRoles$ limpiados completamente',
      );
    }
  }

  /**
   * Observable de los roles actuales (se actualiza automáticamente)
   */
  get currentRoles(): Observable<string[]> {
    return this.currentRoles$.asObservable();
  }

  /**
   * Obtiene los roles actuales de forma síncrona (desde el BehaviorSubject)
   * ⚠️ Puede retornar roles vacíos si aún no se han cargado
   */
  getCurrentRolesSync(): string[] {
    return this.currentRoles$.getValue();
  }

  /**
   * Verifica si el usuario tiene un rol específico en la compañía actual
   * @param roleName - Nombre del rol a verificar (ej: 'super_admin', 'admin', 'member')
   */
  hasRole(roleName: string): Observable<boolean> {
    return this.getCurrentCompanyRoles().pipe(
      map((roles) => roles.includes(roleName)),
    );
  }

  /**
   * Verifica si el usuario tiene un rol específico de forma síncrona
   * ⚠️ Puede retornar false si los roles aún no se han cargado
   */
  hasRoleSync(roleName: string): boolean {
    return this.getCurrentRolesSync().includes(roleName);
  }

  /**
   * Verifica si el usuario es super_admin globalmente (desde el token)
   * super_admin es un rol global del sistema, no depende de la compañía
   * @returns true si el usuario tiene rol super_admin en el token
   */
  isSuperAdminGlobal(): boolean {
    const token = decodeToken();
    if (!token || !token.roles) {
      return false;
    }
    return token.roles.includes('super_admin');
  }
}
