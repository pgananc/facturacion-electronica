import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import {
  PaymentMethodService,
  PaymentMethodDto,
} from './payment-method.service';
import { environment } from '../../../environments/environment';

describe('PaymentMethodService', () => {
  let service: PaymentMethodService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.HOST_EBILLING}/api/payment-method`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [PaymentMethodService],
    });

    service = TestBed.inject(PaymentMethodService);
    httpMock = TestBed.inject(HttpTestingController);
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

  describe('findAll', () => {
    it('should get payment methods from backend', () => {
      const mockResponse: PaymentMethodDto[] = [
        {
          idPaymentMethod: 1,
          paymentMethodType: 'Efectivo',
          paymentMethodCode: '01',
        },
        {
          idPaymentMethod: 2,
          paymentMethodType: 'Tarjeta de Crédito',
          paymentMethodCode: '19',
        },
      ];

      service.findAll().subscribe((methods) => {
        expect(methods.length).toBe(2);
        expect(methods[0].paymentMethodCode).toBe('01');
      });

      const req = httpMock.expectOne(baseUrl);
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });

    it('should use cache on second call', () => {
      const mockResponse: PaymentMethodDto[] = [
        {
          idPaymentMethod: 1,
          paymentMethodType: 'Efectivo',
          paymentMethodCode: '01',
        },
      ];

      // Primera llamada
      service.findAll().subscribe();
      const req1 = httpMock.expectOne(baseUrl);
      req1.flush(mockResponse);

      // Segunda llamada (debe usar cache)
      service.findAll().subscribe((methods) => {
        expect(methods.length).toBe(1);
      });

      // No debe haber segunda petición HTTP
      httpMock.expectNone(baseUrl);
    });

    it('should force refresh when forceRefresh is true', () => {
      const mockResponse: PaymentMethodDto[] = [
        {
          idPaymentMethod: 1,
          paymentMethodType: 'Efectivo',
          paymentMethodCode: '01',
        },
      ];

      // Primera llamada
      service.findAll().subscribe();
      const req1 = httpMock.expectOne(baseUrl);
      req1.flush(mockResponse);

      // Segunda llamada con forceRefresh
      service.findAll(true).subscribe();
      const req2 = httpMock.expectOne(baseUrl);
      req2.flush(mockResponse);
    });

    it('should handle errors', () => {
      service.findAll().subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(error).toBeTruthy();
        },
      });

      const req = httpMock.expectOne(baseUrl);
      req.flush(null, { status: 500, statusText: 'Internal Server Error' });
    });
  });

  describe('findByCode', () => {
    it('should find payment method by code', () => {
      const mockResponse: PaymentMethodDto[] = [
        {
          idPaymentMethod: 1,
          paymentMethodType: 'Efectivo',
          paymentMethodCode: '01',
        },
      ];

      service.findAll().subscribe();
      const req = httpMock.expectOne(baseUrl);
      req.flush(mockResponse);

      const method = service.findByCode('01');
      expect(method).toBeTruthy();
      expect(method?.paymentMethodType).toBe('Efectivo');
    });

    it('should return undefined if code not found', () => {
      const mockResponse: PaymentMethodDto[] = [
        {
          idPaymentMethod: 1,
          paymentMethodType: 'Efectivo',
          paymentMethodCode: '01',
        },
      ];

      service.findAll().subscribe();
      const req = httpMock.expectOne(baseUrl);
      req.flush(mockResponse);

      const method = service.findByCode('99');
      expect(method).toBeUndefined();
    });

    it('should return undefined if cache not initialized', () => {
      const method = service.findByCode('01');
      expect(method).toBeUndefined();
    });
  });

  describe('clearCache', () => {
    it('should clear the cache', () => {
      const mockResponse: PaymentMethodDto[] = [
        {
          idPaymentMethod: 1,
          paymentMethodType: 'Efectivo',
          paymentMethodCode: '01',
        },
      ];

      service.findAll().subscribe();
      const req = httpMock.expectOne(baseUrl);
      req.flush(mockResponse);

      expect(service.isCacheInitialized()).toBe(true);

      service.clearCache();

      expect(service.isCacheInitialized()).toBe(false);
      expect(service.getCachedPaymentMethods()).toEqual([]);
    });
  });

  describe('isCacheInitialized', () => {
    it('should return false when cache is not initialized', () => {
      expect(service.isCacheInitialized()).toBe(false);
    });

    it('should return true when cache is initialized', () => {
      const mockResponse: PaymentMethodDto[] = [
        {
          idPaymentMethod: 1,
          paymentMethodType: 'Efectivo',
          paymentMethodCode: '01',
        },
      ];

      service.findAll().subscribe();
      const req = httpMock.expectOne(baseUrl);
      req.flush(mockResponse);

      expect(service.isCacheInitialized()).toBe(true);
    });
  });

  describe('getCachedPaymentMethods', () => {
    it('should return cached payment methods', () => {
      const mockResponse: PaymentMethodDto[] = [
        {
          idPaymentMethod: 1,
          paymentMethodType: 'Efectivo',
          paymentMethodCode: '01',
        },
      ];

      service.findAll().subscribe();
      const req = httpMock.expectOne(baseUrl);
      req.flush(mockResponse);

      const cached = service.getCachedPaymentMethods();
      expect(cached.length).toBe(1);
      expect(cached[0].paymentMethodCode).toBe('01');
    });

    it('should return empty array if cache not initialized', () => {
      const cached = service.getCachedPaymentMethods();
      expect(cached).toEqual([]);
    });
  });
});
