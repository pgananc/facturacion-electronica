/**
 * Constantes globales para reportes
 */

// Opciones de paginación estándar
export const REPORT_PAGE_SIZE_OPTIONS = [10, 25, 50, 100];
export const REPORT_DEFAULT_PAGE_SIZE = 10;

// Tipos de documentos electrónicos (SRI Ecuador)
export enum DocumentType {
  FACTURA = 'FACTURA',
  NOTA_CREDITO = 'NOTA_CREDITO',
  NOTA_DEBITO = 'NOTA_DEBITO',
  RETENCION = 'RETENCION',
  LIQUIDACION_COMPRA = 'LIQUIDACION_COMPRA',
  GUIA_REMISION = 'GUIA_REMISION',
}

export const DOCUMENT_TYPE_LABELS: { [key in DocumentType]: string } = {
  [DocumentType.FACTURA]: 'Factura',
  [DocumentType.NOTA_CREDITO]: 'Nota de Crédito',
  [DocumentType.NOTA_DEBITO]: 'Nota de Débito',
  [DocumentType.RETENCION]: 'Retención',
  [DocumentType.LIQUIDACION_COMPRA]: 'Liquidación de Compra',
  [DocumentType.GUIA_REMISION]: 'Guía de Remisión',
};

// Estados de autorización SRI
// Catálogo de estados con sus códigos para el backend
export enum SriAuthorizationStatus {
  AUTORIZADA = 'AUTORIZADA',
  RECHAZADA = 'RECHAZADA',
  EN_PROCESO = 'EN_PROCESO',
  PENDIENTE_ENVIO = 'PENDIENTE_ENVIO',
  CADUCADA = 'CADUCADA',
  NO_AUTORIZADA = 'NO_AUTORIZADA',
  DEVUELTA = 'DEVUELTA',
}

// Descripciones amigables para mostrar en la UI
export const SRI_STATUS_LABELS: { [key in SriAuthorizationStatus]: string } = {
  [SriAuthorizationStatus.AUTORIZADA]: 'Autorizada',
  [SriAuthorizationStatus.RECHAZADA]: 'Rechazada',
  [SriAuthorizationStatus.EN_PROCESO]: 'En proceso',
  [SriAuthorizationStatus.PENDIENTE_ENVIO]: 'Pendiente de Envio',
  [SriAuthorizationStatus.CADUCADA]: 'Caducada',
  [SriAuthorizationStatus.NO_AUTORIZADA]: 'No Autorizada',
  [SriAuthorizationStatus.DEVUELTA]: 'Devuelta',
};

// Códigos que se envían al backend en el request
export const SRI_STATUS_CODES: { [key in SriAuthorizationStatus]: string } = {
  [SriAuthorizationStatus.AUTORIZADA]: 'AUTORIZADO',
  [SriAuthorizationStatus.RECHAZADA]: 'RECHAZADO',
  [SriAuthorizationStatus.EN_PROCESO]: 'EN PROCESO',
  [SriAuthorizationStatus.PENDIENTE_ENVIO]: 'PENDIENTE DE ENVIO',
  [SriAuthorizationStatus.CADUCADA]: 'CADUCADA',
  [SriAuthorizationStatus.NO_AUTORIZADA]: 'NO AUTORIZADO',
  [SriAuthorizationStatus.DEVUELTA]: 'DEVUELTA',
};

// Clases CSS para estados (basadas en la paleta corporativa)
export const SRI_STATUS_CLASSES: { [key in SriAuthorizationStatus]: string } = {
  [SriAuthorizationStatus.AUTORIZADA]: 'status-success',
  [SriAuthorizationStatus.RECHAZADA]: 'status-error',
  [SriAuthorizationStatus.EN_PROCESO]: 'status-warning',
  [SriAuthorizationStatus.PENDIENTE_ENVIO]: 'status-info',
  [SriAuthorizationStatus.CADUCADA]: 'status-inactive',
  [SriAuthorizationStatus.NO_AUTORIZADA]: 'status-error',
  [SriAuthorizationStatus.DEVUELTA]: 'status-warning',
};

// Estados de autorización SRI para facturas pendientes
// Catálogo específico con estados pendientes
export enum PendingInvoiceStatus {
  RECHAZADA = 'RECHAZADA',
  CADUCADA = 'CADUCADA',
  PENDIENTE = 'PENDIENTE',
  DEVUELTA = 'DEVUELTA',
}

// Descripciones amigables para mostrar en la UI
export const PENDING_INVOICE_STATUS_LABELS: { [key in PendingInvoiceStatus]: string } = {
  [PendingInvoiceStatus.RECHAZADA]: 'Rechazada',
  [PendingInvoiceStatus.CADUCADA]: 'Caducada',
  [PendingInvoiceStatus.PENDIENTE]: 'Pendiente',
  [PendingInvoiceStatus.DEVUELTA]: 'Devuelta',
};

// Códigos que se envían al backend en el request
export const PENDING_INVOICE_STATUS_CODES: { [key in PendingInvoiceStatus]: string } = {
  [PendingInvoiceStatus.RECHAZADA]: 'RECHAZADO',
  [PendingInvoiceStatus.CADUCADA]: 'CADUCADA',
  [PendingInvoiceStatus.PENDIENTE]: 'PENDIENTE',
  [PendingInvoiceStatus.DEVUELTA]: 'DEVUELTA',
};

// Configuración de fechas por defecto
export const REPORT_DATE_CONFIG = {
  DEFAULT_FROM: 'FIRST_DAY_OF_MONTH', // Primer día del mes actual
  DEFAULT_TO: 'TODAY', // Día actual
  MAX_RANGE_DAYS: 365, // Máximo 1 año de rango
};
