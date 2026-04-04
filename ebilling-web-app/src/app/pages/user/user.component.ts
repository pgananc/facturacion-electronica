import {
  Component,
  OnInit,
  ViewChild,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
} from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { MatTableDataSource } from '@angular/material/table';
import { User } from '../../_model/auth/user';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { UserService } from '../../_service/user/user.service';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute } from '@angular/router';
import { DELETE, EMPTY_DATA } from '../../_constants/constants';
import { environment } from 'src/environments/environment';
import { catchError } from 'rxjs/operators';
import { of } from 'rxjs';
import { UpdateUserComponent } from './update-user/update-user.component';
import { LoggerService } from '../../_service/logger/logger.service';
import { decodeToken } from '../../_functions/functions';
import { ToastService } from '../../_service/notification/toast.service';

/**
 * UserComponent - Lista y gestión de usuarios
 *
 * Optimizado con OnPush Change Detection para mejor performance
 */
@Component({
  selector: 'app-user',
  templateUrl: './user.component.html',
  styleUrls: ['./user.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserComponent implements OnInit {
  form: FormGroup;

  displayedColumns = [
    'name',
    'userName',
    'mail',
    'roleDtos',
    'status',
    'actions',
  ];
  dataSource = new MatTableDataSource<User>([]);
  status: boolean = true;
  quantity: number = 0;

  @ViewChild(MatPaginator, { static: true }) paginator: MatPaginator;
  @ViewChild(MatSort, { static: true }) sort: MatSort;

  constructor(
    private readonly userService: UserService,
    public route: ActivatedRoute,
    private readonly cdr: ChangeDetectorRef,
    private readonly dialog: MatDialog,
    private readonly logger: LoggerService,
    private readonly toast: ToastService
  ) {}

  ngOnInit(): void {
    // Inicializar form primero
    this.form = new FormGroup({
      name: new FormControl(''),
      userName: new FormControl(''),
    });
    this.status = true;

    // Validar token antes de cargar datos
    const decodedToken = decodeToken();
    if (!decodedToken) {
      this.logger.warn('No hay token válido en UserComponent.ngOnInit()');
      return;
    }

    // Inicializar dataSource vacío
    this.dataSource = new MatTableDataSource<User>([]);
    this.dataSource.sort = this.sort;
    this.dataSource.paginator = this.paginator;

    this.userService.userChange.subscribe((data) => {
      this.dataSource.data = data;
      this.cdr.markForCheck();
    });

    let user = {} as User;
    user.userName = this.form.value['name'];
    user.name = this.form.value['userName'];
    user.status = this.status;
    user.idCompany = Number(localStorage.getItem(environment.ID_COMPANY));

    this.userService
      .searchPageable(0, 10, user)
      .pipe(
        catchError((error) => {
          this.logger.error('Error inicial al cargar usuarios:', error);
          return of(null);
        })
      )
      .subscribe((data) => {
        if (data) {
          this.quantity = data.totalElements;
          this.dataSource.data = data.content;
          this.cdr.markForCheck();
        }
      });
  }

  search() {
    // Reset to first page when searching
    this.paginator.pageIndex = 0;

    let user = {} as User;
    user.name = this.form.value['name'];
    user.userName = this.form.value['userName'];
    user.status = this.status;
    user.idCompany = Number(localStorage.getItem(environment.ID_COMPANY));
    this.userService
      .searchPageable(0, 10, user)
      .pipe(
        catchError((error) => {
          this.toast.error('Error al buscar usuarios');
          return of(null);
        })
      )
      .subscribe((data) => {
        if (data != null) {
          this.quantity = data.totalElements;
          this.dataSource.data = data.content;
          this.cdr.markForCheck();
        } else {
          this.toast.info(EMPTY_DATA.MESSAGE_EMPTY_DATA.message);
        }
      });
  }

  limpiar() {
    this.form.reset({
      name: '',
      userName: '',
    });
    this.status = true;
    this.search();
  }

  delete(idUser: number) {
    this.userService.delete(idUser).subscribe({
      next: () => {
      const userDtos = this.dataSource.data.filter(
        (user) => user.idUser !== idUser
      );
      this.userService.userChange.next(userDtos);
        // ✅ Mostrar toast personalizado de éxito
        this.toast.success(DELETE.MESSAGE_DELETE_USER.message);
      },
      error: (error) => {
        this.logger.error('Error al eliminar usuario', error);
        this.toast.httpError(error, 'eliminar el usuario');
      },
    });
  }

  showMore(e: any) {
    let user = {} as User;
    user.name = this.form.value['name'];
    user.userName = this.form.value['userName'];
    user.status = this.status;
    user.idCompany = Number(localStorage.getItem(environment.ID_COMPANY));
    this.userService
      .searchPageable(e.pageIndex, e.pageSize, user)
      .pipe(
        catchError((error) => {
          this.toast.error('Error al cargar usuarios');
          return of(null);
        })
      )
      .subscribe((data) => {
        if (data != null) {
          this.quantity = data.totalElements;
          this.dataSource.data = data.content;
          this.cdr.markForCheck();
        }
      });
  }

  /**
   * Abre el modal para crear o editar un usuario
   */
  openDialog(user?: User): void {
    const dialogRef = this.dialog.open(UpdateUserComponent, {
      width: '700px',
      maxWidth: '90vw',
      maxHeight: '90vh',
      disableClose: false,
      autoFocus: false,
      data: user ? { user, isEdit: true } : { isEdit: false },
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
}
