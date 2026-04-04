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
import {
  Establishment,
  Company,
} from '../../../_model/customer/company-establishment';
import { EstablishmentService } from '../../../_service/customer/establishment.service';
import { CompanyService } from '../../../_service/customer/company.service';
import { UpdateEstablishmentComponent } from '../../company/update-establishment/update-establishment.component';
import { ConfirmDialogComponent } from '../../../_shared/confirm-dialog/confirm-dialog.component';
import { LoggerService } from '../../../_service/logger/logger.service';
import { ToastService } from '../../../_service/notification/toast.service';
import { HttpErrorResponse } from '@angular/common/http';

/**
 * Interfaz extendida para mostrar sucursales con el nombre de la compañía
 */
interface EstablishmentWithCompany extends Establishment {
  companyName?: string;
}

/**
 * Componente para gestión global de sucursales (Super Admin)
 * Permite ver y administrar sucursales de TODAS las compañías
 */
@Component({
  selector: 'app-admin-establishment-list',
  templateUrl: './admin-establishment-list.component.html',
  styleUrls: ['./admin-establishment-list.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminEstablishmentListComponent implements OnInit {
  displayedColumns: string[] = [
    'companyName',
    'code',
    'name',
    'address',
    'type',
    'status',
    'actions',
  ];
  dataSource: MatTableDataSource<EstablishmentWithCompany> =
    new MatTableDataSource<EstablishmentWithCompany>();
  isLoading = false;

  // Lista de todas las compañías (para el filtro)
  companies: Company[] = [];
  allEstablishments: EstablishmentWithCompany[] = [];

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  // Formulario de búsqueda
  searchCompanyId: number | null = null;
  searchCode = '';
  searchName = '';

  constructor(
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

  /**
   * Carga la lista de todas las compañías
   */
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

  /**
   * Carga todas las sucursales de una compañía específica
   */
  loadEstablishmentsByCompany(companyId: number): void {
    this.isLoading = true;
    this.cdr.markForCheck();

    const company = this.companies.find((c) => c.idCompany === companyId);
    const companyName = company?.name || 'Desconocida';

    this.establishmentService.findByCompany(companyId).subscribe({
      next: (data) => {
        // Normalizar el campo status, agregar el nombre de la compañía y asegurar que idCompany esté presente
        const normalizedData: EstablishmentWithCompany[] = (data || []).map(
          (item) => ({
            ...item,
            idCompany: item.idCompany || companyId, // ✅ Asegurar que idCompany esté presente
            status: Boolean(item.status),
            companyName,
          })
        );

        this.allEstablishments = normalizedData;
        this.dataSource.data = normalizedData;
        this.isLoading = false;
        this.logger.info(
          `[ADMIN] ${normalizedData.length} sucursales cargadas para ${companyName}`
        );
        this.cdr.markForCheck();
      },
      error: (err: HttpErrorResponse) => {
        this.logger.error('[ADMIN] Error al cargar sucursales', err);
        this.toast.httpError(err, 'cargar las sucursales');
        this.isLoading = false;
        this.cdr.markForCheck();
      },
    });
  }

  /**
   * Aplica filtros de búsqueda
   */
  applyFilters(): void {
    // Si se seleccionó una compañía, cargar sus sucursales
    if (this.searchCompanyId) {
      this.loadEstablishmentsByCompany(this.searchCompanyId);
      return;
    }

    // Si no hay compañía seleccionada, limpiar tabla
    this.dataSource.data = [];
    this.allEstablishments = [];
    this.toast.warning('Por favor seleccione una compañía para ver sus sucursales');
  }

  /**
   * Aplica filtros de código y nombre sobre los datos ya cargados
   */
  applyLocalFilters(): void {
    const codeFilter = this.searchCode.trim().toLowerCase();
    const nameFilter = this.searchName.trim().toLowerCase();

    if (!codeFilter && !nameFilter) {
      this.dataSource.data = this.allEstablishments;
      return;
    }

    this.dataSource.filterPredicate = (data: EstablishmentWithCompany) => {
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
    this.searchCompanyId = null;
    this.searchCode = '';
    this.searchName = '';
    this.dataSource.data = [];
    this.allEstablishments = [];
    this.dataSource.filter = '';
  }

  /**
   * Abre el modal para crear una nueva sucursal (modo super admin)
   * Permite abrir el modal sin compañía seleccionada, ya que el usuario puede seleccionarla dentro del modal
   */
  openCreateDialog(): void {
    const dialogRef = this.dialog.open(UpdateEstablishmentComponent, {
      width: '700px',
      maxWidth: '90vw',
      maxHeight: '90vh',
      disableClose: false,
      autoFocus: false,
      data: {
        idCompany: this.searchCompanyId || undefined, // Pasar la compañía seleccionada si existe, sino undefined
        isSuperAdmin: true, // ✅ Flag para super admin
      },
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result === true) {
        // Si hay una compañía seleccionada, refrescar sus sucursales
        // Si no hay compañía seleccionada pero se creó una sucursal, el usuario deberá seleccionar la compañía para verla
        if (this.searchCompanyId) {
          this.loadEstablishmentsByCompany(this.searchCompanyId);
        }
      }
    });
  }

  /**
   * Abre el modal para editar una sucursal existente (modo super admin)
   */
  openEditDialog(establishment: EstablishmentWithCompany): void {
    // ✅ Log para depuración
    this.logger.debug(
      `[ADMIN] Abriendo diálogo de edición - establishment.idCompany: ${establishment.idCompany}, establishment:`,
      establishment
    );

    const dialogRef = this.dialog.open(UpdateEstablishmentComponent, {
      width: '700px',
      maxWidth: '90vw',
      maxHeight: '90vh',
      disableClose: false,
      autoFocus: false,
      data: {
        establishment,
        idCompany: establishment.idCompany,
        isSuperAdmin: true, // ✅ OPCIÓN A: Flag para super admin
      },
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result === true && this.searchCompanyId) {
        this.loadEstablishmentsByCompany(this.searchCompanyId);
      }
    });
  }

  /**
   * Elimina una sucursal (confirmación previa)
   */
  delete(establishment: EstablishmentWithCompany): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '500px',
      maxWidth: '90vw',
      data: {
        title: 'Eliminar Sucursal',
        message: `¿Está seguro de eliminar la sucursal "${establishment.name}" (código ${establishment.code}) de la compañía "${establishment.companyName}"?`,
        type: 'danger',
      },
    });

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.isLoading = true;
        this.cdr.markForCheck();

        this.establishmentService
          .delete(establishment.idEstablishment, establishment.idCompany)
          .subscribe({
            next: () => {
              this.logger.info(
                `[ADMIN] DELETE: ${establishment.code} - ${establishment.name} (${establishment.companyName})`
              );
              this.toast.success('Sucursal eliminada exitosamente');
              if (this.searchCompanyId) {
                this.loadEstablishmentsByCompany(this.searchCompanyId);
              }
            },
            error: (err: HttpErrorResponse) => {
              this.logger.error('[ADMIN] Error al eliminar', err);
              this.toast.httpError(err, 'eliminar la sucursal');
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
