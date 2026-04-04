import { BasicEntity } from '../basic-entity';
import { Role } from './role';
export interface RoleResponse extends BasicEntity {
  roleDtos: Role[];
}
