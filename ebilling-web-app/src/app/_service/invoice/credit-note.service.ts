import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { shareReplay, catchError } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import {
  CreditNoteDto,
  CreditNoteResponse,
  CreditNoteSearchDto,
  CreditNoteReasonDto,
  InvoiceAvailableForCreditNoteDto,
  CreateCreditNoteRequest,
  CreditNoteSearchByCompanyDto,
} from '../../_model/invoice/credit-note-dto';
import { PageResponse } from '../../_model/invoice/debit-note-dto';
import { CreditNoteSearchByNumberOrAccessKeyDto, DocumentBasicInfoDto } from '../../_model/guide/document-search-dto';

/**
 * CreditNoteService - Servicio de Notas de Crédito
 *
 * El token JWT es manejado automáticamente por JwtModule
 */
@Injectable({ providedIn: 'root' })
export class CreditNoteService {
  private readonly baseUrl = `${environment.HOST_EBILLING}/api/credit-notes`;
  private readonly reasonsUrl = `${environment.HOST_EBILLING}/api/credit-note-reason`;
  private readonly invoiceUrl = `${environment.HOST_EBILLING}/api/invoice`;

  // Caché de motivos (se carga una vez y se reutiliza)
  private reasonsCache$: Observable<CreditNoteReasonDto[]> | null = null;

  constructor(private readonly http: HttpClient) {}

  /**
   * Obtiene el catálogo de motivos de NC (con caché en memoria)
   * GET /api/credit-note-reason
   */
  getReasons(): Observable<CreditNoteReasonDto[]> {
    if (!this.reasonsCache$) {
      this.reasonsCache$ = this.http
        .get<CreditNoteReasonDto[]>(this.reasonsUrl)
        .pipe(shareReplay(1));
    }
    return this.reasonsCache$;
  }

  /**
   * Obtiene el detalle de una factura disponible para NC
   * GET /api/invoice/{id}/available-for-credit-note
   */
  getInvoiceAvailableForCreditNote(
    idInvoice: number
  ): Observable<InvoiceAvailableForCreditNoteDto> {
    return this.http.get<InvoiceAvailableForCreditNoteDto>(
      `${this.invoiceUrl}/${idInvoice}/available-for-credit-note`
    );
  }

  /**
   * Crea y envía una nota de crédito al SRI
   * POST /api/credit-notes
   */
  createAndSend(
    request: CreateCreditNoteRequest
  ): Observable<CreditNoteResponse> {
    return this.http.post<CreditNoteResponse>(this.baseUrl, request);
  }

  /**
   * Guarda una nota de crédito como borrador (sin enviar al SRI)
   * POST /api/credit-notes/draft
   */
  saveDraft(request: CreateCreditNoteRequest): Observable<CreditNoteResponse> {
    return this.http.post<CreditNoteResponse>(`${this.baseUrl}/draft`, request);
  }

  /**
   * Envía un borrador al SRI
   * POST /api/credit-notes/{id}/send
   */
  sendDraft(
    idCreditNote: number,
    password: string
  ): Observable<CreditNoteResponse> {
    return this.http.post<CreditNoteResponse>(
      `${this.baseUrl}/${idCreditNote}/send`,
      { password }
    );
  }

  /**
   * Envía la NC por email al cliente
   * POST /api/credit-notes/{id}/send-email
   */
  sendEmail(
    idCreditNote: number,
    email?: string
  ): Observable<{ success: boolean; message: string }> {
    const body = email ? { email } : {};
    return this.http.post<{ success: boolean; message: string }>(
      `${this.baseUrl}/${idCreditNote}/send-email`,
      body
    );
  }

  /**
   * Lista todas las notas de crédito
   * GET /api/credit-notes
   */
  findAll(): Observable<CreditNoteDto[]> {
    return this.http.get<CreditNoteDto[]>(this.baseUrl);
  }

  /**
   * Obtiene una nota de crédito por ID
   * GET /api/credit-notes/{id}
   */
  findById(id: number): Observable<CreditNoteDto> {
    return this.http.get<CreditNoteDto>(`${this.baseUrl}/${id}`);
  }

