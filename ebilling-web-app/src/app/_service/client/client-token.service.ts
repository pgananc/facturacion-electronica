import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { ClientTokenRequest } from '../../_model/client/client-token-request';
import { ClientTokenResponse } from '../../_model/client/client-token-response';
import { TokenValidationRequest } from '../../_model/client/token-validation-request';
import { ResendTokenRequest } from '../../_model/client/resend-token-request';
import { ResendTokenResponse } from '../../_model/client/resend-token-response';
import { TokenResponse } from 'src/app/_model/auth/token-response';

@Injectable({
  providedIn: 'root',
})
export class ClientTokenService {
  private readonly API_BASE_URL = environment.HOST_AUTH;

  constructor(private http: HttpClient) {}

  /**
   * Genera un token para el cliente
   * POST /api/token/client
   */
  generateClientToken(
    request: ClientTokenRequest
  ): Observable<ClientTokenResponse> {
    const url = `${this.API_BASE_URL}/api/token/client`;

    return this.http
      .post<ClientTokenResponse>(url, request, {
        headers: {
          'Content-Type': 'application/json',
        },
      })
      .pipe(
        catchError((error) => {
          console.error('Error generating client token:', error);
          return throwError(() => this.handleError(error));
        })
      );
  }

  /**
   * Valida el token del cliente y genera el token de autenticación
   * POST /api/token/validate-client/generate
   */
  validateClientToken(
    request: TokenValidationRequest
  ): Observable<TokenResponse> {
    const url = `${this.API_BASE_URL}/api/token/validate-client/generate`;

    return this.http
      .post<TokenResponse>(url, request, {
        headers: {
          'Content-Type': 'application/json',
        },
      })
      .pipe(
        catchError((error) => {
          console.error('Error validating client token:', error);
          return throwError(() => this.handleError(error));
        })
      );
  }

  /**
   * Reenvía el token al cliente
   * POST /api/token/resend-client
   */
  resendClientToken(
    request: ResendTokenRequest
  ): Observable<ResendTokenResponse> {
    const url = `${this.API_BASE_URL}/api/token/resend-client`;

    return this.http
      .post<ResendTokenResponse>(url, request, {
        headers: {
          'Content-Type': 'application/json',
        },
      })
      .pipe(
        catchError((error) => {
          console.error('Error resending client token:', error);
          return throwError(() => this.handleError(error));
        })
      );
  }

  /**
   * Maneja los errores de la API
   */
  private handleError(error: any): Error {
    if (error.error && error.error.message) {
      // Error del backend con mensaje específico
      return new Error(error.error.message);
    } else if (error.status === 0) {
      // Error de conexión
      return new Error('Error de conexión. Verifique su conexión a internet.');
    } else if (error.status >= 500) {
      // Error del servidor
      return new Error('Error interno del servidor. Intente nuevamente.');
    } else {
      // Error genérico
      return new Error('Ha ocurrido un error inesperado. Intente nuevamente.');
    }
  }
}
