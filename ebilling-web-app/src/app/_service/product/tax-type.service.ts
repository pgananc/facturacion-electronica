import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { map, tap, catchError } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { TaxTypeDto } from '../../_model/product/tax-type-dto';
import { TaxTypeResponse } from '../../_model/product/tax-type-response';
import { LoggerService } from '../logger/logger.service';

/**
 * Servicio para gestionar tipos de impuestos con cache en memoria
 * Los tipos de impuestos son un catálogo que cambia muy poco,
 * por lo que se cachean para mejorar el rendimiento.
 */
@Injectable({
  providedIn: 'root',
})
export class TaxTypeService {
  private readonly url = `${environment.HOST_PRODUCTS}/api/tax-type`;

  // Cache en memoria de tipos de impuestos activos
  private taxTypesCache$ = new BehaviorSubject<TaxTypeDto[]>([]);
  private isCacheLoaded = false;

  constructor(
    private readonly http: HttpClient,
    private readonly logger: LoggerService
  ) {}

  /**
   * Obtiene los tipos de impuestos activos
   * Si ya están en cache, los devuelve inmediatamente
   * Si no, los carga del backend
   * @param forceRefresh Si es true, fuerza la recarga desde el backend
   * @param type Filtro opcional por tipo: 'IVA' o 'ICE' (Estrategia B: Queries específicos)
   */
  getTaxTypes(
    forceRefresh: boolean = false,
    type?: 'IVA' | 'ICE'
  ): Observable<TaxTypeDto[]> {
    // Construir parámetros
    let params = new HttpParams().set('status', 'true');
    if (type) {
      params = params.set('type', type);
    }

    // Si hay filtro por tipo, NO usar cache (queries específicos - Estrategia B)
    if (type) {
      return this.http.get<TaxTypeResponse>(this.url, { params }).pipe(
        map((response) => response.taxTypes || []),
        catchError((error) => {
          this.logger.error(`Error al cargar tax types ${type}`, error);
          return of([]);
        })
      );
    }

    // Si no hay filtro y tenemos cache, devolver cache
    if (
      this.isCacheLoaded &&
      !forceRefresh &&
      this.taxTypesCache$.value.length > 0
    ) {
      return of(this.taxTypesCache$.value);
    }

    // Cargar desde el backend
    return this.http.get<TaxTypeResponse>(this.url, { params }).pipe(
      map((response) => response.taxTypes || []),
      tap((taxTypes) => {
        this.taxTypesCache$.next(taxTypes);
        this.isCacheLoaded = true;
      }),
      catchError((error) => {
        this.logger.error('Error al cargar tax types', error);
        return of([]);
      })
    );
  }

  /**
   * Observable del cache de tipos de impuestos
   * Útil para suscribirse a cambios en el cache
   */
  get taxTypes$(): Observable<TaxTypeDto[]> {
    return this.taxTypesCache$.asObservable();
  }

  /**
   * Obtiene un tipo de impuesto por ID desde el cache
   */
  getTaxTypeById(idTaxType: number): TaxTypeDto | undefined {
    return this.taxTypesCache$.value.find((tt) => tt.idTaxType === idTaxType);
  }

  /**
   * Limpia el cache (útil si se actualizan los tipos de impuestos)
   */
  clearCache(): void {
    this.taxTypesCache$.next([]);
    this.isCacheLoaded = false;
  }

  /**
   * Pre-carga los tipos de impuestos en el cache
   * Útil para llamar al inicio de la aplicación o módulo
   */
  preloadCache(): void {
    if (!this.isCacheLoaded) {
      this.getTaxTypes().subscribe();
    }
  }
}
