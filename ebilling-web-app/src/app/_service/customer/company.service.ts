import { Injectable } from '@angular/core';
import { Subject, throwError, of, Observable } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { CompanyResponse } from '../../_model/customer/company-response';
import { environment } from '../../../environments/environment';
import { CompanyFilter } from '../../_model/customer/company-filter';

@Injectable({
  providedIn: 'root',
})
export class CompanyService {
  companiesChange = new Subject<CompanyFilter[]>();
  companyChange = new Subject<CompanyFilter>();
  companyNameChange = new Subject<string>();
  messageChange = new Subject<string>();
  url: string = `${environment.HOST_CUSTOMERS}/api/company`;

  /** Si true, el diálogo de compañías ya fue abierto con mensaje de advertencia (sin puntos/suscripción). Evita que loadDataCompanies abra otro sin advertencia al recrear Welcome. */
  private companyDialogOpenedWithWarning = false;

  constructor(private readonly http: HttpClient) {}

  setCompanyDialogOpenedWithWarning(value: boolean): void {
    this.companyDialogOpenedWithWarning = value;
  }

  getCompanyDialogOpenedWithWarning(): boolean {
    return this.companyDialogOpenedWithWarning;
  }

  save(company: CompanyFilter) {
    return this.http.post(this.url, company);
  }

  update(company: CompanyFilter, id: number) {
    return this.http.patch(`${this.url}/${id}`, company);
  }

  findAll() {
    return this.http.get<CompanyResponse>(this.url);
  }

  findById(id: number) {
    return this.http.get<CompanyResponse>(`${this.url}/${id}`);
  }

  delete(idCompany: number) {
    return this.http.delete(`${this.url}/${idCompany}`);
  }

  searchPageable(p: number, s: number, company: CompanyFilter) {
    return this.http.post<any>(
      `${this.url}/pageable?page=${p}&size=${s}`,
      company
    );
  }

  existsByIdentification(identification: string) {
    return this.http.get<boolean>(`${this.url}/exist/${identification}`);
  }

  findByIds(ids: number[]) {
    const idsParam = ids.join(',');
    const url = `${this.url}/companies/${idsParam}`;

    return this.http.get<CompanyResponse>(url).pipe(
      catchError((error) => {
        console.error(`[CompanyService] ❌ Error al obtener compañías:`, {
          url,
          idsParam,
          status: error?.status,
          statusText: error?.statusText,
          message: error?.message,
          error: error?.error,
        });

        // Si es un error 500, puede ser que el microservicio no esté disponible
        if (error?.status === 500) {
          console.error(
            `[CompanyService] ⚠️ Error 500 - Verificar que el microservicio en puerto 8180 esté levantado`
          );
        }

        // Re-lanzar el error para que el componente lo maneje
        return throwError(() => error);
      })
    );
    //DEBO RECUPERAR EL ESTABLISHMENT CON SUS PUNTOS DE EMISION POR CADA COMPANY
  }

  /**
   * Sube el logo de una compañía
   * POST /api/company/{idCompany}/logo
   *
   * @param idCompany ID de la compañía
   * @param file Archivo de imagen a subir
   * @returns Observable con la respuesta del servidor (message y logoPath)
   */
  uploadLogo(idCompany: number, file: File) {
    const formData = new FormData();
    formData.append('file', file, file.name);
    return this.http.post<{ message: string; logoPath: string }>(
      `${this.url}/${idCompany}/logo`,
      formData
    );
  }

  /**
   * Obtiene la ruta del logo de una compañía
   * GET /api/company/{idCompany}/logo
   *
   * @param idCompany ID de la compañía
   * @returns Observable con la ruta del logo (logoPath)
   */
  getLogoPath(idCompany: number) {
    return this.http.get<{ logoPath: string }>(`${this.url}/${idCompany}/logo`);
  }

  /**
   * Obtiene la URL completa para mostrar el logo de una compañía
   * GET /api/company/{idCompany}/logo/image
   *
   * Esta URL se puede usar directamente en el atributo src de una etiqueta <img>
   *
   * @param idCompany ID de la compañía
   * @returns URL completa del logo para usar en <img src>
   */
  getLogoUrl(idCompany: number): string {
    return `${this.url}/${idCompany}/logo/image`;
  }

  /**
   * Obtiene el logo de una compañía como Blob (para crear URL de objeto)
   * GET /api/company/{idCompany}/logo/image
   *
   * Este método usa HttpClient que incluye el token JWT automáticamente
   * Si el logo no existe (404), el interceptor lo manejará silenciosamente
   *
   * @param idCompany ID de la compañía
   * @returns Observable<Blob> con la imagen, o error con status 404 si no existe
   */
  getLogoAsBlob(idCompany: number): Observable<Blob> {
    return this.http.get(`${this.url}/${idCompany}/logo/image`, {
      responseType: 'blob',
    });
  }

  /**
   * Elimina el logo de una compañía
   * DELETE /api/company/{idCompany}/logo
   *
   * @param idCompany ID de la compañía
   * @returns Observable con la respuesta del servidor (message)
   */
  deleteLogo(idCompany: number): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(
      `${this.url}/${idCompany}/logo`
    );
  }
}
