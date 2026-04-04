import {
  Component,
  OnInit,
  OnDestroy,
  ViewChild,
  AfterViewInit,
} from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatTableDataSource } from '@angular/material/table';
import { MatSort } from '@angular/material/sort';
import { Subject } from 'rxjs';
import { takeUntil, finalize } from 'rxjs/operators';
import { MenuAdminService } from '../../../../_service/admin/menu-admin.service';
import { MenuAdmin } from '../../../../_model/admin/menu-admin';
import { MenuFormDialogComponent } from '../menu-form-dialog/menu-form-dialog.component';
import { ToastService } from '../../../../_service/notification/toast.service';
import { LoggerService } from '../../../../_service/logger/logger.service';
import { ConfirmDialogComponent } from '../../../../_shared/confirm-dialog/confirm-dialog.component';
import { extractUserFriendlyErrorMessage } from '../../../../_utils/error-message.util';

/**
 * Componente para listar y gestionar menús del sistema
 * Solo accesible para super_admin
 */
@Component({
  selector: 'app-menu-list',
  templateUrl: './menu-list.component.html',
  styleUrls: ['./menu-list.component.css'],
})
export class MenuListComponent implements OnInit, OnDestroy, AfterViewInit {
  menus: MenuAdmin[] = [];
  dataSource = new MatTableDataSource<MenuAdmin & { level?: number }>([]);
  isLoading = false;
  displayedColumns: string[] = [
    'name',
    'icon',
    'url',
    'featureCode',
    'active',
    'actions',
  ];

