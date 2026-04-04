import {
  CLIENT_TYPE,
  CONTACT_TYPE_ADRESS,
  CONTACT_TYPE_MAIL,
  CONTACT_TYPE_MOBIL,
  CONTACT_TYPE_PHONE,
  DELETE,
  EMPTY_DATA,
  ID_TYPE_CONSUMIDOR_FINAL,
} from '../../../_constants/constants';
import {
  Component,
  OnInit,
  OnDestroy,
  AfterViewInit,
  ViewChild,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Injectable,
} from '@angular/core';
import { MatPaginator } from '@angular/material/paginator';
import { MatPaginatorIntl } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { Client } from '../../../_model/customer/client';
import { ClientService } from '../../../_service/customer/client.service';
import { FormControl, FormGroup } from '@angular/forms';
import { environment } from 'src/environments/environment';
import { ClientRequestDto } from '../../../_model/customer/client-request-dto';
import { Subject } from 'rxjs';
import { takeUntil, catchError } from 'rxjs/operators';
import { of } from 'rxjs';
import { MatDialog } from '@angular/material/dialog';
import { UpdateClientComponent } from './update-client/update-client.component';
import { ClientHelperService } from '../../../_service/customer/client-helper.service';
import { LoggerService } from '../../../_service/logger/logger.service';
import { ConfirmDialogComponent } from '../../../_shared/confirm-dialog/confirm-dialog.component';
import { ToastService } from '../../../_service/notification/toast.service';

/**
 * Custom Paginator Intl para personalizar el texto
 */
@Injectable()
export class CustomPaginatorIntl extends MatPaginatorIntl {
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
      return `0 de ${length} clientes`;
    }
    const startIndex = page * pageSize;
    const endIndex =
      startIndex < length
        ? Math.min(startIndex + pageSize, length)
        : startIndex + pageSize;
    return `Mostrando ${startIndex + 1}-${endIndex} de ${length} clientes`;
  };
}

/**
 * ClientComponent - Lista y gestión de clientes
 *
 * Optimizado con OnPush Change Detection para mejor performance
 */
