import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CarrierDto } from '../../_model/guide/carrier-dto';
import { Page } from '../../_model/guide/page.model';
import { HttpSkipInterceptor } from '../../_shared/http-skip-interceptor';

/**
 * CarrierService - Servicio para gestión de transportistas
 *
 * El token JWT es manejado automáticamente por JwtModule
 */
@Injectable({
  providedIn: 'root',
})
export class CarrierService {
  private readonly apiUrl = `${environment.HOST_EBILLING}/api/carrier`;

  constructor(private readonly http: HttpClient) {}

  /**
   * Lista transportistas de una empresa
   * Usa HttpSkipInterceptor para que el componente maneje los errores con ToastService
   */
  getCarriers(
    idCompany: number,
    activeOnly: boolean = false,
    page: number = 0,
    size: number = 10,
    sort: string = 'name,asc'
  ): Observable<Page<CarrierDto>> {
    let params = new HttpParams()
      .set('idCompany', idCompany.toString())
      .set('activeOnly', activeOnly.toString())
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    // Usar HttpSkipInterceptor para que el componente maneje los errores
    const headers = HttpSkipInterceptor.skipAll();
    return this.http.get<Page<CarrierDto>>(this.apiUrl, { params, headers });
  }

  /**
   * Obtiene un transportista por ID
   */
  getCarrierById(id: number): Observable<CarrierDto> {
    return this.http.get<CarrierDto>(`${this.apiUrl}/${id}`);
  }

  /**
   * Crea un nuevo transportista
   */
  createCarrier(carrier: CarrierDto): Observable<CarrierDto> {
    return this.http.post<CarrierDto>(this.apiUrl, carrier);
  }

  /**
   * Actualiza un transportista
   */
  updateCarrier(id: number, carrier: CarrierDto): Observable<CarrierDto> {
    return this.http.put<CarrierDto>(`${this.apiUrl}/${id}`, carrier);
  }

  /**
   * Elimina (desactiva) un transportista
   */
  deleteCarrier(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}

