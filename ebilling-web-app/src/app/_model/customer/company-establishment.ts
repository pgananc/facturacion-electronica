/**
 * Modelos relacionados a la estructura de compañías y sus establecimientos
 * utilizados tanto en la extracción desde el token JWT como en la respuesta
 * del backend (ebill-customer) para mostrar detalles completos.
 */

export interface CompanyEstablishmentClaim {
  idCompany: number;
  establishments: number[];
}

export interface Establishment {
  idEstablishment: number;
  name: string;
  code: string;
  address: string;
  idCompany: number;
  type: string;
  status: boolean;
}

export interface Company {
  idCompany: number;
  idType: number;
  identification: string;
  name: string;
  nameCommercial: string;
  forcedToAccounting: string;
  specialTaxpayer: number;
  regime?: string; // Código del régimen: GENERAL, RIMPE_POPULAR, RIMPE_STARTUP, SPECIAL
  specialTaxpayerResolution?: string; // Resolución del contribuyente especial (solo si regime es SPECIAL)
  status: boolean;
  establishments: Establishment[];
  contacts?: Contact[];
}

export interface Contact {
  contactType: string;
  value: string;
  status: boolean;
}
