import { Component, OnInit } from '@angular/core';
import { PermissionAdminService } from '../../../../_service/permission/permission-admin.service';
import { PermissionService } from '../../../../_service/permission/permission.service';
import { SubscriptionAdminService } from '../../../../_service/subscription/subscription-admin.service';
import {
  SubscriptionPlan,
  FeaturePlanPermission,
  PlanPermissionMatrixRow,
} from '../../../../_model/permission/plan-permission-admin';
import { FeaturePermissionAdmin } from '../../../../_model/permission/permission-admin';
import { ToastService } from '../../../../_service/notification/toast.service';
import { MatTableDataSource } from '@angular/material/table';

@Component({
  selector: 'app-plan-permission-matrix',
  templateUrl: './plan-permission-matrix.component.html',
  styleUrls: ['./plan-permission-matrix.component.css'],
})
export class PlanPermissionMatrixComponent implements OnInit {
  plans: SubscriptionPlan[] = [];
  features: FeaturePlanPermission[] = [];
  matrixData: PlanPermissionMatrixRow[] = [];
  dataSource = new MatTableDataSource<PlanPermissionMatrixRow>([]);
  displayedColumns: string[] = [];

  isLoading = false;
  isSaving = false;
  hasChanges = false;

  // Control de permisos: solo super_admin puede editar
  isSuperAdmin = false;
  canEdit = false; // Solo super_admin puede editar

  // Filtros
  selectedModule = '';
  modules: string[] = [];

  // Map para rastrear cambios
  private changes = new Map<string, string[]>();

  constructor(
    private readonly permissionService: PermissionAdminService,
    private readonly permissionCacheService: PermissionService,
    private readonly subscriptionService: SubscriptionAdminService,
    private readonly toast: ToastService
  ) {
    this.dataSource = new MatTableDataSource<PlanPermissionMatrixRow>([]);
  }

  ngOnInit(): void {
    // Verificar si el usuario es super_admin (solo super_admin puede editar)
    this.isSuperAdmin = this.permissionCacheService.isSuperAdmin();
    this.canEdit = this.isSuperAdmin;

    // Cualquier rol con permisos puede ver (GETs), pero solo super_admin puede editar
    this.loadData();
  }

  loadData(): void {
    const token = localStorage.getItem('token');
    if (!token) {
      console.warn(
        '[PlanPermissionMatrix] No hay token, omitiendo carga de datos'
      );
      this.isLoading = false;
      return;
    }

    this.isLoading = true;

    // Usar getAllFeatures() igual que la matriz de roles (features dinámicas desde DB)
    Promise.all([
      this.subscriptionService.getAllPlans().toPromise(),
      this.permissionService.getAllFeatures().toPromise(),
    ])
      .then(([plans, features]) => {
        this.plans = plans || [];

        if (!features || features.length === 0) {
          this.toast.error(
            'Error: No se pudieron obtener los datos de permisos'
          );
          this.isLoading = false;
          return;
        }

        // Convertir FeaturePermissionAdmin[] a FeaturePlanPermission[]
        // Usando requiredPlans directamente de la DB
        this.features = features.map((f: FeaturePermissionAdmin) => ({
          featureCode: f.featureCode,
          name: f.name,
          module: f.module,
          allowedPlans: f.requiredPlans || [], // requiredPlans viene de la DB
          description: f.description,
        }));

        // Configurar columnas de la tabla
        this.displayedColumns = [
          'feature',
          'module',
          ...this.plans.map((p) => `plan_${p.code}`),
        ];

        // Extraer módulos únicos
        this.modules = [
          ...new Set(this.features?.map((f) => f.module) || []),
        ].sort();

        // Construir matriz
        this.buildMatrix();

        this.isLoading = false;
      })
      .catch((error) => {
        console.error('Error cargando datos:', error);
        this.toast.error('Error al cargar permisos de planes');
        this.isLoading = false;
      });
  }

  buildMatrix(): void {
    // Filtrar features que no tengan featureCode válido
    // El 'name' ya fue normalizado en loadData(), así que siempre existe
    const validFeatures = this.features.filter(
      (f) =>
        f &&
        f.featureCode &&
        typeof f.featureCode === 'string' &&
        f.featureCode.trim() !== ''
    );

    // Agrupar por módulo y ordenar alfabéticamente
    const groupedByModule = new Map<string, typeof this.features>();

    validFeatures.forEach((feature) => {
      const module = feature.module || 'SIN_MODULO';
      if (!groupedByModule.has(module)) {
        groupedByModule.set(module, []);
      }
      groupedByModule.get(module)!.push(feature);
    });

    // Ordenar features dentro de cada módulo alfabéticamente
    groupedByModule.forEach((features, module) => {
      features.sort((a, b) => {
        const nameA = a.name || '';
        const nameB = b.name || '';
        return nameA.localeCompare(nameB);
      });
    });

    // Ordenar módulos alfabéticamente
    const sortedModules = Array.from(groupedByModule.keys()).sort();

    // Construir matriz agrupada y ordenada
    this.matrixData = [];
    sortedModules.forEach((module) => {
      const moduleFeatures = groupedByModule.get(module)!;
      moduleFeatures.forEach((feature) => {
        const permissions: { [planCode: string]: boolean } = {};

        this.plans.forEach((plan) => {
          permissions[plan.code] = feature.allowedPlans.includes(plan.code);
        });

        this.matrixData.push({
          featureCode: feature.featureCode,
          name: feature.name,
          module: feature.module,
          description: feature.description,
          permissions,
        });
      });
    });

    this.applyFilter();
  }

