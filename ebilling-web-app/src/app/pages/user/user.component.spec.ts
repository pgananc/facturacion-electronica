import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ChangeDetectorRef, NO_ERRORS_SCHEMA } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { of, throwError, Subject } from 'rxjs';
import { UserComponent } from './user.component';
import { UserService } from '../../_service/user/user.service';
import { LoggerService } from '../../_service/logger/logger.service';
import { ToastService } from '../../_service/notification/toast.service';
import { User } from '../../_model/auth/user';
import { Role } from '../../_model/auth/role';
import { UpdateUserComponent } from './update-user/update-user.component';
import { environment } from '../../../environments/environment';

describe('UserComponent', () => {
  let component: UserComponent;
  let fixture: ComponentFixture<UserComponent>;
  let userServiceSpy: jasmine.SpyObj<UserService>;
  let loggerSpy: jasmine.SpyObj<LoggerService>;
  let toastSpy: jasmine.SpyObj<ToastService>;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;
  let dialogSpy: jasmine.SpyObj<MatDialog>;
  let cdrSpy: jasmine.SpyObj<ChangeDetectorRef>;

  const mockRoles: Role[] = [
    { idRole: 1, name: 'Admin', description: 'Administrator', status: true },
  ];

  const mockUsers: User[] = [
    {
      idUser: 1,
      name: 'Usuario Test 1',
      userName: 'user1',
      password: '',
      mail: 'user1@test.com',
      status: true,
      idCompany: 1,
      roleDtos: mockRoles,
    },
    {
      idUser: 2,
      name: 'Usuario Test 2',
      userName: 'user2',
      password: '',
      mail: 'user2@test.com',
      status: true,
      idCompany: 1,
      roleDtos: mockRoles,
    },
  ];

  const mockPageResponse = {
    content: mockUsers,
    totalElements: 2,
    totalPages: 1,
    size: 10,
    number: 0,
  };

  // Helper para crear token mock
  const createMockToken = (payload: any): string => {
    const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
    const body = btoa(JSON.stringify(payload));
    return `${header}.${body}.signature`;
  };

  beforeEach(async () => {
    const userService = jasmine.createSpyObj('UserService', [
      'searchPageable',
      'delete',
      'userChange',
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
      'httpError',
    ]);
    const snackBar = jasmine.createSpyObj('MatSnackBar', ['open']);
    const dialog = jasmine.createSpyObj('MatDialog', ['open']);
    const cdr = jasmine.createSpyObj('ChangeDetectorRef', ['markForCheck']);
    const activatedRoute = {
      snapshot: { params: {} },
      queryParams: of({}),
    };

    // Setup userChange as Subject
    userService.userChange = new Subject<User[]>();

    await TestBed.configureTestingModule({
      declarations: [UserComponent],
      imports: [
        ReactiveFormsModule,
        MatTableModule,
        MatPaginatorModule,
        MatSortModule,
        MatSnackBarModule,
        MatDialogModule,
        BrowserAnimationsModule,
      ],
      schemas: [NO_ERRORS_SCHEMA], // Ignorar componentes hijos en el template
      providers: [
        { provide: UserService, useValue: userService },
        { provide: LoggerService, useValue: logger },
        { provide: ToastService, useValue: toast },
        { provide: MatSnackBar, useValue: snackBar },
        { provide: MatDialog, useValue: dialog },
        { provide: ChangeDetectorRef, useValue: cdr },
        { provide: ActivatedRoute, useValue: activatedRoute },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UserComponent);
    component = fixture.componentInstance;
    userServiceSpy = TestBed.inject(UserService) as jasmine.SpyObj<UserService>;
    loggerSpy = TestBed.inject(LoggerService) as jasmine.SpyObj<LoggerService>;
    toastSpy = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
    snackBarSpy = TestBed.inject(MatSnackBar) as jasmine.SpyObj<MatSnackBar>;
    dialogSpy = TestBed.inject(MatDialog) as jasmine.SpyObj<MatDialog>;
    cdrSpy = TestBed.inject(
      ChangeDetectorRef
    ) as jasmine.SpyObj<ChangeDetectorRef>;

    // Setup default mocks
    const mockToken = createMockToken({
      sub: 'testuser',
      idCompany: 1,
      userName: 'testuser',
    });
    localStorage.setItem(environment.TOKEN, mockToken);
    localStorage.setItem(environment.ID_COMPANY, '1');
    userServiceSpy.searchPageable.and.returnValue(of(mockPageResponse));
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
      expect(component.form.get('name')?.value).toBe('');
      expect(component.form.get('userName')?.value).toBe('');
      expect(component.status).toBe(true);
    });

    it('should initialize dataSource', () => {
      component.ngOnInit();
      expect(component.dataSource).toBeDefined();
    });

    it('should load users on init when token exists', () => {
      component.ngOnInit();
      expect(userServiceSpy.searchPageable).toHaveBeenCalled();
      expect(component.quantity).toBe(2);
      expect(component.dataSource.data.length).toBe(2);
    });

    it('should not load users when token does not exist', () => {
      localStorage.removeItem(environment.TOKEN);
      component.ngOnInit();
      expect(userServiceSpy.searchPageable).not.toHaveBeenCalled();
    });

    it('should handle error when loading users fails', () => {
      userServiceSpy.searchPageable.and.returnValue(
        throwError(() => new Error('Network error'))
      );
      component.ngOnInit();
      expect(loggerSpy.error).toHaveBeenCalled();
    });

    it('should subscribe to userChange', () => {
      component.ngOnInit();
      const newUsers: User[] = [
        {
          idUser: 3,
          name: 'New User',
          userName: 'newuser',
          password: '',
          mail: 'new@test.com',
          status: true,
          idCompany: 1,
          roleDtos: [],
        },
      ];
      (userServiceSpy.userChange as Subject<User[]>).next(newUsers);
      expect(component.dataSource.data).toEqual(newUsers);
    });
  });

  describe('search', () => {
    beforeEach(() => {
      component.ngOnInit();
      // No hacer detectChanges para evitar errores de template
    });

    it('should search users with form values', () => {
      component.form.patchValue({
        name: 'Test',
        userName: 'user',
      });
      component.status = true;
      component.search();
      expect(userServiceSpy.searchPageable).toHaveBeenCalled();
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
      userServiceSpy.searchPageable.and.returnValue(
        throwError(() => new Error('Search error'))
      );
      component.search();
      expect(snackBarSpy.open).toHaveBeenCalled();
    });

    it('should update dataSource when search returns results', () => {
      component.search();
      expect(component.dataSource.data.length).toBe(2);
      expect(component.quantity).toBe(2);
    });

    it('should show empty message when search returns null', () => {
      userServiceSpy.searchPageable.and.returnValue(of(null));
      component.search();
      expect(snackBarSpy.open).toHaveBeenCalled();
    });
  });

  describe('limpiar', () => {
    beforeEach(() => {
      component.ngOnInit();
    });

    it('should reset form to default values', () => {
      component.form.patchValue({
        name: 'Test',
        userName: 'user',
      });
      component.status = false;
      component.limpiar();
      expect(component.form.get('name')?.value).toBe('');
      expect(component.form.get('userName')?.value).toBe('');
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
      component.ngOnInit();
      component.dataSource.data = [...mockUsers];
    });

    it('should delete user successfully', () => {
      userServiceSpy.delete.and.returnValue(of({}));
      component.delete(1);
      expect(userServiceSpy.delete).toHaveBeenCalledWith(1);
      expect(toastSpy.success).toHaveBeenCalled();
      expect(component.dataSource.data.length).toBe(1);
    });

    it('should handle delete error', () => {
      userServiceSpy.delete.and.returnValue(
        throwError(() => new Error('Delete error'))
      );
      component.delete(1);
      expect(loggerSpy.error).toHaveBeenCalled();
      expect(toastSpy.httpError).toHaveBeenCalled();
    });
  });

  describe('showMore', () => {
    beforeEach(() => {
      component.ngOnInit();
    });

    it('should load more users with pagination', () => {
      const pageEvent = {
        pageIndex: 1,
        pageSize: 10,
      };
      component.showMore(pageEvent);
      expect(userServiceSpy.searchPageable).toHaveBeenCalledWith(
        1,
        10,
        jasmine.any(Object)
      );
    });

    it('should handle error when loading more users', () => {
      userServiceSpy.searchPageable.and.returnValue(
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
      component.ngOnInit();
    });

    it('should open create dialog when no user provided', () => {
      dialogSpy.open.and.returnValue({
        afterClosed: () => of(false),
      } as any);
      component.openDialog();
      expect(dialogSpy.open).toHaveBeenCalledWith(
        UpdateUserComponent,
        jasmine.objectContaining({
          data: { isEdit: false },
        })
      );
    });

    it('should open edit dialog when user provided', () => {
      dialogSpy.open.and.returnValue({
        afterClosed: () => of(false),
      } as any);
      component.openDialog(mockUsers[0]);
      expect(dialogSpy.open).toHaveBeenCalledWith(
        UpdateUserComponent,
        jasmine.objectContaining({
          data: { user: mockUsers[0], isEdit: true },
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
});