  @ViewChild(MatSort, { static: false }) sort!: MatSort;

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly menuService: MenuAdminService,
    private readonly dialog: MatDialog,
    private readonly toast: ToastService,
    private readonly logger: LoggerService
  ) {}

  ngOnInit(): void {
    this.loadMenus();
  }

  ngAfterViewInit(): void {
    // Conectar el sort al dataSource después de que la vista esté inicializada
    if (this.sort) {
      this.dataSource.sort = this.sort;
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Carga todos los menús desde el backend
   */
  loadMenus(): void {
    this.isLoading = true;
    this.menuService
      .getAllMenus()
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => (this.isLoading = false))
      )
      .subscribe({
        next: (menus) => {
          this.menus = menus;
          this.updateDataSource();
          this.logger.debug(
            '✅ Menús cargados:',
            menus.length,
            'menús principales'
          );
        },
        error: (error) => {
          this.logger.error('❌ Error al cargar menús:', error);
          this.toast.httpError(error, 'cargar los menús');
        },
      });
  }

  /**
   * Abre el diálogo para crear un nuevo menú
   */
  openCreateDialog(parentMenu?: MenuAdmin): void {
    const dialogRef = this.dialog.open(MenuFormDialogComponent, {
      width: '600px',
      maxWidth: '90vw',
      data: {
        menu: null,
        parentMenu: parentMenu || null,
        isEdit: false,
      },
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.toast.success('Menú creado exitosamente');
        this.loadMenus();
      }
    });
  }

  /**
   * Abre el diálogo para editar un menú existente
   */
  openEditDialog(menu: MenuAdmin): void {
    const dialogRef = this.dialog.open(MenuFormDialogComponent, {
      width: '600px',
      maxWidth: '90vw',
      data: {
        menu: menu,
        parentMenu: null,
        isEdit: true,
      },
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.toast.success('Menú actualizado exitosamente');
        this.loadMenus();
      }
    });
  }

  /**
   * Elimina un menú físicamente (hard delete) después de confirmación
   * NOTA: Para desactivar un menú (soft delete), usar el toggle activo/inactivo
   */
  deleteMenu(menu: MenuAdmin): void {
    // Validar que no tenga submenús activos
    if (menu.subMenus && menu.subMenus.length > 0) {
      const activeSubMenus = menu.subMenus.filter((sub) => sub.active);
      if (activeSubMenus.length > 0) {
        this.toast.warning(
          'No se puede eliminar un menú que tiene submenús activos. Elimine primero los submenús o desactívelos.'
        );
        return;
      }
    }

    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '400px',
      data: {
        title: 'Eliminar Menú Permanentemente',
        message: `¿Está seguro de eliminar permanentemente el menú "${menu.name}"? Esta acción no se puede deshacer. Si solo quiere ocultarlo, use el toggle activo/inactivo.`,
        confirmText: 'Eliminar Permanentemente',
        cancelText: 'Cancelar',
      },
    });

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.isLoading = true;
        this.menuService
          .deleteMenu(menu.id)
          .pipe(
            takeUntil(this.destroy$),
            finalize(() => (this.isLoading = false))
          )
          .subscribe({
            next: () => {
              this.toast.success('Menú eliminado permanentemente');
              this.loadMenus();
            },
            error: (error) => {
              this.logger.error('❌ Error al eliminar menú:', error);
              const friendlyMessage = extractUserFriendlyErrorMessage(
                error,
                'eliminar el menú'
              );
              this.toast.error(friendlyMessage);
            },
          });
      }
    });
  }

  /**
   * Alterna el estado activo/inactivo de un menú
   */
  toggleActive(menu: MenuAdmin & { level?: number }): void {
    const newActiveState = !menu.active;
    this.isLoading = true;

    this.menuService
      .updateMenu(menu.id, { active: newActiveState })
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => (this.isLoading = false))
      )
      .subscribe({
        next: () => {
          // Actualizar el estado en el árbol de menús
          this.updateMenuInTree(menu.id, { active: newActiveState });
          // Refrescar el dataSource
          this.updateDataSource();
          this.toast.success(
            `Menú ${newActiveState ? 'activado' : 'desactivado'} exitosamente`
          );
        },
        error: (error) => {
          this.logger.error('❌ Error al actualizar estado del menú:', error);
          this.toast.httpError(error, 'actualizar el estado del menú');
        },
      });
  }

  /**
   * Actualiza un menú en el árbol recursivamente
   */
  private updateMenuInTree(menuId: number, updates: Partial<MenuAdmin>): void {
    const updateRecursive = (menus: MenuAdmin[]): boolean => {
      for (const menu of menus) {
        if (menu.id === menuId) {
          Object.assign(menu, updates);
          return true;
        }
        if (menu.subMenus && menu.subMenus.length > 0) {
          if (updateRecursive(menu.subMenus)) {
            return true;
          }
        }
      }
      return false;
    };
    updateRecursive(this.menus);
  }

  /**
   * Obtiene el nombre del menú padre
   */
  getParentName(parentId: number | null | undefined): string {
    if (!parentId) return '-';
    const parent = this.findMenuById(this.menus, parentId);
    return parent ? parent.name : '-';
  }

  /**
   * Busca un menú por ID recursivamente en el árbol
   */
  private findMenuById(menus: MenuAdmin[], id: number): MenuAdmin | null {
    for (const menu of menus) {
      if (menu.id === id) return menu;
      if (menu.subMenus && menu.subMenus.length > 0) {
        const found = this.findMenuById(menu.subMenus, id);
        if (found) return found;
      }
    }
    return null;
  }

  /**
   * Aplana el árbol de menús para mostrar en tabla
   * Valida y evita duplicados como medida de seguridad defensiva
   */
  getFlattenedMenus(): (MenuAdmin & { level?: number })[] {
    const flattened: (MenuAdmin & { level?: number })[] = [];
    const processedIds = new Set<number>();
    const duplicatesFound: Array<{ id: number; name: string }> = [];

    const flatten = (menus: MenuAdmin[], level: number = 0) => {
      menus.forEach((menu) => {
        // Validación defensiva: evitar duplicados si el backend los envía
        if (!processedIds.has(menu.id)) {
          processedIds.add(menu.id);
          flattened.push({ ...menu, level } as MenuAdmin & { level?: number });

          // Procesar submenús si existen
          if (menu.subMenus && menu.subMenus.length > 0) {
            flatten(menu.subMenus, level + 1);
          }
        } else {
          // Solo registrar duplicados si realmente existen (no generar warnings innecesarios)
          if (!duplicatesFound.find((d) => d.id === menu.id)) {
            duplicatesFound.push({ id: menu.id, name: menu.name });
          }
        }
      });
    };

    flatten(this.menus);

    // Solo mostrar warning si realmente hay duplicados
    if (duplicatesFound.length > 0) {
      this.logger.warn(
        '⚠️ Duplicados detectados en menús (backend debe corregir):',
        duplicatesFound
      );
    }

    return flattened;
  }

  /**
   * Actualiza el dataSource con los menús aplanados
   */
  private updateDataSource(): void {
    const flattenedMenus = this.getFlattenedMenus();
    this.dataSource.data = flattenedMenus;

    // Asegurar que el sort esté conectado después de actualizar los datos
    if (this.sort && !this.dataSource.sort) {
      this.dataSource.sort = this.sort;
    }
  }
}
