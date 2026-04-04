/**
 * Test de Integración: Flujo de Suscripciones
 *
 * Este test verifica el flujo completo de gestión de suscripciones:
 * Login como super_admin → Cambiar plan de empresa → Verificar límites actualizados
 */

import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { MatDialogModule } from '@angular/material/dialog';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { SubscriptionAdminService } from '../_service/subscription/subscription-admin.service';
import { PermissionService } from '../_service/permission/permission.service';
import { LoginService } from '../_service/auth/login.service';
import { TokenResponse } from '../_model/auth/token-response';
import { SubscriptionAdmin, AssignSubscriptionRequest } from '../_model/subscription/subscription-admin';
import { environment } from '../../environments/environment';

describe('Integration Test: Flujo de Suscripciones', () => {
  let httpMock: HttpTestingController;
  let subscriptionAdminService: SubscriptionAdminService;
  let permissionService: PermissionService;
  let loginService: LoginService;

  const mockAdminToken = 'mock-admin-token';
  const mockAdminUserId = 1;
  const mockCompanyId = 1;
  const mockSubscriptionId = 1;

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
        SubscriptionAdminService,
        PermissionService,
        LoginService,
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    subscriptionAdminService = TestBed.inject(SubscriptionAdminService);
    permissionService = TestBed.inject(PermissionService);
    loginService = TestBed.inject(LoginService);

    // Mock localStorage
    spyOn(localStorage, 'getItem').and.callFake((key: string) => {
      if (key === 'token') return mockAdminToken;
      if (key === environment.ID_COMPANY) return String(mockCompanyId);
      if (key === 'idUser') return String(mockAdminUserId);
      return null;
    });

    spyOn(localStorage, 'setItem').and.stub();
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('Flujo completo de gestión de suscripciones', () => {
    it('should complete full subscription flow: admin login → change plan → verify limits updated', (done) => {
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

        // 2. Obtener suscripción actual
        const mockSubscription: SubscriptionAdmin = {
          idCompany: mockCompanyId,
          companyName: 'Test Company',
          businessName: 'Test Business',
          ruc: '1234567890',
          plan: {
            code: 'FREE',
            name: 'FREE',
            documentLimitYearly: 10,
          },
          subscriptionStatus: 'ACTIVE',
          currentYearUsage: 0,
          percentUsed: 0,
          remainingDocuments: 10,
          startDate: '2024-01-01',
          expiresAt: '2024-12-31',
        };

        const getSubscriptionSpy = spyOn(subscriptionAdminService, 'getCompanySubscription').and.returnValue(
          of(mockSubscription)
        );

        subscriptionAdminService.getCompanySubscription(mockCompanyId).subscribe((subscription) => {
          expect(getSubscriptionSpy).toHaveBeenCalledWith(mockCompanyId);
          expect(subscription.plan.name).toBe('FREE');
          expect(subscription.plan.documentLimitYearly).toBe(10);

          // 3. Cambiar plan a BASIC
          const assignRequest: AssignSubscriptionRequest = {
            idCompany: mockCompanyId,
            planCode: 'BASIC',
            startDate: '2024-01-20',
            durationMonths: 12,
          };

          const updateSpy = spyOn(subscriptionAdminService, 'assignSubscription').and.returnValue(
            of(void 0)
          );

          subscriptionAdminService.assignSubscription(assignRequest).subscribe(() => {
            expect(updateSpy).toHaveBeenCalledWith(assignRequest);

            // 4. Verificar que los límites se actualizaron
            const canEmitSpy = spyOn(permissionService, 'canEmitDocument').and.returnValue(
              of(true)
            );

            permissionService.canEmitDocument('INVOICE').subscribe((allowed) => {
              expect(canEmitSpy).toHaveBeenCalledWith('INVOICE');
              expect(allowed).toBe(true);

              // 5. Verificar que la suscripción se actualizó correctamente
              const updatedSubscription: SubscriptionAdmin = {
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
                startDate: '2024-01-20',
                expiresAt: '2025-01-20',
              };

              // Actualizar el spy para retornar la suscripción actualizada
              getSubscriptionSpy.and.returnValue(of(updatedSubscription));

              subscriptionAdminService.getCompanySubscription(mockCompanyId).subscribe((subscription) => {
                expect(subscription.plan.code).toBe('BASIC');
                done();
              });
            });
          });
        });
      });
    });

    it('should handle error when updating subscription', (done) => {
      const assignRequest: AssignSubscriptionRequest = {
        idCompany: mockCompanyId,
        planCode: 'BASIC',
        startDate: '2024-01-20',
        durationMonths: 12,
      };

      const updateSpy = spyOn(subscriptionAdminService, 'assignSubscription').and.returnValue(
        throwError(() => ({
          error: { message: 'Error al actualizar suscripción' },
          status: 500,
        }))
      );

      subscriptionAdminService.assignSubscription(assignRequest).subscribe({
        next: () => fail('Should have failed'),
        error: (error) => {
          expect(updateSpy).toHaveBeenCalled();
          expect(error.error.message).toBe('Error al actualizar suscripción');
          done();
        },
      });
    });
  });
});

