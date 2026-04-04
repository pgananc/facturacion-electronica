import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ChangeDetectorRef } from '@angular/core';
import { of, throwError, Subject } from 'rxjs';
import { ClientComponent } from './client.component';
import { ClientService } from '../../../_service/customer/client.service';
import { ClientHelperService } from '../../../_service/customer/client-helper.service';
import { LoggerService } from '../../../_service/logger/logger.service';
import { ToastService } from '../../../_service/notification/toast.service';
import { Client } from '../../../_model/customer/client';
import { ClientPageResponse } from '../../../_model/customer/client-page-response';
import { UpdateClientComponent } from './update-client/update-client.component';
import { ConfirmDialogComponent } from '../../../_shared/confirm-dialog/confirm-dialog.component';
import { environment } from '../../../../environments/environment';

describe('ClientComponent', () => {
  let component: ClientComponent;
  let fixture: ComponentFixture<ClientComponent>;
  let clientServiceSpy: jasmine.SpyObj<ClientService>;
  let clientHelperSpy: jasmine.SpyObj<ClientHelperService>;
  let loggerSpy: jasmine.SpyObj<LoggerService>;
  let toastSpy: jasmine.SpyObj<ToastService>;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;
  let dialogSpy: jasmine.SpyObj<MatDialog>;
  let cdrSpy: jasmine.SpyObj<ChangeDetectorRef>;

  const mockClients: Client[] = [
    {
      idClient: 1,
      identification: '1234567890001',
      name: 'Cliente Test 1',
      address: 'Dirección 1',
      idType: 1,
      clientType: 1,
      status: true,
      companyIdentification: '1234567890001',
      contacts: [],
    },
    {
      idClient: 2,
      identification: '9876543210001',
      name: 'Cliente Test 2',
      address: 'Dirección 2',
      idType: 1,
      clientType: 2,
      status: true,
      companyIdentification: '1234567890001',
      contacts: [],
    },
  ];

  const mockPageResponse: ClientPageResponse = {
    content: mockClients,
    totalElements: 2,
    totalPages: 1,
    size: 5,
    number: 0,
  };

  beforeEach(async () => {
    const clientService = jasmine.createSpyObj('ClientService', [
      'searchPageable',
      'delete',
      'clientsChange',
    ]);
    const clientHelper = jasmine.createSpyObj('ClientHelperService', [
      'addContactInformation',
      'getCompanyIdentificationFromSession',
    ]);
    const logger = jasmine.createSpyObj('LoggerService', [
      'debug',
      'info',
      'warn',
      'error',
    ]);
    const toast = jasmine.createSpyObj('ToastService', [
      'success',
      'error',
      'warning',
    ]);
    const snackBar = jasmine.createSpyObj('MatSnackBar', ['open']);
    const dialog = jasmine.createSpyObj('MatDialog', ['open']);
    const cdr = jasmine.createSpyObj('ChangeDetectorRef', ['markForCheck']);

    // Setup clientsChange as Subject
    clientService.clientsChange = new Subject<Client[]>();

    await TestBed.configureTestingModule({
      declarations: [ClientComponent],
      imports: [
        ReactiveFormsModule,
        MatTableModule,
        MatPaginatorModule,
        MatSortModule,
        MatSnackBarModule,
        MatDialogModule,
        BrowserAnimationsModule,
      ],
      providers: [
        { provide: ClientService, useValue: clientService },
        { provide: ClientHelperService, useValue: clientHelper },
        { provide: LoggerService, useValue: logger },
        { provide: ToastService, useValue: toast },
        { provide: MatSnackBar, useValue: snackBar },
        { provide: MatDialog, useValue: dialog },
        { provide: ChangeDetectorRef, useValue: cdr },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ClientComponent);
    component = fixture.componentInstance;
    clientServiceSpy = TestBed.inject(
      ClientService
    ) as jasmine.SpyObj<ClientService>;
    clientHelperSpy = TestBed.inject(
      ClientHelperService
    ) as jasmine.SpyObj<ClientHelperService>;
    loggerSpy = TestBed.inject(LoggerService) as jasmine.SpyObj<LoggerService>;
    toastSpy = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
    snackBarSpy = TestBed.inject(MatSnackBar) as jasmine.SpyObj<MatSnackBar>;
    dialogSpy = TestBed.inject(MatDialog) as jasmine.SpyObj<MatDialog>;
    cdrSpy = TestBed.inject(
      ChangeDetectorRef
    ) as jasmine.SpyObj<ChangeDetectorRef>;

    // Setup default mocks
    localStorage.setItem('token', 'test-token');
    localStorage.setItem(environment.IDENTIFICATION, '1234567890001');
    clientHelperSpy.getCompanyIdentificationFromSession.and.returnValue(
      '1234567890001'
    );
    clientHelperSpy.addContactInformation.and.stub(); // Es void, no retorna nada
    clientServiceSpy.searchPageable.and.returnValue(of(mockPageResponse));
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('ngOnInit', () => {
    it('should initialize form with default values', () => {
      component.ngOnInit();
      expect(component.form).toBeDefined();
      expect(component.form.get('identification')?.value).toBe('');
      expect(component.form.get('name')?.value).toBe('');
      expect(component.form.get('clientType')?.value).toBe(0);
      expect(component.form.get('status')?.value).toBe(true);
    });

    it('should load clients on init when token exists', () => {
      component.ngOnInit();
      expect(clientServiceSpy.searchPageable).toHaveBeenCalled();
      expect(component.quantity).toBe(2);
      expect(component.dataSourceClients.data.length).toBe(2);
    });

    it('should not load clients when token does not exist', () => {
      localStorage.removeItem('token');
      component.ngOnInit();
      expect(clientServiceSpy.searchPageable).not.toHaveBeenCalled();
    });

    it('should handle error when loading clients fails', () => {
      clientServiceSpy.searchPageable.and.returnValue(
        throwError(() => new Error('Network error'))
      );
      component.ngOnInit();
      expect(toastSpy.error).toHaveBeenCalled();
      expect(loggerSpy.error).toHaveBeenCalled();
    });

    it('should subscribe to clientsChange', () => {
      component.ngOnInit();
      const newClients: Client[] = [
        {
          idClient: 3,
          identification: '1111111111111',
          name: 'New Client',
          address: 'New Address',
          idType: 1,
          clientType: 1,
          status: true,
          companyIdentification: '1234567890001',
          contacts: [],
        },
      ];
      (clientServiceSpy.clientsChange as Subject<Client[]>).next(newClients);
      expect(component.dataSourceClients.data).toEqual(newClients);
    });
  });

  describe('ngAfterViewInit', () => {
    it('should subscribe to sort changes', () => {
      fixture.detectChanges();
      component.ngAfterViewInit();
      expect(component.sort).toBeDefined();
    });
  });

  describe('search', () => {
    beforeEach(() => {
      component.ngOnInit();
      fixture.detectChanges();
    });

    it('should search clients with form values', () => {
      component.form.patchValue({
        identification: '123',
        name: 'Test',
        clientType: 1,
        status: true,
      });
      component.search();
      expect(clientServiceSpy.searchPageable).toHaveBeenCalled();
      expect(component.isLoading).toBe(false);
    });

    it('should reset paginator to first page when searching', () => {
      component.paginator = {
        pageIndex: 2,
      } as MatPaginator;
      component.search();
      expect(component.paginator.pageIndex).toBe(0);
    });

    it('should set loading state during search', () => {
      component.search();
      expect(component.isLoading).toBe(false); // Should be false after completion
    });

    it('should handle search error', () => {
      clientServiceSpy.searchPageable.and.returnValue(
        throwError(() => new Error('Search error'))
      );
      component.search();
      expect(toastSpy.error).toHaveBeenCalled();
      expect(component.isLoading).toBe(false);
    });

    it('should update dataSource when search returns results', () => {
      component.search();
      expect(component.dataSourceClients.data.length).toBe(2);
      expect(component.quantity).toBe(2);
    });

    it('should clear dataSource when search returns no results', () => {
      clientServiceSpy.searchPageable.and.returnValue(
        of({ ...mockPageResponse, content: [], totalElements: 0 })
      );
      component.search();
      expect(component.dataSourceClients.data.length).toBe(0);
      expect(component.quantity).toBe(0);
    });
  });

  describe('limpiar', () => {
    beforeEach(() => {
      component.ngOnInit();
    });

    it('should reset form to default values', () => {
      component.form.patchValue({
        identification: '123',
        name: 'Test',
        clientType: 2,
        status: false,
      });
      component.limpiar();
      expect(component.form.get('identification')?.value).toBe('');
      expect(component.form.get('name')?.value).toBe('');
      expect(component.form.get('clientType')?.value).toBe(0);
      expect(component.form.get('status')?.value).toBe(true);
    });

    it('should call search after clearing', () => {
      spyOn(component, 'search');
      component.limpiar();
      expect(component.search).toHaveBeenCalled();
    });
  });

  describe('delete', () => {
    beforeEach(() => {
      component.ngOnInit();
      fixture.detectChanges();
    });

    it('should open confirmation dialog', () => {
      dialogSpy.open.and.returnValue({
        afterClosed: () => of(false),
      } as any);
      component.delete(1);
      expect(dialogSpy.open).toHaveBeenCalledWith(ConfirmDialogComponent, {
        width: '500px',
        maxWidth: '90vw',
        data: jasmine.objectContaining({
          title: 'Confirmar eliminación',
          type: 'danger',
        }),
      });
    });

    it('should delete client when confirmed', () => {
      clientServiceSpy.delete.and.returnValue(of({}));
      spyOn(component, 'search');
      dialogSpy.open.and.returnValue({
        afterClosed: () => of(true),
      } as any);
      component.delete(1);
      expect(clientServiceSpy.delete).toHaveBeenCalledWith(1);
      expect(component.search).toHaveBeenCalled();
      expect(toastSpy.success).toHaveBeenCalled();
    });

    it('should not delete client when cancelled', () => {
      dialogSpy.open.and.returnValue({
        afterClosed: () => of(false),
      } as any);
      component.delete(1);
      expect(clientServiceSpy.delete).not.toHaveBeenCalled();
    });

    it('should handle delete error', () => {
      clientServiceSpy.delete.and.returnValue(
        throwError(() => new Error('Delete error'))
      );
      dialogSpy.open.and.returnValue({
        afterClosed: () => of(true),
      } as any);
      component.delete(1);
      expect(toastSpy.error).toHaveBeenCalled();
      expect(loggerSpy.error).toHaveBeenCalled();
    });
  });

  describe('showMore', () => {
    beforeEach(() => {
      component.ngOnInit();
    });

    it('should load more clients with pagination', () => {
      const pageEvent = {
        pageIndex: 1,
        pageSize: 5,
      };
      component.showMore(pageEvent);
      expect(clientServiceSpy.searchPageable).toHaveBeenCalledWith(
        1,
        5,
        jasmine.any(Object),
        '',
        ''
      );
    });

    it('should handle error when loading more clients', () => {
      clientServiceSpy.searchPageable.and.returnValue(
        throwError(() => new Error('Load error'))
      );
      const pageEvent = {
        pageIndex: 1,
        pageSize: 5,
      };
      component.showMore(pageEvent);
      expect(toastSpy.error).toHaveBeenCalled();
    });
  });

  describe('onSortChange', () => {
    beforeEach(() => {
      component.ngOnInit();
      fixture.detectChanges();
    });

    it('should update sort state and call search', () => {
      spyOn(component, 'search');
      component.sort = {
        active: 'name',
        direction: 'asc',
      } as MatSort;
      component.onSortChange();
      expect(component.currentSortField).toBe('name');
      expect(component.currentSortDirection).toBe('asc');
      expect(component.search).toHaveBeenCalled();
    });

    it('should reset paginator when sorting', () => {
      component.paginator = {
        pageIndex: 2,
      } as MatPaginator;
      component.sort = {
        active: 'name',
        direction: 'asc',
      } as MatSort;
      component.onSortChange();
      expect(component.paginator.pageIndex).toBe(0);
    });

    it('should not do anything if sort is not defined', () => {
      component.sort = undefined as any;
      spyOn(component, 'search');
      component.onSortChange();
      expect(component.search).not.toHaveBeenCalled();
    });
  });

  describe('numericOnly', () => {
    it('should remove non-numeric characters', () => {
      const event = {
        target: { value: 'abc123def456' },
      } as any;
      component.numericOnly(event);
      expect(event.target.value).toBe('123456');
    });

    it('should keep only numbers', () => {
      const event = {
        target: { value: '123-456-789' },
      } as any;
      component.numericOnly(event);
      expect(event.target.value).toBe('123456789');
    });
  });

  describe('hasClientData', () => {
    it('should return true when dataSource has data', () => {
      component.dataSourceClients.data = mockClients;
      expect(component.hasClientData).toBe(true);
    });

    it('should return false when dataSource is empty', () => {
      component.dataSourceClients.data = [];
      expect(component.hasClientData).toBe(false);
    });

    it('should return false when dataSource is undefined', () => {
      component.dataSourceClients.data = undefined as any;
      expect(component.hasClientData).toBe(false);
    });
  });

  describe('getClientTypeLabel', () => {
    beforeEach(() => {
      component.ngOnInit();
    });

    it('should return correct label for valid code', () => {
      const label = component.getClientTypeLabel(1);
      expect(label).toBeDefined();
    });

    it('should return "Desconocido" for invalid code', () => {
      const label = component.getClientTypeLabel(999);
      expect(label).toBe('Desconocido');
    });
  });

  describe('openCreateDialog', () => {
    beforeEach(() => {
      component.ngOnInit();
      localStorage.setItem(
        'COMPANY_CONTACTS',
        JSON.stringify([{ contactType: 'MAI', value: 'test@example.com' }])
      );
    });

    it('should open create dialog', (done) => {
      clientServiceSpy.searchPageable.and.returnValue(
        of({ ...mockPageResponse, content: [], totalElements: 0 })
      );
      dialogSpy.open.and.returnValue({
        afterClosed: () => of(true),
      } as any);
      spyOn(component, 'search');
      component.openCreateDialog();
      setTimeout(() => {
        expect(dialogSpy.open).toHaveBeenCalledWith(
          UpdateClientComponent,
          jasmine.objectContaining({
            data: jasmine.objectContaining({
              mode: 'create',
            }),
          })
        );
        done();
      }, 100);
    });
  });

  describe('openEditDialog', () => {
    beforeEach(() => {
      component.ngOnInit();
    });

    it('should open edit dialog with client id', () => {
      dialogSpy.open.and.returnValue({
        afterClosed: () => of(true),
      } as any);
      component.openEditDialog(1);
      expect(dialogSpy.open).toHaveBeenCalledWith(
        UpdateClientComponent,
        jasmine.objectContaining({
          data: { mode: 'edit', idClient: 1 },
        })
      );
    });
  });

  describe('copyEmail', () => {
    let clipboardSpy: jasmine.Spy;

    beforeEach(() => {
      component.ngOnInit();
      clipboardSpy = spyOn(navigator.clipboard, 'writeText').and.returnValue(
        Promise.resolve()
      );
    });

    it('should copy email to clipboard', async () => {
      component.copyEmail('test@example.com');
      await new Promise((resolve) => setTimeout(resolve, 10));
      expect(clipboardSpy).toHaveBeenCalledWith('test@example.com');
      expect(toastSpy.success).toHaveBeenCalledWith(
        'Email copiado: test@example.com'
      );
    });

    it('should show message when email is empty', () => {
      component.copyEmail('');
      expect(toastSpy.warning).toHaveBeenCalledWith('No hay email para copiar');
    });

    it('should handle clipboard error', async () => {
      clipboardSpy.and.returnValue(
        Promise.reject(new Error('Clipboard error'))
      );
      component.copyEmail('test@example.com');
      // Esperar a que la promesa rechazada se maneje en el catch
      await new Promise((resolve) => setTimeout(resolve, 10));
      expect(loggerSpy.error).toHaveBeenCalled();
      expect(toastSpy.error).toHaveBeenCalledWith('Error al copiar email');
    });
  });

  describe('buildClientRequest', () => {
    beforeEach(() => {
      component.ngOnInit();
    });

    it('should build request with form values', () => {
      component.form.patchValue({
        identification: '123',
        name: 'Test',
        clientType: 1,
        status: true,
      });
      const request = component['buildClientRequest']();
      expect(request.identification).toBe('123');
      expect(request.name).toBe('Test');
      expect(request.clientType).toBe(1);
      expect(request.status).toBe(true);
    });

    it('should handle error when building request', () => {
      clientHelperSpy.getCompanyIdentificationFromSession.and.throwError(
        'Session error'
      );
      expect(() => component['buildClientRequest']()).toThrow();
      expect(loggerSpy.error).toHaveBeenCalled();
      expect(toastSpy.error).toHaveBeenCalled();
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
