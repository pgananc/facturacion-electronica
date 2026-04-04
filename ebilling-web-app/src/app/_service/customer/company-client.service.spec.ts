import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { CompanyClientService } from './company-client.service';
import { environment } from '../../../environments/environment';
import { CompanyFilter } from '../../_model/customer/company-filter';
import { CompanyResponse } from '../../_model/customer/company-response';

describe('CompanyClientService', () => {
  let service: CompanyClientService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.HOST_CUSTOMERS}/api/company-client`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [CompanyClientService],
    });

    service = TestBed.inject(CompanyClientService);
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
    it('should create a new company client', () => {
      const company: CompanyFilter = {
        idCompany: 1,
        idType: 1,
        identification: '1234567890001',
        name: 'Test Company',
        nameCommercial: 'Test Company',
        status: true,
        forcedToAccounting: '',
        principal: '',
        isSuperAdmin: false,
      };

      service.save(company).subscribe((response) => {
        expect(response).toBeTruthy();
      });

      const req = httpMock.expectOne(baseUrl);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(company);
      req.flush({ id: 1, ...company });
    });
  });

  describe('update', () => {
    it('should update an existing company client', () => {
      const id = 1;
      const company: CompanyFilter = {
        idCompany: 1,
        idType: 1,
        identification: '1234567890001',
        name: 'Updated Company',
        nameCommercial: 'Updated Company',
        status: true,
        forcedToAccounting: '',
        principal: '',
        isSuperAdmin: false,
      };

      service.update(company, id).subscribe((response) => {
        expect(response).toBeTruthy();
      });

      const req = httpMock.expectOne(`${baseUrl}/${id}`);
      expect(req.request.method).toBe('PATCH');
      expect(req.request.body).toEqual(company);
      req.flush({ id, ...company });
    });
  });

  describe('findAll', () => {
    it('should get all company clients', () => {
      const mockResponse: CompanyResponse = {
        code: 200,
        status: 'success',
        message: 'OK',
        companyDetailsDtos: [
          {
            idCompany: 1,
            idType: 1,
            name: 'Company 1',
            identification: '1234567890001',
            nameCommercial: 'Company 1',
            status: true,
            forcedToAccounting: '',
            specialTaxpayer: 0,
            establishments: [],
          } as any,
        ],
        companyDetailsDto: {
          idCompany: 1,
          idType: 1,
          name: 'Company 1',
          identification: '1234567890001',
          nameCommercial: 'Company 1',
          status: true,
          forcedToAccounting: '',
          specialTaxpayer: 0,
          establishments: [],
        } as any,
      };

      service.findAll().subscribe((response) => {
        expect(response.companyDetailsDtos.length).toBe(1);
      });

      const req = httpMock.expectOne(baseUrl);
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });
  });

  describe('findById', () => {
    it('should get company client by ID', () => {
      const id = 1;
      const mockResponse: CompanyResponse = {
        code: 200,
        status: 'success',
        message: 'OK',
        companyDetailsDtos: [
          {
            idCompany: 1,
            idType: 1,
            name: 'Company 1',
            identification: '1234567890001',
            nameCommercial: 'Company 1',
            status: true,
            forcedToAccounting: '',
            specialTaxpayer: 0,
            establishments: [],
          } as any,
        ],
        companyDetailsDto: {
          idCompany: 1,
          idType: 1,
          name: 'Company 1',
          identification: '1234567890001',
          nameCommercial: 'Company 1',
          status: true,
          forcedToAccounting: '',
          specialTaxpayer: 0,
          establishments: [],
        } as any,
      };

      service.findById(id).subscribe((response) => {
        expect(response.companyDetailsDtos.length).toBe(1);
      });

      const req = httpMock.expectOne(`${baseUrl}/${id}`);
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });
  });

  describe('delete', () => {
    it('should delete a company client', () => {
      const idCompany = 1;
      const idClient = 1;

      service.delete(idCompany, idClient).subscribe(() => {
        expect(true).toBe(true);
      });

      const req = httpMock.expectOne(
        `${baseUrl}/company/${idCompany}/client/${idClient}`
      );
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('searchPageable', () => {
    it('should search with pagination', () => {
      const page = 0;
      const size = 10;
      const idCompany = 1;

      service.searchPageable(page, size, idCompany).subscribe((response) => {
        expect(response).toBeTruthy();
      });

      const req = httpMock.expectOne(
        `${baseUrl}/pageable?page=${page}&size=${size}`
      );
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toBe(idCompany);
      req.flush({ content: [], totalElements: 0 });
    });
  });
});
