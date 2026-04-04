import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { CompanyService } from './company.service';
import { environment } from '../../../environments/environment';
import { CompanyFilter } from '../../_model/customer/company-filter';
import { CompanyResponse } from '../../_model/customer/company-response';

describe('CompanyService', () => {
  let service: CompanyService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.HOST_CUSTOMERS}/api/company`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [CompanyService],
    });

    service = TestBed.inject(CompanyService);
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
    it('should create a new company', () => {
      const company: CompanyFilter = {
        idCompany: null,
        idType: 1,
        identification: '1234567890',
        name: 'Test Company',
        nameCommercial: 'Test Company',
        status: true,
        forcedToAccounting: 'N',
        principal: 'Y',
        isSuperAdmin: false,
      };

      service.save(company).subscribe((response) => {
        expect(response).toBeTruthy();
      });

      const req = httpMock.expectOne(baseUrl);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(company);
      req.flush({ ...company, idCompany: 1 });
    });
  });

  describe('update', () => {
    it('should update an existing company', () => {
      const company: CompanyFilter = {
        idCompany: 1,
        idType: 1,
        identification: '1234567890',
        name: 'Updated Company',
        nameCommercial: 'Updated Company',
        status: true,
        forcedToAccounting: 'N',
        principal: 'Y',
        isSuperAdmin: false,
      };

      service.update(company, 1).subscribe((response) => {
        expect(response).toBeTruthy();
      });

      const req = httpMock.expectOne(`${baseUrl}/1`);
      expect(req.request.method).toBe('PATCH');
      expect(req.request.body).toEqual(company);
      req.flush(company);
    });
  });

  describe('findAll', () => {
    it('should get all companies', () => {
      const mockResponse: CompanyResponse = {
        code: 200,
        status: 'success',
        message: 'OK',
        companyDetailsDtos: [
          {
            idCompany: 1,
            idType: 1,
            identification: '1234567890',
            name: 'Company 1',
            nameCommercial: 'Company 1',
            status: true,
            forcedToAccounting: 'N',
            specialTaxpayer: 0,
            establishments: [],
          },
        ],
        companyDetailsDto: {
          idCompany: 1,
          idType: 1,
          identification: '1234567890',
          name: 'Company 1',
          nameCommercial: 'Company 1',
          status: true,
          forcedToAccounting: 'N',
          specialTaxpayer: 0,
          establishments: [],
        },
      };

      service.findAll().subscribe((response) => {
        expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(baseUrl);
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });
  });

  describe('findById', () => {
    it('should get company by ID', () => {
      const mockResponse: CompanyResponse = {
        code: 200,
        status: 'success',
        message: 'OK',
        companyDetailsDtos: [],
        companyDetailsDto: {
          idCompany: 1,
          idType: 1,
          identification: '1234567890',
          name: 'Company 1',
          nameCommercial: 'Company 1',
          status: true,
          forcedToAccounting: 'N',
          specialTaxpayer: 0,
          establishments: [],
        },
      };

      service.findById(1).subscribe((response) => {
        expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(`${baseUrl}/1`);
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });
  });

  describe('delete', () => {
    it('should delete a company', () => {
      service.delete(1).subscribe((response) => {
        expect(response).toBeTruthy();
      });

      const req = httpMock.expectOne(`${baseUrl}/1`);
      expect(req.request.method).toBe('DELETE');
      req.flush({});
    });
  });

  describe('searchPageable', () => {
    it('should search companies with pagination', () => {
      const company: CompanyFilter = {
        idCompany: null,
        idType: 1,
        identification: '',
        name: 'Test',
        nameCommercial: 'Test',
        status: true,
        forcedToAccounting: 'N',
        principal: 'Y',
        isSuperAdmin: false,
      };

      service.searchPageable(0, 10, company).subscribe((response) => {
        expect(response).toBeTruthy();
      });

      const req = httpMock.expectOne(`${baseUrl}/pageable?page=0&size=10`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(company);
      req.flush({ content: [], totalElements: 0 });
    });
  });

  describe('existsByIdentification', () => {
    it('should check if identification exists', () => {
      service.existsByIdentification('1234567890').subscribe((exists) => {
        expect(exists).toBe(true);
      });

      const req = httpMock.expectOne(`${baseUrl}/exist/1234567890`);
      expect(req.request.method).toBe('GET');
      req.flush(true);
    });
  });

  describe('findByIds', () => {
    it('should get companies by IDs', () => {
      const mockResponse: CompanyResponse = {
        code: 200,
        status: 'success',
        message: 'OK',
        companyDetailsDtos: [
          {
            idCompany: 1,
            idType: 1,
            identification: '1234567890',
            name: 'Company 1',
            nameCommercial: 'Company 1',
            status: true,
            forcedToAccounting: 'N',
            specialTaxpayer: 0,
            establishments: [],
          },
          {
            idCompany: 2,
            idType: 1,
            identification: '0987654321',
            name: 'Company 2',
            nameCommercial: 'Company 2',
            status: true,
            forcedToAccounting: 'N',
            specialTaxpayer: 0,
            establishments: [],
          },
        ],
        companyDetailsDto: null as any,
      };

      service.findByIds([1, 2]).subscribe((response) => {
        expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne('/api/company/companies/1,2');
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });

    it('should handle errors when finding companies by IDs', () => {
      service.findByIds([1, 2]).subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(error.status).toBe(500);
        },
      });

      const req = httpMock.expectOne('/api/company/companies/1,2');
      expect(req.request.method).toBe('GET');
      req.flush(null, { status: 500, statusText: 'Internal Server Error' });
    });
  });

  describe('uploadLogo', () => {
    it('should upload company logo', () => {
      const file = new File(['test'], 'logo.png', { type: 'image/png' });
      const mockResponse = {
        message: 'Logo uploaded successfully',
        logoPath: '/path/to/logo.png',
      };

      service.uploadLogo(1, file).subscribe((response) => {
        expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(`${baseUrl}/1/logo`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body instanceof FormData).toBe(true);
      req.flush(mockResponse);
    });
  });

  describe('getLogoPath', () => {
    it('should get company logo path', () => {
      const mockResponse = {
        logoPath: '/path/to/logo.png',
      };

      service.getLogoPath(1).subscribe((response) => {
        expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(`${baseUrl}/1/logo`);
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });
  });

  describe('getLogoUrl', () => {
    it('should return logo URL', () => {
      const url = service.getLogoUrl(1);
      expect(url).toBe(`${baseUrl}/1/logo/image`);
    });
  });

  describe('getLogoAsBlob', () => {
    it('should get company logo as blob', () => {
      const blob = new Blob(['test'], { type: 'image/png' });

      service.getLogoAsBlob(1).subscribe((response) => {
        expect(response).toBeInstanceOf(Blob);
      });

      const req = httpMock.expectOne(`${baseUrl}/1/logo/image`);
      expect(req.request.method).toBe('GET');
      expect(req.request.responseType).toBe('blob');
      req.flush(blob);
    });
  });

  describe('deleteLogo', () => {
    it('should delete company logo', () => {
      const mockResponse = { message: 'Logo eliminado exitosamente' };

      service.deleteLogo(1).subscribe((response) => {
        expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(`${baseUrl}/1/logo`);
      expect(req.request.method).toBe('DELETE');
      req.flush(mockResponse);
    });
  });
});
