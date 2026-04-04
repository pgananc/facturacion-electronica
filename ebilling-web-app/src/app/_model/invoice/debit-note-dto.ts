/**
 * DTOs para el backend de notas de débito (ebill-billing)
 * Basados en los DTOs de Java del backend
 */

import {
  ClientDto,
  CompanyDto,
  EstablishmentDto,
  InvoiceDetailDto,
  PaymentDto,
} from './invoice-dto';

export interface DebitNoteDto {
  idDebitNote?: number;
  idCompany?: number;
  idClient?: number;
  idIssuancePoint?: number;
  sequential?: number;
  retryCount?: number;

  companyDto?: CompanyDto;
  clientDto?: ClientDto;

  creationDate?: string; // ISO 8601
  issueDate?: string; // ISO 8601

  accessKey?: string; // Generado por el SRI
  debitNoteNumber?: string; // Número de nota de débito (formato: estab-ptoEmi-secuencial)

  authorizationNumber?: string; // Generado por el SRI
  additionalInformation?: string;
  observation?: string;
  authorizationDate?: string; // ISO 8601

  // Totales (se calculan automáticamente desde debitNoteDetails)
  totalAmount?: number;
  discount?: number;
  subtotalZero?: number;
  subtotalIva?: number;
  subtotalIce?: number;
  totalIva?: number;
  totalIce?: number;
  valorModificacion?: number; // Suma de subtotales de detalles
  valorTotal?: number; // valorModificacion + totalIva + totalIce

  status?: string;
  debitNoteStatus?: string; // "BORRADOR" | "AUTORIZADO" | "DEVUELTA" | "EN_PROCESO"

  debitNoteDetails?: InvoiceDetailDto[];
  details?: InvoiceDetailDto[]; // Alias para compatibilidad

  establishment?: EstablishmentDto;
  issuancePoint?: {
    idIssuancePoint: number;
    code: string;
  };

  // Campos específicos de Nota de Débito
  /** ID de la factura original que se modifica */
  originalInvoiceId?: number;

  /** Clave de acceso de la factura original */
  originalAccessKey?: string;

  /** Número de factura original (OBLIGATORIO para SRI) */
  originalInvoiceNumber?: string;

  /** Fecha de emisión de la factura original (OBLIGATORIO para SRI) */
  originalIssueDate?: string; // ISO 8601

  /** Tipo de documento modificado (01=Factura, 03=Liquidación, etc.) - Default: "01" */
  documentTypeModified?: string;

  /** Formas de pago */
  paymentMethods?: PaymentDto[];

  // Campos adicionales del cliente para facilitar la carga
  buyerIdentification?: string;
  buyerIdType?: number;
  buyerName?: string;
  buyerAddress?: string;
  buyerPhone?: string;
  buyerMail?: string;

  // Campos de autorización del SRI
  receptionStatusSri?: string;
  authorizationStatusSri?: string;
  authorizationSri?: AuthorizationSriDto;

  // Password de la firma digital (requerido para enviar al SRI)
  password?: string;

  // Campos de email
  emailSent?: boolean;
  emailSentDate?: string;

  // Environment (se asigna desde configuración)
  environment?: string; // "PRUEBAS" | "PRODUCCION"

  // Deprecated aliases
  /** @deprecated Alias de originalInvoiceId - Usar originalInvoiceId */
  idInvoice?: number;
}

export interface AuthorizationSriDto {
  idAuthorizationSri?: number;
  accessKey?: string;
  authorizationNumber?: string;
  authorizationDate?: string;
  authorizationStatusSri?: string;
  receptionStatusSri?: string;
  status?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface DebitNoteSearchDto {
  debitNoteNumber?: string;
  idClient?: number;
  originalInvoiceId?: number;
  creationDateFrom?: string; // ISO 8601
  creationDateTo?: string; // ISO 8601
  status?: string;
}

export interface DebitNoteResponse {
  code: number;
  status: string;
  message: string;
  debitNoteDto?: DebitNoteDto;
  idDebitNote?: number;
  success?: boolean;
  debitNoteNumber?: string;
  accessKey?: string;
  authorizationNumber?: string;
  authorizationDate?: string;
  authorizationStatusSri?: string;
  receptionStatusSri?: string;
  totalAmount?: number;
  errors?: { code: string; message: string }[];
}

/**
 * DTO para consulta de notas de débito
 */
export interface DebitNoteQueryDto {
  /** Número de nota de débito (búsqueda parcial) Ejemplo: "001-001-000000123" o "001" */
  debitNoteNumber?: string;

  /** ID del cliente */
  idClient?: number;

  /** ID de la factura original */
  originalInvoiceId?: number;

  /** Fecha de creación desde (inclusive) Ejemplo: "2025-11-01" */
  creationDateFrom?: string;

  /** Fecha de creación hasta (inclusive) Ejemplo: "2025-11-30" */
  creationDateTo?: string;

  /** Estado de recepción SRI */
  receptionStatusSri?: string;
}

/**
 * DTO de respuesta para búsqueda de notas de débito
 */
export interface DebitNoteSearchResponseDto {
  idDebitNote?: number;
  debitNoteNumber?: string;
  creationDate?: string;
  issueDate?: string;
  idClient?: number;
  nameClient?: string;
  buyerName?: string;
  buyerIdentification?: string;
  totalAmount?: number;
  valorModificacion?: number;
  valorTotal?: number;
  authorizationNumber?: string;
  receptionStatusSri?: string;
  authorizationStatusSri?: string;
  debitNoteStatus?: string;
  originalInvoiceNumber?: string;
  originalInvoiceId?: number;
}

/**
 * DTO para búsqueda de notas de débito por compañía
 */
export interface DebitNoteSearchByCompanyDto {
  /** ID de la compañía (obligatorio) */
  idCompany: number;
  /** Número de nota de débito (opcional) */
  debitNoteNumber?: string;
  /** Número de factura original (opcional) */
  originalInvoiceNumber?: string;
}

/**
 * DTO de respuesta para lista de notas de débito (usado en search-by-company)
 */
export interface DebitNoteListResponseDto {
  idDebitNote?: number;
  debitNoteNumber?: string;
  originalInvoiceNumber?: string;
  buyerIdentification?: string;
  buyerName?: string;
  totalAmount?: number;
  debitNoteStatus?: string;
  createDate?: string;
  creationDate?: string; // Alias para compatibilidad
}

/**
 * Respuesta paginada del backend
 */
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number; // Página actual (0-indexed)
  size: number;
  first?: boolean;
  last?: boolean;
}



