import { Role } from './role';
export class User {
  idUser = 0;
  name = '';
  userName = '';
  password = '';
  mail = '';
  status = true;
  idCompany = 0;
  roleDtos: Role[] = [];
}
