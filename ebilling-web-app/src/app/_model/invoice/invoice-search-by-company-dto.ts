/**
 * DTO para la búsqueda de facturas por compañía y rango de fechas con filtros opcionales.
 * Basado en InvoiceSearchByCompanyAndDateDto del backend
 */
export interface InvoiceSearchByCompanyAndDateDto {
  /** ID de la compañía (obligatorio) */
  idCompany: number;

  /** Fecha de creación desde (inclusive) - Opcional */
  creationDateFrom?: string; // ISO 8601 format

  /** Fecha de creación hasta (inclusive) - Opcional */
  creationDateTo?: string; // ISO 8601 format

  /** Número de autorización (opcional) */
  numeroAutorizacion?: string;

  /** Número de factura (opcional) */
  numeroFactura?: string;

  /** Nombre del cliente (opcional) */
  nombreCliente?: string;

  /** Identificación del cliente (opcional) */
  identificacionCliente?: string;

  /** Estado de recepción del SRI (opcional) */
  receptionStatusSri?: string;

  /** Estado de autorización del SRI (opcional) */
  authorizationStatusSri?: string;
}

