import { BasicEntity } from '../basicEntity';
import { Contact } from './contact';
export class Client {
  idClient: number = 0;
  idType: number = 0;
  identification: string = '';
  name: string = '';
  clientType: number = 0;
  status: boolean = true;
  idCompany: number = 0;
  contacts: Contact[] = [];
}
