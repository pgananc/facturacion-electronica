import {
  Component,
  OnInit,
  ViewChild,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  AfterViewInit,
} from '@angular/core';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute } from '@angular/router';
import {
  ADMIN_ROLE,
  DELETE,
  EMPTY_DATA,
  SUPER_ADMIN_ROLE,
} from '../../_constants/constants';
import { FormControl, FormGroup } from '@angular/forms';
import { CompanyService } from '../../_service/customer/company.service';
import { decodeToken } from '../../_functions/functions';
import {
  CompanyFilter,
  TAX_REGIME_OPTIONS,
  TaxRegimeOption,
} from '../../_model/customer/company-filter';
import { LoggerService } from '../../_service/logger/logger.service';
import { catchError, of } from 'rxjs';
import { UpdateCompanyComponent } from './update-company/update-company.component';
import { ConfirmDialogComponent } from '../../_shared/confirm-dialog/confirm-dialog.component';
import { ToastService } from '../../_service/notification/toast.service';
import { environment } from '../../../environments/environment';
import { UserRoleService } from '../../_service/auth/user-role.service';

/**
 * CompanyComponent - Lista y gestión de empresas
 *
 * Optimizado con OnPush Change Detection para mejor performance
 */
@Component({
  selector: 'app-company',
  templateUrl: './company.component.html',
  styleUrls: ['./company.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CompanyComponent implements OnInit, AfterViewInit {
  roles: string[] = [];
  companiesIds: number[] = [];
  form: FormGroup;
  displayedColumns = [
    'identification',
    'name',
    'nameCommercial',
    'forceToAccounting',
    'regime',
    'status',
  ];
  dataSource = new MatTableDataSource<CompanyFilter>([]);
  status: boolean = true;
  quantity: number = 0;
  pageSize: number = 5; // Tamaño de página inicial

  @ViewChild(MatPaginator, { static: false }) paginator: MatPaginator;
  @ViewChild(MatSort, { static: false }) sort: MatSort;

  constructor(
    private readonly companyService: CompanyService,
    public route: ActivatedRoute,
    private readonly logger: LoggerService,
    private readonly cdr: ChangeDetectorRef,
    private readonly dialog: MatDialog,
    private readonly toast: ToastService,
    private readonly userRoleService: UserRoleService
  ) {}

  ngOnInit() {
    this.logger.debug('🚀 ngOnInit() - INICIO');

    // Inicializar form primero para evitar errores en el template
    this.form = new FormGroup({
      identification: new FormControl(''),
      name: new FormControl(''),
    });
    this.status = true;
    this.logger.debug('✅ Form inicializado');

    const decodedToken = decodeToken();
    this.logger.debug('🔐 Token decodificado:', decodedToken);

    // Protección: Si no hay token válido, no continuar
    if (!decodedToken) {
      this.logger.error(
        '❌ No hay token válido en CompanyComponent.ngOnInit()'
      );
      return;
    }

    // Obtener companiesIds del token (necesario para filtrado)
    this.companiesIds = decodedToken.companies || [];
    this.logger.debug('🏢 Companies IDs asignados:', this.companiesIds);

    // Cargar roles de la compañía actual usando el servicio centralizado
    this.userRoleService.getCurrentCompanyRoles().subscribe({
      next: (roles) => {
        this.roles = roles || [];
        // super_admin es un rol global en el token; si el backend no devuelve roles para esta compañía,
        // seguir permitiendo edición/creación si el token tiene super_admin o admin
        const tokenRoles = (decodedToken as any)?.roles as string[] | undefined;
        if (tokenRoles?.length) {
          const toAdd = tokenRoles.filter(
            (r) =>
              (r === SUPER_ADMIN_ROLE.name || r === ADMIN_ROLE.name) &&
              !this.roles.includes(r)
          );
          if (toAdd.length) {
            this.roles = [...this.roles, ...toAdd];
            this.logger.debug('👤 Roles del token fusionados para compañía:', this.roles);
          }
        }
        this.logger.debug('👤 Roles asignados desde UserRoleService:', this.roles);

        // Actualizar columnas según roles
        if (
          this.roles.includes(SUPER_ADMIN_ROLE.name) ||
          this.roles.includes(ADMIN_ROLE.name)
        ) {
          if (!this.displayedColumns.includes('actions')) {
            this.displayedColumns.push('actions');
            this.logger.debug('✅ Columna actions agregada');
          }
        }
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.logger.error('Error al cargar roles:', err);
        this.roles = [];
        this.cdr.markForCheck();
      },
    });

    // La lógica de columnas se maneja en la suscripción de roles

    // Cargar datos iniciales
    this.logger.debug('🔍 Llamando a search() desde ngOnInit()');
    this.search();
    this.logger.debug('🚀 ngOnInit() - FIN');
  }

  ngAfterViewInit() {
    // Configurar ordenamiento después de que la vista esté inicializada
    if (this.sort) {
      this.sort.sortChange.subscribe(() => {
        this.logger.debug('🔄 Sort changed:', {
          active: this.sort.active,
          direction: this.sort.direction,
        });
      });
    }
  }

  /**
   * Elimina una compañía con confirmación previa
   */
  delete(idCompany: number): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '500px',
      maxWidth: '90vw',
      data: {
        title: 'Confirmar Eliminación',
        message: '¿Está seguro de que desea eliminar esta compañía?',
        confirmText: 'Eliminar',
        cancelText: 'Cancelar',
        type: 'danger',
      },
    });

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.companyService
          .delete(idCompany)
          .pipe(
            catchError((error) => {
              this.logger.error('Error al eliminar la compañía', error);
              this.toast.error('Error al eliminar la compañía');
              return of(null);
            })
          )
          .subscribe(() => {
            // Recargar la búsqueda con los filtros actuales
            this.search();
            // ✅ Mostrar toast personalizado de éxito
            this.toast.success(DELETE.MESSAGE_DELETE_COMPANY.message);
            this.cdr.detectChanges();
          });
      }
    });
  }

  applyFilter(event: Event) {
    const input = (event.target as HTMLInputElement).value;
    this.dataSource.filter = input.trim().toLowerCase();
  }

  search() {
    this.logger.debug('🔍 search() - INICIO');

    // Resetear el paginador a la primera página cuando se hace una nueva búsqueda
    if (this.paginator) {
      this.paginator.pageIndex = 0;
    }

    let company = {} as CompanyFilter;
    company.identification = this.form.value['identification'];
    company.name = this.form.value['name'];
    company.status = this.status;
    company.isSuperAdmin = this.roles.includes(SUPER_ADMIN_ROLE.name);
    
    // Si NO es super admin, enviar el ID de la compañía logueada en companiesIds
    if (!company.isSuperAdmin) {
      const loggedCompanyId = localStorage.getItem(environment.ID_COMPANY);
      if (loggedCompanyId) {
        company.companiesIds = [parseInt(loggedCompanyId, 10)];
        this.logger.debug('🏢 ID de compañía logueada (en companiesIds):', company.companiesIds);
      } else {
        company.companiesIds = [];
        this.logger.warn('⚠️ No se encontró ID de compañía en localStorage');
      }
    } else {
      // Si es super admin, usar companiesIds del token como antes
      company.companiesIds = this.companiesIds;
    }

    this.logger.debug('📤 Parámetros de búsqueda:', company);
    this.logger.debug('🔒 Roles actuales:', this.roles);
    this.logger.debug('🏢 CompaniesIds actuales:', company.companiesIds);

    // Usar el pageSize del paginador si está disponible, sino usar el valor por defecto
    const currentPageSize = this.paginator?.pageSize || this.pageSize;

    this.companyService
      .searchPageable(0, currentPageSize, company)
      .pipe(
        catchError((error) => {
          this.logger.error('Error en búsqueda de compañías', error);
          this.toast.error(
            'Error al cargar compañías. Por favor, intente nuevamente.'
          );
          return of(null);
        })
      )
      .subscribe((data) => {
        this.logger.debug('📥 Respuesta recibida en subscribe:', data);

        if (data != null) {
          this.logger.debug('✅ Data NO es null');
          this.logger.debug('📊 Total elementos:', data.totalElements);
          this.logger.debug('📋 Contenido length:', data.content?.length);

          // Log temporal para verificar el campo 'type' y 'principal'
          if (data.content?.length > 0) {
            this.logger.debug('🏛️ CAMPO TYPE:', data.content[0].type);
            this.logger.debug('🏛️ CAMPO PRINCIPAL:', data.content[0].principal);
            this.logger.debug(
              '🏛️ Es MATRIZ?',
              data.content[0].type === 'MATRIZ'
            );
            this.logger.debug(
              '🏛️ TODOS los campos de la primera compañía:',
              Object.keys(data.content[0])
            );
          }

          this.quantity = data.totalElements;
          this.dataSource = new MatTableDataSource(data.content);

          // Sincronizar el pageSize y pageIndex del paginador si está disponible
          if (this.paginator) {
            if (this.paginator.pageSize !== this.pageSize) {
              this.paginator.pageSize = this.pageSize;
            }
            // Asegurar que el pageIndex esté en 0 para la nueva búsqueda
            if (this.paginator.pageIndex !== 0) {
              this.paginator.pageIndex = 0;
            }
          }

          // Asignar sort DESPUÉS de crear el dataSource
          this.dataSource.sort = this.sort!;

          // Configurar sortingDataAccessor para asegurar ordenamiento correcto
          this.dataSource.sortingDataAccessor = (item, property) => {
            switch (property) {
              case 'identification':
                return item.identification || '';
              case 'name':
                return item.name || '';
              default:
                return (item as any)[property];
            }
          };

          // Forzar detección de cambios con OnPush
          this.cdr.detectChanges();

          this.logger.debug('✅ DataSource asignado correctamente');
          this.logger.debug(
            '📊 Rows en dataSource:',
            this.dataSource.data.length
          );
          this.logger.debug('🔄 Change detection forzada');
        } else {
          this.logger.error('❌ Data es null');
          this.toast.info(EMPTY_DATA.MESSAGE_EMPTY_DATA.message);
        }

        this.logger.debug('🔍 search() - FIN');
      });
  }

  limpiar() {
    this.form = new FormGroup({
      identification: new FormControl(''),
      name: new FormControl(''),
    });
    this.status = true;
    this.search();
  }

  showMore(e: any) {
    // Actualizar el pageSize cuando el usuario lo cambia
    this.pageSize = e.pageSize;

    let company = {} as CompanyFilter;
    company.identification = this.form.value['identification'];
    company.name = this.form.value['name'];
    company.status = this.status;
    company.isSuperAdmin = this.roles.includes(SUPER_ADMIN_ROLE.name);
    
    // Si NO es super admin, enviar el ID de la compañía logueada en companiesIds
    if (!company.isSuperAdmin) {
      const loggedCompanyId = localStorage.getItem(environment.ID_COMPANY);
      if (loggedCompanyId) {
        company.companiesIds = [parseInt(loggedCompanyId, 10)];
      } else {
        company.companiesIds = [];
      }
    } else {
      // Si es super admin, usar companiesIds del token como antes
      company.companiesIds = this.companiesIds;
    }

    this.companyService
      .searchPageable(e.pageIndex, e.pageSize, company)
      .pipe(
        catchError((error) => {
          this.logger.error('Error al paginar compañías', error);
          this.toast.error(
            'Error al cargar más resultados. Por favor, intente nuevamente.'
          );
          return of(null);
        })
      )
      .subscribe((data) => {
        if (data != null) {
          this.quantity = data.totalElements;
          this.dataSource = new MatTableDataSource(data.content);

          // Sincronizar el pageSize del paginador si está disponible
          if (this.paginator && this.paginator.pageSize !== this.pageSize) {
            this.paginator.pageSize = this.pageSize;
          }

          // Asignar sort DESPUÉS de crear el dataSource
          this.dataSource.sort = this.sort!;

          // Configurar sortingDataAccessor para asegurar ordenamiento correcto
          this.dataSource.sortingDataAccessor = (item, property) => {
            switch (property) {
              case 'identification':
                return item.identification || '';
              case 'name':
                return item.name || '';
              default:
                return (item as any)[property];
            }
          };

          // Forzar detección de cambios con OnPush
          this.cdr.detectChanges();
        }
      });
  }

  /**
   * Abre el modal para crear o editar una compañía
   */
  openDialog(company?: CompanyFilter): void {
    const dialogRef = this.dialog.open(UpdateCompanyComponent, {
      width: '650px',
      maxWidth: '90vw',
      maxHeight: '90vh',
      disableClose: false,
      autoFocus: false,
      data: company ? { company, isEdit: true } : { isEdit: false },
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.logger.debug('✅ Modal cerrado con cambios, refrescando tabla');
        this.search(); // Refresca la tabla después de guardar
      } else {
        this.logger.debug('❌ Modal cerrado sin cambios');
      }
    });
  }

  /**
   * Obtiene el label del régimen tributario basado en el código
   */
  getRegimeLabel(code: string | null | undefined): string {
    if (!code) {
      return '-';
    }
    const option = TAX_REGIME_OPTIONS.find((opt) => opt.code === code);
    return option ? option.label : code;
  }
}
