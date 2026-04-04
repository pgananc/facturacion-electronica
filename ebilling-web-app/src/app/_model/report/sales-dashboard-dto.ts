/**
 * DTO para dashboard de ventas
 * ACTUALIZADO: Coincide con estructura del backend (diciembre 2024)
 */
export interface SalesDashboardDto {
  // KPIs principales
  todayTotal: number;
  weekTotal: number;
  monthTotal: number;
  previousMonthTotal: number;

  // Tendencia de ventas (últimos 30 días)
  salesTrend?: SalesTrendItemDto[];

  // Top 10 productos más vendidos
  topProducts?: TopProductDto[];

  // Distribución por sucursal
  branchDistribution?: BranchDistributionDto[];

  // Métricas adicionales (opcional)
  additionalMetrics?: AdditionalMetricsDto;
}

/**
 * Item de tendencia diaria de ventas
 */
export interface SalesTrendItemDto {
  date: string; // Formato: "YYYY-MM-DD"
  total: number;
  invoiceCount: number;
}

/**
 * Producto más vendido
 */
export interface TopProductDto {
  productId: number;
  productCode: string;
  productName: string;
  totalSales: number;
  quantitySold: number;
  invoiceCount: number;
}

/**
 * Distribución de ventas por sucursal
 */
export interface BranchDistributionDto {
  branchId: number;
  branchCode: string;
  branchName: string;
  totalSales: number;
  invoiceCount: number;
  percentage: number;
}

/**
 * Métricas adicionales del dashboard
 */
export interface AdditionalMetricsDto {
  averageTicket: number;
  totalInvoices: number;
  totalClients: number;
  totalProducts: number;
}

// ========================================
// DEPRECATED: Interfaces antiguas (mantener por compatibilidad)
// ========================================

/**
 * @deprecated Usar SalesTrendItemDto en su lugar
 */
export interface MonthlySalesDto {
  month: string;
  year: number;
  total: number;
}

/**
 * @deprecated Usar BranchDistributionDto en su lugar
 */
export interface BranchSalesDto {
  branchId: number;
  branchName: string;
  total: number;
}

/**
 * @deprecated Usar TopProductDto en su lugar
 */
export interface ProductSalesDto {
  productId: number;
  productName: string;
  total: number;
}



