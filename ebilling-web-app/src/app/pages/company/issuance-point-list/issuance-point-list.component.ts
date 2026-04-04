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
import { InvoiceIssuancePoint } from '../../../_model/customer/invoice-issuance-point';
import { IssuancePointService } from '../../../_service/customer/issuance-point.service';
import { UpdateIssuancePointComponent } from '../update-issuance-point/update-issuance-point.component';
import { ConfirmDialogComponent } from '../../../_shared/confirm-dialog/confirm-dialog.component';
import { LoggerService } from '../../../_service/logger/logger.service';
import { ToastService } from '../../../_service/notification/toast.service';
import { environment } from '../../../../environments/environment';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-issuance-point-list',
  templateUrl: './issuance-point-list.component.html',
  styleUrls: ['./issuance-point-list.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class IssuancePointListComponent implements OnInit {
  displayedColumns: string[] = [
    'code',
    'establishmentCode',
    'establishmentName',
    'sequential',
    'status',
    'actions',
  ];
  dataSource: MatTableDataSource<any> = new MatTableDataSource<any>();
  isLoading = false;
  idCompany: number;

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  searchCode = '';
  searchEstablishment = '';

  constructor(
    private readonly issuancePointService: IssuancePointService,
    private readonly dialog: MatDialog,
    private readonly toast: ToastService,
    private readonly logger: LoggerService,
    private readonly cdr: ChangeDetectorRef
  ) {
    const companyId = localStorage.getItem(environment.ID_COMPANY);
    this.idCompany = companyId ? parseInt(companyId, 10) : 0;
  }

  ngOnInit(): void {
    this.loadIssuancePoints();
  }

  ngAfterViewInit(): void {
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
  }

  loadIssuancePoints(): void {
    if (!this.idCompany) {
      this.logger.error('No hay ID de compañía en localStorage');
      this.toast.error('Error: No se encontró la compañía');
      return;
    }

    this.isLoading = true;
    this.cdr.markForCheck();

    this.issuancePointService.findByCompany(this.idCompany).subscribe({
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
        this.logger.error('[PUNTO EMISIÓN] Error al cargar', err);
        this.toast.httpError(err, 'cargar puntos de emisión', 'Error al cargar puntos de emisión');
        this.isLoading = false;
        this.cdr.markForCheck();
      },
    });
  }

  applyFilters(): void {
    const codeFilter = this.searchCode.trim().toLowerCase();
    const establishmentFilter = this.searchEstablishment.trim().toLowerCase();

    this.dataSource.filterPredicate = (data: any) => {
      const matchesCode =
        !codeFilter || data.code.toLowerCase().includes(codeFilter);
      const matchesEstablishment =
        !establishmentFilter ||
        data.establishmentCode?.toLowerCase().includes(establishmentFilter) ||
        data.establishmentName?.toLowerCase().includes(establishmentFilter);
      return matchesCode && matchesEstablishment;
    };

    this.dataSource.filter = `${codeFilter}${establishmentFilter}`;
  }

  clearFilters(): void {
    this.searchCode = '';
    this.searchEstablishment = '';
    this.dataSource.filter = '';
  }

  openCreateDialog(): void {
    const dialogRef = this.dialog.open(UpdateIssuancePointComponent, {
      width: '700px',
      maxWidth: '90vw',
      maxHeight: '90vh',
      disableClose: false,
      autoFocus: false,
      data: { idCompany: this.idCompany },
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result === true) {
        this.loadIssuancePoints();
      }
    });
  }

  openEditDialog(point: InvoiceIssuancePoint): void {
    const dialogRef = this.dialog.open(UpdateIssuancePointComponent, {
      width: '700px',
      maxWidth: '90vw',
      maxHeight: '90vh',
      disableClose: false,
      autoFocus: false,
      data: { point, idCompany: this.idCompany },
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result === true) {
        this.loadIssuancePoints();
      }
    });
  }

  delete(point: InvoiceIssuancePoint): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '500px',
      maxWidth: '90vw',
      data: {
        title: 'Eliminar Punto de Emisión',
        message: `¿Está seguro de eliminar el punto de emisión "${point.code}"?`,
        type: 'danger',
      },
    });

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.isLoading = true;
        this.cdr.markForCheck();

        this.issuancePointService
          .delete(point.idInvoiceIssuancePoint, point.establishmentId)
          .subscribe({
            next: () => {
              this.logger.info(`[PUNTO EMISIÓN] DELETE: ${point.code}`);
              
              // Eliminar el elemento de la lista localmente inmediatamente
              const currentData = this.dataSource.data;
              const filteredData = currentData.filter(
                (item) => item.idInvoiceIssuancePoint !== point.idInvoiceIssuancePoint
              );
              this.dataSource.data = filteredData;
              this.isLoading = false;
              this.cdr.markForCheck();
              
              this.toast.success('Punto de emisión eliminado exitosamente');
            },
            error: (err: HttpErrorResponse) => {
              this.logger.error('[PUNTO EMISIÓN] Error al eliminar', err);
              this.toast.httpError(err, 'eliminar el punto de emisión', 'Error al eliminar el punto de emisión');
              this.isLoading = false;
              this.cdr.markForCheck();
            },
          });
      }
    });
  }

  getStatusClass(status: boolean): string {
    return status ? 'status-active' : 'status-inactive';
  }

  getStatusText(status: boolean): string {
    return status ? 'ACTIVO' : 'INACTIVO';
  }
}
