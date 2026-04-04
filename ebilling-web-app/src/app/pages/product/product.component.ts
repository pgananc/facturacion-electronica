import {
  Component,
  OnInit,
  OnDestroy,
  ViewChild,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
} from '@angular/core';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { MatDialog } from '@angular/material/dialog';
import {
  DELETE,
  EMPTY_DATA,
  PRODUCT_TYPE,
} from '../../_constants/constants';
import { ToastService } from '../../_service/notification/toast.service';
import { FormControl, FormGroup } from '@angular/forms';
import { ProductService } from '../../_service/product/product.service';
import { environment } from 'src/environments/environment';
import { ProductDto } from '../../_model/product/product-dto';
import { UpdateProductComponent } from './update-product/update-product.component';
import { ConfirmDialogComponent } from '../../_shared/confirm-dialog/confirm-dialog.component';
import { LoggerService } from '../../_service/logger/logger.service';
import { ProductCacheService } from '../../_service/product/product-cache.service';
import { Subject, takeUntil, catchError, of } from 'rxjs';

/**
 * ProductComponent - Lista y gestión de productos
 *
 * Optimizado con OnPush Change Detection para mejor performance
 * Implementa OnDestroy para cleanup de subscripciones
 */
@Component({
  selector: 'app-product',
  templateUrl: './product.component.html',
  styleUrls: ['./product.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProductComponent implements OnInit, OnDestroy {
  form: FormGroup;
  productTypes = PRODUCT_TYPE;
  displayedColumns = [
    'mainCode',
    'name',
    'description',
    'unitPrice',
    'ivaTaxType', // ✅ Nuevo: IVA
    'iceTaxType', // ✅ Nuevo: ICE
    'status',
    'actions',
  ];
  dataSource = new MatTableDataSource<ProductDto>([]);
  quantity: number = 0;
  pageSize: number = 5; // Tamaño de página por defecto (debe coincidir con el HTML)

  private destroy$ = new Subject<void>();

  @ViewChild(MatPaginator, { static: true }) paginator: MatPaginator;
  @ViewChild(MatSort, { static: true }) sort: MatSort;

  constructor(
    private readonly productService: ProductService,
    private readonly productCacheService: ProductCacheService,
    private readonly toast: ToastService,
    private readonly dialog: MatDialog,
    private readonly logger: LoggerService,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    // Inicializar formulario con todos los campos incluidos en el FormGroup
    // status: null = Todos (activos e inactivos)
    this.form = new FormGroup({
      mainCode: new FormControl(''),
      name: new FormControl(''),
      productType: new FormControl(0),
      status: new FormControl(null), // null = Todos
    });

    // Inicializar dataSource vacío
    this.dataSource = new MatTableDataSource<ProductDto>([]);
    this.dataSource.sort = this.sort!;
    // ✅ NO asignar paginator porque usamos paginación del servidor (Server-Side Pagination)
    // this.dataSource.paginator = this.paginator!;

    // ✅ Configurar ordenamiento por defecto: mainCode ascendente
    // setTimeout para evitar ExpressionChangedAfterItHasBeenCheckedError
    setTimeout(() => {
      if (this.sort) {
        this.sort.active = 'mainCode';
        this.sort.direction = 'asc';
      }
    });

    // Subscribirse a cambios de productos con cleanup automático
    this.productService.productChange
      .pipe(takeUntil(this.destroy$))
      .subscribe((data) => {
        this.dataSource.data = data;
        this.cdr.markForCheck(); // Notificar cambios para OnPush
      });

    // Cargar datos iniciales
    this.search();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Abre el modal para crear o editar un producto
   */
  openDialog(product?: ProductDto): void {
    const dialogRef = this.dialog.open(UpdateProductComponent, {
      width: '650px',
      maxWidth: '90vw',
      maxHeight: '90vh',
      disableClose: false,
      autoFocus: false,
      data: product ? { product, isEdit: true } : { isEdit: false },
    });

    dialogRef
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((result) => {
        if (result) {
          this.logger.info('Modal cerrado con resultado:', result);
          // Recargar la lista manteniendo la página actual, pero ajustando si es necesario
          this.reloadCurrentPage();

          // Refrescar el cache de productos para que esté actualizado en facturación
          this.logger.debug(
            '🔄 Refrescando cache de productos después de crear/editar'
          );
          this.productCacheService.refreshCache().subscribe({
            next: (products) => {
              this.logger.debug(
                `✅ Cache actualizado: ${products.length} productos`
              );
            },
            error: (err) => {
              this.logger.error(
                '❌ Error al refrescar cache de productos',
                err
              );
            },
          });
        }
      });
  }

  /**
   * Elimina PERMANENTEMENTE un producto con confirmación previa
   * Esta acción es irreversible (Hard Delete)
   */
  delete(product: ProductDto): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '500px',
      maxWidth: '90vw',
      data: {
        title: '⚠️ Eliminación Permanente',
        message: `¿Está seguro de que desea eliminar PERMANENTEMENTE el producto "${product.name}" (${product.mainCode})?\n\nEsta acción NO se puede deshacer.`,
        confirmText: 'Confirmar',
        cancelText: 'Cancelar',
        type: 'danger',
      },
    });

    dialogRef
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((confirmed) => {
        if (confirmed && product.idProduct) {
          this.productService
            .deletePhysical(product.idProduct)
            .pipe(
              takeUntil(this.destroy$),
              catchError((error) => {
                this.logger.error(
                  'Error al eliminar el producto permanentemente',
                  error
                );
                this.toast.error('Error al eliminar el producto');
                return of(null);
              })
            )
            .subscribe(() => {
              const productDtos = this.dataSource.data.filter(
                (p) => p.idProduct !== product.idProduct
              );
              this.productService.productChange.next(productDtos);
              this.toast.success(
                `Producto "${product.name}" eliminado permanentemente`,
              );
              this.cdr.markForCheck();

              // Refrescar el cache de productos después de eliminar
              this.logger.debug(
                '🔄 Refrescando cache de productos después de eliminar'
              );
              this.productCacheService.refreshCache().subscribe({
                next: (products) => {
                  this.logger.debug(
                    `✅ Cache actualizado: ${products.length} productos`
                  );
                },
                error: (err) => {
                  this.logger.error(
                    '❌ Error al refrescar cache de productos',
                    err
                  );
                },
              });
            });
        }
      });
  }

  /**
   * Busca productos según los filtros del formulario
   */
  search(): void {
    // Reset to first page when searching
    if (this.paginator) {
      this.paginator.pageIndex = 0;
    }

    const idCompany = Number(localStorage.getItem(environment.ID_COMPANY)!);

    if (!idCompany) {
      this.logger.error('No se encontró ID de compañía en localStorage');
      this.toast.error('No se encontró información de la compañía');
      return;
    }

    const filters = {
      idCompany,
      mainCode: this.form.value['mainCode'] || undefined,
      name: this.form.value['name'] || undefined,
      productType: this.form.value['productType'] || undefined,
      status: this.form.value['status'],
    };

    const currentPageSize = this.paginator?.pageSize || this.pageSize;

    this.productService
      .searchPageable(0, currentPageSize, filters)
      .pipe(
        takeUntil(this.destroy$),
        catchError((error) => {
          this.logger.error('Error al buscar productos', error);
          this.toast.error('Error al buscar productos');
          return of(null);
        })
      )
      .subscribe((data) => {
        if (data != null) {
          this.quantity = data.totalElements;
          this.dataSource.data = data.content;
          // Sincronizar paginator
          if (this.paginator) {
            this.paginator.pageIndex = 0;
            this.paginator.length = data.totalElements;
            this.paginator.pageSize = currentPageSize;
          }
          this.cdr.markForCheck();
        } else {
          this.toast.info(EMPTY_DATA.MESSAGE_EMPTY_DATA.message);
        }
      });
  }

  /**
   * Recarga la página actual, ajustando la paginación si la página queda vacía
   */
  reloadCurrentPage(): void {
    const idCompany = Number(localStorage.getItem(environment.ID_COMPANY)!);

    if (!idCompany) {
      this.logger.error('No se encontró ID de compañía en localStorage');
      return;
    }

    const filters = {
      idCompany,
      mainCode: this.form.value['mainCode'] || undefined,
      name: this.form.value['name'] || undefined,
      productType: this.form.value['productType'] || undefined,
      status: this.form.value['status'],
    };

    const currentPageIndex = this.paginator?.pageIndex || 0;
    const currentPageSize = this.paginator?.pageSize || this.pageSize;

    this.productService
      .searchPageable(currentPageIndex, currentPageSize, filters)
      .pipe(
        takeUntil(this.destroy$),
        catchError((error) => {
          this.logger.error('Error al recargar productos', error);
          return of(null);
        })
      )
      .subscribe((data) => {
        if (data != null) {
          this.quantity = data.totalElements;

          // Si la página actual está vacía y no es la primera página, volver a la anterior
          if (data.content.length === 0 && currentPageIndex > 0) {
            const newPageIndex = Math.max(0, currentPageIndex - 1);
            if (this.paginator) {
              this.paginator.pageIndex = newPageIndex;
            }
            // Recargar con la nueva página
            this.productService
              .searchPageable(newPageIndex, currentPageSize, filters)
              .pipe(takeUntil(this.destroy$))
              .subscribe((newData) => {
                if (newData) {
                  this.quantity = newData.totalElements;
                  this.dataSource.data = newData.content;
                  if (this.paginator) {
                    this.paginator.length = newData.totalElements;
                    this.paginator.pageSize = currentPageSize;
                  }
                  this.cdr.markForCheck();
                }
              });
          } else {
            // La página tiene datos, actualizar normalmente
            this.dataSource.data = data.content;
            if (this.paginator) {
              this.paginator.length = data.totalElements;
              this.paginator.pageSize = currentPageSize;
            }
            this.cdr.markForCheck();
          }
        }
      });
  }

  /**
   * Limpia el formulario de búsqueda y recarga la lista
   */
  limpiar(): void {
    this.form.patchValue({
      mainCode: '',
      name: '',
      productType: 0,
      status: null, // null = Todos (activos e inactivos)
    });
    this.search();
  }

  /**
   * Maneja la paginación de la tabla
   */
  showMore(e: any): void {
    const idCompany = Number(localStorage.getItem(environment.ID_COMPANY)!);

    const filters = {
      idCompany,
      mainCode: this.form.value['mainCode'] || undefined,
      name: this.form.value['name'] || undefined,
      productType: this.form.value['productType'] || undefined,
      status: this.form.value['status'],
    };

    this.pageSize = e.pageSize; // Actualizar el tamaño de página

    this.productService
      .searchPageable(e.pageIndex, e.pageSize, filters)
      .pipe(
        takeUntil(this.destroy$),
        catchError((error) => {
          this.logger.error('Error al paginar productos', error);
          return of(null);
        })
      )
      .subscribe((data) => {
        if (data) {
          this.quantity = data.totalElements;
          this.dataSource.data = data.content;
          // Sincronizar paginator
          if (this.paginator) {
            this.paginator.length = data.totalElements;
            this.paginator.pageSize = e.pageSize;
            this.paginator.pageIndex = e.pageIndex;
          }
          this.cdr.markForCheck();
        }
      });
  }
}
