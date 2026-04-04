import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { PermissionService } from './permission.service';
import { LoggerService } from '../logger/logger.service';
import { environment } from '../../../environments/environment';
import { CompanySubscription } from '../../_model/subscription/company-subscription';
import { SubscriptionPlan } from '../../_model/subscription/subscription-plan';
import {
  FeatureValidationResponse,
  FeatureValidationRequest,
  CanEmitDocumentResponse,
} from '../../_model/subscription/feature-validation-response';
import { SubscriptionUsageResponse } from '../../_model/subscription/subscription-usage';
import {
  createMockCompanySubscription,
  createMockSubscriptionPlan,
  createMockSubscriptionUsageResponse,
} from '../../_test-helpers/mock-data';
import { JwtHelperService } from '@auth0/angular-jwt';

describe('PermissionService', () => {
  let service: PermissionService;
  let httpMock: HttpTestingController;
  let loggerSpy: jasmine.SpyObj<LoggerService>;
  let jwtHelper: JwtHelperService;

  // Token JWT válido para tests - usando JwtHelperService para crear tokens válidos
  // Nota: Estos tokens no tienen firma válida, pero JwtHelperService.decodeToken()
  // solo decodifica el payload sin verificar la firma en modo desarrollo
  const createMockToken = (payload: any): string => {
    const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
    const body = btoa(JSON.stringify(payload));
    return `${header}.${body}.signature`;
  };

  // Helper para limpiar peticiones HTTP pendientes con el tipo correcto de respuesta
  const flushPendingRequest = (req: any): void => {
    try {
      const url = req.request.url;
      if (url.includes('/permissions/features/plans')) {
        // Las peticiones de permisos de planes esperan un array
        req.flush([]);
      } else if (url.includes('/user/features')) {
        // Las peticiones de features del usuario esperan un array de strings
        req.flush([]);
      } else if (url.includes('/user/userName/')) {
        // Las peticiones de getUserByUserName esperan UserResponse con userDto
        req.flush({ userDto: { idUser: 1 } });
      } else if (url.includes('/subscription')) {
        // Las peticiones de suscripción esperan un objeto o null
        req.flush(null, { status: 404, statusText: 'Not Found' });
      } else {
        // Para otras peticiones, usar null
        req.flush(null);
      }
    } catch (e) {
      // Ignorar errores
    }
  };

  const VALID_TEST_TOKEN = createMockToken({
    sub: 'testuser',
    roles: ['admin'],
    idCompany: 1,
  });

  const SUPER_ADMIN_TOKEN = createMockToken({
    sub: 'admin',
    roles: ['super_admin'],
    idCompany: 1,
  });

  const TOKEN_WITHOUT_COMPANY = createMockToken({
    sub: 'testuser',
    roles: ['admin'],
    // Sin idCompany
  });

  beforeEach(() => {
    // Limpiar localStorage antes de cada test
    localStorage.clear();

    // Crear spy para LoggerService
    loggerSpy = jasmine.createSpyObj('LoggerService', [
      'debug',
      'info',
      'warn',
      'error',
    ]);

    // Crear instancia de JwtHelperService para los tests
    jwtHelper = new JwtHelperService();

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        PermissionService,
        { provide: LoggerService, useValue: loggerSpy },
      ],
    });

    httpMock = TestBed.inject(HttpTestingController);
    // NO crear el servicio aquí - se creará en cada test según necesite
    // Esto evita que las peticiones HTTP del constructor interfieran con los tests
  });

  afterEach(() => {
    if (httpMock) {
      // Limpiar cualquier petición HTTP pendiente antes de verificar
      const pendingRequests = httpMock.match(() => true);
      pendingRequests.forEach(flushPendingRequest);
      httpMock.verify();
    }
    localStorage.clear();
  });

  it('should be created', () => {
    service = TestBed.inject(PermissionService);
    expect(service).toBeTruthy();
    // Limpiar cualquier petición HTTP del constructor
    httpMock.match(() => true).forEach(flushPendingRequest);
  });

  describe('getSubscription', () => {
    it('should return null when no token exists', (done) => {
      // Asegurar que no hay token
      localStorage.removeItem(environment.TOKEN);
      localStorage.removeItem(environment.ID_COMPANY);

      // Crear servicio sin token
      const newService = TestBed.inject(PermissionService);

      // Limpiar cualquier petición HTTP del constructor
      httpMock.match(() => true).forEach(flushPendingRequest);

      newService.getSubscription().subscribe((subscription) => {
        expect(subscription).toBeNull();
        done();
      });
    });

    it('should return cached subscription when available', (done) => {
      const mockSubscription = createMockCompanySubscription();

      localStorage.setItem(environment.TOKEN, VALID_TEST_TOKEN);
      localStorage.setItem(environment.ID_COMPANY, '1');

      // Limpiar cualquier petición HTTP pendiente antes de crear el servicio
      const oldRequests = httpMock.match(() => true);
      oldRequests.forEach(flushPendingRequest);

      // Crear servicio - esto disparará loadSubscription() en el constructor
      const newService = TestBed.inject(PermissionService);

      // Mockear la petición HTTP que se hace en el constructor
      const subscriptionRequests = httpMock.match((req) =>
        req.url.includes('/subscription/1/current')
      );
      expect(subscriptionRequests.length).toBeGreaterThan(0);
      const req = subscriptionRequests[0];
      expect(req.request.method).toBe('GET');
      req.flush(mockSubscription);

      // Mockear la petición de permisos de planes que se hace en el constructor
      const planRequests = httpMock.match((req) =>
        req.url.includes('/permissions/features/plans')
      );
      planRequests.forEach((req) => req.flush([]));

      // Mockear la petición de user/features que se hace en el constructor
      const userFeaturesRequests = httpMock.match((req) =>
        req.url.includes('/user/features')
      );
      userFeaturesRequests.forEach((req) => req.flush([]));

        newService.getSubscription().subscribe((subscription) => {
          expect(subscription).toBeTruthy();
          expect(subscription?.plan?.code).toBe('PROFESSIONAL');
          done();
        });
    });

    it('should fetch subscription from backend when not cached', (done) => {
      const mockSubscription = createMockCompanySubscription();

      localStorage.setItem(environment.TOKEN, VALID_TEST_TOKEN);
      localStorage.setItem(environment.ID_COMPANY, '1');

      // Limpiar cualquier petición HTTP pendiente antes de crear el servicio
      const oldRequests = httpMock.match(() => true);
      oldRequests.forEach(flushPendingRequest);

      // Crear servicio - esto disparará loadSubscription() en el constructor
      const newService = TestBed.inject(PermissionService);

      // Mockear la petición HTTP que se hace en el constructor
      const subscriptionRequests = httpMock.match((req) =>
        req.url.includes('/subscription/1/current')
      );
      expect(subscriptionRequests.length).toBeGreaterThan(0);
      const req = subscriptionRequests[0];
      expect(req.request.method).toBe('GET');
      req.flush(mockSubscription);

      // Mockear la petición de permisos de planes que se hace en el constructor
      const planRequests = httpMock.match((req) =>
        req.url.includes('/permissions/features/plans')
      );
      planRequests.forEach((req) => req.flush([]));

      // Mockear la petición de user/features que se hace en el constructor
      const userFeaturesRequests = httpMock.match((req) =>
        req.url.includes('/user/features')
      );
      userFeaturesRequests.forEach((req) => req.flush([]));

        newService.getSubscription().subscribe((subscription) => {
          expect(subscription).toBeTruthy();
        expect(subscription?.plan?.code).toBe('PROFESSIONAL');
          done();
        });
    });

    it('should return null when backend returns 404', (done) => {
      localStorage.setItem(environment.TOKEN, VALID_TEST_TOKEN);
      localStorage.setItem(environment.ID_COMPANY, '1');

      // Limpiar cualquier petición HTTP pendiente antes de crear el servicio
      const oldRequests = httpMock.match(() => true);
      oldRequests.forEach(flushPendingRequest);

      // Crear servicio
      const newService = TestBed.inject(PermissionService);

      // Mockear la petición HTTP que se hace en el constructor con 404
      const subscriptionRequests = httpMock.match((req) =>
        req.url.includes('/subscription/1/current')
      );
      if (subscriptionRequests.length > 0) {
        subscriptionRequests[0].flush(null, {
          status: 404,
          statusText: 'Not Found',
        });
      }

      // Mockear la petición de permisos de planes que se hace en el constructor
      const planRequests = httpMock.match((req) =>
        req.url.includes('/permissions/features/plans')
      );
      planRequests.forEach((req) => req.flush([]));

      // Mockear la petición de user/features que se hace en el constructor
      const userFeaturesRequests = httpMock.match((req) =>
        req.url.includes('/user/features')
      );
      userFeaturesRequests.forEach((req) => req.flush([]));

      newService.getSubscription().subscribe((subscription) => {
        expect(subscription).toBeNull();
        done();
      });
    });
  });

  describe('refreshSubscription', () => {
    beforeEach(() => {
      // NO limpiar peticiones aquí - el test las manejará
    });

    it('should reload subscription from backend', (done) => {
      const mockSubscription = createMockCompanySubscription();

      localStorage.setItem(environment.TOKEN, VALID_TEST_TOKEN);
      localStorage.setItem(environment.ID_COMPANY, '1');

      // Limpiar cualquier petición HTTP pendiente antes de crear el servicio
      const oldRequests = httpMock.match(() => true);
      oldRequests.forEach(flushPendingRequest);

      // Crear servicio - esto disparará loadSubscription() en el constructor
      const newService = TestBed.inject(PermissionService);

      // Mockear la petición HTTP que se hace en el constructor
      const subscriptionRequests = httpMock.match((req) =>
        req.url.includes('/subscription/1/current')
      );
      expect(subscriptionRequests.length).toBeGreaterThan(0);
      const req1 = subscriptionRequests[0];
      expect(req1.request.method).toBe('GET');
      req1.flush(mockSubscription);

      // Mockear la petición de permisos de planes que se hace en el constructor
      const planRequests = httpMock.match((req) =>
        req.url.includes('/permissions/features/plans')
      );
      planRequests.forEach((req) => req.flush([]));

      // Mockear la petición de user/features que se hace en el constructor
      const userFeaturesRequests = httpMock.match((req) =>
        req.url.includes('/user/features')
      );
      userFeaturesRequests.forEach((req) => req.flush([]));

      // Ahora llamar refreshSubscription
      newService.refreshSubscription();

      // Mockear la nueva petición de refreshSubscription
      const refreshRequests = httpMock.match((req) =>
        req.url.includes('/subscription/1/current')
      );
      expect(refreshRequests.length).toBeGreaterThan(0);
      const req2 = refreshRequests[0];
      expect(req2.request.method).toBe('GET');
      req2.flush(mockSubscription);

      // Mockear las nuevas peticiones de refreshSubscription (plan permissions y user features)
      const refreshPlanRequests = httpMock.match((req) =>
        req.url.includes('/permissions/features/plans')
      );
      refreshPlanRequests.forEach((req) => req.flush([]));

      const refreshUserFeaturesRequests = httpMock.match((req) =>
        req.url.includes('/user/features')
      );
      refreshUserFeaturesRequests.forEach((req) => req.flush([]));

      newService.getSubscription().subscribe((subscription) => {
        expect(subscription).toBeTruthy();
        expect(subscription?.plan?.code).toBe('PROFESSIONAL');
        done();
      });
    });
  });

  describe('isSuperAdmin', () => {
    beforeEach(() => {
      localStorage.setItem(environment.TOKEN, VALID_TEST_TOKEN);
      localStorage.setItem(environment.ID_COMPANY, '1');
      service = TestBed.inject(PermissionService);
      const pendingRequests = httpMock.match(() => true);
      pendingRequests.forEach(flushPendingRequest);
    });

    it('should return true when user is super_admin', () => {
      localStorage.setItem(environment.TOKEN, SUPER_ADMIN_TOKEN);
      const newService = TestBed.inject(PermissionService);
      httpMock.match(() => true).forEach(flushPendingRequest);
      expect(newService.isSuperAdmin()).toBeTruthy();
    });

    it('should return false when user is not super_admin', () => {
      expect(service.isSuperAdmin()).toBeFalsy();
    });
  });

  describe('canAccess', () => {
    beforeEach(() => {
      localStorage.setItem(environment.TOKEN, VALID_TEST_TOKEN);
      localStorage.setItem(environment.ID_COMPANY, '1');
      // Crear servicio para estos tests
      service = TestBed.inject(PermissionService);
      // NO flushar las peticiones del constructor aquí - cada test las manejará individualmente
    });

    it('should return false when no token exists', (done) => {
      localStorage.removeItem(environment.TOKEN);
      // Crear nuevo servicio sin token
      const newService = TestBed.inject(PermissionService);

      // Limpiar peticiones del constructor (no debería haber ninguna sin token)
      const pendingRequests = httpMock.match(() => true);
      pendingRequests.forEach(flushPendingRequest);

      newService.canAccess('TEST_FEATURE').subscribe((allowed) => {
        expect(allowed).toBeFalse();
        done();
      });
    });

    it('should return true for super_admin without backend call', (done) => {
      // Usar token super_admin
      localStorage.setItem(environment.TOKEN, SUPER_ADMIN_TOKEN);
      // Crear nuevo servicio con token super_admin
      const newService = TestBed.inject(PermissionService);

      // Limpiar peticiones del constructor
      const pendingRequests = httpMock.match(() => true);
      pendingRequests.forEach(flushPendingRequest);

      newService.canAccess('TEST_FEATURE').subscribe((allowed) => {
        expect(allowed).toBeTrue();
        done();
      });

      // Verificar que NO se hizo petición HTTP (super_admin no necesita backend)
      httpMock.expectNone(
        `${environment.HOST_AUTH}/api/permission/user/features`
      );
    });

    it('should return true when feature is in allowed features', fakeAsync(() => {
      const mockFeatures = ['TEST_FEATURE', 'OTHER_FEATURE'];

      // Flushear todas las peticiones del constructor primero (subscription, planPermissions, userFeatures)
      const constructorReqs = httpMock.match(() => true);
      constructorReqs.forEach((req) => {
        const url = req.request.url;
        if (url.includes('/user/features')) {
          req.flush(mockFeatures);
        } else {
          flushPendingRequest(req);
        }
      });
      tick();

      // Ahora llamar canAccess - debería usar el cache
      service.canAccess('TEST_FEATURE').subscribe((allowed) => {
        expect(allowed).toBeTrue();
      });
      tick();
    }));

    it('should return false when feature is not in allowed features', fakeAsync(() => {
      const mockFeatures = ['OTHER_FEATURE', 'ANOTHER_FEATURE'];

      // Flushear todas las peticiones del constructor primero (subscription, planPermissions, userFeatures)
      const constructorReqs = httpMock.match(() => true);
      constructorReqs.forEach((req) => {
        const url = req.request.url;
        if (url.includes('/user/features')) {
          req.flush(mockFeatures);
        } else {
          flushPendingRequest(req);
        }
      });
      tick();

      // Ahora llamar canAccess - debería usar el cache
      service.canAccess('PREMIUM_FEATURE').subscribe((allowed) => {
        expect(allowed).toBeFalse();
      });
      tick();
    }));

    it('should load features from backend when cache is empty', fakeAsync(() => {
      const mockFeatures = ['CACHED_FEATURE'];

      // Flushear todas las peticiones del constructor primero
      const constructorReqs = httpMock.match(() => true);
      constructorReqs.forEach((req) => {
        const url = req.request.url;
        if (url.includes('/user/features')) {
          req.flush([]); // Flush con array vacío
        } else {
          flushPendingRequest(req);
        }
      });
      tick();

      // Invalidar cache - esto genera una nueva petición, pero NO la flusheamos
      // para que featuresLoaded siga siendo false
      service.invalidateFeaturesCache();
      // NO flushar la petición de invalidateFeaturesCache para que featuresLoaded siga siendo false
      tick();

      service.canAccess('CACHED_FEATURE').subscribe((allowed) => {
        expect(allowed).toBeTrue();
      });

      // Debe hacer petición a /user/features cuando canAccess llama porque el cache está vacío
      // Puede haber dos peticiones (una de invalidateFeaturesCache y una de canAccess)
      const reqs = httpMock.match((req) => req.url.includes('/user/features'));
      expect(reqs.length).toBeGreaterThan(0);
      // Flushear la última petición (la de canAccess)
      reqs[reqs.length - 1].flush(mockFeatures);
      tick();
    }));

    it('should return false when no idCompany available', (done) => {
      // Usar token sin idCompany y sin ID_COMPANY en localStorage
      localStorage.setItem(environment.TOKEN, TOKEN_WITHOUT_COMPANY);
      localStorage.removeItem(environment.ID_COMPANY);

      const newService = TestBed.inject(PermissionService);
      const pendingRequests = httpMock.match(() => true);
      pendingRequests.forEach(flushPendingRequest);

      newService.canAccess('TEST_FEATURE').subscribe((allowed) => {
        expect(allowed).toBeFalse();
        done();
      });
    });

    it('should cache validation result and return from cache on second call', fakeAsync(() => {
      const mockFeatures = ['CACHED_FEATURE'];

      // Flushear todas las peticiones del constructor primero
      const constructorReqs = httpMock.match(() => true);
      constructorReqs.forEach((req) => {
        const url = req.request.url;
        if (url.includes('/user/features')) {
          req.flush([]); // Flush con array vacío
        } else {
          flushPendingRequest(req);
        }
      });
      tick();

      // Invalidar cache - esto genera una nueva petición, pero NO la flusheamos
      // para que featuresLoaded siga siendo false
      service.invalidateFeaturesCache();
      // NO flushar la petición de invalidateFeaturesCache para que featuresLoaded siga siendo false
      tick();

      let callCount = 0;

      // Primera llamada - debe hacer HTTP request
      service.canAccess('CACHED_FEATURE').subscribe((allowed) => {
        callCount++;
        expect(allowed).toBeTrue();
        expect(callCount).toBe(1);
      });

      // Puede haber dos peticiones (una de invalidateFeaturesCache y una de canAccess)
      const reqs = httpMock.match((req) => req.url.includes('/user/features'));
      expect(reqs.length).toBeGreaterThan(0);
      // Flushear la última petición (la de canAccess)
      reqs[reqs.length - 1].flush(mockFeatures);
      tick();

        // Segunda llamada - debe usar cache (sin HTTP request)
        service.canAccess('CACHED_FEATURE').subscribe((cachedAllowed) => {
          callCount++;
          expect(cachedAllowed).toBeTrue();
          expect(callCount).toBe(2);
      });

      // Verificar que NO se hace segunda petición
      httpMock.expectNone(
        `${environment.HOST_AUTH}/api/permission/user/features`
      );
      tick();
    }));

    it('should reload features after invalidating cache', fakeAsync(() => {
      const mockFeatures1 = ['FEATURE_TO_INVALIDATE'];
      const mockFeatures2 = ['FEATURE_TO_INVALIDATE'];

      // Flushear todas las peticiones del constructor primero
      const constructorReqs = httpMock.match(() => true);
      constructorReqs.forEach((req) => {
        const url = req.request.url;
        if (url.includes('/user/features')) {
          req.flush([]); // Flush con array vacío
        } else {
          flushPendingRequest(req);
        }
      });
      tick();

      // Invalidar cache para empezar limpio - esto genera una nueva petición, pero NO la flusheamos
      service.invalidateFeaturesCache();
      // NO flushar la petición de invalidateFeaturesCache para que featuresLoaded siga siendo false
      tick();

      // Primera llamada - guarda en cache
      service.canAccess('FEATURE_TO_INVALIDATE').subscribe((allowed) => {
        expect(allowed).toBeTrue();
      });

      // Puede haber dos peticiones (una de invalidateFeaturesCache y una de canAccess)
      const reqs1 = httpMock.match((req) => req.url.includes('/user/features'));
      expect(reqs1.length).toBeGreaterThan(0);
      // Flushear la última petición (la de canAccess)
      reqs1[reqs1.length - 1].flush(mockFeatures1);
      tick();

      // Invalidar cache - esto genera otra nueva petición, pero NO la flusheamos
      service.invalidateFeaturesCache();
      // NO flushar la petición de invalidateFeaturesCache para que featuresLoaded siga siendo false
      tick();

        // Segunda llamada después de invalidar - debe hacer HTTP request nuevamente
        service.canAccess('FEATURE_TO_INVALIDATE').subscribe((allowed2) => {
          expect(allowed2).toBeTrue();
        });

      // Puede haber dos peticiones (una de invalidateFeaturesCache y una de canAccess)
      const reqs2 = httpMock.match((req) => req.url.includes('/user/features'));
      expect(reqs2.length).toBeGreaterThan(0);
      // Flushear la última petición (la de canAccess)
      reqs2[reqs2.length - 1].flush(mockFeatures2);
      tick();
    }));
  });

  describe('canAccessWithDetails', () => {
    beforeEach(() => {
      localStorage.setItem(environment.TOKEN, VALID_TEST_TOKEN);
      localStorage.setItem(environment.ID_COMPANY, '1');
      // Crear servicio para estos tests
      service = TestBed.inject(PermissionService);
      // NO flushar las peticiones del constructor aquí - cada test las manejará individualmente
    });

    it('should return full response when feature is in cache', fakeAsync(() => {
      const mockFeatures = ['TEST_FEATURE'];

      // Flushear todas las peticiones del constructor primero (subscription, planPermissions, userFeatures)
      const constructorReqs = httpMock.match(() => true);
      constructorReqs.forEach((req) => {
        const url = req.request.url;
        if (url.includes('/user/features')) {
          req.flush(mockFeatures);
        } else {
          flushPendingRequest(req);
        }
      });
      tick();

      service.canAccessWithDetails('TEST_FEATURE').subscribe((response) => {
        expect(response.allowed).toBeTrue();
        expect(response.message).toBe('Acceso permitido');
      });
      tick();
    }));

    it('should return full response when feature is not in cache', fakeAsync(() => {
      const mockFeatures = ['OTHER_FEATURE'];

      // Flushear todas las peticiones del constructor primero (subscription, planPermissions, userFeatures)
      const constructorReqs = httpMock.match(() => true);
      constructorReqs.forEach((req) => {
        const url = req.request.url;
        if (url.includes('/user/features')) {
          req.flush(mockFeatures);
        } else {
          flushPendingRequest(req);
        }
      });
      tick();

      service.canAccessWithDetails('PREMIUM_FEATURE').subscribe((response) => {
        expect(response.allowed).toBeFalse();
        expect(response.reason).toBe('NO_ROLE');
        expect(response.message).toBe(
          'No tienes permisos para acceder a esta funcionalidad'
        );
      });
      tick();
    }));

    it('should call backend when cache is not loaded', fakeAsync(() => {
      const mockResponse: FeatureValidationResponse = {
        allowed: true,
        reason: 'SUCCESS',
        message: 'Access granted',
      };

      // Flushear todas las peticiones del constructor primero
      const constructorReqs = httpMock.match(() => true);
      constructorReqs.forEach((req) => {
        const url = req.request.url;
        if (url.includes('/user/features')) {
          req.flush([]); // Flush con array vacío
        } else if (url.includes('/user/userName/')) {
          req.flush({ userDto: { idUser: 1 } }); // Flush preloadIdUser con idUser
        } else {
          flushPendingRequest(req);
        }
      });
      tick();

      // Invalidar cache - esto genera una nueva petición, pero NO la flusheamos
      // para que featuresLoaded siga siendo false
      service.invalidateFeaturesCache();
      // NO flushar la petición de invalidateFeaturesCache para que featuresLoaded siga siendo false
      tick();

      service.canAccessWithDetails('TEST_FEATURE').subscribe((response) => {
        expect(response.allowed).toBeTrue();
        expect(response.reason).toBe('SUCCESS');
        expect(response.message).toBe('Access granted');
      });

      // canAccessWithDetails llama a getIdUser() que usa el cache (ya fue cargado por preloadIdUser)
      // Luego hace petición POST a /validate-feature
      // Puede haber peticiones pendientes de invalidateFeaturesCache, así que usamos match
      const validateReqs = httpMock.match((req) =>
        req.url.includes('/validate-feature')
      );
      expect(validateReqs.length).toBe(1);
      expect(validateReqs[0].request.method).toBe('POST');
      validateReqs[0].flush(mockResponse);
      tick();
    }));

    it('should use cache for canAccessWithDetails when available', fakeAsync(() => {
      const mockFeatures = ['CACHED_FEATURE_DETAILS'];

      // Flushear todas las peticiones del constructor primero
      const constructorReqs = httpMock.match(() => true);
      constructorReqs.forEach((req) => {
        const url = req.request.url;
        if (url.includes('/user/features')) {
          req.flush([]); // Flush con array vacío
        } else {
          flushPendingRequest(req);
        }
      });
      tick();

      // Invalidar cache - esto genera una nueva petición, pero NO la flusheamos
      // para que featuresLoaded siga siendo false
      service.invalidateFeaturesCache();
      // NO flushar la petición de invalidateFeaturesCache para que featuresLoaded siga siendo false
      tick();

      // Primero cargar features en cache con canAccess
      service.canAccess('CACHED_FEATURE_DETAILS').subscribe((allowed) => {
        expect(allowed).toBeTrue();
      });

      // Puede haber dos peticiones (una de invalidateFeaturesCache y una de canAccess)
      const reqs = httpMock.match((req) => req.url.includes('/user/features'));
      expect(reqs.length).toBeGreaterThan(0);
      // Flushear la última petición (la de canAccess)
      reqs[reqs.length - 1].flush(mockFeatures);
      tick();

        // Segunda llamada con canAccessWithDetails - debe usar cache
        service
          .canAccessWithDetails('CACHED_FEATURE_DETAILS')
          .subscribe((response) => {
            expect(response.allowed).toBeTrue();
          expect(response.message).toBe('Acceso permitido');
          });

      // NO debe hacer petición HTTP adicional
        httpMock.expectNone(
        `${environment.HOST_AUTH}/api/permission/validate-feature`
      );
      tick();
    }));
  });

  describe('canEmitDocument', () => {
    beforeEach(() => {
      localStorage.setItem(environment.TOKEN, VALID_TEST_TOKEN);
      localStorage.setItem(environment.ID_COMPANY, '1');
      // Crear servicio para estos tests
      service = TestBed.inject(PermissionService);
      // Limpiar cualquier petición HTTP pendiente del constructor
      const pendingRequests = httpMock.match(() => true);
      pendingRequests.forEach(flushPendingRequest);
    });

    it('should return true when backend allows document emission', (done) => {
      // canEmitDocument retorna Observable<boolean>, no CanEmitDocumentResponse
      service.canEmitDocument('INVOICE').subscribe((allowed) => {
        expect(allowed).toBeTrue();
        done();
      });

      const req = httpMock.expectOne(
        `${environment.HOST_EBILLING}/api/subscription/1/can-emit`
      );
      expect(req.request.method).toBe('POST');
      expect(req.request.body.documentType).toBe('INVOICE');
      // El backend retorna CanEmitDocumentResponse pero el método retorna boolean
      req.flush({
        allowed: true,
        reason: 'SUCCESS',
        message: 'Document can be emitted',
      });
    });

    it('should return false when backend denies document emission', (done) => {
      service.canEmitDocument('INVOICE').subscribe((allowed) => {
        expect(allowed).toBeFalse();
        done();
      });

      const req = httpMock.expectOne(
        `${environment.HOST_EBILLING}/api/subscription/1/can-emit`
      );
      req.flush({
        allowed: false,
        reason: 'LIMIT_EXCEEDED',
        message: 'Document limit exceeded',
      });
    });
  });
});