  applyFilter(): void {
    if (this.selectedModule) {
      this.dataSource.data = this.matrixData.filter(
        (row) => row.module === this.selectedModule
      );
    } else {
      this.dataSource.data = this.matrixData;
    }
  }

  onFilterChange(): void {
    this.applyFilter();
  }

  togglePermission(row: PlanPermissionMatrixRow, planCode: string): void {
    // Solo super_admin puede editar
    if (!this.canEdit) {
      this.toast.warning(
        'Solo los super administradores pueden modificar permisos de planes'
      );
      return;
    }

    // Toggle el estado
    row.permissions[planCode] = !row.permissions[planCode];

    // Calcular nuevos allowedPlans
    const allowedPlans = this.plans
      .filter((plan) => row.permissions[plan.code])
      .map((plan) => plan.code);

    // Guardar cambio
    this.changes.set(row.featureCode, allowedPlans);
    this.hasChanges = true;
  }

  saveChanges(): void {
    // Validar que solo super_admin puede guardar cambios
    if (!this.canEdit) {
      this.toast.warning(
        'Solo los super administradores pueden guardar cambios'
      );
      return;
    }

    if (!this.hasChanges || this.isSaving) {
      return;
    }

    this.isSaving = true;

    // Actualizar cada feature modificada usando el endpoint estándar
    const updatePromises = Array.from(this.changes.entries()).map(
      ([featureCode, allowedPlans]) => {
        return this.permissionService
          .updateFeaturePlans(featureCode, allowedPlans)
          .toPromise()
          .catch((error) => {
            console.error(`Error actualizando feature ${featureCode}:`, error);
            return { featureCode, error: error.message || 'Error desconocido' };
          });
      }
    );

    Promise.all(updatePromises)
      .then((results) => {
        const errors = results.filter((r) => r && (r as any).error);
        const successCount = results.length - errors.length;

        if (errors.length === 0) {
          this.toast.success(
            `Se actualizaron exitosamente ${successCount} funcionalidad(es)`
          );
        } else {
          this.toast.warning(
            `Se actualizaron ${successCount} funcionalidad(es), ${errors.length} fallaron`
          );
          errors.forEach((error: any) => {
            this.toast.error(`Feature ${error.featureCode}: ${error.error}`);
          });
        }

        this.changes.clear();
        this.hasChanges = false;
        this.isSaving = false;

        // Invalidar cache de validaciones para todas las features afectadas
        this.permissionCacheService.invalidateFeatureCache();
        this.permissionCacheService.invalidateFeaturesCache();

        // Recargar datos
        this.loadData();
      })
      .catch((error) => {
        console.error('Error guardando cambios:', error);
        const errorMessage =
          error.error?.message ||
          error.message ||
          'Error al guardar los cambios';
        this.toast.error(errorMessage);
        this.isSaving = false;
      });
  }

  discardChanges(): void {
    if (
      confirm('¿Está seguro que desea descartar todos los cambios sin guardar?')
    ) {
      this.changes.clear();
      this.hasChanges = false;
      this.loadData();
    }
  }

  getModuleIcon(module: string): string {
    const icons: { [key: string]: string } = {
      INVOICING: 'receipt',
      CREDIT_NOTES: 'receipt_long',
      CLIENTS: 'people',
      REPORTS: 'bar_chart',
      ADMINISTRATION: 'settings',
      API: 'api',
      PRODUCTS: 'folder',
    };
    return icons[module] || 'folder';
  }

  /**
   * Obtiene nombre amigable de un módulo (para display)
   */
  getModuleDisplayName(module: string): string {
    const displayNames: { [key: string]: string } = {
      INVOICING: 'Facturación',
      CREDIT_NOTES: 'Notas de Crédito',
      CLIENTS: 'Clientes',
      REPORTS: 'Reportes',
      ADMINISTRATION: 'Administración',
      API: 'API',
      PRODUCTS: 'Productos',
    };
    return displayNames[module] || module;
  }

  getModuleColor(module: string): string {
    const colors: { [key: string]: string } = {
      INVOICING: '#2196f3',
      CREDIT_NOTES: '#9c27b0',
      CLIENTS: '#4caf50',
      REPORTS: '#ff9800',
      ADMINISTRATION: '#f44336',
      API: '#607d8b',
      PRODUCTS: '#757575',
    };
    return colors[module] || '#757575';
  }

  /**
   * Obtiene nombre amigable de un plan (para display)
   */
  getPlanDisplayName(planCode: string): string {
    const plan = this.plans.find((p) => p.code === planCode);
    return plan ? plan.name : planCode;
  }

  /**
   * Obtiene descripción de un plan
   */
  getPlanDescription(planCode: string): string {
    const plan = this.plans.find((p) => p.code === planCode);
    return plan ? plan.description : '';
  }

  /**
   * Obtiene clase CSS para badge de plan
   */
  getPlanBadgeClass(planCode: string): string {
    const classes: { [code: string]: string } = {
      FREE: 'plan-free',
      BASIC: 'plan-basic',
      PRO: 'plan-professional', // Clase CSS sigue siendo 'plan-professional' para estilos
      ENTERPRISE: 'plan-enterprise',
    };
    return classes[planCode] || 'plan-default';
  }
}
