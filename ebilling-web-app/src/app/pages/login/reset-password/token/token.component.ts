import { UserService } from '../../../../_service/user/user.service';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { ConfirmPasswordValidator } from './confirm-password.validator';
import { TokenService } from 'src/app/_service/auth/token.service';
import { LoggerService } from 'src/app/_service/logger/logger.service';

@Component({
  selector: 'app-token',
  templateUrl: './token.component.html',
  styleUrls: ['./token.component.css'],
})
export class TokenComponent implements OnInit {
  form: FormGroup;
  token: string;
  message: string;
  error: string;
  response: number;
  validToken: boolean = false;
  isSubmitting: boolean = false;
  isValidating: boolean = true;
  passwordChangedSuccessfully: boolean = false;

  constructor(
    private readonly fb: FormBuilder,
    private readonly router: Router,
    private readonly route: ActivatedRoute,
    private readonly userService: UserService,
    private readonly tokenService: TokenService,
    private readonly logger: LoggerService
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group(
      {
        password: [''],
        confirmPassword: [''],
      },
      {
        validator: ConfirmPasswordValidator('password', 'confirmPassword'),
      }
    );

    this.route.params.subscribe(
      (params: Params) => {
        this.token = params['token'];
        this.isValidating = true;
        this.validToken = false;

        this.tokenService.validateTokenReset(this.token).subscribe({
          next: (data) => {
            this.logger.debug('Validación de token', data);
            this.isValidating = false;
            if (data) {
              this.validToken = true;
            } else {
              this.validToken = false;
              setTimeout(() => {
                this.router.navigate(['login']);
              }, 2000);
            }
          },
          error: (err) => {
            this.logger.error('Error al validar token:', err);
            this.isValidating = false;
            this.validToken = false;
            setTimeout(() => {
              this.router.navigate(['login']);
            }, 2000);
          },
        });
      },
      (err) => {
        this.isValidating = false;
        this.validToken = false;
      }
    );
  }

  onSubmit() {
    if (this.form.invalid) {
      return;
    }

    this.isSubmitting = true;
    this.message = '';
    this.error = '';

    let clave: string = this.form.value.confirmPassword;

    // Log para debug
    this.logger.debug('Cambiando contraseña con token:', {
      token: this.token,
      passwordLength: clave?.length,
    });

    this.userService.resetPassword(this.token, clave).subscribe({
      next: (data) => {
        if (data) {
          this.message = 'Se cambió la contraseña exitosamente.';
          this.error = '';

          // Marcar que el cambio fue exitoso y ocultar el formulario
          this.passwordChangedSuccessfully = true;
          this.validToken = false;

          setTimeout(() => {
            // Limpiar localStorage antes de redirigir
            localStorage.clear();
            this.router.navigate(['login']);
          }, 2000);
        } else {
          this.error = 'No se pudo cambiar la contraseña. Intente nuevamente.';
          this.message = '';
        }
        this.isSubmitting = false;
      },
      error: (err) => {
        this.logger.error('Error al cambiar contraseña:', err);
        this.error =
          err.message || 'Error al cambiar la contraseña. Intente nuevamente.';
        this.message = '';
        this.isSubmitting = false;
      },
    });
  }
}
