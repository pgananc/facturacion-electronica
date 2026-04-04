/**
 * DTO para reporte comparativo por sucursal
 * Estructura real del backend: { items: [...] }
 */
export interface BranchComparisonItemDto {
  idEstablishment: number;
  establishmentName: string;
  idIssuancePoint: number;
  issuancePointName: string;
  totalInvoiced: number; // BigDecimal en el backend
  totalInvoices: number; // Long en el backend
  averageTicket: number; // BigDecimal en el backend
  ranking: number; // Integer en el backend
}

export interface BranchComparisonResponse {
  items: BranchComparisonItemDto[];
}

// Alias para compatibilidad con código existente
export type BranchComparisonDto = BranchComparisonItemDto;

