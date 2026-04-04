import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { GuideDto, GuideResponse, GuideListResponseDto, GuideSearchByCompanyDto, GuideSearchResponseDto } from '../../_model/guide/guide-dto';
import { Page } from '../../_model/guide/page.model';

/**
 * GuideService - Servicio para gestión de guías de remisión
 *
 * El token JWT es manejado automáticamente por JwtModule
 */
@Injectable({
  providedIn: 'root',
})
export class GuideService {
  private readonly apiUrl = `${environment.HOST_EBILLING}/api/guides`;

  constructor(private readonly http: HttpClient) {}

  /**
   * Lista todas las guías de remisión
   */
  getAllGuides(): Observable<GuideListResponseDto[]> {
    return this.http.get<GuideListResponseDto[]>(this.apiUrl);
  }

  /**
   * Obtiene una guía por ID
   */
  getGuideById(id: number): Observable<GuideDto> {
    return this.http.get<GuideDto>(`${this.apiUrl}/${id}`);
  }

  /**
   * Crea una nueva guía y la envía al SRI
   */
  createGuide(guide: GuideDto): Observable<GuideResponse> {
    return this.http.post<GuideResponse>(this.apiUrl, guide);
  }

  /**
   * Guarda una guía como borrador
   */
  saveDraft(guide: GuideDto): Observable<GuideResponse> {
    return this.http.post<GuideResponse>(`${this.apiUrl}/draft`, guide);
  }

  /**
   * Actualiza una guía (solo borradores)
   */
  updateGuide(id: number, guide: GuideDto): Observable<GuideResponse> {
    return this.http.put<GuideResponse>(`${this.apiUrl}/${id}`, guide);
  }

  /**
   * Elimina una guía (solo borradores)
   */
  deleteGuide(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  /**
   * Envía un borrador al SRI
   */
  sendToSri(id: number, password: string): Observable<GuideResponse> {
    const params = new HttpParams().set('password', password);
    return this.http.post<GuideResponse>(`${this.apiUrl}/${id}/send-to-sri`, null, { params });
  }

  /**
   * Busca guías con filtros
   */
  searchGuides(
    filters: {
      guideNumber?: string;
      idClient?: number;
      idCarrier?: number;
    },
    page: number = 0,
    size: number = 20,
    sort: string = 'creationDate,desc'
  ): Observable<Page<any>> {
    let params = new HttpParams().set('page', page.toString()).set('size', size.toString()).set('sort', sort);

    if (filters.guideNumber) {
      params = params.set('guideNumber', filters.guideNumber);
    }
    if (filters.idClient) {
      params = params.set('idClient', filters.idClient.toString());
    }
    if (filters.idCarrier) {
      params = params.set('idCarrier', filters.idCarrier.toString());
    }

    return this.http.get<Page<any>>(`${this.apiUrl}/search`, { params });
  }

  /**
   * Busca guías por compañía (POST)
   */
  searchByCompany(searchDto: GuideSearchByCompanyDto): Observable<GuideSearchResponseDto[]> {
    return this.http.post<GuideSearchResponseDto[]>(`${this.apiUrl}/search-by-company`, searchDto);
  }

  /**
   * Descarga el PDF de una guía
   */
  downloadPdf(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${id}/pdf`, {
      responseType: 'blob',
    });
  }

  /**
   * Descarga el XML de una guía
   */
  downloadXml(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${id}/xml`, {
      responseType: 'blob',
    });
  }

  /**
   * Envía la guía por email
   */
  sendByEmail(id: number, email?: string): Observable<void> {
    let params = new HttpParams();
    if (email) {
      params = params.set('email', email);
    }
    return this.http.post<void>(`${this.apiUrl}/${id}/send-email`, null, { params });
  }
}

