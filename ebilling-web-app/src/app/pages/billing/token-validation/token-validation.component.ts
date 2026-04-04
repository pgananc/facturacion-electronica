import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { ToastService } from '../../../_service/notification/toast.service';
import { ClientTokenService } from '../../../_service/client/client-token.service';
import { TokenValidationRequest } from '../../../_model/client/token-validation-request';
import { ResendTokenRequest } from '../../../_model/client/resend-token-request';
import { environment } from 'src/environments/environment';

@Component({
  selector: 'app-token-validation',
  templateUrl: './token-validation.component.html',
  styleUrls: ['./token-validation.component.css'],
})
export class TokenValidationComponent implements OnInit, OnDestroy {
  tokenForm: FormGroup;
  isLoading = false;
  isResending = false;
  clientNumber: string = '';
  clientEmail: string = '';
  clientToken: string = '';

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private route: ActivatedRoute,
    private toast: ToastService,
    private clientTokenService: ClientTokenService
  ) {
    this.tokenForm = this.fb.group({
      token: ['', [Validators.required, Validators.minLength(6)]],
    });
  }

  ngOnInit(): void {
    // Obtener datos del localStorage
    this.clientNumber =
      localStorage.getItem('consultationIdentificationNumber') || '';
    this.clientEmail = localStorage.getItem('clientEmail') || '';
    this.clientToken = localStorage.getItem('clientToken') || '';

    // Verificar que tenemos los datos necesarios
    if (!this.clientNumber || !this.clientEmail || !this.clientToken) {
      this.toast.error('Datos de cliente no encontrados.');
      this.router.navigate(['/invoice-consultation']);
      return;
    }

    // Check if we need to reset (coming back from consultation)
    this.route.queryParams.subscribe((params) => {
      if (params['reset'] === 'true') {
        this.clearForm();
        // Clear the reset parameter from URL
        this.router.navigate([], {
          relativeTo: this.route,
          queryParams: {},
          replaceUrl: true,
        });
      }
    });
  }

  ngOnDestroy(): void {
    // Clean up if needed
  }

  private clearForm(): void {
    this.tokenForm.reset();
    localStorage.removeItem('consultationIdentificationNumber');
    localStorage.removeItem('clientEmail');
    localStorage.removeItem('clientToken');
  }

  onSubmit(): void {
    if (this.isFormValid()) {
      this.isLoading = true;

      const token = this.tokenForm.get('token')?.value;

      // Crear request para validar token
      const validationRequest: TokenValidationRequest = {
        clientNumber: this.clientNumber,
        token: token,
      };

      // Llamar al servicio para validar token
      this.clientTokenService.validateClientToken(validationRequest).subscribe({
        next: (response) => {
          console.debug('🔍 [token-validation] Respuesta recibida:', response);
          console.debug('🔍 Response code:', response.code);
          console.debug('🔍 Response token:', response.token);

          if (response.code == 1) {
            // ✅ AGREGAR: Validar que el token existe antes de guardarlo
            if (!response.token || !response.token.token) {
              console.error(
                '❌ [token-validation] Token no viene en la respuesta'
              );
              console.error(
                '❌ Response completa:',
                JSON.stringify(response, null, 2)
              );
              this.toast.error(
                'Token no recibido del servidor. Intente nuevamente.'
              );
              this.isLoading = false;
              return;
            }

            // ✅ AGREGAR: Logs de debug
            console.debug('✅ [token-validation] Token recibido correctamente');
            console.debug('✅ Token length:', response.token.token.length);
            console.debug(
              '✅ Token preview:',
              response.token.token.substring(0, 20) + '...'
            );

            // ✅ CORRECCIÓN: Guardar el token
            localStorage.setItem(environment.TOKEN, response.token.token);

            // ✅ AGREGAR: Verificar que se guardó correctamente
            const savedToken = localStorage.getItem(environment.TOKEN);
            console.debug('✅ Token guardado en localStorage:', !!savedToken);
            console.debug('✅ Token guardado length:', savedToken?.length || 0);
            console.debug(
              '✅ Token guardado preview:',
              savedToken ? savedToken.substring(0, 20) + '...' : 'null'
            );

            // ✅ AGREGAR: Pequeño delay para asegurar que el token se guarde
            // y que el JwtModule lo detecte antes de navegar
            setTimeout(() => {
              console.debug(
                '✅ [token-validation] Navegando a /invoice-consultation/view'
              );
              this.router.navigate(['/invoice-consultation/view']);
            }, 100);
          } else {
            // Token inválido
            console.error(
              '❌ [token-validation] Token inválido, code:',
              response.code
            );
            this.toast.error(
              'Token inválido. Verifique el código e intente nuevamente.'
            );
            this.tokenForm.get('token')?.setValue('');
          }
          this.isLoading = false;
        },
        error: (error) => {
          console.error('❌ [token-validation] Error validating token:', error);
          console.error('❌ Error completo:', JSON.stringify(error, null, 2));
          console.error('❌ Error status:', error.status);
          console.error('❌ Error message:', error.message);
          this.toast.error(
            error.message || 'Error al validar el token. Intente nuevamente.'
          );
          this.tokenForm.get('token')?.setValue('');
          this.isLoading = false;
        },
      });
    } else {
      this.toast.warning('Por favor, ingrese el token de verificación.');
    }
  }

  isFormValid(): boolean {
    const token = this.tokenForm.get('token')?.value;

    return this.tokenForm.valid && token && token.trim().length >= 6;
  }

  get tokenError(): string {
    const control = this.tokenForm.get('token');
    if (control?.errors && control.touched) {
      if (control.errors['required']) {
        return 'El token es obligatorio';
      }
      if (control.errors['minlength']) {
        return 'El token debe tener al menos 6 caracteres';
      }
    }
    return '';
  }

  goBackToConsultation(): void {
    this.clearForm();
    this.router.navigate(['/invoice-consultation'], {
      queryParams: { reset: 'true' },
    });
  }

  resendToken(): void {
    if (!this.clientNumber || !this.clientEmail) {
      this.toast.error('Datos de cliente no encontrados.');
      return;
    }

    this.isResending = true;

    const resendRequest: ResendTokenRequest = {
      clientNumber: this.clientNumber,
      clientEmail: this.clientEmail,
    };

    this.clientTokenService.resendClientToken(resendRequest).subscribe({
      next: (response) => {
        if (response.status) {
          this.toast.success(
            response.message ||
              'Token reenviado al email. Verifique su bandeja de entrada.'
          );
        } else {
          this.toast.error('Error al reenviar el token. Intente nuevamente.');
        }
        this.isResending = false;
      },
      error: (error) => {
        console.error('Error resending token:', error);
        this.toast.error(
          error.message || 'Error al reenviar el token. Intente nuevamente.'
        );
        this.isResending = false;
      },
    });
  }
}
