import { BasicEntity } from '../basic-entity';
import { ProductDto } from './product-dto';

export interface ProductResponse extends BasicEntity {
  productDtos: ProductDto[];
}
