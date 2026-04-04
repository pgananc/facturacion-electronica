import {
  ComponentFixture,
  TestBed,
  fakeAsync,
  tick,
} from '@angular/core/testing';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { of, throwError } from 'rxjs';
import { ViewingInvoicesComponent } from './viewing-invoices.component';
import { InvoiceService } from '../../../_service/invoice/invoice.service';
import { CreditNoteService } from '../../../_service/invoice/credit-note.service';
import { DocumentSearchResponseDto } from '../../../_model/invoice/document-query-dto';
import { environment } from '../../../../environments/environment';

describe('ViewingInvoicesComponent', () => {
  let component: ViewingInvoicesComponent;
  let fixture: ComponentFixture<ViewingInvoicesComponent>;
  let invoiceServiceSpy: jasmine.SpyObj<InvoiceService>;
  let creditNoteServiceSpy: jasmine.SpyObj<CreditNoteService>;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;
  let routerSpy: jasmine.SpyObj<Router>;

  const mockDocuments: DocumentSearchResponseDto[] = [
    {
      documentType: 1,
      idDocument: 1,
      documentNumber: '001-001-000000123',
      creationDate: '2024-01-15',
      nameClient: 'Cliente Test 1',
      totalAmount: 1000.0,
      authorizationStatusSri: 'AUTORIZADO',
      authorizationNumber: '1234567890',
    },
    {
      documentType: 2,
      idDocument: 2,
      documentNumber: '001-001-000000456',
      creationDate: '2024-01-16',
      nameClient: 'Cliente Test 2',
      totalAmount: 2000.0,
      authorizationStatusSri: 'AUTORIZADO',
      authorizationNumber: '0987654321',
    },
  ];

  const mockPaginationResponse = {
    content: mockDocuments,
    totalElements: 2,
    totalPages: 1,
    size: 10,
    number: 0,
  };

  beforeEach(async () => {
    const invoiceService = jasmine.createSpyObj('InvoiceService', [
      'queryDocuments',
      'downloadPdf',
      'downloadXml',
    ]);
    const creditNoteService = jasmine.createSpyObj('CreditNoteService', [
      'downloadPdf',
      'downloadXml',
    ]);
    const snackBar = jasmine.createSpyObj('MatSnackBar', ['open']);
    const router = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      declarations: [ViewingInvoicesComponent],
      imports: [
        ReactiveFormsModule,
        MatTableModule,
        MatPaginatorModule,
        MatSortModule,
        MatSnackBarModule,
        MatSelectModule,
        MatFormFieldModule,
        MatInputModule,
        BrowserAnimationsModule,
      ],
      providers: [
        FormBuilder,
        { provide: InvoiceService, useValue: invoiceService },
        { provide: CreditNoteService, useValue: creditNoteService },
        { provide: MatSnackBar, useValue: snackBar },
        { provide: Router, useValue: router },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();

    fixture = TestBed.createComponent(ViewingInvoicesComponent);
    component = fixture.componentInstance;
    invoiceServiceSpy = TestBed.inject(
      InvoiceService
    ) as jasmine.SpyObj<InvoiceService>;
    creditNoteServiceSpy = TestBed.inject(
      CreditNoteService
    ) as jasmine.SpyObj<CreditNoteService>;
    snackBarSpy = TestBed.inject(MatSnackBar) as jasmine.SpyObj<MatSnackBar>;
    routerSpy = TestBed.inject(Router) as jasmine.SpyObj<Router>;

    // Setup default mocks
    invoiceServiceSpy.queryDocuments.and.returnValue(
      of(mockPaginationResponse)
    );
    invoiceServiceSpy.downloadPdf.and.returnValue(
      of(new Blob(['pdf content'], { type: 'application/pdf' }))
    );
    invoiceServiceSpy.downloadXml.and.returnValue(
      of(new Blob(['xml content'], { type: 'application/xml' }))
    );
    creditNoteServiceSpy.downloadPdf.and.returnValue(
      of(new Blob(['pdf content'], { type: 'application/pdf' }))
    );
    creditNoteServiceSpy.downloadXml.and.returnValue(
      of(new Blob(['xml content'], { type: 'application/xml' }))
    );

    // Mock localStorage - se reseteará en cada test individual si es necesario
    spyOn(localStorage, 'getItem').and.returnValue('mock-token');
    spyOn(localStorage, 'removeItem').and.stub();

    // Mock window methods
    spyOn(window.URL, 'createObjectURL').and.returnValue('blob:url');
    spyOn(window.URL, 'revokeObjectURL').and.stub();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('ngOnInit', () => {
    it('should initialize form with default values', () => {
      component.ngOnInit();
      expect(component.searchForm).toBeDefined();
      expect(component.searchForm.get('documentType')?.value).toBe('0');
    });

    it('should load stored identification number from localStorage', () => {
      (localStorage.getItem as jasmine.Spy).and.callFake((key: string) => {
        if (key === 'consultationIdentificationNumber') return '1234567890';
        if (key === environment.TOKEN) return 'mock-token';
        return null;
      });
      component.ngOnInit();
      expect(component.searchForm.get('buyerIdentification')?.value).toBe(
        '1234567890'
      );
    });

    it('should clear dataSource when documentType changes', () => {
      component.ngOnInit();
      component.dataSource.data = [{ id: 1 } as any];
      component.quantity = 1;
      component.searchForm.patchValue({ documentType: '1' });
      expect(component.dataSource.data.length).toBe(0);
      expect(component.quantity).toBe(0);
    });
  });

  describe('ngAfterViewInit', () => {
    it('should setup dataSource sort and paginator', () => {
      component.ngOnInit();
      fixture.detectChanges();
      component.ngAfterViewInit();
      expect(component.sort).toBeDefined();
      expect(component.paginator).toBeDefined();
    });
  });

  describe('applyFilter', () => {
    beforeEach(() => {
      component.ngOnInit();
      component.searchForm.patchValue({
        creationDateFrom: new Date('2024-01-01'),
        creationDateTo: new Date('2024-01-31'),
      });
    });

    it('should load documents when form is valid', () => {
      spyOn(component as any, 'loadDocuments');
      component.applyFilter();
      expect((component as any).loadDocuments).toHaveBeenCalledWith(0, 10);
    });

    it('should not load documents when form is invalid', () => {
      component.searchForm.patchValue({
        creationDateFrom: null,
        creationDateTo: null,
      });
      spyOn(component as any, 'loadDocuments');
      component.applyFilter();
      expect((component as any).loadDocuments).not.toHaveBeenCalled();
    });
  });

  describe('isFormValid', () => {
    beforeEach(() => {
      component.ngOnInit();
    });

    it('should return true when dates are set', () => {
      component.searchForm.patchValue({
        creationDateFrom: new Date('2024-01-01'),
        creationDateTo: new Date('2024-01-31'),
      });
      expect(component.isFormValid).toBe(true);
    });

    it('should return false when dates are not set', () => {
      component.searchForm.patchValue({
        creationDateFrom: null,
        creationDateTo: null,
      });
      expect(component.isFormValid).toBe(false);
    });
  });

  describe('clearFilters', () => {
    beforeEach(() => {
      component.ngOnInit();
      component.dataSource.data = [{ id: 1 } as any];
      component.quantity = 1;
    });

    it('should reset form and clear dataSource', () => {
      component.searchForm.patchValue({
        invoiceNumber: '123',
        buyerIdentification: '1234567890',
      });
      component.clearFilters();
      expect(component.searchForm.get('invoiceNumber')?.value).toBe('');
      expect(component.dataSource.data.length).toBe(0);
      expect(component.quantity).toBe(0);
    });

    it('should preserve buyerIdentification if set', () => {
      component.searchForm.patchValue({
        buyerIdentification: '1234567890',
      });
      component.clearFilters();
      expect(component.searchForm.get('buyerIdentification')?.value).toBe(
        '1234567890'
      );
    });
  });

  describe('goBackToConsultation', () => {
    it('should clear localStorage and navigate', () => {
      component.goBackToConsultation();
      expect(localStorage.removeItem).toHaveBeenCalledWith(
        'consultationIdentificationNumber'
      );
      expect(localStorage.removeItem).toHaveBeenCalledWith('clientEmail');
      expect(localStorage.removeItem).toHaveBeenCalledWith('clientToken');
      expect(routerSpy.navigate).toHaveBeenCalledWith(
        ['/invoice-consultation'],
        { queryParams: { reset: 'true' } }
      );
    });
  });

  describe('showMore', () => {
    it('should load documents with pagination', () => {
      spyOn(component as any, 'loadDocuments');
      const event = { pageIndex: 1, pageSize: 20 };
      component.showMore(event);
      expect((component as any).loadDocuments).toHaveBeenCalledWith(1, 20);
    });
  });

  describe('loadDocuments', () => {
    beforeEach(() => {
      component.ngOnInit();
      component.searchForm.patchValue({
        creationDateFrom: new Date('2024-01-01'),
        creationDateTo: new Date('2024-01-31'),
      });
    });

    it('should load documents and update dataSource', () => {
      (component as any).loadDocuments(0, 10);
      expect(invoiceServiceSpy.queryDocuments).toHaveBeenCalled();
      expect(component.dataSource.data.length).toBe(2);
      expect(component.quantity).toBe(2);
    });

    it('should handle array response', () => {
      invoiceServiceSpy.queryDocuments.and.returnValue(of(mockDocuments));
      (component as any).loadDocuments(0, 10);
      expect(component.dataSource.data.length).toBe(2);
    });

    it('should navigate to login when no token', () => {
      (localStorage.getItem as jasmine.Spy).and.returnValue(null);
      (component as any).loadDocuments(0, 10);
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/login']);
      expect(snackBarSpy.open).toHaveBeenCalled();
    });

    it('should handle 401 error', () => {
      invoiceServiceSpy.queryDocuments.and.returnValue(
        throwError(() => ({ status: 401 }))
      );
      (component as any).loadDocuments(0, 10);
      expect(snackBarSpy.open).toHaveBeenCalled();
    });

    it('should handle other errors', () => {
      invoiceServiceSpy.queryDocuments.and.returnValue(
        throwError(() => ({ status: 500 }))
      );
      (component as any).loadDocuments(0, 10);
      expect(snackBarSpy.open).toHaveBeenCalled();
    });
  });

  describe('downloadPdf', () => {
    beforeEach(() => {
      // Mock DOM methods
      const mockLink = {
        href: '',
        download: '',
        style: { display: '' },
        click: jasmine.createSpy('click'),
      };
      spyOn(document, 'createElement').and.returnValue(mockLink as any);
      spyOn(document.body, 'appendChild').and.stub();
      spyOn(document.body, 'removeChild').and.stub();
    });

    it('should download PDF for invoice', (done) => {
      component.downloadPdf(1, '001-001-000000123', 1);
      expect(invoiceServiceSpy.downloadPdf).toHaveBeenCalledWith(1);
      setTimeout(() => {
        expect(snackBarSpy.open).toHaveBeenCalled();
        done();
      }, 100);
    });

    it('should download PDF for credit note', (done) => {
      component.downloadPdf(2, '001-001-000000456', 2);
      expect(creditNoteServiceSpy.downloadPdf).toHaveBeenCalledWith(2);
      setTimeout(() => {
        expect(snackBarSpy.open).toHaveBeenCalled();
        done();
      }, 100);
    });

    it('should handle error when downloading PDF', () => {
      invoiceServiceSpy.downloadPdf.and.returnValue(
        throwError(() => ({ error: { message: 'Error' } }))
      );
      component.downloadPdf(1, '001-001-000000123', 1);
      expect(snackBarSpy.open).toHaveBeenCalled();
    });

    it('should handle JSON error response', fakeAsync(() => {
      const errorBlob = new Blob(['{"message":"Error"}'], {
        type: 'application/json',
      });
      invoiceServiceSpy.downloadPdf.and.returnValue(of(errorBlob));
      // Mock blob.text() method
      spyOn(errorBlob, 'text').and.returnValue(
        Promise.resolve('{"message":"Error"}')
      );
      component.downloadPdf(1, '001-001-000000123', 1);
      // Avanzar el tiempo para ejecutar las promesas pendientes
      tick(100);
      expect(snackBarSpy.open).toHaveBeenCalled();
    }));
  });

  describe('downloadXml', () => {
    beforeEach(() => {
      const mockLink = {
        href: '',
        download: '',
        style: { display: '' },
        click: jasmine.createSpy('click'),
      };
      spyOn(document, 'createElement').and.returnValue(mockLink as any);
      spyOn(document.body, 'appendChild').and.stub();
      spyOn(document.body, 'removeChild').and.stub();
    });

    it('should download XML for invoice', (done) => {
      component.downloadXml(1, '1234567890', 1);
      expect(invoiceServiceSpy.downloadXml).toHaveBeenCalledWith(1);
      setTimeout(() => {
        expect(snackBarSpy.open).toHaveBeenCalled();
        done();
      }, 100);
    });

    it('should download XML for credit note', (done) => {
      component.downloadXml(2, '0987654321', 2);
      expect(creditNoteServiceSpy.downloadXml).toHaveBeenCalledWith(2);
      setTimeout(() => {
        expect(snackBarSpy.open).toHaveBeenCalled();
        done();
      }, 100);
    });
  });

  describe('printTicket', () => {
    it('should open print window for invoice', () => {
      const mockWindow = {
        closed: false,
      };
      spyOn(window, 'open').and.returnValue(mockWindow as any);
      component.printTicket(1, 1);
      expect(window.open).toHaveBeenCalled();
    });

    it('should not print ticket for credit note', () => {
      component.printTicket(2, 2);
      expect(snackBarSpy.open).toHaveBeenCalledWith(
        'No se puede imprimir recibo para notas de crédito',
        'Cerrar',
        { duration: 3000 }
      );
    });

    it('should handle window open failure', () => {
      spyOn(window, 'open').and.returnValue(null);
      component.printTicket(1, 1);
      expect(snackBarSpy.open).toHaveBeenCalled();
    });
  });

  describe('ngOnDestroy', () => {
    it('should cleanup subscriptions and timers', () => {
      component.ngOnInit();
      spyOn(component as any, 'stopInactivityTimer');
      spyOn(component as any, 'removeActivityListeners');
      component.ngOnDestroy();
      expect((component as any).stopInactivityTimer).toHaveBeenCalled();
      expect((component as any).removeActivityListeners).toHaveBeenCalled();
    });
  });
});
