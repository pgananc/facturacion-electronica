import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ChangeDetectorRef, NO_ERRORS_SCHEMA } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { of, throwError } from 'rxjs';
import { CompanyComponent } from './company.component';
import { CompanyService } from '../../_service/customer/company.service';
import { LoggerService } from '../../_service/logger/logger.service';
import { ToastService } from '../../_service/notification/toast.service';
import { UserRoleService } from '../../_service/auth/user-role.service';
import { CompanyFilter } from '../../_model/customer/company-filter';
import { UpdateCompanyComponent } from './update-company/update-company.component';
import { ConfirmDialogComponent } from '../../_shared/confirm-dialog/confirm-dialog.component';
import { environment } from '../../../environments/environment';
import { SUPER_ADMIN_ROLE, ADMIN_ROLE } from '../../_constants/constants';

describe('CompanyComponent', () => {
  let component: CompanyComponent;
  let fixture: ComponentFixture<CompanyComponent>;
  let companyServiceSpy: jasmine.SpyObj<CompanyService>;
  let loggerSpy: jasmine.SpyObj<LoggerService>;
  let toastSpy: jasmine.SpyObj<ToastService>;
  let userRoleServiceSpy: jasmine.SpyObj<UserRoleService>;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;
  let dialogSpy: jasmine.SpyObj<MatDialog>;
  let cdrSpy: jasmine.SpyObj<ChangeDetectorRef>;

  const mockCompanies: CompanyFilter[] = [
    {
      idCompany: 1,
      idType: 1,
      identification: '1234567890001',
      name: 'Empresa Test 1',
      nameCommercial: 'Comercial Test 1',
      status: true,
      forcedToAccounting: 'N',
      principal: 'S',
      isSuperAdmin: false,
      companiesIds: [],
    },
    {
      idCompany: 2,
      idType: 1,
      identification: '9876543210001',
      name: 'Empresa Test 2',
      nameCommercial: 'Comercial Test 2',
      status: true,
      forcedToAccounting: 'N',
      principal: 'N',
      isSuperAdmin: false,
      companiesIds: [],
    },
  ];

  const mockPageResponse = {
    content: mockCompanies,
    totalElements: 2,
    totalPages: 1,
    size: 5,
    number: 0,
  };

  // Helper para crear token mock
  const createMockToken = (payload: any): string => {
    const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
    const body = btoa(JSON.stringify(payload));
    return `${header}.${body}.signature`;
  };

  beforeEach(async () => {
    const companyService = jasmine.createSpyObj('CompanyService', [
      'searchPageable',
      'delete',
    ]);
    const logger = jasmine.createSpyObj('LoggerService', [
      'debug',
      'info',
      'warn',
      'error',
    ]);
    const toast = jasmine.createSpyObj('ToastService', ['success', 'error']);
    const userRoleService = jasmine.createSpyObj('UserRoleService', [
      'getCurrentCompanyRoles',
      'getCurrentRolesSync',
    ]);
    const snackBar = jasmine.createSpyObj('MatSnackBar', ['open']);
    const dialog = jasmine.createSpyObj('MatDialog', ['open']);
    const cdr = jasmine.createSpyObj('ChangeDetectorRef', [
      'markForCheck',
      'detectChanges',
    ]);
    // Mock detectChanges para evitar que renderice el template en los tests
    cdr.detectChanges.and.callFake(() => {
      // No hacer nada, solo evitar el error
    });
    const activatedRoute = {
      snapshot: { params: {} },
      queryParams: of({}),
      children: [], // Agregar children para evitar error en template
    };

    await TestBed.configureTestingModule({
      declarations: [CompanyComponent],
      imports: [
        ReactiveFormsModule,
        MatTableModule,
        MatPaginatorModule,
        MatSortModule,
        MatSnackBarModule,
        MatDialogModule,
        BrowserAnimationsModule,
        HttpClientTestingModule,
      ],
      schemas: [NO_ERRORS_SCHEMA], // Ignorar componentes hijos en el template
      providers: [
        { provide: CompanyService, useValue: companyService },
        { provide: LoggerService, useValue: logger },
        { provide: ToastService, useValue: toast },
        { provide: UserRoleService, useValue: userRoleService },
        { provide: MatSnackBar, useValue: snackBar },
        { provide: MatDialog, useValue: dialog },
        { provide: ChangeDetectorRef, useValue: cdr },
        { provide: ActivatedRoute, useValue: activatedRoute },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CompanyComponent);
    component = fixture.componentInstance;
    companyServiceSpy = TestBed.inject(
      CompanyService
    ) as jasmine.SpyObj<CompanyService>;
    loggerSpy = TestBed.inject(LoggerService) as jasmine.SpyObj<LoggerService>;
    toastSpy = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
    userRoleServiceSpy = TestBed.inject(
      UserRoleService
    ) as jasmine.SpyObj<UserRoleService>;
    snackBarSpy = TestBed.inject(MatSnackBar) as jasmine.SpyObj<MatSnackBar>;
    dialogSpy = TestBed.inject(MatDialog) as jasmine.SpyObj<MatDialog>;
    cdrSpy = TestBed.inject(
      ChangeDetectorRef
    ) as jasmine.SpyObj<ChangeDetectorRef>;

    // Setup default mocks
    const mockToken = createMockToken({
      sub: 'testuser',
      roles: ['user'],
      companies: [1, 2],
    });
    localStorage.setItem(environment.TOKEN, mockToken);
    companyServiceSpy.searchPageable.and.returnValue(of(mockPageResponse));
    // Mock default roles
    userRoleServiceSpy.getCurrentCompanyRoles.and.returnValue(of(['user']));
    userRoleServiceSpy.getCurrentRolesSync.and.returnValue(['user']);

    // Inicializar dataSource antes de cualquier test para evitar errores en el template
    component.dataSource = new MatTableDataSource<CompanyFilter>([]);
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('ngOnInit', () => {
    it('should initialize form with default values', () => {
      component.dataSource = new MatTableDataSource<CompanyFilter>([]);
      component.ngOnInit();
      expect(component.form).toBeDefined();
      expect(component.form.get('identification')?.value).toBe('');
      expect(component.form.get('name')?.value).toBe('');
      expect(component.status).toBe(true);
    });

    it('should load companies on init when token exists', () => {
      // Inicializar dataSource antes de ngOnInit para evitar errores en el template
      component.dataSource = new MatTableDataSource<CompanyFilter>([]);
      component.ngOnInit();
      expect(companyServiceSpy.searchPageable).toHaveBeenCalled();
      expect(component.quantity).toBe(2);
      expect(component.dataSource.data.length).toBe(2);
    });

    it('should not load companies when token does not exist', () => {
      component.dataSource = new MatTableDataSource<CompanyFilter>([]);
      localStorage.removeItem(environment.TOKEN);
      component.ngOnInit();
      expect(companyServiceSpy.searchPageable).not.toHaveBeenCalled();
    });

    it('should add actions column for super_admin', (done) => {
      component.dataSource = new MatTableDataSource<CompanyFilter>([]);
      const superAdminToken = createMockToken({
        sub: 'admin',
        roles: [SUPER_ADMIN_ROLE.name],
        companies: [1, 2],
      });
      localStorage.setItem(environment.TOKEN, superAdminToken);
      userRoleServiceSpy.getCurrentCompanyRoles.and.returnValue(
        of([SUPER_ADMIN_ROLE.name])
      );
      userRoleServiceSpy.getCurrentRolesSync.and.returnValue([
        SUPER_ADMIN_ROLE.name,
      ]);
      component.ngOnInit();
      // Esperar a que el Observable se complete
      setTimeout(() => {
        expect(component.displayedColumns).toContain('actions');
        done();
      }, 100);
    });

    it('should add actions column for admin', (done) => {
      component.dataSource = new MatTableDataSource<CompanyFilter>([]);
      const adminToken = createMockToken({
        sub: 'admin',
        roles: [ADMIN_ROLE.name],
        companies: [1, 2],
      });
      localStorage.setItem(environment.TOKEN, adminToken);
      userRoleServiceSpy.getCurrentCompanyRoles.and.returnValue(
        of([ADMIN_ROLE.name])
      );
      userRoleServiceSpy.getCurrentRolesSync.and.returnValue([ADMIN_ROLE.name]);
      component.ngOnInit();
      // Esperar a que el Observable se complete
      setTimeout(() => {
        expect(component.displayedColumns).toContain('actions');
        done();
      }, 100);
    });

    it('should set roles and companiesIds from token', (done) => {
      component.dataSource = new MatTableDataSource<CompanyFilter>([]);
      const token = createMockToken({
        sub: 'testuser',
        roles: ['user', 'admin'],
        companies: [1, 2, 3],
      });
      localStorage.setItem(environment.TOKEN, token);
      userRoleServiceSpy.getCurrentCompanyRoles.and.returnValue(
        of(['user', 'admin'])
      );
      userRoleServiceSpy.getCurrentRolesSync.and.returnValue(['user', 'admin']);
      component.ngOnInit();
      // Esperar a que el Observable se complete
      setTimeout(() => {
        expect(component.roles).toEqual(['user', 'admin']);
        expect(component.companiesIds).toEqual([1, 2, 3]);
        done();
      }, 100);
    });
  });

  describe('ngAfterViewInit', () => {
    it('should subscribe to sort changes', () => {
      component.dataSource = new MatTableDataSource<CompanyFilter>([]);
      component.ngOnInit();
      fixture.detectChanges();
      component.ngAfterViewInit();
      expect(component.sort).toBeDefined();
    });
  });

  describe('search', () => {
    beforeEach(() => {
      // Inicializar dataSource antes de ngOnInit para evitar errores en el template
      component.dataSource = new MatTableDataSource<CompanyFilter>([]);
      component.ngOnInit();
      // No hacer detectChanges para evitar errores de template
    });

    it('should search companies with form values', () => {
      component.form.patchValue({
        identification: '123',
        name: 'Test',
      });
      component.status = true;
      component.search();
      expect(companyServiceSpy.searchPageable).toHaveBeenCalled();
      expect(component.quantity).toBe(2);
      expect(component.dataSource.data.length).toBe(2);
    });

    it('should reset paginator to first page when searching', () => {
      component.paginator = {
        pageIndex: 2,
      } as MatPaginator;
      component.search();
      expect(component.paginator.pageIndex).toBe(0);
    });

    it('should handle search error', () => {
      companyServiceSpy.searchPageable.and.returnValue(
        throwError(() => new Error('Search error'))
      );
      component.search();
      expect(snackBarSpy.open).toHaveBeenCalled();
      expect(loggerSpy.error).toHaveBeenCalled();
    });

    it('should update dataSource when search returns results', () => {
      component.search();
      expect(component.dataSource.data.length).toBe(2);
      expect(component.quantity).toBe(2);
    });

    it('should show empty message when search returns null', () => {
      companyServiceSpy.searchPageable.and.returnValue(of(null));
      component.search();
      expect(snackBarSpy.open).toHaveBeenCalled();
    });
  });

  describe('limpiar', () => {
    beforeEach(() => {
      component.dataSource = new MatTableDataSource<CompanyFilter>([]);
      component.ngOnInit();
    });

    it('should reset form to default values', () => {
      component.form.patchValue({
        identification: '123',
        name: 'Test',
      });
      component.status = false;
      component.limpiar();
      expect(component.form.get('identification')?.value).toBe('');
      expect(component.form.get('name')?.value).toBe('');
      expect(component.status).toBe(true);
    });

    it('should call search after clearing', () => {
      spyOn(component, 'search');
      component.limpiar();
      expect(component.search).toHaveBeenCalled();
    });
  });

  describe('delete', () => {
    beforeEach(() => {
      component.dataSource = new MatTableDataSource<CompanyFilter>([]);
      component.ngOnInit();
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
          title: 'Confirmar Eliminación',
          type: 'danger',
        }),
      });
    });

    it('should delete company when confirmed', () => {
      companyServiceSpy.delete.and.returnValue(of({}));
      spyOn(component, 'search');
      dialogSpy.open.and.returnValue({
        afterClosed: () => of(true),
      } as any);
      component.delete(1);
      expect(companyServiceSpy.delete).toHaveBeenCalledWith(1);
      expect(component.search).toHaveBeenCalled();
      expect(toastSpy.success).toHaveBeenCalled();
    });

    it('should not delete company when cancelled', () => {
      dialogSpy.open.and.returnValue({
        afterClosed: () => of(false),
      } as any);
      component.delete(1);
      expect(companyServiceSpy.delete).not.toHaveBeenCalled();
    });

    it('should handle delete error', () => {
      companyServiceSpy.delete.and.returnValue(
        throwError(() => new Error('Delete error'))
      );
      dialogSpy.open.and.returnValue({
        afterClosed: () => of(true),
      } as any);
      component.delete(1);
      expect(snackBarSpy.open).toHaveBeenCalled();
      expect(loggerSpy.error).toHaveBeenCalled();
    });
  });

  describe('showMore', () => {
    beforeEach(() => {
      component.dataSource = new MatTableDataSource<CompanyFilter>([]);
      component.ngOnInit();
    });

    it('should load more companies with pagination', () => {
      const pageEvent = {
        pageIndex: 1,
        pageSize: 10,
      };
      component.showMore(pageEvent);
      expect(companyServiceSpy.searchPageable).toHaveBeenCalledWith(
        1,
        10,
        jasmine.any(Object)
      );
    });

    it('should update pageSize when paginating', () => {
      const pageEvent = {
        pageIndex: 1,
        pageSize: 10,
      };
      component.showMore(pageEvent);
      expect(component.pageSize).toBe(10);
    });

    it('should handle error when loading more companies', () => {
      companyServiceSpy.searchPageable.and.returnValue(
        throwError(() => new Error('Load error'))
      );
      const pageEvent = {
        pageIndex: 1,
        pageSize: 10,
      };
      component.showMore(pageEvent);
      expect(snackBarSpy.open).toHaveBeenCalled();
    });
  });

  describe('openDialog', () => {
    beforeEach(() => {
      component.dataSource = new MatTableDataSource<CompanyFilter>([]);
      component.ngOnInit();
    });

    it('should open create dialog when no company provided', () => {
      dialogSpy.open.and.returnValue({
        afterClosed: () => of(false),
      } as any);
      component.openDialog();
      expect(dialogSpy.open).toHaveBeenCalledWith(
        UpdateCompanyComponent,
        jasmine.objectContaining({
          data: { isEdit: false },
        })
      );
    });

    it('should open edit dialog when company provided', () => {
      dialogSpy.open.and.returnValue({
        afterClosed: () => of(false),
      } as any);
      component.openDialog(mockCompanies[0]);
      expect(dialogSpy.open).toHaveBeenCalledWith(
        UpdateCompanyComponent,
        jasmine.objectContaining({
          data: { company: mockCompanies[0], isEdit: true },
        })
      );
    });

    it('should call search when dialog returns result', () => {
      spyOn(component, 'search');
      dialogSpy.open.and.returnValue({
        afterClosed: () => of(true),
      } as any);
      component.openDialog();
      expect(component.search).toHaveBeenCalled();
    });

    it('should not call search when dialog returns no result', () => {
      spyOn(component, 'search');
      dialogSpy.open.and.returnValue({
        afterClosed: () => of(false),
      } as any);
      component.openDialog();
      expect(component.search).not.toHaveBeenCalled();
    });
  });

  describe('applyFilter', () => {
    beforeEach(() => {
      component.dataSource = new MatTableDataSource<CompanyFilter>([]);
      component.ngOnInit();
    });

    it('should apply filter to dataSource', () => {
      const event = {
        target: { value: 'test' },
      } as any;
      component.applyFilter(event);
      expect(component.dataSource.filter).toBe('test');
    });

    it('should trim and lowercase filter value', () => {
      const event = {
        target: { value: '  TEST  ' },
      } as any;
      component.applyFilter(event);
      expect(component.dataSource.filter).toBe('test');
    });
  });

  describe('getRegimeLabel', () => {
    it('should return label for valid regime code', () => {
      const label = component.getRegimeLabel('GENERAL');
      expect(label).toBe('General');
    });

    it('should return "-" for null or undefined', () => {
      expect(component.getRegimeLabel(null)).toBe('-');
      expect(component.getRegimeLabel(undefined)).toBe('-');
    });

    it('should return code if not found in options', () => {
      const label = component.getRegimeLabel('UNKNOWN');
      expect(label).toBe('UNKNOWN');
    });
  });
});
