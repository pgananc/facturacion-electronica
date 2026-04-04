import {
  Component,
  OnInit,
  ViewChild,
  ChangeDetectorRef,
  ChangeDetectionStrategy,
} from '@angular/core';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { MatDialog } from '@angular/material/dialog';
import { ToastService } from '../../../_service/notification/toast.service';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import {
  EMPTY_DATA,
  SUPER_ADMIN_ROLE,
} from '../../../_constants/constants';
import { FormGroup } from '@angular/forms';
import { CompanyService } from '../../../_service/customer/company.service';
import { User } from '../../../_model/auth/user';
import { CompanyUserService } from '../../../_service/auth/company-user.service';
import { UserService } from '../../../_service/user/user.service';
import { UpdateUserComponent } from '../../user/update-user/update-user.component';
import { environment } from '../../../../environments/environment';
import { decodeToken } from '../../../_functions/functions';
import { Company } from '../../../_model/customer/company-establishment';
import { LoggerService } from '../../../_service/logger/logger.service';
import { catchError } from 'rxjs/operators';
import { forkJoin, of } from 'rxjs';
import { UserRoleService } from '../../../_service/auth/user-role.service';

@Component({
  selector: 'app-company',
  templateUrl: 'company-user.component.html',
  styleUrls: ['company-user.component.css'],
})
export class CompanyUserComponent implements OnInit {
  form: FormGroup;
  companies: Company[] = [];
  companyId: number | undefined;
  userId: number;
  displayedColumnsClient = [
    'name',
    'username',
    'mail',
    'role',
    'status',
    'actions',
  ];
  displayedColumnsAvailable = ['name', 'username', 'mail', 'status', 'actions'];
  dsCurrentUsersInCompany: MatTableDataSource<User>;
  dsAvailableUsers: MatTableDataSource<User>;
  status = true;
  quantityCurrentUsers = 0;
  quantityAvailableUsers = 0;
  isAdmin: boolean;
  loggedUser: string;

  @ViewChild('paginatorCompanyUsers', { static: true })
  paginatorCurrent: MatPaginator;
  @ViewChild('paginatorAvailableUsersForCompany', { static: true })
  paginatorAvailable: MatPaginator;

  @ViewChild('sortCompanyUsers', { static: true }) sortCurrent: MatSort;
  @ViewChild('sortAvailableUsers', { static: true }) sortAvailable: MatSort;

  constructor(
    private readonly companyService: CompanyService,
    private readonly companyUserService: CompanyUserService,
    private readonly userService: UserService,
    private readonly toast: ToastService,
    public route: ActivatedRoute,
    private readonly logger: LoggerService,
    private readonly cdr: ChangeDetectorRef,
    private readonly dialog: MatDialog,
    private readonly userRoleService: UserRoleService
  ) {}

  ngOnInit() {
    // Inicializar el formulario PRIMERO (antes de cualquier validación)
    // Esto evita errores si el template intenta renderizarse
    this.form = new FormGroup({});

    // Inicializar datasources vacíos para evitar errores en el template
    this.dsCurrentUsersInCompany = new MatTableDataSource<User>([]);
    this.dsAvailableUsers = new MatTableDataSource<User>([]);

    const decodedToken = decodeToken();

    // Protección: Si no hay token válido, no continuar
    if (!decodedToken) {
      this.logger.warn(
        'No hay token válido en CompanyUserComponent.ngOnInit()'
      );
      return;
    }

    this.loggedUser = decodedToken.userName;

    // Cargar roles de la compañía actual usando el servicio centralizado
    this.userRoleService.getCurrentCompanyRoles().subscribe({
      next: (roles) => {
        // Verificar si el usuario es super_admin en la compañía actual
        const isSuperAdmin = roles.includes(SUPER_ADMIN_ROLE.name);
        
        if (!isSuperAdmin) {
          this.isAdmin = false;
          this.companyId = parseInt(localStorage.getItem(environment.ID_COMPANY)!);
          this.search();
        } else {
          this.isAdmin = true;
        }
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.logger.error('Error al cargar roles:', err);
        // Fallback: asumir que no es admin si falla la carga
        this.isAdmin = false;
        this.companyId = parseInt(localStorage.getItem(environment.ID_COMPANY)!);
        this.search();
        this.cdr.markForCheck();
      },
    });

    if (this.companies.length === 0) {
      this.companyService.findAll().subscribe((data) => {
        this.companies = data.companyDetailsDtos;
      });
    }
  }

