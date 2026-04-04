import { environment } from '../../../environments/environment';
import { Menu } from '../../_model/auth/menu';
import { map, Observable, of } from 'rxjs';
import { decodeToken } from '../../_functions/functions';
import {
  ActivatedRouteSnapshot,
  CanActivate,
  Router,
  RouterStateSnapshot,
  UrlTree,
} from '@angular/router';
import { CompanyService } from '../customer/company.service';
import { MenuService } from '../auth/menu.service';
import { LoginService } from '../auth/login.service';
import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class CanActiveUserGuard implements CanActivate {
  constructor(
    private readonly loginService: LoginService,
    private readonly menuService: MenuService,
    private readonly companyService: CompanyService,
    private readonly router: Router
  ) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean | UrlTree> {
    const isLogin = this.loginService.isLogin();
    if (!isLogin) {
      this.loginService.closeSession();
      return of(this.router.parseUrl('/login'));
    }

    const token = decodeToken();
    if (!token) {
      console.warn('Token decodificado es null, cerrando sesión');
      this.loginService.closeSession();
      return of(this.router.parseUrl('/login'));
    }

    const userName = token.userName;
    const url = state.url;

    // Si no requiere validación de menú (ej. perfil o cambio de contraseña)
    if (this.validateWithoutMenu(url)) {
      return this.menuService.findMenuByUser(userName).pipe(
        map((menus: Menu[]) => {
          this.menuService.menuChange.next(menus);
          this.companyService.companyNameChange.next(
            localStorage.getItem(environment.NAME_COMPANY) || ''
          );
          return true;
        })
      );
    }

    // Si requiere validación de menú
    return this.menuService.findMenuByUser(userName).pipe(
      map((menus: Menu[]) => {
        this.menuService.menuChange.next(menus);
        
        // ✅ Si la ruta tiene PermissionGuard, permitir acceso y dejar que PermissionGuard valide
        // Esto evita que CanActiveUserGuard bloquee rutas protegidas por PermissionGuard
        // Verificar si la ruta tiene PermissionGuard en su configuración
        const hasPermissionGuard = this.routeHasPermissionGuard(route);
        
        if (url.startsWith('/setting') || hasPermissionGuard) {
          this.companyService.companyNameChange.next(
            localStorage.getItem(environment.NAME_COMPANY) || ''
          );
          return true; // Permitir acceso, PermissionGuard validará los permisos
        }
        
        const hasAccess = this.menuService.hasAccessTo(url);

        if (hasAccess) {
          this.companyService.companyNameChange.next(
            localStorage.getItem(environment.NAME_COMPANY) || ''
          );
          return true;
        } else {
          return this.router.parseUrl('/not-403');
        }
      })
    );
  }

  private validateWithoutMenu(url: string): boolean {
    return (
      url.startsWith('/profile') ||
      url.startsWith('/change-password')
      // ✅ /setting ahora requiere validación de menú (PermissionGuard)
    );
  }

  /**
   * Verifica si la ruta tiene PermissionGuard en su configuración
   * Esto permite que CanActiveUserGuard no bloquee rutas protegidas por PermissionGuard
   */
  private routeHasPermissionGuard(route: ActivatedRouteSnapshot): boolean {
    // Verificar en la configuración de la ruta actual
    if (route.routeConfig?.canActivate) {
      const guards = route.routeConfig.canActivate;
      const guardNames = guards.map((guard) => {
        if (typeof guard === 'function') {
          return guard.name;
        }
        return guard?.constructor?.name || '';
      });
      return guardNames.some((name) => name.includes('PermissionGuard'));
    }

    // Verificar en rutas padre (para rutas lazy-loaded)
    let parent = route.parent;
    while (parent) {
      if (parent.routeConfig?.canActivate) {
        const guards = parent.routeConfig.canActivate;
        const guardNames = guards.map((guard) => {
          if (typeof guard === 'function') {
            return guard.name;
          }
          return guard?.constructor?.name || '';
        });
        if (guardNames.some((name) => name.includes('PermissionGuard'))) {
          return true;
        }
      }
      parent = parent.parent;
    }

    return false;
  }
}
