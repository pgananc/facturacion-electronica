import { Injectable } from '@angular/core';
import { CanActivate, Router, UrlTree } from '@angular/router';
import { Observable } from 'rxjs';
import { decodeToken } from '../_functions/functions';
import { SUPER_ADMIN_ROLE } from '../_constants/constants';
import { UserRoleService } from '../_service/auth/user-role.service';

/**
 * Guard para proteger rutas de administración global
 * Solo permite acceso a usuarios con rol super_admin
 */
@Injectable({
  providedIn: 'root',
})
export class AdminGuard implements CanActivate {
  constructor(
    private readonly router: Router,
    private readonly userRoleService: UserRoleService,
  ) {}

  canActivate():
    | Observable<boolean | UrlTree>
    | Promise<boolean | UrlTree>
    | boolean
    | UrlTree {
    // PRIORIDAD 1: Verificar si es super_admin globalmente (desde el token)
    // super_admin es un rol global del sistema, no depende de la compañía
    const isSuperAdminGlobal = this.userRoleService.isSuperAdminGlobal();

    if (isSuperAdminGlobal) {
      console.log(
        '✅ [AdminGuard] Acceso permitido para super_admin (rol global)',
      );
      return true;
    }

    // PRIORIDAD 2: Si no es super_admin global, verificar roles por compañía
    // (aunque super_admin no debería estar aquí, por si acaso)
    const roles = this.userRoleService.getCurrentRolesSync();
    const isSuperAdminByCompany = roles.includes(SUPER_ADMIN_ROLE.name);

    if (isSuperAdminByCompany) {
      console.log(
        '✅ [AdminGuard] Acceso permitido para super_admin (por compañía)',
      );
      return true;
    }

    // No tiene permisos de super_admin
    console.warn(
      '⚠️ [AdminGuard] Usuario sin permisos de super_admin intentó acceder',
    );
    this.router.navigate(['/welcome']);
    return false;
  }
}
