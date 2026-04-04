import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { LoginService } from './login.service';
import { environment } from '../../../environments/environment';
import { TokenResponse } from '../../_model/auth/token-response';
import { InvoicePointSessionService } from './invoice-point-session.service';
import { of, throwError } from 'rxjs';

describe('LoginService', () => {
  let service: LoginService;
  let httpMock: HttpTestingController;
  let router: jasmine.SpyObj<Router>;
  let invoicePointSessionService: jasmine.SpyObj<InvoicePointSessionService>;
  const baseUrl = `${environment.HOST_AUTH}/api/login`;

  beforeEach(() => {
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    const invoicePointSessionServiceSpy = jasmine.createSpyObj(
      'InvoicePointSessionService',
      ['releaseInvoicePointWithHeartbeat']
    );

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        LoginService,
        { provide: Router, useValue: routerSpy },
        {
          provide: InvoicePointSessionService,
          useValue: invoicePointSessionServiceSpy,
        },
      ],
    });

    service = TestBed.inject(LoginService);
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
    invoicePointSessionService = TestBed.inject(
      InvoicePointSessionService
    ) as jasmine.SpyObj<InvoicePointSessionService>;

    // Limpiar localStorage antes de cada test
    localStorage.clear();
  });

  afterEach(() => {
    if (httpMock) {
      httpMock.verify();
    }
    localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('login', () => {
    it('should login successfully', () => {
      const userName = 'testuser';
      const password = 'testpass';
      const mockResponse: TokenResponse = {
        token: { token: 'mock-token' },
        code: 0,
        status: 'success',
        message: 'OK',
      };

      service.login(userName, password).subscribe((response) => {
        expect(response).toEqual(mockResponse);
        expect(response.token.token).toBe('mock-token');
      });

      const req = httpMock.expectOne(baseUrl);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ userName, password });
      req.flush(mockResponse);
    });

    it('should handle login errors', () => {
      const userName = 'testuser';
      const password = 'wrongpass';

      service.login(userName, password).subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(error.status).toBe(401);
        },
      });

      const req = httpMock.expectOne(baseUrl);
      req.flush(null, { status: 401, statusText: 'Unauthorized' });
    });
  });

  describe('isLogin', () => {
    it('should return false when no token exists', () => {
      localStorage.removeItem(environment.TOKEN);
      expect(service.isLogin()).toBe(false);
    });

    it('should return true when token exists', () => {
      localStorage.setItem(environment.TOKEN, 'test-token');
      expect(service.isLogin()).toBe(true);
    });
  });

  describe('setLoginStatus', () => {
    it('should update login state', () => {
      // Suscribirse primero para capturar el cambio
      let receivedValue = false;
      const subscription = service.loginState$.subscribe((isLoggedIn) => {
        if (isLoggedIn) {
          receivedValue = true;
        }
      });

      // Cambiar el estado
      service.setLoginStatus(true);

      // Verificar que el valor se actualizó
      expect(receivedValue).toBe(true);

      subscription.unsubscribe();
    });
  });

  describe('sendMail', () => {
    it('should send mail successfully', () => {
      const mail = 'test@example.com';
      const mockResponse = 1;

      service.sendMail(mail).subscribe((response) => {
        expect(response).toBe(mockResponse);
      });

      const req = httpMock.expectOne(`${environment.HOST_AUTH}/api/mail/send`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toBe(mail);
      req.flush(mockResponse);
    });
  });

  describe('logoutFromServer', () => {
    it('should logout from server successfully', () => {
      const userName = 'testuser';

      service.logoutFromServer(userName).subscribe((response) => {
        expect(response).toBeNull();
      });

      const req = httpMock.expectOne(
        (request) =>
          request.url === `${environment.HOST_AUTH}/api/login/logout` &&
          request.params.get('userName') === userName
      );
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('closeSession', () => {
    beforeEach(() => {
      invoicePointSessionService.releaseInvoicePointWithHeartbeat.and.returnValue(
        Promise.resolve()
      );
    });

    it('should close session when token exists', (done) => {
      localStorage.setItem(
        environment.TOKEN,
        'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyTmFtZSI6InRlc3R1c2VyIn0.test'
      );

      service.closeSession();

      setTimeout(() => {
        expect(
          invoicePointSessionService.releaseInvoicePointWithHeartbeat
        ).toHaveBeenCalled();

        // Mockear la petición HTTP de logout
        const req = httpMock.expectOne(
          (request) =>
            request.url === `${environment.HOST_AUTH}/api/login/logout` &&
            request.params.get('userName') === 'testuser'
        );
        expect(req.request.method).toBe('DELETE');
        req.flush(null);

        done();
      }, 100);
    });

    it('should close session when no token exists', () => {
      localStorage.removeItem(environment.TOKEN);

      service.closeSession();

      expect(router.navigate).toHaveBeenCalledWith(['login']);
      expect(localStorage.length).toBe(0);
    });
  });
});
