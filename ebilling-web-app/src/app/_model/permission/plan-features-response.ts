/**
 * Modelos para la nueva API de permisos de planes (puerto 8480 - billing)
 *
 * Formato de respuesta del endpoint GET /api/permissions/features/plans
 */

/**
 * Features de un plan (formato booleano)
 */
export interface PlanFeatures {
  hasApiAccess: boolean;
  hasMultiUser: boolean;
  hasAdvancedReports: boolean;
  hasElectronicInvoice: boolean;
  hasCreditNote: boolean;
  hasDebitNote: boolean;
  hasRetention: boolean;
  hasGuide: boolean;
  hasPrioritySupport: boolean;
  hasCustomBranding: boolean;
  hasAutomatedBackups: boolean;
}

/**
 * Plan con sus features (respuesta del backend)
 */
export interface PlanFeaturesResponse {
  planCode: string;
  planName: string;
  features: PlanFeatures;
  /**
   * Feature codes habilitados para este plan (NUEVO - cuando el backend lo implemente)
   * Si este campo está presente, el frontend lo usará directamente sin mapeo hardcodeado
   * Si no está presente, el frontend usará el mapeo de booleanos a feature codes
   */
  featureCodes?: string[];
}

/**
 * Request para actualizar features de un plan
 */
export interface PlanFeaturesUpdateRequest {
  planCode: string;
  features: Partial<PlanFeatures>; // Parcial para permitir actualizar solo algunos campos
}

/**
 * Request para actualización masiva de features de planes
 */
export interface BulkPlanFeaturesUpdateRequest {
  updates: PlanFeaturesUpdateRequest[];
}

/**
 * Respuesta de actualización masiva
 */
export interface BulkUpdateResponse {
  success: boolean;
  message: string;
  totalUpdated: number;
  totalFailed: number;
  results: Array<{
    identifier: string; // planCode o featureCode
    success: boolean;
    message: string;
    error: string | null;
  }>;
  timestamp: string;
}

/**
 * Feature del sistema (respuesta de GET /api/permissions/features)
 */
export interface SystemFeature {
  code: string;
  name: string;
  description: string;
  category: string; // SYSTEM, DOCUMENT, SUPPORT
}
