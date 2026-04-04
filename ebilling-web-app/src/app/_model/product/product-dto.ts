import { TaxTypeDto } from './tax-type-dto';

export interface ProductDto {
  idProduct?: number; // Opcional para productos nuevos
  idCompany?: number;
  mainCode: string;
  name: string;
  productType?: number;
  unitPrice: number;
  description?: string;
  tourismIva: boolean;
  ice: boolean; // Flag UI: true = muestra combo ICE
  status?: boolean;
  ivaTaxType: TaxTypeDto; // Obligatorio - IVA del producto
  iceTaxType?: TaxTypeDto | null; // Opcional - ICE del producto (solo si ice=true)
}
