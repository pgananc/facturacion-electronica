import {environment} from './../../../environments/environment';
import {Injectable} from '@angular/core';
import {Subject} from 'rxjs';
import {HttpClient} from '@angular/common/http';
import {Product} from "../../_model/product/product";
import {ProductResponse} from "../../_model/product/productResponse";

@Injectable({
  providedIn: 'root',
})
export class ProductService {
  productChange = new Subject<Product[]>();
  messageChange = new Subject<string>();
  url: string = `${environment.HOST_PRODUCTS}/api/product`;
  constructor(private http: HttpClient) {}

  save(product: Product) {
    return this.http.post(this.url, product);
  }

  update(product: Product, id: Number) {
    return this.http.patch(`${this.url}/${id}`, product);
  }

  findAll() {
    return this.http.get<ProductResponse>(this.url);
  }

  findById(id: Number) {
    return this.http.get<ProductResponse>(`${this.url}/${id}`);
  }

  delete(idProduct: number) {
    return this.http.delete(`${this.url}/${idProduct}`);
  }

  findClientByIdentificationOrNameOrType(product: Product) {
    return this.http.post<ProductResponse>(`${this.url}/search`, product);
  }

  searchPageable(p: number, s: number, product: Product) {
    return this.http.post<any>(
      `${this.url}/pageable?page=${p}&size=${s}`,
      product
    );
  }

  existsByMainCode(mainCode: String) {
    return this.http.get<boolean>(`${this.url}/exist/${mainCode}`);
  }
}
