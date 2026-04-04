import { ProductDto } from '../product/product-dto';
import { CarrierDto } from './carrier-dto';

/**
 * DTOs para Guías de Remisión
 */

export interface GuideDto {
  // Campos generados automáticamente
  idGuide?: number;
  idCompany?: number;
  idClient?: number;
  idCarrier?: number;
  idEstablishment?: number;
  idIssuancePoint?: number;
  sequential?: number;
  creationDate?: string;
  accessKey?: string;
  guideNumber?: string;
  authorizationNumber?: string;
  authorizationDate?: string;
  status?: string;
  environment?: string;
  emailSent?: boolean;
  emailSentDate?: string;

  // Campos obligatorios
  companyDto: CompanyDto;
  clientDto: ClientDto;
  carrierDto: CarrierDto;
  issueDate: string; // yyyy-MM-dd
  startDate: string; // yyyy-MM-dd
  endDate: string; // yyyy-MM-dd
  originAddress: string;
  destinationAddress: string;
  guideDetails: GuideDetailDto[];

  // Campos opcionales
  establishment?: EstablishmentDto;
  transportType?: string;
  transportPlate?: string;
  reasonForTransfer?: string;
  route?: string;
  additionalInformation?: string;
  documentReferences?: GuideDocumentReferenceDto[];
}

export interface GuideDetailDto {
  idGuideDetail?: number;
  idGuide?: number;
  idProduct: number;
  productDto?: ProductDto;
  quantity: number;
  description?: string;
  unit?: string;
  weight?: number;
  additionalInfo?: string;
}

export interface GuideDocumentReferenceDto {
  idGuideDocumentReference?: number;
  documentType: string; // "01" = Factura, "04" = Nota de Crédito
  documentNumber: string;
  accessKey?: string;
  issueDate?: string;
}

export interface GuideListResponseDto {
  idGuide: number;
  idCompany: number;
  idClient: number;
  idCarrier: number;
  guideNumber: string;
  issueDate: string;
  startDate: string;
  endDate: string;
  originAddress: string;
  destinationAddress: string;
  transportPlate: string;
  transportType: string;
  reasonForTransfer: string;
  route?: string;
  buyerName: string;
  buyerIdentification: string;
  carrierName: string;
  carrierIdentification: string;
  guideStatus: string; // "BORRADOR", "AUTORIZADO", "NO_AUTORIZADO"
  authorizationStatusSri?: string;
  authorizationSri?: AuthorizationSriDto;
  carrier?: CarrierDto;
  emailSent: boolean;
  emailSentDate?: string;
  creationDate: string;
}

export interface AuthorizationSriDto {
  idAuthorizationSri?: number;
  accessKey?: string;
  authorizationNumber?: string;
  authorizationDate?: string;
  authorizationStatusSri?: string;
  receptionStatusSri?: string;
  authorizationError?: string;
  receptionXml?: string;
  authorizationXml?: string;
  authorizationStoragePath?: string;
  status?: boolean;
  creationDate?: string;
  updateDate?: string;
}

export interface GuideResponse {
  code: number;
  status: string;
  message?: string;
  guideDto?: GuideDto;
  guideDtos?: GuideDto[];
  idGuide?: number;
  guideNumber?: string;
  accessKey?: string;
  authorizationNumber?: string;
  authorizationDate?: string;
  receptionStatusSri?: string;
  authorizationStatusSri?: string;
  storagePath?: string;
}

// DTOs compartidos
export interface CompanyDto {
  idCompany: number;
  name: string;
  nameCommercial?: string;
  identification: string;
  idType?: number;
  forcedToAccounting?: boolean;
  regime?: string;
  specialTaxpayerResolution?: string;
  status?: boolean;
  logoPath?: string;
  contacts?: ContactDto[];
}

export interface ClientDto {
  idClient: number;
  name: string;
  identification: string;
  idType: number;
  contacts?: ContactDto[];
}

export interface ContactDto {
  contactType: string;
  value: string;
  status?: boolean;
}

export interface EstablishmentDto {
  idEstablishment: number;
  name?: string;
  code: string;
  type?: string; // "MATRIZ" o "SUCURSAL"
  address: string;
  issuancePoint: IssuancePointDto;
}

export interface IssuancePointDto {
  idIssuancePoint: number;
  code: string;
  sequentialInvoice?: string;
}

/**
 * DTO para búsqueda de guías por compañía
 */
export interface GuideSearchByCompanyDto {
  idCompany: number;
  guideNumber?: string;
}

/**
 * DTO de respuesta para búsqueda de guías
 */
export interface GuideSearchResponseDto {
  idGuide: number;
  guideNumber: string;
  createDate: string;
  destinatarioNombre: string;
  carrierIdentification: string;
  carrierName: string;
  authorizationStatus: string;
}

