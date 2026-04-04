import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import {
  IssuancePointService,
  IssuancePointRequest,
} from './issuance-point.service';
import { environment } from '../../../environments/environment';
import { InvoiceIssuancePoint } from '../../_model/customer/invoice-issuance-point';

describe('IssuancePointService', () => {
  let service: IssuancePointService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.HOST_CUSTOMERS}/api/invoice-issuance-points`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [IssuancePointService],
    });

    service = TestBed.inject(IssuancePointService);
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

  describe('findByEstablishment', () => {
    it('should get issuance points by establishment ID', () => {
      const establishmentId = 1;
      const mockResponse: InvoiceIssuancePoint[] = [
        {
          idInvoiceIssuancePoint: 1,
          code: '001',
          establishmentId: establishmentId,
          sequentialInvoice: 1,
          status: true,
        },
      ];

      service.findByEstablishment(establishmentId).subscribe((points) => {
        expect(points.length).toBe(1);
        expect(points[0].code).toBe('001');
      });

      const req = httpMock.expectOne(
        `${baseUrl}/establishment/${establishmentId}`
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });
  });

  describe('findByCompany', () => {
    it('should get issuance points by company ID', () => {
      const companyId = 1;
      const mockResponse = [
        {
          idInvoiceIssuancePoint: 1,
          code: '001',
          establishmentId: 1,
          sequentialInvoice: 1,
          status: true,
          establishmentCode: '001',
          establishmentName: 'Sucursal Centro',
        },
      ];

      service.findByCompany(companyId).subscribe((points) => {
        expect(points.length).toBe(1);
        expect(points[0].establishmentCode).toBe('001');
      });

      const req = httpMock.expectOne(`${baseUrl}/company/${companyId}`);
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });
  });

  describe('findById', () => {
    it('should get issuance point by ID', () => {
      const idInvoiceIssuancePoint = 1;
      const establishmentId = 1;
      const mockResponse: InvoiceIssuancePoint = {
        idInvoiceIssuancePoint: 1,
        code: '001',
        establishmentId: establishmentId,
        sequentialInvoice: 1,
        status: true,
      };

      service
        .findById(idInvoiceIssuancePoint, establishmentId)
        .subscribe((point) => {
          expect(point.idInvoiceIssuancePoint).toBe(1);
          expect(point.code).toBe('001');
        });

      const req = httpMock.expectOne(
        (request) =>
          request.url === `${baseUrl}/${idInvoiceIssuancePoint}` &&
          request.params.get('establishmentId') === establishmentId.toString()
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });
  });

  describe('create', () => {
    it('should create a new issuance point', () => {
      const request: IssuancePointRequest = {
        code: '002',
        establishmentId: 1,
        sequentialInvoice: 1,
        status: true,
      };

      const mockResponse: InvoiceIssuancePoint = {
        idInvoiceIssuancePoint: 2,
        code: request.code,
        establishmentId: request.establishmentId,
        sequentialInvoice: request.sequentialInvoice,
        status: request.status ?? true,
      };

      service.create(request).subscribe((point) => {
        expect(point.idInvoiceIssuancePoint).toBe(2);
        expect(point.code).toBe('002');
      });

      const req = httpMock.expectOne(baseUrl);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(request);
      req.flush(mockResponse);
    });
  });

  describe('update', () => {
    it('should update an existing issuance point', () => {
      const idInvoiceIssuancePoint = 1;
      const request: IssuancePointRequest = {
        code: '001',
        establishmentId: 1,
        sequentialInvoice: 2,
        status: true,
      };

      const mockResponse: InvoiceIssuancePoint = {
        idInvoiceIssuancePoint: 1,
        code: request.code,
        establishmentId: request.establishmentId,
        sequentialInvoice: request.sequentialInvoice,
        status: request.status ?? true,
      };

      service.update(idInvoiceIssuancePoint, request).subscribe((point) => {
        expect(point.sequentialInvoice).toBe(2);
      });

      const req = httpMock.expectOne(
        (request) =>
          request.url === `${baseUrl}/${idInvoiceIssuancePoint}` &&
          request.params.get('establishmentId') === '1'
      );
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(request);
      req.flush(mockResponse);
    });
  });

  describe('delete', () => {
    it('should delete an issuance point', () => {
      const idInvoiceIssuancePoint = 1;
      const establishmentId = 1;

      service.delete(idInvoiceIssuancePoint, establishmentId).subscribe(() => {
        expect(true).toBe(true);
      });

      const req = httpMock.expectOne(
        (request) =>
          request.url === `${baseUrl}/${idInvoiceIssuancePoint}` &&
          request.params.get('establishmentId') === establishmentId.toString()
      );
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });
});
