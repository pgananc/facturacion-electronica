import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import {
  MatDialogRef,
  MAT_DIALOG_DATA,
  MatDialog,
} from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatTableModule } from '@angular/material/table';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { NO_ERRORS_SCHEMA, ChangeDetectorRef } from '@angular/core';
import { of, throwError } from 'rxjs';
import {
  CreateDebitNoteComponent,
  CreateDebitNoteDialogData,
} from './create-debit-note.component';
import { DebitNoteService } from '../../../../_service/invoice/debit-note.service';
import { InvoiceService } from '../../../../_service/invoice/invoice.service';
import { ProductCacheService } from '../../../../_service/product/product-cache.service';
import { ProductService } from '../../../../_service/product/product.service';
import { PaymentMethodService } from '../../../../_service/invoice/payment-method.service';
import { CompanyService } from '../../../../_service/customer/company.service';
import { LoggerService } from '../../../../_service/logger/logger.service';
import { PermissionService } from '../../../../_service/permission/permission.service';
import { InvoiceDto, PaymentDto } from '../../../../_model/invoice/invoice-dto';
import { ProductDto } from '../../../../_model/product/product-dto';

describe('CreateDebitNoteComponent', () => {
  let component: CreateDebitNoteComponent;
  let fixture: ComponentFixture<CreateDebitNoteComponent>;
  let dialogRefSpy: jasmine.SpyObj<MatDialogRef<CreateDebitNoteComponent>>;
  let dialogSpy: jasmine.SpyObj<MatDialog>;
  let debitNoteServiceSpy: jasmine.SpyObj<DebitNoteService>;
  let invoiceServiceSpy: jasmine.SpyObj<InvoiceService>;
  let productCacheServiceSpy: jasmine.SpyObj<ProductCacheService>;
  let productServiceSpy: jasmine.SpyObj<ProductService>;
  let paymentMethodServiceSpy: jasmine.SpyObj<PaymentMethodService>;
  let companyServiceSpy: jasmine.SpyObj<CompanyService>;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;
  let loggerSpy: jasmine.SpyObj<LoggerService>;
  let permissionServiceSpy: jasmine.SpyObj<PermissionService>;
  let cdrSpy: jasmine.SpyObj<ChangeDetectorRef>;

  const mockInvoice: InvoiceDto = {
    idInvoice: 1,
    idCompany: 1,
    invoiceNumber: '001-001-000000123',
    issueDate: '2024-01-15',
    creationDate: '2024-01-15',
    totalAmount: 1000,
    discount: 0,
    tip: 0,
    subtotalZero: 0,
    subtotalIva: 1000,
    subtotalIce: 0,
    subtotalDiscount: 0,
    establishment: {
      idEstablishment: 1,
      code: '001',
    },
    buyerName: 'Test Client',
    buyerIdentification: '1234567890',
    clientDto: {
      idClient: 1,
      identification: '1234567890',
      idType: 1,
      name: 'Test Client',
      email: 'test@example.com',
    },
    invoiceDetails: [
      {
        idInvoiceDetail: 1,
        idProduct: 1,
        code: 'PROD-001',
        description: 'Test Product',
        quantity: 10,
        unitPrice: 100,
        discount: 0,
        subtotal: 1000,
        ivaPercentage: 15,
      },
    ],
  };

  const mockProduct: ProductDto = {
    idProduct: 1,
    mainCode: 'PROD-001',
    name: 'Test Product',
    description: 'Test Product Description',
    unitPrice: 100,
    tourismIva: false,
    ice: false,
    ivaTaxType: {
      idTaxType: 1,
      type: '2',
      value: 15,
      name: 'IVA 15%',
    },
  };

  beforeEach(async () => {
    // Mock localStorage
    spyOn(localStorage, 'getItem').and.returnValue('1');

    dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['close']);
    dialogSpy = jasmine.createSpyObj('MatDialog', ['open']);
    debitNoteServiceSpy = jasmine.createSpyObj('DebitNoteService', [
      'createDraft',
      'create',
    ]);
    invoiceServiceSpy = jasmine.createSpyObj('InvoiceService', ['findById']);
    productCacheServiceSpy = jasmine.createSpyObj('ProductCacheService', [
      'getProducts',
    ]);
    productServiceSpy = jasmine.createSpyObj('ProductService', [
      'searchPageable',
    ]);
    paymentMethodServiceSpy = jasmine.createSpyObj('PaymentMethodService', [
      'getAll',
    ]);
    companyServiceSpy = jasmine.createSpyObj('CompanyService', ['findById']);
    companyServiceSpy.findById.and.returnValue(
      of({
        code: 200,
        status: 'OK',
        message: 'Success',
        companyDetailsDtos: [],
        companyDetailsDto: {
          idCompany: 1,
          idType: 1,
          identification: '1234567890',
          name: 'Test Company',
          nameCommercial: 'Test Company',
          status: true,
          forcedToAccounting: 'N',
          specialTaxpayer: 0,
          establishments: [],
        },
      })
    );
    snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    loggerSpy = jasmine.createSpyObj('LoggerService', [
      'debug',
      'error',
      'warn',
    ]);
    permissionServiceSpy = jasmine.createSpyObj('PermissionService', [
      'canAccess',
      'canEmitDocument',
    ]);
    cdrSpy = jasmine.createSpyObj('ChangeDetectorRef', [
      'markForCheck',
      'detectChanges',
    ]);

    await TestBed.configureTestingModule({
      declarations: [CreateDebitNoteComponent],
      imports: [
        ReactiveFormsModule,
        MatSnackBarModule,
        MatSelectModule,
        MatFormFieldModule,
        MatInputModule,
        MatDatepickerModule,
        MatNativeDateModule,
        MatTableModule,
        BrowserAnimationsModule,
        HttpClientTestingModule,
      ],
      providers: [
        FormBuilder,
        { provide: MatDialogRef, useValue: dialogRefSpy },
        { provide: MatDialog, useValue: dialogSpy },
        {
          provide: MAT_DIALOG_DATA,
          useValue: { invoice: mockInvoice } as CreateDebitNoteDialogData,
        },
        { provide: DebitNoteService, useValue: debitNoteServiceSpy },
        { provide: InvoiceService, useValue: invoiceServiceSpy },
        { provide: ProductCacheService, useValue: productCacheServiceSpy },
        { provide: ProductService, useValue: productServiceSpy },
        { provide: PaymentMethodService, useValue: paymentMethodServiceSpy },
        { provide: CompanyService, useValue: companyServiceSpy },
        { provide: MatSnackBar, useValue: snackBarSpy },
        { provide: LoggerService, useValue: loggerSpy },
        { provide: PermissionService, useValue: permissionServiceSpy },
        { provide: ChangeDetectorRef, useValue: cdrSpy },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();

    fixture = TestBed.createComponent(CreateDebitNoteComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('ngOnInit', () => {
    it('should initialize without loading invoice if client data exists', () => {
      component.ngOnInit();

      expect(invoiceServiceSpy.findById).not.toHaveBeenCalled();
    });

    it('should load invoice details if client data is missing', () => {
      const invoiceWithoutClient = {
        ...mockInvoice,
        buyerName: undefined,
        buyerIdentification: undefined,
      };
      invoiceWithoutClient.clientDto = {
        idClient: 0,
        identification: '',
        idType: 1,
        name: '',
        email: '',
      };
      // Actualizar data.invoice en lugar de selectedInvoice porque ngOnInit lo sobrescribe
      component.data.invoice = invoiceWithoutClient;
      invoiceServiceSpy.findById.and.returnValue(of({ data: mockInvoice }));

      component.ngOnInit();

      expect(invoiceServiceSpy.findById).toHaveBeenCalledWith(1);
    });

    it('should handle error when loading invoice details', () => {
      const invoiceWithoutClient = {
        ...mockInvoice,
        buyerName: undefined,
        buyerIdentification: undefined,
      };
      invoiceWithoutClient.clientDto = {
        idClient: 0,
        identification: '',
        idType: 1,
        name: '',
        email: '',
      };
      // Actualizar data.invoice en lugar de selectedInvoice porque ngOnInit lo sobrescribe
      component.data.invoice = invoiceWithoutClient;
      invoiceServiceSpy.findById.and.returnValue(
        throwError(() => new Error('Error'))
      );

      component.ngOnInit();

      expect(loggerSpy.error).toHaveBeenCalled();
    });
  });

  describe('initForm', () => {
    it('should initialize form with invoice data', () => {
      component['initForm']();

      expect(component.form).toBeDefined();
      expect(component.form.get('issueDate')).toBeDefined();
      expect(component.form.get('observation')).toBeDefined();
      expect(component.form.get('originalInvoiceId')?.value).toBe(1);
      expect(component.form.get('originalInvoiceNumber')?.value).toBe(
        '001-001-000000123'
      );
    });
  });

  describe('initDateLimits', () => {
    it('should set min and max dates correctly', () => {
      component['initDateLimits']();

      expect(component.minDate).toBeDefined();
      expect(component.maxDate).toBeDefined();
    });
  });

  describe('loadInvoiceDetails', () => {
    it('should load invoice details successfully', () => {
      invoiceServiceSpy.findById.and.returnValue(of({ data: mockInvoice }));

      component['loadInvoiceDetails'](1);

      expect(invoiceServiceSpy.findById).toHaveBeenCalledWith(1);
      expect(component.isLoadingCompany).toBe(false);
    });

    it('should handle error when loading invoice details', () => {
      invoiceServiceSpy.findById.and.returnValue(
        throwError(() => new Error('Error'))
      );

      component['loadInvoiceDetails'](1);

      expect(loggerSpy.error).toHaveBeenCalled();
      expect(component.isLoadingCompany).toBe(false);
    });
  });

  describe('addProducts', () => {
    it('should add products to data source', () => {
      const products = [mockProduct];
      component.productsDataSource.data = [];

      component['addProducts'](products);

      expect(component.productsDataSource.data.length).toBe(1);
    });

    it('should not add duplicate products', () => {
      const products = [mockProduct];
      component.productsDataSource.data = [{ idProduct: 1 }];

      component['addProducts'](products);

      expect(component.productsDataSource.data.length).toBe(1);
    });
  });

  describe('removeProduct', () => {
    it('should remove product at given index', () => {
      component.productsDataSource.data = [
        { idProduct: 1 },
        { idProduct: 2 },
        { idProduct: 3 },
      ];

      component.removeProduct(1);

      expect(component.productsDataSource.data.length).toBe(2);
      expect(component.productsDataSource.data[1].idProduct).toBe(3);
    });
  });

  describe('updateProduct', () => {
    it('should update product field', () => {
      component.productsDataSource.data = [{ idProduct: 1, quantity: 10 }];

      component.updateProduct(0, 'quantity', 20);

      expect(component.productsDataSource.data[0].quantity).toBe(20);
    });

    it('should recalculate subtotal when quantity changes', () => {
      spyOn(component, 'calculateTotals');
      component.productsDataSource.data = [
        { idProduct: 1, quantity: 10, unitPrice: 100 },
      ];

      component.updateProduct(0, 'quantity', 20);

      expect(component.calculateTotals).toHaveBeenCalled();
    });
  });

  describe('addPayment', () => {
    it('should add payment to payments array', () => {
      const paymentData = {
        paymentType: '01',
        value: 100,
        term: 30,
        time: 'días',
      };

      component['addPayment'](paymentData);

      expect(component.paymentsDataSource.length).toBe(1);
      expect(component.paymentsDataSource[0].value).toBe(100);
    });
  });

  describe('removePayment', () => {
    it('should remove payment at given index', () => {
      component.paymentsDataSource = [
        { paymentType: '01', value: 100 },
        { paymentType: '02', value: 200 },
      ];

      component.removePayment(0);

      expect(component.paymentsDataSource.length).toBe(1);
      expect(component.paymentsDataSource[0].value).toBe(200);
    });
  });

  describe('updatePaymentValue', () => {
    it('should update payment value', () => {
      component.paymentsDataSource = [{ paymentType: '01', value: 100 }];

      component.updatePaymentValue(0, 200);

      expect(component.paymentsDataSource[0].value).toBe(200);
    });
  });

  describe('calculateTotals', () => {
    it('should calculate totals correctly', () => {
      component.productsDataSource.data = [
        {
          idProduct: 1,
          quantity: 10,
          unitPrice: 100,
          subtotal: 1000,
          discount: 0,
          ivaPercentage: 15,
          icePercentage: 0,
        },
      ];

      component.calculateTotals();

      expect(component.valorModificacion).toBeGreaterThan(0);
      expect(component.totalIva).toBeGreaterThanOrEqual(0);
    });

    it('should reset totals when no products', () => {
      component.productsDataSource.data = [];

      component.calculateTotals();

      expect(component.valorModificacion).toBe(0);
      expect(component.totalIva).toBe(0);
    });
  });

  describe('getTotalPayments', () => {
    it('should return sum of all payment values', () => {
      component.paymentsDataSource = [
        { paymentType: '01', value: 100 },
        { paymentType: '02', value: 200 },
      ];

      const total = component.getTotalPayments();

      expect(total).toBe(300);
    });

    it('should return 0 when no payments', () => {
      component.paymentsDataSource = [];

      const total = component.getTotalPayments();

      expect(total).toBe(0);
    });
  });

  describe('paymentsMatchTotal', () => {
    it('should return true when payments match total', () => {
      component.valorTotal = 300;
      component.paymentsDataSource = [{ paymentType: '01', value: 300 }];

      const result = component.paymentsMatchTotal();

      expect(result).toBe(true);
    });

    it('should return false when payments do not match total', () => {
      component.valorTotal = 300;
      component.paymentsDataSource = [{ paymentType: '01', value: 200 }];

      const result = component.paymentsMatchTotal();

      expect(result).toBe(false);
    });
  });

  describe('saveDraft', () => {
    it('should save draft successfully', () => {
      component.form.patchValue({ issueDate: '2024-01-20' });
      component.productsDataSource.data = [
        { idProduct: 1, quantity: 10, unitPrice: 100, subtotal: 1000 },
      ];
      debitNoteServiceSpy.createDraft.and.returnValue(
        of({
          code: 200,
          status: 'OK',
          message: 'Borrador guardado',
          idDebitNote: 1,
        })
      );

      component.saveDraft();

      expect(debitNoteServiceSpy.createDraft).toHaveBeenCalled();
      expect(snackBarSpy.open).toHaveBeenCalled();
    });

    it('should handle error when saving draft', () => {
      component.form.patchValue({ issueDate: '2024-01-20' });
      component.productsDataSource.data = [
        { idProduct: 1, quantity: 10, unitPrice: 100, subtotal: 1000 },
      ];
      debitNoteServiceSpy.createDraft.and.returnValue(
        throwError(() => ({ error: { message: 'Error' } }))
      );

      component.saveDraft();

      expect(loggerSpy.error).toHaveBeenCalled();
      expect(snackBarSpy.open).toHaveBeenCalled();
    });
  });

  describe('createAndSend', () => {
    it('should create and send debit note successfully', () => {
      component.form.patchValue({ issueDate: '2024-01-20' });
      component.productsDataSource.data = [
        { idProduct: 1, quantity: 10, unitPrice: 100, subtotal: 1000 },
      ];
      component.paymentsDataSource = [{ paymentType: '01', value: 1000 }];
      component.calculateTotals(); // Calculate totals
      permissionServiceSpy.canEmitDocument.and.returnValue(of(true));
      debitNoteServiceSpy.create.and.returnValue(
        of({
          code: 200,
          status: 'OK',
          message: 'Nota de débito creada',
          idDebitNote: 1,
        })
      );

      component.createAndSend();

      expect(permissionServiceSpy.canEmitDocument).toHaveBeenCalledWith(
        'DEBIT_NOTE'
      );
    });

    it('should handle error when creating debit note', () => {
      component.form.patchValue({ issueDate: '2024-01-20' });
      component.productsDataSource.data = [
        { idProduct: 1, quantity: 10, unitPrice: 100, subtotal: 1000 },
      ];
      component.paymentsDataSource = [{ paymentType: '01', value: 1000 }];
      component.calculateTotals(); // Calculate totals
      permissionServiceSpy.canEmitDocument.and.returnValue(of(true));
      debitNoteServiceSpy.create.and.returnValue(
        throwError(() => ({ error: { message: 'Error' } }))
      );

      component.createAndSend();

      expect(permissionServiceSpy.canEmitDocument).toHaveBeenCalled();
    });
  });

  describe('cancel', () => {
    it('should close dialog when cancelled', () => {
      component.cancel();

      expect(dialogRefSpy.close).toHaveBeenCalledWith(null);
    });
  });

  describe('formatCurrency', () => {
    it('should format currency correctly', () => {
      const result = component.formatCurrency(1000.5);

      // Intl.NumberFormat con 'es-EC' usa punto para miles y coma para decimales
      expect(result).toMatch(/\$.*1[.,]000/);
    });

    it('should handle zero value', () => {
      const result = component.formatCurrency(0);

      expect(result).toContain('0');
    });
  });

  describe('getClientName', () => {
    it('should return buyer name when available', () => {
      component.selectedInvoice = { ...mockInvoice, buyerName: 'Test Buyer' };

      const result = component.getClientName();

      expect(result).toBe('Test Buyer');
    });

    it('should return clientDto name when buyer name is not available', () => {
      component.selectedInvoice = {
        ...mockInvoice,
        buyerName: undefined,
        clientDto: {
          idClient: 1,
          identification: '1234567890',
          idType: 1,
          name: 'Client DTO Name',
          email: 'test@example.com',
        },
      };

      const result = component.getClientName();

      expect(result).toBe('Client DTO Name');
    });
  });

  describe('getClientIdentification', () => {
    it('should return buyer identification when available', () => {
      component.selectedInvoice = {
        ...mockInvoice,
        buyerIdentification: '1234567890',
      };

      const result = component.getClientIdentification();

      expect(result).toBe('1234567890');
    });

    it('should return clientDto identification when buyer identification is not available', () => {
      component.selectedInvoice = {
        ...mockInvoice,
        buyerIdentification: undefined,
        clientDto: {
          idClient: 1,
          identification: '9876543210',
          idType: 1,
          name: 'Test Client',
          email: 'test@example.com',
        },
      };

      const result = component.getClientIdentification();

      expect(result).toBe('9876543210');
    });
  });

  describe('getOriginalInvoiceNumber', () => {
    it('should return invoice number', () => {
      const result = component.getOriginalInvoiceNumber();

      expect(result).toBe('001-001-000000123');
    });
  });

  describe('hasClientData', () => {
    it('should return true when invoice has client data', () => {
      const result = component['hasClientData'](mockInvoice);

      expect(result).toBe(true);
    });

    it('should return false when invoice has no client data', () => {
      const invoiceWithoutClient = { idInvoice: 1 };
      const result = component['hasClientData'](
        invoiceWithoutClient as InvoiceDto
      );

      expect(result).toBe(false);
    });
  });

  describe('ngOnDestroy', () => {
    it('should complete destroy subject', () => {
      spyOn(component['destroy$'], 'next');
      spyOn(component['destroy$'], 'complete');

      component.ngOnDestroy();

      expect(component['destroy$'].next).toHaveBeenCalled();
      expect(component['destroy$'].complete).toHaveBeenCalled();
    });
  });
});
