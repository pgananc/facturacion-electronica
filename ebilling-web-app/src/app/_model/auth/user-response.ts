import { BasicEntity } from '../basic-entity';
import { User } from './user';

export interface UserResponse extends BasicEntity {
  userDtos: User[];
  userDto: User;
}
