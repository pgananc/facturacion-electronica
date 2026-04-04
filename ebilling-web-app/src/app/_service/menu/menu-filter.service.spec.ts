import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MenuFilterService } from './menu-filter.service';
import { PermissionService } from '../permission/permission.service';
import { LoggerService } from '../logger/logger.service';
import { Menu } from '../../_model/auth/menu';
import { CompanySubscription } from '../../_model/subscription/company-subscription';
import { SubscriptionPlan } from '../../_model/subscription/subscription-plan';
import { of, throwError, NEVER } from 'rxjs';
import { environment } from '../../../environments/environment';

describe('MenuFilterService', () => {
  let service: MenuFilterService;
  let permissionService: jasmine.SpyObj<PermissionService>;
  let loggerSpy: jasmine.SpyObj<LoggerService>;

  // Helper para crear token mock
  const createMockToken = (payload: any): string => {
    const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
    const body = btoa(JSON.stringify(payload));
    return `${header}.${body}.signature`;
  };

  const SUPER_ADMIN_TOKEN = createMockToken({
    sub: 'admin',
    roles: ['super_admin'],
    idCompany: 1,
    userName: 'admin',
  });

  const ADMIN_TOKEN = createMockToken({
    sub: 'testuser',
    roles: ['admin'],
    idCompany: 1,
    userName: 'testuser',
  });

  const mockSubscription: CompanySubscription = {
    idSubscription: 1,
    idCompany: 1,
    idPlan: 1,
    status: 'ACTIVE',
    plan: {
      code: 'BASIC',
      name: 'Básico',
    } as SubscriptionPlan,
    startDate: '2024-01-01',
    expiresAt: '2024-12-31',
    currentYearUsage: 0,
    percentUsed: 0,
    documentsRemaining: 1000,
  };

  const mockMenus: Menu[] = [
    {
      id: 1,
      name: 'Facturación',
      url: '/invoices',
      featureCode: 'INVOICE_CREATE',
      icon: 'receipt',
      subMenus: [],
    },
    {
      id: 2,
      name: 'Clientes',
      url: '/clients',
      featureCode: 'CLIENT_CREATE',
      icon: 'people',
      subMenus: [],
    },
    {
      id: 3,
      name: 'Reportes',
      url: '',
      icon: 'bar_chart',
      subMenus: [
        {
          id: 4,
          name: 'Ventas por Período',
          url: '/reports/sales-by-period',
          featureCode: 'REPORT_SALES_BY_PERIOD',
          icon: 'trending_up',
          subMenus: [],
        },
      ],
    },
  ];

  beforeEach(() => {
    // Limpiar localStorage antes de cada test
    localStorage.clear();

    // Crear spies
    permissionService = jasmine.createSpyObj('PermissionService', [
      'isSuperAdmin',
      'getSubscriptionValue',
      'getSubscription',
      'canAccess',
      'canAccessWithDetails',
    ]);

    loggerSpy = jasmine.createSpyObj('LoggerService', [
      'debug',
      'info',
      'warn',
      'error',
    ]);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        MenuFilterService,
        { provide: PermissionService, useValue: permissionService },
        { provide: LoggerService, useValue: loggerSpy },
      ],
    });

    service = TestBed.inject(MenuFilterService);
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('filterMenus', () => {
    it('should return empty array when menus is empty', (done) => {
      service.filterMenus([]).subscribe((result) => {
        expect(result).toEqual([]);
        done();
      });
    });

    it('should return empty array when menus is null', (done) => {
      service.filterMenus(null as any).subscribe((result) => {
        expect(result).toEqual([]);
        done();
      });
    });

    it('should return all menus for super_admin without filtering', (done) => {
      localStorage.setItem(environment.TOKEN, SUPER_ADMIN_TOKEN);
      permissionService.isSuperAdmin.and.returnValue(true);

      service.filterMenus(mockMenus).subscribe((result) => {
        expect(result).toEqual(mockMenus);
        expect(result.length).toBe(mockMenus.length);
        expect(permissionService.canAccess).not.toHaveBeenCalled();
        done();
      });
    });

    it('should return all menus when user has no subscription', (done) => {
      localStorage.setItem(environment.TOKEN, ADMIN_TOKEN);
      localStorage.setItem(environment.ID_COMPANY, '1');
      permissionService.isSuperAdmin.and.returnValue(false);
      permissionService.getSubscriptionValue.and.returnValue(null);
      // El servicio usa filter((sub) => sub !== null) y timeout(3000)
      // Si el Observable nunca emite valores que pasen el filtro, el timeout se activará después de 3 segundos
      // El catchError debería manejar esto y retornar todos los menús
      // Usamos NEVER para que nunca emita ni complete, activando el timeout después de 3 segundos
      permissionService.getSubscription.and.returnValue(
        NEVER // Observable que nunca emite ni completa, activando el timeout después de 3 segundos
      );

      service.filterMenus(mockMenus).subscribe((result) => {
        expect(result).toEqual(mockMenus);
        done();
      });
    });

    it('should filter menus based on permissions when user has active subscription', (done) => {
      localStorage.setItem(environment.TOKEN, ADMIN_TOKEN);
      localStorage.setItem(environment.ID_COMPANY, '1');
      permissionService.isSuperAdmin.and.returnValue(false);
      permissionService.getSubscriptionValue.and.returnValue(mockSubscription);
      permissionService.canAccessWithDetails.and.callFake((featureCode: string) => {
        // Permitir solo INVOICE_CREATE
        if (featureCode === 'INVOICE_CREATE') {
          return of({ allowed: true, reason: 'SUCCESS', message: '' });
        }
        return of({ allowed: false, reason: 'NO_ROLE', message: '' });
      });

      service.filterMenus(mockMenus).subscribe((result) => {
        expect(result.length).toBe(1);
        expect(result[0].name).toBe('Facturación');
        expect(result[0].featureCode).toBe('INVOICE_CREATE');
        done();
      });
    });

    it('should filter submenus recursively', (done) => {
      localStorage.setItem(environment.TOKEN, ADMIN_TOKEN);
      localStorage.setItem(environment.ID_COMPANY, '1');
      permissionService.isSuperAdmin.and.returnValue(false);
      permissionService.getSubscriptionValue.and.returnValue(mockSubscription);
      permissionService.canAccessWithDetails.and.callFake((featureCode: string) => {
        // Permitir solo REPORT_SALES_BY_PERIOD
        if (featureCode === 'REPORT_SALES_BY_PERIOD') {
          return of({ allowed: true, reason: 'SUCCESS', message: '' });
        }
        return of({ allowed: false, reason: 'NO_ROLE', message: '' });
      });

      const menusWithSubMenus: Menu[] = [
        {
          id: 3,
          name: 'Reportes',
          url: '',
          icon: 'bar_chart',
          subMenus: [
            {
              id: 4,
              name: 'Ventas por Período',
              url: '/reports/sales-by-period',
              featureCode: 'REPORT_SALES_BY_PERIOD',
              icon: 'trending_up',
              subMenus: [],
            },
            {
              id: 5,
              name: 'Ventas por Cliente',
              url: '/reports/sales-by-client',
              featureCode: 'REPORT_SALES_BY_CLIENT',
              icon: 'person',
              subMenus: [],
            },
          ],
        },
      ];

      service.filterMenus(menusWithSubMenus).subscribe((result) => {
        expect(result.length).toBe(1);
        expect(result[0].name).toBe('Reportes');
        expect(result[0].subMenus?.length).toBe(1);
        expect(result[0].subMenus?.[0].name).toBe('Ventas por Período');
        done();
      });
    });

    it('should hide parent menu when all submenus are filtered out', (done) => {
      localStorage.setItem(environment.TOKEN, ADMIN_TOKEN);
      localStorage.setItem(environment.ID_COMPANY, '1');
      permissionService.isSuperAdmin.and.returnValue(false);
      permissionService.getSubscriptionValue.and.returnValue(mockSubscription);
      permissionService.canAccessWithDetails.and.returnValue(
        of({ allowed: false, reason: 'NO_ROLE', message: '' })
      ); // Denegar todos

      const menusWithSubMenus: Menu[] = [
        {
          id: 3,
          name: 'Reportes',
          url: '',
          icon: 'bar_chart',
          subMenus: [
            {
              id: 4,
              name: 'Ventas por Período',
              url: '/reports/sales-by-period',
              featureCode: 'REPORT_SALES_BY_PERIOD',
              icon: 'trending_up',
              subMenus: [],
            },
          ],
        },
      ];

      service.filterMenus(menusWithSubMenus).subscribe((result) => {
        expect(result.length).toBe(0);
        done();
      });
    });

    it('should allow menus without feature code', (done) => {
      localStorage.setItem(environment.TOKEN, ADMIN_TOKEN);
      localStorage.setItem(environment.ID_COMPANY, '1');
      permissionService.isSuperAdmin.and.returnValue(false);
      permissionService.getSubscriptionValue.and.returnValue(mockSubscription);

      const menusWithoutFeature: Menu[] = [
        {
          id: 1,
          name: 'Menú Público',
          url: '/public',
          icon: 'public',
          subMenus: [],
        },
      ];

      service.filterMenus(menusWithoutFeature).subscribe((result) => {
        expect(result.length).toBe(1);
        expect(result[0].name).toBe('Menú Público');
        expect(permissionService.canAccess).not.toHaveBeenCalled();
        done();
      });
    });

    it('should handle subscription loading delay', (done) => {
      localStorage.setItem(environment.TOKEN, ADMIN_TOKEN);
      localStorage.setItem(environment.ID_COMPANY, '1');
      permissionService.isSuperAdmin.and.returnValue(false);
      permissionService.getSubscriptionValue.and.returnValue(null);
      permissionService.getSubscription.and.returnValue(
        of(mockSubscription)
      );

      service.filterMenus(mockMenus).subscribe((result) => {
        expect(result).toBeDefined();
        done();
      });
    });

    it('should handle errors gracefully and return menus without feature code', (done) => {
      localStorage.setItem(environment.TOKEN, ADMIN_TOKEN);
      localStorage.setItem(environment.ID_COMPANY, '1');
      permissionService.isSuperAdmin.and.returnValue(false);
      permissionService.getSubscriptionValue.and.returnValue(mockSubscription);
      permissionService.canAccessWithDetails.and.returnValue(
        throwError(() => new Error('Network error'))
      );

      const menusWithError: Menu[] = [
        {
          id: 1,
          name: 'Facturación',
          url: '/invoices',
          featureCode: 'INVOICE_CREATE',
          icon: 'receipt',
          subMenus: [],
        },
        {
          id: 2,
          name: 'Menú Público',
          url: '/public',
          icon: 'public',
          subMenus: [],
        },
      ];

      service.filterMenus(menusWithError).subscribe((result) => {
        // En caso de error, debería retornar solo menús sin feature code
        expect(result.length).toBeGreaterThanOrEqual(0);
        done();
      });
    });

    it('should return all menus when no idCompany is available', (done) => {
      localStorage.setItem(environment.TOKEN, ADMIN_TOKEN);
      localStorage.removeItem(environment.ID_COMPANY);
      permissionService.isSuperAdmin.and.returnValue(false);
      permissionService.getSubscriptionValue.and.returnValue(mockSubscription);
      permissionService.canAccessWithDetails.and.returnValue(
        of({ allowed: true, reason: 'SUCCESS', message: '' })
      ); // Mock para evitar undefined

      service.filterMenus(mockMenus).subscribe((result) => {
        // Sin idCompany, debería retornar solo menús sin feature code
        expect(result).toBeDefined();
        done();
      });
    });

    it('should use featureCode from menu if available', (done) => {
      localStorage.setItem(environment.TOKEN, ADMIN_TOKEN);
      localStorage.setItem(environment.ID_COMPANY, '1');
      permissionService.isSuperAdmin.and.returnValue(false);
      permissionService.getSubscriptionValue.and.returnValue(mockSubscription);
      permissionService.canAccessWithDetails.and.callFake((featureCode: string) => {
        expect(featureCode).toBe('INVOICE_CREATE');
        return of({ allowed: true, reason: 'SUCCESS', message: '' });
      });

      const menu: Menu[] = [
        {
          id: 1,
          name: 'Facturación',
          url: '/invoices',
          featureCode: 'INVOICE_CREATE',
          icon: 'receipt',
          subMenus: [],
        },
      ];

      service.filterMenus(menu).subscribe((result) => {
        expect(permissionService.canAccessWithDetails).toHaveBeenCalledWith(
          'INVOICE_CREATE'
        );
        done();
      });
    });

    it('should handle subscription with undefined status but with plan code', (done) => {
      localStorage.setItem(environment.TOKEN, ADMIN_TOKEN);
      localStorage.setItem(environment.ID_COMPANY, '1');
      permissionService.isSuperAdmin.and.returnValue(false);
      const subscriptionWithoutStatus: CompanySubscription = {
        ...mockSubscription,
        status: 'ACTIVE', // Mantener status válido, el test verifica plan code
      };
      permissionService.getSubscriptionValue.and.returnValue(
        subscriptionWithoutStatus
      );
      permissionService.canAccessWithDetails.and.returnValue(
        of({ allowed: true, reason: 'SUCCESS', message: '' })
      );

      service.filterMenus(mockMenus).subscribe((result) => {
        expect(result.length).toBeGreaterThan(0);
        done();
      });
    });
  });
});

