import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { InvoiceDto } from '../../_model/invoice/invoice-dto';

/**
 * Servicio para gestionar borradores de facturas (draft invoices)
 *
 * Un borrador es una factura guardada sin:
 * - Secuencial
 * - Firma digital
 * - Envío al SRI
 * - Autorización del SRI
 */
@Injectable({
  providedIn: 'root',
})
export class DraftInvoiceService {
  private readonly url: string = `${environment.HOST_EBILLING}/api/invoice`;

  constructor(private readonly http: HttpClient) {}

  /**
   * Guarda una factura como borrador sin validar firma digital ni enviar al SRI
   *
   * @param invoiceDto - Datos de la factura a guardar como borrador
   * @returns Observable con la respuesta del backend
   */
  saveDraft(invoiceDto: InvoiceDto): Observable<any> {
    return this.http.post<any>(`${this.url}/draft`, invoiceDto);
  }

  /**
   * Obtiene todas las facturas que están en estado borrador
   * Un borrador se identifica porque authorizationSri = NULL
   *
   * @returns Observable con array de facturas borrador
   */
  getDrafts(): Observable<InvoiceDto[]> {
    return this.http.get<InvoiceDto[]>(`${this.url}/drafts`);
  }

  /**
   * Obtiene un borrador específico por su ID
   *
   * @param idInvoice - ID de la factura borrador
   * @returns Observable con los datos completos del borrador
   */
  getDraftById(idInvoice: number): Observable<InvoiceDto> {
    return this.http.get<InvoiceDto>(`${this.url}/draft/${idInvoice}`);
  }

  /**
   * Procesa un borrador: genera secuencial, firma digitalmente y envía al SRI
   *
   * @param idInvoice - ID de la factura borrador a procesar
   * @returns Observable con la respuesta del backend
   */
  processDraft(idInvoice: number): Observable<any> {
    return this.http.post<any>(`${this.url}/draft/${idInvoice}/process`, {});
  }

  /**
   * Elimina un borrador (endpoint que puede agregarse en el futuro)
   *
   * @param idInvoice - ID de la factura borrador a eliminar
   * @returns Observable con la respuesta del backend
   */
  deleteDraft(idInvoice: number): Observable<any> {
    return this.http.delete<any>(`${this.url}/draft/${idInvoice}`);
  }
}
