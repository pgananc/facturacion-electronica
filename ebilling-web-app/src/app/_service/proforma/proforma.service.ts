import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ProformaCreateRequestDto,
  ProformaUpdateRequestDto,
  ProformaDto,
  ProformaListResponseDto,
  ProformaConvertToInvoiceResponseDto,
} from '../../_model/proforma/proforma-dto';
import { PageResponse } from '../../_model/invoice/debit-note-dto';

/**
 * ProformaService - Servicio para gestión de proformas
 *
 * Base URL: /api/proforma
 * Auth: Bearer JWT (manejado por JwtModule)
 */
@Injectable({ providedIn: 'root' })
export class ProformaService {
  private readonly baseUrl = `${environment.HOST_EBILLING}/api/proforma`;

  constructor(private readonly http: HttpClient) {}

  /**
   * Crear proforma
   * POST /api/proforma
   */
  create(dto: ProformaCreateRequestDto): Observable<ProformaDto> {
    return this.http.post<ProformaDto>(this.baseUrl, dto);
  }

  /**
   * Actualizar proforma (solo DRAFT)
   * PUT /api/proforma/{id}
   */
  update(id: number, dto: ProformaUpdateRequestDto): Observable<ProformaDto> {
    return this.http.put<ProformaDto>(`${this.baseUrl}/${id}`, dto);
  }

  /**
   * Obtener proforma por id
   * GET /api/proforma/{id}?idCompany=1
   */
  findById(id: number, idCompany: number): Observable<ProformaDto> {
    const params = new HttpParams().set('idCompany', idCompany.toString());
    return this.http.get<ProformaDto>(`${this.baseUrl}/${id}`, { params });
  }

  /**
   * Listar/buscar proformas (paginado)
   * GET /api/proforma?idCompany=1&page=0&size=20&sort=issueDate,desc
   * Query params opcionales: issueDateFrom, issueDateTo, status, idClient, sort
   */
  search(
    idCompany: number,
    page: number = 0,
    size: number = 20,
    params?: {
      issueDateFrom?: string;
      issueDateTo?: string;
      status?: string;
      identification?: string;
      sort?: string;
    },
  ): Observable<PageResponse<ProformaListResponseDto>> {
    let httpParams = new HttpParams()
      .set('idCompany', idCompany.toString())
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', params?.sort || 'issueDate,desc');

    if (params?.issueDateFrom) {
      httpParams = httpParams.set('issueDateFrom', params.issueDateFrom);
    }
    if (params?.issueDateTo) {
      httpParams = httpParams.set('issueDateTo', params.issueDateTo);
    }
    if (params?.status) {
      httpParams = httpParams.set('status', params.status);
    }
    if (params?.identification != null) {
      httpParams = httpParams.set('identification', params.identification);
    }

    return this.http.get<PageResponse<ProformaListResponseDto>>(this.baseUrl, {
      params: httpParams,
    });
  }

  /**
   * Eliminar proforma (solo DRAFT)
   * DELETE /api/proforma/{id}?idCompany=1
   */
  delete(id: number, idCompany: number): Observable<void> {
    const params = new HttpParams().set('idCompany', idCompany.toString());
    return this.http.delete<void>(`${this.baseUrl}/${id}`, { params });
  }

  /**
   * Convertir proforma a factura (draft)
   * POST /api/proforma/{id}/convert-to-invoice?idCompany=1
   */
  convertToInvoice(
    id: number,
    idCompany: number,
  ): Observable<ProformaConvertToInvoiceResponseDto> {
    const params = new HttpParams().set('idCompany', idCompany.toString());
    return this.http.post<ProformaConvertToInvoiceResponseDto>(
      `${this.baseUrl}/${id}/convert-to-invoice`,
      {},
      { params },
    );
  }

  /**
   * Descargar PDF de proforma
   * GET /api/proforma/{id}/pdf?idCompany=1
   */
  getPdf(id: number, idCompany: number): Observable<Blob> {
    const params = new HttpParams().set('idCompany', idCompany.toString());
    return this.http.get(`${this.baseUrl}/${id}/pdf`, {
      params,
      responseType: 'blob',
    });
  }
}
