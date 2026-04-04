import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { AdminGuard } from './admin.guard';
import { SUPER_ADMIN_ROLE } from '../_constants/constants';
import { JwtHelperService } from '@auth0/angular-jwt';

describe('AdminGuard', () => {
  let guard: AdminGuard;
  let routerSpy: jasmine.SpyObj<Router>;

  const createMockToken = (payload: any): string => {
    const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
    const body = btoa(JSON.stringify(payload));
    return `${header}.${body}.signature`;
  };

  beforeEach(() => {
    const router = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AdminGuard, { provide: Router, useValue: router }],
    });

    guard = TestBed.inject(AdminGuard);
    routerSpy = TestBed.inject(Router) as jasmine.SpyObj<Router>;
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should be created', () => {
    expect(guard).toBeTruthy();
  });

  it('should allow access for super_admin', () => {
    const token = createMockToken({
      sub: 'admin',
      roles: [SUPER_ADMIN_ROLE.name],
      idCompany: 1,
    });
    localStorage.setItem('token', token);

    const result = guard.canActivate();

    expect(result).toBe(true);
    expect(routerSpy.navigate).not.toHaveBeenCalled();
  });

  it('should redirect to login when no token exists', () => {
    localStorage.removeItem('token');

    const result = guard.canActivate();

    expect(result).toBe(false);
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('should redirect to login when token has no roles', () => {
    const token = createMockToken({
      sub: 'user',
      idCompany: 1,
    });
    localStorage.setItem('token', token);

    const result = guard.canActivate();

    expect(result).toBe(false);
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('should redirect to welcome when user is not super_admin', () => {
    const token = createMockToken({
      sub: 'user',
      roles: ['admin', 'member'],
      idCompany: 1,
    });
    localStorage.setItem('token', token);

    const result = guard.canActivate();

    expect(result).toBe(false);
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/welcome']);
  });

  // Note: Testing decodeToken returning null is covered by the "no token" test
  // since decodeToken is an exported function that reads from localStorage

  it('should allow access when user has super_admin role among multiple roles', () => {
    const token = createMockToken({
      sub: 'admin',
      roles: ['admin', SUPER_ADMIN_ROLE.name, 'member'],
      idCompany: 1,
    });
    localStorage.setItem('token', token);

    const result = guard.canActivate();

    expect(result).toBe(true);
    expect(routerSpy.navigate).not.toHaveBeenCalled();
  });
});
