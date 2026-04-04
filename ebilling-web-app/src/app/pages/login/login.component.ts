import { environment } from '../../../environments/environment';
import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ToastService } from '../../_service/notification/toast.service';
import { Router } from '@angular/router';
import { LoginService } from '../../_service/auth/login.service';
import { MenuService } from '../../_service/auth/menu.service';
import { CompanyService } from '../../_service/customer/company.service';
import { decodeToken } from '../../_functions/functions';
import { CompanyFilter } from '../../_model/customer/company-filter';
import { LoggerService } from '../../_service/logger/logger.service';
import { HttpErrorResponse } from '@angular/common/http';
import { PermissionService } from '../../_service/permission/permission.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
  encapsulation: ViewEncapsulation.None,
})
export class LoginComponent implements OnInit {
  loginForm!: FormGroup;
  hidePassword: boolean = true; // Para toggle show/hide password
  isLoading: boolean = false; // Loading state para el botón
  isSuccess: boolean = false; // Success state para mostrar checkmark

  // Rate limiting visual
  failedAttempts: number = 0;
  maxAttempts: number = 5; // Máximo de intentos antes de bloquear
  isBlocked: boolean = false;
  blockUntil: Date | null = null;
  remainingBlockTime: number = 0; // Segundos restantes de bloqueo

  // Remember me
  rememberMe: boolean = false;

  constructor(
    private readonly fb: FormBuilder,
    private readonly loginService: LoginService,
    private readonly menuService: MenuService,
    private readonly companyService: CompanyService,
    private readonly toast: ToastService,
    private readonly router: Router,
    private readonly logger: LoggerService,
    private readonly permissionService: PermissionService
  ) {}

  ngOnInit() {
    // Solo limpiar localStorage si NO hay un token válido ya guardado
    const existingToken = localStorage.getItem(environment.TOKEN);

    if (!existingToken) {
      localStorage.clear();
    }

    // Cargar estado de rate limiting desde localStorage
    this.loadRateLimitState();

    // Verificar si está bloqueado
    this.checkBlockStatus();

    // Inicializar Reactive Form
    // NOTA: No validamos minLength en login porque las contraseñas existentes pueden tener cualquier longitud
    // La validación de longitud mínima solo aplica al CREAR/CAMBIAR contraseña
    this.loginForm = this.fb.group({
      userName: ['', [Validators.required, Validators.maxLength(256)]],
      password: ['', [Validators.required]], // Solo requerido, sin restricción de longitud
    });

    // Cargar datos de "Remember Me" si existen (después de inicializar el form)
    this.loadRememberedCredentials();

    this.loginService.messageChange.subscribe((data) => {
      this.toast.error(data);
    });
  }

  // Cargar estado de rate limiting
  private loadRateLimitState(): void {
    const savedAttempts = localStorage.getItem('login_failed_attempts');
    const savedBlockUntil = localStorage.getItem('login_block_until');

    if (savedAttempts) {
      this.failedAttempts = parseInt(savedAttempts, 10);
    }

    if (savedBlockUntil) {
      this.blockUntil = new Date(savedBlockUntil);
    }
  }

  // Verificar estado de bloqueo
  private checkBlockStatus(): void {
    if (this.blockUntil && this.blockUntil > new Date()) {
      this.isBlocked = true;
      this.updateFormControlsState();
      this.updateRemainingBlockTime();

      // Actualizar contador cada segundo
      const interval = setInterval(() => {
        this.updateRemainingBlockTime();
        if (!this.isBlocked) {
          clearInterval(interval);
        }
      }, 1000);
    } else {
      this.isBlocked = false;
      this.blockUntil = null;
      localStorage.removeItem('login_block_until');
      this.updateFormControlsState();
    }
  }

  // Actualizar estado de los controles del formulario basado en isBlocked
  private updateFormControlsState(): void {
    if (!this.loginForm) return;

    if (this.isBlocked) {
      this.loginForm.get('userName')?.disable();
      this.loginForm.get('password')?.disable();
    } else {
      this.loginForm.get('userName')?.enable();
      this.loginForm.get('password')?.enable();
    }
  }

