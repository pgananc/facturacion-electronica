import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { NO_ERRORS_SCHEMA, ChangeDetectorRef } from '@angular/core';
import { of, throwError } from 'rxjs';
import {
  CreateCreditNoteComponent,
  CreateCreditNoteDialogData,
} from './create-credit-note.component';
import { CreditNoteService } from '../../../../_service/invoice/credit-note.service';
import { InvoiceService } from '../../../../_service/invoice/invoice.service';
import { LoggerService } from '../../../../_service/logger/logger.service';
import { PermissionService } from '../../../../_service/permission/permission.service';
import {
  CreditNoteReasonDto,
  InvoiceAvailableForCreditNoteDto,
  InvoiceDetailAvailableDto,
} from '../../../../_model/invoice/credit-note-dto';
import { InvoiceDto } from '../../../../_model/invoice/invoice-dto';

describe('CreateCreditNoteComponent', () => {
  let component: CreateCreditNoteComponent;
  let fixture: ComponentFixture<CreateCreditNoteComponent>;
  let dialogRefSpy: jasmine.SpyObj<MatDialogRef<CreateCreditNoteComponent>>;
  let creditNoteServiceSpy: jasmine.SpyObj<CreditNoteService>;
  let invoiceServiceSpy: jasmine.SpyObj<InvoiceService>;
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

  const mockReasons: CreditNoteReasonDto[] = [
    { idReason: 1, code: '01', description: 'Devolución de productos' },
    { idReason: 2, code: '02', description: 'Descuento aplicado' },
  ];

  const mockInvoiceAvailable: InvoiceAvailableForCreditNoteDto = {
    idInvoice: 1,
    invoiceNumber: '001-001-000000123',
    issueDate: '2024-01-15',
    accessKey: 'test-access-key',
    authorizationNumber: '1234567890',
    client: {
      idClient: 1,
      identification: '1234567890',
      name: 'Test Client',
      email: 'test@example.com',
    },
    totalInvoice: 1000,
    totalCreditNotesApplied: 0,
    availableAmount: 1000,
    details: [
      {
        idInvoiceDetail: 1,
        idProduct: 1,
        mainCode: 'PROD-001',
        description: 'Test Product',
        quantityInvoiced: 10,
        quantityReturned: 0,
        quantityAvailable: 10,
        unitPrice: 100,
        discount: 0,
        subtotal: 1000,
        ivaPercentage: 15,
      },
    ],
  };

  beforeEach(async () => {
    // Mock localStorage
    spyOn(localStorage, 'getItem').and.callFake((key: string) => {
      if (key === 'INVOICE_POINT_DATA') {
        return JSON.stringify({ id: 1 });
      }
      return '1';
    });

    dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['close']);
    creditNoteServiceSpy = jasmine.createSpyObj('CreditNoteService', [
      'getReasons',
      'getInvoiceAvailableForCreditNote',
      'saveDraft',
      'createAndSend',
    ]);
    invoiceServiceSpy = jasmine.createSpyObj('InvoiceService', ['findById']);
    snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    loggerSpy = jasmine.createSpyObj('LoggerService', ['debug', 'error']);
    permissionServiceSpy = jasmine.createSpyObj('PermissionService', [
      'canAccess',
      'canEmitDocument',
    ]);
    cdrSpy = jasmine.createSpyObj('ChangeDetectorRef', [
      'markForCheck',
      'detectChanges',
    ]);

    await TestBed.configureTestingModule({
      declarations: [CreateCreditNoteComponent],
      imports: [
        ReactiveFormsModule,
        MatSnackBarModule,
        MatSelectModule,
        MatFormFieldModule,
        MatInputModule,
        MatDatepickerModule,
        MatNativeDateModule,
        BrowserAnimationsModule,
        HttpClientTestingModule,
      ],
      providers: [
        FormBuilder,
        { provide: MatDialogRef, useValue: dialogRefSpy },
        {
          provide: MAT_DIALOG_DATA,
          useValue: { invoice: mockInvoice } as CreateCreditNoteDialogData,
        },
        { provide: CreditNoteService, useValue: creditNoteServiceSpy },
        { provide: InvoiceService, useValue: invoiceServiceSpy },
        { provide: MatSnackBar, useValue: snackBarSpy },
        { provide: LoggerService, useValue: loggerSpy },
        { provide: PermissionService, useValue: permissionServiceSpy },
        { provide: ChangeDetectorRef, useValue: cdrSpy },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();

    fixture = TestBed.createComponent(CreateCreditNoteComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('ngOnInit', () => {
    it('should load reasons and invoice details', () => {
      creditNoteServiceSpy.getReasons.and.returnValue(of(mockReasons));
      creditNoteServiceSpy.getInvoiceAvailableForCreditNote.and.returnValue(
        of(mockInvoiceAvailable)
      );

      component.ngOnInit();

      expect(creditNoteServiceSpy.getReasons).toHaveBeenCalled();
      expect(
        creditNoteServiceSpy.getInvoiceAvailableForCreditNote
      ).toHaveBeenCalledWith(1);
    });

    it('should handle error when loading reasons', () => {
      creditNoteServiceSpy.getReasons.and.returnValue(
        throwError(() => new Error('Error'))
      );
      creditNoteServiceSpy.getInvoiceAvailableForCreditNote.and.returnValue(
        of(mockInvoiceAvailable)
      );

      component.ngOnInit();

      expect(loggerSpy.error).toHaveBeenCalled();
      expect(snackBarSpy.open).toHaveBeenCalled();
    });
  });

  describe('loadReasons', () => {
    it('should load reasons successfully', () => {
      creditNoteServiceSpy.getReasons.and.returnValue(of(mockReasons));

      component['loadReasons']();

      expect(component.isLoadingReasons).toBe(false);
      expect(component.reasons).toEqual(mockReasons);
    });

    it('should handle error when loading reasons', () => {
      creditNoteServiceSpy.getReasons.and.returnValue(
        throwError(() => new Error('Error'))
      );

      component['loadReasons']();

      expect(component.isLoadingReasons).toBe(false);
      expect(loggerSpy.error).toHaveBeenCalled();
      expect(snackBarSpy.open).toHaveBeenCalled();
    });
  });

  describe('loadInvoiceDetails', () => {
    it('should load invoice details when invoice has details', () => {
      creditNoteServiceSpy.getInvoiceAvailableForCreditNote.and.returnValue(
        of(mockInvoiceAvailable)
      );

      component['loadInvoiceDetails']();

      expect(
        creditNoteServiceSpy.getInvoiceAvailableForCreditNote
      ).toHaveBeenCalledWith(1);
    });

    it('should load invoice available data when invoice has no details', () => {
      const invoiceWithoutDetails = { ...mockInvoice, invoiceDetails: [] };
      component.selectedInvoice = invoiceWithoutDetails;
      creditNoteServiceSpy.getInvoiceAvailableForCreditNote.and.returnValue(
        of(mockInvoiceAvailable)
      );

      component['loadInvoiceDetails']();

      // El componente usa getInvoiceAvailableForCreditNote, no findById
      expect(
        creditNoteServiceSpy.getInvoiceAvailableForCreditNote
      ).toHaveBeenCalledWith(1);
      // Si no hay detalles en la respuesta, usará buildDetailsFromInvoice con los detalles vacíos
    });

    it('should handle error when invoice ID is missing', () => {
      component.selectedInvoice = { ...mockInvoice, idInvoice: undefined };

      component['loadInvoiceDetails']();

      expect(snackBarSpy.open).toHaveBeenCalled();
    });
  });

  describe('initForm', () => {
    it('should initialize form with default values', () => {
      component['initForm']();

      expect(component.form).toBeDefined();
      expect(component.form.get('idReason')).toBeDefined();
      expect(component.form.get('observation')).toBeDefined();
      expect(component.form.get('issueDate')).toBeDefined();
      expect(component.form.get('details')).toBeDefined();
    });
  });

  describe('initDateLimits', () => {
    it('should set min and max dates correctly', () => {
      component['initDateLimits']();

      expect(component.minDate).toBeDefined();
      expect(component.maxDate).toBeDefined();
      expect(component.minDateDisplay).toBeDefined();
      expect(component.maxDateDisplay).toBeDefined();
    });
  });

  describe('nextStep', () => {
    it('should advance to next step when current step is less than 3', () => {
      component.currentStep = 1;

      component.nextStep();

      expect(component.currentStep).toBe(2);
    });

    it('should not advance beyond step 3', () => {
      component.currentStep = 3;

      component.nextStep();

      expect(component.currentStep).toBe(3);
    });
  });

  describe('previousStep', () => {
    it('should go back to previous step when current step is greater than 1', () => {
      component.currentStep = 2;

      component.previousStep();

      expect(component.currentStep).toBe(1);
    });

    it('should not go below step 1', () => {
      component.currentStep = 1;

      component.previousStep();

      expect(component.currentStep).toBe(1);
    });
  });

  describe('canProceed', () => {
    it('should return true for step 1 when reason is valid', () => {
      component.currentStep = 1;
      component.form.patchValue({ idReason: 1 });

      const result = component.canProceed();

      expect(result).toBe(true);
    });

    it('should return false for step 1 when reason is invalid', () => {
      component.currentStep = 1;
      component.form.patchValue({ idReason: null });

      const result = component.canProceed();

      expect(result).toBe(false);
    });

    it('should return true for step 3', () => {
      component.currentStep = 3;

      const result = component.canProceed();

      expect(result).toBe(true);
    });
  });

  describe('calculateTotals', () => {
    it('should calculate totals correctly', () => {
      component.invoiceData = mockInvoiceAvailable;
      component['buildDetailsFormArray'](mockInvoiceAvailable.details);
      component.detailsArray.controls[0].patchValue({
        selected: true,
        quantityReturned: 5,
      });

      component.calculateTotals();

      expect(component.totalAmount).toBeGreaterThan(0);
    });

    it('should reset totals when no items selected', () => {
      component.invoiceData = mockInvoiceAvailable;
      component['buildDetailsFormArray'](mockInvoiceAvailable.details);
      component.detailsArray.controls[0].patchValue({ selected: false });

      component.calculateTotals();

      expect(component.totalAmount).toBe(0);
    });
  });

  describe('toggleSelectAll', () => {
    it('should select all items when selectAll is true', () => {
      component.invoiceData = mockInvoiceAvailable;
      component['buildDetailsFormArray'](mockInvoiceAvailable.details);

      component.toggleSelectAll(true);

      component.detailsArray.controls.forEach((control) => {
        expect(control.get('selected')?.value).toBe(true);
      });
    });

    it('should deselect all items when selectAll is false', () => {
      component.invoiceData = mockInvoiceAvailable;
      component['buildDetailsFormArray'](mockInvoiceAvailable.details);
      component.toggleSelectAll(true); // First select all

      component.toggleSelectAll(false);

      component.detailsArray.controls.forEach((control) => {
        expect(control.get('selected')?.value).toBe(false);
      });
    });
  });

  describe('saveDraft', () => {
    it('should save draft successfully', () => {
      component.selectedInvoice = mockInvoice;
      component.form.patchValue({ idReason: 1, issueDate: '2024-01-20' });
      component.invoiceData = mockInvoiceAvailable;
      component['buildDetailsFormArray'](mockInvoiceAvailable.details);
      component.detailsArray.controls[0].patchValue({
        selected: true,
        quantity: 5,
      });
      component.calculateTotals(); // Calculate totals to ensure totalAmount > 0
      creditNoteServiceSpy.saveDraft.and.returnValue(
        of({
          code: 200,
          status: 'OK',
          message: 'Borrador guardado',
          idCreditNote: 1,
        })
      );

      component.saveDraft();

      expect(creditNoteServiceSpy.saveDraft).toHaveBeenCalled();
      expect(snackBarSpy.open).toHaveBeenCalled();
    });

    it('should handle error when saving draft', () => {
      component.selectedInvoice = mockInvoice;
      component.form.patchValue({ idReason: 1, issueDate: '2024-01-20' });
      component.invoiceData = mockInvoiceAvailable;
      component['buildDetailsFormArray'](mockInvoiceAvailable.details);
      component.detailsArray.controls[0].patchValue({
        selected: true,
        quantity: 5,
      });
      component.calculateTotals(); // Calculate totals to ensure totalAmount > 0
      creditNoteServiceSpy.saveDraft.and.returnValue(
        throwError(() => ({ error: { message: 'Error' } }))
      );

      component.saveDraft();

      expect(loggerSpy.error).toHaveBeenCalled();
      expect(snackBarSpy.open).toHaveBeenCalled();
    });
  });

  describe('submit', () => {
    it('should submit credit note successfully', () => {
      component.selectedInvoice = mockInvoice;
      component.form.patchValue({ idReason: 1, issueDate: '2024-01-20' });
      component.invoiceData = mockInvoiceAvailable;
      component['buildDetailsFormArray'](mockInvoiceAvailable.details);
      component.detailsArray.controls[0].patchValue({
        selected: true,
        quantity: 5,
      });
      component.calculateTotals(); // Calculate totals to ensure totalAmount > 0
      permissionServiceSpy.canEmitDocument.and.returnValue(of(true));
      creditNoteServiceSpy.createAndSend.and.returnValue(
        of({
          code: 200,
          status: 'OK',
          message: 'Nota de crédito creada',
          idCreditNote: 1,
        })
      );

      component.submit();

      expect(permissionServiceSpy.canEmitDocument).toHaveBeenCalledWith(
        'CREDIT_NOTE'
      );
    });

    it('should handle error when submitting', () => {
      component.selectedInvoice = mockInvoice;
      component.form.patchValue({ idReason: 1, issueDate: '2024-01-20' });
      component.invoiceData = mockInvoiceAvailable;
      component['buildDetailsFormArray'](mockInvoiceAvailable.details);
      component.detailsArray.controls[0].patchValue({
        selected: true,
        quantity: 5,
      });
      component.calculateTotals(); // Calculate totals to ensure totalAmount > 0
      permissionServiceSpy.canEmitDocument.and.returnValue(of(true));
      creditNoteServiceSpy.createAndSend.and.returnValue(
        throwError(() => ({ error: { message: 'Error' } }))
      );

      component.submit();

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

      expect(result).toBe('$1000.50');
    });

    it('should handle zero value', () => {
      const result = component.formatCurrency(0);

      expect(result).toBe('$0.00');
    });
  });

  describe('getSelectedReasonName', () => {
    it('should return reason name when reason is selected', () => {
      component.reasons = mockReasons;
      component.form.patchValue({ idReason: 1 });

      const result = component.getSelectedReasonName();

      expect(result).toBe('Devolución de productos');
    });

    it('should return "-" when no reason is selected', () => {
      component.reasons = mockReasons;
      component.form.patchValue({ idReason: null });

      const result = component.getSelectedReasonName();

      expect(result).toBe('-');
    });
  });

  describe('allSelected', () => {
    it('should return true when all items are selected', () => {
      component.invoiceData = mockInvoiceAvailable;
      component['buildDetailsFormArray'](mockInvoiceAvailable.details);
      component.toggleSelectAll(true);

      const result = component.allSelected;

      expect(result).toBe(true);
    });

    it('should return false when not all items are selected', () => {
      component.invoiceData = mockInvoiceAvailable;
      component['buildDetailsFormArray'](mockInvoiceAvailable.details);

      const result = component.allSelected;

      expect(result).toBe(false);
    });
  });

  describe('hasSelectedItems', () => {
    it('should return true when at least one item is selected', () => {
      component.invoiceData = mockInvoiceAvailable;
      component['buildDetailsFormArray'](mockInvoiceAvailable.details);
      component.detailsArray.controls[0].patchValue({ selected: true });

      const result = component.hasSelectedItems;

      expect(result).toBe(true);
    });

    it('should return false when no items are selected', () => {
      component.invoiceData = mockInvoiceAvailable;
      component['buildDetailsFormArray'](mockInvoiceAvailable.details);

      const result = component.hasSelectedItems;

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
