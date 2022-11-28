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
import { DURATION_TIME_MESSAGE } from '../_constants/constants';
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
            }
          }
        })
      )
      .pipe(
        catchError((err) => {
          console.log(err);
          if (
            err.status === MESSAGE_ERROR_SERVER.MESSAGE_ERROR_SERVER_400.code
          ) {
            this.snackBar.open(
              ERRORS.MESSAGE_ERROR.message,
              MESSAGE_ERROR_SERVER.MESSAGE_ERROR_SERVER_400.message,
              {
                duration: DURATION_TIME_MESSAGE.value,
              }
            );
          } else if (
            err.status === MESSAGE_ERROR_SERVER.MESSAGE_ERROR_SERVER_401.code
          ) {
            this.snackBar.open(
              err.message,
              MESSAGE_ERROR_SERVER.MESSAGE_ERROR_SERVER_401.message,
              {
                duration: DURATION_TIME_MESSAGE.value,
              }
            );
            sessionStorage.clear();
            this.router.navigate(['/login']);
          } else if (
            err.status === MESSAGE_ERROR_SERVER.MESSAGE_ERROR_SERVER_404.code
          ) {
            this.snackBar.open(
              ERRORS.MESSAGE_ERROR_NOT_FOUND.message,
              MESSAGE_ERROR_SERVER.MESSAGE_ERROR_SERVER_404.message,
              {
                duration: DURATION_TIME_MESSAGE.value,
              }
            );
            sessionStorage.clear();
            this.router.navigate(['/not-404']);
          } else if (
            err.status === MESSAGE_ERROR_SERVER.MESSAGE_ERROR_SERVER_500.code
          ) {
            this.snackBar.open(
              ERRORS.MESSAGE_ERROR.message,
              MESSAGE_ERROR_SERVER.MESSAGE_ERROR_SERVER_500.message,
              {
                duration: DURATION_TIME_MESSAGE.value,
              }
            );
          } else {
            this.snackBar.open(
              ERRORS.MESSAGE_ERROR_NOT_AVAILABLE.message,
              HEADER_MESSAGE.MESSAGE_HEADER_ERROR.message,
              {
                duration: DURATION_TIME_MESSAGE.value,
              }
            );
          }
          return EMPTY;
        })
      );
  }
}
