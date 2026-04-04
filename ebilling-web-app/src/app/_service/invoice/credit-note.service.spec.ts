import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { CreditNoteService } from './credit-note.service';
import { environment } from '../../../environments/environment';
import {
  CreditNoteDto,
  CreditNoteResponse,
  CreditNoteSearchDto,
  CreditNoteReasonDto,
  InvoiceAvailableForCreditNoteDto,
  CreateCreditNoteRequest,
  CreditNoteSearchByCompanyDto,
} from '../../_model/invoice/credit-note-dto';
import { PageResponse } from '../../_model/invoice/debit-note-dto';

describe('CreditNoteService', () => {
  let service: CreditNoteService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.HOST_EBILLING}/api/credit-notes`;
  const reasonsUrl = `${environment.HOST_EBILLING}/api/credit-note-reason`;
  const invoiceUrl = `${environment.HOST_EBILLING}/api/invoice`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [CreditNoteService],
    });

    service = TestBed.inject(CreditNoteService);
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

  describe('getReasons', () => {
    it('should get credit note reasons with cache', () => {
      const mockReasons: CreditNoteReasonDto[] = [
        { idReason: 1, code: '01', description: 'Reason 1' },
        { idReason: 2, code: '02', description: 'Reason 2' },
      ];

      // First call
      service.getReasons().subscribe((reasons) => {
        expect(reasons).toEqual(mockReasons);
      });

      const req1 = httpMock.expectOne(reasonsUrl);
      expect(req1.request.method).toBe('GET');
      req1.flush(mockReasons);

      // Second call should use cache (no new request)
      service.getReasons().subscribe((reasons) => {
        expect(reasons).toEqual(mockReasons);
      });

      httpMock.expectNone(reasonsUrl);
    });
  });

  describe('getInvoiceAvailableForCreditNote', () => {
    it('should get invoice available for credit note', () => {
      const mockInvoice: InvoiceAvailableForCreditNoteDto = {
        idInvoice: 1,
        invoiceNumber: '001-001-000001',
        issueDate: '2024-01-01',
        accessKey: 'test-access-key',
        authorizationNumber: 'test-auth',
        client: {
          idClient: 1,
          identification: '1234567890',
          name: 'Test Client',
          email: 'test@example.com',
        },
        totalInvoice: 100.0,
        totalCreditNotesApplied: 0,
        availableAmount: 100.0,
        details: [],
      };

      service.getInvoiceAvailableForCreditNote(1).subscribe((invoice) => {
        expect(invoice).toEqual(mockInvoice);
      });

      const req = httpMock.expectOne(
        `${invoiceUrl}/1/available-for-credit-note`
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockInvoice);
    });
  });

  describe('createAndSend', () => {
    it('should create and send credit note', () => {
      const request: CreateCreditNoteRequest = {
        idInvoice: 1,
        idReason: 1,
        idIssuancePoint: 1,
        issueDate: '2024-01-01',
        details: [],
        companyDto: { idCompany: 1 },
        clientDto: { idClient: 1 },
      };

      const mockResponse: CreditNoteResponse = {
        idCreditNote: 1,
        creditNoteNumber: '001-001-000001',
        status: 'AUTHORIZED',
        code: 200,
        message: 'Success',
      };

      service.createAndSend(request).subscribe((response) => {
        expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(baseUrl);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(request);
      req.flush(mockResponse);
    });
  });

  describe('saveDraft', () => {
    it('should save credit note as draft', () => {
      const request: CreateCreditNoteRequest = {
        idInvoice: 1,
        idReason: 1,
        idIssuancePoint: 1,
        issueDate: '2024-01-01',
        details: [],
        companyDto: { idCompany: 1 },
        clientDto: { idClient: 1 },
      };

      const mockResponse: CreditNoteResponse = {
        idCreditNote: 1,
        creditNoteNumber: '001-001-000001',
        status: 'DRAFT',
        code: 200,
        message: 'Success',
      };

      service.saveDraft(request).subscribe((response) => {
        expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(`${baseUrl}/draft`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(request);
      req.flush(mockResponse);
    });
  });

  describe('sendDraft', () => {
    it('should send draft credit note to SRI', () => {
      const mockResponse: CreditNoteResponse = {
        idCreditNote: 1,
        creditNoteNumber: '001-001-000001',
        status: 'AUTHORIZED',
        code: 200,
        message: 'Success',
      };

      service.sendDraft(1, 'password123').subscribe((response) => {
        expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(`${baseUrl}/1/send`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ password: 'password123' });
      req.flush(mockResponse);
    });
  });

  describe('sendEmail', () => {
    it('should send credit note by email', () => {
      const mockResponse = {
        success: true,
        message: 'Email sent successfully',
      };

      service.sendEmail(1, 'test@example.com').subscribe((response) => {
        expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(`${baseUrl}/1/send-email`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ email: 'test@example.com' });
      req.flush(mockResponse);
    });

    it('should send credit note by email without email parameter', () => {
      const mockResponse = {
        success: true,
        message: 'Email sent successfully',
      };

      service.sendEmail(1).subscribe((response) => {
        expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(`${baseUrl}/1/send-email`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({});
      req.flush(mockResponse);
    });
  });

  describe('findAll', () => {
    it('should get all credit notes', () => {
      const mockCreditNotes: CreditNoteDto[] = [
        {
          idCreditNote: 1,
          creditNoteNumber: '001-001-000001',
          originalInvoiceId: 1,
        },
      ];

      service.findAll().subscribe((creditNotes) => {
        expect(creditNotes).toEqual(mockCreditNotes);
      });

      const req = httpMock.expectOne(baseUrl);
      expect(req.request.method).toBe('GET');
      req.flush(mockCreditNotes);
    });
  });

  describe('findById', () => {
    it('should get credit note by ID', () => {
      const mockCreditNote: CreditNoteDto = {
        idCreditNote: 1,
        creditNoteNumber: '001-001-000001',
        originalInvoiceId: 1,
      };

      service.findById(1).subscribe((creditNote) => {
        expect(creditNote).toEqual(mockCreditNote);
      });

      const req = httpMock.expectOne(`${baseUrl}/1`);
      expect(req.request.method).toBe('GET');
      req.flush(mockCreditNote);
    });
  });

  describe('create', () => {
    it('should create credit note', () => {
      const creditNote: CreditNoteDto = {
        idCreditNote: undefined,
        creditNoteNumber: '',
        originalInvoiceId: 1,
      };

      const mockResponse: CreditNoteResponse = {
        idCreditNote: 1,
        creditNoteNumber: '001-001-000001',
        status: 'AUTHORIZED',
        code: 200,
        message: 'Success',
      };

      service.create(creditNote).subscribe((response) => {
        expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(baseUrl);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(creditNote);
      req.flush(mockResponse);
    });
  });

  describe('update', () => {
    it('should update credit note', () => {
      const creditNote: CreditNoteDto = {
        idCreditNote: 1,
        creditNoteNumber: '001-001-000001',
        originalInvoiceId: 1,
      };

      const mockResponse: CreditNoteResponse = {
        idCreditNote: 1,
        creditNoteNumber: '001-001-000001',
        status: 'DRAFT',
        code: 200,
        message: 'Success',
      };

      service.update(1, creditNote).subscribe((response) => {
        expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(`${baseUrl}/1`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(creditNote);
      req.flush(mockResponse);
    });
  });

  describe('delete', () => {
    it('should delete credit note', () => {
      service.delete(1).subscribe((response) => {
        expect(response).toBeNull();
      });

      const req = httpMock.expectOne(`${baseUrl}/1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('search', () => {
    it('should search credit notes with filters', () => {
      const searchDto: CreditNoteSearchDto = {
        creditNoteNumber: '001-001-000001',
        idClient: 1,
        originalInvoiceId: 1,
      };

      service.search(searchDto, 0, 10, 'id', 'asc').subscribe((response) => {
        expect(response).toBeTruthy();
      });

      const req = httpMock.expectOne(
        (request) => request.url === `${baseUrl}/search`
      );
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('page')).toBe('0');
      expect(req.request.params.get('size')).toBe('10');
      expect(req.request.params.get('creditNoteNumber')).toBe('001-001-000001');
      expect(req.request.params.get('idClient')).toBe('1');
      expect(req.request.params.get('originalInvoiceId')).toBe('1');
      expect(req.request.params.get('sort')).toBe('id,asc');
      req.flush({ content: [], totalElements: 0 });
    });
  });

  describe('findByOriginalInvoice', () => {
    it('should get credit notes by original invoice', () => {
      const mockCreditNotes: CreditNoteDto[] = [
        {
          idCreditNote: 1,
          creditNoteNumber: '001-001-000001',
          originalInvoiceId: 1,
        },
      ];

      service.findByOriginalInvoice(1).subscribe((creditNotes) => {
        expect(creditNotes).toEqual(mockCreditNotes);
      });

      const req = httpMock.expectOne(`${baseUrl}/by-invoice/1`);
      expect(req.request.method).toBe('GET');
      req.flush(mockCreditNotes);
    });
  });

  describe('downloadPdf', () => {
    it('should download credit note PDF', () => {
      const blob = new Blob(['test'], { type: 'application/pdf' });

      service.downloadPdf(1).subscribe((response) => {
        expect(response).toBeInstanceOf(Blob);
      });

      const req = httpMock.expectOne(`${baseUrl}/1/pdf`);
      expect(req.request.method).toBe('GET');
      expect(req.request.responseType).toBe('blob');
      req.flush(blob);
    });
  });

  describe('downloadXml', () => {
    it('should download credit note XML', () => {
      const blob = new Blob(['test'], { type: 'application/xml' });

      service.downloadXml(1).subscribe((response) => {
        expect(response).toBeInstanceOf(Blob);
      });

      const req = httpMock.expectOne(`${baseUrl}/1/xml`);
      expect(req.request.method).toBe('GET');
      expect(req.request.responseType).toBe('blob');
      req.flush(blob);
    });
  });

  describe('searchByCompany', () => {
    it('should search credit notes by company', () => {
      const searchDto: CreditNoteSearchByCompanyDto = {
        idCompany: 1,
        creditNoteNumber: '001-001-000001',
      };

      const mockResponse: PageResponse<CreditNoteDto> = {
        content: [
          {
            idCreditNote: 1,
            creditNoteNumber: '001-001-000001',
            originalInvoiceId: 1,
          },
        ],
        totalElements: 1,
        totalPages: 1,
        size: 10,
        number: 0,
      };

      service.searchByCompany(searchDto, 0, 10).subscribe((response) => {
        expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(
        (request) => request.url === `${baseUrl}/search-by-company`
      );
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(searchDto);
      expect(req.request.params.get('page')).toBe('0');
      expect(req.request.params.get('size')).toBe('10');
      req.flush(mockResponse);
    });
  });
});
