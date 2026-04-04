export interface FeatureValidationResponse {
  allowed: boolean;
  reason?: string; // 'ACCESS_GRANTED', 'PLAN_DOES_NOT_INCLUDE_FEATURE', 'NO_ACTIVE_SUBSCRIPTION', etc.
  message: string;
  upgradeUrl?: string;
}

export interface FeatureValidationRequest {
  featureCode: string;
  idCompany: number;
  idUser: number; // ✅ Requerido por el backend para validar permisos del rol
}

export interface CanEmitDocumentRequest {
  documentType:
    | 'INVOICE'
    | 'CREDIT_NOTE'
    | 'DEBIT_NOTE'
    | 'RETENTION'
    | 'GUIDE';
}

export interface CanEmitDocumentResponse {
  allowed: boolean;
  currentUsage: number;
  limit: number | null;
  remaining: number;
  message: string;
}
