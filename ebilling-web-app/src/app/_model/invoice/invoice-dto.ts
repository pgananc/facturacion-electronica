/**
 * DTOs para el backend de facturación (ebill-billing)
 * Basados en los DTOs de Java del backend
 */

export interface InvoiceDto {
  idInvoice?: number;
  idCompany: number;
  companyDto?: CompanyDto;
  clientDto: ClientDto;
  idClient?: number; // ID del cliente en la BD (si ya existe)
  // paymentMethod e idPaymentMethod eliminados - ya no se requieren
  paymentMethod?: PaymentMethodDto; // Opcional solo para compatibilidad con borradores antiguos
  idPaymentMethod?: number; // Opcional solo para compatibilidad con borradores antiguos
  creationDate?: string; // ISO 8601
  issueDate: string; // ISO 8601
  accessKey?: string; // Generado por el SRI
  invoiceNumber?: string; // Generado por el backend
  environment?: string;
  authorizationNumber?: string; // Generado por el SRI
  authorizationSri?: string; // Autorización del SRI (null = borrador)
  additionalInformation?: string;
  authorizationDate?: string; // ISO 8601
  totalAmount: number;
  discount: number;
  tip: number;
  subtotalZero: number;
  subtotalIva: number;
  subtotalIce: number;
  subtotalDiscount: number;
  status?: string;
  invoiceDetails: InvoiceDetailDto[];
  details?: InvoiceDetailDto[]; // Alias para invoiceDetails (usado en algunos endpoints)
  payments?: PaymentDto[]; // Array de formas de pago
  establishment: EstablishmentDto;
  password?: string; // Password de la firma digital
  receptionStatusSri?: string;
  authorizationStatusSri?: string;

  // Campos de estado de pago (para cuentas por cobrar)
  paymentStatus?: string; // 'PAID' | 'PENDING' | 'OVERDUE'
  amountPaid?: number; // Monto pagado hasta el momento

  // Campos adicionales del cliente para facilitar la carga de borradores
  buyerIdentification?: string;
  buyerIdType?: number;
  buyerName?: string;
  buyerAddress?: string;
  buyerPhone?: string;
  buyerMail?: string;
}

export interface InvoiceDetailDto {
  idInvoiceDetail?: number;
  idProduct: number;
  productDto?: ProductDtoSimple;
  unitPrice: number;
  discount: number;
  subtotal: number;
  quantity: number;

  // Campos adicionales para facilitar la carga de borradores
  code?: string;
  description?: string;
  ivaPercentage?: number;
}

export interface PaymentDto {
  idPayment?: number;
  idPaymentMethod?: number;
  paymentMethod?: string; // Nombre de la forma de pago
  paymentType?: string; // Tipo de forma de pago
  paymentMethodCode?: string; // Código de la forma de pago
  total?: number; // Monto de esta forma de pago
  value?: number; // Alias para total
  term?: number;
  timeUnit?: string;
  time?: string; // Alias para timeUnit
}

export interface CompanyDto {
  idCompany: number;
  name: string;
  identification: string;
  // Agregar otros campos si son necesarios
}

export interface PaymentMethodDto {
  idPaymentMethod?: number;
  paymentType: string;
  value: number;
  term?: number;
  timeUnit?: string;
}

export interface ClientDto {
  idClient?: number;
  identification: string;
  idType: number;
  name: string;
  address?: string;
  phone?: string;
  email: string;
}

export interface EstablishmentDto {
  idEstablishment: number;
  code: string;
  address?: string;
}

export interface ProductDtoSimple {
  idProduct: number;
  name: string;
  description?: string;
  code?: string; // Código principal del producto
  unitPrice: number;
}

/**
 * DTO para búsqueda de facturas para crear nota de débito
 */
export interface InvoiceSearchForDebitNoteDto {
  /** ID de la compañía (obligatorio) */
  idCompany: number;
  /** Número de autorización (opcional) */
  numeroAutorizacion?: string;
  /** Número de factura (opcional) */
  numeroFactura?: string;
  /** Nombre del cliente (opcional) */
  nombreCliente?: string;
  /** Identificación del cliente (opcional) */
  identificacionCliente?: string;
}

/**
 * DTO para búsqueda de facturas para crear nota de crédito
 */
export interface InvoiceSearchForCreditNoteDto {
  /** ID de la compañía (obligatorio) */
  idCompany: number;
  /** Número de autorización (opcional) */
  numeroAutorizacion?: string;
  /** Número de factura (opcional) */
  numeroFactura?: string;
  /** Nombre del cliente (opcional) */
  nombreCliente?: string;
  /** Identificación del cliente (opcional) */
  identificacionCliente?: string;
}

/**
 * DTO de respuesta para facturas listadas para crear nota de débito
 */
export interface InvoiceForDebitNoteResponseDto {
  idInvoice?: number;
  invoiceNumber?: string;
  sequential?: number;
  issueDate?: string; // YYYY-MM-DD
  creationDate?: string; // ISO 8601
  totalAmount?: number;
  tip?: number;
  availableAmount?: number;
  idCompany?: number;
  idIssuancePoint?: number;
  accessKey?: string;
  authorizationSri?: AuthorizationSriDto;
  clientDto?: ClientDto;
  buyerName?: string;
  buyerIdentification?: string;
}

/**
 * DTO para autorización SRI
 */
export interface AuthorizationSriDto {
  idAuthorizationSri?: number;
  authorizationNumber?: string;
  accessKey?: string;
  authorizationStatusSri?: string;
  authorizationDate?: string; // ISO 8601
  receptionStatusSri?: string; // Estado de recepción del SRI (RECIBIDA, DEVUELTA, etc.)
  receptionError?: string; // Mensaje de error si fue devuelta
  receptionXml?: string; // XML de respuesta de recepción
}