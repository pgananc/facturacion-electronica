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
import { ToastService } from '../../../_service/notification/toast.service';
import {
  Establishment,
  Company,
} from '../../../_model/customer/company-establishment';
import { IssuancePointService } from '../../../_service/customer/issuance-point.service';
import { EstablishmentService } from '../../../_service/customer/establishment.service';
import { CompanyService } from '../../../_service/customer/company.service';
import { UpdateIssuancePointComponent } from '../../company/update-issuance-point/update-issuance-point.component';
import { ConfirmDialogComponent } from '../../../_shared/confirm-dialog/confirm-dialog.component';
import { LoggerService } from '../../../_service/logger/logger.service';
import { HttpErrorResponse } from '@angular/common/http';

interface IssuancePointWithDetails extends InvoiceIssuancePoint {
  establishmentCode?: string;
  establishmentName?: string;
  companyName?: string;
  idCompany?: number; // ✅ ID de la compañía para el modal
}

/**
 * Componente para gestión global de puntos de emisión (Super Admin)
 * Permite ver y administrar puntos de emisión de TODAS las compañías
 */
@Component({
  selector: 'app-admin-issuance-point-list',
  templateUrl: './admin-issuance-point-list.component.html',
  styleUrls: ['./admin-issuance-point-list.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminIssuancePointListComponent implements OnInit {
  displayedColumns: string[] = [
    'companyName',
    'establishmentCode',
    'establishmentName',
    'code',
    'sequential',
    'status',
    'actions',
  ];
  dataSource: MatTableDataSource<IssuancePointWithDetails> =
    new MatTableDataSource<IssuancePointWithDetails>();
  isLoading = false;

  companies: Company[] = [];
  establishments: Establishment[] = [];
  allIssuancePoints: IssuancePointWithDetails[] = [];

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  searchCompanyId: number | null = null;
  searchEstablishmentId: number | null = null;
  searchCode = '';

  constructor(
    private readonly issuancePointService: IssuancePointService,
    private readonly establishmentService: EstablishmentService,
    private readonly companyService: CompanyService,
    private readonly dialog: MatDialog,
    private readonly toast: ToastService,
    private readonly logger: LoggerService,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadCompanies();
  }

  ngAfterViewInit(): void {
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
  }

  loadCompanies(): void {
    this.companyService.findAll().subscribe({
      next: (response) => {
        this.companies = response.companyDetailsDtos || [];
        this.logger.info(`[ADMIN] ${this.companies.length} compañías cargadas`);
        this.cdr.markForCheck();
      },
      error: (err: HttpErrorResponse) => {
        this.logger.error('[ADMIN] Error al cargar compañías', err);
        this.toast.httpError(err, 'cargar las compañías');
        this.cdr.markForCheck();
      },
    });
  }

  onCompanyChange(): void {
    this.searchEstablishmentId = null;
    this.establishments = [];
    this.dataSource.data = [];
    this.allIssuancePoints = [];

    if (this.searchCompanyId) {
      this.loadEstablishments(this.searchCompanyId);
    }
  }

  loadEstablishments(companyId: number): void {
    this.establishmentService.findByCompany(companyId).subscribe({
      next: (data) => {
        this.establishments = data || [];
        this.logger.info(
          `[ADMIN] ${this.establishments.length} sucursales cargadas para compañía ${companyId}`
        );
        this.cdr.markForCheck();
      },
      error: (err: HttpErrorResponse) => {
        this.logger.error('[ADMIN] Error al cargar sucursales', err);
        this.toast.httpError(err, 'cargar las sucursales');
        this.cdr.markForCheck();
      },
    });
  }

  applyFilters(): void {
    if (!this.searchCompanyId) {
      this.toast.warning('Debe seleccionar una compañía');
      return;
    }

    if (!this.searchEstablishmentId) {
      this.toast.warning('Debe seleccionar una sucursal');
      return;
    }

    this.loadIssuancePoints();
  }

  loadIssuancePoints(): void {
    if (!this.searchCompanyId || !this.searchEstablishmentId) {
      return;
    }

    this.isLoading = true;
    this.cdr.markForCheck();

    const company = this.companies.find(
      (c) => c.idCompany === this.searchCompanyId
    );
    const establishment = this.establishments.find(
      (e) => e.idEstablishment === this.searchEstablishmentId
    );

    const companyName = company?.name || 'Desconocida';
    const establishmentCode = establishment?.code || '';
    const establishmentName = establishment?.name || '';

    this.issuancePointService
      .findByEstablishment(this.searchEstablishmentId)
      .subscribe({
        next: (data) => {
          const normalizedData: IssuancePointWithDetails[] = (data || []).map(
            (item) => ({
              ...item,
              status: Boolean(item.status),
              companyName,
              establishmentCode,
              establishmentName,
              idCompany: this.searchCompanyId!, // ✅ Agregar idCompany para el modal
            })
          );

          this.allIssuancePoints = normalizedData;
          this.dataSource.data = normalizedData;
          this.isLoading = false;
          this.logger.info(
            `[ADMIN] ${normalizedData.length} puntos de emisión cargados para ${companyName} - ${establishmentName}`
          );
          this.cdr.markForCheck();
        },
        error: (err: HttpErrorResponse) => {
          this.logger.error('[ADMIN] Error al cargar puntos de emisión', err);
          this.toast.httpError(err, 'cargar los puntos de emisión');
          this.isLoading = false;
          this.cdr.markForCheck();
        },
      });
  }

  applyLocalFilters(): void {
    const codeFilter = this.searchCode.trim().toLowerCase();

    if (!codeFilter) {
      this.dataSource.data = this.allIssuancePoints;
      return;
    }

    this.dataSource.filterPredicate = (data: IssuancePointWithDetails) => {
      return data.code.toLowerCase().includes(codeFilter);
    };

    this.dataSource.filter = codeFilter;
  }

  clearFilters(): void {
    this.searchCompanyId = null;
    this.searchEstablishmentId = null;
    this.searchCode = '';
    this.establishments = [];
    this.dataSource.data = [];
    this.allIssuancePoints = [];
    this.dataSource.filter = '';
  }

  /**
   * Abre el modal para crear un nuevo punto de emisión (modo super admin)
   * Permite abrir el modal sin compañía/sucursal seleccionada, ya que el usuario puede seleccionarlas dentro del modal
   */
  openCreateDialog(): void {
    const dialogRef = this.dialog.open(UpdateIssuancePointComponent, {
      width: '700px',
      maxWidth: '90vw',
      maxHeight: '90vh',
      disableClose: false,
      autoFocus: false,
      data: {
        idCompany: this.searchCompanyId || undefined, // Pasar la compañía seleccionada si existe, sino undefined
        establishmentId: this.searchEstablishmentId || undefined, // Pasar la sucursal seleccionada si existe, sino undefined
        isSuperAdmin: true,
      },
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result === true) {
        // Si hay una compañía y sucursal seleccionada, refrescar sus puntos de emisión
        // Si no hay selección pero se creó un punto, el usuario deberá seleccionar para verlo
        if (this.searchCompanyId && this.searchEstablishmentId) {
          this.loadIssuancePoints();
        }
      }
    });
  }

  openEditDialog(issuancePoint: IssuancePointWithDetails): void {
    const dialogRef = this.dialog.open(UpdateIssuancePointComponent, {
      width: '700px',
      maxWidth: '90vw',
      maxHeight: '90vh',
      disableClose: false,
      autoFocus: false,
      data: {
        point: issuancePoint, // ✅ Nombre correcto de la propiedad
        idCompany: issuancePoint.idCompany,
        isSuperAdmin: true,
      },
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result === true) {
        // Refrescar si hay compañía y sucursal seleccionadas
        if (this.searchCompanyId && this.searchEstablishmentId) {
          this.loadIssuancePoints();
        }
      }
    });
  }

  delete(issuancePoint: IssuancePointWithDetails): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '500px',
      maxWidth: '90vw',
      data: {
        title: 'Eliminar Punto de Emisión',
        message: `¿Está seguro de eliminar el punto de emisión "${issuancePoint.code}" de la sucursal "${issuancePoint.establishmentName}" (${issuancePoint.companyName})?`,
        type: 'danger',
      },
    });

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.isLoading = true;
        this.cdr.markForCheck();

        this.issuancePointService
          .delete(
            issuancePoint.idInvoiceIssuancePoint,
            issuancePoint.establishmentId
          )
          .subscribe({
            next: () => {
              this.logger.info(
                `[ADMIN] DELETE: Punto emisión ${issuancePoint.code} (${issuancePoint.companyName} - ${issuancePoint.establishmentName})`
              );
              
              // Eliminar el elemento de la lista localmente inmediatamente
              const currentData = this.dataSource.data;
              const filteredData = currentData.filter(
                (item) => item.idInvoiceIssuancePoint !== issuancePoint.idInvoiceIssuancePoint
              );
              this.dataSource.data = filteredData;
              this.allIssuancePoints = filteredData;
              this.isLoading = false;
              this.cdr.markForCheck();
              
              this.toast.success('Punto de emisión eliminado exitosamente');
            },
            error: (err: HttpErrorResponse) => {
              this.logger.error('[ADMIN] Error al eliminar', err);
              this.toast.httpError(err, 'eliminar el punto de emisión');
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
