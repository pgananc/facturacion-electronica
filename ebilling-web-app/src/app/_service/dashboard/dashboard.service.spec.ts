import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { DashboardService } from './dashboard.service';
import { environment } from '../../../environments/environment';
import { WelcomeDashboardResponse } from '../../_model/report/welcome-dashboard-dto';

describe('DashboardService', () => {
  let service: DashboardService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.HOST_EBILLING}/api/dashboard`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [DashboardService],
    });

    service = TestBed.inject(DashboardService);
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

  describe('getWelcomeDashboard', () => {
    it('should get welcome dashboard for a company', () => {
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
        expect(response).toEqual(mockResponse);
        expect(response.data.kpis.monthSales.amount).toBe(20000.0);
      });

      const req = httpMock.expectOne(
        (request) =>
          request.url === `${baseUrl}/welcome` &&
          request.params.get('companyId') === '1'
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });

    it('should handle errors when getting dashboard', () => {
      const companyId = 1;

      service.getWelcomeDashboard(companyId).subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(error.status).toBe(500);
        },
      });

      const req = httpMock.expectOne(
        (request) =>
          request.url === `${baseUrl}/welcome` &&
          request.params.get('companyId') === '1'
      );
      req.flush(null, { status: 500, statusText: 'Internal Server Error' });
    });
  });
});
