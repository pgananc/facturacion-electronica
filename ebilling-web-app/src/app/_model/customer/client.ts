import {Contact} from './contact';

export class Client {
  idClient = 0;
  idType = 0;
  idTypeEnum:any;
  identification = '';
  name: string = '';
  clientType = 0;
  clientTypeEnum:any;
  status = true;
  contacts: Contact[] = [];
}
