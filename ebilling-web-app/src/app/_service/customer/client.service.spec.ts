import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { ClientService } from './client.service';
import { environment } from '../../../environments/environment';
import { ClientSaveDto } from '../../_model/customer/client-save-dto';
import { ClientRequestDto } from '../../_model/customer/client-request-dto';
import { Client } from '../../_model/customer/client';

describe('ClientService', () => {
  let service: ClientService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ClientService],
    });

    service = TestBed.inject(ClientService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('save', () => {
    it('should save a new client', (done) => {
      const clientDto: ClientSaveDto = {
        idType: 1,
        clientType: 1,
        identification: '1234567890',
        name: 'Test Client',
        status: true,
        companyIdentification: '0987654321',
        contacts: [],
      };
      const mockResponse: Client = {
        idClient: 1,
        ...clientDto,
      } as Client;

      service.save(clientDto).subscribe((client) => {
        expect(client).toBeTruthy();
        expect(client.idClient).toBe(1);
        done();
      });

      const req = httpMock.expectOne(
        `${environment.HOST_CUSTOMERS}/api/client`
      );
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(clientDto);
      req.flush(mockResponse);
    });
  });

  describe('update', () => {
    it('should update an existing client', (done) => {
      const clientDto: ClientSaveDto = {
        idType: 1,
        clientType: 1,
        identification: '1234567890',
        name: 'Updated Client',
        status: true,
        companyIdentification: '0987654321',
        contacts: [],
      };
      const mockResponse: Client = {
        idClient: 1,
        ...clientDto,
      } as Client;

      service.update(clientDto, 1).subscribe((client) => {
        expect(client).toBeTruthy();
        expect(client.name).toBe('Updated Client');
        done();
      });

      const req = httpMock.expectOne(
        `${environment.HOST_CUSTOMERS}/api/client/1`
      );
      expect(req.request.method).toBe('PATCH');
      expect(req.request.body).toEqual(clientDto);
      req.flush(mockResponse);
    });
  });

  describe('findByCompany', () => {
    it('should get client by company identification', (done) => {
      const mockResponse = {
        code: 0,
        message: 'Success',
        clientDtos: [
          {
            idClient: 1,
            identification: '1234567890',
            name: 'Test Client',
          } as Client,
        ],
      };

      service.findByCompany('1234567890').subscribe((response) => {
        expect(response).toBeTruthy();
        expect(response.clientDtos).toBeDefined();
        expect(response.clientDtos.length).toBeGreaterThan(0);
        done();
      });

      const req = httpMock.expectOne(
        `${environment.HOST_CUSTOMERS}/api/client/company/1234567890`
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });
  });

  describe('findById', () => {
    it('should get client by id', (done) => {
      const mockResponse = {
        code: 0,
        message: 'Success',
        clientDtos: [
          {
            idClient: 1,
            identification: '1234567890',
            name: 'Test Client',
          } as Client,
        ],
      };

      service.findById(1).subscribe((response) => {
        expect(response).toBeTruthy();
        expect(response.clientDtos).toBeDefined();
        expect(response.clientDtos.length).toBeGreaterThan(0);
        done();
      });

      const req = httpMock.expectOne(
        `${environment.HOST_CUSTOMERS}/api/client/1`
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });
  });

  describe('delete', () => {
    it('should delete a client', (done) => {
      service.delete(1).subscribe(() => {
        done();
      });

      const req = httpMock.expectOne(
        `${environment.HOST_CUSTOMERS}/api/client/1`
      );
      expect(req.request.method).toBe('DELETE');
      req.flush({});
    });
  });

  describe('findClientByIdentificationOrNameOrType', () => {
    it('should search client by identification, name or type', (done) => {
      const searchClient: Client = {
        identification: '1234567890',
      } as Client;
      const mockResponse = {
        idClient: 1,
        identification: '1234567890',
        name: 'Test Client',
      };

      service
        .findClientByIdentificationOrNameOrType(searchClient)
        .subscribe((client) => {
          expect(client).toBeTruthy();
          done();
        });

      const req = httpMock.expectOne(
        `${environment.HOST_CUSTOMERS}/api/client/search`
      );
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(searchClient);
      req.flush(mockResponse);
    });
  });

  describe('searchPageable', () => {
    it('should search clients with pagination', (done) => {
      const searchDto: ClientRequestDto = {
        identification: '1234567890',
        name: '',
        clientType: 1,
        status: true,
        companyIdentification: '0987654321',
      };
      const mockResponse = {
        content: [],
        totalElements: 0,
        totalPages: 0,
      };

      service.searchPageable(0, 10, searchDto).subscribe((response) => {
        expect(response).toBeTruthy();
        done();
      });

      const req = httpMock.expectOne(
        `${environment.HOST_CUSTOMERS}/api/client/pageable?page=0&size=10`
      );
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(searchDto);
      req.flush(mockResponse);
    });

    it('should search clients with sorting', (done) => {
      const searchDto: ClientRequestDto = {
        identification: '1234567890',
        name: '',
        clientType: 1,
        status: true,
        companyIdentification: '0987654321',
      };
      const mockResponse = {
        content: [],
        totalElements: 0,
        totalPages: 0,
      };

      service
        .searchPageable(0, 10, searchDto, 'name', 'ASC')
        .subscribe((response) => {
          expect(response).toBeTruthy();
          done();
        });

      const req = httpMock.expectOne(
        `${environment.HOST_CUSTOMERS}/api/client/pageable?page=0&size=10&sort=name,ASC`
      );
      expect(req.request.method).toBe('POST');
      req.flush(mockResponse);
    });

    it('should not add sort parameter when sortField is empty', (done) => {
      const searchDto: ClientRequestDto = {
        identification: '1234567890',
        name: '',
        clientType: 1,
        status: true,
        companyIdentification: '0987654321',
      };
      const mockResponse = {
        content: [],
        totalElements: 0,
        totalPages: 0,
      };

      service.searchPageable(0, 10, searchDto, '', 'ASC').subscribe(() => {
        done();
      });

      const req = httpMock.expectOne(
        `${environment.HOST_CUSTOMERS}/api/client/pageable?page=0&size=10`
      );
      expect(req.request.url).not.toContain('sort=');
      req.flush(mockResponse);
    });

    it('should not add sort parameter when sortDirection is empty', (done) => {
      const searchDto: ClientRequestDto = {
        identification: '1234567890',
        name: '',
        clientType: 1,
        status: true,
        companyIdentification: '0987654321',
      };
      const mockResponse = {
        content: [],
        totalElements: 0,
        totalPages: 0,
      };

      service.searchPageable(0, 10, searchDto, 'name', '').subscribe(() => {
        done();
      });

      const req = httpMock.expectOne(
        `${environment.HOST_CUSTOMERS}/api/client/pageable?page=0&size=10`
      );
      expect(req.request.url).not.toContain('sort=');
      req.flush(mockResponse);
    });
  });

  describe('Subjects', () => {
    it('should have clientsChange subject', () => {
      expect(service.clientsChange).toBeTruthy();
      // Verificar que es un Subject de RxJS
      expect(service.clientsChange.subscribe).toBeDefined();
    });

    it('should have messageChange subject', () => {
      expect(service.messageChange).toBeTruthy();
      // Verificar que es un Subject de RxJS
      expect(service.messageChange.subscribe).toBeDefined();
    });
  });
});
