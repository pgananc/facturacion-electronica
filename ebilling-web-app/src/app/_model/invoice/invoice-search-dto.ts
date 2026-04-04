export interface InvoiceSearchDto {
  invoiceNumber?: string;
  buyerIdentification?: string;
  creationDateFrom?: Date;
  creationDateTo?: Date;
  receptionStatusSri?: string;
  authorizationStatusSri?: string;
}
