import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { MenuService } from './menu.service';
import { environment } from '../../../environments/environment';
import { Menu } from '../../_model/auth/menu';

describe('MenuService', () => {
  let service: MenuService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.HOST_AUTH}/api/menu`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [MenuService],
    });

    service = TestBed.inject(MenuService);
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

  describe('findMenuByUser', () => {
    it('should get menus by username', () => {
      const mockMenus: Menu[] = [
        {
          id: 1,
          name: 'Dashboard',
          url: '/dashboard',
          icon: 'dashboard',
          subMenus: [],
        },
      ];

      service.findMenuByUser('testuser').subscribe((menus) => {
        expect(menus).toEqual(mockMenus);
      });

      const req = httpMock.expectOne(`${baseUrl}/testuser`);
      expect(req.request.method).toBe('GET');
      req.flush(mockMenus);
    });
  });

  describe('hasAccessTo', () => {
    it('should return true if user has access to URL', () => {
      const menus: Menu[] = [
        {
          id: 1,
          name: 'Dashboard',
          url: '/dashboard',
          icon: 'dashboard',
          subMenus: [],
        },
        {
          id: 2,
          name: 'Billing',
          url: '/billing',
          icon: 'receipt',
          subMenus: [],
        },
      ];

      service.menuChange.next(menus);
      const hasAccess = service.hasAccessTo('/dashboard');
      expect(hasAccess).toBe(true);
    });

    it('should return false if user does not have access to URL', () => {
      const menus: Menu[] = [
        {
          id: 1,
          name: 'Dashboard',
          url: '/dashboard',
          icon: 'dashboard',
          subMenus: [],
        },
      ];

      service.menuChange.next(menus);
      const hasAccess = service.hasAccessTo('/admin');
      expect(hasAccess).toBe(false);
    });

    it('should check access in submenus', () => {
      const menus: Menu[] = [
        {
          id: 1,
          name: 'Reports',
          url: '/reports',
          icon: 'bar_chart',
          subMenus: [
            {
              id: 2,
              name: 'Sales Report',
              url: 'sales',
              icon: 'trending_up',
              subMenus: [],
            },
          ],
        },
      ];

      service.menuChange.next(menus);
      const hasAccess = service.hasAccessTo('/reports/sales');
      expect(hasAccess).toBe(true);
    });

    it('should handle null URLs', () => {
      const menus: Menu[] = [
        {
          id: 1,
          name: 'Menu',
          url: 'null',
          icon: 'menu',
          subMenus: [],
        },
      ];

      service.menuChange.next(menus);
      const hasAccess = service.hasAccessTo('/some-url');
      expect(hasAccess).toBe(false);
    });

    it('should handle relative URLs in reports context', () => {
      const menus: Menu[] = [
        {
          id: 1,
          name: 'Reports',
          url: '/reports',
          icon: 'bar_chart',
          subMenus: [
            {
              id: 2,
              name: 'Sales',
              url: 'sales',
              icon: 'trending_up',
              subMenus: [],
            },
          ],
        },
      ];

      service.menuChange.next(menus);
      const hasAccess = service.hasAccessTo('/reports/sales');
      expect(hasAccess).toBe(true);
    });

    it('should match URLs that start with menu URL', () => {
      const menus: Menu[] = [
        {
          id: 1,
          name: 'Billing',
          url: '/billing',
          icon: 'receipt',
          subMenus: [],
        },
      ];

      service.menuChange.next(menus);
      const hasAccess = service.hasAccessTo('/billing/create');
      expect(hasAccess).toBe(true);
    });
  });

  describe('menuChange BehaviorSubject', () => {
    it('should emit menu changes', () => {
      const menus: Menu[] = [
        {
          id: 1,
          name: 'Dashboard',
          url: '/dashboard',
          icon: 'dashboard',
          subMenus: [],
        },
      ];

      // BehaviorSubject emite el valor inicial, así que verificamos después de emitir
      service.menuChange.next(menus);
      
      // Verificar el valor actual después de emitir
      const currentMenus = service.menuChange.getValue();
      expect(currentMenus).toEqual(menus);
    });

    it('should get current menu value', () => {
      const menus: Menu[] = [
        {
          id: 1,
          name: 'Dashboard',
          url: '/dashboard',
          icon: 'dashboard',
          subMenus: [],
        },
      ];

      service.menuChange.next(menus);
      const currentMenus = service.menuChange.getValue();
      expect(currentMenus).toEqual(menus);
    });
  });

  describe('isLoginChange Subject', () => {
    it('should emit login status changes', (done) => {
      service.isLoginChange.subscribe((isLogin) => {
        expect(isLogin).toBe(true);
        done();
      });

      service.isLoginChange.next(true);
    });
  });

  describe('openSidenavTrigger Subject', () => {
    it('should emit sidenav open trigger', (done) => {
      service.openSidenavTrigger.subscribe(() => {
        done();
      });

      service.openSidenavTrigger.next();
    });
  });
});