  /**
   * Obtiene un borrador de nota de crédito por ID
   * GET /api/credit-notes/draft/{id}
   */
  getDraftById(idCreditNote: number): Observable<CreditNoteDto> {
    return this.http.get<CreditNoteDto>(`${this.baseUrl}/draft/${idCreditNote}`);
  }

  /**
   * Crea y envía una nota de crédito al SRI
   * POST /api/credit-notes
   */
  create(creditNote: CreditNoteDto): Observable<CreditNoteResponse> {
    return this.http.post<CreditNoteResponse>(this.baseUrl, creditNote);
  }

  /**
   * Actualiza una nota de crédito (solo borradores)
   * PUT /api/credit-notes/{id}
   */
  update(
    id: number,
    creditNote: CreditNoteDto
  ): Observable<CreditNoteResponse> {
    return this.http.put<CreditNoteResponse>(
      `${this.baseUrl}/${id}`,
      creditNote
    );
  }

  /**
   * Elimina una nota de crédito (solo borradores)
   * DELETE /api/credit-notes/{id}
   */
  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  /**
   * Busca notas de crédito con filtros y paginación
   * GET /api/credit-notes/search
   */
  search(
    searchDto: CreditNoteSearchDto,
    page: number = 0,
    size: number = 10,
    sortField?: string,
    sortDirection?: string
  ): Observable<any> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    // Agregar filtros opcionales
    if (searchDto.creditNoteNumber) {
      params = params.set('creditNoteNumber', searchDto.creditNoteNumber);
    }
    if (searchDto.idClient) {
      params = params.set('idClient', searchDto.idClient.toString());
    }
    if (searchDto.originalInvoiceId) {
      params = params.set(
        'originalInvoiceId',
        searchDto.originalInvoiceId.toString()
      );
    }

    // Agregar ordenamiento si está presente
    if (sortField && sortDirection) {
      params = params.set('sort', `${sortField},${sortDirection}`);
    }

    return this.http.get<any>(`${this.baseUrl}/search`, { params });
  }

  /**
   * Obtiene todas las notas de crédito de una factura original
   * GET /api/credit-notes/by-invoice/{originalInvoiceId}
   */
  findByOriginalInvoice(
    originalInvoiceId: number
  ): Observable<CreditNoteDto[]> {
    return this.http.get<CreditNoteDto[]>(
      `${this.baseUrl}/by-invoice/${originalInvoiceId}`
    );
  }

  /**
   * Descarga el PDF de una nota de crédito
   * GET /api/credit-notes/{id}/pdf
   */
  downloadPdf(idCreditNote: number): Observable<Blob> {
    const url = `${this.baseUrl}/${idCreditNote}/pdf`;
    return this.http.get(url, { responseType: 'blob' });
  }

  /**
   * Descarga el XML de una nota de crédito
   * GET /api/credit-notes/{id}/xml
   */
  downloadXml(idCreditNote: number): Observable<Blob> {
    const url = `${this.baseUrl}/${idCreditNote}/xml`;
    return this.http.get(url, { responseType: 'blob' });
  }

  /**
   * Busca notas de crédito por compañía con filtros y paginación
   * POST /api/credit-notes/search-by-company
   */
  searchByCompany(
    searchDto: CreditNoteSearchByCompanyDto,
    page: number = 0,
    size: number = 10
  ): Observable<PageResponse<CreditNoteDto>> {
    const url = `${this.baseUrl}/search-by-company`;
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.post<PageResponse<CreditNoteDto>>(url, searchDto, { params });
  }

  /**
   * Busca nota de crédito autorizada activa por número o clave de acceso
   * POST /api/credit-notes/search-authorized-active
   * Retorna un array de DocumentBasicInfoDto
   */
  searchAuthorizedActive(
    searchDto: CreditNoteSearchByNumberOrAccessKeyDto
  ): Observable<DocumentBasicInfoDto | DocumentBasicInfoDto[]> {
    const url = `${this.baseUrl}/search-authorized-active`;
    return this.http.post<DocumentBasicInfoDto | DocumentBasicInfoDto[]>(url, searchDto);
  }
}
