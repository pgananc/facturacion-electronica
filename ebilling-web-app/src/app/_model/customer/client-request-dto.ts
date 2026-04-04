export interface ClientRequestDto {
  identification: string;
  name: string;
  clientType: number;
  idType?: number; // ✅ Tipo de identificación (1=Cédula, 2=RUC, 3=Pasaporte, 4=Consumidor Final, 5=Exterior)
  status: boolean;
  companyIdentification: string;
}
