import { Role } from './role';
export class User {
  idUser: Number = 0;
  name: String = '';
  userName: String = '';
  password: String = '';
  mail: String = '';
  status: boolean = true;
  roleDtos: Role[] = [];
}
