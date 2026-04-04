import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ChangeDetectorRef, NO_ERRORS_SCHEMA } from '@angular/core';
import { of, throwError, Subject } from 'rxjs';
import { ProductComponent } from './product.component';
import { ProductService } from '../../_service/product/product.service';
import { ProductCacheService } from '../../_service/product/product-cache.service';
import { LoggerService } from '../../_service/logger/logger.service';
import { ProductDto } from '../../_model/product/product-dto';
import { TaxTypeDto } from '../../_model/product/tax-type-dto';
import { UpdateProductComponent } from './update-product/update-product.component';
import { ConfirmDialogComponent } from '../../_shared/confirm-dialog/confirm-dialog.component';
import { environment } from '../../../environments/environment';

describe('ProductComponent', () => {
  let component: ProductComponent;
  let fixture: ComponentFixture<ProductComponent>;
  let productServiceSpy: jasmine.SpyObj<ProductService>;
  let productCacheServiceSpy: jasmine.SpyObj<ProductCacheService>;
  let loggerSpy: jasmine.SpyObj<LoggerService>;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;
  let dialogSpy: jasmine.SpyObj<MatDialog>;
  let cdrSpy: jasmine.SpyObj<ChangeDetectorRef>;

  const mockTaxType: TaxTypeDto = {
    idTaxType: 1,
    type: '2',
    name: 'IVA',
    value: 0,
  };

  const mockProducts: ProductDto[] = [
    {
      idProduct: 1,
      mainCode: 'PROD001',
      name: 'Producto Test 1',
      unitPrice: 10.0,
      tourismIva: false,
      ice: false,
      ivaTaxType: mockTaxType,
      status: true,
    },
    {
      idProduct: 2,
      mainCode: 'PROD002',
      name: 'Producto Test 2',
      unitPrice: 20.0,
      tourismIva: false,
      ice: false,
      ivaTaxType: mockTaxType,
      status: true,
    },
  ];

  const mockPageResponse = {
    content: mockProducts,
    totalElements: 2,
    totalPages: 1,
    size: 5,
    number: 0,
  };

  beforeEach(async () => {
    const productService = jasmine.createSpyObj('ProductService', [
      'searchPageable',
      'deletePhysical',
      'productChange',
    ]);
    const productCacheService = jasmine.createSpyObj('ProductCacheService', [
      'refreshCache',
    ]);
    const logger = jasmine.createSpyObj('LoggerService', [
      'debug',
      'info',
      'warn',
      'error',
    ]);
    const snackBar = jasmine.createSpyObj('MatSnackBar', ['open']);
    const dialog = jasmine.createSpyObj('MatDialog', ['open']);
    const cdr = jasmine.createSpyObj('ChangeDetectorRef', ['markForCheck']);

    // Setup productChange as Subject
    productService.productChange = new Subject<ProductDto[]>();

    await TestBed.configureTestingModule({
      declarations: [ProductComponent],
      imports: [
        ReactiveFormsModule,
        MatTableModule,
        MatPaginatorModule,
        MatSortModule,
        MatSnackBarModule,
        MatDialogModule,
        MatSelectModule,
        MatFormFieldModule,
        MatInputModule,
        BrowserAnimationsModule,
      ],
      schemas: [NO_ERRORS_SCHEMA], // Ignorar componentes hijos en el template
      providers: [
        { provide: ProductService, useValue: productService },
        { provide: ProductCacheService, useValue: productCacheService },
        { provide: LoggerService, useValue: logger },
        { provide: MatSnackBar, useValue: snackBar },
        { provide: MatDialog, useValue: dialog },
        { provide: ChangeDetectorRef, useValue: cdr },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProductComponent);
    component = fixture.componentInstance;
    productServiceSpy = TestBed.inject(
      ProductService
    ) as jasmine.SpyObj<ProductService>;
    productCacheServiceSpy = TestBed.inject(
      ProductCacheService
    ) as jasmine.SpyObj<ProductCacheService>;
    loggerSpy = TestBed.inject(LoggerService) as jasmine.SpyObj<LoggerService>;
    snackBarSpy = TestBed.inject(MatSnackBar) as jasmine.SpyObj<MatSnackBar>;
    dialogSpy = TestBed.inject(MatDialog) as jasmine.SpyObj<MatDialog>;
    cdrSpy = TestBed.inject(
      ChangeDetectorRef
    ) as jasmine.SpyObj<ChangeDetectorRef>;

    // Setup default mocks
    localStorage.setItem(environment.ID_COMPANY, '1');
    productServiceSpy.searchPageable.and.returnValue(of(mockPageResponse));
    productCacheServiceSpy.refreshCache.and.returnValue(of(mockProducts));
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
      expect(component.form.get('mainCode')?.value).toBe('');
      expect(component.form.get('name')?.value).toBe('');
      expect(component.form.get('productType')?.value).toBe(0);
      expect(component.form.get('status')?.value).toBe(null);
    });

    it('should initialize dataSource', () => {
      component.ngOnInit();
      expect(component.dataSource).toBeDefined();
    });

    it('should call search on init', () => {
      spyOn(component, 'search');
      component.ngOnInit();
      expect(component.search).toHaveBeenCalled();
    });

    it('should subscribe to productChange', () => {
      component.ngOnInit();
      const newProducts: ProductDto[] = [
        {
          idProduct: 3,
          mainCode: 'PROD003',
          name: 'New Product',
          unitPrice: 30.0,
          tourismIva: false,
          ice: false,
          ivaTaxType: mockTaxType,
          status: true,
        },
      ];
      (productServiceSpy.productChange as Subject<ProductDto[]>).next(
        newProducts
      );
      expect(component.dataSource.data).toEqual(newProducts);
    });
  });

  describe('search', () => {
    beforeEach(() => {
      component.ngOnInit();
      // No hacer detectChanges para evitar errores de template
    });

    it('should search products with form values', () => {
      component.form.patchValue({
        mainCode: 'PROD',
        name: 'Test',
        productType: 1,
        status: true,
      });
      component.search();
      expect(productServiceSpy.searchPageable).toHaveBeenCalled();
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

    it('should handle error when searching', () => {
      productServiceSpy.searchPageable.and.returnValue(
        throwError(() => new Error('Search error'))
      );
      component.search();
      expect(snackBarSpy.open).toHaveBeenCalled();
      expect(loggerSpy.error).toHaveBeenCalled();
    });

    it('should show error when company ID is missing', () => {
      localStorage.removeItem(environment.ID_COMPANY);
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
      productServiceSpy.searchPageable.and.returnValue(of(null));
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
        mainCode: 'PROD',
        name: 'Test',
        productType: 2,
        status: false,
      });
      component.limpiar();
      expect(component.form.get('mainCode')?.value).toBe('');
      expect(component.form.get('name')?.value).toBe('');
      expect(component.form.get('productType')?.value).toBe(0);
      expect(component.form.get('status')?.value).toBe(null);
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
      // No hacer detectChanges para evitar errores de template
    });

    it('should open confirmation dialog', () => {
      dialogSpy.open.and.returnValue({
        afterClosed: () => of(false),
      } as any);
      component.delete(mockProducts[0]);
      expect(dialogSpy.open).toHaveBeenCalledWith(ConfirmDialogComponent, {
        width: '500px',
        maxWidth: '90vw',
        data: jasmine.objectContaining({
          title: '⚠️ Eliminación Permanente',
          type: 'danger',
        }),
      });
    });

    it('should delete product when confirmed', () => {
      productServiceSpy.deletePhysical.and.returnValue(of({}));
      dialogSpy.open.and.returnValue({
        afterClosed: () => of(true),
      } as any);
      component.dataSource.data = [...mockProducts];
      component.delete(mockProducts[0]);
      expect(productServiceSpy.deletePhysical).toHaveBeenCalledWith(1);
      expect(productCacheServiceSpy.refreshCache).toHaveBeenCalled();
    });

    it('should not delete product when cancelled', () => {
      dialogSpy.open.and.returnValue({
        afterClosed: () => of(false),
      } as any);
      component.delete(mockProducts[0]);
      expect(productServiceSpy.deletePhysical).not.toHaveBeenCalled();
    });

    it('should handle delete error', () => {
      productServiceSpy.deletePhysical.and.returnValue(
        throwError(() => new Error('Delete error'))
      );
      dialogSpy.open.and.returnValue({
        afterClosed: () => of(true),
      } as any);
      component.delete(mockProducts[0]);
      expect(snackBarSpy.open).toHaveBeenCalled();
      expect(loggerSpy.error).toHaveBeenCalled();
    });
  });

  describe('showMore', () => {
    beforeEach(() => {
      component.ngOnInit();
    });

    it('should load more products with pagination', () => {
      const pageEvent = {
        pageIndex: 1,
        pageSize: 5,
      };
      component.showMore(pageEvent);
      expect(productServiceSpy.searchPageable).toHaveBeenCalledWith(
        1,
        5,
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

    it('should handle error when loading more products', () => {
      productServiceSpy.searchPageable.and.returnValue(
        throwError(() => new Error('Load error'))
      );
      const pageEvent = {
        pageIndex: 1,
        pageSize: 5,
      };
      component.showMore(pageEvent);
      expect(loggerSpy.error).toHaveBeenCalled();
    });
  });

  describe('openDialog', () => {
    beforeEach(() => {
      component.ngOnInit();
    });

    it('should open create dialog when no product provided', () => {
      dialogSpy.open.and.returnValue({
        afterClosed: () => of(false),
      } as any);
      component.openDialog();
      expect(dialogSpy.open).toHaveBeenCalledWith(
        UpdateProductComponent,
        jasmine.objectContaining({
          data: { isEdit: false },
        })
      );
    });

    it('should open edit dialog when product provided', () => {
      dialogSpy.open.and.returnValue({
        afterClosed: () => of(false),
      } as any);
      component.openDialog(mockProducts[0]);
      expect(dialogSpy.open).toHaveBeenCalledWith(
        UpdateProductComponent,
        jasmine.objectContaining({
          data: { product: mockProducts[0], isEdit: true },
        })
      );
    });

    it('should reload current page when dialog returns result', () => {
      spyOn(component, 'reloadCurrentPage');
      dialogSpy.open.and.returnValue({
        afterClosed: () => of(true),
      } as any);
      component.openDialog();
      expect(component.reloadCurrentPage).toHaveBeenCalled();
      expect(productCacheServiceSpy.refreshCache).toHaveBeenCalled();
    });

    it('should not reload when dialog returns no result', () => {
      spyOn(component, 'reloadCurrentPage');
      dialogSpy.open.and.returnValue({
        afterClosed: () => of(false),
      } as any);
      component.openDialog();
      expect(component.reloadCurrentPage).not.toHaveBeenCalled();
    });
  });

  describe('reloadCurrentPage', () => {
    beforeEach(() => {
      component.ngOnInit();
    });

    it('should reload current page with same filters', () => {
      productServiceSpy.searchPageable.calls.reset(); // Resetear llamadas de ngOnInit
      component.paginator = {
        pageIndex: 0,
        pageSize: 5,
      } as MatPaginator;
      component.reloadCurrentPage();
      expect(productServiceSpy.searchPageable).toHaveBeenCalled();
    });

    it('should handle error when reloading', () => {
      productServiceSpy.searchPageable.and.returnValue(
        throwError(() => new Error('Reload error'))
      );
      component.paginator = {
        pageIndex: 0,
        pageSize: 5,
      } as MatPaginator;
      component.reloadCurrentPage();
      expect(loggerSpy.error).toHaveBeenCalled();
    });

    it('should go to previous page if current page is empty', () => {
      productServiceSpy.searchPageable.calls.reset(); // Resetear llamadas de ngOnInit
      productServiceSpy.searchPageable.and.returnValues(
        of({ ...mockPageResponse, content: [] }),
        of(mockPageResponse)
      );
      component.paginator = {
        pageIndex: 1,
        pageSize: 5,
      } as MatPaginator;
      component.reloadCurrentPage();
      expect(productServiceSpy.searchPageable).toHaveBeenCalledTimes(2);
    });

    it('should not reload when company ID is missing', () => {
      productServiceSpy.searchPageable.calls.reset(); // Resetear llamadas de ngOnInit
      localStorage.removeItem(environment.ID_COMPANY);
      component.reloadCurrentPage();
      expect(productServiceSpy.searchPageable).not.toHaveBeenCalled();
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
