import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { tap, catchError, map } from 'rxjs/operators';
import { ProductDto } from '../../_model/product/product-dto';
import { ProductService } from './product.service';
import { LoggerService } from '../logger/logger.service';
import { environment } from '../../../environments/environment';

/**
 * ProductCacheService - Servicio de caché de productos en memoria
 *
 * Este servicio mantiene una copia en memoria de todos los productos de la compañía
 * para evitar llamadas repetidas al backend. Se actualiza automáticamente cuando:
 * - Se crea un nuevo producto
 * - Se actualiza un producto existente
 * - Se elimina un producto
 * - Se cambia de compañía
 *
 * Uso:
 * 1. Inyectar el servicio en el componente
 * 2. Llamar a loadProducts() al inicio (opcional, se carga automáticamente)
 * 3. Usar getProductById(id) para obtener un producto por ID
 * 4. Llamar a refreshCache() después de crear/editar/eliminar productos
 */
@Injectable({
  providedIn: 'root',
})
export class ProductCacheService {
  // Cache de productos en memoria
  private productsCache$ = new BehaviorSubject<ProductDto[]>([]);

  // Indica si el cache está cargado
  private isCacheLoaded = false;

  // ID de la compañía actual (para detectar cambios)
  private currentCompanyId: number | null = null;

  constructor(
    private readonly productService: ProductService,
    private readonly logger: LoggerService
  ) {
    this.logger.debug('🗄️ ProductCacheService inicializado');
  }

  /**
   * Obtiene los productos del cache como Observable
   * Si el cache no está cargado, lo carga automáticamente
   */
  getProducts$(): Observable<ProductDto[]> {
    if (!this.isCacheLoaded) {
      this.loadProducts();
    }
    return this.productsCache$.asObservable();
  }

  /**
   * Obtiene los productos del cache de forma síncrona
   * Retorna array vacío si el cache no está cargado
   */
  getProducts(): ProductDto[] {
    return this.productsCache$.value;
  }

  /**
   * Busca un producto por ID en el cache
   * Retorna undefined si no se encuentra
   */
  getProductById(idProduct: number): ProductDto | undefined {
    const products = this.productsCache$.value;
    const product = products.find((p) => p.idProduct === idProduct);

    if (!product) {
      this.logger.warn(
        `⚠️ Producto con ID ${idProduct} no encontrado en cache`
      );
    }

    return product;
  }

  /**
   * Busca un producto por ID en el cache, si no lo encuentra lo busca en el backend
   * Este método es ideal para cargar borradores donde el producto puede no estar en cache
   *
   * @param idProduct ID del producto a buscar
   * @returns Observable con el producto encontrado
   */
  getProductByIdWithFallback(idProduct: number): Observable<ProductDto | null> {
    // 1. Intentar buscar en cache primero
    const productInCache = this.getProductById(idProduct);

    if (productInCache) {
      this.logger.debug(`✅ Producto ${idProduct} encontrado en CACHE`);
      return of(productInCache);
    }

    // 2. Si no está en cache, buscar en el backend
    this.logger.debug(
      `🔍 Producto ${idProduct} NO en cache, buscando en backend...`
    );

    return this.productService.findById(idProduct).pipe(
      map((response) => {
        // Asumiendo que el backend retorna { productDtos: [...] }
        const product = response?.productDtos?.[0] || null;

        if (product) {
          this.logger.debug(
            `✅ Producto ${idProduct} encontrado en BACKEND:`,
            product
          );

          // Agregar al cache para futuras búsquedas
          this.upsertProduct(product);
          this.logger.debug(`💾 Producto ${idProduct} agregado al cache`);
        } else {
          this.logger.warn(`⚠️ Producto ${idProduct} NO encontrado en backend`);
        }

        return product;
      }),
      catchError((error) => {
        this.logger.error(
          `❌ Error al buscar producto ${idProduct} en backend`,
          error
        );
        return of(null);
      })
    );
  }

