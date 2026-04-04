import { environment } from '../../../environments/environment';
import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { map } from 'rxjs/operators';
import { HttpClient, HttpParams } from '@angular/common/http';
import { ProductDto } from '../../_model/product/product-dto';
import { ProductResponse } from '../../_model/product/product-response';

@Injectable({
  providedIn: 'root',
})
export class ProductService {
  productChange = new Subject<ProductDto[]>();
  messageChange = new Subject<string>();
  url: string = `${environment.HOST_PRODUCTS}/api/product`;

  constructor(private readonly http: HttpClient) {}

  /**
   * Crear un nuevo producto
   * POST /api/product
   */
  save(product: ProductDto) {
    return this.http.post(this.url, product);
  }

  /**
   * Actualizar un producto existente
   * PATCH /api/product/{idProduct}
   */
  update(product: ProductDto, id: number) {
    return this.http.patch(`${this.url}/${id}`, product);
  }

  /**
   * Obtener productos por compañía
   * GET /api/product/company/{idCompany}
   */
  findByIdCompany(idCompany: number) {
    return this.http.get<ProductResponse>(`${this.url}/company/${idCompany}`);
  }

  /**
   * Obtener producto por ID
   * GET /api/product/id/{idProduct}
   */
  findById(id: number) {
    return this.http.get<ProductResponse>(`${this.url}/id/${id}`);
  }

  /**
   * Eliminar producto (Soft Delete - cambia status a false)
   * DELETE /api/product/{idProduct}
   *
   * Esta acción desactiva el producto (borrado lógico).
   * El producto sigue existiendo en la base de datos pero con status = false
   */
  delete(idProduct: number) {
    return this.http.delete(`${this.url}/${idProduct}`);
  }

  /**
   * Eliminar producto PERMANENTEMENTE (Hard Delete)
   * DELETE /api/product/{idProduct}/permanent
   *
   * ⚠️ Esta acción elimina FÍSICAMENTE el producto de la base de datos.
   * NO se puede deshacer.
   */
  deletePhysical(idProduct: number) {
    return this.http.delete(`${this.url}/${idProduct}/permanent`);
  }

  /**
   * Buscar productos (método legacy - considerar deprecar)
   * POST /api/product/search
   */
  findClientByIdentificationOrNameOrType(product: ProductDto) {
    return this.http.post<ProductResponse>(`${this.url}/search`, product);
  }

  /**
   * Listar productos con paginación y filtros
   * GET /api/product/pageable?page=0&size=10&idCompany=1&status=true
   *
   * @param filters.status - true (activos), false (inactivos), null/undefined (todos)
   */
  searchPageable(
    page: number,
    size: number,
    filters?: {
      idCompany?: number;
      mainCode?: string;
      name?: string;
      productType?: number;
      status?: boolean | null; // null = todos
    }
  ) {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (filters) {
      if (filters.idCompany !== undefined) {
        params = params.set('idCompany', filters.idCompany.toString());
      }
      if (filters.mainCode) {
        params = params.set('mainCode', filters.mainCode);
      }
      if (filters.name) {
        params = params.set('name', filters.name);
      }
      if (filters.productType !== undefined && filters.productType !== 0) {
        params = params.set('productType', filters.productType.toString());
      }
      // Solo enviar status si NO es null (null = traer todos)
      if (filters.status !== undefined && filters.status !== null) {
        params = params.set('status', filters.status.toString());
      }
    }

    return this.http.get<any>(`${this.url}/pageable`, { params });
  }

  /**
   * Verificar si existe un código principal para una compañía específica
   * GET /api/product/exists/{mainCode}/company/{idCompany}
   * 
   * @param mainCode - Código principal del producto a verificar
   * @param idCompany - ID de la compañía del usuario logueado
   */
  existsByMainCode(mainCode: string, idCompany: number) {
    return this.http.get<boolean>(`${this.url}/exists/${mainCode}/company/${idCompany}`);
  }

  /**
   * Buscar productos por código o descripción
   * GET /api/product/search?term={searchTerm}
   */
  searchByCodeOrDescription(term: string): Observable<ProductDto[]> {
    return this.http.get<ProductDto[]>(
      `${this.url}/search?term=${encodeURIComponent(term)}`
    );
  }

  /**
   * Obtener productos por estado
   * GET /api/product/products-status/{status}
   */
  findByStatus(status: boolean): Observable<ProductDto[]> {
    return this.http.get<ProductDto[]>(`${this.url}/products-status/${status}`);
  }

  /**
   * Buscar productos activos de una compañía específica para facturación
   * GET /api/product/billing/search?idCompany={id}&term={term}
   *
   * Endpoint específico para billing que:
   * - Solo retorna productos activos (status = true)
   * - Filtra por compañía
   * - Ordena por código principal (mainCode ASC)
   * - Si term está vacío, retorna TODOS los productos activos de la compañía
   * - Si term tiene valor, busca en mainCode, name o description (case-insensitive)
   *
   * @param term - Término de búsqueda (código, nombre o descripción). Vacío = todos
   * @param idCompany - ID de la compañía (requerido)
   */
  searchActiveProductsForBilling(
    term: string = '',
    idCompany: number
  ): Observable<ProductDto[]> {
    return this.http
      .get<ProductResponse>(
        `${
          this.url
        }/billing/search?idCompany=${idCompany}&term=${encodeURIComponent(
          term
        )}`
      )
      .pipe(
        map((response: ProductResponse) => {
          // El backend retorna ProductResponse con productDtos array
          return response.productDtos || [];
        })
      );
  }

  /**
   * Buscar productos activos de una compañía (sin término de búsqueda)
   * GET /api/product/company/{idCompany}
   *
   * Útil para cargar todos los productos activos en el modal
   */
  findActiveProductsByCompany(idCompany: number): Observable<ProductDto[]> {
    return this.http.get<ProductDto[]>(`${this.url}/company/${idCompany}`);
  }
}