  setCompanyId() {
    if (this.companyId) {
      localStorage.setItem('companyIdCompanyUser', this.companyId.toString());
      this.logger.debug('Company ID seleccionado:', this.companyId);
    }
  }

  /**
   * Abre el modal para crear un nuevo usuario
   */
  openDialog(): void {
    const dialogRef = this.dialog.open(UpdateUserComponent, {
      width: '700px',
      maxWidth: '90vw',
      maxHeight: '90vh',
      disableClose: false,
      autoFocus: false,
      data: {
        isEdit: false,
        companies: this.companies, // Pasar lista de compañías para selección
        allowCompanySelection: true, // Indicar que debe mostrar selector de compañía
        preselectedCompanyId: this.companyId, // Pasar compañía preseleccionada si existe
      },
    });

    dialogRef.afterClosed().subscribe((result) => {
      // result puede ser: boolean (true/false) o { success: boolean, companyId: number }
      const success = typeof result === 'boolean' ? result : result?.success;
      const companyId =
        typeof result === 'object' ? result?.companyId : undefined;

      if (success) {
        this.logger.debug('✅ Modal cerrado con cambios, refrescando listas');
        // Si se seleccionó una compañía en el modal, actualizar y refrescar
        if (companyId) {
          this.companyId = companyId;
          this.setCompanyId();
          this.search();
        } else if (this.companyId) {
          // Si ya había una compañía seleccionada, solo refrescar
          this.search();
        }
      } else {
        this.logger.debug('❌ Modal cerrado sin cambios');
      }
    });
  }

