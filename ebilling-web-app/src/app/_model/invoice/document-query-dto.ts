/**
 * DTOs para consulta unificada de documentos (facturas y notas de crédito)
 * Basados en DocumentQueryDto y DocumentSearchResponseDto del backend
 */

/**
 * DTO para consulta de documentos
 * Basado en DocumentQueryDto del backend
 */
export interface DocumentQueryDto {
  /**
   * Número de documento (factura o nota de crédito) - búsqueda parcial
   * Ejemplo: "001-001-000000123" o "001"
   */
  documentNumber?: string;

  /**
   * Identificación del comprador (cédula/RUC) Ejemplo: "1234567890"
   * Nota: Se busca primero el cliente por identificación y luego sus documentos
   */
  buyerIdentification?: string;

  /** Fecha de creación desde (inclusive) Ejemplo: "2025-11-01" */
  creationDateFrom?: string;

  /** Fecha de creación hasta (inclusive) Ejemplo: "2025-11-30" */
  creationDateTo?: string;

  /**
   * Tipo de comprobante a consultar:
   * 0 = Facturas y Notas de Crédito (ambos)
   * 1 = Solo Facturas
   * 2 = Solo Notas de Crédito
   */
  documentType?: number;

  /** Estado de recepción SRI */
  receptionStatusSri?: string;

  /** Estado de autorización SRI */
  authorizationStatusSri?: string;
}

/**
 * DTO de respuesta para búsqueda de documentos
 * Basado en DocumentSearchResponseDto del backend
 */
export interface DocumentSearchResponseDto {
  /**
   * Tipo de documento: 1 = Factura, 2 = Nota de Crédito
   */
  documentType?: number;

  /**
   * ID del documento (idInvoice o idCreditNote según el tipo)
   */
  idDocument?: number;

  /**
   * Número del documento (invoiceNumber o creditNoteNumber)
   */
  documentNumber?: string;

  /** Fecha de creación */
  creationDate?: string;

  /** ID del cliente */
  idClient?: number;

  /** Nombre del cliente */
  nameClient?: string;

  /** Monto total */
  totalAmount?: number;

  /** Número de autorización */
  authorizationNumber?: string;

  /** Estado de recepción SRI */
  receptionStatusSri?: string;

  /** Estado de autorización SRI */
  authorizationStatusSri?: string;
}
