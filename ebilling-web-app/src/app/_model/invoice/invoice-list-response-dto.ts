/**
 * DTO para la respuesta de listado de facturas.
 * Incluye todos los campos de Invoice más los datos básicos del cliente (name e identification).
 * Basado en InvoiceListResponseDto del backend
 */
import { InvoiceDetail } from './invoice-detail';
import { Payment } from './payment';
import { AuthorizationSriDto } from './invoice-dto';

export interface InvoiceListResponseDto {
  idInvoice?: number;
  idCompany?: number;
  idClient?: number;
  idIssuancePoint?: number;
  sequential?: number;
  retryCount?: number;
  issueDate?: string; // ISO 8601
  invoiceNumber?: string;
  environment?: string;
  additionalInformation?: string;
  totalAmount?: number;
  tip?: number;
  subtotalZero?: number;
  subtotalIva?: number;
  subtotalIce?: number;
  discount?: number;
  emailSent?: boolean;
  emailSentDate?: string; // ISO 8601
  authorizationSri?: AuthorizationSriDto;
  invoiceDetails?: InvoiceDetail[];
  invoicePayments?: Payment[];

  // Campos de BaseEntity
  creationDate?: string; // ISO 8601
  updateDate?: string; // ISO 8601
  status?: boolean;

  // Datos del cliente (nuevos campos)
  clientName?: string;
  clientIdentification?: string;

  // Monto disponible para notas de crédito (totalAmount - suma de NC autorizadas)
  availableAmount?: number;

  // Estado de autorización del SRI (campo directo para facilitar acceso)
  authorizationStatusSri?: string;
  
  // Estado de recepción del SRI (campo directo para facilitar acceso)
  receptionStatusSri?: string;
}

