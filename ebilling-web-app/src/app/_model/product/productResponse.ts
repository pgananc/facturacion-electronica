import {BasicEntity} from '../basicEntity';
import {Product} from "./product";

export class ProductResponse extends BasicEntity {
  productDtos: Product[] = [];
}
