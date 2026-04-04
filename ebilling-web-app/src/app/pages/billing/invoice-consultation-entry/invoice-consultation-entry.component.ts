import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { ToastService } from '../../../_service/notification/toast.service';
import { ClientTokenService } from '../../../_service/client/client-token.service';
import { ClientTokenRequest } from '../../../_model/client/client-token-request';

@Component({
  selector: 'app-invoice-consultation-entry',
  templateUrl: './invoice-consultation-entry.component.html',
  styleUrls: ['./invoice-consultation-entry.component.css'],
})
export class InvoiceConsultationEntryComponent implements OnInit, OnDestroy {
  consultationForm: FormGroup;
  isLoading = false;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private route: ActivatedRoute,
    private toast: ToastService,
    private clientTokenService: ClientTokenService
  ) {
    this.consultationForm = this.fb.group({
      identificationNumber: [
        '',
        [
          Validators.required,
          Validators.maxLength(13),
          Validators.pattern(/^[0-9]+$/),
        ],
      ],
      clientEmail: ['', [Validators.required, Validators.email]],
    });
  }

  ngOnInit(): void {
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
    this.consultationForm.reset();
    localStorage.removeItem('consultationIdentificationNumber');
    localStorage.removeItem('clientEmail');
  }

  onSubmit(): void {
    if (this.isFormValid()) {
      this.isLoading = true;

      const identificationNumber = this.consultationForm.get(
        'identificationNumber'
      )?.value;
      const clientEmail = this.consultationForm.get('clientEmail')?.value;

      // Crear request para generar token
      const tokenRequest: ClientTokenRequest = {
        clientNumber: identificationNumber,
        clientEmail: clientEmail,
      };

      // Llamar al servicio para generar token
      this.clientTokenService.generateClientToken(tokenRequest).subscribe({
        next: (response) => {
          if (response.status) {
            // Token generado exitosamente, redirigir a validación
            localStorage.setItem(
              'consultationIdentificationNumber',
              identificationNumber
            );
            localStorage.setItem('clientEmail', clientEmail);
            localStorage.setItem('clientToken', response.token);
            this.router.navigate(['/invoice-consultation/validate']);
          } else {
            // Error en la generación del token
            this.toast.error('Error al generar el token. Intente nuevamente.');
          }
          this.isLoading = false;
        },
        error: (error) => {
          console.error('Error generating client token:', error);
          this.toast.error(
            error.message ||
              'Error al procesar la solicitud. Intente nuevamente.'
          );
          this.clearForm();
          this.isLoading = false;
        },
      });
    } else {
      this.toast.warning('Por favor, complete todos los campos correctamente.');
    }
  }

  isFormValid(): boolean {
    const identificationNumber = this.consultationForm.get(
      'identificationNumber'
    )?.value;
    const clientEmail = this.consultationForm.get('clientEmail')?.value;

    return (
      this.consultationForm.valid &&
      identificationNumber &&
      identificationNumber.trim().length > 0 &&
      clientEmail &&
      clientEmail.trim().length > 0
    );
  }

  get identificationNumberError(): string {
    const control = this.consultationForm.get('identificationNumber');
    if (control?.errors && control.touched) {
      if (control.errors['required']) {
        return 'El número de identificación es obligatorio';
      }
      if (control.errors['maxlength']) {
        return 'El número de identificación no puede exceder 13 caracteres';
      }
      if (control.errors['pattern']) {
        return 'El número de identificación debe contener solo números';
      }
    }
    return '';
  }

  get clientEmailError(): string {
    const control = this.consultationForm.get('clientEmail');
    if (control?.errors && control.touched) {
      if (control.errors['required']) {
        return 'El email es obligatorio';
      }
      if (control.errors['email']) {
        return 'Ingrese un email válido';
      }
    }
    return '';
  }
}
