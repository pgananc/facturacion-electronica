import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { PermissionAdminService } from '../../../../_service/permission/permission-admin.service';
import { PermissionService } from '../../../../_service/permission/permission.service';
import {
  FeaturePermissionAdmin,
  Role,
  PermissionMatrixRow,
  BulkPermissionUpdateRequest,
  RolePermissionUpdate,
} from '../../../../_model/permission/permission-admin';
import { ToastService } from '../../../../_service/notification/toast.service';
import { MatTableDataSource } from '@angular/material/table';

@Component({
  selector: 'app-permission-matrix',
  templateUrl: './permission-matrix.component.html',
  styleUrls: ['./permission-matrix.component.css'],
})
export class PermissionMatrixComponent implements OnInit {
  roles: Role[] = [];
  features: FeaturePermissionAdmin[] = [];
  matrixData: PermissionMatrixRow[] = [];
  dataSource = new MatTableDataSource<PermissionMatrixRow>([]);
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
  private changes = new Map<string, number[]>();

  constructor(
    private readonly permissionService: PermissionAdminService,
    private readonly permissionCacheService: PermissionService,
    private readonly toast: ToastService
  ) {
    this.dataSource = new MatTableDataSource<PermissionMatrixRow>([]);
  }

  ngOnInit(): void {
    // Verificar si el usuario es super_admin (solo super_admin puede editar)
    this.isSuperAdmin = this.permissionCacheService.isSuperAdmin();
    this.canEdit = this.isSuperAdmin;
    
    // Cualquier rol con permisos puede ver (GETs), pero solo super_admin puede editar
    this.loadData();
  }

  loadData(): void {
    // 🔧 Verificar si hay token antes de cargar datos
    const token = localStorage.getItem('token');
    if (!token) {
      console.warn('[PermissionMatrix] No hay token, omitiendo carga de datos');
      this.isLoading = false;
      return;
    }

    this.isLoading = true;

    Promise.all([
      this.permissionService.getAllRoles().toPromise(),
      this.permissionService.getAllFeatures().toPromise(),
    ])
      .then(([roles, features]) => {
        this.roles = roles || [];
        this.features = features || [];

        // Configurar columnas de la tabla (sin planes - ahora está separado)
        this.displayedColumns = [
          'feature',
          'module',
          ...this.roles.map((r) => `role_${r.id}`),
        ];

        // Extraer módulos únicos (filtrar null/undefined)
        this.modules = [
          ...new Set(
            features
              ?.map((f) => f.module)
              .filter((m) => m && m.trim() !== '') || []
          ),
        ].sort();

        // Construir matriz
        this.buildMatrix();

        this.isLoading = false;
      })
      .catch((error) => {
        console.error('Error cargando datos:', error);
        this.toast.error('Error al cargar permisos y roles');
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
      const module =
        feature.module && feature.module.trim() !== ''
          ? feature.module
          : 'SIN_MODULO';
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
        const permissions: { [roleId: number]: boolean } = {};

        this.roles.forEach((role) => {
          permissions[role.id] = feature.allowedRoles.includes(role.id);
        });

        this.matrixData.push({
          featureCode: feature.featureCode,
          name: feature.name,
          module: feature.module,
          requiredPlans: feature.requiredPlans,
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

  togglePermission(row: PermissionMatrixRow, roleId: number): void {
    // Solo super_admin puede editar
    if (!this.canEdit) {
      this.toast.warning(
        'Solo los super administradores pueden modificar permisos de roles'
      );
      return;
    }

    // Toggle el estado
    row.permissions[roleId] = !row.permissions[roleId];

    // Calcular nuevos allowed_roles
    const allowedRoles = this.roles
      .filter((role) => row.permissions[role.id])
      .map((role) => role.id);

    // Guardar cambio
    this.changes.set(row.featureCode, allowedRoles);
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

    // Convertir cambios al formato del backend: { "featureRoles": { "FEATURE_CODE": [roleIds] } }
    const featureRoles: { [featureCode: string]: number[] } = {};
    this.changes.forEach((allowedRoles, featureCode) => {
      featureRoles[featureCode] = allowedRoles;
    });

    const request: BulkPermissionUpdateRequest = { featureRoles };

    this.isSaving = true;
    this.permissionService.bulkUpdatePermissions(request).subscribe({
      next: (updatedFeatures) => {
        this.toast.success(
          `${
            Object.keys(featureRoles).length
          } permisos actualizados exitosamente`
        );
        this.changes.clear();
        this.hasChanges = false;
        this.isSaving = false;

        // Invalidar cache de validaciones para todas las features afectadas
        Object.keys(featureRoles).forEach((featureCode) => {
          this.permissionCacheService.invalidateFeatureCache(featureCode);
        });

        // Recargar datos
        this.loadData();
      },
      error: (error) => {
        console.error('Error guardando cambios:', error);
        this.toast.error('Error al guardar los cambios');
        this.isSaving = false;
      },
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
    const displayNames: { [key: string]: string } = {
      FREE: 'Gratuito',
      BASIC: 'Básico',
      PRO: 'Profesional',
      ENTERPRISE: 'Empresarial',
    };
    return displayNames[planCode] || planCode;
  }

  getRoleBadgeClass(roleId: number): string {
    const classes: { [id: number]: string } = {
      1: 'role-super-admin',
      2: 'role-member',
      3: 'role-admin',
      4: 'role-accountant',
      5: 'role-operator',
    };
    return classes[roleId] || 'role-default';
  }
}
