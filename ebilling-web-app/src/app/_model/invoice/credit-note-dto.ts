/**
 * DTOs para el backend de notas de crédito (ebill-billing)
 * Basados en los DTOs de Java del backend
 */

import {
  ClientDto,
  CompanyDto,
  EstablishmentDto,
  InvoiceDetailDto,
} from './invoice-dto';

export interface CreditNoteDto {
  idCreditNote?: number;
  idCompany?: number;
  idClient?: number;
  idIssuancePoint?: number;
  sequential?: number;

  companyDto?: CompanyDto;
  clientDto?: ClientDto;

  creationDate?: string; // ISO 8601
  issueDate?: string; // ISO 8601

  accessKey?: string; // Generado por el SRI
  creditNoteNumber?: string; // Número de nota de crédito (formato: estab-ptoEmi-secuencial)

  authorizationNumber?: string; // Generado por el SRI
  additionalInformation?: string;
  authorizationDate?: string; // ISO 8601

  totalAmount?: number;
  discount?: number;
  subtotalZero?: number;
  subtotalIva?: number;
  subtotalIce?: number;
  subtotalDiscount?: number;
  valorModificacion?: number;

  status?: string;

  creditNoteDetails?: InvoiceDetailDto[];
  details?: InvoiceDetailDto[]; // Alias para compatibilidad con frontend

  establishment?: EstablishmentDto;

  // Campos específicos de Nota de Crédito
  /** ID de la factura original que se modifica/anula */
  originalInvoiceId?: number;

  /** Clave de acceso de la factura original */
  originalAccessKey?: string;

  /** Número de factura original (formato: estab-ptoEmi-secuencial) */
  originalInvoiceNumber?: string;

  /** Fecha de emisión de la factura original */
  originalIssueDate?: string; // ISO 8601

  /** Tipo de documento modificado (01=Factura, 03=Liquidación, etc.) */
  documentTypeModified?: string;

  /** Motivo de la nota de crédito (obligatorio) */
  reason?: string;

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
}

export interface AuthorizationSriDto {
  idAuthorizationSri?: number;
  accessKey?: string;
  authorizationNumber?: string;
  authorizationDate?: string;
  status?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreditNoteSearchDto {
  creditNoteNumber?: string;
  idClient?: number;
  originalInvoiceId?: number;
  creationDateFrom?: string; // ISO 8601
  creationDateTo?: string; // ISO 8601
  status?: string;
}

export interface CreditNoteResponse {
  code: number;
  status: string;
  message: string;
  creditNoteDto?: CreditNoteDto;
  idCreditNote?: number;
  success?: boolean;
  creditNoteNumber?: string;
  accessKey?: string;
  authorizationNumber?: string;
  authorizationDate?: string;
  authorizationStatus?: string;
  totalAmount?: number;
  errors?: { code: string; message: string }[];
}

/**
 * Motivo de Nota de Crédito (catálogo)
 */
export interface CreditNoteReasonDto {
  idReason: number;
  code: string;
  description: string;
  status?: boolean;
}

/**
 * Detalle de factura disponible para NC
 */
export interface InvoiceAvailableForCreditNoteDto {
  idInvoice: number;
  invoiceNumber: string;
  issueDate: string;
  accessKey: string;
  authorizationNumber: string;
  client: {
    idClient: number;
    identification: string;
    name: string;
    email: string;
    address?: string;
    phone?: string;
  };
  totalInvoice: number;
  tip?: number; // Propina (no reembolsable)
  totalCreditNotesApplied: number;
  availableAmount: number;
  details: InvoiceDetailAvailableDto[];
}

export interface InvoiceDetailAvailableDto {
  idInvoiceDetail: number;
  idProduct: number;
  mainCode: string;
  auxCode?: string;
  description: string;
  quantityInvoiced: number;
  quantityReturned: number;
  quantityAvailable: number;
  unitPrice: number;
  discount: number;
  subtotal: number;
  ivaPercentage: number;
}

/**
 * Request para crear Nota de Crédito
 * Estructura esperada por el backend
 */
export interface CreateCreditNoteRequest {
  idInvoice: number;
  idReason: number;
  idIssuancePoint: number;
  observation?: string;
  issueDate: string;
  details: CreateCreditNoteDetailRequest[];
  companyDto: {
    idCompany: number;
  };
  clientDto: {
    idClient: number;
  };
}

export interface CreateCreditNoteDetailRequest {
  idInvoiceDetail: number;
  idProduct: number;
  quantity: number;
  unitPrice: number;
  discount: number;
  subtotal?: number;
}

/**
 * DTO para consulta de notas de crédito
 * Basado en CreditNoteQueryDto del backend
 */
export interface CreditNoteQueryDto {
  /** Número de nota de crédito (búsqueda parcial) Ejemplo: "001-001-000000123" o "001" */
  creditNoteNumber?: string;

  /**
   * Identificación del comprador (cédula/RUC) Ejemplo: "1234567890"
   * Nota: Se busca primero el cliente por identificación y luego sus notas de crédito
   */
  buyerIdentification?: string;

  /** Fecha de creación desde (inclusive) Ejemplo: "2025-11-01" */
  creationDateFrom?: string;

  /** Fecha de creación hasta (inclusive) Ejemplo: "2025-11-30" */
  creationDateTo?: string;

  /** Estado de recepción SRI */
  receptionStatusSri?: string;
}

/**
 * DTO de respuesta para búsqueda de notas de crédito
 * Basado en CreditNoteSearchResponseDto del backend
 */
export interface CreditNoteSearchResponseDto {
  idCreditNote?: number;
  creditNoteNumber?: string;
  creationDate?: string;
  idClient?: number;
  nameClient?: string;
  totalAmount?: number;
  authorizationNumber?: string;
  receptionStatusSri?: string;
  authorizationStatusSri?: string;
}

/**
 * DTO para búsqueda de notas de crédito por compañía
 */
export interface CreditNoteSearchByCompanyDto {
  /** ID de la compañía (obligatorio) */
  idCompany: number;
  /** Número de nota de crédito (opcional) */
  creditNoteNumber?: string;
  /** Número de factura original (opcional) */
  originalInvoiceNumber?: string;
}