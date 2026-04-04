/**
 * Modelos para gestión CRUD de planes de suscripción (super_admin)
 */

import { PlanResponse } from './subscription-admin';

/**
 * Request para crear un nuevo plan
 * Usa las mismas propiedades que PlanResponse del backend
 */
export interface CreatePlanRequest {
  code: string; // FREE, BASIC, PRO, etc.
  name: string;
  description: string;
  priceAnnual: number;
  priceMonthly: number;
  documentLimitYearly: number | null; // null o -1 = ilimitado
  userLimit: number | null; // null o -1 = ilimitado
  establishmentLimit: number | null; // null o -1 = ilimitado
  issuancePointLimit: number | null; // null o -1 = ilimitado
  hasApiAccess: boolean;
  hasMultiUser: boolean;
  hasAdvancedReports: boolean;
  hasElectronicInvoice: boolean;
  hasCreditNote: boolean;
  hasDebitNote: boolean;
  hasRetention: boolean;
  hasGuide: boolean; // Nota: PlanResponse usa hasGuide, no hasGuides
  hasPrioritySupport: boolean;
  hasCustomBranding: boolean;
  hasAutomatedBackups: boolean;
  isActive: boolean;
  displayOrder: number;
}

/**
 * Request para actualizar un plan existente
 * Todos los campos son opcionales (partial update)
 * Usa las mismas propiedades que PlanResponse del backend
 */
export interface UpdatePlanRequest {
  code?: string;
  name?: string;
  description?: string;
  priceAnnual?: number;
  priceMonthly?: number;
  documentLimitYearly?: number | null;
  userLimit?: number | null;
  establishmentLimit?: number | null;
  issuancePointLimit?: number | null;
  hasApiAccess?: boolean;
  hasMultiUser?: boolean;
  hasAdvancedReports?: boolean;
  hasElectronicInvoice?: boolean;
  hasCreditNote?: boolean;
  hasDebitNote?: boolean;
  hasRetention?: boolean;
  hasGuide?: boolean; // Nota: PlanResponse usa hasGuide, no hasGuides
  hasPrioritySupport?: boolean;
  hasCustomBranding?: boolean;
  hasAutomatedBackups?: boolean;
  isActive?: boolean;
  displayOrder?: number;
}

/**
 * Response de creación/actualización de plan
 */
export type PlanManagementResponse = PlanResponse;
