import { BasicEntity } from '../basic-entity';
import { Client } from '../customer/client';
import { InvoiceDto } from './invoice-dto';
export interface InvoiceSearchResponseDto extends BasicEntity {
  invoiceDtos: InvoiceDto[];
}
