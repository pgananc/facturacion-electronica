import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { InvoiceIssuancePoint } from '../../_model/customer/invoice-issuance-point';

@Injectable({
  providedIn: 'root',
})
export class InvoicePointService {
  private readonly url = `${environment.HOST_AUTH}/api/invoice-points/session`;

  constructor(private readonly http: HttpClient) {}

  /**
   * Asigna el punto de emisión al usuario autenticado.
   */
  assignInvoicePoint(idInvoiceIssuancePoint: number): Observable<void> {
    return this.http.post<void>(this.url, { idInvoiceIssuancePoint });
  }

  /**
   * Consulta puntos de emisión disponibles para las sucursales dadas.
   */
  findAvailableByEstablishments(
    idEstablishments: number[]
  ): Observable<InvoiceIssuancePoint[]> {
    const params = new HttpParams().set(
      'idEstablishments',
      idEstablishments.join(',')
    );
    return this.http.get<InvoiceIssuancePoint[]>(`${this.url}/available`, {
      params,
    });
  }

  /**
   * Consulta el punto de emisión actual asignado al usuario.
   */
  getCurrentSession(userName: string): Observable<InvoiceIssuancePoint | null> {
    return this.http.get<InvoiceIssuancePoint | null>(
      `${this.url}/current-session`,
      {
        params: new HttpParams().set('userName', userName),
      }
    );
  }

  /**
   * Libera el punto de emisión asignado al usuario actual.
   * DELETE /api/invoice-points/session
   */
  releaseInvoicePoint(): Observable<void> {
    return this.http.delete<void>(this.url);
  }

  /**
   * Envía heartbeat para mantener activa la sesión del punto de emisión.
   * POST /api/invoice-points/session/heartbeat
   *
   * El heartbeat debe enviarse cada 2 minutos para evitar que el servidor
   * libere automáticamente la sesión después de 10 minutos de inactividad.
   *
   * IMPORTANTE: Este endpoint usa X-Skip-Interceptor para evitar que el
   * interceptor de errores muestre mensajes genéricos. El error 404 se
   * maneja específicamente en InvoicePointSessionService.
   */
  sendHeartbeat(): Observable<void> {
    // Agregar header para evitar que el interceptor de errores maneje el 404
    const headers = new HttpHeaders().set('X-Skip-Interceptor', 'true');
    return this.http.post<void>(`${this.url}/heartbeat`, {}, { headers });
  }
}
