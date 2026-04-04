import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { UserService } from './user.service';
import { environment } from '../../../environments/environment';
import { User } from '../../_model/auth/user';
import { UserResponse } from '../../_model/auth/user-response';

describe('UserService', () => {
  let service: UserService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.HOST_AUTH}/api/user`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [UserService],
    });

    service = TestBed.inject(UserService);
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
    it('should create a new user', () => {
      const user: User = {
        idUser: 0,
        name: 'Test User',
        userName: 'testuser',
        password: 'password123',
        mail: 'test@example.com',
        status: true,
        idCompany: 1,
        roleDtos: [],
      };

      service.save(user).subscribe((response) => {
        expect(response).toBeTruthy();
      });

      const req = httpMock.expectOne(baseUrl);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(user);
      req.flush({ ...user, idUser: 1 });
    });
  });

  describe('update', () => {
    it('should update an existing user', () => {
      const user: User = {
        idUser: 1,
        name: 'Updated User',
        userName: 'testuser',
        password: 'password123',
        mail: 'updated@example.com',
        status: true,
        idCompany: 1,
        roleDtos: [],
      };

      service.update(user, 1).subscribe((response) => {
        expect(response).toBeTruthy();
      });

      const req = httpMock.expectOne(`${baseUrl}/1`);
      expect(req.request.method).toBe('PATCH');
      expect(req.request.body).toEqual(user);
      req.flush(user);
    });
  });

  describe('searchPageable', () => {
    it('should search users with pagination', () => {
      const user: User = {
        idUser: 0,
        name: 'Test',
        userName: 'test',
        password: '',
        mail: '',
        status: true,
        idCompany: 1,
        roleDtos: [],
      };

      service.searchPageable(0, 10, user).subscribe((response) => {
        expect(response).toBeTruthy();
      });

      const req = httpMock.expectOne(`${baseUrl}/pageable?page=0&size=10`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(user);
      req.flush({ content: [], totalElements: 0 });
    });
  });

  describe('delete', () => {
    it('should delete a user', () => {
      service.delete(1).subscribe((response) => {
        expect(response).toBeTruthy();
      });

      const req = httpMock.expectOne(`${baseUrl}/1`);
      expect(req.request.method).toBe('DELETE');
      req.flush({});
    });
  });

  describe('findByIdCompany', () => {
    it('should get users by company ID', () => {
      const mockResponse: UserResponse = {
        code: 200,
        status: 'success',
        message: 'OK',
        userDto: null as any,
        userDtos: [
          {
            idUser: 1,
            name: 'Test User',
            userName: 'testuser',
            password: '',
            mail: 'test@example.com',
            status: true,
            idCompany: 1,
            roleDtos: [],
          },
        ],
      };

      service.findByIdCompany(1).subscribe((response) => {
        expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(`${baseUrl}/company/1`);
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });
  });

  describe('findById', () => {
    it('should get user by ID', () => {
      const mockResponse: UserResponse = {
        code: 200,
        status: 'success',
        message: 'OK',
        userDto: null as any,
        userDtos: [
          {
            idUser: 1,
            name: 'Test User',
            userName: 'testuser',
            password: '',
            mail: 'test@example.com',
            status: true,
            idCompany: 1,
            roleDtos: [],
          },
        ],
      };

      service.findById(1).subscribe((response) => {
        expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(`${baseUrl}/1`);
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });
  });

  describe('existsByUserName', () => {
    it('should check if username exists', () => {
      service.existsByUserName('testuser').subscribe((exists) => {
        expect(exists).toBe(true);
      });

      const req = httpMock.expectOne(`${baseUrl}/exist/userName/testuser`);
      expect(req.request.method).toBe('GET');
      req.flush(true);
    });
  });

  describe('resetPassword', () => {
    it('should reset user password', () => {
      service.resetPassword('token123', 'newpassword').subscribe((success) => {
        expect(success).toBe(true);
      });

      const req = httpMock.expectOne(`${baseUrl}/restore/token123`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toBe('newpassword');
      expect(req.request.headers.get('Content-Type')).toBe('text/plain');
      req.flush(true);
    });
  });

  describe('findActiveUsersAndNotInCompany', () => {
    it('should get active users not in company', () => {
      const mockResponse: UserResponse = {
        code: 200,
        status: 'success',
        message: 'OK',
        userDto: null as any,
        userDtos: [
          {
            idUser: 1,
            name: 'Test User',
            userName: 'testuser',
            password: '',
            mail: 'test@example.com',
            status: true,
            idCompany: 1,
            roleDtos: [],
          },
        ],
      };

      service.findActiveUsersAndNotInCompany(1).subscribe((response) => {
        expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(`${baseUrl}/not-company/1`);
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });
  });

  describe('existsByMail', () => {
    it('should check if email exists', () => {
      service.existsByMail('test@example.com').subscribe((exists) => {
        expect(exists).toBe(true);
      });

      const req = httpMock.expectOne(`${baseUrl}/exist/mail/test@example.com`);
      expect(req.request.method).toBe('GET');
      req.flush(true);
    });
  });

  describe('findUserByUserNameAndPassword', () => {
    it('should find user by username and password', () => {
      const user: User = {
        idUser: 0,
        name: 'Test User',
        userName: 'testuser',
        password: 'password123',
        mail: '',
        status: true,
        idCompany: 1,
        roleDtos: [],
      };

      const mockResponse: UserResponse = {
        code: 200,
        status: 'success',
        message: 'OK',
        userDto: user,
        userDtos: [user],
      };

      service.findUserByUserNameAndPassword(user).subscribe((response) => {
        expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(`${baseUrl}/userName/password`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(user);
      req.flush(mockResponse);
    });
  });

  describe('updatePassword', () => {
    it('should update user password', () => {
      const newPassword = 'newpassword';
      const idUser = 1;

      service.updatePassword(newPassword, idUser).subscribe((response) => {
        expect(response).toBe(true);
      });

      const req = httpMock.expectOne(`${baseUrl}/1/password`);
      expect(req.request.method).toBe('PATCH');
      expect(req.request.body).toEqual({ password: newPassword });
      req.flush(true);
    });
  });

  describe('findUserByUserName', () => {
    it('should find user by username', () => {
      const mockResponse: UserResponse = {
        code: 200,
        status: 'success',
        message: 'OK',
        userDto: null as any,
        userDtos: [
          {
            idUser: 1,
            name: 'Test User',
            userName: 'testuser',
            password: '',
            mail: 'test@example.com',
            status: true,
            idCompany: 1,
            roleDtos: [],
          },
        ],
      };

      service.findUserByUserName('testuser').subscribe((response) => {
        expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(`${baseUrl}/userName/testuser`);
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });
  });
});
