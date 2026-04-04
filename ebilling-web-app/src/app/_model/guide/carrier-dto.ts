/**
 * DTOs para Transportistas
 */

export interface CarrierDto {
  idCarrier?: number;
  idCompany: number;
  identificationType: string; // "1" = Cédula, "2" = RUC, "3" = Pasaporte
  identification: string;
  name: string;
  address?: string;
  phone?: string;
  email?: string;
  licenseNumber?: string;
  vehicleType?: string;
  status?: boolean;
}

