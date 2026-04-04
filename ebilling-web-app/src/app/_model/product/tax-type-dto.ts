export interface TaxTypeDto {
  idTaxType: number;
  type: string;
  name: string;
  value: number; // BigDecimal → number en TypeScript
}
