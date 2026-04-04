import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { InvoiceService } from './invoice.service';
import { environment } from '../../../environments/environment';
import { InvoiceDto } from '../../_model/invoice/invoice-dto';
import { InvoiceSearchDto } from '../../_model/invoice/invoice-search-dto';
import { DocumentQueryDto } from '../../_model/invoice/document-query-dto';
import { InvoiceSearchForDebitNoteDto } from '../../_model/invoice/invoice-dto';
import { InvoiceSearchForCreditNoteDto } from '../../_model/invoice/invoice-dto';

describe('InvoiceService', () => {
  let service: InvoiceService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [InvoiceService],
    });

    service = TestBed.inject(InvoiceService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('register', () => {
    it('should register a new invoice', (done) => {
      const mockInvoice: InvoiceDto = {
        idInvoice: 0,
        idCompany: 1,
        issueDate: '2025-01-01',
        clientDto: {
          identification: '1234567890',
          idType: 1,
          name: 'Test Client',
          email: 'test@example.com',
        },
        establishment: {
          idEstablishment: 1,
          code: '001',
        },
        invoiceDetails: [],
        payments: [],
        discount: 0,
        tip: 0,
        subtotalZero: 0,
        subtotalIva: 100,
        subtotalIce: 0,
        subtotalDiscount: 0,
        totalAmount: 115,
      };

      const mockResponse = { ...mockInvoice, idInvoice: 1 };

      service.register(mockInvoice).subscribe((response) => {
        expect(response).toBeTruthy();
        expect(response.idInvoice).toBe(1);
        done();
      });

      const req = httpMock.expectOne(
        `${environment.HOST_EBILLING}/api/invoice`
      );
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(mockInvoice);
      req.flush(mockResponse);
    });
  });

  describe('findById', () => {
    it('should get invoice by id', (done) => {
      const mockInvoice = {
        idInvoice: 1,
        totalAmount: 115,
      };

      service.findById(1).subscribe((invoice) => {
        expect(invoice).toBeTruthy();
        expect(invoice.idInvoice).toBe(1);
        done();
      });

      const req = httpMock.expectOne(
        `${environment.HOST_EBILLING}/api/invoice/1`
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockInvoice);
    });
  });

  describe('findAll', () => {
    it('should get all invoices with default pagination', (done) => {
      const mockResponse = {
        content: [],
        totalElements: 0,
        totalPages: 0,
      };

      service.findAll().subscribe((response) => {
        expect(response).toBeTruthy();
        done();
      });

      const req = httpMock.expectOne(
        `${environment.HOST_EBILLING}/api/invoice?page=0&size=10`
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });

    it('should get all invoices with custom pagination', (done) => {
      const mockResponse = {
        content: [],
        totalElements: 0,
        totalPages: 0,
      };

      service.findAll(2, 20).subscribe((response) => {
        expect(response).toBeTruthy();
        done();
      });

      const req = httpMock.expectOne(
        `${environment.HOST_EBILLING}/api/invoice?page=2&size=20`
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });
  });

  describe('searchPageable', () => {
    it('should search invoices with pagination', (done) => {
      const searchDto: InvoiceSearchDto = {
        buyerIdentification: '1234567890',
      };
      const mockResponse = {
        content: [],
        totalElements: 0,
        totalPages: 0,
      };

      service.searchPageable(0, 10, searchDto).subscribe((response) => {
        expect(response).toBeTruthy();
        done();
      });

      const req = httpMock.expectOne(
        `${environment.HOST_EBILLING}/api/invoice/query?page=0&size=10`
      );
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(searchDto);
      req.flush(mockResponse);
    });

    it('should search invoices with sorting', (done) => {
      const searchDto: InvoiceSearchDto = {
        buyerIdentification: '1234567890',
      };
      const mockResponse = {
        content: [],
        totalElements: 0,
        totalPages: 0,
      };

      service
        .searchPageable(0, 10, searchDto, 'issueDate', 'DESC')
        .subscribe((response) => {
          expect(response).toBeTruthy();
          done();
        });

      const req = httpMock.expectOne(
        `${environment.HOST_EBILLING}/api/invoice/query?page=0&size=10&sort=issueDate,DESC`
      );
      expect(req.request.method).toBe('POST');
      req.flush(mockResponse);
    });

    it('should not add sort parameter when sortField is empty', (done) => {
      const searchDto: InvoiceSearchDto = {
        buyerIdentification: '1234567890',
      };
      const mockResponse = {
        content: [],
        totalElements: 0,
        totalPages: 0,
      };

      service.searchPageable(0, 10, searchDto, '', 'DESC').subscribe(() => {
        done();
      });

      const req = httpMock.expectOne(
        `${environment.HOST_EBILLING}/api/invoice/query?page=0&size=10`
      );
      expect(req.request.url).not.toContain('sort=');
      req.flush(mockResponse);
    });
  });

  describe('downloadPdf', () => {
    it('should download PDF as blob', (done) => {
      const mockBlob = new Blob(['PDF content'], { type: 'application/pdf' });

      service.downloadPdf(1).subscribe((blob) => {
        expect(blob).toBeTruthy();
        expect(blob.type).toBe('application/pdf');
        done();
      });

      const req = httpMock.expectOne(
        `${environment.HOST_EBILLING}/api/invoice/1/pdf`
      );
      expect(req.request.method).toBe('GET');
      expect(req.request.responseType).toBe('blob');
      req.flush(mockBlob);
    });
  });

  describe('downloadXml', () => {
    it('should download XML as blob', (done) => {
      const mockBlob = new Blob(['XML content'], { type: 'application/xml' });

      service.downloadXml(1).subscribe((blob) => {
        expect(blob).toBeTruthy();
        expect(blob.type).toBe('application/xml');
        done();
      });

      const req = httpMock.expectOne(
        `${environment.HOST_EBILLING}/api/invoice/1/xml`
      );
      expect(req.request.method).toBe('GET');
      expect(req.request.responseType).toBe('blob');
      req.flush(mockBlob);
    });
  });

  describe('queryDocuments', () => {
    it('should query documents with pagination', (done) => {
      const queryDto: DocumentQueryDto = {
        documentType: 1, // 1 = Solo Facturas
        creationDateFrom: '2025-01-01',
        creationDateTo: '2025-12-31',
      };
      const mockResponse = {
        content: [],
        totalElements: 0,
        totalPages: 0,
      };

      service.queryDocuments(0, 10, queryDto).subscribe((response) => {
        expect(response).toBeTruthy();
        done();
      });

      const req = httpMock.expectOne(
        `${environment.HOST_EBILLING}/api/invoice/documents/query?page=0&size=10`
      );
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(queryDto);
      req.flush(mockResponse);
    });

    it('should query documents with sorting', (done) => {
      const queryDto: DocumentQueryDto = {
        documentType: 1, // 1 = Solo Facturas
      };
      const mockResponse = {
        content: [],
        totalElements: 0,
        totalPages: 0,
      };

      service
        .queryDocuments(0, 10, queryDto, 'issueDate', 'ASC')
        .subscribe((response) => {
          expect(response).toBeTruthy();
          done();
        });

      const req = httpMock.expectOne(
        `${environment.HOST_EBILLING}/api/invoice/documents/query?page=0&size=10&sort=issueDate,ASC`
      );
      expect(req.request.method).toBe('POST');
      req.flush(mockResponse);
    });
  });

  describe('searchForDebitNote', () => {
    it('should search invoices for debit note', (done) => {
      const searchDto: InvoiceSearchForDebitNoteDto = {
        idCompany: 1,
        identificacionCliente: '1234567890',
      };
      const mockResponse = {
        content: [],
        totalElements: 0,
        totalPages: 0,
      };

      service.searchForDebitNote(searchDto, 0, 100).subscribe((response) => {
        expect(response).toBeTruthy();
        done();
      });

      const req = httpMock.expectOne(
        (request) =>
          request.url ===
            `${environment.HOST_EBILLING}/api/invoice/search-for-debit-note` &&
          request.params.get('page') === '0' &&
          request.params.get('size') === '100'
      );
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(searchDto);
      req.flush(mockResponse);
    });
  });

  describe('searchForCreditNote', () => {
    it('should search invoices for credit note', (done) => {
      const searchDto: InvoiceSearchForCreditNoteDto = {
        idCompany: 1,
        identificacionCliente: '1234567890',
      };
      const mockResponse = {
        content: [],
        totalElements: 0,
        totalPages: 0,
      };

      service.searchForCreditNote(searchDto, 0, 100).subscribe((response) => {
        expect(response).toBeTruthy();
        done();
      });

      const req = httpMock.expectOne(
        (request) =>
          request.url ===
            `${environment.HOST_EBILLING}/api/invoice/search-for-credit-note` &&
          request.params.get('page') === '0' &&
          request.params.get('size') === '100'
      );
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(searchDto);
      req.flush(mockResponse);
    });
  });
});
