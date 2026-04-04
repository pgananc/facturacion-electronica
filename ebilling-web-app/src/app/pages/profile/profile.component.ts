import { Component, OnInit, OnDestroy } from '@angular/core';
import { UserService } from 'src/app/_service/user/user.service';
import { UserRoleService } from '../../_service/auth/user-role.service';
import { environment } from 'src/environments/environment';
import { decodeToken } from '../../_functions/functions';
import { LoggerService } from '../../_service/logger/logger.service';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css'],
})
export class ProfileComponent implements OnInit, OnDestroy {
  userName = '';
  roles: string[] = [];
  mail = '';
  idUser: number = 0;
  private destroy$ = new Subject<void>();

  constructor(
    private userService: UserService,
    private userRoleService: UserRoleService,
    private logger: LoggerService
  ) {}

  ngOnInit(): void {
    this.obtainData();

    // Suscribirse a cambios en los roles (cuando se cambia de compañía)
    // Esta suscripción se actualizará automáticamente cuando se cambie de compañía
    this.userRoleService.currentRoles
      .pipe(takeUntil(this.destroy$))
      .subscribe((roles) => {
        this.roles = roles;
        this.logger.debug(
          '[ProfileComponent] Roles actualizados desde currentRoles$:',
          roles
        );
      });

    // ✅ Solo buscar usuario si tenemos userName válido
    if (this.userName) {
      this.userService.findUserByUserName(this.userName).subscribe({
        next: (data) => {
          this.mail = data.userDto.mail;
          // Si el idUser del token no está disponible, usar el del servicio
          if (!this.idUser && data.userDto.idUser) {
            this.idUser = data.userDto.idUser;
          }
          // Cargar roles del usuario en el contexto de la compañía actual
          // Esto actualizará currentRoles$ y la suscripción anterior recibirá los nuevos roles
          this.loadUserRolesInCurrentCompany();
        },
        error: (err) => {
          this.logger.error('[ProfileComponent] Error al buscar usuario:', err);
          // Aún así, intentar cargar roles si tenemos idUser del token
          if (this.idUser) {
            this.loadUserRolesInCurrentCompany();
          }
        },
      });
    } else if (this.idUser) {
      // Si tenemos idUser del token pero no userName, cargar roles directamente
      this.loadUserRolesInCurrentCompany();
    } else {
      // Si no tenemos idUser, intentar cargar roles de todas formas
      // Esto asegura que se carguen los roles incluso si no hay idUser del token
      this.loadUserRolesInCurrentCompany();
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  obtainData() {
    // ✅ Validar token antes de decodificar
    const token = localStorage.getItem(environment.TOKEN);
    if (!token) {
      console.warn('[ProfileComponent] No hay token disponible');
      return;
    }

    const decodedToken = decodeToken();
    if (!decodedToken) {
      console.warn(
        '[ProfileComponent] Token inválido o no se pudo decodificar'
      );
      return;
    }

    // ✅ Validar que decodedToken tiene userName antes de acceder
    if (decodedToken.userName) {
      this.userName = decodedToken.userName;
    }

    // Obtener idUser del token para cargar roles por compañía
    if (decodedToken.idUser) {
      this.idUser = decodedToken.idUser;
    }
  }

  /**
   * Carga los roles del usuario en el contexto de la compañía actual
   * Usa el servicio centralizado UserRoleService
   * Nota: Los roles se actualizarán automáticamente a través de la suscripción a currentRoles$
   */
  loadUserRolesInCurrentCompany(): void {
    // Usar getCurrentCompanyRoles que automáticamente obtiene el idCompany de localStorage
    // Esto actualizará currentRoles$ y la suscripción recibirá los nuevos roles
    this.userRoleService
      .getCurrentCompanyRoles()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (roles) => {
          // Los roles ya se actualizaron en currentRoles$ por el servicio
          // La suscripción a currentRoles$ recibirá estos roles automáticamente
          this.logger.debug(
            '[ProfileComponent] Roles cargados desde UserRoleService:',
            roles
          );
        },
        error: (err) => {
          this.logger.error(
            '[ProfileComponent] Error al cargar roles desde UserRoleService:',
            err
          );
          // No limpiar roles aquí, dejar que currentRoles$ maneje el estado
        },
      });
  }

  /**
   * Convierte el código del rol a un nombre amigable para mostrar
   */
  getRoleDisplayName(roleCode: string): string {
    const roleMap: { [key: string]: string } = {
      super_admin: 'Super Administrador',
      admin: 'Administrador',
      member: 'Miembro',
      accountant: 'Contador',
      operator: 'Operador',
    };
    return roleMap[roleCode] || roleCode;
  }

  /**
   * Obtiene la clase CSS para el badge del rol
   */
  getRoleBadgeClass(roleCode: string): string {
    const classMap: { [key: string]: string } = {
      super_admin: 'role-badge-super-admin',
      admin: 'role-badge-admin',
      member: 'role-badge-member',
      accountant: 'role-badge-accountant',
      operator: 'role-badge-operator',
    };
    return classMap[roleCode] || 'role-badge-default';
  }
}
