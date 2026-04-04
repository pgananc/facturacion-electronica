import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { PermissionGuard } from './permission.guard';
import { PermissionService } from '../_service/permission/permission.service';
import { ToastService } from '../_service/notification/toast.service';
import { MenuService } from '../_service/auth/menu.service';
import { Menu } from '../_model/auth/menu';
import { CompanySubscription } from '../_model/subscription/company-subscription';
import { SubscriptionPlan } from '../_model/subscription/subscription-plan';
import {
  createMockCompanySubscription,
  createMockSubscriptionPlan,
} from '../_test-helpers/mock-data';
import { FeatureValidationResponse } from '../_model/subscription/feature-validation-response';
import { of, Observable, BehaviorSubject } from 'rxjs';

describe('PermissionGuard', () => {
  let guard: PermissionGuard;
  let permissionServiceSpy: jasmine.SpyObj<PermissionService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let toastServiceSpy: jasmine.SpyObj<ToastService>;
  let route: ActivatedRouteSnapshot;
  let state: RouterStateSnapshot;

  beforeEach(() => {
    const permissionService = jasmine.createSpyObj('PermissionService', [
      'isSuperAdmin',
      'getSubscriptionValue',
      'canAccessWithDetails',
    ]);
    const router = jasmine.createSpyObj('Router', ['createUrlTree']);
    const toastService = jasmine.createSpyObj('ToastService', [
      'warning',
      'error',
    ]);
    const menuService = {
      menuChange: new BehaviorSubject<any[]>([]),
    };

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        PermissionGuard,
        { provide: PermissionService, useValue: permissionService },
        { provide: Router, useValue: router },
        { provide: ToastService, useValue: toastService },
        { provide: MenuService, useValue: menuService },
      ],
    });

    guard = TestBed.inject(PermissionGuard);
    permissionServiceSpy = TestBed.inject(
      PermissionService
    ) as jasmine.SpyObj<PermissionService>;
    routerSpy = TestBed.inject(Router) as jasmine.SpyObj<Router>;
    toastServiceSpy = TestBed.inject(
      ToastService
    ) as jasmine.SpyObj<ToastService>;

    route = {
      data: {},
    } as ActivatedRouteSnapshot;

    state = {
      url: '/test',
    } as RouterStateSnapshot;

    // Mock createUrlTree
    routerSpy.createUrlTree.and.returnValue({} as any);
  });

  it('should be created', () => {
    expect(guard).toBeTruthy();
  });

  it('should allow access when no permission is required', () => {
    route.data = {};

    const result = guard.canActivate(route, state);

    expect(result).toBeTrue();
  });

  it('should allow access for super_admin without validation', () => {
    route.data = { permission: 'TEST_FEATURE' };
    permissionServiceSpy.isSuperAdmin.and.returnValue(true);

    const result = guard.canActivate(route, state);

    expect(result).toBeTrue();
    expect(permissionServiceSpy.canAccessWithDetails).not.toHaveBeenCalled();
  });

  it('should allow access when user has no active subscription', () => {
    route.data = { permission: 'TEST_FEATURE' };
    permissionServiceSpy.isSuperAdmin.and.returnValue(false);
    permissionServiceSpy.getSubscriptionValue.and.returnValue(null);

    const result = guard.canActivate(route, state);

    expect(result).toBeTrue();
    expect(permissionServiceSpy.canAccessWithDetails).not.toHaveBeenCalled();
  });

  it('should allow access when subscription is not ACTIVE', () => {
    route.data = { permission: 'TEST_FEATURE' };
    permissionServiceSpy.isSuperAdmin.and.returnValue(false);
    const inactiveSubscription = createMockCompanySubscription({
      status: 'EXPIRED',
    });
    permissionServiceSpy.getSubscriptionValue.and.returnValue(
      inactiveSubscription
    );

    const result = guard.canActivate(route, state);

    expect(result).toBeTrue();
    expect(permissionServiceSpy.canAccessWithDetails).not.toHaveBeenCalled();
  });

  it('should allow access when permission is granted', (done) => {
    route.data = { permission: 'TEST_FEATURE' };
    permissionServiceSpy.isSuperAdmin.and.returnValue(false);
    const activeSubscription = createMockCompanySubscription();
    permissionServiceSpy.getSubscriptionValue.and.returnValue(
      activeSubscription
    );

    const mockResponse: FeatureValidationResponse = {
      allowed: true,
      reason: 'SUCCESS',
      message: 'Access granted',
    };
    permissionServiceSpy.canAccessWithDetails.and.returnValue(of(mockResponse));

    const result = guard.canActivate(route, state);

    if (typeof result === 'boolean') {
      expect(result).toBeTrue();
      done();
    } else if (result instanceof Observable) {
      result.subscribe((allowed: boolean | any) => {
        expect(allowed).toBeTrue();
        expect(toastServiceSpy.warning).not.toHaveBeenCalled();
        expect(toastServiceSpy.error).not.toHaveBeenCalled();
        done();
      });
    } else {
      // UrlTree case
      expect(result).toBeTruthy();
      done();
    }
  });

  it('should deny access and show warning for PLAN_DOES_NOT_INCLUDE_FEATURE', (done) => {
    route.data = { permission: 'PREMIUM_FEATURE' };
    permissionServiceSpy.isSuperAdmin.and.returnValue(false);
    const activeSubscription = createMockCompanySubscription({
      status: 'ACTIVE',
      plan: createMockSubscriptionPlan({
        code: 'BASIC',
        documentLimitYearly: 500,
      }),
    });
    permissionServiceSpy.getSubscriptionValue.and.returnValue(
      activeSubscription
    );

    const mockResponse: FeatureValidationResponse = {
      allowed: false,
      reason: 'PLAN_DOES_NOT_INCLUDE_FEATURE',
      message: 'Feature not available in current plan',
    };
    permissionServiceSpy.canAccessWithDetails.and.returnValue(of(mockResponse));

    // Mock menuService para que retorne un menú que coincida con la URL
    // Esto hace que el guard muestre el toast en lugar de redirigir silenciosamente
    const menuService = TestBed.inject(MenuService) as MenuService;
    const mockMenu: Menu[] = [
      {
        id: 1,
        name: 'Premium Feature',
        url: state.url,
        featureCode: 'PREMIUM_FEATURE',
        icon: 'star',
        subMenus: [],
      },
    ];
    spyOn(menuService.menuChange, 'getValue').and.returnValue(mockMenu);

    const result = guard.canActivate(route, state);

    if (typeof result === 'boolean') {
      expect(result).toBeFalse();
      done();
    } else if (result instanceof Observable) {
      result.subscribe((allowed: boolean | any) => {
        expect(allowed).not.toBeTrue();
        expect(toastServiceSpy.warning).toHaveBeenCalledWith(
          '🔒 Esta funcionalidad requiere un plan superior. Actualiza tu suscripción.'
        );
        expect(routerSpy.createUrlTree).toHaveBeenCalledWith(['/welcome']);
        done();
      });
    } else {
      // UrlTree case
      expect(result).toBeTruthy();
      done();
    }
  });

  it('should deny access and show error for EXPIRED subscription', (done) => {
    route.data = { permission: 'TEST_FEATURE' };
    permissionServiceSpy.isSuperAdmin.and.returnValue(false);
    const activeSubscription = createMockCompanySubscription({
      status: 'ACTIVE',
    });
    permissionServiceSpy.getSubscriptionValue.and.returnValue(
      activeSubscription
    );

    const mockResponse: FeatureValidationResponse = {
      allowed: false,
      reason: 'EXPIRED',
      message: 'Subscription has expired',
    };
    permissionServiceSpy.canAccessWithDetails.and.returnValue(of(mockResponse));

    // Mock menuService para que retorne un menú que coincida con la URL
    const menuService = TestBed.inject(MenuService) as MenuService;
    const mockMenu: Menu[] = [
      {
        id: 1,
        name: 'Test Feature',
        url: state.url,
        featureCode: 'TEST_FEATURE',
        icon: 'test',
        subMenus: [],
      },
    ];
    spyOn(menuService.menuChange, 'getValue').and.returnValue(mockMenu);

    const result = guard.canActivate(route, state);

    if (typeof result === 'boolean') {
      expect(result).toBeFalse();
      done();
    } else if (result instanceof Observable) {
      result.subscribe((allowed: boolean | any) => {
        expect(allowed).not.toBeTrue();
        expect(toastServiceSpy.error).toHaveBeenCalledWith(
          '❌ Tu suscripción ha expirado. Renueva tu plan.'
        );
        done();
      });
    } else {
      // UrlTree case
      expect(result).toBeTruthy();
      done();
    }
  });

  it('should deny access and show warning for LIMIT_EXCEEDED', (done) => {
    route.data = { permission: 'INVOICE_CREATE' };
    permissionServiceSpy.isSuperAdmin.and.returnValue(false);
    const activeSubscription = createMockCompanySubscription({
      status: 'ACTIVE',
      plan: createMockSubscriptionPlan({
        code: 'BASIC',
        documentLimitYearly: 500,
      }),
    });
    permissionServiceSpy.getSubscriptionValue.and.returnValue(
      activeSubscription
    );

    const mockResponse: FeatureValidationResponse = {
      allowed: false,
      reason: 'LIMIT_EXCEEDED',
      message: 'Document limit exceeded',
    };
    permissionServiceSpy.canAccessWithDetails.and.returnValue(of(mockResponse));

    // Mock menuService para que retorne un menú que coincida con la URL
    const menuService = TestBed.inject(MenuService) as MenuService;
    const mockMenu: Menu[] = [
      {
        id: 1,
        name: 'Invoice Create',
        url: state.url,
        featureCode: 'INVOICE_CREATE',
        icon: 'receipt',
        subMenus: [],
      },
    ];
    spyOn(menuService.menuChange, 'getValue').and.returnValue(mockMenu);

    const result = guard.canActivate(route, state);

    if (typeof result === 'boolean') {
      expect(result).toBeFalse();
      done();
    } else if (result instanceof Observable) {
      result.subscribe((allowed: boolean | any) => {
        expect(allowed).not.toBeTrue();
        expect(toastServiceSpy.warning).toHaveBeenCalledWith(
          '⚠️ Has alcanzado el límite de tu plan. Actualiza tu suscripción.'
        );
        done();
      });
    } else {
      // UrlTree case
      expect(result).toBeTruthy();
      done();
    }
  });

  it('should deny access and show error for NO_ROLE', (done) => {
    route.data = { permission: 'ADMIN_FEATURE' };
    permissionServiceSpy.isSuperAdmin.and.returnValue(false);
    const activeSubscription = createMockCompanySubscription();
    permissionServiceSpy.getSubscriptionValue.and.returnValue(
      activeSubscription
    );

    const mockResponse: FeatureValidationResponse = {
      allowed: false,
      reason: 'NO_ROLE',
      message: 'User does not have required role',
    };
    permissionServiceSpy.canAccessWithDetails.and.returnValue(of(mockResponse));

    const result = guard.canActivate(route, state);

    if (typeof result === 'boolean') {
      expect(result).toBeFalse();
      done();
    } else if (result instanceof Observable) {
      result.subscribe((allowed: boolean | any) => {
        expect(allowed).not.toBeTrue();
        expect(toastServiceSpy.error).toHaveBeenCalledWith(
          '🚫 No tienes permisos para acceder a esta sección.'
        );
        done();
      });
    } else {
      // UrlTree case
      expect(result).toBeTruthy();
      done();
    }
  });

  it('should cache error messages to avoid spam', (done) => {
    route.data = { permission: 'TEST_FEATURE' };
    permissionServiceSpy.isSuperAdmin.and.returnValue(false);
    const activeSubscription = createMockCompanySubscription({
      status: 'ACTIVE',
    });
    permissionServiceSpy.getSubscriptionValue.and.returnValue(
      activeSubscription
    );

    const mockResponse: FeatureValidationResponse = {
      allowed: false,
      reason: 'PLAN_DOES_NOT_INCLUDE_FEATURE',
      message: 'Feature not available',
    };
    permissionServiceSpy.canAccessWithDetails.and.returnValue(of(mockResponse));

    // Mock menuService para que retorne un menú que coincida con la URL
    const menuService = TestBed.inject(MenuService) as MenuService;
    const mockMenu: Menu[] = [
      {
        id: 1,
        name: 'Test Feature',
        url: state.url,
        featureCode: 'TEST_FEATURE',
        icon: 'test',
        subMenus: [],
      },
    ];
    spyOn(menuService.menuChange, 'getValue').and.returnValue(mockMenu);

    // Primera llamada
    const result1 = guard.canActivate(route, state);
    // Segunda llamada inmediata (debería usar cache)
    const result2 = guard.canActivate(route, state);

    const handleResult = (result: any, callback: () => void) => {
      if (typeof result === 'boolean') {
        expect(result).toBeFalse();
        callback();
      } else if (result instanceof Observable) {
        result.subscribe(() => callback());
      } else {
        // UrlTree case
        expect(result).toBeTruthy();
        callback();
      }
    };

    handleResult(result1, () => {
      handleResult(result2, () => {
        // El toast solo debería llamarse una vez
        expect(toastServiceSpy.warning).toHaveBeenCalledTimes(1);
        done();
      });
    });
  });
});
