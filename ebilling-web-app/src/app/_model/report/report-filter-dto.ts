/**
 * DTO para filtros de reportes
 */
export interface ReportFilterDto {
  dateFrom?: string;
  dateTo?: string;
  companyId?: number;
  branchId?: number;
  emissionPointId?: number;
  documentType?: string;
  status?: string;
  clientId?: number;
  productId?: number;
}



