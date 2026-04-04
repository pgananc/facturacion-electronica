/**
 * DTO para reporte de formas de pago
 * Estructura real del backend: { items: [...], totalAmount: number }
 */
export interface PaymentMethodReportItemDto {
  idPaymentMethod: number;
  paymentMethodCode: string;
  paymentMethodType: string;
  totalAmount: number; // BigDecimal en el backend
  totalTransactions: number; // Long en el backend
  percentage: number; // BigDecimal en el backend
}

export interface PaymentMethodReportResponse {
  items: PaymentMethodReportItemDto[];
  totalAmount: number; // BigDecimal en el backend
  totalElements?: number;
  totalPages?: number;
  size?: number;
  number?: number;
}

// Alias para compatibilidad con código existente
export type PaymentMethodReportDto = PaymentMethodReportItemDto;

