import {
  Component,
  OnInit,
  OnDestroy,
  ViewChild,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Injectable,
} from '@angular/core';
import { MatPaginator, MatPaginatorIntl } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { CarrierDto } from '../../_model/guide/carrier-dto';
import { CarrierService } from '../../_service/guide/carrier.service';
import { FormControl, FormGroup } from '@angular/forms';
import { environment } from 'src/environments/environment';
import { Subject } from 'rxjs';
import { takeUntil, catchError } from 'rxjs/operators';
import { of } from 'rxjs';
import { MatDialog } from '@angular/material/dialog';
import { UpdateCarrierComponent } from './update-carrier/update-carrier.component';
import { LoggerService } from '../../_service/logger/logger.service';
import { ToastService } from '../../_service/notification/toast.service';
import { ConfirmDialogComponent } from '../../_shared/confirm-dialog/confirm-dialog.component';

/**
 * Custom Paginator Intl para personalizar el texto
 */
@Injectable()
export class CustomCarrierPaginatorIntl extends MatPaginatorIntl {
  override itemsPerPageLabel = 'Items por página:';
  override nextPageLabel = 'Siguiente';
  override previousPageLabel = 'Anterior';
  override firstPageLabel = 'Primera página';
  override lastPageLabel = 'Última página';

  override getRangeLabel = (
    page: number,
    pageSize: number,
    length: number
  ): string => {
    if (length === 0 || pageSize === 0) {
      return `0 de ${length} transportistas`;
    }
    const startIndex = page * pageSize;
    const endIndex =
      startIndex < length
        ? Math.min(startIndex + pageSize, length)
        : startIndex + pageSize;
    return `Mostrando ${startIndex + 1}-${endIndex} de ${length} transportistas`;
  };
}

/**
 * CarrierComponent - Lista y gestión de transportistas
 *
 * Optimizado con OnPush Change Detection para mejor performance
 */
@Component({
  selector: 'app-carrier',
  templateUrl: './carrier.component.html',
  styleUrls: ['./carrier.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    { provide: MatPaginatorIntl, useClass: CustomCarrierPaginatorIntl },
  ],
})
export class CarrierComponent implements OnInit, OnDestroy {
  form: FormGroup;
  displayedColumns = [
    'identification',
    'name',
    'address',
    'phone',
    'email',
    'status',
    'actions',
  ];
  dataSourceCarriers = new MatTableDataSource<CarrierDto>([]);
  quantity: number = 0;
  pageSize: number = 10;
  isLoading: boolean = false;

  // Server-side sorting state
  currentSortField: string = '';
  currentSortDirection: string = '';

  private destroy$ = new Subject<void>();

  @ViewChild(MatPaginator, { static: true }) paginator: MatPaginator;
  @ViewChild(MatSort, { static: true }) sort: MatSort;

  constructor(
    private readonly carrierService: CarrierService,
    private readonly cdr: ChangeDetectorRef,
    private readonly dialog: MatDialog,
    private readonly logger: LoggerService,
    private readonly toast: ToastService
  ) {}