@Component({
  selector: 'app-client',
  templateUrl: './client.component.html',
  styleUrls: ['./client.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [{ provide: MatPaginatorIntl, useClass: CustomPaginatorIntl }],
})
export class ClientComponent implements OnInit, AfterViewInit, OnDestroy {
  form: FormGroup;
  clientTypes = CLIENT_TYPE;
  displayedColumns = [
    'identification',
    'name',
    'address',
    'mail',
    'phone',
    'mobil',
    'actions',
  ];
  dataSourceClients = new MatTableDataSource<Client>([]);
  quantity: number = 0;
  pageSize: number = 5; // Unified page size
  isLoading: boolean = false; // Loading state for search

  // Server-side sorting state
  currentSortField: string = '';
  currentSortDirection: string = '';

  private destroy$ = new Subject<void>();

  @ViewChild(MatPaginator, { static: true }) paginator: MatPaginator;
  @ViewChild(MatSort, { static: false }) sort: MatSort;

  constructor(
    private readonly clientService: ClientService,
    private readonly cdr: ChangeDetectorRef,
    private readonly dialog: MatDialog,
    private readonly clientHelper: ClientHelperService,
    private readonly logger: LoggerService,
    private readonly toast: ToastService
  ) {}

  ngOnInit() {
    this.form = new FormGroup({
      identification: new FormControl(''),
      name: new FormControl(''),
      clientType: new FormControl(0),
      status: new FormControl(true),
    });

    // Subscribe to clients changes with automatic cleanup
    this.clientService.clientsChange
      .pipe(takeUntil(this.destroy$))
      .subscribe((data) => {
        this.dataSourceClients = new MatTableDataSource(data);
        if (this.sort) {
          this.dataSourceClients.sort = this.sort;
        }
        // ✅ NO asignar paginator porque usamos paginación del servidor (Server-Side Pagination)
        // this.dataSourceClients.paginator = this.paginator;
        this.cdr.markForCheck();
      });

    // Verificar si hay token antes de cargar datos
    const token = localStorage.getItem('token');
    if (!token) {
      this.logger.warn(
        '[ClientComponent] No hay token válido, omitiendo carga de datos'
      );
      return;
    }

    // Initial load with error handling
    const clientRequest = this.buildClientRequest();
    this.clientService
      .searchPageable(
        0,
        this.pageSize,
        clientRequest,
        this.currentSortField,
        this.currentSortDirection
      )
      .pipe(
        takeUntil(this.destroy$),
        catchError((error) => {
          this.logger.error('Error loading clients', error);
          this.toast.error(
            'Error al cargar clientes. Por favor intente nuevamente.'
          );
          return of({ content: [], totalElements: 0 });
        })
      )
      .subscribe((data) => {
        let clientList = data.content as Client[];
        this.clientHelper.addContactInformation(clientList);
        this.quantity = data.totalElements;
        this.dataSourceClients = new MatTableDataSource(clientList);
        if (this.sort) {
          this.dataSourceClients.sort = this.sort;
        }
        this.cdr.markForCheck();
      });
  }

  ngAfterViewInit() {
    // Subscribe to sort changes for server-side sorting
    // Must be in ngAfterViewInit to ensure MatSort is initialized
    if (this.sort) {
      this.sort.sortChange.pipe(takeUntil(this.destroy$)).subscribe(() => {
        // Reset to first page when sorting
        if (this.paginator) {
          this.paginator.pageIndex = 0;
        }
        this.onSortChange();
      });
    }
  }

  onSortChange() {
    if (!this.sort) {
      return;
    }

    // Map frontend column names to backend field names
    // Spring Boot will handle the alias automatically in JPQL
    const fieldMapping: { [key: string]: string } = {
      identification: 'identification',
      name: 'name',
      clientType: 'clientType',
      status: 'status',
    };

    // Update current sort state
    this.currentSortField = fieldMapping[this.sort.active] || this.sort.active;
    this.currentSortDirection = this.sort.direction;

    // Reload data with new sort
    this.search();
  }

  delete(idClient: number) {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '500px',
      maxWidth: '90vw',
      data: {
        title: 'Confirmar eliminación',
        message:
          '¿Está seguro de que desea eliminar este cliente? Esta acción no se puede deshacer.',
        confirmText: 'Eliminar',
        cancelText: 'Cancelar',
        type: 'danger',
      },
    });

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.clientService
          .delete(idClient)
          .pipe(
            takeUntil(this.destroy$),
            catchError((error) => {
              this.logger.error('Error deleting client', error);
              this.toast.error(
                'Error al eliminar cliente. Por favor intente nuevamente.'
              );
              return of(null);
            })
          )
          .subscribe((response) => {
            if (response !== null) {
              // ✅ Refrescar desde el servidor en lugar de filtrar localmente
              this.search();

              // ✅ Mostrar toast personalizado de éxito
              this.toast.success(DELETE.MESSAGE_DELETE_CLIENT.message);
            }
          });
      }
    });
  }

  search() {
    // Reset to first page when searching
    if (this.paginator) {
      this.paginator.pageIndex = 0;
    }
    this.isLoading = true; // ✅ Activar loading
    this.cdr.markForCheck(); // Forzar detección de cambios

    const client = this.buildClientRequest();
    this.clientService
      .searchPageable(
        0,
        this.pageSize,
        client,
        this.currentSortField,
        this.currentSortDirection
      )
      .pipe(
        takeUntil(this.destroy$),
        catchError((error) => {
          this.logger.error('Error searching clients', error);
          this.isLoading = false; // ✅ Desactivar loading en error
          this.toast.error(
            'Error al buscar clientes. Por favor intente nuevamente.'
          );
          return of(null);
        })
      )
      .subscribe((data) => {
        this.isLoading = false; // ✅ Desactivar loading
        if (data != null && data.content && data.content.length > 0) {
          let clientList = data.content as Client[];
          this.clientHelper.addContactInformation(clientList);
          this.quantity = data.totalElements;
          this.dataSourceClients.data = clientList;
          this.cdr.markForCheck();
        } else {
          this.dataSourceClients.data = [];
          this.quantity = 0;
          // ✅ NO mostrar snackbar, el empty state en el HTML lo muestra
          this.cdr.markForCheck();
        }
      });
  }

  limpiar() {
    this.form.reset({
      identification: '',
      name: '',
      clientType: 0,
      status: true,
    });
    this.cdr.markForCheck();
    // Recargar la lista con filtros limpios
    this.search();
  }

  showMore(e: any) {
    const client = this.buildClientRequest();
    this.clientService
      .searchPageable(
        e.pageIndex,
        e.pageSize,
        client,
        this.currentSortField,
        this.currentSortDirection
      )
      .pipe(
        takeUntil(this.destroy$),
        catchError((error) => {
          this.logger.error('Error loading more clients', error);
          this.toast.error(
            'Error al cargar más clientes. Por favor intente nuevamente.'
          );
          return of({ content: [], totalElements: 0 });
        })
      )
      .subscribe((data) => {
        let clientList = data.content as Client[];
        this.clientHelper.addContactInformation(clientList);
        this.quantity = data.totalElements;
        this.dataSourceClients.data = clientList; // Solo actualizar los datos, NO crear nuevo DataSource
        this.cdr.markForCheck();
      });
  }

  numericOnly(event: Event): void {
    const input = event.target as HTMLInputElement;
    input.value = input.value.replace(/[^0-9]/g, '');
  }

  get hasClientData(): boolean {
    return (
      Array.isArray(this.dataSourceClients?.data) &&
      this.dataSourceClients.data.length > 0
    );
  }

  private buildClientRequest(): ClientRequestDto {
    try {
      const companyIdentification =
        this.clientHelper.getCompanyIdentificationFromSession(
          environment.IDENTIFICATION
        );

      return {
        identification: this.form.get('identification')?.value ?? '',
        name: this.form.get('name')?.value ?? '',
        clientType: this.form.get('clientType')?.value ?? 0,
        status: this.form.get('status')?.value ?? true,
        companyIdentification: companyIdentification,
      };
    } catch (error) {
      this.logger.error('Error building client request', error);
      this.toast.error('Sesión expirada. Por favor inicie sesión nuevamente.');
      throw error;
    }
  }

  getClientTypeLabel(code: number): string {
    const type = this.clientTypes.find((t) => t.code === code);
    return type ? type.value : 'Desconocido';
  }

  openCreateDialog(): void {
    // ✅ Verificar si ya existe Consumidor Final antes de abrir el modal
    this.checkConsumidorFinalExists().then(
      ({ hasConsumidorFinal, companyEmail }) => {
        this.logger.debug('[ClientComponent] Datos para modal:', {
          hasConsumidorFinal,
          companyEmail: companyEmail ? 'presente' : 'vacío',
        });

        const dialogRef = this.dialog.open(UpdateClientComponent, {
          width: '700px',
          maxWidth: '90vw',
          maxHeight: '90vh',
          disableClose: true,
          data: {
            mode: 'create',
            hasConsumidorFinal,
            companyEmail,
          },
          panelClass: 'custom-dialog-container',
          autoFocus: false,
        });

        dialogRef.afterClosed().subscribe((result) => {
          if (result) {
            // Refresh the list after creating
            this.search();
          }
        });
      }
    );
  }

  /**
   * ✅ Verifica si ya existe un cliente Consumidor Final activo para la compañía
   * Retorna también el email de la compañía para pre-llenar el campo
   */
  private async checkConsumidorFinalExists(): Promise<{
    hasConsumidorFinal: boolean;
    companyEmail: string;
  }> {
    const companyEmail = this.getCompanyEmail();

    try {
      const companyIdentification =
        this.clientHelper.getCompanyIdentificationFromSession(
          environment.IDENTIFICATION
        );

      // ✅ Búsqueda por identificación específica del Consumidor Final (9999999999999)
      const request = {
        identification: '9999999999999', // Identificación fija de Consumidor Final
        name: '',
        clientType: 0,
        status: true,
        companyIdentification,
      };

      const result = await this.clientService
        .searchPageable(0, 10, request as any, '', '')
        .pipe(
          catchError((err) => {
            this.logger.warn('Error verificando consumidor final:', err);
            return of({ content: [], totalElements: 0 });
          })
        )
        .toPromise();

      // ✅ Verificar que realmente existe un cliente con idType=4 (Consumidor Final)
      const clients = (result?.content || []) as Client[];
      const consumidorFinal = clients.find(
        (c) => c.idType === ID_TYPE_CONSUMIDOR_FINAL && c.status === true
      );

      const hasConsumidorFinal = !!consumidorFinal;

      this.logger.debug('✅ Verificación Consumidor Final:', {
        hasConsumidorFinal,
        companyEmail,
        clientsFound: clients.length,
        consumidorFinalFound: consumidorFinal?.identification,
      });

      return { hasConsumidorFinal, companyEmail };
    } catch (error) {
      this.logger.error('Error en checkConsumidorFinalExists:', error);
      return { hasConsumidorFinal: false, companyEmail };
    }
  }

  /**
   * ✅ Obtiene el email de la compañía desde localStorage
   */
  private getCompanyEmail(): string {
    try {
      const contactsJson = localStorage.getItem('COMPANY_CONTACTS');
      this.logger.debug('📧 COMPANY_CONTACTS raw:', contactsJson);

      if (!contactsJson) {
        this.logger.warn('📧 COMPANY_CONTACTS no existe en localStorage');
        return '';
      }

      const contacts = JSON.parse(contactsJson);
      this.logger.debug('📧 Contactos parseados:', contacts);

      // Buscar contacto de tipo email (MAI)
      const mailContact = contacts.find(
        (c: any) =>
          c.contactType === CONTACT_TYPE_MAIL.code || c.contactType === 'MAI'
      );

      this.logger.debug('📧 Contacto email encontrado:', mailContact);

      const email = mailContact?.value || '';
      this.logger.debug('📧 Email de compañía:', email);

      return email;
    } catch (error) {
      this.logger.warn('Error obteniendo email de compañía:', error);
      return '';
    }
  }

  openEditDialog(idClient: number): void {
    const dialogRef = this.dialog.open(UpdateClientComponent, {
      width: '700px',
      maxWidth: '90vw',
      maxHeight: '90vh',
      disableClose: true,
      data: { mode: 'edit', idClient: idClient },
      panelClass: 'custom-dialog-container',
      autoFocus: false,
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        // Refresh the list after editing
        this.cdr.markForCheck();
      }
    });
  }

  /**
   * Copia el email del cliente al portapapeles
   */
  copyEmail(email: string): void {
    if (!email) {
      this.toast.warning('No hay email para copiar');
      return;
    }

    navigator.clipboard
      .writeText(email)
      .then(() => {
        this.toast.success('Email copiado: ' + email);
      })
      .catch((err) => {
        this.logger.error('Error copying email', err);
        this.toast.error('Error al copiar email');
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
