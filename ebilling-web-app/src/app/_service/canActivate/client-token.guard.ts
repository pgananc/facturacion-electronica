import { Injectable } from '@angular/core';
import { CanActivate, Router, UrlTree } from '@angular/router';

@Injectable({
  providedIn: 'root',
})
export class ClientTokenGuard implements CanActivate {
  constructor(private router: Router) {}

  canActivate(): boolean | UrlTree {
    // Verificar si existe el token de cliente en localStorage
    const clientToken = localStorage.getItem('clientToken');
    const clientNumber = localStorage.getItem(
      'consultationIdentificationNumber'
    );
    const clientEmail = localStorage.getItem('clientEmail');

    // Si no tiene los datos necesarios del proceso de validación
    if (!clientToken || !clientNumber || !clientEmail) {
      console.warn('Acceso denegado: Token de cliente no encontrado');
      // 1. Limpiar toda la sesión (incluyendo token JWT si existe)
      localStorage.clear();
      // 2. Redirigir a login
      return this.router.parseUrl('/invoice-consultation');
    }

    // Verificar que el token no esté vacío
    if (
      clientToken.trim() === '' ||
      clientNumber.trim() === '' ||
      clientEmail.trim() === ''
    ) {
      console.warn('Acceso denegado: Datos de cliente vacíos');
      // 1. Limpiar toda la sesión (incluyendo token JWT si existe)
      localStorage.clear();
      // 2. Redirigir a login
      return this.router.parseUrl('/login');
    }

    // Token válido, permitir acceso
    return true;
  }
}
