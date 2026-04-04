import { Component, OnInit, Inject, Optional } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { ActivatedRoute, Router } from '@angular/router';
import { User } from '../../../_model/auth/user';
import { UserService } from '../../../_service/user/user.service';
import { RoleService } from '../../../_service/role/role.service';
import { Role } from '../../../_model/auth/role';
import { Company } from '../../../_model/customer/company-establishment';
import { decodeToken } from '../../../_functions/functions';
import {
  EXIST_DATA,
  IDENTIFICATION_TYPE_CEDULA_LENGTH,
  IDENTIFICATION_TYPE_RUC_LENGTH,
  SUCCESS,
  SUPER_ADMIN_ROLE,
  UPDATE,
} from '../../../_constants/constants';
import { environment } from 'src/environments/environment';
import { LoggerService } from '../../../_service/logger/logger.service';
import { ToastService } from '../../../_service/notification/toast.service';
import { UserRoleService } from '../../../_service/auth/user-role.service';

@Component({
  selector: 'app-update-user',
  templateUrl: './update-user.component.html',
  styleUrls: ['./update-user.component.css'],
})
export class UpdateUserComponent implements OnInit {
  idUser = 0;
  user: User = {} as User;
  roles: Role[] = [];
  edition = false;
  idRole = 0;
  userNameLast = '';
  mailLast = '';
  idRetrievedCompanyUser: string | null;
  identificationMaxlength: number = IDENTIFICATION_TYPE_RUC_LENGTH.value;
  identificationMinlength: number = IDENTIFICATION_TYPE_CEDULA_LENGTH.value;
  form: FormGroup;
  hidePassword = true; // Para mostrar/ocultar password
  companies: Company[] = [];
  allowCompanySelection = false; // Si debe mostrar selector de compañía
  selectedCompanyId: number | undefined;

  constructor(
    private userService: UserService,
    private roleService: RoleService,
    private logger: LoggerService,
    private toast: ToastService,
    private router: Router,
    private route: ActivatedRoute,
    private userRoleService: UserRoleService,
    @Optional() public dialogRef: MatDialogRef<UpdateUserComponent>,
    @Optional()
    @Inject(MAT_DIALOG_DATA)
    public data?: {
      user?: User;
      isEdit: boolean;
      companies?: Company[];
      allowCompanySelection?: boolean;
      preselectedCompanyId?: number;
      companyId?: number; // ID de la compañía a la que pertenece el usuario (para edición)
    }
  ) {}

  ngOnInit(): void {
    this.idRetrievedCompanyUser = localStorage.getItem('companyIdCompanyUser');

    this.user = {} as User;

    // Determinar si es diálogo o ruta
    const isDialog = !!this.dialogRef;
    const isRoute = !isDialog;

    if (isRoute) {
      // Modo ruta: obtener datos de la ruta
      const idUserParam = this.route.snapshot.paramMap.get('idUser');
      this.edition = !!idUserParam;

      this.logger.debug(
        '🔍 UpdateUserComponent.ngOnInit() - Modo RUTA - edition:',
        this.edition,
        'idUser:',
        idUserParam
      );

      if (this.edition && idUserParam) {
        // Cargar usuario desde el backend
        this.idUser = parseInt(idUserParam, 10);
        this.loadUserById(this.idUser);
      }
    } else {
      // Modo diálogo: usar datos del diálogo
      this.edition = this.data?.isEdit || false;

      this.logger.debug(
        '🔍 UpdateUserComponent.ngOnInit() - Modo DIÁLOGO - edition:',
        this.edition
      );
      this.logger.debug(
        '🔍 UpdateUserComponent.ngOnInit() - data.user:',
        this.data?.user
      );
    }

    // Inicializar propiedades de compañía si vienen en el data
    if (isDialog && this.data) {
      this.companies = this.data.companies || [];
      this.allowCompanySelection = this.data.allowCompanySelection || false;
      this.selectedCompanyId = this.data.preselectedCompanyId;
    }

    // Inicializar form SIEMPRE primero con valores vacíos
    // Password solo es requerido en creación, no en edición
    // CompanyId es requerido solo si allowCompanySelection es true
    const formControls: any = {
      id: new FormControl(0),
      name: new FormControl('', Validators.required),
      userName: new FormControl('', Validators.required),
      password: new FormControl('', this.edition ? [] : [Validators.required]),
      email: new FormControl('', [Validators.required, Validators.email]),
      status: new FormControl(true),
    };

    // Agregar control de compañía si se permite selección
    if (this.allowCompanySelection && !this.edition) {
      formControls.companyId = new FormControl(
        this.selectedCompanyId || null,
        Validators.required
      );
    }

    this.form = new FormGroup(formControls);

    this.findRoles();

    // Si es diálogo y hay datos de edición, cargarlos
    if (isDialog && this.edition && this.data?.user) {
      this.idUser = this.data.user.idUser;
      this.logger.debug(
        '✅ Modo EDICIÓN (DIÁLOGO) - Cargando datos del usuario:',
        this.idUser
      );
      this.initForm();
    } else if (isDialog) {
      this.logger.debug('✅ Modo NUEVO (DIÁLOGO) - Form vacío');
    }
  }

