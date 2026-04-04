import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { DraftInvoiceService } from './draft-invoice.service';
import { environment } from '../../../environments/environment';
import { InvoiceDto } from '../../_model/invoice/invoice-dto';

describe('DraftInvoiceService', () => {
  let service: DraftInvoiceService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.HOST_EBILLING}/api/invoice`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [DraftInvoiceService],
    });

    service = TestBed.inject(DraftInvoiceService);
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

  describe('saveDraft', () => {
    it('should save invoice as draft', () => {
      const invoiceDto: InvoiceDto = {
        idInvoice: undefined,
        idCompany: 1,
        idClient: 1,
        issueDate: '2024-01-01',
        totalAmount: 0,
        discount: 0,
        tip: 0,
        subtotalZero: 0,
        subtotalIva: 0,
        subtotalIce: 0,
        subtotalDiscount: 0,
        invoiceDetails: [],
        details: [],
        payments: [],
        clientDto: {} as any,
        establishment: {} as any,
      };

      const mockResponse = {
        idInvoice: 1,
        invoiceNumber: '',
        status: 'DRAFT',
      };

      service.saveDraft(invoiceDto).subscribe((response) => {
        expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(`${baseUrl}/draft`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(invoiceDto);
      req.flush(mockResponse);
    });
  });

  describe('getDrafts', () => {
    it('should get all draft invoices', () => {
      const mockDrafts: InvoiceDto[] = [
        {
          idInvoice: 1,
          idCompany: 1,
          idClient: 1,
          issueDate: '2024-01-01',
          totalAmount: 0,
          discount: 0,
          tip: 0,
          subtotalZero: 0,
          subtotalIva: 0,
          subtotalIce: 0,
          subtotalDiscount: 0,
          invoiceDetails: [],
          details: [],
          payments: [],
          clientDto: {} as any,
          establishment: {} as any,
        },
        {
          idInvoice: 2,
          idCompany: 1,
          idClient: 2,
          issueDate: '2024-01-01',
          totalAmount: 0,
          discount: 0,
          tip: 0,
          subtotalZero: 0,
          subtotalIva: 0,
          subtotalIce: 0,
          subtotalDiscount: 0,
          invoiceDetails: [],
          details: [],
          payments: [],
          clientDto: {} as any,
          establishment: {} as any,
        },
      ];

      service.getDrafts().subscribe((drafts) => {
        expect(drafts).toEqual(mockDrafts);
      });

      const req = httpMock.expectOne(`${baseUrl}/drafts`);
      expect(req.request.method).toBe('GET');
      req.flush(mockDrafts);
    });
  });

  describe('getDraftById', () => {
    it('should get draft invoice by ID', () => {
      const mockDraft: InvoiceDto = {
        idInvoice: 1,
        idCompany: 1,
        idClient: 1,
        issueDate: '2024-01-01',
        totalAmount: 0,
        discount: 0,
        tip: 0,
        subtotalZero: 0,
        subtotalIva: 0,
        subtotalIce: 0,
        subtotalDiscount: 0,
        invoiceDetails: [],
        details: [],
        payments: [],
        clientDto: {} as any,
        establishment: {} as any,
      };

      service.getDraftById(1).subscribe((draft) => {
        expect(draft).toEqual(mockDraft);
      });

      const req = httpMock.expectOne(`${baseUrl}/draft/1`);
      expect(req.request.method).toBe('GET');
      req.flush(mockDraft);
    });
  });

  describe('processDraft', () => {
    it('should process draft invoice', () => {
      const mockResponse = {
        idInvoice: 1,
        invoiceNumber: '001-001-000001',
        authorizationSri: '1234567890',
        status: 'AUTHORIZED',
      };

      service.processDraft(1).subscribe((response) => {
        expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(`${baseUrl}/draft/1/process`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({});
      req.flush(mockResponse);
    });
  });

  describe('deleteDraft', () => {
    it('should delete draft invoice', () => {
      const mockResponse = {
        message: 'Draft deleted successfully',
      };

      service.deleteDraft(1).subscribe((response) => {
        expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(`${baseUrl}/draft/1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(mockResponse);
    });
  });
});
