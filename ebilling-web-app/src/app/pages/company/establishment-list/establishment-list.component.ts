import {
  Component,
  OnInit,
  ViewChild,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
} from '@angular/core';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatDialog } from '@angular/material/dialog';
import { ToastService } from '../../../_service/notification/toast.service';
import { Establishment } from '../../../_model/customer/company-establishment';
import { EstablishmentService } from '../../../_service/customer/establishment.service';
import { IssuancePointService } from '../../../_service/customer/issuance-point.service';
import { UpdateEstablishmentComponent } from '../update-establishment/update-establishment.component';
import { ConfirmDialogComponent } from '../../../_shared/confirm-dialog/confirm-dialog.component';
import { LoggerService } from '../../../_service/logger/logger.service';
import { environment } from '../../../../environments/environment';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-establishment-list',
  templateUrl: './establishment-list.component.html',
  styleUrls: ['./establishment-list.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EstablishmentListComponent implements OnInit {
  displayedColumns: string[] = [
    'code',
    'name',
    'address',
    'type',
    'status',
    'actions',
  ];
  dataSource: MatTableDataSource<Establishment> =
    new MatTableDataSource<Establishment>();
  isLoading = false;
  idCompany: number;

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  // Formulario de búsqueda
  searchCode = '';
  searchName = '';

  constructor(
    private readonly establishmentService: EstablishmentService,
    private readonly issuancePointService: IssuancePointService,
    private readonly dialog: MatDialog,
    private readonly toast: ToastService,
    private readonly logger: LoggerService,
    private readonly cdr: ChangeDetectorRef,
  ) {
    const companyId = localStorage.getItem(environment.ID_COMPANY);
    this.idCompany = companyId ? parseInt(companyId, 10) : 0;
  }

  ngOnInit(): void {
    this.loadEstablishments();
  }

  ngAfterViewInit(): void {
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
  }

  /**
   * Carga todas las sucursales de la compañía actual
   */
  loadEstablishments(): void {
    if (!this.idCompany) {
      this.logger.error('No hay ID de compañía en localStorage');
      this.toast.error('No se encontró la compañía');
      return;
    }

    this.isLoading = true;
    this.cdr.markForCheck();

    this.establishmentService.findByCompany(this.idCompany).subscribe({
      next: (data) => {
        // Normalizar el campo status a boolean
        const normalizedData = (data || []).map((item) => ({
          ...item,
          status: Boolean(item.status),
        }));

        this.dataSource.data = normalizedData;
        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: (err: HttpErrorResponse) => {
        this.logger.error('[SUCURSAL] Error al cargar sucursales', err);
        this.toast.error('Error al cargar sucursales');
        this.isLoading = false;
        this.cdr.markForCheck();
      },
    });
  }

  /**
   * Aplica filtros de búsqueda
   */
  applyFilters(): void {
    const codeFilter = this.searchCode.trim().toLowerCase();
    const nameFilter = this.searchName.trim().toLowerCase();

    this.dataSource.filterPredicate = (data: Establishment) => {
      const matchesCode =
        !codeFilter || data.code.toLowerCase().includes(codeFilter);
      const matchesName =
        !nameFilter || data.name.toLowerCase().includes(nameFilter);
      return matchesCode && matchesName;
    };

    // Trigger filter with a dummy value
    this.dataSource.filter = `${codeFilter}${nameFilter}`;
  }

  /**
   * Limpia los filtros de búsqueda
   */
  clearFilters(): void {
    this.searchCode = '';
    this.searchName = '';
    this.dataSource.filter = '';
  }

  /**
   * Abre el modal para crear una nueva sucursal
   */
  openCreateDialog(): void {
    const dialogRef = this.dialog.open(UpdateEstablishmentComponent, {
      width: '700px',
      maxWidth: '90vw',
      maxHeight: '90vh',
      disableClose: false,
      autoFocus: false,
      data: { idCompany: this.idCompany },
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result === true) {
        this.loadEstablishments();
      }
    });
  }

  /**
   * Abre el modal para editar una sucursal existente
   */
  openEditDialog(establishment: Establishment): void {
    const dialogRef = this.dialog.open(UpdateEstablishmentComponent, {
      width: '700px',
      maxWidth: '90vw',
      maxHeight: '90vh',
      disableClose: false,
      autoFocus: false,
      data: { establishment, idCompany: this.idCompany },
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result === true) {
        this.loadEstablishments();
      }
    });
  }

  /**
   * Elimina una sucursal: consulta puntos de emisión antes de confirmar.
   * Si tiene puntos activos, advierte al usuario y usa cascade=true.
   */
  delete(establishment: Establishment): void {
    this.issuancePointService
      .findByEstablishment(establishment.idEstablishment)
      .subscribe({
        next: (points) => {
          const activePoints = (points || []).filter((p) => p.status);
          const cascade = activePoints.length > 0;

          let message = `¿Está seguro de eliminar la sucursal "${establishment.name}" (código ${establishment.code})?`;

          if (cascade) {
            message +=
              `\n\nEsta sucursal tiene ${activePoints.length} punto(s) de emisión activo(s) ` +
              `(${activePoints.map((p) => p.code).join(', ')}). ` +
              `Al eliminarla, se desactivarán automáticamente.`;
          }

          this.confirmAndDelete(establishment, message, cascade);
        },
        error: () => {
          this.confirmAndDelete(
            establishment,
            `¿Está seguro de eliminar la sucursal "${establishment.name}" (código ${establishment.code})?`,
            false,
          );
        },
      });
  }

  private confirmAndDelete(
    establishment: Establishment,
    message: string,
    cascade: boolean,
  ): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '500px',
      maxWidth: '90vw',
      data: {
        title: cascade
          ? 'Eliminar Sucursal y Puntos de Emisión'
          : 'Eliminar Sucursal',
        message,
        type: 'danger',
      },
    });

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.isLoading = true;
        this.cdr.markForCheck();

        this.establishmentService
          .delete(establishment.idEstablishment, this.idCompany, cascade)
          .subscribe({
            next: () => {
              this.logger.info(
                `[SUCURSAL] DELETE${cascade ? ' (cascade)' : ''}: ${establishment.code} - ${establishment.name}`,
              );
              this.toast.success(
                cascade
                  ? 'Sucursal y puntos de emisión eliminados exitosamente'
                  : 'Sucursal eliminada exitosamente',
              );
              this.loadEstablishments();
            },
            error: (err: HttpErrorResponse) => {
              this.logger.error('[SUCURSAL] Error al eliminar', err);
              const errorMsg =
                err.error?.detail ||
                err.error?.message ||
                (typeof err.error === 'string' ? err.error : null) ||
                'Error al eliminar la sucursal';
              this.toast.error(errorMsg);
              this.isLoading = false;
              this.cdr.markForCheck();
            },
          });
      }
    });
  }

  /**
   * Retorna la clase CSS del badge según el estado
   */
  getStatusClass(status: boolean): string {
    return status ? 'status-active' : 'status-inactive';
  }

  /**
   * Retorna el texto del badge según el estado
   */
  getStatusText(status: boolean): string {
    return status ? 'ACTIVO' : 'INACTIVO';
  }
}
