/**
 * Modelos para Administración de Permisos de Planes (super_admin)
 *
 * Este sistema permite gestionar qué funcionalidades están incluidas en cada plan de suscripción.
 * Separado de los permisos de roles para permitir gestión independiente.
 */

/**
 * Plan de suscripción con información básica
 */
export interface SubscriptionPlan {
  code: string; // FREE, BASIC, PRO, ENTERPRISE
  name: string; // Nombre amigable: Gratuito, Básico, Profesional, Empresarial
  description: string;
}

/**
 * Feature con información de permisos de planes
 */
export interface FeaturePlanPermission {
  featureCode: string;
  name: string;
  module: string;
  allowedPlans: string[]; // Códigos de planes que incluyen este feature
  description?: string;
}

/**
 * Fila de la matriz de permisos de planes
 */
export interface PlanPermissionMatrixRow {
  featureCode: string;
  name: string;
  module: string;
  description?: string;
  permissions: { [planCode: string]: boolean }; // Mapa de planCode -> tiene permiso
}

/**
 * Request para actualizar permisos de un feature en múltiples planes
 */
export interface PlanPermissionUpdate {
  featureCode: string;
  allowedPlans: string[]; // Lista de códigos de planes que tendrán acceso
}

/**
 * Request para actualización masiva de permisos de planes
 */
export interface BulkPlanPermissionUpdateRequest {
  featurePlans: { [featureCode: string]: string[] }; // Formato del backend
}

// Formato alternativo para compatibilidad (si se necesita)
export interface BulkPlanPermissionUpdateRequestLegacy {
  updates: PlanPermissionUpdate[];
}
