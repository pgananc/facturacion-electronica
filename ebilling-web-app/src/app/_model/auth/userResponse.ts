import {BasicEntity} from '../basicEntity';
import {User} from './user';

export class UserResponse extends BasicEntity {
  userDtos: User[] = [];
}
