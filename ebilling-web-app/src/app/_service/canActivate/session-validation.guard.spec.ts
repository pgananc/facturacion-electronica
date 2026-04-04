import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { SessionValidationGuard } from './session-validation.guard';
import { SessionValidationService } from '../auth/session-validation.service';
import { LoggerService } from '../logger/logger.service';
import { ToastService } from '../notification/toast.service';
import { environment } from '../../../environments/environment';
import { of, throwError } from 'rxjs';
import { SessionValidationResponse } from '../auth/session-validation.service';

describe('SessionValidationGuard', () => {
  let guard: SessionValidationGuard;
  let sessionValidationServiceSpy: jasmine.SpyObj<SessionValidationService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let loggerSpy: jasmine.SpyObj<LoggerService>;
  let toastSpy: jasmine.SpyObj<ToastService>;

  beforeEach(() => {
    const sessionService = jasmine.createSpyObj('SessionValidationService', [
      'validateSession',
      'clearSession',
    ]);
    const router = jasmine.createSpyObj('Router', ['createUrlTree']);
    const logger = jasmine.createSpyObj('LoggerService', [
      'debug',
      'warn',
      'error',
    ]);
    const toast = jasmine.createSpyObj('ToastService', [
      'success',
      'error',
      'warning',
      'info',
    ]);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        SessionValidationGuard,
        { provide: SessionValidationService, useValue: sessionService },
        { provide: Router, useValue: router },
        { provide: LoggerService, useValue: logger },
        { provide: ToastService, useValue: toast },
      ],
    });

    guard = TestBed.inject(SessionValidationGuard);
    sessionValidationServiceSpy = TestBed.inject(
      SessionValidationService
    ) as jasmine.SpyObj<SessionValidationService>;
    routerSpy = TestBed.inject(Router) as jasmine.SpyObj<Router>;
    loggerSpy = TestBed.inject(LoggerService) as jasmine.SpyObj<LoggerService>;
    toastSpy = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;

    routerSpy.createUrlTree.and.returnValue({} as any);
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should be created', () => {
    expect(guard).toBeTruthy();
  });

  it('should redirect to login when no token exists', (done) => {
    localStorage.removeItem(environment.TOKEN);

    guard.canActivate().subscribe((result) => {
      expect(result).toEqual({} as any);
      expect(routerSpy.createUrlTree).toHaveBeenCalledWith(['/login']);
      expect(loggerSpy.debug).toHaveBeenCalled();
      done();
    });
  });

  it('should allow access when session is valid', (done) => {
    localStorage.setItem(environment.TOKEN, 'valid-token');
    const validResponse: SessionValidationResponse = {
      valid: true,
    };
    sessionValidationServiceSpy.validateSession.and.returnValue(
      of(validResponse)
    );

    guard.canActivate().subscribe((result) => {
      expect(result).toBe(true);
      expect(loggerSpy.debug).toHaveBeenCalledWith(
        '[SessionGuard] ✅ Sesión válida, permitiendo acceso'
      );
      done();
    });
  });

  it('should redirect to login when session is invalid', (done) => {
    localStorage.setItem(environment.TOKEN, 'invalid-token');
    const invalidResponse: SessionValidationResponse = {
      valid: false,
      reason: 'token_expired',
    };
    sessionValidationServiceSpy.validateSession.and.returnValue(
      of(invalidResponse)
    );

    guard.canActivate().subscribe((result) => {
      expect(result).toEqual({} as any);
      expect(routerSpy.createUrlTree).toHaveBeenCalledWith(['/login']);
      expect(sessionValidationServiceSpy.clearSession).toHaveBeenCalled();
      expect(toastSpy.warning).toHaveBeenCalled();
      done();
    });
  });

  it('should handle IP mismatch reason', (done) => {
    localStorage.setItem(environment.TOKEN, 'token');
    const invalidResponse: SessionValidationResponse = {
      valid: false,
      reason: 'ip_mismatch',
    };
    sessionValidationServiceSpy.validateSession.and.returnValue(
      of(invalidResponse)
    );

    guard.canActivate().subscribe((result) => {
      expect(result).toEqual({} as any);
      expect(toastSpy.warning).toHaveBeenCalledWith(
        jasmine.stringContaining('cambio de red')
      );
      done();
    });
  });

  it('should handle browser mismatch reason', (done) => {
    localStorage.setItem(environment.TOKEN, 'token');
    const invalidResponse: SessionValidationResponse = {
      valid: false,
      reason: 'browser_mismatch',
    };
    sessionValidationServiceSpy.validateSession.and.returnValue(
      of(invalidResponse)
    );

    guard.canActivate().subscribe((result) => {
      expect(result).toEqual({} as any);
      expect(toastSpy.warning).toHaveBeenCalledWith(
        jasmine.stringContaining('cambio de navegador')
      );
      done();
    });
  });

  it('should handle invoice point released reason', (done) => {
    localStorage.setItem(environment.TOKEN, 'token');
    const invalidResponse: SessionValidationResponse = {
      valid: false,
      reason: 'invoice_point_released',
    };
    sessionValidationServiceSpy.validateSession.and.returnValue(
      of(invalidResponse)
    );

    guard.canActivate().subscribe((result) => {
      expect(result).toEqual({} as any);
      expect(toastSpy.warning).toHaveBeenCalledWith(
        jasmine.stringContaining('punto de emisión fue liberado')
      );
      done();
    });
  });

  it('should handle multiple sessions reason', (done) => {
    localStorage.setItem(environment.TOKEN, 'token');
    const invalidResponse: SessionValidationResponse = {
      valid: false,
      reason: 'multiple_sessions',
    };
    sessionValidationServiceSpy.validateSession.and.returnValue(
      of(invalidResponse)
    );

    guard.canActivate().subscribe((result) => {
      expect(result).toEqual({} as any);
      expect(toastSpy.warning).toHaveBeenCalledWith(
        jasmine.stringContaining('múltiples sesiones')
      );
      done();
    });
  });

  it('should handle network error', (done) => {
    localStorage.setItem(environment.TOKEN, 'token');
    sessionValidationServiceSpy.validateSession.and.returnValue(
      throwError(() => new Error('Network error'))
    );

    guard.canActivate().subscribe((result) => {
      expect(result).toEqual({} as any);
      expect(loggerSpy.error).toHaveBeenCalled();
      expect(sessionValidationServiceSpy.clearSession).toHaveBeenCalled();
      expect(toastSpy.warning).toHaveBeenCalledWith(
        jasmine.stringContaining('Error de conexión')
      );
      expect(routerSpy.createUrlTree).toHaveBeenCalledWith(['/login']);
      done();
    });
  });
});

