import {
  Component,
  Input,
  OnInit,
  ChangeDetectorRef,
  Output,
  EventEmitter,
} from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { Menu } from '../../_model/auth/menu';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-menu-item',
  templateUrl: './menu-item.component.html',
  styleUrls: ['./menu-item.component.css'],
})
export class MenuItemComponent implements OnInit {
  @Input() menu!: Menu;
  @Input() level: number = 0;
  @Output() menuItemClick = new EventEmitter<void>();
  isExpanded: boolean = false;
  isRouteActive: boolean = false;

  constructor(private router: Router, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    // Verificar si alguna ruta del menú está activa
    this.checkRouteActive();

    // Escuchar cambios de ruta para actualizar el estado
    this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe(() => {
        this.checkRouteActive();
        this.cdr.detectChanges();
      });
  }

  /**
   * Verifica si alguna ruta del menú o sus submenús está activa
   */
  private checkRouteActive(): void {
    const currentUrl = this.router.url;
    this.isRouteActive = this.isMenuRouteActive(this.menu, currentUrl);

    // Si alguna ruta está activa, expandir el panel
    if (this.isRouteActive && this.menu.subMenus?.length) {
      this.isExpanded = true;
    }
  }

  /**
   * Verifica recursivamente si alguna ruta del menú está activa
   */
  private isMenuRouteActive(menu: Menu, currentUrl: string): boolean {
    // Verificar la URL del menú actual
    if (menu.url && menu.url !== 'null') {
      const fullUrl = this.getFullUrl(menu.url);
      if (fullUrl && currentUrl.startsWith(fullUrl)) {
        return true;
      }
    }

    // Verificar recursivamente en submenús
    if (menu.subMenus && menu.subMenus.length > 0) {
      return menu.subMenus.some((subMenu) =>
        this.isMenuRouteActive(subMenu, currentUrl)
      );
    }

    return false;
  }

  /**
   * Construye la URL completa
   * Si la URL es relativa y estamos en reportes, agregar /reports/
   */
  getFullUrl(url: string): string | null {
    if (!url || url === 'null') {
      return null;
    }

    // Si ya es una URL absoluta, retornarla
    if (url.startsWith('/')) {
      return url;
    }

    // Si es relativa, verificar el contexto del menú padre
    // Si el menú padre es "Reportes" o está dentro de reportes, agregar /reports/
    if (this.isInReportsContext()) {
      return `/reports/${url}`;
    }

    // Por defecto, retornar la URL relativa (Angular Router la manejará)
    return url;
  }

  /**
   * Verifica si estamos en el contexto de reportes
   * Revisa recursivamente si algún menú padre tiene nombre "Reportes"
   */
  private isInReportsContext(): boolean {
    // Si el nivel es 1 o mayor, probablemente estamos dentro de "Reportes"
    // También podemos verificar el nombre del menú si es necesario
    // Por ahora, asumimos que URLs relativas en niveles 2+ son de reportes
    return this.level >= 1;
  }

  /**
   * Maneja el click en un item de menú
   */
  onMenuItemClick(): void {
    // Si tiene submenús, no hacer nada (solo expandir/colapsar)
    if (this.menu.subMenus && this.menu.subMenus.length > 0) {
      return;
    }

    // Si no tiene URL válida, no hacer nada
    const url = this.getFullUrl(this.menu.url);
    if (!url) {
      return;
    }

    // Emitir evento para cerrar sidenav en móvil
    this.menuItemClick.emit();

    // Navegar a la ruta
    this.router.navigate([url]);
  }

  /**
   * Verifica si el menú tiene submenús
   */
  hasSubMenus(): boolean {
    return this.menu.subMenus && this.menu.subMenus.length > 0;
  }

  /**
   * Verifica si el menú es clickeable (tiene URL válida)
   */
  isClickable(): boolean {
    return !this.hasSubMenus() && !!this.menu.url && this.menu.url !== 'null';
  }

  /**
   * Rutas que comparten prefijo con otras (ej: /invoices vs /invoices/drafts)
   * necesitan exact match para no resaltar ambos ítems a la vez.
   */
  shouldUseExactMatch(): boolean {
    const url = this.getFullUrl(this.menu.url);
    if (!url) return false;
    const prefixRoutes = ['/invoices', '/invoices/create'];
    return prefixRoutes.includes(url);
  }

  /**
   * TrackBy function para mejorar el rendimiento del *ngFor
   */
  trackByMenuId(index: number, menu: Menu): number {
    return menu.id;
  }
}
