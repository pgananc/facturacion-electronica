import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { UserService } from '../../../_service/user/user.service';
import { ConfirmPasswordValidator } from '../../login/reset-password/token/confirm-password.validator';
import { decodeToken } from '../../../_functions/functions';
import { User } from '../../../_model/auth/user';
import { ToastService } from '../../../_service/notification/toast.service';
import { LoggerService } from '../../../_service/logger/logger.service';
import { HttpErrorResponse } from '@angular/common/http';
import { PASSWORD_INCORRECT } from '../../../_constants/constants';

@Component({
  selector: 'app-change-password',
  templateUrl: './change-password.component.html',
  styleUrls: ['./change-password.component.css'],
})
export class ChangePasswordComponent implements OnInit {
  form: FormGroup;
  token: string;
  message: string;
  error: string;
  response: number;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private userService: UserService,
    private toast: ToastService,
    private logger: LoggerService
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group(
      {
        currentPassword: [''],
        password: [''],
        confirmPassword: [''],
      },
      {
        validator: ConfirmPasswordValidator('password', 'confirmPassword'),
      }
    );
  }

  onSubmit() {
    let currentPassword: string = this.form.value.currentPassword?.trim() || '';
    let password: string = this.form.value.confirmPassword?.trim() || '';
    let token = decodeToken();

    // Protección: Si no hay token válido, no continuar
    if (!token) {
      this.toast.error('Sesión expirada. Por favor, inicie sesión nuevamente.');
      return;
    }

    // Validar que la contraseña actual no esté vacía
    if (!currentPassword) {
      this.toast.error('Por favor, ingrese su contraseña actual.');
      return;
    }

    // 🔍 LOGGING PARA DEBUG - Ver qué se está enviando
    this.logger.debug('[CHANGE-PASSWORD] Datos a enviar:', {
      userName: token.userName,
      currentPasswordLength: currentPassword.length,
      currentPasswordPreview: currentPassword ? '***' : 'empty',
      newPasswordLength: password.length,
    });

    let user = {} as User;
    user.userName = token.userName;
    user.password = currentPassword;
    
    // 🔍 LOGGING PARA DEBUG - Ver el objeto completo que se envía
    this.logger.debug('[CHANGE-PASSWORD] Objeto User a enviar:', {
      userName: user.userName,
      passwordLength: user.password?.length,
      passwordPreview: user.password ? '***' : 'empty',
      fullUserObject: { ...user, password: '***' }, // Ocultar password en el log
    });
    
    // 🔍 LOGGING DEL JSON EXACTO QUE SE ENVIARÁ (sin mostrar password real)
    const requestBody = {
      userName: user.userName,
      password: user.password
    };
    this.logger.debug('[CHANGE-PASSWORD] Request Body JSON (para comparar con backend):', {
      userName: requestBody.userName,
      passwordLength: requestBody.password?.length,
      passwordChars: requestBody.password?.split('').map(() => '*').join(''),
      jsonString: JSON.stringify({ userName: requestBody.userName, password: '***' }),
    });
    
    let idUser = 0;
    this.userService.findUserByUserNameAndPassword(user).subscribe({
      next: (data) => {
        // 🔍 LOGGING PARA DEBUG
        this.logger.debug('[CHANGE-PASSWORD] Respuesta del backend:', {
          code: data.code,
          status: data.status,
          message: data.message,
          hasUserDto: !!data.userDto,
          userDtoId: data.userDto?.idUser,
          fullResponse: data,
        });

        // Verificar que la respuesta sea exitosa y tenga userDto
        // IMPORTANTE: Si hay userDto con idUser válido, la contraseña es correcta (independientemente del code)
        // El backend puede devolver code 0 pero con userDto cuando la contraseña es correcta
        // O puede devolver code 200 con userDto cuando es exitoso
        const hasValidUser = data.userDto && data.userDto.idUser && data.userDto.idUser > 0;
        
        // Si hay userDto válido, la contraseña es correcta - proceder directamente
        if (hasValidUser) {
          // Contraseña correcta, proceder a actualizar
          this.logger.info('[CHANGE-PASSWORD] Contraseña correcta (userDto encontrado), actualizando...', {
            idUser: data.userDto.idUser,
            code: data.code,
            status: data.status,
          });
          idUser = data.userDto.idUser;
          // El backend solo necesita el campo password en el body
          this.userService.updatePassword(password, idUser).subscribe({
            next: (response) => {
              // El backend devuelve true (boolean) cuando es exitoso
              if (response === true) {
                this.logger.info('[CHANGE-PASSWORD] Contraseña actualizada exitosamente');
                this.toast.success('Se cambió la contraseña exitosamente.');
                setTimeout(() => {
                  this.router.navigate(['login']);
                }, 2000);
              } else {
                this.logger.warn('[CHANGE-PASSWORD] Respuesta inesperada del backend:', response);
                this.toast.error('Error al actualizar la contraseña. Intente nuevamente.');
              }
            },
            error: (error: HttpErrorResponse) => {
              this.toast.httpError(
                error,
                'actualizar la contraseña',
                'Error al actualizar la contraseña. Intente nuevamente.'
              );
            },
          });
        } else {
          // No hay userDto válido - contraseña incorrecta
          const isPasswordIncorrect = true;
          // Contraseña incorrecta o respuesta inválida
          this.logger.warn('[CHANGE-PASSWORD] Contraseña incorrecta detectada:', {
            code: data.code,
            status: data.status,
            message: data.message,
            userName: token.userName,
            responseReceived: data,
          });
          
          // Si el backend devuelve un mensaje específico, usarlo
          // Si no, usar el mensaje por defecto
          let errorMessage = PASSWORD_INCORRECT.MESSAGE_PASSWORD_INCORRECT.message;
          
          if (data.message && data.message.trim()) {
            errorMessage = data.message;
          } else if (data.code === 0) {
            // El backend devolvió code 0 pero sin mensaje específico
            errorMessage = 'La contraseña actual ingresada no es correcta. Por favor, verifique que no tenga espacios al inicio o al final.';
          }
          
          this.toast.error(errorMessage);
        }
      },
      error: (error: HttpErrorResponse) => {
        // 🔍 LOGGING PARA DEBUG
        this.logger.error('[CHANGE-PASSWORD] Error HTTP:', {
          status: error.status,
          statusText: error.statusText,
          url: error.url,
          error: error.error,
          message: error.message,
        });
        // Mostrar el mensaje del backend si está disponible
        const errorMessage =
          error.error?.message ||
          PASSWORD_INCORRECT.MESSAGE_PASSWORD_INCORRECT.message;
        this.toast.error(errorMessage);
      },
    });
  }
}
