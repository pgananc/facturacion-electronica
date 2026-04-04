import { environment } from '../../../environments/environment';
import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { BehaviorSubject, Subject } from 'rxjs';
import { Menu } from '../../_model/auth/menu';

@Injectable({
  providedIn: 'root',
})
export class MenuService {
  menuChange = new BehaviorSubject<Menu[]>([]);
  isLoginChange = new Subject<boolean>();
  openSidenavTrigger = new Subject<void>(); // Nuevo: trigger para abrir el sidenav

  url: string = `${environment.HOST_AUTH}/api/menu`;
  constructor(private readonly http: HttpClient) {}

  findMenuByUser(userName: string) {
    return this.http.get<Menu[]>(`${this.url}/${userName}`);
  }

  hasAccessTo(url: string): boolean {
    const menus = this.menuChange.getValue();
    return this.recursiveMatch(url, menus);
  }

  private recursiveMatch(
    url: string,
    menus: Menu[],
    parentUrl?: string
  ): boolean {
    for (const menu of menus) {
      if (menu.url && menu.url !== 'null') {
        // Construir la URL completa igual que MenuItemComponent.getFullUrl()
        let fullMenuUrl = menu.url;

        // Si la URL del menú es relativa (no empieza con /)
        if (!menu.url.startsWith('/')) {
          // Si estamos en el contexto de reportes, agregar /reports/
          if (
            parentUrl === '/reports' ||
            (parentUrl && parentUrl.startsWith('/reports'))
          ) {
            fullMenuUrl = `/reports/${menu.url}`;
          } else if (parentUrl) {
            // Si hay un parentUrl, construir la ruta completa
            fullMenuUrl = `${parentUrl}/${menu.url}`;
          }
        }

        // Comparar la URL completa con la URL del menú (completa o parcial)
        if (url === fullMenuUrl || url.startsWith(fullMenuUrl + '/')) {
          return true;
        }

        // También comparar directamente si la URL del menú es absoluta
        if (
          menu.url.startsWith('/') &&
          (url === menu.url || url.startsWith(menu.url + '/'))
        ) {
          return true;
        }
      }

      // Determinar el parentUrl para los submenús
      let nextParentUrl = parentUrl;
      if (menu.url && menu.url !== 'null') {
        if (menu.url.startsWith('/')) {
          nextParentUrl = menu.url;
        } else if (
          parentUrl === '/reports' ||
          (parentUrl && parentUrl.startsWith('/reports'))
        ) {
          nextParentUrl = `/reports/${menu.url}`;
        } else if (parentUrl) {
          nextParentUrl = `${parentUrl}/${menu.url}`;
        } else if (menu.name === 'Reportes') {
          nextParentUrl = '/reports';
        }
      } else if (menu.name === 'Reportes') {
        nextParentUrl = '/reports';
      }

      if (menu.subMenus && menu.subMenus.length > 0) {
        const match = this.recursiveMatch(url, menu.subMenus, nextParentUrl);
        if (match) {
          return true;
        }
      }
    }
    return false;
  }
}
