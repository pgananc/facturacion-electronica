/**
 * DTO para reporte de ventas por período
 * Estructura real del backend: { items: [...], metrics: {...} }
 */
export interface SalesByPeriodItemDto {
  invoiceNumber: string;
  idClient: number;
  clientName: string;
  clientIdentification: string;
  subtotal: number;
  iva: number;
  total: number;
  paymentMethod: string;
  authorizationStatusSri: string;
  receptionStatusSri: string;
  issueDate: string; // LocalDateTime del backend se serializa como string ISO
  idIssuancePoint: number;
}

export interface SalesByPeriodMetricsDto {
  totalInvoiced: number;
  totalComprobantes: number; // Long en el backend
  averageTicket: number;
}

export interface SalesByPeriodResponse {
  items: SalesByPeriodItemDto[];
  metrics: SalesByPeriodMetricsDto;
  totalElements?: number;
  totalPages?: number;
  size?: number;
  number?: number;
}

// Alias para compatibilidad con código existente
export type SalesByPeriodDto = SalesByPeriodItemDto;
export type SalesByPeriodMetrics = SalesByPeriodMetricsDto;

