/**
 * DTO para reporte de ventas por cliente
 * Estructura real del backend: { items: [...] }
 */
export interface SalesByClientDto {
  idClient: number;
  clientName: string;
  clientIdentification: string; // El backend usa 'clientIdentification' no 'identification'
  totalSales: number;
  totalInvoices: number; // El backend usa 'totalInvoices' no 'numberOfInvoices'
  averageTicket: number;
}

export interface SalesByClientResponse {
  items: SalesByClientDto[]; // El backend usa 'items' no 'content'
  totalElements?: number;
  totalPages?: number;
  size?: number;
  number?: number;
}

