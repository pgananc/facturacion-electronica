import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { TokenService } from './token.service';
import { environment } from '../../../environments/environment';

describe('TokenService', () => {
  let service: TokenService;
  let httpMock: HttpTestingController;
  let router: jasmine.SpyObj<Router>;
  const baseUrl = `${environment.HOST_AUTH}/api/token`;

  beforeEach(() => {
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [TokenService, { provide: Router, useValue: routerSpy }],
    });

    service = TestBed.inject(TokenService);
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
  });

  afterEach(() => {
    if (httpMock) {
      httpMock.verify();
    }
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('validateTokenReset', () => {
    it('should validate reset token successfully', () => {
      const token = 'valid-reset-token';
      const mockResponse = true;

      service.validateTokenReset(token).subscribe((response) => {
        expect(response).toBe(true);
      });

      const req = httpMock.expectOne(
        `${baseUrl}/restore-password/validate/${token}`
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });

    it('should return false for invalid token', () => {
      const token = 'invalid-token';
      const mockResponse = false;

      service.validateTokenReset(token).subscribe((response) => {
        expect(response).toBe(false);
      });

      const req = httpMock.expectOne(
        `${baseUrl}/restore-password/validate/${token}`
      );
      req.flush(mockResponse);
    });

    it('should handle errors when validating token', () => {
      const token = 'expired-token';

      service.validateTokenReset(token).subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(error.status).toBe(400);
        },
      });

      const req = httpMock.expectOne(
        `${baseUrl}/restore-password/validate/${token}`
      );
      req.flush(null, { status: 400, statusText: 'Bad Request' });
    });
  });
});
