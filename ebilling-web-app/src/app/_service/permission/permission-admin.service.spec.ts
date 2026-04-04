import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { PermissionAdminService } from './permission-admin.service';
import { environment } from '../../../environments/environment';
import {
  FeaturePermissionAdmin,
  Role,
  BulkPermissionUpdateRequest,
} from '../../_model/permission/permission-admin';
import {
  FeaturePlanPermission,
  BulkPlanPermissionUpdateRequest,
} from '../../_model/permission/plan-permission-admin';
import { BulkUpdateResponse } from '../../_model/permission/plan-features-response';
import { PlanFeaturesResponse } from '../../_model/permission/plan-features-response';
import { Menu } from '../../_model/auth/menu';
import { PlanResponse } from '../../_model/subscription/subscription-admin';

describe('PermissionAdminService', () => {
  let service: PermissionAdminService;
  let httpMock: HttpTestingController;
  const apiUrl = `${environment.HOST_AUTH}/api/permission/permissions`;
  const billingApiUrl = `${environment.HOST_EBILLING}/api/permissions`;
  const menuUrl = `${environment.HOST_AUTH}/api/menu`;
  const roleUrl = `${environment.HOST_AUTH}/api/role`;

  // Helper para crear token mock
  const createMockToken = (payload: any): string => {
    const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
    const body = btoa(JSON.stringify(payload));
    return `${header}.${body}.signature`;
  };

  const VALID_TOKEN = createMockToken({
    sub: 'testuser',
    roles: ['admin'],
    idCompany: 1,
    userName: 'testuser',
  });

  beforeEach(() => {
    // Limpiar localStorage antes de cada test
    localStorage.clear();

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [PermissionAdminService],
    });

    service = TestBed.inject(PermissionAdminService);
    httpMock = TestBed.inject(HttpTestingController);
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

  describe('getAllFeatures', () => {
    it('should get features from menus first, then enrich with backend permissions', (done) => {
      localStorage.setItem(environment.TOKEN, VALID_TOKEN);

      const mockMenus: Menu[] = [
        {
          id: 1,
          name: 'Facturación',
          url: '/invoices',
          featureCode: 'INVOICE_CREATE',
          icon: 'receipt',
          subMenus: [],
        },
      ];

      const mockBackendFeatures: any[] = [
        {
          featureCode: 'INVOICE_CREATE',
          featureName: 'Facturación',
          module: 'INVOICING',
          allowedRoles: [1, 2],
          requiredPlans: ['BASIC', 'PRO'],
          description: 'Permite generar facturas',
        },
      ];

      service.getAllFeatures().subscribe((features) => {
        expect(features.length).toBeGreaterThan(0);
        expect(features[0].featureCode).toBe('INVOICE_CREATE');
        expect(features[0].name).toBe('Facturación'); // Nombre del menú
        expect(features[0].allowedRoles).toEqual([1, 2]); // Permisos del backend
        done();
      });

      // Primera petición: obtener menús
      const menuReq = httpMock.expectOne(`${menuUrl}/testuser`);
      expect(menuReq.request.method).toBe('GET');
      menuReq.flush(mockMenus);

      // Segunda petición: obtener permisos del backend
      const featuresReq = httpMock.expectOne(`${apiUrl}/features`);
      expect(featuresReq.request.method).toBe('GET');
      featuresReq.flush(mockBackendFeatures);
    });

    it('should return empty array if menu request fails (no fallback)', (done) => {
      localStorage.setItem(environment.TOKEN, VALID_TOKEN);

      service.getAllFeatures().subscribe((features) => {
        expect(features).toEqual([]); // Debe retornar array vacío, NO usar fallback
        done();
      });

      // Petición de menús falla
      const menuReq = httpMock.expectOne(`${menuUrl}/testuser`);
      menuReq.flush(null, { status: 500, statusText: 'Server Error' });

      // NO debe haber petición al endpoint alternativo (sin fallback)
      httpMock.verify();
    });

    it('should return empty array when no token exists', (done) => {
      localStorage.removeItem(environment.TOKEN);

      service.getAllFeatures().subscribe((features) => {
        expect(features).toEqual([]); // Sin token, retorna array vacío
        done();
      });

      // NO debe haber peticiones HTTP sin token
      httpMock.verify();
    });
  });

  describe('getRolePermissions', () => {
    it('should get permissions for a specific role', () => {
      const roleId = 1;
      const mockPermissions: FeaturePermissionAdmin[] = [
        {
          featureCode: 'INVOICE_CREATE',
          name: 'Facturación',
          module: 'INVOICING',
          allowedRoles: [1],
          requiredPlans: ['BASIC'],
          description: 'Permite generar facturas',
        },
      ];

      service.getRolePermissions(roleId).subscribe((permissions) => {
        expect(permissions.length).toBe(1);
        expect(permissions[0].featureCode).toBe('INVOICE_CREATE');
      });

      const req = httpMock.expectOne(`${apiUrl}/role/${roleId}`);
      expect(req.request.method).toBe('GET');
      req.flush(mockPermissions);
    });
  });

  describe('getAllRoles', () => {
    it('should get all roles from backend', () => {
      const mockRoleResponse = {
        roleDtos: [
          {
            idRole: 1,
            name: 'Super Admin',
            description: 'Acceso total',
          },
          {
            idRole: 2,
            name: 'Admin',
            description: 'Administrador',
          },
        ],
      };

      service.getAllRoles().subscribe((roles) => {
        expect(roles.length).toBe(2);
        expect(roles[0].id).toBe(1);
        expect(roles[0].name).toBe('Super Admin');
        expect(roles[1].id).toBe(2);
        expect(roles[1].name).toBe('Admin');
      });

      const req = httpMock.expectOne(roleUrl);
      expect(req.request.method).toBe('GET');
      req.flush(mockRoleResponse);
    });

    it('should return empty array when backend fails (no hardcoded fallback)', () => {
      service.getAllRoles().subscribe((roles) => {
        expect(roles).toEqual([]); // Debe retornar array vacío, NO usar datos hardcodeados
      });

      const req = httpMock.expectOne(roleUrl);
      req.flush(null, { status: 500, statusText: 'Server Error' });
    });
  });

  describe('updateFeatureRoles', () => {
    it('should update allowed roles for a feature', () => {
      const featureCode = 'INVOICE_CREATE';
      const allowedRoles = [1, 2];

      service.updateFeatureRoles(featureCode, allowedRoles).subscribe(() => {
        expect(true).toBe(true);
      });

      const req = httpMock.expectOne(`${apiUrl}/feature/${featureCode}`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual({ allowedRoles });
      req.flush(null);
    });
  });

  describe('bulkUpdatePermissions', () => {
    it('should update multiple permissions in bulk', () => {
      const request: BulkPermissionUpdateRequest = {
        featureRoles: {
          INVOICE_CREATE: [1, 2],
          CLIENT_CREATE: [1],
        },
      };

      const mockResponse: FeaturePermissionAdmin[] = [
        {
          featureCode: 'INVOICE_CREATE',
          name: 'Facturación',
          module: 'INVOICING',
          allowedRoles: [1, 2],
          requiredPlans: [],
          description: '',
        },
        {
          featureCode: 'CLIENT_CREATE',
          name: 'Clientes',
          module: 'CLIENTS',
          allowedRoles: [1],
          requiredPlans: [],
          description: '',
        },
      ];

      service.bulkUpdatePermissions(request).subscribe((features) => {
        expect(features.length).toBe(2);
      });

      const req = httpMock.expectOne(`${apiUrl}/bulk`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(request);
      req.flush(mockResponse);
    });
  });

  describe('getAllFeaturesWithPlanPermissions', () => {
    it('should get features with plan permissions from menu first', (done) => {
      localStorage.setItem(environment.TOKEN, VALID_TOKEN);

      const mockMenus: Menu[] = [
        {
          id: 1,
          name: 'Facturación',
          url: '/invoices',
          featureCode: 'INVOICE_CREATE',
          icon: 'receipt',
          subMenus: [],
        },
      ];

      const mockBackendFeatures: FeaturePlanPermission[] = [
        {
          featureCode: 'INVOICE_CREATE',
          name: 'Facturación',
          module: 'INVOICING',
          allowedPlans: ['BASIC', 'PRO'],
          description: 'Permite generar facturas',
        },
      ];

      service.getAllFeaturesWithPlanPermissions().subscribe((features) => {
        expect(features.length).toBeGreaterThan(0);
        done();
      });

      // Primera petición: obtener menús
      const menuReq = httpMock.expectOne(`${menuUrl}/testuser`);
      menuReq.flush(mockMenus);

      // Segunda petición: obtener permisos de planes del backend
      const featuresReq = httpMock.expectOne(`${apiUrl}/features/plans`);
      featuresReq.flush(mockBackendFeatures);
    });

    it('should return empty array if menu request fails (no fallback)', (done) => {
      localStorage.setItem(environment.TOKEN, VALID_TOKEN);

      service.getAllFeaturesWithPlanPermissions().subscribe((features) => {
        expect(features.length).toBe(0); // Debe retornar array vacío, NO usar fallback
        done();
      });

      // Petición de menús falla
      const menuReq = httpMock.expectOne(`${menuUrl}/testuser`);
      menuReq.flush(null, { status: 500, statusText: 'Server Error' });

      // NO debe haber petición al endpoint alternativo (sin fallback)
      httpMock.verify();
    });

    it('should return empty array if no features found in menus', (done) => {
      localStorage.setItem(environment.TOKEN, VALID_TOKEN);

      const mockMenus: Menu[] = []; // Menús vacíos

      service.getAllFeaturesWithPlanPermissions().subscribe((features) => {
        expect(features.length).toBe(0); // Debe retornar array vacío si no hay features
        done();
      });

      // Petición de menús retorna array vacío
      const menuReq = httpMock.expectOne(`${menuUrl}/testuser`);
      menuReq.flush(mockMenus);

      // NO debe haber petición al endpoint alternativo (sin fallback)
      httpMock.verify();
    });
  });

  describe('getFeaturePlanPermissions', () => {
    it('should get plan permissions for a specific feature', () => {
      const featureCode = 'INVOICE_CREATE';
      const mockResponse = {
        featureCode: 'INVOICE_CREATE',
        featureName: 'Facturación',
        module: 'INVOICING',
        requiredPlans: ['BASIC', 'PRO'],
        description: 'Permite generar facturas',
      };

      service.getFeaturePlanPermissions(featureCode).subscribe((feature) => {
        expect(feature.featureCode).toBe('INVOICE_CREATE');
        expect(feature.allowedPlans).toEqual(['BASIC', 'PRO']);
      });

      const req = httpMock.expectOne(`${apiUrl}/feature/${featureCode}/plans`);
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });
  });

  describe('updateFeaturePlans', () => {
    it('should update plan permissions for a feature', () => {
      const featureCode = 'INVOICE_CREATE';
      const allowedPlans = ['BASIC', 'PRO'];

      const mockResponse: FeaturePlanPermission = {
        featureCode: 'INVOICE_CREATE',
        name: 'Facturación',
        module: 'INVOICING',
        allowedPlans: ['BASIC', 'PRO'],
        description: 'Permite generar facturas',
      };

      service
        .updateFeaturePlans(featureCode, allowedPlans)
        .subscribe((feature) => {
          expect(feature.featureCode).toBe('INVOICE_CREATE');
          expect(feature.allowedPlans).toEqual(allowedPlans);
        });

      const req = httpMock.expectOne(`${apiUrl}/feature/${featureCode}/plans`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual({ requiredPlans: allowedPlans });
      req.flush(mockResponse);
    });
  });

  describe('bulkUpdatePlanPermissions', () => {
    it('should update multiple plan permissions in bulk', () => {
      const request: BulkPlanPermissionUpdateRequest = {
        featurePlans: {
          INVOICE_CREATE: ['BASIC', 'PRO'],
          CLIENT_CREATE: ['BASIC'],
        },
      };

      const mockPlanFeaturesResponse: PlanFeaturesResponse[] = [
        {
          planCode: 'BASIC',
          planName: 'Básico',
          features: {
            hasApiAccess: true,
            hasMultiUser: true,
            hasAdvancedReports: false,
            hasElectronicInvoice: true,
            hasCreditNote: true,
            hasDebitNote: false,
            hasRetention: false,
            hasGuide: false,
            hasPrioritySupport: false,
            hasCustomBranding: false,
            hasAutomatedBackups: false,
          },
        },
      ];

      const mockBulkResponse: BulkUpdateResponse = {
        success: true,
        message: 'Updates completed',
        totalUpdated: 2,
        totalFailed: 0,
        results: [
          {
            identifier: 'BASIC',
            success: true,
            message: 'Updated',
            error: null,
          },
        ],
        timestamp: new Date().toISOString(),
      };

      service.bulkUpdatePlanPermissions(request).subscribe((response) => {
        expect(response.success).toBe(true);
        expect(response.totalUpdated).toBe(2);
        expect(response.totalFailed).toBe(0);
      });

      // Primera llamada: GET para obtener los planes actuales
      const getReq = httpMock.expectOne(`${billingApiUrl}/features/plans`);
      expect(getReq.request.method).toBe('GET');
      getReq.flush(mockPlanFeaturesResponse);

      // Segunda llamada: PUT para actualizar
      const putReq = httpMock.expectOne(`${billingApiUrl}/plans/bulk`);
      expect(putReq.request.method).toBe('PUT');
      putReq.flush(mockBulkResponse);
    });
  });

  describe('syncRolePermissions', () => {
    it('should complete without error (not implemented in backend)', (done) => {
      const roleId = 1;
      const features = ['INVOICE_CREATE', 'CLIENT_CREATE'];

      service.syncRolePermissions(roleId, features).subscribe({
        next: () => {
          expect(true).toBe(true);
          done();
        },
        error: () => {
          fail('should not error');
        },
      });
    });
  });
});
