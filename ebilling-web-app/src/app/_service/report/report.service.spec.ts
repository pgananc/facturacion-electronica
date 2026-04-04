import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { ReportService } from './report.service';
import { environment } from '../../../environments/environment';
import { ReportFilterDto } from '../../_model/report/report-filter-dto';
import { SalesDashboardDto } from '../../_model/report/sales-dashboard-dto';
import { WelcomeDashboardResponse } from '../../_model/report/welcome-dashboard-dto';

describe('ReportService', () => {
  let service: ReportService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.HOST_REPORT}/api/report`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ReportService],
    });

    service = TestBed.inject(ReportService);
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

  describe('getSalesDashboard', () => {
    it('should get sales dashboard', () => {
      const filter: ReportFilterDto = {
        companyId: 1,
        dateFrom: '2024-01-01',
        dateTo: '2024-12-31',
      };

      const mockResponse = {
        status: 'OK',
        message: 'Success',
        data: {
          todayTotal: 1000.0,
          weekTotal: 5000.0,
          monthTotal: 20000.0,
          previousMonthTotal: 18000.0,
          salesTrend: [],
          topProducts: [],
          branchDistribution: [],
          additionalMetrics: {
            averageTicket: 450.5,
            totalInvoices: 280,
            totalClients: 156,
            totalProducts: 48,
          },
        } as SalesDashboardDto,
      };

      service.getSalesDashboard(filter).subscribe((data) => {
        expect(data.todayTotal).toBe(1000.0);
        expect(data.monthTotal).toBe(20000.0);
      });

      const req = httpMock.expectOne(
        (request) =>
          request.url === `${baseUrl}/dashboard/sales` &&
          request.params.get('companyId') === '1'
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });

    it('should throw error if companyId is missing', () => {
      const filter: ReportFilterDto = {
        dateFrom: '2024-01-01',
        dateTo: '2024-12-31',
      };

      expect(() => service.getSalesDashboard(filter)).toThrowError(
        'companyId es requerido para cargar el dashboard'
      );
    });

    it('should include branchId when provided', () => {
      const filter: ReportFilterDto = {
        companyId: 1,
        branchId: 1,
        dateFrom: '2024-01-01',
        dateTo: '2024-12-31',
      };

      const mockResponse = {
        status: 'OK',
        message: 'Success',
        data: {} as SalesDashboardDto,
      };

      service.getSalesDashboard(filter).subscribe();

      const req = httpMock.expectOne(
        (request) =>
          request.url === `${baseUrl}/dashboard/sales` &&
          request.params.get('companyId') === '1' &&
          request.params.get('branchId') === '1'
      );
      req.flush(mockResponse);
    });
  });

  describe('getSalesByPeriod', () => {
    it('should get sales by period', () => {
      const filter: ReportFilterDto = {
        companyId: 1,
        dateFrom: '2024-01-01',
        dateTo: '2024-12-31',
      };

      const mockResponse = {
        content: [],
        totalElements: 0,
      };

      service.getSalesByPeriod(filter, 0, 20).subscribe((response) => {
        expect(response).toBeTruthy();
      });

      const req = httpMock.expectOne(
        (request) =>
          request.url === `${baseUrl}/sales/by-period` &&
          request.params.get('page') === '0' &&
          request.params.get('size') === '20'
      );
      expect(req.request.method).toBe('POST');
      req.flush(mockResponse);
    });
  });

  describe('getSalesByClient', () => {
    it('should get sales by client', () => {
      const filter: ReportFilterDto = {
        companyId: 1,
        dateFrom: '2024-01-01',
        dateTo: '2024-12-31',
      };

      const mockResponse = {
        content: [],
        totalElements: 0,
      };

      service.getSalesByClient(filter, 0, 20).subscribe((response) => {
        expect(response).toBeTruthy();
      });

      const req = httpMock.expectOne(
        (request) =>
          request.url === `${baseUrl}/sales/by-client` &&
          request.params.get('page') === '0'
      );
      expect(req.request.method).toBe('POST');
      req.flush(mockResponse);
    });
  });

  describe('getSalesByProduct', () => {
    it('should get sales by product', () => {
      const filter: ReportFilterDto = {
        companyId: 1,
        dateFrom: '2024-01-01',
        dateTo: '2024-12-31',
      };

      const mockResponse = {
        content: [],
        totalElements: 0,
      };

      service.getSalesByProduct(filter, 0, 20).subscribe((response) => {
        expect(response).toBeTruthy();
      });

      const req = httpMock.expectOne(
        (request) =>
          request.url === `${baseUrl}/sales/by-product` &&
          request.params.get('page') === '0'
      );
      expect(req.request.method).toBe('POST');
      req.flush(mockResponse);
    });
  });

  describe('getCreditNoteReport', () => {
    it('should get credit note report', () => {
      const filter: ReportFilterDto = {
        companyId: 1,
        dateFrom: '2024-01-01',
        dateTo: '2024-12-31',
      };

      const mockResponse = {
        items: [],
        totalAmount: 0,
      };

      service.getCreditNoteReport(filter, 0, 10).subscribe((response) => {
        expect(response).toBeTruthy();
      });

      const req = httpMock.expectOne(
        (request) =>
          request.url === `${baseUrl}/credit-notes` &&
          request.params.get('page') === '0'
      );
      expect(req.request.method).toBe('POST');
      req.flush(mockResponse);
    });
  });

  describe('getPendingInvoices', () => {
    it('should get pending invoices', () => {
      const filter: ReportFilterDto = {
        companyId: 1,
        dateFrom: '2024-01-01',
        dateTo: '2024-12-31',
      };

      const mockResponse = {
        items: [],
        totalCount: 0,
      };

      service.getPendingInvoices(filter, 0, 10).subscribe((response) => {
        expect(response).toBeTruthy();
      });

      const req = httpMock.expectOne(`${baseUrl}/invoices/pending`);
      expect(req.request.method).toBe('POST');
      req.flush(mockResponse);
    });
  });

  describe('getBranchComparison', () => {
    it('should get branch comparison', () => {
      const filter: ReportFilterDto = {
        companyId: 1,
        dateFrom: '2024-01-01',
        dateTo: '2024-12-31',
      };

      const mockResponse = {
        branches: [],
        totalSales: 0,
      };

      service.getBranchComparison(filter, 0, 10).subscribe((response) => {
        expect(response).toBeTruthy();
      });

      const req = httpMock.expectOne(`${baseUrl}/dashboard/comparison`);
      expect(req.request.method).toBe('POST');
      req.flush(mockResponse);
    });
  });

  describe('getPaymentMethodReport', () => {
    it('should get payment method report', () => {
      const filter: ReportFilterDto = {
        companyId: 1,
        dateFrom: '2024-01-01',
        dateTo: '2024-12-31',
      };

      const mockResponse = {
        content: [],
        totalElements: 0,
      };

      service.getPaymentMethodReport(filter, 0, 10).subscribe((response) => {
        expect(response).toBeTruthy();
      });

      const req = httpMock.expectOne(
        (request) =>
          request.url === `${baseUrl}/dashboard/payment-methods` &&
          request.params.get('page') === '0'
      );
      expect(req.request.method).toBe('POST');
      req.flush(mockResponse);
    });
  });

  describe('getWelcomeDashboard', () => {
    it('should get welcome dashboard', () => {
      const companyId = 1;
      const mockResponse: WelcomeDashboardResponse = {
        status: 'OK',
        message: 'Success',
        data: {
          kpis: {
            todaySales: { amount: 1000.0, invoiceCount: 5 },
            weekSales: { amount: 5000.0, invoiceCount: 25 },
            monthSales: { amount: 20000.0, invoiceCount: 100 },
            pendingCollection: {
              amount: 5000.0,
              invoiceCount: 5,
              overdueCount: 2,
            },
          },
          salesTrend: [],
          trendSummary: { average: 666.67, total: 20000.0 },
          recentInvoices: [],
          alerts: [],
        },
      };

      service.getWelcomeDashboard(companyId).subscribe((response) => {
        expect(response.data.kpis.monthSales.amount).toBe(20000.0);
      });

      const req = httpMock.expectOne(
        (request) =>
          request.url === `${baseUrl}/dashboard/welcome` &&
          request.params.get('companyId') === '1'
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });
  });
});
