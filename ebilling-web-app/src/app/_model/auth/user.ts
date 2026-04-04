import { Role } from './role';

export interface User {
  idUser: number;
  name: string;
  userName: string;
  password: string;
  mail: string;
  status: boolean;
  idCompany: number;
  roleDtos: Role[];
  nameRole?: string;
}
