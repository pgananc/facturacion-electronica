export interface InvoiceDetail {
  idProduct?: number; // Opcional porque productos nuevos no tienen ID
  code?: string; // ✅ Código principal del producto (mainCode)
  description: string;
  unitPrice: number;
  iva?: number; // ✅ Porcentaje de IVA (0-100) desde ivaTaxType.value
  discount: number; // Porcentaje de descuento (0-100)
  ice: number; // ✅ Porcentaje de ICE (0-100) desde iceTaxType.value
  quantity: number;
  totalValue: number;
  product?: any; // ✅ Producto completo para tener acceso a mainCode, ivaTaxType, iceTaxType, etc.
}
