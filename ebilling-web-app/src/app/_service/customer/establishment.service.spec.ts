import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import {
  EstablishmentService,
  EstablishmentRequest,
} from './establishment.service';
import { environment } from '../../../environments/environment';
import { Establishment } from '../../_model/customer/company-establishment';

describe('EstablishmentService', () => {
  let service: EstablishmentService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.HOST_CUSTOMERS}/api/establishments`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [EstablishmentService],
    });

    service = TestBed.inject(EstablishmentService);
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

  describe('findByCompany', () => {
    it('should get establishments by company ID', () => {
      const companyId = 1;
      const mockResponse: Establishment[] = [
        {
          idEstablishment: 1,
          name: 'Sucursal Centro',
          code: '001',
          address: 'Av. Principal 123',
          idCompany: companyId,
          type: 'SUCURSAL',
          status: true,
        },
      ];

      service.findByCompany(companyId).subscribe((establishments) => {
        expect(establishments.length).toBe(1);
        expect(establishments[0].name).toBe('Sucursal Centro');
      });

      const req = httpMock.expectOne(`${baseUrl}/company/${companyId}`);
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });
  });

  describe('findById', () => {
    it('should get establishment by ID', () => {
      const idEstablishment = 1;
      const companyId = 1;
      const mockResponse: Establishment = {
        idEstablishment: 1,
        name: 'Sucursal Centro',
        code: '001',
        address: 'Av. Principal 123',
        idCompany: companyId,
        type: 'SUCURSAL',
        status: true,
      };

      service
        .findById(idEstablishment, companyId)
        .subscribe((establishment) => {
          expect(establishment.idEstablishment).toBe(1);
          expect(establishment.name).toBe('Sucursal Centro');
        });

      const req = httpMock.expectOne(
        (request) =>
          request.url === `${baseUrl}/${idEstablishment}` &&
          request.params.get('companyId') === companyId.toString()
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });
  });

  describe('create', () => {
    it('should create a new establishment', () => {
      const request: EstablishmentRequest = {
        name: 'Nueva Sucursal',
        code: '002',
        address: 'Av. Nueva 456',
        companyId: 1,
        type: 'SUCURSAL',
        status: true,
      };

      const mockResponse: Establishment = {
        idEstablishment: 2,
        name: request.name,
        code: request.code,
        address: request.address,
        idCompany: request.companyId,
        type: request.type,
        status: request.status ?? true,
      };

      service.create(request).subscribe((establishment) => {
        expect(establishment.idEstablishment).toBe(2);
        expect(establishment.name).toBe('Nueva Sucursal');
      });

      const req = httpMock.expectOne(baseUrl);
      expect(req.request.method).toBe('POST');
      expect(req.request.body.companyId).toBe(1);
      req.flush(mockResponse);
    });

    it('should throw error if companyId is missing', () => {
      const request: EstablishmentRequest = {
        name: 'Nueva Sucursal',
        code: '002',
        address: 'Av. Nueva 456',
        companyId: 0, // Invalid
        type: 'SUCURSAL',
      };

      expect(() => service.create(request)).toThrow();
    });
  });

  describe('update', () => {
    it('should update an existing establishment', () => {
      const idEstablishment = 1;
      const request: EstablishmentRequest = {
        name: 'Sucursal Actualizada',
        code: '001',
        address: 'Av. Principal 456',
        companyId: 1,
        type: 'SUCURSAL',
        status: true,
      };

      const mockResponse: Establishment = {
        idEstablishment: 1,
        name: request.name,
        code: request.code,
        address: request.address,
        idCompany: request.companyId,
        type: request.type,
        status: request.status ?? true,
      };

      service.update(idEstablishment, request).subscribe((establishment) => {
        expect(establishment.name).toBe('Sucursal Actualizada');
      });

      const req = httpMock.expectOne(
        (request) =>
          request.url === `${baseUrl}/${idEstablishment}` &&
          request.params.get('companyId') === '1'
      );
      expect(req.request.method).toBe('PUT');
      expect(req.request.body.idCompany).toBe(request.companyId);
      req.flush(mockResponse);
    });
  });

  describe('delete', () => {
    it('should delete an establishment', () => {
      const idEstablishment = 1;
      const companyId = 1;

      service.delete(idEstablishment, companyId).subscribe((response) => {
        expect(response).toBeNull();
      });

      const req = httpMock.expectOne(
        (request) =>
          request.url === `${baseUrl}/${idEstablishment}` &&
          request.params.get('companyId') === companyId.toString()
      );
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });
});
