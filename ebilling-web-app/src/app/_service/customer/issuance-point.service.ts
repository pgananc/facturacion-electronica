import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { InvoiceIssuancePoint } from '../../_model/customer/invoice-issuance-point';

export interface IssuancePointRequest {
  code: string;
  establishmentId: number;
  sequentialInvoice: number;
  status?: boolean;
}

export interface InvoiceIssuancePointWithEstablishment
  extends InvoiceIssuancePoint {
  establishmentCode?: string;
  establishmentName?: string;
}

@Injectable({
  providedIn: 'root',
})
export class IssuancePointService {
  private readonly url = `${environment.HOST_CUSTOMERS}/api/invoice-issuance-points`;

  constructor(private readonly http: HttpClient) {}

  /**
   * Obtiene todos los puntos de emisión de una sucursal (sin paginación)
   * Backend: GET /api/invoice-issuance-points/establishment/{establishmentId}
   */
  findByEstablishment(
    establishmentId: number
  ): Observable<InvoiceIssuancePoint[]> {
    return this.http.get<InvoiceIssuancePoint[]>(
      `${this.url}/establishment/${establishmentId}`
    );
  }

  /**
   * Obtiene todos los puntos de emisión de una compañía (todas sus sucursales)
   * Backend: GET /api/invoice-issuance-points/company/{idCompany}
   * Retorna con información del establecimiento (código y nombre)
   */
  findByCompany(
    idCompany: number
  ): Observable<InvoiceIssuancePointWithEstablishment[]> {
    return this.http.get<InvoiceIssuancePointWithEstablishment[]>(
      `${this.url}/company/${idCompany}`
    );
  }

  /**
   * Obtiene un punto de emisión por ID
   * Backend: GET /api/invoice-issuance-points/{id}?establishmentId={establishmentId}
   */
  findById(
    idInvoiceIssuancePoint: number,
    establishmentId: number
  ): Observable<InvoiceIssuancePoint> {
    const params = new HttpParams().set(
      'establishmentId',
      establishmentId.toString()
    );
    return this.http.get<InvoiceIssuancePoint>(
      `${this.url}/${idInvoiceIssuancePoint}`,
      { params }
    );
  }

  /**
   * Crea un nuevo punto de emisión
   * Backend: POST /api/invoice-issuance-points
   */
  create(
    issuancePoint: IssuancePointRequest
  ): Observable<InvoiceIssuancePoint> {
    return this.http.post<InvoiceIssuancePoint>(this.url, issuancePoint);
  }

  /**
   * Actualiza un punto de emisión existente
   * Backend: PUT /api/invoice-issuance-points/{id}?establishmentId={establishmentId}
   */
  update(
    idInvoiceIssuancePoint: number,
    issuancePoint: IssuancePointRequest
  ): Observable<InvoiceIssuancePoint> {
    const params = new HttpParams().set(
      'establishmentId',
      issuancePoint.establishmentId.toString()
    );
    return this.http.put<InvoiceIssuancePoint>(
      `${this.url}/${idInvoiceIssuancePoint}`,
      issuancePoint,
      { params }
    );
  }

  /**
   * Elimina un punto de emisión (eliminación lógica)
   * Backend: DELETE /api/invoice-issuance-points/{id}?establishmentId={establishmentId}
   */
  delete(
    idInvoiceIssuancePoint: number,
    establishmentId: number
  ): Observable<void> {
    const params = new HttpParams().set(
      'establishmentId',
      establishmentId.toString()
    );
    return this.http.delete<void>(`${this.url}/${idInvoiceIssuancePoint}`, {
      params,
    });
  }

  /**
   * Valida si el código de punto de emisión ya existe para una sucursal
   * Backend: GET /api/invoice-issuance-points/establishment/{establishmentId}/exists/{code}
   */
  existsByCodeAndEstablishment(
    code: string,
    establishmentId: number
  ): Observable<boolean> {
    return this.http.get<boolean>(
      `${this.url}/establishment/${establishmentId}/exists/${code}`
    );
  }
}
