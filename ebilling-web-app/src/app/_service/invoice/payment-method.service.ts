import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, BehaviorSubject } from 'rxjs';
import { tap, shareReplay, catchError } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { LoggerService } from '../logger/logger.service';

export interface PaymentMethodDto {
  idPaymentMethod: number;
  paymentMethodType: string;
  paymentMethodCode: string;
  createdAt?: string;
  updatedAt?: string;
}

/**
 * PaymentMethodService - Servicio de formas de pago con caché en memoria
 *
 * Este servicio implementa un patrón de caché para evitar múltiples llamadas
 * al backend. Las formas de pago se cargan una sola vez y se reutilizan.
 *
 * El token JWT es manejado automáticamente por JwtModule
 * (igual que ProductService, CompanyService, etc.)
 *
 * Beneficios:
 * - ✅ Reduce llamadas innecesarias al backend
 * - ✅ Mejora el rendimiento de la aplicación
 * - ✅ Reduce latencia al abrir el modal de formas de pago
 * - ✅ Datos estáticos que no cambian frecuentemente
 */
@Injectable({
  providedIn: 'root',
})
export class PaymentMethodService {
  private readonly baseUrl = `${environment.HOST_EBILLING}/api/payment-method`;

  // Caché en memoria de las formas de pago
  private paymentMethodsCache: PaymentMethodDto[] | null = null;

  // BehaviorSubject para observar cambios en las formas de pago
  private paymentMethods$ = new BehaviorSubject<PaymentMethodDto[]>([]);

  // Observable compartido para evitar múltiples llamadas simultáneas
  private cachedRequest$: Observable<PaymentMethodDto[]> | null = null;

  constructor(
    private readonly http: HttpClient,
    private readonly logger: LoggerService,
  ) {}

  /**
   * Obtiene todas las formas de pago disponibles
   * - Primera llamada: consulta al backend y guarda en caché
   * - Llamadas posteriores: retorna desde caché (sin llamada HTTP)
   *
   * @param forceRefresh - Si es true, fuerza una recarga desde el backend
   */
  findAll(forceRefresh: boolean = false): Observable<PaymentMethodDto[]> {
    if (this.paymentMethodsCache && !forceRefresh) {
      this.logger.debug('[PaymentMethodService] Retornando desde caché', {
        total: this.paymentMethodsCache.length,
      });
      return of(this.paymentMethodsCache);
    }

    if (this.cachedRequest$ && !forceRefresh) {
      return this.cachedRequest$;
    }

    this.logger.debug('[PaymentMethodService] Cargando desde backend...', {
      url: this.baseUrl,
    });

    this.cachedRequest$ = this.http.get<PaymentMethodDto[]>(this.baseUrl).pipe(
      tap((methods) => {
        this.paymentMethodsCache = methods;
        this.paymentMethods$.next(methods);
        this.cachedRequest$ = null;
        this.logger.debug('[PaymentMethodService] Formas de pago cargadas', {
          total: methods.length,
        });
      }),
      catchError((error) => {
        if (error.status !== 0) {
          console.error(
            '[PaymentMethodService] Error al cargar formas de pago:',
            { status: error.status, url: error.url }
          );
        }
        this.cachedRequest$ = null;
        throw error;
      }),
      // shareReplay evita múltiples suscripciones simultáneas
      shareReplay(1)
    );

    return this.cachedRequest$;
  }

  /**
   * Obtiene una forma de pago por su código SRI
   * @param code - Código SRI (ej: "01", "16", "19")
   */
  findByCode(code: string): PaymentMethodDto | undefined {
    if (!this.paymentMethodsCache) {
      return undefined;
    }

    return this.paymentMethodsCache.find((pm) => pm.paymentMethodCode === code);
  }

  /**
   * Obtiene el observable de formas de pago (para suscripciones reactivas)
   */
  getPaymentMethods$(): Observable<PaymentMethodDto[]> {
    return this.paymentMethods$.asObservable();
  }

  /**
   * Limpia el caché y fuerza una recarga en la próxima llamada
   */
  clearCache(): void {
    this.paymentMethodsCache = null;
    this.paymentMethods$.next([]);
    this.cachedRequest$ = null;
  }

  /**
   * Verifica si el caché está inicializado
   */
  isCacheInitialized(): boolean {
    return this.paymentMethodsCache !== null;
  }

  /**
   * Obtiene las formas de pago desde el caché (síncrono)
   * Retorna un array vacío si el caché no está inicializado
   */
  getCachedPaymentMethods(): PaymentMethodDto[] {
    return this.paymentMethodsCache || [];
  }
}
