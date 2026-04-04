import { TestBed } from '@angular/core/testing';
import { Router, RouterStateSnapshot } from '@angular/router';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { CanActiveUserGuard } from './can-active-user.guard';
import { LoginService } from '../auth/login.service';
import { MenuService } from '../auth/menu.service';
import { CompanyService } from '../customer/company.service';
import { environment } from '../../../environments/environment';
import { of } from 'rxjs';
import { Menu } from '../../_model/auth/menu';
import { CompanyResponse } from '../../_model/customer/company-response';
import { JwtHelperService } from '@auth0/angular-jwt';
import { Subject } from 'rxjs';

describe('CanActiveUserGuard', () => {
  let guard: CanActiveUserGuard;
  let loginServiceSpy: jasmine.SpyObj<LoginService>;
  let menuServiceSpy: jasmine.SpyObj<MenuService>;
  let companyServiceSpy: jasmine.SpyObj<CompanyService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let httpMock: HttpTestingController;

  const createMockToken = (payload: any): string => {
    const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
    const body = btoa(JSON.stringify(payload));
    return `${header}.${body}.signature`;
  };

  beforeEach(() => {
    const loginService = jasmine.createSpyObj('LoginService', [
      'isLogin',
      'closeSession',
    ]);
    const menuService = jasmine.createSpyObj('MenuService', [
      'findMenuByUser',
      'hasAccessTo',
    ]);
    const companyService = jasmine.createSpyObj('CompanyService', [], {
      companyNameChange: new Subject<string>(),
    });
    const router = jasmine.createSpyObj('Router', ['parseUrl']);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        CanActiveUserGuard,
        { provide: LoginService, useValue: loginService },
        { provide: MenuService, useValue: menuService },
        { provide: CompanyService, useValue: companyService },
        { provide: Router, useValue: router },
      ],
    });

    guard = TestBed.inject(CanActiveUserGuard);
    loginServiceSpy = TestBed.inject(
      LoginService
    ) as jasmine.SpyObj<LoginService>;
    menuServiceSpy = TestBed.inject(MenuService) as jasmine.SpyObj<MenuService>;
    companyServiceSpy = TestBed.inject(
      CompanyService
    ) as jasmine.SpyObj<CompanyService>;
    routerSpy = TestBed.inject(Router) as jasmine.SpyObj<Router>;
    httpMock = TestBed.inject(HttpTestingController);

    routerSpy.parseUrl.and.returnValue({} as any);
  });

  afterEach(() => {
    localStorage.clear();
    if (httpMock) {
      httpMock.verify();
    }
  });

  it('should be created', () => {
    expect(guard).toBeTruthy();
  });

  it('should redirect to login when user is not logged in', (done) => {
    loginServiceSpy.isLogin.and.returnValue(false);

    const route = {} as any;
    const state = { url: '/dashboard' } as RouterStateSnapshot;

    guard.canActivate(route, state).subscribe((result) => {
      expect(result).toEqual({} as any);
      expect(loginServiceSpy.closeSession).toHaveBeenCalled();
      expect(routerSpy.parseUrl).toHaveBeenCalledWith('/login');
      done();
    });
  });

  it('should redirect to login when token is invalid', (done) => {
    loginServiceSpy.isLogin.and.returnValue(true);
    localStorage.removeItem(environment.TOKEN);

    const route = {} as any;
    const state = { url: '/dashboard' } as RouterStateSnapshot;

    guard.canActivate(route, state).subscribe((result) => {
      expect(result).toEqual({} as any);
      expect(loginServiceSpy.closeSession).toHaveBeenCalled();
      expect(routerSpy.parseUrl).toHaveBeenCalledWith('/login');
      done();
    });
  });

  it('should allow access for profile routes without menu validation', (done) => {
    const token = createMockToken({
      userName: 'testuser',
      roles: ['admin'],
      idCompany: 1,
    });
    localStorage.setItem(environment.TOKEN, token);

    loginServiceSpy.isLogin.and.returnValue(true);
    const mockMenus: Menu[] = [
      {
        id: 1,
        name: 'Profile',
        url: '/profile',
        icon: 'person',
        subMenus: [],
      },
    ];
    menuServiceSpy.findMenuByUser.and.returnValue(of(mockMenus));
    menuServiceSpy.menuChange = {
      next: jasmine.createSpy('next'),
    } as any;
    companyServiceSpy.companyNameChange = {
      next: jasmine.createSpy('next'),
    } as any;

    const route = {} as any;
    const state = { url: '/profile' } as RouterStateSnapshot;

    guard.canActivate(route, state).subscribe((result) => {
      expect(result).toBe(true);
      expect(menuServiceSpy.findMenuByUser).toHaveBeenCalledWith('testuser');
      expect(menuServiceSpy.hasAccessTo).not.toHaveBeenCalled();
      done();
    });
  });

  it('should allow access when user has menu access', (done) => {
    const token = createMockToken({
      userName: 'testuser',
      roles: ['admin'],
      idCompany: 1,
    });
    localStorage.setItem(environment.TOKEN, token);

    loginServiceSpy.isLogin.and.returnValue(true);
    const mockMenus: Menu[] = [
      {
        id: 1,
        name: 'Dashboard',
        url: '/dashboard',
        icon: 'dashboard',
        subMenus: [],
      },
    ];
    menuServiceSpy.findMenuByUser.and.returnValue(of(mockMenus));
    menuServiceSpy.hasAccessTo.and.returnValue(true);
    menuServiceSpy.menuChange = {
      next: jasmine.createSpy('next'),
    } as any;
    companyServiceSpy.companyNameChange = {
      next: jasmine.createSpy('next'),
    } as any;

    const route = {} as any;
    const state = { url: '/dashboard' } as RouterStateSnapshot;

    guard.canActivate(route, state).subscribe((result) => {
      expect(result).toBe(true);
      expect(menuServiceSpy.hasAccessTo).toHaveBeenCalledWith('/dashboard');
      done();
    });
  });

  it('should redirect to 403 when user does not have menu access', (done) => {
    const token = createMockToken({
      userName: 'testuser',
      roles: ['admin'],
      idCompany: 1,
    });
    localStorage.setItem(environment.TOKEN, token);

    loginServiceSpy.isLogin.and.returnValue(true);
    const mockMenus: Menu[] = [
      {
        id: 1,
        name: 'Dashboard',
        url: '/dashboard',
        icon: 'dashboard',
        subMenus: [],
      },
    ];
    menuServiceSpy.findMenuByUser.and.returnValue(of(mockMenus));
    menuServiceSpy.hasAccessTo.and.returnValue(false);
    menuServiceSpy.menuChange = {
      next: jasmine.createSpy('next'),
    } as any;

    const route = {} as any;
    const state = { url: '/admin' } as RouterStateSnapshot;

    guard.canActivate(route, state).subscribe((result) => {
      expect(result).toEqual({} as any);
      expect(routerSpy.parseUrl).toHaveBeenCalledWith('/not-403');
      done();
    });
  });

  it('should allow access for change-password routes without menu validation', (done) => {
    const token = createMockToken({
      userName: 'testuser',
      roles: ['admin'],
      idCompany: 1,
    });
    localStorage.setItem(environment.TOKEN, token);

    loginServiceSpy.isLogin.and.returnValue(true);
    const mockMenus: Menu[] = [];
    menuServiceSpy.findMenuByUser.and.returnValue(of(mockMenus));
    menuServiceSpy.menuChange = {
      next: jasmine.createSpy('next'),
    } as any;
    companyServiceSpy.companyNameChange = {
      next: jasmine.createSpy('next'),
    } as any;

    const route = {} as any;
    const state = { url: '/change-password' } as RouterStateSnapshot;

    guard.canActivate(route, state).subscribe((result) => {
      expect(result).toBe(true);
      expect(menuServiceSpy.hasAccessTo).not.toHaveBeenCalled();
      done();
    });
  });

  it('should allow access for setting routes without menu validation', (done) => {
    const token = createMockToken({
      userName: 'testuser',
      roles: ['admin'],
      idCompany: 1,
    });
    localStorage.setItem(environment.TOKEN, token);

    loginServiceSpy.isLogin.and.returnValue(true);
    const mockMenus: Menu[] = [];
    menuServiceSpy.findMenuByUser.and.returnValue(of(mockMenus));
    menuServiceSpy.menuChange = {
      next: jasmine.createSpy('next'),
    } as any;
    companyServiceSpy.companyNameChange = {
      next: jasmine.createSpy('next'),
    } as any;

    const route = {} as any;
    const state = { url: '/setting' } as RouterStateSnapshot;

    guard.canActivate(route, state).subscribe((result) => {
      expect(result).toBe(true);
      expect(menuServiceSpy.hasAccessTo).not.toHaveBeenCalled();
      done();
    });
  });
});

