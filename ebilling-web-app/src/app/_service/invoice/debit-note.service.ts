import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  DebitNoteDto,
  DebitNoteResponse,
  DebitNoteSearchDto,
  DebitNoteQueryDto,
  DebitNoteSearchResponseDto,
  DebitNoteSearchByCompanyDto,
  DebitNoteListResponseDto,
  PageResponse,
} from '../../_model/invoice/debit-note-dto';
import { DebitNoteSearchByNumberOrAccessKeyDto, DocumentBasicInfoDto } from '../../_model/guide/document-search-dto';

/**
 * DebitNoteService - Servicio de Notas de Débito
 *
 * El token JWT es manejado automáticamente por JwtModule
 */
@Injectable({ providedIn: 'root' })
export class DebitNoteService {
  private readonly baseUrl = `${environment.HOST_EBILLING}/api/debit-notes`;

  constructor(private readonly http: HttpClient) {}

  /**
   * Crea una nueva nota de débito y la envía al SRI
   * POST /api/debit-notes
   */
  create(debitNote: DebitNoteDto): Observable<DebitNoteResponse> {
    return this.http.post<DebitNoteResponse>(this.baseUrl, debitNote);
  }

  /**
   * Guarda una nota de débito como borrador
   * POST /api/debit-notes/draft
   */
  createDraft(debitNote: DebitNoteDto): Observable<DebitNoteResponse> {
    return this.http.post<DebitNoteResponse>(`${this.baseUrl}/draft`, debitNote);
  }

  /**
   * Actualiza una nota de débito (solo borradores)
   * PUT /api/debit-notes/{id}
   */
  update(id: number, debitNote: Partial<DebitNoteDto>): Observable<DebitNoteResponse> {
    return this.http.put<DebitNoteResponse>(`${this.baseUrl}/${id}`, debitNote);
  }

  /**
   * Elimina una nota de débito (solo borradores)
   * DELETE /api/debit-notes/{id}
   */
  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  /**
   * Obtiene una nota de débito por ID
   * GET /api/debit-notes/{id}
   */
  findById(id: number): Observable<DebitNoteDto> {
    return this.http.get<DebitNoteDto>(`${this.baseUrl}/${id}`);
  }

  /**
   * Obtiene un borrador de nota de débito por ID
   * GET /api/debit-notes/draft/{id}
   */
  getDraftById(idDebitNote: number): Observable<DebitNoteDto> {
    return this.http.get<DebitNoteDto>(`${this.baseUrl}/draft/${idDebitNote}`);
  }

  /**
   * Lista todas las notas de débito
   * GET /api/debit-notes
   */
  findAll(): Observable<DebitNoteDto[]> {
    return this.http.get<DebitNoteDto[]>(this.baseUrl);
  }

  /**
   * Busca notas de débito por compañía con filtros y paginación
   * POST /api/debit-notes/search-by-company
   */
  searchByCompany(
    searchDto: DebitNoteSearchByCompanyDto,
    page: number = 0,
    size: number = 10
  ): Observable<PageResponse<DebitNoteListResponseDto>> {
    const url = `${this.baseUrl}/search-by-company`;
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.post<PageResponse<DebitNoteListResponseDto>>(url, searchDto, { params });
  }

  /**
   * Busca notas de débito con filtros y paginación
   * GET /api/debit-notes/search
   */
  search(
    query: DebitNoteQueryDto,
    page: number = 0,
    size: number = 20,
    sort?: string
  ): Observable<any> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (query.debitNoteNumber) {
      params = params.set('debitNoteNumber', query.debitNoteNumber);
    }
    if (query.idClient) {
      params = params.set('idClient', query.idClient.toString());
    }
    if (query.originalInvoiceId) {
      params = params.set('originalInvoiceId', query.originalInvoiceId.toString());
    }
    if (query.creationDateFrom) {
      params = params.set('creationDateFrom', query.creationDateFrom);
    }
    if (query.creationDateTo) {
      params = params.set('creationDateTo', query.creationDateTo);
    }
    if (sort) {
      params = params.set('sort', sort);
    }

    return this.http.get<any>(`${this.baseUrl}/search`, { params });
  }

  /**
   * Obtiene todas las notas de débito asociadas a una factura original
   * GET /api/debit-notes/by-invoice/{originalInvoiceId}
   */
  findByInvoiceId(originalInvoiceId: number): Observable<DebitNoteDto[]> {
    return this.http.get<DebitNoteDto[]>(
      `${this.baseUrl}/by-invoice/${originalInvoiceId}`
    );
  }

  /**
   * Envía un borrador de nota de débito al SRI
   * POST /api/debit-notes/{id}/send
   */
  sendToSri(id: number, password: string): Observable<DebitNoteResponse> {
    return this.http.post<DebitNoteResponse>(`${this.baseUrl}/${id}/send`, {
      password,
    });
  }

  /**
   * Descarga el PDF de una nota de débito
   * GET /api/debit-notes/{id}/pdf
   */
  downloadPdf(idDebitNote: number): Observable<Blob> {
    const url = `${this.baseUrl}/${idDebitNote}/pdf`;
    return this.http.get(url, { responseType: 'blob' });
  }

  /**
   * Descarga el XML de una nota de débito
   * GET /api/debit-notes/{id}/xml
   */
  downloadXml(idDebitNote: number): Observable<Blob> {
    const url = `${this.baseUrl}/${idDebitNote}/xml`;
    return this.http.get(url, { responseType: 'blob' });
  }

  /**
   * Envía la nota de débito por email al cliente
   * POST /api/debit-notes/{id}/send-email
   */
  sendEmail(id: number, email?: string): Observable<any> {
    const body = email ? { email } : {};
    return this.http.post<any>(`${this.baseUrl}/${id}/send-email`, body);
  }

  /**
   * Busca nota de débito autorizada activa por número o clave de acceso
   * POST /api/debit-notes/search-authorized-active
   * Retorna un array de DocumentBasicInfoDto
   */
  searchAuthorizedActive(
    searchDto: DebitNoteSearchByNumberOrAccessKeyDto
  ): Observable<DocumentBasicInfoDto | DocumentBasicInfoDto[]> {
    const url = `${this.baseUrl}/search-authorized-active`;
    return this.http.post<DocumentBasicInfoDto | DocumentBasicInfoDto[]>(url, searchDto);
  }
}