  /**
   * Busca un producto por código principal en el cache
   * Retorna undefined si no se encuentra
   */
  getProductByCode(mainCode: string): ProductDto | undefined {
    const products = this.productsCache$.value;
    return products.find((p) => p.mainCode === mainCode);
  }

  /**
   * Carga todos los productos de la compañía actual en el cache
   * Esta operación se ejecuta automáticamente al inicio o cuando se detecta
   * un cambio de compañía
   */
  loadProducts(): Observable<ProductDto[]> {
    const idCompany = this.getCompanyId();

    if (!idCompany) {
      this.logger.warn(
        '⚠️ No se puede cargar productos: ID de compañía no encontrado'
      );
      return of([]);
    }

    // Si cambió la compañía, limpiar el cache
    if (this.currentCompanyId !== null && this.currentCompanyId !== idCompany) {
      this.logger.debug('🔄 Compañía cambió, limpiando cache de productos');
      this.clearCache();
    }

    this.currentCompanyId = idCompany;

    this.logger.debug('📦 Cargando productos en cache...');

    // Cargar TODOS los productos activos de la compañía
    // Usamos un pageSize grande para obtener todos los productos
    return this.productService
      .searchPageable(0, 10000, {
        idCompany,
        status: true, // Solo productos activos
      })
      .pipe(
        map((response) => response.content || []),
        tap((products) => {
          this.productsCache$.next(products);
          this.isCacheLoaded = true;
          this.logger.debug(
            `✅ Cache de productos cargado: ${products.length} productos`
          );
        }),
        catchError((error) => {
          this.logger.error('❌ Error al cargar productos en cache', error);
          this.productsCache$.next([]);
          this.isCacheLoaded = false;
          return of([]);
        })
      );
  }

  /**
   * Refresca el cache cargando los productos nuevamente desde el backend
   * Útil después de crear, editar o eliminar productos
   */
  refreshCache(): Observable<ProductDto[]> {
    this.logger.debug('🔄 Refrescando cache de productos...');
    this.isCacheLoaded = false;
    return this.loadProducts();
  }

  /**
   * Limpia el cache de productos
   * Útil al cerrar sesión o cambiar de compañía
   */
  clearCache(): void {
    this.logger.debug('🗑️ Limpiando cache de productos');
    this.productsCache$.next([]);
    this.isCacheLoaded = false;
    this.currentCompanyId = null;
  }

  /**
   * Verifica si el cache está cargado
   */
  isCacheReady(): boolean {
    return this.isCacheLoaded && this.productsCache$.value.length > 0;
  }

  /**
   * Obtiene el ID de la compañía actual desde localStorage
   */
  private getCompanyId(): number | null {
    const idCompanyStr = localStorage.getItem(environment.ID_COMPANY);
    return idCompanyStr ? Number(idCompanyStr) : null;
  }

  /**
   * Agrega o actualiza un producto en el cache
   * Útil después de crear o editar un producto sin recargar todo el cache
   */
  upsertProduct(product: ProductDto): void {
    const products = this.productsCache$.value;
    const index = products.findIndex((p) => p.idProduct === product.idProduct);

    if (index >= 0) {
      // Actualizar producto existente
      products[index] = product;
      this.logger.debug(`✏️ Producto actualizado en cache: ${product.name}`);
    } else {
      // Agregar nuevo producto
      products.push(product);
      this.logger.debug(`➕ Producto agregado al cache: ${product.name}`);
    }

    this.productsCache$.next([...products]); // Crear nuevo array para trigger change detection
  }

  /**
   * Elimina un producto del cache
   * Útil después de eliminar un producto sin recargar todo el cache
   */
  removeProduct(idProduct: number): void {
    const products = this.productsCache$.value;
    const filtered = products.filter((p) => p.idProduct !== idProduct);

    if (filtered.length < products.length) {
      this.productsCache$.next(filtered);
      this.logger.debug(`🗑️ Producto eliminado del cache: ID ${idProduct}`);
    }
  }
}
