import { BasicEntity } from '../basic-entity';
import { Client } from './client';
export interface ClientResponse extends BasicEntity {
  clientDtos: Client[];
}
