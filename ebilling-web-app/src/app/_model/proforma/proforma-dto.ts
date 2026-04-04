/**
 * DTOs para la API de Proformas (ebill-billing)
 * Base URL: /api/proforma
 * Auth: Bearer JWT
 */

import { ClientDto } from '../invoice/invoice-dto';

/** Estados posibles de una proforma */
export type ProformaStatus = 'DRAFT' | 'SENT' | 'CONVERTED' | 'EXPIRED';

/** Detalle de producto para crear/actualizar proforma */
export interface ProformaDetailRequestDto {
  idProduct: number;
  productCode?: string;
  description?: string;
  quantity: number;
  unitPrice: number;
  discount?: number;
  ivaPercentage: number;
  icePercentage?: number;
  lineOrder: number;
}

/** Request para crear proforma */
export interface ProformaCreateRequestDto {
  idCompany: number;
  idEstablishment: number;
  idIssuancePoint: number;
  idClient: number;
  issueDate: string; // yyyy-MM-dd
  validUntil: string; // yyyy-MM-dd
  additionalInformation?: string;
  details: ProformaDetailRequestDto[];
}

/** Request para actualizar proforma (misma forma que create) */
export type ProformaUpdateRequestDto = ProformaCreateRequestDto;

/** Detalle de proforma en respuesta */
export interface ProformaDetailDto {
  idProformaDetail?: number;
  idProforma?: number;
  idProduct: number;
  productCode?: string;
  description?: string;
  quantity: number;
  unitPrice: number;
  discount?: number;
  ivaPercentage: number;
  icePercentage?: number;
  subtotal?: number;
  lineOrder: number;
}

/** Proforma completa (response de create, update, getById) */
export interface ProformaDto {
  idProforma: number;
  idCompany: number;
  idEstablishment: number;
  idIssuancePoint: number;
  idClient: number;
  proformaNumber: string;
  issueDate: string; // yyyy-MM-dd
  validUntil: string; // yyyy-MM-dd
  status: ProformaStatus;
  idInvoice: number | null;
  additionalInformation?: string;
  createdAt?: string; // yyyy-MM-dd'T'HH:mm:ss
  updatedAt?: string; // yyyy-MM-dd'T'HH:mm:ss
  createdBy?: string | null;
  clientDto?: ClientDto;
  details: ProformaDetailDto[];
  subtotalZero: number;
  subtotalIva: number;
  subtotalIce: number;
  totalDiscount: number;
  totalAmount: number;
}

/** Item del listado paginado */
export interface ProformaListResponseDto {
  idProforma: number;
  proformaNumber: string;
  issueDate: string; // yyyy-MM-dd
  validUntil: string; // yyyy-MM-dd
  status: ProformaStatus;
  idInvoice: number | null;
  totalAmount: number;
  idClient: number;
  clientIdentification: string;
  clientName: string;
}

/** Response de convert-to-invoice */
export interface ProformaConvertToInvoiceResponseDto {
  idInvoice: number;
  invoiceNumber: string;
  invoiceDto?: unknown; // Opcional, backend puede no poblarlo
}
