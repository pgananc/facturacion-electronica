import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { TaxTypeService } from './tax-type.service';
import { environment } from '../../../environments/environment';
import { TaxTypeResponse } from '../../_model/product/tax-type-response';
import { LoggerService } from '../logger/logger.service';

describe('TaxTypeService', () => {
  let service: TaxTypeService;
  let httpMock: HttpTestingController;
  let logger: jasmine.SpyObj<LoggerService>;
  const baseUrl = `${environment.HOST_PRODUCTS}/api/tax-type`;

  beforeEach(() => {
    const loggerSpy = jasmine.createSpyObj('LoggerService', ['error', 'debug']);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        TaxTypeService,
        { provide: LoggerService, useValue: loggerSpy },
      ],
    });

    service = TestBed.inject(TaxTypeService);
    httpMock = TestBed.inject(HttpTestingController);
    logger = TestBed.inject(LoggerService) as jasmine.SpyObj<LoggerService>;
  });

  afterEach(() => {
    if (httpMock) {
      httpMock.verify();
    }
    service.clearCache();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getTaxTypes', () => {
    it('should get tax types from backend', () => {
      const mockResponse: TaxTypeResponse = {
        code: 200,
        status: 'success',
        message: 'OK',
        taxTypes: [
          {
            idTaxType: 1,
            type: 'IVA',
            name: 'IVA 0%',
            value: 0,
          },
          {
            idTaxType: 2,
            type: 'IVA',
            name: 'IVA 12%',
            value: 12,
          },
        ],
      };

      service.getTaxTypes().subscribe((taxTypes) => {
        expect(taxTypes.length).toBe(2);
        expect(taxTypes[0].name).toBe('IVA 0%');
      });

      const req = httpMock.expectOne(
        (request) =>
          request.url === baseUrl && request.params.get('status') === 'true'
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });

    it('should use cache on second call', () => {
      const mockResponse: TaxTypeResponse = {
        code: 200,
        status: 'success',
        message: 'OK',
        taxTypes: [
          {
            idTaxType: 1,
            type: 'IVA',
            name: 'IVA 0%',
            value: 0,
          },
        ],
      };

      // Primera llamada
      service.getTaxTypes().subscribe();
      const req1 = httpMock.expectOne(
        (request) =>
          request.url === baseUrl && request.params.get('status') === 'true'
      );
      req1.flush(mockResponse);

      // Segunda llamada (debe usar cache)
      service.getTaxTypes().subscribe((taxTypes) => {
        expect(taxTypes.length).toBe(1);
      });

      // No debe haber segunda petición HTTP
      httpMock.expectNone(baseUrl);
    });

    it('should force refresh when forceRefresh is true', () => {
      const mockResponse: TaxTypeResponse = {
        code: 200,
        status: 'success',
        message: 'OK',
        taxTypes: [
          {
            idTaxType: 1,
            type: 'IVA',
            name: 'IVA 0%',
            value: 0,
          },
        ],
      };

      // Primera llamada
      service.getTaxTypes().subscribe();
      const req1 = httpMock.expectOne(
        (request) =>
          request.url === baseUrl && request.params.get('status') === 'true'
      );
      req1.flush(mockResponse);

      // Segunda llamada con forceRefresh
      service.getTaxTypes(true).subscribe();
      const req2 = httpMock.expectOne(
        (request) =>
          request.url === baseUrl && request.params.get('status') === 'true'
      );
      req2.flush(mockResponse);
    });

    it('should filter by type when type is provided', () => {
      const mockResponse: TaxTypeResponse = {
        code: 200,
        status: 'success',
        message: 'OK',
        taxTypes: [
          {
            idTaxType: 1,
            type: 'IVA',
            name: 'IVA 0%',
            value: 0,
          },
        ],
      };

      service.getTaxTypes(false, 'IVA').subscribe((taxTypes) => {
        expect(taxTypes.length).toBe(1);
      });

      const req = httpMock.expectOne(
        (request) =>
          request.url === baseUrl &&
          request.params.get('status') === 'true' &&
          request.params.get('type') === 'IVA'
      );
      req.flush(mockResponse);
    });

    it('should handle errors gracefully', () => {
      service.getTaxTypes().subscribe((taxTypes) => {
        expect(taxTypes).toEqual([]);
      });

      const req = httpMock.expectOne(
        (request) =>
          request.url === baseUrl && request.params.get('status') === 'true'
      );
      req.flush(null, { status: 500, statusText: 'Internal Server Error' });

      expect(logger.error).toHaveBeenCalled();
    });
  });

  describe('getTaxTypeById', () => {
    it('should get tax type by id from cache', () => {
      const mockResponse: TaxTypeResponse = {
        code: 200,
        status: 'success',
        message: 'OK',
        taxTypes: [
          {
            idTaxType: 1,
            type: 'IVA',
            name: 'IVA 0%',
            value: 0,
          },
        ],
      };

      service.getTaxTypes().subscribe();
      const req = httpMock.expectOne(
        (request) =>
          request.url === baseUrl && request.params.get('status') === 'true'
      );
      req.flush(mockResponse);

      const taxType = service.getTaxTypeById(1);
      expect(taxType).toBeTruthy();
      expect(taxType?.name).toBe('IVA 0%');
    });

    it('should return undefined if tax type not found', () => {
      const taxType = service.getTaxTypeById(999);
      expect(taxType).toBeUndefined();
    });
  });

  describe('clearCache', () => {
    it('should clear the cache', () => {
      const mockResponse: TaxTypeResponse = {
        code: 200,
        status: 'success',
        message: 'OK',
        taxTypes: [
          {
            idTaxType: 1,
            type: 'IVA',
            name: 'IVA 0%',
            value: 0,
          },
        ],
      };

      service.getTaxTypes().subscribe();
      const req = httpMock.expectOne(
        (request) =>
          request.url === baseUrl && request.params.get('status') === 'true'
      );
      req.flush(mockResponse);

      service.clearCache();
      expect(service.getTaxTypeById(1)).toBeUndefined();
    });
  });

  describe('preloadCache', () => {
    it('should preload cache if not loaded', () => {
      const mockResponse: TaxTypeResponse = {
        code: 200,
        status: 'success',
        message: 'OK',
        taxTypes: [
          {
            idTaxType: 1,
            type: 'IVA',
            name: 'IVA 0%',
            value: 0,
          },
        ],
      };

      service.preloadCache();

      const req = httpMock.expectOne(
        (request) =>
          request.url === baseUrl && request.params.get('status') === 'true'
      );
      req.flush(mockResponse);
    });

    it('should not reload if cache is already loaded', () => {
      const mockResponse: TaxTypeResponse = {
        code: 200,
        status: 'success',
        message: 'OK',
        taxTypes: [
          {
            idTaxType: 1,
            type: 'IVA',
            name: 'IVA 0%',
            value: 0,
          },
        ],
      };

      // Cargar cache primero
      service.getTaxTypes().subscribe();
      const req1 = httpMock.expectOne(
        (request) =>
          request.url === baseUrl && request.params.get('status') === 'true'
      );
      req1.flush(mockResponse);

      // Preload no debe hacer otra petición
      service.preloadCache();
      httpMock.expectNone(baseUrl);
    });
  });
});
