import { BasicEntity } from '../basicEntity';
import { Role } from './role';
import { User } from './user';
export class UserResponse extends BasicEntity {
  userDtos: User[] = [];
}
