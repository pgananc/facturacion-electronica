import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { ProductService } from './product.service';
import { environment } from '../../../environments/environment';
import { ProductDto } from '../../_model/product/product-dto';
import { ProductResponse } from '../../_model/product/product-response';

describe('ProductService', () => {
  let service: ProductService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.HOST_PRODUCTS}/api/product`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ProductService],
    });

    service = TestBed.inject(ProductService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    if (httpMock) {
      httpMock.verify();
    }
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('save', () => {
    it('should create a new product', () => {
      const product: ProductDto = {
        idProduct: undefined,
        mainCode: 'PROD001',
        name: 'Test Product',
        description: 'Test Description',
        productType: 1,
        unitPrice: 10.0,
        tourismIva: false,
        ice: false,
        status: true,
        ivaTaxType: { idTaxType: 1, type: '2', name: 'IVA', value: 0 },
      };

      service.save(product).subscribe((response) => {
        expect(response).toBeTruthy();
      });

      const req = httpMock.expectOne(baseUrl);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(product);
      req.flush({ idProduct: 1, ...product });
    });
  });

  describe('update', () => {
    it('should update an existing product', () => {
      const product: ProductDto = {
        idProduct: 1,
        mainCode: 'PROD001',
        name: 'Updated Product',
        description: 'Updated Description',
        productType: 1,
        unitPrice: 10.0,
        tourismIva: false,
        ice: false,
        status: true,
        ivaTaxType: { idTaxType: 1, type: '2', name: 'IVA', value: 0 },
      };

      service.update(product, 1).subscribe((response) => {
        expect(response).toBeTruthy();
      });

      const req = httpMock.expectOne(`${baseUrl}/1`);
      expect(req.request.method).toBe('PATCH');
      expect(req.request.body).toEqual(product);
      req.flush(product);
    });
  });

  describe('findByIdCompany', () => {
    it('should get products by company ID', () => {
      const mockResponse: ProductResponse = {
        code: 200,
        status: 'success',
        message: 'OK',
        productDtos: [
          {
            idProduct: 1,
            mainCode: 'PROD001',
            name: 'Product 1',
            description: 'Description 1',
            productType: 1,
            unitPrice: 10.0,
            tourismIva: false,
            ice: false,
            status: true,
            ivaTaxType: { idTaxType: 1, type: '2', name: 'IVA', value: 0 },
          },
        ],
      };

      service.findByIdCompany(1).subscribe((response) => {
        expect(response).toEqual(mockResponse);
        expect(response.productDtos.length).toBe(1);
      });

      const req = httpMock.expectOne(`${baseUrl}/company/1`);
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });
  });

  describe('findById', () => {
    it('should get product by ID', () => {
      const mockResponse: ProductResponse = {
        code: 200,
        status: 'success',
        message: 'OK',
        productDtos: [
          {
            idProduct: 1,
            mainCode: 'PROD001',
            name: 'Product 1',
            description: 'Description 1',
            productType: 1,
            unitPrice: 10.0,
            tourismIva: false,
            ice: false,
            status: true,
            ivaTaxType: { idTaxType: 1, type: '2', name: 'IVA', value: 0 },
          },
        ],
      };

      service.findById(1).subscribe((response) => {
        expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(`${baseUrl}/id/1`);
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });
  });

  describe('delete', () => {
    it('should delete a product (soft delete)', () => {
      service.delete(1).subscribe((response) => {
        expect(response).toBeTruthy();
      });

      const req = httpMock.expectOne(`${baseUrl}/1`);
      expect(req.request.method).toBe('DELETE');
      req.flush({});
    });
  });

  describe('deletePhysical', () => {
    it('should permanently delete a product', () => {
      service.deletePhysical(1).subscribe((response) => {
        expect(response).toBeTruthy();
      });

      const req = httpMock.expectOne(`${baseUrl}/1/permanent`);
      expect(req.request.method).toBe('DELETE');
      req.flush({});
    });
  });

  describe('searchPageable', () => {
    it('should search products with pagination', () => {
      const filters = {
        idCompany: 1,
        status: true,
      };

      service.searchPageable(0, 10, filters).subscribe((response) => {
        expect(response).toBeTruthy();
      });

      const req = httpMock.expectOne(
        (request) => request.url === `${baseUrl}/pageable`
      );
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('page')).toBe('0');
      expect(req.request.params.get('size')).toBe('10');
      expect(req.request.params.get('idCompany')).toBe('1');
      expect(req.request.params.get('status')).toBe('true');
      req.flush({ content: [], totalElements: 0 });
    });

    it('should search products without filters', () => {
      service.searchPageable(0, 10).subscribe((response) => {
        expect(response).toBeTruthy();
      });

      const req = httpMock.expectOne(
        (request) => request.url === `${baseUrl}/pageable`
      );
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('page')).toBe('0');
      expect(req.request.params.get('size')).toBe('10');
      req.flush({ content: [], totalElements: 0 });
    });
  });

  describe('existsByMainCode', () => {
    it('should check if main code exists for a company', () => {
      const idCompany = 1;
      service.existsByMainCode('PROD001', idCompany).subscribe((exists) => {
        expect(exists).toBe(true);
      });

      const req = httpMock.expectOne(`${baseUrl}/exists/PROD001/company/${idCompany}`);
      expect(req.request.method).toBe('GET');
      req.flush(true);
    });
  });

  describe('searchByCodeOrDescription', () => {
    it('should search products by code or description', () => {
      const mockProducts: ProductDto[] = [
        {
          idProduct: 1,
          mainCode: 'PROD001',
          name: 'Product 1',
          description: 'Description 1',
          productType: 1,
          unitPrice: 10.0,
          tourismIva: false,
          ice: false,
          status: true,
          ivaTaxType: { idTaxType: 1, type: '2', name: 'IVA', value: 0 },
        },
      ];

      service.searchByCodeOrDescription('PROD').subscribe((products) => {
        expect(products).toEqual(mockProducts);
      });

      const req = httpMock.expectOne(
        `${baseUrl}/search?term=${encodeURIComponent('PROD')}`
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockProducts);
    });
  });

  describe('findByStatus', () => {
    it('should get products by status', () => {
      const mockProducts: ProductDto[] = [
        {
          idProduct: 1,
          mainCode: 'PROD001',
          name: 'Product 1',
          description: 'Description 1',
          productType: 1,
          unitPrice: 10.0,
          tourismIva: false,
          ice: false,
          status: true,
          ivaTaxType: { idTaxType: 1, type: '2', name: 'IVA', value: 0 },
        },
      ];

      service.findByStatus(true).subscribe((products) => {
        expect(products).toEqual(mockProducts);
      });

      const req = httpMock.expectOne(`${baseUrl}/products-status/true`);
      expect(req.request.method).toBe('GET');
      req.flush(mockProducts);
    });
  });

  describe('searchActiveProductsForBilling', () => {
    it('should search active products for billing', () => {
      const mockResponse: ProductResponse = {
        code: 200,
        status: 'success',
        message: 'OK',
        productDtos: [
          {
            idProduct: 1,
            mainCode: 'PROD001',
            name: 'Product 1',
            description: 'Description 1',
            productType: 1,
            unitPrice: 10.0,
            tourismIva: false,
            ice: false,
            status: true,
            ivaTaxType: { idTaxType: 1, type: '2', name: 'IVA', value: 0 },
          },
        ],
      };

      service
        .searchActiveProductsForBilling('PROD', 1)
        .subscribe((products) => {
          expect(products).toEqual(mockResponse.productDtos);
        });

      const req = httpMock.expectOne(
        `${baseUrl}/billing/search?idCompany=1&term=${encodeURIComponent(
          'PROD'
        )}`
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });
  });

  describe('findActiveProductsByCompany', () => {
    it('should get active products by company', () => {
      const mockProducts: ProductDto[] = [
        {
          idProduct: 1,
          mainCode: 'PROD001',
          name: 'Product 1',
          description: 'Description 1',
          productType: 1,
          unitPrice: 10.0,
          tourismIva: false,
          ice: false,
          status: true,
          ivaTaxType: { idTaxType: 1, type: '2', name: 'IVA', value: 0 },
        },
      ];

      service.findActiveProductsByCompany(1).subscribe((products) => {
        expect(products).toEqual(mockProducts);
      });

      const req = httpMock.expectOne(`${baseUrl}/company/1`);
      expect(req.request.method).toBe('GET');
      req.flush(mockProducts);
    });
  });
});
