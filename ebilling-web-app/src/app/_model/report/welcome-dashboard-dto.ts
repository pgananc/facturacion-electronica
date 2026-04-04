/**
 * DTOs para el endpoint /api/dashboard/welcome
 * Estructura completa del dashboard de bienvenida
 */

export interface WelcomeDashboardDto {
  kpis: KpiSummaryDto;
  salesTrend: SalesTrendItemDto[];
  trendSummary: TrendSummaryDto;
  recentInvoices: RecentInvoiceDto[];
  alerts: AlertDto[];
}

export interface KpiSummaryDto {
  todaySales: KpiDto;
  weekSales: KpiDto;
  monthSales: KpiDto;
  pendingCollection: PendingCollectionDto;
}

export interface KpiDto {
  amount: number;
  invoiceCount: number;
  percentageChange?: number;
  goalPercentage?: number;
}

export interface PendingCollectionDto {
  amount: number;
  invoiceCount: number;
  overdueCount: number;
}

export interface SalesTrendItemDto {
  date: string; // ISO 8601 format (YYYY-MM-DD)
  dayOfWeek: string; // "Lunes", "Martes", etc.
  amount: number;
  invoiceCount: number;
}

export interface TrendSummaryDto {
  average: number;
  total: number;
}

export interface RecentInvoiceDto {
  idInvoice: number;
  invoiceNumber: string;
  clientName: string;
  clientIdentification: string;
  totalAmount: number;
  issueDate: string; // ISO 8601 with time
  authorizationStatus: string; // "AUTORIZADO", "PENDIENTE", "RECHAZADA"
  paymentStatus: string; // "PAID", "PENDING", "OVERDUE"
}

export interface AlertDto {
  id: string;
  type: AlertType;
  priority: AlertPriority;
  title: string;
  message: string;
  count?: number;
  actionUrl?: string;
  metadata?: { [key: string]: any };
  createdAt: string; // ISO 8601 with time
}

export enum AlertType {
  ERROR = 'ERROR',
  WARNING = 'WARNING',
  INFO = 'INFO',
  SUCCESS = 'SUCCESS',
}

export enum AlertPriority {
  HIGH = 'HIGH',
  MEDIUM = 'MEDIUM',
  LOW = 'LOW',
}

/**
 * Response wrapper del endpoint
 */
export interface WelcomeDashboardResponse {
  status: string;
  message: string;
  data: WelcomeDashboardDto;
}
