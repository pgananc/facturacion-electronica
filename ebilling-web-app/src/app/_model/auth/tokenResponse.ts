import { BasicEntity } from '../basicEntity';
import { Role } from './role';
import { Token } from './token';
export class TokenResponse extends BasicEntity {
  token: Token;
}
