/**
 * Modelos para Administración de Suscripciones (super_admin)
 */

export interface SubscriptionAdmin {
  idCompany: number;
  companyName: string;
  businessName: string;
  ruc: string;
  plan: {
    code: string;
    name: string;
    documentLimitYearly: number;
    monthlyPrice?: number;
  };
  subscriptionStatus: 'ACTIVE' | 'TRIAL' | 'EXPIRED' | 'CANCELLED';
  currentYearUsage: number;
  percentUsed: number;
  remainingDocuments: number;
  startDate: string;
  expiresAt: string;
  cancelledAt?: string; // Fecha de cancelación (cuando cancelImmediately = true o cuando la tarea programada procesa la cancelación)
  statusDetail?: string;
}

export interface PlanChangeRequest {
  newPlanCode: string; // Código del nuevo plan (ej: "BASIC", "PRO")
  paymentDate: string; // Fecha del pago en formato ISO LocalDateTime: "YYYY-MM-DDTHH:mm:ss"
  paymentAmount?: number; // Monto del pago (condicional: si se envía, debe ser > 0)
  paymentReference?: string; // Referencia del pago (opcional)
  notes?: string; // Notas adicionales (opcional)
}

export interface SubscriptionExtendRequest {
  extensionMonths: number; // 🔧 Campo correcto según backend
  reason?: string;
}

export interface SubscriptionCancelRequest {
  reason: string; // Razón de cancelación (ej: "PAYMENT_FAILURE", "CUSTOMER_REQUEST", etc.)
  cancelImmediately: boolean; // Si true, cancela inmediatamente; si false, cancela al final del período
  notes?: string; // Notas adicionales sobre la cancelación
}

export interface AssignSubscriptionRequest {
  idCompany: number;
  planCode: string;
  startDate: string; // Formato ISO 8601: YYYY-MM-DD
  durationMonths: number; // Entre 1 y 60 meses
}

/**
 * Respuesta completa del backend para GET /api/subscriptions/plans
 */
export interface PlanResponse {
  idPlan: number;
  code: string;
  name: string;
  description: string;
  priceAnnual: number;
  priceMonthly: number;
  documentLimitYearly: number; // -1 = ilimitado
  userLimit: number; // -1 = ilimitado
  establishmentLimit: number; // -1 = ilimitado
  issuancePointLimit: number; // -1 = ilimitado
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
  isActive: boolean;
  displayOrder: number;
  features: string[];
}

/**
 * Plan simplificado para uso en componentes (compatible con versión anterior)
 */
export interface AvailablePlan {
  code: string;
  name: string;
  price: number; // Precio mensual (mapeado desde priceMonthly)
  documentLimit: number; // Límite de documentos anuales (mapeado desde documentLimitYearly, -1 = ilimitado)
  features: string[];
}

/**
 * Respuesta paginada de Spring para suscripciones
 */
export interface SubscriptionPageResponse {
  content: SubscriptionAdmin[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
  pageable?: any;
  sort?: any;
}
