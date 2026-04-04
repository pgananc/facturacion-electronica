import { environment } from '../../../environments/environment';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { TokenResponse } from '../../_model/auth/token-response';
import { decodeToken } from '../../_functions/functions';
import { InvoicePointSessionService } from './invoice-point-session.service';
import { HttpSkipInterceptor } from '../../_shared/http-skip-interceptor';
import { LoggerService } from '../logger/logger.service';

@Injectable({
  providedIn: 'root',
})
export class LoginService {
  url: string = `${environment.HOST_AUTH}/api/login`;
  messageChange = new Subject<string>();

  constructor(
    private readonly http: HttpClient,
    private readonly router: Router,
    private readonly invoicePointSessionService: InvoicePointSessionService,
    private readonly logger: LoggerService,
  ) {}

  private readonly loginState = new BehaviorSubject<boolean>(this.hasToken());
  loginState$ = this.loginState.asObservable();

  private hasToken(): boolean {
    return localStorage.getItem(environment.TOKEN) != null;
  }

  login(userName: string, password: string) {
    // Usar HttpSkipInterceptor para manejar errores personalizados en el componente
    // Esto evita que el interceptor global muestre mensajes genéricos
    return this.http.post<TokenResponse>(
      this.url,
      { userName, password },
      {
        withCredentials: true,
        headers: HttpSkipInterceptor.skipAll(),
      }
    );
  }

  setLoginStatus(loggedIn: boolean) {
    this.loginState.next(loggedIn);
  }

  closeSession() {
    const token = decodeToken();
    const userName = token?.userName;

    // Si no hay token válido, solo limpiar y redirigir
    if (!userName) {
      localStorage.clear();
      this.setLoginStatus(false);
      this.router.navigate(['login']);
      return;
    }

    // Primero liberar punto de emisión y detener heartbeat
    this.invoicePointSessionService
      .releaseInvoicePointWithHeartbeat()
      .then(() => {
        this.logger.debug('[LoginService] Punto de emisión liberado durante logout');
      })
      .catch((err) => {
        this.logger.warn('[LoginService] Error al liberar punto de emisión durante logout', err);
      })
      .finally(() => {
        // Realizar logout del servidor (siempre se ejecuta, independiente del resultado anterior)
        this.logoutFromServer(userName).subscribe({
          next: () => {
            localStorage.clear();
            this.setLoginStatus(false);
            this.router.navigate(['login']);
          },
          error: () => {
            // Incluso si falla el logout del servidor, igual cerramos sesión localmente
            localStorage.clear();
            this.setLoginStatus(false);
            this.router.navigate(['login']);
          },
        });
      });
  }

  isLogin() {
    let token = localStorage.getItem(environment.TOKEN);
    return token != null;
  }

  sendMail(mail: string) {
    return this.http.post<number>(
      `${environment.HOST_AUTH}/api/mail/send`,
      mail,
      {
        headers: new HttpHeaders().set('Content-Type', 'text/plain'),
      }
    );
  }

  logoutFromServer(userName: string): Observable<void> {
    const params = new HttpParams().set('userName', userName);
    return this.http.delete<void>(`${environment.HOST_AUTH}/api/login/logout`, {
      params,
    });
  }
}
