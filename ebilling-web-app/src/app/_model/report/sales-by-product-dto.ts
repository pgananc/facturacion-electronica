/**
 * DTO para reporte de ventas por producto/servicio
 * Estructura real del backend: { items: [...] }
 */
export interface SalesByProductItemDto {
  idProduct: number;
  productName: string;
  productCode: string;
  quantitySold: number; // Integer en el backend
  totalInvoiced: number; // BigDecimal en el backend
  unitPrice: number; // BigDecimal en el backend
  margin?: number; // BigDecimal en el backend, opcional si se maneja costo
}

export interface SalesByProductResponse {
  items: SalesByProductItemDto[];
  totalElements?: number;
  totalPages?: number;
  size?: number;
  number?: number;
}

// Alias para compatibilidad con código existente
export type SalesByProductDto = SalesByProductItemDto;