  ngOnInit() {
    this.form = new FormGroup({
      identification: new FormControl(''),
      name: new FormControl(''),
      status: new FormControl(true),
    });

    // Subscribe to sort changes for server-side sorting
    this.sort.sortChange.pipe(takeUntil(this.destroy$)).subscribe(() => {
      // Reset to first page when sorting
      this.paginator.pageIndex = 0;
      this.onSortChange();
    });

    // Verificar si hay token antes de cargar datos
    const token = localStorage.getItem('token');
    if (!token) {
      this.logger.warn(
        '[CarrierComponent] No hay token válido, omitiendo carga de datos'
      );
      return;
    }

    // Initial load with error handling
    this.loadCarriers();
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onSortChange() {
    const fieldMapping: { [key: string]: string } = {
      identification: 'identification',
      name: 'name',
      status: 'status',
    };

    // Update current sort state
    this.currentSortField =
      fieldMapping[this.sort.active] || this.sort.active;
    this.currentSortDirection = this.sort.direction;

    // Reload data with new sort
    this.search();
  }

  delete(idCarrier: number) {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '500px',
      maxWidth: '90vw',
      data: {
        title: 'Confirmar eliminación',
        message:
          '¿Está seguro de que desea eliminar este transportista? Esta acción no se puede deshacer.',
        confirmText: 'Eliminar',
        cancelText: 'Cancelar',
        type: 'danger',
      },
    });

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.carrierService
          .deleteCarrier(idCarrier)
          .pipe(
            takeUntil(this.destroy$),
            catchError((error) => {
              this.logger.error('Error deleting carrier', error);
              this.toast.httpError(error, 'eliminar transportista', 'Error al eliminar transportista. Por favor intente nuevamente.');
              return of(null);
            })
          )
          .subscribe((response) => {
            if (response !== null || response === undefined) {
              // Refrescar desde el servidor
              this.search();

              // Mostrar toast de confirmación exitosa
              this.toast.success('Transportista eliminado exitosamente');
            }
          });
      }
    });
  }

  search() {
    // Reset to first page when searching
    this.paginator.pageIndex = 0;
    this.loadCarriers();
  }

  loadCarriers() {
    this.isLoading = true;
    this.cdr.markForCheck();

    const idCompany = Number(localStorage.getItem(environment.ID_COMPANY));
    if (!idCompany) {
      this.logger.error('No se encontró ID de compañía');
      this.isLoading = false;
      this.cdr.markForCheck();
      return;
    }

    const formValue = this.form.value;
    const sortStr = this.currentSortField
      ? `${this.currentSortField},${this.currentSortDirection || 'asc'}`
      : 'name,asc';

    // Buscar transportistas activos por defecto, o todos si status es false
    const activeOnly = formValue.status !== false;

    this.carrierService
      .getCarriers(
        idCompany,
        activeOnly,
        this.paginator.pageIndex,
        this.paginator.pageSize || this.pageSize,
        sortStr
      )
      .pipe(
        takeUntil(this.destroy$),
        catchError((error) => {
          this.logger.error('Error loading carriers', error);
          this.isLoading = false;
          this.cdr.markForCheck();
          // Usar ToastService para manejar el error de forma consistente
          this.toast.httpError(error, 'cargar transportistas', 'Error al cargar transportistas. Por favor intente nuevamente.');
          return of({ content: [], totalElements: 0 } as any);
        })
      )
      .subscribe((page) => {
        this.isLoading = false;
        if (page && page.content) {
          // Filtrar por identificación y nombre si se proporcionan
          let filtered: CarrierDto[] = page.content;
          if (formValue.identification) {
            filtered = filtered.filter((c: CarrierDto) =>
              c.identification
                .toLowerCase()
                .includes(formValue.identification.toLowerCase())
            );
          }
          if (formValue.name) {
            filtered = filtered.filter((c: CarrierDto) =>
              c.name.toLowerCase().includes(formValue.name.toLowerCase())
            );
          }

          this.quantity = filtered.length;
          this.dataSourceCarriers = new MatTableDataSource(filtered);
          this.dataSourceCarriers.sort = this.sort;
          this.cdr.markForCheck();
        } else {
          this.dataSourceCarriers.data = [];
          this.quantity = 0;
          this.cdr.markForCheck();
        }
      });
  }

  limpiar() {
    this.form.reset({
      identification: '',
      name: '',
      status: true,
    });
    this.cdr.markForCheck();
    // Recargar la lista con filtros limpios
    this.loadCarriers();
  }

  showMore(e: any) {
    this.pageSize = e.pageSize;
    this.loadCarriers();
  }

  get hasCarrierData(): boolean {
    return (
      Array.isArray(this.dataSourceCarriers?.data) &&
      this.dataSourceCarriers.data.length > 0
    );
  }

  openCreateDialog(): void {
    const dialogRef = this.dialog.open(UpdateCarrierComponent, {
      width: '600px',
      maxWidth: '95vw',
      data: {
        mode: 'create',
      },
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.loadCarriers();
      }
    });
  }

  openEditDialog(idCarrier: number): void {
    const dialogRef = this.dialog.open(UpdateCarrierComponent, {
      width: '600px',
      maxWidth: '95vw',
      data: {
        mode: 'edit',
        idCarrier: idCarrier,
      },
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.loadCarriers();
      }
    });
  }

  getStatusLabel(status: boolean | undefined): string {
    return status ? 'Activo' : 'Inactivo';
  }

  getStatusClass(status: boolean | undefined): string {
    return status ? 'status-active' : 'status-inactive';
  }
}

