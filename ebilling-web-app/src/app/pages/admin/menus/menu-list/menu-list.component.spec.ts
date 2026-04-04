import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { of, throwError } from 'rxjs';
import { MenuListComponent } from './menu-list.component';
import { MenuAdminService } from '../../../../_service/admin/menu-admin.service';
import { ToastService } from '../../../../_service/notification/toast.service';
import { LoggerService } from '../../../../_service/logger/logger.service';
import { MenuAdmin } from '../../../../_model/admin/menu-admin';
import { MenuFormDialogComponent } from '../menu-form-dialog/menu-form-dialog.component';
import { ConfirmDialogComponent } from '../../../../_shared/confirm-dialog/confirm-dialog.component';

describe('MenuListComponent', () => {
  let component: MenuListComponent;
  let fixture: ComponentFixture<MenuListComponent>;
  let menuServiceSpy: jasmine.SpyObj<MenuAdminService>;
  let toastSpy: jasmine.SpyObj<ToastService>;
  let loggerSpy: jasmine.SpyObj<LoggerService>;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;
  let dialogSpy: jasmine.SpyObj<MatDialog>;

  const mockMenus: MenuAdmin[] = [
    {
      id: 1,
      name: 'Dashboard',
      icon: 'dashboard',
      url: '/dashboard',
      featureCode: 'DASHBOARD',
      active: true,
      subMenus: [],
    },
    {
      id: 2,
      name: 'Facturación',
      icon: 'receipt',
      url: '/invoices',
      featureCode: 'INVOICES',
      active: true,
      subMenus: [
        {
          id: 3,
          name: 'Crear Factura',
          icon: 'add',
          url: '/invoices/create',
          featureCode: 'INVOICES_CREATE',
          active: true,
          subMenus: [],
        },
      ],
    },
  ];

  beforeEach(async () => {
    const menuService = jasmine.createSpyObj('MenuAdminService', [
      'getAllMenus',
      'deleteMenu',
      'updateMenu',
    ]);
    const toast = jasmine.createSpyObj('ToastService', [
      'success',
      'error',
      'warning',
      'httpError',
    ]);
    const logger = jasmine.createSpyObj('LoggerService', [
      'debug',
      'error',
      'warn',
    ]);
    const snackBar = jasmine.createSpyObj('MatSnackBar', ['open']);
    const dialog = jasmine.createSpyObj('MatDialog', ['open']);

    await TestBed.configureTestingModule({
      declarations: [MenuListComponent],
      imports: [
        MatDialogModule,
        MatSnackBarModule,
        MatTableModule,
        MatSortModule,
        BrowserAnimationsModule,
      ],
      providers: [
        { provide: MenuAdminService, useValue: menuService },
        { provide: ToastService, useValue: toast },
        { provide: LoggerService, useValue: logger },
        { provide: MatSnackBar, useValue: snackBar },
        { provide: MatDialog, useValue: dialog },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();

    fixture = TestBed.createComponent(MenuListComponent);
    component = fixture.componentInstance;
    menuServiceSpy = TestBed.inject(
      MenuAdminService
    ) as jasmine.SpyObj<MenuAdminService>;
    toastSpy = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
    loggerSpy = TestBed.inject(LoggerService) as jasmine.SpyObj<LoggerService>;
    snackBarSpy = TestBed.inject(MatSnackBar) as jasmine.SpyObj<MatSnackBar>;
    dialogSpy = TestBed.inject(MatDialog) as jasmine.SpyObj<MatDialog>;

    // Setup default mocks
    menuServiceSpy.getAllMenus.and.returnValue(of(mockMenus));
    component.dataSource = new MatTableDataSource<
      MenuAdmin & { level?: number }
    >([]);
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('ngOnInit', () => {
    it('should load menus on init', () => {
      component.ngOnInit();
      expect(menuServiceSpy.getAllMenus).toHaveBeenCalled();
      expect(component.menus).toEqual(mockMenus);
      expect(component.dataSource.data.length).toBeGreaterThan(0);
    });

    it('should handle error when loading menus', () => {
      const error = new Error('Failed to load menus');
      menuServiceSpy.getAllMenus.and.returnValue(throwError(() => error));
      component.ngOnInit();
      expect(loggerSpy.error).toHaveBeenCalled();
      expect(toastSpy.httpError).toHaveBeenCalled();
    });
  });

  describe('ngAfterViewInit', () => {
    it('should connect sort to dataSource', () => {
      component.ngOnInit();
      fixture.detectChanges();
      component.ngAfterViewInit();
      expect(component.sort).toBeDefined();
      expect(component.dataSource.sort).toBe(component.sort);
    });
  });

  describe('loadMenus', () => {
    it('should load menus and update dataSource', () => {
      component.loadMenus();
      expect(menuServiceSpy.getAllMenus).toHaveBeenCalled();
      expect(component.menus).toEqual(mockMenus);
      expect(component.dataSource.data.length).toBe(3); // 2 padres + 1 submenú
    });

    it('should set isLoading to false after loading', (done) => {
      component.isLoading = true;
      component.loadMenus();
      setTimeout(() => {
        // El isLoading se establece en false en el finalize del pipe
        expect(component.isLoading).toBe(false);
        expect(component.menus.length).toBeGreaterThanOrEqual(0);
        done();
      }, 100);
    });

    it('should handle error when loading menus', () => {
      const error = new Error('Failed to load menus');
      menuServiceSpy.getAllMenus.and.returnValue(throwError(() => error));
      component.loadMenus();
      expect(loggerSpy.error).toHaveBeenCalled();
      expect(toastSpy.httpError).toHaveBeenCalled();
    });
  });

  describe('openCreateDialog', () => {
    it('should open create dialog without parent', () => {
      const dialogRef = {
        afterClosed: () => of(true),
      };
      dialogSpy.open.and.returnValue(dialogRef as any);

      component.openCreateDialog();
      expect(dialogSpy.open).toHaveBeenCalledWith(MenuFormDialogComponent, {
        width: '600px',
        maxWidth: '90vw',
        data: {
          menu: null,
          parentMenu: null,
          isEdit: false,
        },
      });
    });

    it('should open create dialog with parent menu', () => {
      const parentMenu = mockMenus[0];
      const dialogRef = {
        afterClosed: () => of(true),
      };
      dialogSpy.open.and.returnValue(dialogRef as any);
      spyOn(component, 'loadMenus');

      component.openCreateDialog(parentMenu);
      expect(dialogSpy.open).toHaveBeenCalledWith(MenuFormDialogComponent, {
        width: '600px',
        maxWidth: '90vw',
        data: {
          menu: null,
          parentMenu: parentMenu,
          isEdit: false,
        },
      });
    });

    it('should reload menus when dialog returns true', () => {
      const dialogRef = {
        afterClosed: () => of(true),
      };
      dialogSpy.open.and.returnValue(dialogRef as any);
      spyOn(component, 'loadMenus');

      component.openCreateDialog();
      expect(component.loadMenus).toHaveBeenCalled();
      expect(toastSpy.success).toHaveBeenCalledWith('Menú creado exitosamente');
    });

    it('should not reload menus when dialog returns false', () => {
      const dialogRef = {
        afterClosed: () => of(false),
      };
      dialogSpy.open.and.returnValue(dialogRef as any);
      spyOn(component, 'loadMenus');

      component.openCreateDialog();
      expect(component.loadMenus).not.toHaveBeenCalled();
    });
  });

  describe('openEditDialog', () => {
    it('should open edit dialog with menu data', () => {
      const menu = mockMenus[0];
      const dialogRef = {
        afterClosed: () => of(true),
      };
      dialogSpy.open.and.returnValue(dialogRef as any);
      spyOn(component, 'loadMenus');

      component.openEditDialog(menu);
      expect(dialogSpy.open).toHaveBeenCalledWith(MenuFormDialogComponent, {
        width: '600px',
        maxWidth: '90vw',
        data: {
          menu: menu,
          parentMenu: null,
          isEdit: true,
        },
      });
    });

    it('should reload menus when dialog returns true', () => {
      const menu = mockMenus[0];
      const dialogRef = {
        afterClosed: () => of(true),
      };
      dialogSpy.open.and.returnValue(dialogRef as any);
      spyOn(component, 'loadMenus');

      component.openEditDialog(menu);
      expect(component.loadMenus).toHaveBeenCalled();
      expect(toastSpy.success).toHaveBeenCalledWith(
        'Menú actualizado exitosamente'
      );
    });
  });

  describe('deleteMenu', () => {
    beforeEach(() => {
      component.ngOnInit();
    });

    it('should not delete menu with active submenus', () => {
      const menu = mockMenus[1]; // Tiene un submenú activo
      component.deleteMenu(menu);
      expect(toastSpy.warning).toHaveBeenCalled();
      expect(menuServiceSpy.deleteMenu).not.toHaveBeenCalled();
    });

    it('should open confirmation dialog', () => {
      const menu = mockMenus[0]; // No tiene submenús
      const dialogRef = {
        afterClosed: () => of(false),
      };
      dialogSpy.open.and.returnValue(dialogRef as any);

      component.deleteMenu(menu);
      expect(dialogSpy.open).toHaveBeenCalledWith(ConfirmDialogComponent, {
        width: '400px',
        data: {
          title: 'Eliminar Menú Permanentemente',
          message: jasmine.stringContaining(menu.name),
          confirmText: 'Eliminar Permanentemente',
          cancelText: 'Cancelar',
        },
      });
    });

    it('should delete menu when confirmed', () => {
      const menu = mockMenus[0];
      const dialogRef = {
        afterClosed: () => of(true),
      };
      dialogSpy.open.and.returnValue(dialogRef as any);
      menuServiceSpy.deleteMenu.and.returnValue(of({} as any));
      spyOn(component, 'loadMenus');

      component.deleteMenu(menu);
      expect(menuServiceSpy.deleteMenu).toHaveBeenCalledWith(menu.id);
      expect(component.loadMenus).toHaveBeenCalled();
      expect(toastSpy.success).toHaveBeenCalledWith(
        'Menú eliminado permanentemente'
      );
    });

    it('should handle error when deleting menu', () => {
      const menu = mockMenus[0];
      const dialogRef = {
        afterClosed: () => of(true),
      };
      dialogSpy.open.and.returnValue(dialogRef as any);
      const error = { message: 'Failed to delete' };
      menuServiceSpy.deleteMenu.and.returnValue(throwError(() => error));

      component.deleteMenu(menu);
      expect(loggerSpy.error).toHaveBeenCalled();
      expect(toastSpy.error).toHaveBeenCalled();
    });

    it('should not delete when cancelled', () => {
      const menu = mockMenus[0];
      const dialogRef = {
        afterClosed: () => of(false),
      };
      dialogSpy.open.and.returnValue(dialogRef as any);

      component.deleteMenu(menu);
      expect(menuServiceSpy.deleteMenu).not.toHaveBeenCalled();
    });
  });

  describe('toggleActive', () => {
    beforeEach(() => {
      component.ngOnInit();
    });

    it('should toggle menu active state', () => {
      const menu = { ...mockMenus[0], level: 0 };
      const newActiveState = !menu.active;
      menuServiceSpy.updateMenu.and.returnValue(of({} as any));
      const initialDataSourceLength = component.dataSource.data.length;

      component.toggleActive(menu);
      expect(menuServiceSpy.updateMenu).toHaveBeenCalledWith(menu.id, {
        active: newActiveState,
      });
      // Verificar que el dataSource se actualizó indirectamente
      expect(component.dataSource.data.length).toBe(initialDataSourceLength);
      expect(toastSpy.success).toHaveBeenCalled();
    });

    it('should handle error when toggling active state', () => {
      const menu = { ...mockMenus[0], level: 0 };
      const error = new Error('Failed to update');
      menuServiceSpy.updateMenu.and.returnValue(throwError(() => error));

      component.toggleActive(menu);
      expect(loggerSpy.error).toHaveBeenCalled();
      expect(toastSpy.httpError).toHaveBeenCalled();
    });
  });

  describe('getParentName', () => {
    beforeEach(() => {
      component.ngOnInit();
    });

    it('should return parent name when parentId exists', () => {
      const parentId = mockMenus[1].id;
      const subMenu = mockMenus[1].subMenus[0];
      // Simular que el submenú tiene un parentId
      const result = component.getParentName(parentId);
      expect(result).toBe(mockMenus[1].name);
    });

    it('should return "-" when parentId is null', () => {
      const result = component.getParentName(null);
      expect(result).toBe('-');
    });

    it('should return "-" when parentId is undefined', () => {
      const result = component.getParentName(undefined);
      expect(result).toBe('-');
    });

    it('should return "-" when parent not found', () => {
      const result = component.getParentName(999);
      expect(result).toBe('-');
    });
  });

  describe('getFlattenedMenus', () => {
    beforeEach(() => {
      component.ngOnInit();
    });

    it('should flatten menu tree correctly', () => {
      const flattened = component.getFlattenedMenus();
      expect(flattened.length).toBe(3); // 2 padres + 1 submenú
      expect(flattened[0].level).toBe(0);
      expect(flattened[1].level).toBe(0);
      expect(flattened[2].level).toBe(1); // Submenú
    });

    it('should assign correct level to nested menus', () => {
      const flattened = component.getFlattenedMenus();
      const subMenu = flattened.find((m) => m.id === 3);
      expect(subMenu?.level).toBe(1);
    });

    it('should avoid duplicates', () => {
      // Simular menús duplicados agregándolos manualmente
      component.menus = [
        ...mockMenus,
        { ...mockMenus[0], id: 1 }, // Duplicado
      ];
      const flattened = component.getFlattenedMenus();
      const ids = flattened.map((m) => m.id);
      const uniqueIds = new Set(ids);
      expect(ids.length).toBe(uniqueIds.size);
    });
  });

  describe('updateDataSource', () => {
    it('should update dataSource with flattened menus', () => {
      component.ngOnInit();
      const initialLength = component.dataSource.data.length;
      expect(initialLength).toBe(3);
    });

    it('should connect sort if not already connected', () => {
      component.ngOnInit();
      fixture.detectChanges();
      component.ngAfterViewInit();
      component.dataSource.sort = null as any;
      // Llamar loadMenus que internamente llama a updateDataSource
      component.loadMenus();
      expect(component.dataSource.sort).toBe(component.sort);
    });
  });
});
