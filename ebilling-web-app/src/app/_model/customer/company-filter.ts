export interface CompanyFilter {
  idCompany: number | null;
  idType: number;
  identification: string;
  name: string;
  nameCommercial: string;
  type?: string; // "MATRIZ" o "SUCURSAL" (opcional)
  branchOfficeCode?: string;
  status: boolean;
  forcedToAccounting: string;
  regime?: string; // Código del régimen: GENERAL, RIMPE_POPULAR, RIMPE_STARTUP, SPECIAL
  specialTaxpayerResolution?: string; // Resolución del contribuyente especial (solo si regime es SPECIAL)
  logoPath?: string; // Ruta del logo de la compañía en el servidor
  principal: string;
  contacts?: any[];
  isSuperAdmin: boolean;
  companiesIds?: any[];
  idTypeEnum?: any;
}

// Opciones de régimen tributario
export interface TaxRegimeOption {
  label: string;
  code: string;
}

export const TAX_REGIME_OPTIONS: TaxRegimeOption[] = [
  { label: 'General', code: 'GENERAL' },
  { label: 'RIMPE Negocio Popular', code: 'RIMPE_POPULAR' },
  { label: 'RIMPE Emprendedor', code: 'RIMPE_STARTUP' },
  { label: 'Especial', code: 'SPECIAL' },
];
