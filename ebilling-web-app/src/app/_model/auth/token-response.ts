import { BasicEntity } from '../basic-entity';
import { Token } from './token';

export interface TokenResponse extends BasicEntity {
  token: Token;
}
