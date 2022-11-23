import { Injectable } from '@angular/core';
import {
  ActivatedRouteSnapshot,
  CanActivate,
  RouterStateSnapshot,
  UrlTree,
} from '@angular/router';
import { Observable, map } from 'rxjs';
import { LoginService } from '../auth/login.service';
import { MenuService } from '../auth/menu.service';
import { Router } from '@angular/router';
import { environment } from 'src/environments/environment';
import { Menu } from '../../_model/auth/menu';

@Injectable({
  providedIn: 'root',
})
export class CanActiveUserGuard implements CanActivate {
  constructor(
    private loginService: LoginService,
    private menuService: MenuService,
    private router: Router
  ) {}
  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ):
    | Observable<boolean | UrlTree>
    | Promise<boolean | UrlTree>
    | boolean
    | UrlTree {
    let rpta = this.loginService.isLogin();
    console.log(rpta);
    if (!rpta) {
      //SI ESTA LOGUEADO
      this.loginService.closeSession();
      return false;
    } else {
      //Obtener usuario en storage
      let userName = sessionStorage.getItem(environment.USER);
      if (userName != null && userName != '') {
        //SI TIENES EL ROL NECESARIO

        let url = state.url;
        //  const decodedToken = helper.decodeToken(token);

        return this.menuService.findMenuByUser(userName).pipe(
          map((data: Menu[]) => {
            this.menuService.menuChange.next(data);

            let cont = 0;
            for (let m of data) {
              if (url.startsWith(m.url)) {
                cont++;
                break;
              }
            }

            if (cont > 0) {
              return true;
            } else {
              this.router.navigate(['not-403']);
              return false;
            }
          })
        );
      } else {
        this.loginService.closeSession();
        return false;
      }
    }
  }
}