  /**
   * Carga un usuario por ID desde el backend (modo ruta)
   */
  loadUserById(idUser: number): void {
    this.userService.findById(idUser).subscribe({
      next: (response) => {
        if (response && response.userDto) {
          const user = response.userDto;
          this.data = { user, isEdit: true };
          this.initForm();
        } else {
          this.logger.error('Usuario no encontrado');
          this.toast.error('Usuario no encontrado');
          this.router.navigate(['/users']);
        }
      },
      error: (err) => {
        this.logger.error('Error al cargar usuario', err);
        this.toast.error('Error al cargar el usuario');
        this.router.navigate(['/users']);
      },
    });
  }

  initForm() {
    if (this.edition && this.data?.user) {
      // Usar this.data.user directamente (sincrónico)
      const user = this.data.user;
      this.idRole =
        user.roleDtos && user.roleDtos.length > 0 ? user.roleDtos[0].idRole : 0;
      this.userNameLast = user.userName;
      this.mailLast = user.mail;

      // Usar patchValue() en lugar de recrear el FormGroup
      this.form.patchValue({
        id: user.idUser,
        name: user.name || '',
        userName: user.userName || '',
        password: user.password || '', // Mostrar password actual (super admin)
        email: user.mail || '',
        status: user.status !== undefined ? user.status : true,
      });

      // Marcar form como touched para forzar elevación de labels
      this.form.markAllAsTouched();
    }
  }

  findRoles() {
    // Usar UserRoleService para obtener roles de la compañía actual
    this.userRoleService.getCurrentCompanyRoles().subscribe({
      next: (roles) => {
        if (roles.includes('super_admin')) {
          this.roleService.findAll().subscribe((data) => {
            if (data.code == 0 && data.roleDtos.length > 0) {
              this.roles = data.roleDtos;
            }
          });
        } else {
          this.roleService.findAdministration().subscribe((data) => {
            if (data.code == 0 && data.roleDtos.length > 0) {
              this.roles = data.roleDtos;
            }
          });
        }
      },
      error: (err) => {
        this.logger.error('Error al cargar roles:', err);
        this.toast.error(
          'Error al cargar roles. Por favor, intente nuevamente.'
        );
      },
    });
  }

  validateSave() {
    if (this.edition && this.userNameLast === this.form.value['userName']) {
      this.save();
    } else {
      this.validateUserName();
    }
  }

  validateUserName() {
    this.userService
      .existsByUserName(this.form.value['userName'])
      .subscribe((data) => {
        if (data) {
          this.toast.warning(EXIST_DATA.MESSAGE_EXISTS_USER.message);
        } else {
          this.validateMail();
        }
      });
  }

  validateMail() {
    if (this.edition && this.mailLast === this.form.value['email']) {
      this.save();
    } else {
      this.userService
        .existsByMail(this.form.value['email'])
        .subscribe((data) => {
          if (data) {
this.toast.warning(EXIST_DATA.MESSAGE_EXISTS_MAIL.message);
          } else {
            this.save();
          }
        });
    }
  }

