import { HttpHeaders } from '@angular/common/http';

/**
 * Utilidad para facilitar el manejo de errores HTTP en componentes
 *
 * Uso:
 * ```typescript
 * // Opción 1: Suprimir completamente el interceptor
 * this.http.get(url, { headers: HttpSkipInterceptor.skipAll() })
 *   .subscribe({
 *     next: (data) => console.log(data),
 *     error: (err) => {
 *       // Manejar error sin que el interceptor muestre snackbar
 *       if (err.status === 404) {
 *         console.log('Cliente no encontrado');
 *       }
 *     }
 *   });
 *
 * // Opción 2: Suprimir solo el mensaje, pero mantener la navegación
 * this.http.get(url, { headers: HttpSkipInterceptor.skipMessage() })
 * ```
 */
export class HttpSkipInterceptor {
  /**
   * Header para indicar que el interceptor debe saltarse completamente
   * El error se propagará al componente sin mostrar snackbar
   */
  private static readonly SKIP_INTERCEPTOR_HEADER = 'X-Skip-Interceptor';

  /**
   * Crea headers para saltar completamente el interceptor
   * El componente es responsable de manejar todos los errores
   */
  static skipAll(): HttpHeaders {
    return new HttpHeaders({
      [this.SKIP_INTERCEPTOR_HEADER]: 'true',
    });
  }

  /**
   * Añade el header de skip a headers existentes
   */
  static addSkipToHeaders(headers?: HttpHeaders): HttpHeaders {
    if (!headers) {
      return this.skipAll();
    }
    return headers.set(this.SKIP_INTERCEPTOR_HEADER, 'true');
  }

  /**
   * Verifica si una request tiene el header de skip
   */
  static hasSkipHeader(headers: HttpHeaders): boolean {
    return headers.has(this.SKIP_INTERCEPTOR_HEADER);
  }
}
