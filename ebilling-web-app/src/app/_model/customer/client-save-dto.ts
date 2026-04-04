import { Contact } from './contact';

/**
 * DTO for saving/updating client data
 * Used for POST and PATCH operations
 */
export interface ClientSaveDto {
  idClient?: number; // Only for updates
  idType: number;
  clientType: number;
  identification: string;
  name: string;
  status: boolean;
  companyIdentification: string;
  contacts: Contact[];
  idCompany?: number; // Only for updates when present
}