  save() {
    // Usar UserRoleService para obtener roles de la compañía actual
    const roles = this.userRoleService.getCurrentRolesSync();
    
    if (!roles || roles.length === 0) {
      // Si no hay roles cargados, intentar cargarlos de forma asíncrona
      this.userRoleService.getCurrentCompanyRoles().subscribe({
        next: (loadedRoles) => {
          this.performSave(loadedRoles);
        },
        error: (err) => {
          this.logger.error('Error al cargar roles:', err);
          this.toast.error(
            'Error al cargar roles. Por favor, intente nuevamente.'
          );
        },
      });
      return;
    }

    this.performSave(roles);
  }

  private performSave(roles: string[]) {
    let idCompany = 0;

    // Si se permite selección de compañía y hay una seleccionada en el form, usarla
    if (this.allowCompanySelection && this.form.get('companyId')?.value) {
      idCompany = this.form.get('companyId')?.value;
      this.logger.debug('ID de compañía seleccionada en el modal:', idCompany);
    } else if (this.edition && this.data?.companyId) {
      // Modo edición: usar el companyId pasado en el data (compañía a la que pertenece el usuario)
      idCompany = this.data.companyId;
      this.logger.debug('ID de compañía del usuario (desde data):', idCompany);
    } else if (roles.find((role) => role !== SUPER_ADMIN_ROLE.description)) {
      // Usuario no super admin: usar su compañía
      idCompany = parseInt(localStorage.getItem(environment.ID_COMPANY)!);
    } else {
      // Super admin: usar compañía de localStorage o la seleccionada
      const storedCompanyId = localStorage.getItem('companyIdCompanyUser');
      idCompany = storedCompanyId
        ? parseInt(storedCompanyId)
        : this.selectedCompanyId || 0;
    }
    this.logger.debug('ID de compañía para guardar usuario:', idCompany);

    this.user.idUser = this.idUser;
    this.user.name = this.form.value['name'];
    this.user.userName = this.form.value['userName'];

    // Solo actualizar password si no está vacío
    const passwordValue = this.form.value['password'];
    if (passwordValue && passwordValue.trim() !== '') {
      this.user.password = passwordValue;
    }

    this.user.status = this.form.value['status'];
    this.user.mail = this.form.value['email'];
    this.user.roleDtos = this.addRoles();
    this.user.idCompany = idCompany;

    if (this.user.idUser > 0) {
      this.userService.update(this.user, this.user.idUser).subscribe({
        next: () => {
          // ✅ Mostrar toast personalizado de éxito
          this.toast.success(UPDATE.MESSAGE_UPDATE_USER.message);
          this.closeDialogOrNavigate(true);
        },
        error: (err) => {
          this.logger.error('Error al actualizar usuario', err);
          this.toast.httpError(err, 'actualizar el usuario');
        },
      });
    } else {
      this.userService.save(this.user).subscribe({
        next: () => {
          // ✅ Mostrar toast personalizado de éxito
          this.toast.success(SUCCESS.MESSAGE_REGISTER_USER.message);
          this.closeDialogOrNavigate(true);
        },
        error: (err) => {
          this.logger.error('Error al guardar usuario', err);
          this.toast.httpError(err, 'guardar el usuario');
        },
      });
    }
  }

  cancel(): void {
    this.closeDialogOrNavigate(false);
  }

  /**
   * Cierra el diálogo si está en modo diálogo, o navega de vuelta si está en modo ruta
   */
  private closeDialogOrNavigate(result: boolean): void {
    if (this.dialogRef) {
      // Modo diálogo: cerrar el diálogo con el companyId si se seleccionó uno
      if (this.allowCompanySelection && this.form.get('companyId')?.value) {
        // Si hay compañía seleccionada, retornar objeto con result y companyId
        this.dialogRef.close({
          success: result,
          companyId: this.form.get('companyId')?.value,
        });
      } else {
        // Si no hay compañía seleccionada, retornar solo el booleano
        this.dialogRef.close(result);
      }
    } else {
      // Modo ruta: navegar de vuelta a la lista de usuarios
      this.router.navigate(['/users']);
    }
  }

  addRoles(): Role[] {
    let role = {} as Role;
    role.idRole = this.idRole;
    let roles = [];
    roles.push(role);
    return roles;
  }

  get f() {
    return this.form.controls;
  }
}
