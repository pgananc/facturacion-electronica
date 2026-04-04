import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { DebitNoteService } from './debit-note.service';
import { environment } from '../../../environments/environment';
import {
  DebitNoteDto,
  DebitNoteResponse,
  DebitNoteQueryDto,
  DebitNoteSearchByCompanyDto,
  DebitNoteListResponseDto,
  PageResponse,
} from '../../_model/invoice/debit-note-dto';

describe('DebitNoteService', () => {
  let service: DebitNoteService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.HOST_EBILLING}/api/debit-notes`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [DebitNoteService],
    });

    service = TestBed.inject(DebitNoteService);
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

  describe('create', () => {
    it('should create and send debit note', () => {
      const debitNote: DebitNoteDto = {
        idDebitNote: undefined,
        debitNoteNumber: '',
        originalInvoiceId: 1,
      };

      const mockResponse: DebitNoteResponse = {
        idDebitNote: 1,
        debitNoteNumber: '001-001-000001',
        status: 'AUTHORIZED',
        code: 200,
        message: 'Success',
      };

      service.create(debitNote).subscribe((response) => {
        expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(baseUrl);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(debitNote);
      req.flush(mockResponse);
    });
  });

  describe('createDraft', () => {
    it('should save debit note as draft', () => {
      const debitNote: DebitNoteDto = {
        idDebitNote: undefined,
        debitNoteNumber: '',
        originalInvoiceId: 1,
      };

      const mockResponse: DebitNoteResponse = {
        idDebitNote: 1,
        debitNoteNumber: '001-001-000001',
        status: 'DRAFT',
        code: 200,
        message: 'Success',
      };

      service.createDraft(debitNote).subscribe((response) => {
        expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(`${baseUrl}/draft`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(debitNote);
      req.flush(mockResponse);
    });
  });

  describe('update', () => {
    it('should update debit note', () => {
      const debitNote: Partial<DebitNoteDto> = {
        debitNoteNumber: '001-001-000001',
      };

      const mockResponse: DebitNoteResponse = {
        idDebitNote: 1,
        debitNoteNumber: '001-001-000001',
        status: 'DRAFT',
        code: 200,
        message: 'Success',
      };

      service.update(1, debitNote).subscribe((response) => {
        expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(`${baseUrl}/1`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(debitNote);
      req.flush(mockResponse);
    });
  });

  describe('delete', () => {
    it('should delete debit note', () => {
      service.delete(1).subscribe((response) => {
        expect(response).toBeNull();
      });

      const req = httpMock.expectOne(`${baseUrl}/1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('findById', () => {
    it('should get debit note by ID', () => {
      const mockDebitNote: DebitNoteDto = {
        idDebitNote: 1,
        debitNoteNumber: '001-001-000001',
        originalInvoiceId: 1,
      };

      service.findById(1).subscribe((debitNote) => {
        expect(debitNote).toEqual(mockDebitNote);
      });

      const req = httpMock.expectOne(`${baseUrl}/1`);
      expect(req.request.method).toBe('GET');
      req.flush(mockDebitNote);
    });
  });

  describe('findAll', () => {
    it('should get all debit notes', () => {
      const mockDebitNotes: DebitNoteDto[] = [
        {
          idDebitNote: 1,
          debitNoteNumber: '001-001-000001',
          originalInvoiceId: 1,
        },
      ];

      service.findAll().subscribe((debitNotes) => {
        expect(debitNotes).toEqual(mockDebitNotes);
      });

      const req = httpMock.expectOne(baseUrl);
      expect(req.request.method).toBe('GET');
      req.flush(mockDebitNotes);
    });
  });

  describe('searchByCompany', () => {
    it('should search debit notes by company', () => {
      const searchDto: DebitNoteSearchByCompanyDto = {
        idCompany: 1,
      };

      const mockResponse: PageResponse<DebitNoteListResponseDto> = {
        content: [
          {
            idDebitNote: 1,
            debitNoteNumber: '001-001-000001',
            originalInvoiceNumber: '001-001-000001',
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

  describe('search', () => {
    it('should search debit notes with filters', () => {
      const query: DebitNoteQueryDto = {
        debitNoteNumber: '001-001-000001',
        idClient: 1,
        originalInvoiceId: 1,
        creationDateFrom: '2024-01-01',
        creationDateTo: '2024-12-31',
      };

      service.search(query, 0, 20, 'id,asc').subscribe((response) => {
        expect(response).toBeTruthy();
      });

      const req = httpMock.expectOne(
        (request) => request.url === `${baseUrl}/search`
      );
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('page')).toBe('0');
      expect(req.request.params.get('size')).toBe('20');
      expect(req.request.params.get('debitNoteNumber')).toBe('001-001-000001');
      expect(req.request.params.get('idClient')).toBe('1');
      expect(req.request.params.get('originalInvoiceId')).toBe('1');
      expect(req.request.params.get('creationDateFrom')).toBe('2024-01-01');
      expect(req.request.params.get('creationDateTo')).toBe('2024-12-31');
      expect(req.request.params.get('sort')).toBe('id,asc');
      req.flush({ content: [], totalElements: 0 });
    });
  });

  describe('findByInvoiceId', () => {
    it('should get debit notes by invoice ID', () => {
      const mockDebitNotes: DebitNoteDto[] = [
        {
          idDebitNote: 1,
          debitNoteNumber: '001-001-000001',
          originalInvoiceId: 1,
        },
      ];

      service.findByInvoiceId(1).subscribe((debitNotes) => {
        expect(debitNotes).toEqual(mockDebitNotes);
      });

      const req = httpMock.expectOne(`${baseUrl}/by-invoice/1`);
      expect(req.request.method).toBe('GET');
      req.flush(mockDebitNotes);
    });
  });

  describe('sendToSri', () => {
    it('should send draft debit note to SRI', () => {
      const mockResponse: DebitNoteResponse = {
        idDebitNote: 1,
        debitNoteNumber: '001-001-000001',
        status: 'AUTHORIZED',
        code: 200,
        message: 'Success',
      };

      service.sendToSri(1, 'password123').subscribe((response) => {
        expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(`${baseUrl}/1/send`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ password: 'password123' });
      req.flush(mockResponse);
    });
  });

  describe('downloadPdf', () => {
    it('should download debit note PDF', () => {
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
    it('should download debit note XML', () => {
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

  describe('sendEmail', () => {
    it('should send debit note by email', () => {
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

    it('should send debit note by email without email parameter', () => {
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
});
