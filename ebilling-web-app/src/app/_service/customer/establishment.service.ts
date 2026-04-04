import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Establishment } from '../../_model/customer/company-establishment';
import { LoggerService } from '../logger/logger.service';

export interface EstablishmentRequest {
  name: string;
  code: string;
  address: string;
  companyId: number;
  type: string;
  status?: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class EstablishmentService {
  private readonly url = `${environment.HOST_CUSTOMERS}/api/establishments`;

  constructor(
    private readonly http: HttpClient,
    private readonly logger: LoggerService,
  ) {}

  /**
   * Obtiene todas las sucursales de una compañía (sin paginación)
   * Backend: GET /api/establishments/company/{idCompany}
   */
  findByCompany(idCompany: number): Observable<Establishment[]> {
    return this.http.get<Establishment[]>(`${this.url}/company/${idCompany}`);
  }

  /**
   * Obtiene una sucursal por ID
   * Backend: GET /api/establishments/{id}?companyId={idCompany}
   */
  findById(
    idEstablishment: number,
    companyId: number
  ): Observable<Establishment> {
    const params = new HttpParams().set('companyId', companyId.toString());
    return this.http.get<Establishment>(`${this.url}/${idEstablishment}`, {
      params,
    });
  }

  /**
   * Crea una nueva sucursal
   * Backend: POST /api/establishments
   * El backend espera 'idCompany' en el body, no 'companyId'
   */
  create(establishment: EstablishmentRequest): Observable<Establishment> {
    // Validar que companyId existe
    if (!establishment.companyId) {
      throw new Error('El ID de la compañía es obligatorio');
    }

    // Mapear companyId a idCompany para el backend
    // Asegurar que idCompany sea un número explícito
    const idCompanyValue = Number(establishment.companyId);

    if (!idCompanyValue || isNaN(idCompanyValue)) {
      throw new Error('El ID de la compañía debe ser un número válido');
    }

    // El backend podría estar esperando 'companyId' en lugar de 'idCompany'
    // Probar primero con companyId ya que es el nombre que usa el frontend
    const requestBody = {
      name: establishment.name?.trim() || '',
      code: establishment.code?.trim() || '',
      address: establishment.address?.trim() || '',
      companyId: idCompanyValue, // Probar con companyId (nombre del frontend)
      type: establishment.type || 'SUCURSAL',
      status:
        establishment.status !== undefined
          ? Boolean(establishment.status)
          : true,
    };

    if (!requestBody.companyId) {
      throw new Error('El ID de la compañía es obligatorio');
    }

    this.logger.debug('[EstablishmentService] POST create', {
      url: this.url,
      companyId: requestBody.companyId,
      code: requestBody.code,
    });

    return this.http.post<Establishment>(this.url, requestBody);
  }

  /**
   * Actualiza una sucursal existente
   * Backend: PUT /api/establishments/{id}?companyId={companyId}
   * El backend espera 'companyId' en el body (consistente con create)
   */
  update(
    idEstablishment: number,
    establishment: EstablishmentRequest
  ): Observable<Establishment> {
    // Validar que companyId existe
    if (!establishment.companyId) {
      throw new Error('El ID de la compañía es obligatorio');
    }

    const params = new HttpParams().set(
      'companyId',
      establishment.companyId.toString()
    );

    // Asegurar que companyId sea un número explícito
    const idCompanyValue = Number(establishment.companyId);

    if (!idCompanyValue || isNaN(idCompanyValue)) {
      throw new Error('El ID de la compañía debe ser un número válido');
    }

    // Usar companyId en el body (consistente con create)
    const requestBody = {
      name: establishment.name?.trim() || '',
      code: establishment.code?.trim() || '',
      address: establishment.address?.trim() || '',
      companyId: idCompanyValue, // ✅ Usar companyId (consistente con create)
      type: establishment.type || 'SUCURSAL',
      status:
        establishment.status !== undefined
          ? Boolean(establishment.status)
          : true,
    };

    // Validar que companyId existe antes de enviar
    if (
      !requestBody.companyId ||
      requestBody.companyId === null ||
      requestBody.companyId === undefined
    ) {
      throw new Error('El ID de la compañía es obligatorio');
    }

    return this.http.put<Establishment>(
      `${this.url}/${idEstablishment}`,
      requestBody,
      { params }
    );
  }

  /**
   * Elimina una sucursal.
   * Sin cascade: falla con 400 si tiene puntos de emisión activos.
   * Con cascade: desactiva primero los puntos de emisión y luego la sucursal.
   * Backend: DELETE /api/establishments/{id}?companyId={companyId}&cascade={true|false}
   */
  delete(
    idEstablishment: number,
    companyId: number,
    cascade = false,
  ): Observable<void> {
    let params = new HttpParams().set('companyId', companyId.toString());
    if (cascade) {
      params = params.set('cascade', 'true');
    }
    return this.http.delete<void>(`${this.url}/${idEstablishment}`, { params });
  }

  /**
   * Valida si el código de sucursal ya existe para una compañía
   * Backend: GET /api/establishments/company/{idCompany}/exists/{code}
   */
  existsByCodeAndCompany(code: string, idCompany: number): Observable<boolean> {
    return this.http.get<boolean>(
      `${this.url}/company/${idCompany}/exists/${code}`
    );
  }
}
