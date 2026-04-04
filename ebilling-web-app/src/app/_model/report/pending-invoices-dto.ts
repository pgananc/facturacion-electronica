/**
 * DTO para reporte de facturas pendientes/errores SRI
 * Estructura real del backend: { items: [...], totalPending: number }
 */
export interface PendingInvoicesItemDto {
  invoiceNumber: string;
  issueDate: string; // LocalDateTime del backend se serializa como string ISO
  receptionStatusSri: string;
  authorizationStatusSri: string;
  receptionError?: string;
  authorizationError?: string;
  idClient: number;
  clientName: string;
  idIssuancePoint: number;
  retryCount: number; // Integer en el backend
}

export interface PendingInvoiceResponse {
  items: PendingInvoicesItemDto[];
  totalPending: number; // Long en el backend
}

// Alias para compatibilidad con código existente
export type PendingInvoiceDto = PendingInvoicesItemDto;

