import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { ProductCacheService } from './product-cache.service';
import { ProductService } from './product.service';
import { LoggerService } from '../logger/logger.service';
import { environment } from '../../../environments/environment';
import { ProductDto } from '../../_model/product/product-dto';
import { ProductResponse } from '../../_model/product/product-response';

describe('ProductCacheService', () => {
  let service: ProductCacheService;
  let productService: ProductService;
  let httpMock: HttpTestingController;
  let logger: jasmine.SpyObj<LoggerService>;
  const baseUrl = `${environment.HOST_PRODUCTS}/api/product`;

  beforeEach(() => {
    const loggerSpy = jasmine.createSpyObj('LoggerService', [
      'debug',
      'warn',
      'error',
    ]);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        ProductCacheService,
        ProductService,
        { provide: LoggerService, useValue: loggerSpy },
      ],
    });

    service = TestBed.inject(ProductCacheService);
    productService = TestBed.inject(ProductService);
    httpMock = TestBed.inject(HttpTestingController);
    logger = TestBed.inject(LoggerService) as jasmine.SpyObj<LoggerService>;

    // Limpiar localStorage antes de cada test
    localStorage.clear();
  });

  afterEach(() => {
    if (httpMock) {
      httpMock.verify();
    }
    localStorage.clear();
    service.clearCache();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getProducts', () => {
    it('should return empty array when cache is not loaded', () => {
      const products = service.getProducts();
      expect(products).toEqual([]);
    });

    it('should return products from cache when loaded', () => {
      localStorage.setItem(environment.ID_COMPANY, '1');
      const mockResponse = {
        content: [
          {
            idProduct: 1,
            mainCode: 'PROD001',
            name: 'Product 1',
            productType: 1,
            unitPrice: 10.0,
            status: true,
          } as ProductDto,
        ],
        totalElements: 1,
      };

      service.loadProducts().subscribe();
      const req = httpMock.expectOne(
        (request) =>
          request.url === `${baseUrl}/pageable` &&
          request.params.get('page') === '0' &&
          request.params.get('size') === '10000'
      );
      req.flush(mockResponse);

      const products = service.getProducts();
      expect(products.length).toBe(1);
      expect(products[0].name).toBe('Product 1');
    });
  });

  describe('getProductById', () => {
    it('should return product by id from cache', () => {
      localStorage.setItem(environment.ID_COMPANY, '1');
      const mockResponse = {
        content: [
          {
            idProduct: 1,
            mainCode: 'PROD001',
            name: 'Product 1',
            productType: 1,
            unitPrice: 10.0,
            status: true,
          } as ProductDto,
        ],
        totalElements: 1,
      };

      service.loadProducts().subscribe();
      const req = httpMock.expectOne(
        (request) =>
          request.url === `${baseUrl}/pageable` &&
          request.params.get('page') === '0'
      );
      req.flush(mockResponse);

      const product = service.getProductById(1);
      expect(product).toBeTruthy();
      expect(product?.name).toBe('Product 1');
    });

    it('should return undefined if product not found', () => {
      const product = service.getProductById(999);
      expect(product).toBeUndefined();
    });
  });

  describe('getProductByCode', () => {
    it('should return product by code from cache', () => {
      localStorage.setItem(environment.ID_COMPANY, '1');
      const mockResponse = {
        content: [
          {
            idProduct: 1,
            mainCode: 'PROD001',
            name: 'Product 1',
            productType: 1,
            unitPrice: 10.0,
            status: true,
          } as ProductDto,
        ],
        totalElements: 1,
      };

      service.loadProducts().subscribe();
      const req = httpMock.expectOne(
        (request) =>
          request.url === `${baseUrl}/pageable` &&
          request.params.get('page') === '0'
      );
      req.flush(mockResponse);

      const product = service.getProductByCode('PROD001');
      expect(product).toBeTruthy();
      expect(product?.name).toBe('Product 1');
    });
  });

  describe('loadProducts', () => {
    it('should load products from backend', () => {
      localStorage.setItem(environment.ID_COMPANY, '1');
      const mockResponse = {
        content: [
          {
            idProduct: 1,
            mainCode: 'PROD001',
            name: 'Product 1',
            productType: 1,
            unitPrice: 10.0,
            status: true,
          } as ProductDto,
        ],
        totalElements: 1,
      };

      service.loadProducts().subscribe((products) => {
        expect(products.length).toBe(1);
      });

      const req = httpMock.expectOne(
        (request) =>
          request.url === `${baseUrl}/pageable` &&
          request.params.get('page') === '0' &&
          request.params.get('size') === '10000'
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });

    it('should return empty array if no company ID', () => {
      localStorage.removeItem(environment.ID_COMPANY);

      service.loadProducts().subscribe((products) => {
        expect(products).toEqual([]);
      });

      httpMock.expectNone(baseUrl);
    });

    it('should clear cache when company changes', () => {
      localStorage.setItem(environment.ID_COMPANY, '1');
      const mockResponse1 = {
        content: [
          {
            idProduct: 1,
            mainCode: 'PROD001',
            name: 'Product 1',
            productType: 1,
            unitPrice: 10.0,
            status: true,
          } as ProductDto,
        ],
        totalElements: 1,
      };

      // Primera carga
      service.loadProducts().subscribe();
      const req1 = httpMock.expectOne(
        (request) =>
          request.url === `${baseUrl}/pageable` &&
          request.params.get('page') === '0'
      );
      req1.flush(mockResponse1);

      // Cambiar compañía
      localStorage.setItem(environment.ID_COMPANY, '2');
      const mockResponse2 = {
        content: [],
        totalElements: 0,
      };

      service.loadProducts().subscribe();
      const req2 = httpMock.expectOne(
        (request) =>
          request.url === `${baseUrl}/pageable` &&
          request.params.get('page') === '0'
      );
      req2.flush(mockResponse2);
    });
  });

  describe('refreshCache', () => {
    it('should reload products from backend', () => {
      localStorage.setItem(environment.ID_COMPANY, '1');
      const mockResponse = {
        content: [
          {
            idProduct: 1,
            mainCode: 'PROD001',
            name: 'Product 1',
            productType: 1,
            unitPrice: 10.0,
            status: true,
          } as ProductDto,
        ],
        totalElements: 1,
      };

      service.refreshCache().subscribe((products) => {
        expect(products.length).toBe(1);
      });

      const req = httpMock.expectOne(
        (request) =>
          request.url === `${baseUrl}/pageable` &&
          request.params.get('page') === '0'
      );
      req.flush(mockResponse);
    });
  });

  describe('clearCache', () => {
    it('should clear the cache', () => {
      localStorage.setItem(environment.ID_COMPANY, '1');
      const mockResponse = {
        content: [
          {
            idProduct: 1,
            mainCode: 'PROD001',
            name: 'Product 1',
            productType: 1,
            unitPrice: 10.0,
            status: true,
          } as ProductDto,
        ],
        totalElements: 1,
      };

      service.loadProducts().subscribe();
      const req = httpMock.expectOne(
        (request) =>
          request.url === `${baseUrl}/pageable` &&
          request.params.get('page') === '0'
      );
      req.flush(mockResponse);

      expect(service.isCacheReady()).toBe(true);

      service.clearCache();

      expect(service.isCacheReady()).toBe(false);
      expect(service.getProducts()).toEqual([]);
    });
  });

  describe('upsertProduct', () => {
    it('should add new product to cache', () => {
      const product: ProductDto = {
        idProduct: 1,
        mainCode: 'PROD001',
        name: 'New Product',
        productType: 1,
        unitPrice: 10.0,
        status: true,
        tourismIva: false,
        ice: false,
        ivaTaxType: { idTaxType: 1, type: '2', name: 'IVA', value: 0 },
      };

      service.upsertProduct(product);

      const cached = service.getProductById(1);
      expect(cached).toBeTruthy();
      expect(cached?.name).toBe('New Product');
    });

    it('should update existing product in cache', () => {
      const product1: ProductDto = {
        idProduct: 1,
        mainCode: 'PROD001',
        name: 'Product 1',
        productType: 1,
        unitPrice: 10.0,
        status: true,
        tourismIva: false,
        ice: false,
        ivaTaxType: { idTaxType: 1, type: '2', name: 'IVA', value: 0 },
      };

      service.upsertProduct(product1);

      const product2: ProductDto = {
        ...product1,
        name: 'Updated Product',
      };

      service.upsertProduct(product2);

      const cached = service.getProductById(1);
      expect(cached?.name).toBe('Updated Product');
    });
  });

  describe('removeProduct', () => {
    it('should remove product from cache', () => {
      const product: ProductDto = {
        idProduct: 1,
        mainCode: 'PROD001',
        name: 'Product 1',
        productType: 1,
        unitPrice: 10.0,
        status: true,
        tourismIva: false,
        ice: false,
        ivaTaxType: { idTaxType: 1, type: '2', name: 'IVA', value: 0 },
      };

      service.upsertProduct(product);
      expect(service.getProductById(1)).toBeTruthy();

      service.removeProduct(1);
      expect(service.getProductById(1)).toBeUndefined();
    });
  });

  describe('getProductByIdWithFallback', () => {
    it('should return product from cache if found', (done) => {
      const product: ProductDto = {
        idProduct: 1,
        mainCode: 'PROD001',
        name: 'Product 1',
        productType: 1,
        unitPrice: 10.0,
        status: true,
        tourismIva: false,
        ice: false,
        ivaTaxType: { idTaxType: 1, type: '2', name: 'IVA', value: 0 },
      };

      service.upsertProduct(product);

      service.getProductByIdWithFallback(1).subscribe((result) => {
        expect(result).toBeTruthy();
        expect(result?.name).toBe('Product 1');
        done();
      });
    });

    it('should fetch from backend if not in cache', () => {
      const mockResponse: ProductResponse = {
        code: 200,
        status: 'success',
        message: 'OK',
        productDtos: [
          {
            idProduct: 1,
            mainCode: 'PROD001',
            name: 'Product 1',
            productType: 1,
            unitPrice: 10.0,
            status: true,
          } as ProductDto,
        ],
      };

      service.getProductByIdWithFallback(1).subscribe((result) => {
        expect(result).toBeTruthy();
        expect(result?.name).toBe('Product 1');
      });

      const req = httpMock.expectOne(`${baseUrl}/id/1`);
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });
  });
});
