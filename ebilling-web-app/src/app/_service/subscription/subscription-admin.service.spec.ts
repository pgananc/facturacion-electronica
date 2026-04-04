import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { SubscriptionAdminService } from './subscription-admin.service';
import { CompanyService } from '../customer/company.service';
import { environment } from '../../../environments/environment';
import {
  SubscriptionAdmin,
  PlanChangeRequest,
  SubscriptionExtendRequest,
  SubscriptionCancelRequest,
  AssignSubscriptionRequest,
  AvailablePlan,
  PlanResponse,
  SubscriptionPageResponse,
} from '../../_model/subscription/subscription-admin';
import { Company } from '../../_model/customer/company-establishment';
import { of, throwError } from 'rxjs';

describe('SubscriptionAdminService', () => {
  let service: SubscriptionAdminService;
  let httpMock: HttpTestingController;
  let companyService: jasmine.SpyObj<CompanyService>;
  const baseUrl = `${environment.HOST_EBILLING}/api/subscriptions`;

  beforeEach(() => {
    const companyServiceSpy = jasmine.createSpyObj('CompanyService', [
      'findById',
    ]);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        SubscriptionAdminService,
        { provide: CompanyService, useValue: companyServiceSpy },
      ],
    });

    service = TestBed.inject(SubscriptionAdminService);
    httpMock = TestBed.inject(HttpTestingController);
    companyService = TestBed.inject(
      CompanyService
    ) as jasmine.SpyObj<CompanyService>;
  });

  afterEach(() => {
    if (httpMock) {
      httpMock.verify();
    }
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getAllSubscriptions', () => {
    it('should get all subscriptions', () => {
      const mockPageResponse: SubscriptionPageResponse = {
        content: [
          {
            idCompany: 1,
            plan: {
              code: 'BASIC',
              name: 'Plan Básico',
              documentLimitYearly: 500,
            },
            subscriptionStatus: 'ACTIVE',
            currentYearUsage: 100,
            percentUsed: 20,
            remainingDocuments: 400,
            startDate: '2024-01-01',
            expiresAt: '2024-12-31',
          } as SubscriptionAdmin,
        ],
        totalElements: 1,
        totalPages: 1,
        size: 20,
        number: 0,
        first: true,
        last: true,
        empty: false,
      };

      const mockCompany: Company = {
        idCompany: 1,
        name: 'Test Company',
        identification: '1234567890001',
        nameCommercial: 'Test Company',
        idType: 1,
        status: true,
        forcedToAccounting: '',
        specialTaxpayer: 0,
        establishments: [],
      };

      companyService.findById.and.returnValue(
        of({
          code: 200,
          status: 'success',
          message: 'OK',
          companyDetailsDtos: [mockCompany],
          companyDetailsDto: mockCompany,
        })
      );

      service.getAllSubscriptions().subscribe((subscriptions) => {
        expect(subscriptions.length).toBe(1);
        expect(subscriptions[0].idCompany).toBe(1);
      });

      const req = httpMock.expectOne(baseUrl);
      expect(req.request.method).toBe('GET');
      req.flush(mockPageResponse);
    });

    it('should return empty array when no subscriptions', () => {
      const mockPageResponse: SubscriptionPageResponse = {
        content: [],
        totalElements: 0,
        totalPages: 0,
        size: 20,
        number: 0,
        first: true,
        last: true,
        empty: true,
      };

      service.getAllSubscriptions().subscribe((subscriptions) => {
        expect(subscriptions).toEqual([]);
      });

      const req = httpMock.expectOne(baseUrl);
      req.flush(mockPageResponse);
    });
  });

  describe('getCompanySubscription', () => {
    it('should get subscription by company ID', () => {
      const companyId = 1;
      const mockResponse: SubscriptionAdmin = {
        idCompany: companyId,
        companyName: 'Test Company',
        businessName: 'Test Company S.A.',
        ruc: '1234567890001',
        plan: {
          code: 'BASIC',
          name: 'Plan Básico',
          documentLimitYearly: 500,
        },
        subscriptionStatus: 'ACTIVE',
        currentYearUsage: 100,
        percentUsed: 20,
        remainingDocuments: 400,
        startDate: '2024-01-01',
        expiresAt: '2024-12-31',
      };

      service.getCompanySubscription(companyId).subscribe((subscription) => {
        expect(subscription.idCompany).toBe(companyId);
        expect(subscription.plan.code).toBe('BASIC');
      });

      const req = httpMock.expectOne(`${baseUrl}/company/${companyId}`);
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });
  });

  describe('changePlan', () => {
    it('should change subscription plan', () => {
      const companyId = 1;
      const request: PlanChangeRequest = {
        newPlanCode: 'PRO',
        paymentDate: '2024-01-01T00:00:00',
        notes: 'Upgrade to PRO plan',
      };

      service.changePlan(companyId, request).subscribe(() => {
        expect(true).toBe(true);
      });

      const req = httpMock.expectOne(`${baseUrl}/company/${companyId}/upgrade`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(request);
      req.flush(null);
    });
  });

  describe('extendSubscription', () => {
    it('should extend subscription', () => {
      const companyId = 1;
      const request: SubscriptionExtendRequest = {
        extensionMonths: 3,
        reason: 'Extending subscription',
      };

      service.extendSubscription(companyId, request).subscribe(() => {
        expect(true).toBe(true);
      });

      const req = httpMock.expectOne(`${baseUrl}/company/${companyId}/extend`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(request);
      req.flush(null);
    });
  });

  describe('cancelSubscription', () => {
    it('should cancel subscription', () => {
      const companyId = 1;
      const request: SubscriptionCancelRequest = {
        reason: 'CUSTOMER_REQUEST',
        cancelImmediately: false,
        notes: 'Customer requested cancellation',
      };

      service.cancelSubscription(companyId, request).subscribe(() => {
        expect(true).toBe(true);
      });

      const req = httpMock.expectOne(`${baseUrl}/company/${companyId}/cancel`);
      expect(req.request.method).toBe('DELETE');
      expect(req.request.body).toEqual(request);
      req.flush(null);
    });
  });

  describe('getAvailablePlans', () => {
    it('should get available plans from backend', () => {
      const mockResponse: PlanResponse[] = [
        {
          idPlan: 1,
          code: 'FREE',
          name: 'Plan Gratuito',
          description: 'Plan básico gratuito',
          priceAnnual: 0,
          priceMonthly: 0,
          documentLimitYearly: 100,
          userLimit: 1,
          establishmentLimit: 1,
          issuancePointLimit: 1,
          hasApiAccess: false,
          hasMultiUser: false,
          hasAdvancedReports: false,
          hasElectronicInvoice: true,
          hasCreditNote: false,
          hasDebitNote: false,
          hasRetention: false,
          hasGuide: false,
          hasPrioritySupport: false,
          hasCustomBranding: false,
          hasAutomatedBackups: false,
          isActive: true,
          displayOrder: 1,
          features: ['Hasta 100 documentos/año'],
        },
      ];

      service.getAvailablePlans().subscribe((plans) => {
        expect(plans.length).toBe(1);
        expect(plans[0].code).toBe('FREE');
        expect(plans[0].price).toBe(0);
      });

      const req = httpMock.expectOne(`${baseUrl}/plans`);
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });

    it('should use fallback when endpoint fails', () => {
      service.getAvailablePlans().subscribe((plans) => {
        expect(plans.length).toBeGreaterThan(0);
        expect(plans[0].code).toBe('FREE');
      });

      const req = httpMock.expectOne(`${baseUrl}/plans`);
      req.flush(null, { status: 404, statusText: 'Not Found' });
    });
  });

  describe('getCompaniesWithoutSubscription', () => {
    it('should get companies without subscription', () => {
      const mockResponse = [
        {
          idCompany: 1,
          businessName: 'Company 1',
          ruc: '1234567890001',
        },
      ];

      service.getCompaniesWithoutSubscription().subscribe((companies) => {
        expect(companies.length).toBe(1);
        expect(companies[0].idCompany).toBe(1);
      });

      const req = httpMock.expectOne(`${baseUrl}/available-companies`);
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });
  });

  describe('assignSubscription', () => {
    it('should assign subscription to company', () => {
      const request: AssignSubscriptionRequest = {
        idCompany: 1,
        planCode: 'BASIC',
        startDate: '2024-01-01',
        durationMonths: 12,
      };

      service.assignSubscription(request).subscribe(() => {
        expect(true).toBe(true);
      });

      const req = httpMock.expectOne(baseUrl);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(request);
      req.flush(null);
    });
  });
});
