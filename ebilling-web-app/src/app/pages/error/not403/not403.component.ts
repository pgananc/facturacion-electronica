import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { environment } from 'src/environments/environment';
import { JwtHelperService } from '@auth0/angular-jwt';

@Component({
  selector: 'app-not403',
  templateUrl: './not403.component.html',
  styleUrls: ['./not403.component.css'],
})
export class Not403Component implements OnInit {
  userName: string = '';
  isClientTokenError: boolean = false;
  errorMessage: string = '';

  constructor(private router: Router) {}

  ngOnInit(): void {
    // Verificar si es un error de token de cliente
    const clientToken = localStorage.getItem('clientToken');
    const clientNumber = localStorage.getItem(
      'consultationIdentificationNumber'
    );

    if (!clientToken || !clientNumber) {
      this.isClientTokenError = true;
      this.errorMessage =
        'Debe completar la validación de token para acceder a esta página.';
      return;
    }

    // Si es un usuario autenticado normal
    try {
      let token = localStorage.getItem(environment.TOKEN);
      if (token) {
        const helper = new JwtHelperService();
        let decodedToken = helper.decodeToken(token);
        this.userName = decodedToken.userName;
        this.errorMessage = 'No tienes permisos para acceder a esta página.';
      } else {
        this.errorMessage = 'Debe iniciar sesión para acceder a esta página.';
      }
    } catch (error) {
      this.errorMessage = 'Error de autenticación.';
    }
  }

  goToInvoiceConsultation(): void {
    this.router.navigate(['/invoice-consultation']);
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }

  goToWelcome(): void {
    this.router.navigate(['/welcome']);
  }
}
