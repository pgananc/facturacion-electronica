import { Contact } from './contact';

export interface Client {
  idClient: number;
  idType: number;
  idTypeEnum?: any;
  identification: string;
  name: string;
  clientType: number;
  clientTypeEnum?: any;
  status: boolean;
  idCompany?: number;
  companyIdentification: string;
  contacts: Contact[];
  mail?: string;
  address?: string;
  phone?: string;
  mobil?: string;
}