  // Actualizar tiempo restante de bloqueo
  private updateRemainingBlockTime(): void {
    if (this.blockUntil) {
      const now = new Date();
      const diff = Math.max(
        0,
        Math.floor((this.blockUntil.getTime() - now.getTime()) / 1000)
      );
      this.remainingBlockTime = diff;

      if (diff === 0) {
        this.isBlocked = false;
        this.blockUntil = null;
        localStorage.removeItem('login_block_until');
        this.updateFormControlsState();
      }
    }
  }

  // Cargar credenciales guardadas (Remember Me)
  private loadRememberedCredentials(): void {
    const rememberedUser = localStorage.getItem('remembered_username');
    if (rememberedUser && this.loginForm) {
      this.rememberMe = true;
      // Cargar el usuario en el formulario
      this.loginForm.patchValue({ userName: rememberedUser });
      // No cargamos la contraseña por seguridad, solo el usuario
    }
  }

  // Guardar credenciales (Remember Me)
  private saveRememberedCredentials(): void {
    if (this.rememberMe) {
      const userName = this.loginForm.get('userName')?.value;
      if (userName) {
        localStorage.setItem('remembered_username', userName);
      }
    } else {
      localStorage.removeItem('remembered_username');
    }
  }

  // Getter para facilitar el acceso a los controles del formulario
  get f() {
    return this.loginForm.controls;
  }

  // Toggle para mostrar/ocultar contraseña
  togglePasswordVisibility(): void {
    this.hidePassword = !this.hidePassword;
  }

  // Manejar intento de login fallido
  private handleFailedLogin(): void {
    this.failedAttempts++;
    localStorage.setItem(
      'login_failed_attempts',
      this.failedAttempts.toString()
    );

    // Si alcanza el máximo de intentos, bloquear por 15 minutos
    if (this.failedAttempts >= this.maxAttempts) {
      const blockDuration = 15 * 60 * 1000; // 15 minutos en milisegundos
      this.blockUntil = new Date(Date.now() + blockDuration);
      this.isBlocked = true;
      localStorage.setItem('login_block_until', this.blockUntil.toISOString());

      // Deshabilitar controles del formulario
      this.updateFormControlsState();

      // Iniciar contador de bloqueo
      this.updateRemainingBlockTime();
      const interval = setInterval(() => {
        this.updateRemainingBlockTime();
        if (!this.isBlocked) {
          clearInterval(interval);
        }
      }, 1000);
    }
  }

  // Resetear contador de intentos fallidos (después de login exitoso)
  private resetFailedAttempts(): void {
    this.failedAttempts = 0;
    this.isBlocked = false;
    this.blockUntil = null;
    localStorage.removeItem('login_failed_attempts');
    localStorage.removeItem('login_block_until');

    // Habilitar controles del formulario
    this.updateFormControlsState();
  }

  // Getter para mostrar intentos restantes
  get remainingAttempts(): number {
    return Math.max(0, this.maxAttempts - this.failedAttempts);
  }

  // Getter para formato de tiempo de bloqueo
  get blockTimeFormatted(): string {
    if (!this.remainingBlockTime) return '';
    const minutes = Math.floor(this.remainingBlockTime / 60);
    const seconds = this.remainingBlockTime % 60;
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  }

