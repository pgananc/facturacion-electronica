import { Injectable } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor,
} from '@angular/common/http';
import { Observable, throwError, TimeoutError } from 'rxjs';
import { timeout, catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';

/**
 * Interceptor de Timeout HTTP
 * 
 * Aplica un timeout global a todas las peticiones HTTP para evitar loading infinito.
 * 
 * Timeout por defecto: 30 segundos
 * 
 * Para sobrescribir el timeout en una petición específica, usar el header:
 * - 'X-Timeout': número en milisegundos (ej: '60000' para 60 segundos)
 * - 'X-No-Timeout': 'true' para deshabilitar timeout (no recomendado)
 */
@Injectable()
export class HttpTimeoutInterceptor implements HttpInterceptor {
  // Timeout por defecto: 30 segundos
  private readonly DEFAULT_TIMEOUT = 30000;

  intercept(
    request: HttpRequest<any>,
    next: HttpHandler
  ): Observable<HttpEvent<any>> {
    // Verificar si se solicita deshabilitar timeout
    if (request.headers.get('X-No-Timeout') === 'true') {
      const clonedRequest = request.clone({
        headers: request.headers.delete('X-No-Timeout'),
      });
      return next.handle(clonedRequest);
    }

    // Obtener timeout personalizado del header o usar el por defecto
    const timeoutValue =
      request.headers.get('X-Timeout') !== null
        ? parseInt(request.headers.get('X-Timeout')!, 10)
        : this.DEFAULT_TIMEOUT;

    // Clonar request sin el header de timeout (solo para configuración)
    const clonedRequest = request.clone({
      headers: request.headers.delete('X-Timeout'),
    });

    return next.handle(clonedRequest).pipe(
      timeout(timeoutValue),
      catchError((error) => {
        // Si es un timeout, convertir a HttpErrorResponse para consistencia
        if (error instanceof TimeoutError || error.name === 'TimeoutError') {
          return throwError(
            () =>
              new Error(
                `La petición tardó más de ${timeoutValue / 1000} segundos. Por favor verifica la conexión con el servidor.`
              )
          );
        }
        return throwError(() => error);
      })
    );
  }
}

