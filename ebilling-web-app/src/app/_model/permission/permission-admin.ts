/**
 * Modelos para Administración de Permisos de Roles (super_admin)
 */

export interface FeaturePermissionAdmin {
  featureCode: string;
  name: string;
  module: string;
  allowedRoles: number[];
  requiredPlans: string[];
  description?: string;
}

export interface RolePermissionUpdate {
  featureCode: string;
  allowedRoles: number[];
}

export interface Role {
  id: number;
  name: string;
  description: string;
}

export interface PermissionMatrixRow {
  featureCode: string;
  name: string;
  module: string;
  requiredPlans: string[];
  description?: string;
  permissions: { [roleId: number]: boolean };
}

export interface BulkPermissionUpdateRequest {
  featureRoles: { [featureCode: string]: number[] }; // Formato del backend
}

// Formato alternativo para compatibilidad (si se necesita)
export interface BulkPermissionUpdateRequestLegacy {
  updates: RolePermissionUpdate[];
}
