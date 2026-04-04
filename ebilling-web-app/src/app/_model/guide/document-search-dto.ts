/**
 * DTOs para búsqueda de documentos autorizados activos para guías de remisión
 */

/**
 * Request para búsqueda de factura por número o clave de acceso
 */
export interface InvoiceSearchByNumberOrAccessKeyDto {
  /** ID de la compañía (obligatorio) */
  idCompany: number;
  /** Número de factura (opcional) */
  invoiceNumber?: string;
  /** Clave de acceso (opcional) */
  accessKey?: string;
}

/**
 * Request para búsqueda de nota de débito por número o clave de acceso
 */
export interface DebitNoteSearchByNumberOrAccessKeyDto {
  /** ID de la compañía (obligatorio) */
  idCompany: number;
  /** Número de comprobante (opcional) */
  debitNoteNumber?: string;
  /** Clave de acceso (opcional) */
  accessKey?: string;
}

/**
 * Request para búsqueda de nota de crédito por número o clave de acceso
 */
export interface CreditNoteSearchByNumberOrAccessKeyDto {
  /** ID de la compañía (obligatorio) */
  idCompany: number;
  /** Número de comprobante (opcional) */
  creditNoteNumber?: string;
  /** Clave de acceso (opcional) */
  accessKey?: string;
}

/**
 * Información básica de un producto asociado a un comprobante
 */
export interface ProductInfoDto {
  /** ID del producto */
  idProduct?: number;
  /** Código principal del producto (mainCode) */
  mainCode?: string;
  /** Código auxiliar del producto (si existe) */
  auxiliarCode?: string;
  /** Nombre del producto */
  name?: string;
  /** Cantidad del producto en el comprobante */
  quantity?: number;
  /** Precio unitario del producto */
  unitPrice?: number;
  /** Descuento aplicado al producto */
  discount?: number;
  /** Subtotal del producto (quantity * unitPrice - discount) */
  subtotal?: number;
}

/**
 * Información básica de un documento (factura, nota de crédito o nota de débito)
 */
export interface DocumentBasicInfoDto {
  /** Clave de acceso del documento autorizado (accessKey) */
  accessKey?: string;
  /** Número de autorización del SRI (authorizationNumber) */
  authorizationNumber?: string;
  /** Fecha de emisión del documento (issueDate) */
  issueDate?: string;
  /** Número de comprobante (invoiceNumber, creditNoteNumber o debitNoteNumber) */
  invoiceNumber?: string;
  /** Lista de productos asociados al comprobante */
  products?: ProductInfoDto[];
}

