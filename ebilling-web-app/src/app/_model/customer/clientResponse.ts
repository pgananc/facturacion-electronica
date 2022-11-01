import { BasicEntity } from '../basicEntity';
import { Client } from './client';
export class ClientResponse extends BasicEntity {
  clientDtos: Client[] = [];
}
