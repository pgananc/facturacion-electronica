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
import {
  MESSAGE_ERROR_SERVER,
  ERRORS,
  HEADER_MESSAGE,
} from '../_constants/constants';

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
            this.snackBar.open(
              ERRORS.MESSAGE_ERROR.message,
              MESSAGE_ERROR_SERVER.MESSAGE_ERROR_SERVER_400.message,
              {
                duration: 5000,
              }
            );
          } else if (err.status === 401) {
            this.snackBar.open(
              err.message,
              MESSAGE_ERROR_SERVER.MESSAGE_ERROR_SERVER_401.message,
              {
                duration: 5000,
              }
            );
            sessionStorage.clear();
            this.router.navigate(['/login']);
          } else if (err.status === 500) {
            this.snackBar.open(
              ERRORS.MESSAGE_ERROR.message,
              MESSAGE_ERROR_SERVER.MESSAGE_ERROR_SERVER_500.message,
              {
                duration: 5000,
              }
            );
          } else {
            this.snackBar.open(
              ERRORS.MESSAGE_ERROR_NOT_AVAILABLE.message,
              HEADER_MESSAGE.MESSAGE_HEADER_ERROR.message,
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