  login() {
    // Verificar si está bloqueado
    if (this.isBlocked) {
      const minutes = Math.floor(this.remainingBlockTime / 60);
      const seconds = this.remainingBlockTime % 60;
      this.toast.warning(
        `Demasiados intentos fallidos. Por favor, espera ${minutes}:${seconds
          .toString()
          .padStart(2, '0')} antes de intentar nuevamente.`
      );
      return;
    }

    // Marcar todos los campos como touched para mostrar errores
    if (this.loginForm.invalid) {
      Object.keys(this.loginForm.controls).forEach((key) => {
        this.loginForm.get(key)?.markAsTouched();
      });

      // Validar que los campos no estén vacíos
      if (
        !this.loginForm.get('userName')?.value?.trim() ||
        !this.loginForm.get('password')?.value?.trim()
      ) {
        this.toast.warning(
          'Por favor, completa todos los campos requeridos para continuar.'
        );
      }
      return;
    }

    // Activar loading state y resetear success
    this.isLoading = true;
    this.isSuccess = false;

    // Obtener valores del formulario
    const userName = this.loginForm.get('userName')?.value?.trim() || '';
    const password = this.loginForm.get('password')?.value || '';

    // Log para debug - ver qué se está enviando
    this.logger.debug('Intentando login con:', {
      userName: userName,
      passwordLength: password?.length,
      passwordPreview: password ? '***' : 'empty',
    });

    this.loginService.login(userName, password).subscribe({
      next: (tokenResponse) => {
        if (tokenResponse.code == 0) {
          // Usuario/clave incorrecta - Incrementar contador de intentos fallidos
          this.handleFailedLogin();

          this.isLoading = false;
          this.isSuccess = false;

          const remainingAttempts = this.maxAttempts - this.failedAttempts;
          let errorMessage =
            'Las credenciales ingresadas no son correctas. Por favor, verifica tu usuario y contraseña.';

          if (remainingAttempts > 0 && remainingAttempts < this.maxAttempts) {
            errorMessage += ` (${remainingAttempts} ${
              remainingAttempts === 1
                ? 'intento restante'
                : 'intentos restantes'
            })`;
          }

          this.toast.error(errorMessage);
          this.loginService.messageChange.next('Usuario / clave incorrecta');
        } else if (tokenResponse.token?.token) {
          // Login exitoso - Resetear contador de intentos fallidos
          this.resetFailedAttempts();

          // Guardar credenciales si "Remember Me" está activado
          this.saveRememberedCredentials();

          localStorage.setItem(environment.TOKEN, tokenResponse.token.token);

          // ⚠️ IMPORTANTE: Invalidar todo el caché de permisos al iniciar sesión
          // Esto asegura que los permisos se validen contra el backend al momento de loguear
          this.permissionService.invalidateFeatureCache();
          this.permissionService.invalidateAllCaches();
          this.logger.debug(
            '[LoginComponent] ✅ Caché de permisos invalidado después del login'
          );

          let token = decodeToken();

          if (token?.companyEstablishments) {
            // Login exitoso - Feedback visual en botón (mejor práctica moderna)
            this.isLoading = false;
            this.isSuccess = true; // Mostrar checkmark en botón

            const company: CompanyFilter = {
              idCompany: 0,
              idType: 0,
              identification: '',
              name: '',
              nameCommercial: '',
              branchOfficeCode: '',
              status: true,
              forcedToAccounting: '',
              principal: '',
              isSuperAdmin: false,
            };
            this.loginService.setLoginStatus(true);
            this.menuService.menuChange.next([]);
            this.menuService.isLoginChange.next(false);
            this.companyService.companyNameChange.next('');
            this.companyService.companyChange.next(company);

            // Esperar 500ms para que usuario vea el feedback visual
            // Y asegurar que el token esté completamente guardado antes de navegar
            setTimeout(() => {
              // Verificar que el token esté guardado antes de navegar
              const savedToken = localStorage.getItem(environment.TOKEN);
              if (!savedToken) {
                this.logger.error(
                  'Token no encontrado en localStorage después del login'
                );
                return;
              }

              this.router
                .navigate(['/welcome'])
                .then((success) => {
                  if (!success) {
                    this.logger.warn(
                      'La ruta /welcome estuvo bloqueada o falló la navegación'
                    );
                  }
                })
                .catch((err) => this.logger.error('Error al navegar:', err));
            }, 500);
          } else {
            // Usuario sin compañía asignada
            this.isLoading = false;
            this.isSuccess = false;
            this.loginForm.reset(); // Limpiar formulario
            localStorage.clear();
            this.toast.warning(
              'Tu cuenta no tiene una compañía asignada. Por favor, contacta al administrador del sistema para resolver este problema.'
            );
            this.loginService.messageChange.next(
              'Usuario no tiene asignado una compañia. Comuníquese con el administrador.'
            );
          }
        }
      },
      error: (err: HttpErrorResponse) => {
        this.isLoading = false;
        this.isSuccess = false;

        // Log detallado para debugging
        this.logger.error('Error al realizar login', {
          status: err.status,
          statusText: err.statusText,
          error: err.error,
          message: err.message,
          url: err.url,
        });

        // Detectar si el error es por credenciales incorrectas
        // El backend puede devolver esto como error HTTP o en el body con code == 0
        const errorBody = err.error;
        let errorMessageStr = '';

        // Convertir el error body a string para buscar palabras clave
        if (typeof errorBody === 'string') {
          errorMessageStr = errorBody.toLowerCase();
        } else if (errorBody && typeof errorBody === 'object') {
          // Intentar extraer el mensaje de diferentes propiedades comunes
          const message =
            errorBody.message || errorBody.error || errorBody.status || '';
          // También convertir todo el objeto a string para buscar en todas las propiedades
          const fullErrorStr = JSON.stringify(errorBody).toLowerCase();
          errorMessageStr = message ? message.toLowerCase() : fullErrorStr;

          // Si el mensaje no tiene las palabras clave, buscar en el string completo
          if (
            !errorMessageStr.includes('usuario') &&
            !errorMessageStr.includes('password')
          ) {
            errorMessageStr = fullErrorStr;
          }
        }

        const isInvalidCredentials =
          err.status === 401 || // Unauthorized
          err.status === 403 || // Forbidden
          (err.status === 500 &&
            // Verificar si el código es 0 (indica credenciales incorrectas)
            (errorBody?.code === 0 ||
              // Verificar si el mensaje indica credenciales incorrectas
              (errorMessageStr &&
                (errorMessageStr.includes('usuario') ||
                  errorMessageStr.includes('contraseña') ||
                  errorMessageStr.includes('password') ||
                  errorMessageStr.includes('credenciales') ||
                  errorMessageStr.includes('incorrect') ||
                  errorMessageStr.includes('invalid') ||
                  errorMessageStr.includes('authentication') ||
                  errorMessageStr.includes('unauthorized') ||
                  errorMessageStr.includes('login failed') ||
                  errorMessageStr.includes('wrong') ||
                  errorMessageStr.includes('incorrecto') ||
                  errorMessageStr.includes('validar usuario') ||
                  errorMessageStr.includes('invalid user') ||
                  errorMessageStr.includes('user not found') ||
                  errorMessageStr.includes('bad credentials')))));

        let errorMessage =
          'No se pudo conectar con el servidor. Por favor, verifica tu conexión e intenta nuevamente.';

        if (isInvalidCredentials) {
          // Incrementar contador de intentos fallidos
          this.handleFailedLogin();

          // Mensaje específico para credenciales incorrectas
          const remainingAttempts = this.maxAttempts - this.failedAttempts;
          errorMessage =
            'Las credenciales ingresadas no son correctas. Por favor, verifica tu usuario y contraseña.';

          if (remainingAttempts > 0 && remainingAttempts < this.maxAttempts) {
            errorMessage += ` (${remainingAttempts} ${
              remainingAttempts === 1
                ? 'intento restante'
                : 'intentos restantes'
            })`;
          }
        } else {
          // Mensajes más específicos según el código de error
          if (err.status === 0) {
            errorMessage =
              'No se pudo conectar con el servidor. Verifica tu conexión a internet.';
          } else if (err.status === 500) {
            errorMessage =
              'Error en el servidor. Por favor, intenta nuevamente en unos momentos.';
          } else if (err.status === 503) {
            errorMessage =
              'Servicio temporalmente no disponible. Por favor, intenta más tarde.';
          } else if (err.error?.message) {
            errorMessage = err.error.message;
          } else if (err.message) {
            errorMessage = err.message;
          }
        }

        this.toast.error(errorMessage);
      },
    });
  }
}