  /**
   * Abre el modal para editar un usuario existente
   */
  openEditDialog(user: User): void {
    if (!this.companyId) {
      this.toast.warning('No hay compañía seleccionada');
      return;
    }

    // Asegurar que el companyId esté guardado en localStorage antes de abrir el modal
    this.setCompanyId();

    const dialogRef = this.dialog.open(UpdateUserComponent, {
      width: '700px',
      maxWidth: '90vw',
      maxHeight: '90vh',
      disableClose: false,
      autoFocus: false,
      data: {
        user,
        isEdit: true,
        companyId: this.companyId, // Pasar el ID de la compañía seleccionada/buscada
      },
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.logger.debug('✅ Modal cerrado con cambios, refrescando listas');
        // Refrescar ambas listas después de editar un usuario
        if (this.companyId) {
          // Limpiar datasources antes de refrescar para forzar actualización completa
          this.dsCurrentUsersInCompany = new MatTableDataSource<User>([]);
          this.cdr.detectChanges();
          // Llamar a search() después de un pequeño delay para asegurar que el backend haya procesado la actualización
          setTimeout(() => {
            this.search();
          }, 100);
        }
      } else {
        this.logger.debug('❌ Modal cerrado sin cambios');
      }
    });
  }

  delete(companyId: number | undefined, userId: number) {
    if (!companyId) {
      this.toast.warning('No hay compañía seleccionada');
      return;
    }

    this.companyUserService.delete(companyId, userId).subscribe({
      next: () => {
        const userDel = this.dsCurrentUsersInCompany.data.find(
          (user) => user.idUser === userId
        );
        if (userDel) {
          this.dsAvailableUsers.data.push(userDel);
          this.dsAvailableUsers.data = [...this.dsAvailableUsers.data];
        }
        this.dsCurrentUsersInCompany.data =
          this.dsCurrentUsersInCompany.data.filter(
            (user) => user.idUser !== userId
          );
        this.quantityCurrentUsers--;
        this.quantityAvailableUsers++;
        this.toast.success('Usuario eliminado de la compañía exitosamente');
        this.cdr.detectChanges();
      },
      error: (error: HttpErrorResponse) => {
        this.logger.error('Error al eliminar usuario de compañía', error);
        this.toast.httpError(error, 'eliminar el usuario de la compañía');
      },
    });
  }

  applyFilter(event: Event) {
    const input = (event.target as HTMLInputElement).value;
    this.dsAvailableUsers!.filter = input.trim().toLowerCase();
  }

  search() {
    if (!this.companyId) {
      this.toast.warning('Por favor seleccione una compañía');
      return;
    }

    // Limpiar completamente el datasource antes de buscar
    this.dsCurrentUsersInCompany = new MatTableDataSource<User>([]);
    this.quantityCurrentUsers = 0;
    this.cdr.detectChanges();

    // Buscar usuarios de la compañía
    this.companyUserService
      .searchPageable(0, 10, this.companyId)
      .pipe(
        catchError((error) => {
          // Si es error 500 con mensaje de índice fuera de rango, es porque no hay usuarios
          // Esto NO es un error, es un caso válido (compañía sin usuarios)
          const errorMessage = error?.error?.message || error?.message || '';
          const isIndexOutOfBounds =
            error?.status === 500 &&
            errorMessage.includes('Index') &&
            errorMessage.includes('out of bounds');

          if (isIndexOutOfBounds) {
            // Caso válido: compañía sin usuarios - no es un error
            this.logger.debug(
              'Compañía sin usuarios (caso válido, no es error)'
            );
            // Obtener nombre de la compañía para el mensaje informativo
            const selectedCompany = this.companies.find(
              (c) => c.idCompany === this.companyId
            );
            const companyName = selectedCompany?.name || 'seleccionada';
            // Mostrar mensaje informativo con el nombre de la compañía
            this.toast.info(
              `La compañía "${companyName}" no tiene usuarios asignados aún`
            );
            this.quantityCurrentUsers = 0;
            this.dsCurrentUsersInCompany = new MatTableDataSource<User>([]);
            this.cdr.detectChanges();
            // Continuar con la búsqueda de usuarios disponibles
            this.loadAvailableUsers();
            return of(null);
          } else {
            // Para otros errores reales, mostrar mensaje
            this.logger.error('Error al buscar usuarios de compañía:', error);
            this.toast.httpError(error, 'cargar usuarios de la compañía');
            // Aún así, intentar cargar usuarios disponibles
            this.loadAvailableUsers();
            return of(null);
          }
        })
      )
      .subscribe((data) => {
        if (data != null) {
          this.quantityCurrentUsers = data.totalElements;

          const content = data.content || []; // evita null

          // Si hay usuarios, obtener los roles de cada uno en el contexto de la compañía
          if (content.length > 0 && this.companyId) {
            // Crear array de observables para obtener detalles completos de cada usuario
            // Usar el endpoint correcto: GET /api/user-company/user/{idUser}/company/{idCompany}
            const userDetailRequests = content.map((user: User) =>
              this.companyUserService
                .findByIdCompanyAndIdUser(this.companyId!, user.idUser)
                .pipe(
                  catchError((error) => {
                    this.logger.warn(
                      `No se pudieron obtener los roles del usuario ${user.idUser} en la compañía ${this.companyId}`,
                      error
                    );
                    // Retornar el usuario original si falla la obtención de detalles
                    return of({ userDtos: [user], userDto: null });
                  })
                )
            );

            // Ejecutar todas las peticiones en paralelo
            forkJoin(userDetailRequests).subscribe(
              (responses) => {
                const responsesArray = responses as any[];

                // Combinar los usuarios originales con sus roles obtenidos
                const usersWithRoles = content.map(
                  (user: User, index: number) => {
                    const response = responsesArray[index];
                    // El endpoint retorna userDtos (array) con el usuario y sus roles en el contexto de la compañía
                    const userDetail =
                      response?.userDtos?.[0] || response?.userDto || user;

                    // Si el detalle tiene roles, usarlos; si no, mantener el usuario original
                    const roleDtos =
                      userDetail?.roleDtos || user.roleDtos || [];

                    const finalUser: User = {
                      ...user,
                      roleDtos: roleDtos,
                    };

                    return finalUser;
                  }
                );

                // Crear nuevo datasource con los usuarios actualizados
                this.dsCurrentUsersInCompany = new MatTableDataSource(
                  usersWithRoles
                );
                this.dsCurrentUsersInCompany.sort = this.sortCurrent!;
                // Forzar actualización del paginador si existe
                if (this.paginatorCurrent) {
                  this.dsCurrentUsersInCompany.paginator =
                    this.paginatorCurrent;
                }
                // Forzar detección de cambios
                this.cdr.detectChanges();
                // Forzar actualización adicional del datasource
                this.dsCurrentUsersInCompany.data = [...usersWithRoles];
              },
              (error: any) => {
                this.logger.error(
                  'Error al obtener detalles de usuarios:',
                  error
                );
                // Si falla, usar los usuarios sin roles
                this.dsCurrentUsersInCompany = new MatTableDataSource(content);
                this.dsCurrentUsersInCompany.sort = this.sortCurrent!;
                this.cdr.detectChanges();
              }
            );
          } else {
            // Si no hay usuarios, inicializar vacío
            this.dsCurrentUsersInCompany = new MatTableDataSource<User>([]);
            this.cdr.detectChanges();
          }
        } else {
          // Si data es null (error manejado como "sin usuarios"), ya se inicializó arriba
          // Solo asegurar que esté vacío
          this.dsCurrentUsersInCompany = new MatTableDataSource<User>([]);
        }

        // SIEMPRE cargar usuarios disponibles, independientemente del resultado anterior
        this.loadAvailableUsers();
      });
  }

  /**
   * Carga los usuarios disponibles para agregar a la compañía
   */
  private loadAvailableUsers(): void {
    if (!this.companyId) {
      return;
    }

    this.userService
      .findActiveUsersAndNotInCompany(this.companyId)
      .pipe(
        catchError((error) => {
          this.logger.error('Error al buscar usuarios disponibles:', error);
          this.toast.httpError(error, 'cargar usuarios disponibles');
          return of({ userDtos: [] });
        })
      )
      .subscribe((userResponse) => {
        const users: User[] = userResponse?.userDtos || [];
        this.quantityAvailableUsers = users.length;
        this.dsAvailableUsers = new MatTableDataSource<User>(users);
        this.dsAvailableUsers.sort = this.sortAvailable!;
        this.cdr.detectChanges();
      });
  }

  clearFilters() {
    this.companyId = undefined;
    this.dsCurrentUsersInCompany = new MatTableDataSource<User>([]);
    this.dsAvailableUsers = new MatTableDataSource<User>([]);
    this.quantityCurrentUsers = 0;
    this.quantityAvailableUsers = 0;
    this.cdr.detectChanges();
  }

  save(idUser: number) {
    if (!this.companyId) {
      this.toast.warning('No hay compañía seleccionada');
      return;
    }

    this.companyUserService
      .save({ idCompany: this.companyId, idUser: idUser })
      .subscribe({
        next: (data) => {
          if (data.code === 0) {
            // Remover el usuario de la lista de disponibles
            this.dsAvailableUsers.data = this.dsAvailableUsers.data.filter(
              (user) => user.idUser !== idUser
            );
            this.quantityAvailableUsers--;
            this.cdr.detectChanges();
            
            // Mostrar mensaje de éxito
            this.toast.success(
              'Usuario agregado a la compañía exitosamente'
            );
            
            // ✅ Usar search() para refrescar la lista completa con roles
            // Esto asegura que todos los usuarios (incluido el recién agregado) tengan sus roles correctos
            setTimeout(() => {
              this.search();
            }, 100);
          } else {
            this.toast.error('Error al agregar usuario a la compañía');
          }
        },
        error: (error: HttpErrorResponse) => {
          this.logger.error('Error al agregar usuario a compañía', error);
          this.toast.httpError(error, 'agregar el usuario a la compañía');
        },
      });
  }

  showMore(e: any) {
    if (!this.companyId) {
      this.toast.warning('No hay compañía seleccionada');
      return;
    }

    this.companyUserService
      .searchPageable(e.pageIndex, e.pageSize, this.companyId)
      .pipe(
        catchError((error) => {
          this.logger.error('Error al paginar usuarios:', error);
          this.toast.httpError(error, 'cargar más usuarios');
          return of(null);
        })
      )
      .subscribe((data) => {
        if (data != null) {
          this.quantityCurrentUsers = data.totalElements;
          const content = data.content || [];
          this.dsCurrentUsersInCompany = new MatTableDataSource<User>(content);
          this.dsCurrentUsersInCompany.sort = this.sortCurrent!;
          this.cdr.detectChanges();
        }
      });
  }
}
