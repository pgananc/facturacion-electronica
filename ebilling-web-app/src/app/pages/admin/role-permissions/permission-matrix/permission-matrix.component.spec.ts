import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { PermissionMatrixComponent } from './permission-matrix.component';
import { PermissionAdminService } from '../../../../_service/permission/permission-admin.service';
import { ToastService } from '../../../../_service/notification/toast.service';
import { of, throwError } from 'rxjs';
import {
  Role,
  FeaturePermissionAdmin,
  PermissionMatrixRow,
} from '../../../../_model/permission/permission-admin';

describe('PermissionMatrixComponent', () => {
  let component: PermissionMatrixComponent;
  let fixture: ComponentFixture<PermissionMatrixComponent>;
  let permissionServiceSpy: jasmine.SpyObj<PermissionAdminService>;
  let toastSpy: jasmine.SpyObj<ToastService>;

  const mockRoles: Role[] = [
    { id: 1, name: 'Admin', description: 'Administrator' },
    { id: 2, name: 'Member', description: 'Member' },
  ];

  const mockFeatures: FeaturePermissionAdmin[] = [
    {
      featureCode: 'INVOICE_CREATE',
      name: 'Crear Facturas',
      module: 'INVOICING',
      allowedRoles: [1],
      requiredPlans: ['BASIC', 'PRO'],
      description: 'Permite generar facturas electrónicas',
    },
  ];

  beforeEach(async () => {
    const permissionService = jasmine.createSpyObj('PermissionAdminService', [
      'getAllRoles',
      'getAllFeatures',
      'bulkUpdatePermissions',
    ]);
    const toast = jasmine.createSpyObj('ToastService', [
      'error',
      'success',
      'warning',
    ]);

    await TestBed.configureTestingModule({
      declarations: [PermissionMatrixComponent],
      imports: [
        CommonModule,
        HttpClientTestingModule,
        BrowserAnimationsModule,
        MatTableModule,
        MatCheckboxModule,
        MatFormFieldModule,
        MatSelectModule,
        MatButtonModule,
        MatIconModule,
        MatCardModule,
        MatProgressSpinnerModule,
        MatTooltipModule,
      ],
      providers: [
        { provide: PermissionAdminService, useValue: permissionService },
        { provide: ToastService, useValue: toast },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PermissionMatrixComponent);
    component = fixture.componentInstance;
    permissionServiceSpy = TestBed.inject(
      PermissionAdminService
    ) as jasmine.SpyObj<PermissionAdminService>;
    toastSpy = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
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

  it('should load data on init', async () => {
    permissionServiceSpy.getAllRoles.and.returnValue(of(mockRoles));
    permissionServiceSpy.getAllFeatures.and.returnValue(of(mockFeatures));

    fixture.detectChanges();
    await fixture.whenStable();

    expect(permissionServiceSpy.getAllRoles).toHaveBeenCalled();
    expect(permissionServiceSpy.getAllFeatures).toHaveBeenCalled();
    expect(component.roles).toEqual(mockRoles);
    expect(component.features).toEqual(mockFeatures);
  });

  it('should not load data when no token exists', () => {
    localStorage.removeItem('token');

    fixture.detectChanges();

    expect(permissionServiceSpy.getAllRoles).not.toHaveBeenCalled();
    expect(permissionServiceSpy.getAllFeatures).not.toHaveBeenCalled();
    expect(component.isLoading).toBe(false);
  });

  it('should handle error when loading data', async () => {
    permissionServiceSpy.getAllRoles.and.returnValue(
      throwError(() => new Error('Network error'))
    );
    permissionServiceSpy.getAllFeatures.and.returnValue(
      throwError(() => new Error('Network error'))
    );

    fixture.detectChanges();
    await fixture.whenStable();

    expect(toastSpy.error).toHaveBeenCalledWith(
      'Error al cargar permisos y roles'
    );
    expect(component.isLoading).toBe(false);
  });

  it('should build matrix correctly', () => {
    component.roles = mockRoles;
    component.features = mockFeatures;

    component.buildMatrix();

    expect(component.matrixData.length).toBeGreaterThan(0);
    expect(component.matrixData[0].featureCode).toBe('INVOICE_CREATE');
    expect(component.matrixData[0].permissions[1]).toBe(true);
    expect(component.matrixData[0].permissions[2]).toBe(false);
  });

  it('should apply filter by module', () => {
    component.matrixData = [
      {
        featureCode: 'INVOICE_CREATE',
        name: 'Crear Facturas',
        module: 'INVOICING',
        requiredPlans: [],
        description: 'Permite generar facturas electrónicas',
        permissions: {},
      },
      {
        featureCode: 'CLIENT_CREATE',
        name: 'Crear Clientes',
        module: 'CLIENTS',
        requiredPlans: [],
        description: 'Permite crear y gestionar clientes',
        permissions: {},
      },
    ];
    component.selectedModule = 'INVOICING';

    component.applyFilter();

    expect(component.dataSource.data.length).toBe(1);
    expect(component.dataSource.data[0].module).toBe('INVOICING');
  });

  it('should toggle permission', () => {
    component.roles = mockRoles;
    component.canEdit = true; // Enable editing for this test
    const row: PermissionMatrixRow = {
      featureCode: 'INVOICE_CREATE',
      name: 'Crear Facturas',
      module: 'INVOICING',
      requiredPlans: [],
      description: 'Permite generar facturas electrónicas',
      permissions: { 1: true, 2: false },
    };

    component.togglePermission(row, 2);

    expect(row.permissions[2]).toBe(true);
    expect(component.hasChanges).toBe(true);
  });

  it('should save changes', () => {
    // Build matrix first to have data
    component.roles = mockRoles;
    component.features = mockFeatures;
    component.canEdit = true; // Enable editing for this test
    component.buildMatrix();

    // Toggle a permission to create changes
    if (component.matrixData.length > 0) {
      component.togglePermission(component.matrixData[0], 2);
    }

    permissionServiceSpy.bulkUpdatePermissions.and.returnValue(
      of(mockFeatures)
    );
    permissionServiceSpy.getAllRoles.and.returnValue(of(mockRoles));
    permissionServiceSpy.getAllFeatures.and.returnValue(of(mockFeatures));

    component.saveChanges();

    expect(permissionServiceSpy.bulkUpdatePermissions).toHaveBeenCalled();
    expect(component.hasChanges).toBe(false);
  });

  it('should not save when no changes', () => {
    component.hasChanges = false;

    component.saveChanges();

    expect(permissionServiceSpy.bulkUpdatePermissions).not.toHaveBeenCalled();
  });

  it('should discard changes', () => {
    // Build matrix first to have data
    component.roles = mockRoles;
    component.features = mockFeatures;
    component.buildMatrix();

    // Toggle a permission to create changes
    if (component.matrixData.length > 0) {
      component.togglePermission(component.matrixData[0], 2);
    }

    spyOn(window, 'confirm').and.returnValue(true);
    permissionServiceSpy.getAllRoles.and.returnValue(of(mockRoles));
    permissionServiceSpy.getAllFeatures.and.returnValue(of(mockFeatures));

    component.discardChanges();

    expect(component.hasChanges).toBe(false);
  });

  it('should get module icon', () => {
    expect(component.getModuleIcon('INVOICING')).toBe('receipt');
    expect(component.getModuleIcon('CLIENTS')).toBe('people');
    expect(component.getModuleIcon('UNKNOWN')).toBe('folder');
  });

  it('should get module display name', () => {
    expect(component.getModuleDisplayName('INVOICING')).toBe('Facturación');
    expect(component.getModuleDisplayName('CLIENTS')).toBe('Clientes');
  });

  it('should get module color', () => {
    expect(component.getModuleColor('INVOICING')).toBe('#2196f3');
    expect(component.getModuleColor('CLIENTS')).toBe('#4caf50');
  });

  it('should get plan display name', () => {
    expect(component.getPlanDisplayName('FREE')).toBe('Gratuito');
    expect(component.getPlanDisplayName('BASIC')).toBe('Básico');
  });

  it('should get role badge class', () => {
    expect(component.getRoleBadgeClass(1)).toBe('role-super-admin');
    expect(component.getRoleBadgeClass(2)).toBe('role-member');
  });
});
