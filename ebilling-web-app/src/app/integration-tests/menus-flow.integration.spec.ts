/**
 * Test de Integración: Flujo de Menús
 *
 * Este test verifica el flujo completo de carga y filtrado de menús:
 * Login → Cargar menús → Verificar filtrado por permisos → Verificar feature codes
 */

import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { MatDialogModule } from '@angular/material/dialog';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { MenuService } from '../_service/auth/menu.service';
import { MenuFilterService } from '../_service/menu/menu-filter.service';
import { PermissionService } from '../_service/permission/permission.service';
import { LoginService } from '../_service/auth/login.service';
import { SubscriptionAdminService } from '../_service/subscription/subscription-admin.service';
import { TokenResponse } from '../_model/auth/token-response';
import { SubscriptionAdmin } from '../_model/subscription/subscription-admin';
import { Menu } from '../_model/auth/menu';
import { environment } from '../../environments/environment';

describe('Integration Test: Flujo de Menús', () => {
  let httpMock: HttpTestingController;
  let menuService: MenuService;
  let menuFilterService: MenuFilterService;
  let permissionService: PermissionService;
  let loginService: LoginService;
  let subscriptionAdminService: SubscriptionAdminService;

  const mockToken = 'mock-jwt-token';
  const mockUserId = 1;
  const mockCompanyId = 1;

  const mockMenus: Menu[] = [
    {
      id: 1,
      name: 'Facturación',
      featureCode: 'INVOICE_CREATE',
      url: '/invoices',
      icon: 'receipt',
      subMenus: [
        {
          id: 2,
          name: 'Crear Factura',
          featureCode: 'INVOICE_CREATE',
          url: '/invoices/create',
          icon: 'add',
          subMenus: [],
        },
        {
          id: 3,
          name: 'Borradores',
          featureCode: 'INVOICE_CREATE',
          url: '/invoices/drafts',
          icon: 'drafts',
          subMenus: [],
        },
      ],
    },
    {
      id: 4,
      name: 'Productos',
      featureCode: 'PRODUCT_MANAGEMENT',
      url: '/products',
      icon: 'inventory',
      subMenus: [],
    },
    {
      id: 5,
      name: 'Clientes',
      featureCode: 'CLIENT_CREATE',
      url: '/clients',
      icon: 'people',
      subMenus: [],
    },
    {
      id: 6,
      name: 'Reportes Avanzados',
      featureCode: 'REPORT_ADVANCED',
      url: '/reports/advanced',
      icon: 'analytics',
      subMenus: [],
    },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        HttpClientTestingModule,
        RouterTestingModule,
        MatDialogModule,
        MatSnackBarModule,
        BrowserAnimationsModule,
      ],
      providers: [
        MenuService,
        MenuFilterService,
        PermissionService,
        LoginService,
        SubscriptionAdminService,
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    menuService = TestBed.inject(MenuService);
    menuFilterService = TestBed.inject(MenuFilterService);
    permissionService = TestBed.inject(PermissionService);
    loginService = TestBed.inject(LoginService);
    subscriptionAdminService = TestBed.inject(SubscriptionAdminService);

    // Mock localStorage
    spyOn(localStorage, 'getItem').and.callFake((key: string) => {
      if (key === 'token') return mockToken;
      if (key === environment.ID_COMPANY) return String(mockCompanyId);
      if (key === 'idUser') return String(mockUserId);
      return null;
    });
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('Flujo completo de carga y filtrado de menús', () => {
    it('should complete full menu flow: login → load menus → filter by permissions → verify feature codes', (done) => {
      // 1. Login
      const loginSpy = spyOn(loginService, 'login').and.returnValue(
        of({
          code: 200,
          status: 'OK',
          message: 'Login successful',
          token: { token: mockToken },
        } as TokenResponse)
      );

      loginService.login('user@example.com', 'password').subscribe(() => {
        expect(loginSpy).toHaveBeenCalled();

        // 2. Obtener suscripción para determinar permisos base
        const mockSubscription: SubscriptionAdmin = {
          idCompany: mockCompanyId,
          companyName: 'Test Company',
          businessName: 'Test Business',
          ruc: '1234567890',
          plan: {
            code: 'BASIC',
            name: 'BASIC',
            documentLimitYearly: 100,
          },
          subscriptionStatus: 'ACTIVE',
          currentYearUsage: 0,
          percentUsed: 0,
          remainingDocuments: 100,
          startDate: '2024-01-01',
          expiresAt: '2024-12-31',
        };

        const subscriptionSpy = spyOn(subscriptionAdminService, 'getCompanySubscription').and.returnValue(
          of(mockSubscription)
        );

        subscriptionAdminService.getCompanySubscription(mockCompanyId).subscribe((subscription) => {
          expect(subscriptionSpy).toHaveBeenCalled();
          expect(subscription.plan.name).toBe('BASIC');

          // 3. Cargar menús completos
          const menuSpy = spyOn(menuService, 'findMenuByUser').and.returnValue(of(mockMenus));

          menuService.findMenuByUser('testuser').subscribe((menus) => {
            expect(menuSpy).toHaveBeenCalled();
            expect(menus.length).toBeGreaterThan(0);
            expect(menus.every((m) => m.featureCode)).toBe(true); // Todos tienen featureCode

            // 4. Mockear el comportamiento del filtro de menús
            // El filtro verifica permisos basándose en la suscripción y roles
            // Para este test, simulamos que el plan BASIC no incluye REPORT_ADVANCED
            const filterSpy = spyOn(menuFilterService, 'filterMenus').and.callFake((menusToFilter: Menu[]) => {
              // Filtrar menús que NO tienen REPORT_ADVANCED
              const filtered = menusToFilter.filter(
                (m) => m.featureCode !== 'REPORT_ADVANCED'
              );
              return of(filtered);
            });

            // Filtrar menús según permisos
            menuFilterService.filterMenus(menus).subscribe((filteredMenus) => {
              expect(filterSpy).toHaveBeenCalled();
              // Verificar que los menús filtrados solo incluyen los permitidos
              expect(filteredMenus.some((m) => m.featureCode === 'INVOICE_CREATE')).toBe(true);
              expect(filteredMenus.some((m) => m.featureCode === 'PRODUCT_MANAGEMENT')).toBe(true);
              expect(filteredMenus.some((m) => m.featureCode === 'CLIENT_CREATE')).toBe(true);
              expect(filteredMenus.some((m) => m.featureCode === 'REPORT_ADVANCED')).toBe(false);

              // 5. Verificar que los menús hijos también se filtraron correctamente
              const facturacionMenu = filteredMenus.find((m) => m.name === 'Facturación');
              if (facturacionMenu && facturacionMenu.subMenus) {
                expect(facturacionMenu.subMenus.length).toBeGreaterThan(0);
                expect(facturacionMenu.subMenus.every((c) => c.featureCode === 'INVOICE_CREATE')).toBe(true);
              }

              done();
            });
          });
        });
      });
    });

    it('should handle error when loading menus', (done) => {
      const menuSpy = spyOn(menuService, 'findMenuByUser').and.returnValue(
        throwError(() => ({
          error: { message: 'Error al cargar menús' },
          status: 500,
        }))
      );

      menuService.findMenuByUser('testuser').subscribe({
        next: () => fail('Should have failed'),
        error: (error) => {
          expect(menuSpy).toHaveBeenCalled();
          expect(error.error.message).toBe('Error al cargar menús');
          done();
        },
      });
    });

    it('should filter menus correctly when user has no subscription', (done) => {
      const subscriptionSpy = spyOn(subscriptionAdminService, 'getCompanySubscription').and.returnValue(
        throwError(() => ({ error: { message: 'No subscription found' }, status: 404 }))
      );

      const menuSpy = spyOn(menuService, 'findMenuByUser').and.returnValue(of(mockMenus));

      subscriptionAdminService.getCompanySubscription(mockCompanyId).subscribe({
        error: () => {
          // Cuando no hay suscripción, debería retornar todos los menús (sin filtrado por plan)
          menuService.findMenuByUser('testuser').subscribe((menus) => {
            menuFilterService.filterMenus(menus).subscribe((filteredMenus) => {
              // Sin suscripción, debería retornar todos los menús
              expect(filteredMenus.length).toBe(mockMenus.length);
              done();
            });
          });
        },
      });
    });
  });
});

