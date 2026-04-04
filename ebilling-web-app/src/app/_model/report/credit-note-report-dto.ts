/**
 * DTO para reporte de notas de crédito
 */
export interface CreditNoteReportDto {
  creditNoteNumber: string;
  creationDate: string;
  originalInvoiceId: number;
  originalInvoiceNumber: string;
  originalAccessKey?: string;
  reason: string;
  totalAmount: number | { source: string; parsedValue: number };
  authorizationStatusSri: string;
  idClient: number;
  clientName: string;
  clientIdentification: string;
}

export interface CreditNoteReportPagination {
  currentPage: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
  isFirst: boolean;
  isLast: boolean;
}

export interface CreditNoteReportResponse {
  items: CreditNoteReportDto[];
  totalReturned: number;
  totalCreditNotes: number;
  pagination: CreditNoteReportPagination;
}
