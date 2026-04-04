import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router, ActivatedRoute } from '@angular/router';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { CommonModule } from '@angular/common';
import { BillingComponent } from './billing.component';
import { ProductService } from '../../_service/product/product.service';
import { ProductCacheService } from '../../_service/product/product-cache.service';
import { InvoiceService } from '../../_service/invoice/invoice.service';
import { DraftInvoiceService } from '../../_service/invoice/draft-invoice.service';
import { ClientService } from '../../_service/customer/client.service';
import { CompanyService } from '../../_service/customer/company.service';
import { InvoicePointService } from '../../_service/auth/invoice-point.service';
import { PaymentMethodService } from '../../_service/invoice/payment-method.service';
import { LoggerService } from '../../_service/logger/logger.service';
import { of, throwError } from 'rxjs';
import { JwtHelperService } from '@auth0/angular-jwt';

describe('BillingComponent', () => {
  let component: BillingComponent;
  let fixture: ComponentFixture<BillingComponent>;
  let productServiceSpy: jasmine.SpyObj<ProductService>;
  let invoiceServiceSpy: jasmine.SpyObj<InvoiceService>;
  let clientServiceSpy: jasmine.SpyObj<ClientService>;
  let dialogSpy: jasmine.SpyObj<MatDialog>;
  let routerSpy: jasmine.SpyObj<Router>;
  let loggerSpy: jasmine.SpyObj<LoggerService>;

  const createMockToken = (payload: any): string => {
    const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
    const body = btoa(JSON.stringify(payload));
    return `${header}.${body}.signature`;
  };

  beforeEach(async () => {
    const productService = jasmine.createSpyObj('ProductService', [
      'searchActiveProductsForBilling',
    ]);
    const productCacheService = jasmine.createSpyObj('ProductCacheService', [
      'getCachedProducts',
    ]);
    const invoiceService = jasmine.createSpyObj('InvoiceService', [
      'create',
      'findById',
    ]);
    const draftInvoiceService = jasmine.createSpyObj('DraftInvoiceService', [
      'getDraftById',
      'saveDraft',
      'processDraft',
    ]);
    const clientService = jasmine.createSpyObj('ClientService', [
      'findByIdentificationOrName',
    ]);
    const companyService = jasmine.createSpyObj('CompanyService', [
      'findById',
    ]);
    const invoicePointService = jasmine.createSpyObj('InvoicePointService', [
      'findByCompany',
    ]);
    const paymentMethodService = jasmine.createSpyObj('PaymentMethodService', [
      'findAll',
    ]);
    const logger = jasmine.createSpyObj('LoggerService', [
      'debug',
      'info',
      'warn',
      'error',
    ]);
    const dialog = jasmine.createSpyObj('MatDialog', ['open']);
    const router = jasmine.createSpyObj('Router', ['navigate']);
    const snackBar = jasmine.createSpyObj('MatSnackBar', ['open']);
    const activatedRoute = {
      snapshot: { params: {} },
      queryParams: of({}),
    };

    await TestBed.configureTestingModule({
      declarations: [BillingComponent],
      imports: [
        CommonModule,
        ReactiveFormsModule,
        HttpClientTestingModule,
        BrowserAnimationsModule,
      ],
      providers: [
        FormBuilder,
        { provide: ProductService, useValue: productService },
        { provide: ProductCacheService, useValue: productCacheService },
        { provide: InvoiceService, useValue: invoiceService },
        { provide: DraftInvoiceService, useValue: draftInvoiceService },
        { provide: ClientService, useValue: clientService },
        { provide: CompanyService, useValue: companyService },
        { provide: InvoicePointService, useValue: invoicePointService },
        { provide: PaymentMethodService, useValue: paymentMethodService },
        { provide: LoggerService, useValue: logger },
        { provide: MatDialog, useValue: dialog },
        { provide: Router, useValue: router },
        { provide: MatSnackBar, useValue: snackBar },
        { provide: ActivatedRoute, useValue: activatedRoute },
        { provide: JwtHelperService, useValue: {} },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(BillingComponent);
    component = fixture.componentInstance;
    productServiceSpy = TestBed.inject(
      ProductService
    ) as jasmine.SpyObj<ProductService>;
    invoiceServiceSpy = TestBed.inject(
      InvoiceService
    ) as jasmine.SpyObj<InvoiceService>;
    clientServiceSpy = TestBed.inject(
      ClientService
    ) as jasmine.SpyObj<ClientService>;
    dialogSpy = TestBed.inject(MatDialog) as jasmine.SpyObj<MatDialog>;
    routerSpy = TestBed.inject(Router) as jasmine.SpyObj<Router>;
    loggerSpy = TestBed.inject(LoggerService) as jasmine.SpyObj<LoggerService>;

    // Mock dialog.afterClosed
    dialogSpy.open.and.returnValue({
      afterClosed: () => of(false),
    } as any);
  });

  beforeEach(() => {
    const token = createMockToken({
      userName: 'testuser',
      roles: ['admin'],
      idCompany: 1,
    });
    localStorage.setItem('token', token);
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize forms', () => {
    expect(component.invoiceForm).toBeDefined();
    expect(component.buyerForm).toBeDefined();
    expect(component.detailsForm).toBeDefined();
    expect(component.paymentsForm).toBeDefined();
  });

  it('should have Math exposed', () => {
    expect(component.Math).toBe(Math);
  });

  it('should have correct displayed columns for invoice details', () => {
    expect(component.displayedColumnsPurchaseProofDetail).toEqual([
      'code',
      'quantity',
      'description',
      'unitPrice',
      'discount',
      'totalValue',
      'iva',
      'ice',
    ]);
  });

  it('should have correct displayed columns for payments', () => {
    expect(component.displayedPaymentColumns).toEqual([
      'paymentType',
      'value',
      'term',
      'time',
      'actions',
    ]);
  });

  it('should initialize totals array', () => {
    expect(component.totals.length).toBe(5);
    expect(component.totals[0].label).toBe('Subtotal sin impuestos');
    expect(component.totals[1].label).toBe('Total descuento');
    expect(component.totals[2].label).toBe('Subtotal con descuento');
    expect(component.totals[3].label).toBe('IVA');
    expect(component.totals[4].label).toBe('Valor ICE');
  });

  it('should have maxDate set to current date', () => {
    const today = new Date();
    const componentDate = component.maxDate;
    expect(componentDate.getDate()).toBe(today.getDate());
    expect(componentDate.getMonth()).toBe(today.getMonth());
    expect(componentDate.getFullYear()).toBe(today.getFullYear());
  });

  it('should have globalIvaPercentage initialized', () => {
    expect(component.globalIvaPercentage.value).toBe(15);
    expect(component.globalIvaPercentage.valid).toBe(true);
  });
});

