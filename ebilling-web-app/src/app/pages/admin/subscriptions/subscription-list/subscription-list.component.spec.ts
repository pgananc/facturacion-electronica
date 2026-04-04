import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatSortModule } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { SubscriptionListComponent } from './subscription-list.component';
import { SubscriptionAdminService } from '../../../../_service/subscription/subscription-admin.service';
import { ToastService } from '../../../../_service/notification/toast.service';
import { LoggerService } from '../../../../_service/logger/logger.service';
import { of, throwError } from 'rxjs';
import { SubscriptionAdmin } from '../../../../_model/subscription/subscription-admin';

describe('SubscriptionListComponent', () => {
  let component: SubscriptionListComponent;
  let fixture: ComponentFixture<SubscriptionListComponent>;
  let subscriptionServiceSpy: jasmine.SpyObj<SubscriptionAdminService>;
  let dialogSpy: jasmine.SpyObj<MatDialog>;
  let routerSpy: jasmine.SpyObj<Router>;
  let toastSpy: jasmine.SpyObj<ToastService>;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;
  let loggerSpy: jasmine.SpyObj<LoggerService>;

  const mockSubscriptions: SubscriptionAdmin[] = [
    {
      idCompany: 1,
      companyName: 'Test Company',
      businessName: 'Test Company',
      ruc: '1234567890',
      plan: { code: 'BASIC', name: 'Básico', documentLimitYearly: 100 },
      subscriptionStatus: 'ACTIVE',
      startDate: '2024-01-01',
      currentYearUsage: 50,
      percentUsed: 50,
      remainingDocuments: 50,
      expiresAt: '2024-12-31',
    },
  ];

  beforeEach(async () => {
    const subscriptionService = jasmine.createSpyObj(
      'SubscriptionAdminService',
      ['getAllSubscriptions']
    );
    const dialog = jasmine.createSpyObj('MatDialog', ['open']);
    const router = jasmine.createSpyObj('Router', ['navigate']);
    const toast = jasmine.createSpyObj('ToastService', ['error', 'success']);
    const snackBar = jasmine.createSpyObj('MatSnackBar', ['open']);
    const logger = jasmine.createSpyObj('LoggerService', [
      'debug',
      'info',
      'warn',
      'error',
    ]);

    await TestBed.configureTestingModule({
      declarations: [SubscriptionListComponent],
      imports: [
        CommonModule,
        HttpClientTestingModule,
        BrowserAnimationsModule,
        MatTableModule,
        MatPaginatorModule,
        MatSortModule,
        MatButtonModule,
        MatIconModule,
        MatFormFieldModule,
        MatInputModule,
        MatProgressSpinnerModule,
      ],
      providers: [
        { provide: SubscriptionAdminService, useValue: subscriptionService },
        { provide: MatDialog, useValue: dialog },
        { provide: Router, useValue: router },
        { provide: ToastService, useValue: toast },
        { provide: MatSnackBar, useValue: snackBar },
        { provide: LoggerService, useValue: logger },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SubscriptionListComponent);
    component = fixture.componentInstance;
    subscriptionServiceSpy = TestBed.inject(
      SubscriptionAdminService
    ) as jasmine.SpyObj<SubscriptionAdminService>;
    dialogSpy = TestBed.inject(MatDialog) as jasmine.SpyObj<MatDialog>;
    routerSpy = TestBed.inject(Router) as jasmine.SpyObj<Router>;
    toastSpy = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
    snackBarSpy = TestBed.inject(MatSnackBar) as jasmine.SpyObj<MatSnackBar>;
    loggerSpy = TestBed.inject(LoggerService) as jasmine.SpyObj<LoggerService>;

    // Mock dialog.afterClosed
    dialogSpy.open.and.returnValue({
      afterClosed: () => of(false),
    } as any);
  });

  beforeEach(() => {
    localStorage.setItem('token', 'test-token');
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load subscriptions on init', () => {
    subscriptionServiceSpy.getAllSubscriptions.and.returnValue(
      of(mockSubscriptions)
    );

    fixture.detectChanges();

    expect(subscriptionServiceSpy.getAllSubscriptions).toHaveBeenCalled();
    expect(component.dataSource.data).toEqual(mockSubscriptions);
    expect(component.isLoading).toBe(false);
  });

  it('should not load subscriptions when no token exists', () => {
    localStorage.removeItem('token');

    fixture.detectChanges();

    expect(subscriptionServiceSpy.getAllSubscriptions).not.toHaveBeenCalled();
    expect(component.isLoading).toBe(false);
  });

  it('should handle error when loading subscriptions', () => {
    subscriptionServiceSpy.getAllSubscriptions.and.returnValue(
      throwError(() => new Error('Network error'))
    );

    fixture.detectChanges();

    expect(toastSpy.error).toHaveBeenCalledWith(
      'Error al cargar las suscripciones'
    );
    expect(component.isLoading).toBe(false);
  });

  it('should open assign dialog', () => {
    subscriptionServiceSpy.getAllSubscriptions.and.returnValue(
      of(mockSubscriptions)
    );
    dialogSpy.open.and.returnValue({
      afterClosed: () => of(true),
    } as any);

    fixture.detectChanges();
    component.openAssignDialog();

    expect(dialogSpy.open).toHaveBeenCalled();
    expect(subscriptionServiceSpy.getAllSubscriptions).toHaveBeenCalledTimes(2);
  });

  it('should apply filter', () => {
    subscriptionServiceSpy.getAllSubscriptions.and.returnValue(
      of(mockSubscriptions)
    );

    fixture.detectChanges();

    const event = {
      target: { value: 'Test' },
    } as any;

    component.applyFilter(event);

    expect(component.dataSource.filter).toBe('test');
  });

  it('should get status class correctly', () => {
    expect(component.getStatusClass('ACTIVE')).toBe('status-active');
    expect(component.getStatusClass('TRIAL')).toBe('status-trial');
    expect(component.getStatusClass('EXPIRED')).toBe('status-expired');
    expect(component.getStatusClass('CANCELLED')).toBe('status-cancelled');
    expect(component.getStatusClass('UNKNOWN')).toBe('');
  });

  it('should get usage class correctly', () => {
    expect(component.getUsageClass(95)).toBe('usage-critical');
    expect(component.getUsageClass(80)).toBe('usage-warning');
    expect(component.getUsageClass(50)).toBe('usage-normal');
  });

  it('should view details', () => {
    subscriptionServiceSpy.getAllSubscriptions.and.returnValue(
      of(mockSubscriptions)
    );
    dialogSpy.open.and.returnValue({
      afterClosed: () => of(false),
    } as any);

    fixture.detectChanges();
    component.viewDetails(mockSubscriptions[0]);

    expect(dialogSpy.open).toHaveBeenCalled();
  });
});
