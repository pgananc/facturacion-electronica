import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { InvoiceDto, InvoiceSearchForDebitNoteDto, InvoiceForDebitNoteResponseDto, InvoiceSearchForCreditNoteDto } from '../../_model/invoice/invoice-dto';
import { PageResponse } from '../../_model/invoice/debit-note-dto';
import { InvoiceSearchDto } from 'src/app/_model/invoice/invoice-search-dto';
import { InvoiceSearchResponseDto } from 'src/app/_model/invoice/invoice-search-response-dto';
import { DocumentQueryDto, DocumentSearchResponseDto } from 'src/app/_model/invoice/document-query-dto';
import { InvoiceSearchByNumberOrAccessKeyDto, DocumentBasicInfoDto } from '../../_model/guide/document-search-dto';
import { InvoiceSearchByCompanyAndDateDto } from '../../_model/invoice/invoice-search-by-company-dto';
import { InvoiceListResponseDto } from '../../_model/invoice/invoice-list-response-dto';
import { HttpParams } from '@angular/common/http';

/**
 * InvoiceService - Servicio de facturación
 *
 * El token JWT es manejado automáticamente por JwtModule
 * (igual que ProductService, CompanyService, etc.)
 */
@Injectable({ providedIn: 'root' })
export class InvoiceService {
  private readonly baseUrl = `${environment.HOST_EBILLING}/api/invoice`;

  constructor(private readonly http: HttpClient) {}

  /**
   * Registra una nueva factura (borrador o para firmar)
   * POST /api/invoice
   */
  register(invoice: InvoiceDto): Observable<any> {
    return this.http.post(this.baseUrl, invoice);
  }

  /**
   * Obtiene una factura por ID
   * GET /api/invoice/{id}
   */
  findById(id: number): Observable<any> {
    return this.http.get(`${this.baseUrl}/${id}`);
  }

  /**
   * Lista todas las facturas (con paginación)
   * GET /api/invoice
   */
  findAll(page: number = 0, size: number = 10): Observable<any> {
    return this.http.get(`${this.baseUrl}?page=${page}&size=${size}`);
  }

  searchPageable(
    p: number,
    s: number,
    client: InvoiceSearchDto,
    sortField?: string,
    sortDirection?: string
  ) {
    let url = `${this.baseUrl}/query?page=${p}&size=${s}`;

    // Add sorting parameters ONLY if both field and direction are provided and valid
    if (
      sortField &&
      sortField.trim() !== '' &&
      sortDirection &&
      sortDirection.trim() !== ''
    ) {
      url += `&sort=${sortField},${sortDirection}`;
    }

    return this.http.post<InvoiceSearchResponseDto>(url, client);
  }

  /**
   * Descarga el PDF de una factura
   * GET /api/invoice/{id}/pdf
   */
  downloadPdf(idInvoice: number): Observable<Blob> {
    const url = `${this.baseUrl}/${idInvoice}/pdf`;
    return this.http.get(url, { responseType: 'blob' });
  }

  /**
   * Descarga el XML de una factura
   * GET /api/invoice/{id}/xml
   */
  downloadXml(idInvoice: number): Observable<Blob> {
    const url = `${this.baseUrl}/${idInvoice}/xml`;
    return this.http.get(url, { responseType: 'blob' });
  }

  /**
   * Consulta documentos (facturas y/o notas de crédito) con filtros y paginación
   * POST /api/invoice/documents/query
   */
  queryDocuments(
    page: number,
    size: number,
    queryDto: DocumentQueryDto,
    sortField?: string,
    sortDirection?: string
  ): Observable<any> {
    let url = `${this.baseUrl}/documents/query?page=${page}&size=${size}`;

    // Add sorting parameters ONLY if both field and direction are provided and valid
    if (
      sortField &&
      sortField.trim() !== '' &&
      sortDirection &&
      sortDirection.trim() !== ''
    ) {
      url += `&sort=${sortField},${sortDirection}`;
    }

    return this.http.post<any>(url, queryDto);
  }

  /**
   * Busca facturas autorizadas para crear nota de débito
   * POST /api/invoice/search-for-debit-note
   */
  searchForDebitNote(
    searchDto: InvoiceSearchForDebitNoteDto,
    page: number = 0,
    size: number = 100
  ): Observable<PageResponse<InvoiceForDebitNoteResponseDto>> {
    const url = `${this.baseUrl}/search-for-debit-note`;
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.post<PageResponse<InvoiceForDebitNoteResponseDto>>(url, searchDto, { params });
  }

  /**
   * Busca facturas autorizadas para crear nota de crédito
   * POST /api/invoice/search-for-credit-note
   */
  searchForCreditNote(
    searchDto: InvoiceSearchForCreditNoteDto,
    page: number = 0,
    size: number = 100
  ): Observable<PageResponse<InvoiceForDebitNoteResponseDto>> {
    const url = `${this.baseUrl}/search-for-credit-note`;
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.post<PageResponse<InvoiceForDebitNoteResponseDto>>(url, searchDto, { params });
  }

  /**
   * Busca factura autorizada activa por número o clave de acceso
   * POST /api/invoice/search-authorized-active
   * Retorna un array de DocumentBasicInfoDto
   */
  searchAuthorizedActive(
    searchDto: InvoiceSearchByNumberOrAccessKeyDto
  ): Observable<DocumentBasicInfoDto | DocumentBasicInfoDto[]> {
    const url = `${this.baseUrl}/search-authorized-active`;
    return this.http.post<DocumentBasicInfoDto | DocumentBasicInfoDto[]>(url, searchDto);
  }

  /**
   * Busca facturas por compañía y rango de fechas con filtros opcionales
   * POST /api/invoice/search
   * Retorna una página de InvoiceListResponseDto
   */
  search(
    searchDto: InvoiceSearchByCompanyAndDateDto,
    page: number = 0,
    size: number = 10
  ): Observable<PageResponse<InvoiceListResponseDto>> {
    const url = `${this.baseUrl}/search`;
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.post<PageResponse<InvoiceListResponseDto>>(url, searchDto, { params });
  }
}
