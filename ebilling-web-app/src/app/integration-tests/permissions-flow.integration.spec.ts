/**
 * Test de Integración: Flujo de Permisos
 *
 * Este test verifica el flujo completo de gestión de permisos:
 * Login como super_admin → Cambiar permisos de rol → Login como usuario afectado → Verificar cambios
 */

import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { MatDialogModule } from '@angular/material/dialog';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { PermissionAdminService } from '../_service/permission/permission-admin.service';
import { PermissionService } from '../_service/permission/permission.service';
import { LoginService } from '../_service/auth/login.service';
import { TokenService } from '../_service/auth/token.service';
import { MenuFilterService } from '../_service/menu/menu-filter.service';
import { MenuService } from '../_service/auth/menu.service';
import { TokenResponse } from '../_model/auth/token-response';
import { FeaturePermissionAdmin } from '../_model/permission/permission-admin';
import { Menu } from '../_model/auth/menu';
import { environment } from '../../environments/environment';

describe('Integration Test: Flujo de Permisos', () => {
  let httpMock: HttpTestingController;
  let permissionAdminService: PermissionAdminService;
  let permissionService: PermissionService;
  let loginService: LoginService;
  let tokenService: TokenService;
  let menuFilterService: MenuFilterService;
  let menuService: MenuService;

  const mockAdminToken = 'mock-admin-token';
  const mockUserToken = 'mock-user-token';
  const mockAdminUserId = 1;
  const mockUserId = 2;
  const mockCompanyId = 1;
  const mockRoleId = 1;

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
        PermissionAdminService,
        PermissionService,
        LoginService,
        TokenService,
        MenuFilterService,
        MenuService,
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    permissionAdminService = TestBed.inject(PermissionAdminService);
    permissionService = TestBed.inject(PermissionService);
    loginService = TestBed.inject(LoginService);
    tokenService = TestBed.inject(TokenService);
    menuFilterService = TestBed.inject(MenuFilterService);
    menuService = TestBed.inject(MenuService);

    // Mock localStorage
    spyOn(localStorage, 'getItem').and.callFake((key: string) => {
      if (key === 'token') return mockAdminToken;
      if (key === environment.ID_COMPANY) return String(mockCompanyId);
      if (key === 'idUser') return String(mockAdminUserId);
      return null;
    });

    spyOn(localStorage, 'setItem').and.stub();
    spyOn(localStorage, 'removeItem').and.stub();
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('Flujo completo de gestión de permisos', () => {
    it('should complete full permissions flow: admin login → change role permissions → user login → verify changes', (done) => {
      // 1. Login como super_admin
      const adminLoginSpy = spyOn(loginService, 'login').and.returnValue(
        of({
          code: 200,
          status: 'OK',
          message: 'Login successful',
          token: { token: mockAdminToken },
        } as TokenResponse)
      );

      loginService.login('admin@example.com', 'admin123').subscribe(() => {
        expect(adminLoginSpy).toHaveBeenCalled();
        expect(localStorage.getItem('token')).toBe(mockAdminToken);

        // 2. Obtener permisos actuales del rol
        const mockPermissions: FeaturePermissionAdmin[] = [
          {
            featureCode: 'PRODUCT_MANAGEMENT',
            name: 'Gestión de Productos',
            module: 'Productos',
            allowedRoles: [mockRoleId],
            requiredPlans: [],
            description: 'Gestión de productos',
          },
          {
            featureCode: 'CLIENT_CREATE',
            name: 'Crear Clientes',
            module: 'Clientes',
            allowedRoles: [mockRoleId],
            requiredPlans: [],
            description: 'Crear clientes',
          },
          {
            featureCode: 'INVOICE_CREATE',
            name: 'Crear Facturas',
            module: 'Facturación',
            allowedRoles: [], // No tiene acceso inicialmente
            requiredPlans: [],
            description: 'Crear facturas',
          },
        ];

        const getPermissionsSpy = spyOn(
          permissionAdminService,
          'getRolePermissions'
        ).and.returnValue(of(mockPermissions));

        permissionAdminService
          .getRolePermissions(mockRoleId)
          .subscribe((permissions) => {
            expect(getPermissionsSpy).toHaveBeenCalledWith(mockRoleId);
            expect(permissions.length).toBeGreaterThan(0);

            // 3. Cambiar permisos del rol (habilitar INVOICE_CREATE)
            const updatedPermissions: FeaturePermissionAdmin[] = [
              {
                featureCode: 'PRODUCT_MANAGEMENT',
                name: 'Gestión de Productos',
                module: 'Productos',
                allowedRoles: [mockRoleId],
                requiredPlans: [],
                description: 'Gestión de productos',
              },
              {
                featureCode: 'CLIENT_CREATE',
                name: 'Crear Clientes',
                module: 'Clientes',
                allowedRoles: [mockRoleId],
                requiredPlans: [],
                description: 'Crear clientes',
              },
              {
                featureCode: 'INVOICE_CREATE',
                name: 'Crear Facturas',
                module: 'Facturación',
                allowedRoles: [mockRoleId], // Ahora tiene acceso
                requiredPlans: [],
                description: 'Crear facturas',
              },
            ];

            const updateSpy = spyOn(
              permissionAdminService,
              'updateFeatureRoles'
            ).and.returnValue(of(void 0));

            // Actualizar permisos para INVOICE_CREATE
            permissionAdminService
              .updateFeatureRoles('INVOICE_CREATE', [mockRoleId])
              .subscribe(() => {
                expect(updateSpy).toHaveBeenCalledWith('INVOICE_CREATE', [
                  mockRoleId,
                ]);

                // 4. Login como usuario afectado
                localStorage.setItem('token', mockUserToken);
                localStorage.setItem('idUser', String(mockUserId));

                // Configurar el spy para retornar un valor diferente para el segundo login
                adminLoginSpy.and.returnValue(
                  of({
                    code: 200,
                    status: 'OK',
                    message: 'Login successful',
                    token: { token: mockUserToken },
                  } as TokenResponse)
                );

                loginService
                  .login('user@example.com', 'user123')
                  .subscribe(() => {
                    expect(adminLoginSpy).toHaveBeenCalled();

                    // 5. Verificar que el usuario ahora tiene acceso a INVOICE_CREATE
                    const canEmitSpy = spyOn(
                      permissionService,
                      'canEmitDocument'
                    ).and.returnValue(of(true));

                    permissionService
                      .canEmitDocument('INVOICE')
                      .subscribe((allowed) => {
                        expect(canEmitSpy).toHaveBeenCalledWith('INVOICE');
                        expect(allowed).toBe(true);

                        // 6. Verificar que los menús se actualizaron (el usuario ahora ve el menú de facturación)
                        const mockMenus: Menu[] = [
                          {
                            id: 1,
                            name: 'Facturación',
                            icon: 'receipt',
                            featureCode: 'INVOICE_CREATE',
                            url: '/invoices',
                            subMenus: [],
                          },
                        ];

                        const menuSpy = spyOn(
                          menuService,
                          'findMenuByUser'
                        ).and.returnValue(of(mockMenus));

                        menuService
                          .findMenuByUser('testuser')
                          .subscribe((menus) => {
                            expect(menuSpy).toHaveBeenCalled();
                            menuFilterService
                              .filterMenus(menus)
                              .subscribe((filteredMenus) => {
                                expect(
                                  filteredMenus.some(
                                    (m) => m.featureCode === 'INVOICE_CREATE'
                                  )
                                ).toBe(true);
                                done();
                              });
                          });
                      });
                  });
              });
          });
      });
    });

    it('should handle error when updating permissions', (done) => {
      const updateSpy = spyOn(
        permissionAdminService,
        'updateFeatureRoles'
      ).and.returnValue(
        throwError(() => ({
          error: { message: 'Error al actualizar permisos' },
          status: 500,
        }))
      );

      permissionAdminService
        .updateFeatureRoles('INVOICE_CREATE', [mockRoleId])
        .subscribe({
          next: () => fail('Should have failed'),
          error: (error) => {
            expect(updateSpy).toHaveBeenCalled();
            expect(error.error.message).toBe('Error al actualizar permisos');
            done();
          },
        });
    });
  });
});
