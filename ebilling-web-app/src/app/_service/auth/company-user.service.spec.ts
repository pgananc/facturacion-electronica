import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { CompanyUserService } from './company-user.service';
import { environment } from '../../../environments/environment';
import { UserCompanyRequest } from '../../_model/auth/user-company-request';
import { UserCompanyResponse } from '../../_model/auth/user-company-response';
import { UserResponse } from '../../_model/auth/user-response';

describe('CompanyUserService', () => {
  let service: CompanyUserService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.HOST_AUTH}/api/user-company`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [CompanyUserService],
    });

    service = TestBed.inject(CompanyUserService);
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
    it('should create a new company user', () => {
      const request: UserCompanyRequest = {
        idUser: 1,
        idCompany: 1,
      };

      const mockResponse = {
        code: 200,
        status: 'success',
      };

      service.save(request).subscribe((response) => {
        expect(response.code).toBe(200);
      });

      const req = httpMock.expectOne(baseUrl);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(request);
      req.flush(mockResponse);
    });
  });

  describe('delete', () => {
    it('should delete a company user', () => {
      const idCompany = 1;
      const idUser = 1;

      service.delete(idCompany, idUser).subscribe(() => {
        expect(true).toBe(true);
      });

      const req = httpMock.expectOne(
        `${baseUrl}/company/${idCompany}/user/${idUser}`
      );
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('findUsersByIdCompany', () => {
    it('should get users by company ID', () => {
      const idCompany = 1;
      const mockUserCompany = {
        idCompany: idCompany,
        userDtos: [
          {
            idUser: 1,
            userName: 'testuser',
          } as any,
        ],
      };
      const mockResponse: UserCompanyResponse = {
        code: 200,
        status: 'success',
        message: 'OK',
        userCompanyDtos: [mockUserCompany],
        userCompanyDto: mockUserCompany,
      };

      service.findUsersByIdCompany(idCompany).subscribe((response) => {
        expect(response.userCompanyDtos.length).toBe(1);
      });

      const req = httpMock.expectOne(`${baseUrl}/company/${idCompany}`);
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });
  });

  describe('findByIdCompanyAndIdUser', () => {
    it('should get company user by company and user ID', () => {
      const idCompany = 1;
      const idUser = 1;
      // Estructura de respuesta según el endpoint: GET /api/user-company/user/{idUser}/company/{idCompany}
      const mockResponse: UserResponse = {
        status: 'OK',
        code: 200,
        message: '',
        userDtos: [
          {
            idUser: idUser,
            userName: 'testuser',
            name: 'Test User',
            mail: 'test@example.com',
            status: true,
            idCompany: idCompany,
            roleDtos: [
              {
                idRole: 1,
                name: 'member',
                description: 'Miembro',
                status: true,
              } as any,
            ],
          } as any,
        ],
        userDto: null as any,
      };

      service
        .findByIdCompanyAndIdUser(idCompany, idUser)
        .subscribe((response) => {
          expect(response.userDtos?.length).toBe(1);
          expect(response.userDtos?.[0].idUser).toBe(idUser);
        });

      // Ruta correcta: /api/user-company/user/{idUser}/company/{idCompany}
      const req = httpMock.expectOne(
        `${baseUrl}/user/${idUser}/company/${idCompany}`
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
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
