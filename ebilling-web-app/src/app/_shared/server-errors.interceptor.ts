import { environment } from './../../environments/environment';
import { Injectable } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor,
  HttpResponse,
} from '@angular/common/http';
import { catchError, EMPTY, Observable, retry, tap } from 'rxjs';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';

@Injectable()
export class ServerErrorsInterceptor implements HttpInterceptor {
  constructor(private snackBar: MatSnackBar, private router: Router) {}

  intercept(
    request: HttpRequest<any>,
    next: HttpHandler
  ): Observable<HttpEvent<any>> {
    return next
      .handle(request)
      .pipe(retry(environment.REINTENTOS))
      .pipe(
        tap((event) => {
          if (event instanceof HttpResponse) {
            if (
              event.body &&
              event.body.error === true &&
              event.body.errorMessage
            ) {
              throw new Error(event.body.errorMessage);
            } /*else{
                    this.snackBar.open("EXITO", 'AVISO', { duration: 5000 });    
                }*/
          }
        })
      )
      .pipe(
        catchError((err) => {
          console.log(err);
          if (err.status === 400) {
            this.snackBar.open(err.message, 'ERROR 400', { duration: 5000 });
          } else if (err.status === 401) {
            this.snackBar.open(err.message, 'ERROR 401', { duration: 5000 });
            sessionStorage.clear();
            this.router.navigate(['/login']);
          } else if (err.status === 500) {
            this.snackBar.open(err.error.mensaje, 'ERROR 500', {
              duration: 5000,
            });
          } else {
            this.snackBar.open(
              'Servicio no disponible intente más tarde',
              'ERROR',
              {
                duration: 5000,
              }
            );
          }
          return EMPTY;
        })
      );
  }
}
