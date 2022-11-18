import { BasicEntity } from '../basicEntity';
import { Role } from './role';
export class RoleResponse extends BasicEntity {
  roleDtos: Role[] = [];
}
