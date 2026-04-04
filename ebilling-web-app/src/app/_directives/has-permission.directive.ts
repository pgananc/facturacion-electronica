import {
  Directive,
  Input,
  TemplateRef,
  ViewContainerRef,
  OnInit,
  OnDestroy,
} from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { PermissionService } from '../_service/permission/permission.service';

/**
 * HasPermissionDirective
 *
 * Directiva estructural para mostrar/ocultar elementos según permisos
 *
 * Uso:
 * ```html
 * <div *appHasPermission="'INVOICE_CREATE'">
 *   Contenido visible solo si tiene permiso
 * </div>
 *
 * <button *appHasPermission="'REPORT_ADVANCED'; else upgradeTpl">
 *   Ver Reportes Avanzados
 * </button>
 * <ng-template #upgradeTpl>
 *   <button class="upgrade-btn">🔒 Actualizar Plan</button>
 * </ng-template>
 * ```
 */
@Directive({
  selector: '[appHasPermission]',
})
export class HasPermissionDirective implements OnInit, OnDestroy {
  @Input() appHasPermission!: string; // Feature code
  @Input() appHasPermissionElse?: TemplateRef<any>; // Fallback template

  private destroy$ = new Subject<void>();

  constructor(
    private readonly templateRef: TemplateRef<any>,
    private readonly viewContainer: ViewContainerRef,
    private readonly permissionService: PermissionService
  ) {}

  ngOnInit(): void {
    // super_admin siempre tiene acceso
    if (this.permissionService.isSuperAdmin()) {
      this.viewContainer.createEmbeddedView(this.templateRef);
      return;
    }

    // Verificar permiso
    this.permissionService
      .canAccess(this.appHasPermission)
      .pipe(takeUntil(this.destroy$))
      .subscribe((allowed) => {
        this.viewContainer.clear();

        if (allowed) {
          this.viewContainer.createEmbeddedView(this.templateRef);
        } else if (this.appHasPermissionElse) {
          this.viewContainer.createEmbeddedView(this.appHasPermissionElse);
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
