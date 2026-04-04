import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, forkJoin, of, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  SubscriptionAdmin,
  PlanChangeRequest,
  SubscriptionExtendRequest,
  SubscriptionCancelRequest,
  AssignSubscriptionRequest,
  AvailablePlan,
  PlanResponse,
  SubscriptionPageResponse,
} from '../../_model/subscription/subscription-admin';
import { SubscriptionPlan } from '../../_model/permission/plan-permission-admin';
import {
  CreatePlanRequest,
  UpdatePlanRequest,
  PlanManagementResponse,
} from '../../_model/subscription/plan-management';
import { map, switchMap, catchError } from 'rxjs/operators';
import { CompanyService } from '../customer/company.service';
import { LoggerService } from '../logger/logger.service';

@Injectable({
  providedIn: 'root',
})
export class SubscriptionAdminService {
  private readonly apiUrl = `${environment.HOST_EBILLING}/api/subscriptions`;

  constructor(
    private readonly http: HttpClient,
    private readonly companyService: CompanyService,
    private readonly logger: LoggerService,
  ) {}

  /**
   * Obtener todas las suscripciones
   * El backend retorna una respuesta paginada de Spring (Page<T>)
   * También obtiene los datos de las empresas para completar la información
   */
  getAllSubscriptions(): Observable<SubscriptionAdmin[]> {
    return this.http.get<SubscriptionPageResponse>(this.apiUrl).pipe(
      switchMap((response) => {
        // Extraer el array 'content' de la respuesta paginada
        const subscriptions = response.content || [];

        if (subscriptions.length === 0) {
          return of([]);
        }

        // Extraer IDs únicos de empresas
        const companyIds = [
          ...new Set(subscriptions.map((sub: any) => sub.idCompany)),
        ];

        // Obtener datos de las empresas usando llamadas individuales (más robusto)
        // Si el endpoint batch falla, usar forkJoin con llamadas individuales
        const companyRequests = companyIds.map((id) =>
          this.companyService.findById(id).pipe(
            catchError((error) => {
              this.logger.warn(`[SubscriptionService] Error al obtener empresa ID=${id}`, error);
              return of(null);
            })
          )
        );

        return forkJoin(companyRequests).pipe(
          map((companyResponses: any[]) => {
            // Crear un mapa de empresas por ID para acceso rápido
            const companiesMap = new Map<number, any>();

            // companyResponses es un array de CompanyResponse (uno por cada ID)
            companyResponses.forEach((companyResponse: any, index: number) => {
              if (!companyResponse) {
                return; // Saltar si la respuesta es null (error)
              }

              // Extraer la empresa de CompanyResponse
              const company =
                companyResponse.companyDetailsDto ||
                companyResponse.companyDetailsDtos?.[0] ||
                companyResponse;

              if (company && (company.idCompany || company.id)) {
                const id = company.idCompany || company.id;
                companiesMap.set(id, company);
              }
            });

            this.logger.debug(`[SubscriptionService] Empresas mapeadas: ${companiesMap.size}/${companyIds.length}`);

            return subscriptions.map((sub: any) => {
              const company = companiesMap.get(sub.idCompany);

              return {
                ...sub,
                // Datos de empresa desde el mapa
                // Company usa 'name' como razón social y 'identification' como RUC
                businessName:
                  company?.name ||
                  company?.businessName ||
                  company?.nameCommercial ||
                  '',
                companyName:
                  company?.name ||
                  company?.companyName ||
                  company?.businessName ||
                  '',
                ruc: company?.identification || company?.ruc || '',
                // Mapear remainingDocuments si viene como documentsRemaining
                remainingDocuments:
                  sub.remainingDocuments ?? sub.documentsRemaining ?? 0,
                // Mapear cancelledAt desde cancelled_at (snake_case del backend)
                cancelledAt: sub.cancelledAt ?? sub.cancelled_at,
              } as SubscriptionAdmin;
            });
          }),
          catchError((error) => {
            console.error(
              '[SubscriptionService] ❌ Error al obtener empresas:',
              error
            );
            // Si falla obtener empresas, retornar suscripciones sin datos de empresa
            return of(
              subscriptions.map((sub: any) => ({
                ...sub,
                businessName: '',
                companyName: '',
                ruc: '',
                remainingDocuments:
                  sub.remainingDocuments ?? sub.documentsRemaining ?? 0,
                // Mapear cancelledAt desde cancelled_at (snake_case del backend)
                cancelledAt: sub.cancelledAt ?? sub.cancelled_at,
              })) as SubscriptionAdmin[]
            );
          })
        );
      })
    );
  }

  /**
   * Obtener suscripción de una empresa específica
   */
  getCompanySubscription(idCompany: number): Observable<SubscriptionAdmin> {
    return this.http.get<SubscriptionAdmin>(
      `${this.apiUrl}/company/${idCompany}`
    );
  }

  /**
   * Cambiar plan de una empresa (upgrade)
   * Timeout manejado globalmente por HttpTimeoutInterceptor (30 segundos por defecto)
   */
  changePlan(idCompany: number, request: PlanChangeRequest): Observable<void> {
    return this.http.post<void>(
      `${this.apiUrl}/company/${idCompany}/upgrade`,
      request
    );
  }

  /**
   * Extender vigencia de una suscripción
   */
  extendSubscription(
    idCompany: number,
    request: SubscriptionExtendRequest
  ): Observable<void> {
    return this.http.post<void>(
      `${this.apiUrl}/company/${idCompany}/extend`,
      request
    );
  }

  /**
   * Cancelar suscripción de una empresa
   * @param idCompany ID de la empresa
   * @param request Datos de cancelación (razón, cancelar inmediatamente, notas)
   */
  cancelSubscription(
    idCompany: number,
    request: SubscriptionCancelRequest
  ): Observable<void> {
    return this.http.delete<void>(
      `${this.apiUrl}/company/${idCompany}/cancel`,
      { body: request }
    );
  }

  /**
   * Obtener planes disponibles
   * Intenta usar GET /api/subscriptions/plans del backend
   * Si el endpoint no existe o falla, usa datos hardcoded como fallback
   *
   * @param includeInactive - Si es true, incluye planes inactivos (por defecto false)
   */
  getAvailablePlans(
    includeInactive: boolean = false
  ): Observable<AvailablePlan[]> {
    let params = new HttpParams();
    if (includeInactive) {
      params = params.set('includeInactive', 'true');
    }

    return this.http
      .get<PlanResponse[]>(`${this.apiUrl}/plans`, { params })
      .pipe(
        map((plans: PlanResponse[]) => {
          // Mapear PlanResponse[] a AvailablePlan[] para compatibilidad con componentes
          return plans
            .filter((plan) => includeInactive || plan.isActive) // Filtrar inactivos si no se solicitan
            .sort((a, b) => a.displayOrder - b.displayOrder) // Ordenar por displayOrder
            .map((plan) => ({
              code: plan.code,
              name: plan.name,
              price: plan.priceMonthly, // Usar precio mensual
              documentLimit: plan.documentLimitYearly, // Mapear documentLimitYearly -> documentLimit
              features: plan.features || [], // Usar features del backend
            }));
        }),
        catchError((error) => {
          // Si el endpoint no existe (404) o hay otro error, usar datos hardcoded
          this.logger.warn(
            '[SubscriptionService] Endpoint /api/subscriptions/plans no disponible, usando fallback',
            { status: error.status },
          );
          return of(this.getHardcodedPlans());
        })
      );
  }

  /**
   * Obtener todos los planes disponibles (versión para permisos)
   * Retorna SubscriptionPlan[] para uso en componentes de permisos
   *
   * @param includeInactive - Si es true, incluye planes inactivos (por defecto false)
   */
  getAllPlans(
    includeInactive: boolean = false
  ): Observable<SubscriptionPlan[]> {
    let params = new HttpParams();
    if (includeInactive) {
      params = params.set('includeInactive', 'true');
    }

    return this.http
      .get<PlanResponse[]>(`${this.apiUrl}/plans`, { params })
      .pipe(
        map((plans: PlanResponse[]) => {
          // Mapear PlanResponse[] a SubscriptionPlan[]
          return plans
            .filter((plan) => includeInactive || plan.isActive) // Filtrar inactivos si no se solicitan
            .sort((a, b) => a.displayOrder - b.displayOrder) // Ordenar por displayOrder
            .map((plan) => ({
              code: plan.code,
              name: plan.name,
              description: plan.description || plan.features?.join(', ') || '',
            }));
        }),
        catchError((error) => {
          // Si el endpoint no existe (404) o hay otro error, usar datos hardcoded
          this.logger.warn(
            '[SubscriptionAdminService] Endpoint /api/subscriptions/plans no disponible, usando fallback',
            { status: error.status },
          );
          return of(this.getHardcodedSubscriptionPlans());
        })
      );
  }

  /**
   * Planes hardcoded como fallback para SubscriptionPlan[]
   * Se usa cuando el endpoint GET /api/subscriptions/plans no está disponible
   */
  private getHardcodedSubscriptionPlans(): SubscriptionPlan[] {
    return [
      {
        code: 'FREE',
        name: 'Gratuito',
        description: 'Plan gratuito con funcionalidades básicas',
      },
      {
        code: 'BASIC',
        name: 'Básico',
        description: 'Plan básico para pequeñas empresas',
      },
      {
        code: 'PROFESSIONAL',
        name: 'Profesional',
        description: 'Plan profesional con funcionalidades avanzadas',
      },
      {
        code: 'ENTERPRISE',
        name: 'Empresarial',
        description: 'Plan empresarial con todas las funcionalidades',
      },
    ];
  }

  /**
   * Planes hardcoded como fallback para AvailablePlan[]
   * Se usa cuando el endpoint GET /api/subscriptions/plans no está disponible
   */
  private getHardcodedPlans(): AvailablePlan[] {
    return [
      {
        code: 'FREE',
        name: 'Plan Gratuito',
        price: 0,
        documentLimit: 50,
        features: [
          'Hasta 50 documentos/año',
          'Facturación básica',
          'Soporte por email',
        ],
      },
      {
        code: 'BASIC',
        name: 'Plan Básico',
        price: 29.99,
        documentLimit: 500,
        features: [
          'Hasta 500 documentos/año',
          'Notas de crédito',
          'Reportes operativos',
          'Soporte prioritario',
        ],
      },
      {
        code: 'PRO',
        name: 'Plan Profesional',
        price: 79.99,
        documentLimit: 2000,
        features: [
          'Hasta 2000 documentos/año',
          'Reportes avanzados',
          'API REST',
          'Webhooks',
          'Soporte 24/7',
        ],
      },
      {
        code: 'ENTERPRISE',
        name: 'Plan Empresarial',
        price: 199.99,
        documentLimit: -1,
        features: [
          'Documentos ilimitados',
          'Todas las características',
          'Soporte dedicado',
          'SLA garantizado',
          'Personalización',
        ],
      },
    ];
  }

  /**
   * Crear un nuevo plan de suscripción
   * Endpoint: POST /api/subscriptions/plans
   */
  createPlan(request: CreatePlanRequest): Observable<PlanManagementResponse> {
    return this.http.post<PlanManagementResponse>(
      `${this.apiUrl}/plans`,
      request
    );
  }

  /**
   * Actualizar un plan existente
   * Endpoint: PUT /api/subscriptions/plans/{idPlan}
   */
  updatePlan(
    idPlan: number,
    request: UpdatePlanRequest
  ): Observable<PlanManagementResponse> {
    return this.http.put<PlanManagementResponse>(
      `${this.apiUrl}/plans/${idPlan}`,
      request
    );
  }

  /**
   * Eliminar/desactivar un plan
   * Endpoint: DELETE /api/subscriptions/plans/{idPlan}
   */
  deletePlan(idPlan: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/plans/${idPlan}`);
  }

  /**
   * Obtener un plan por ID
   * Endpoint: GET /api/subscriptions/plans/{idPlan}
   */
  getPlanById(idPlan: number): Observable<PlanResponse> {
    return this.http.get<PlanResponse>(`${this.apiUrl}/plans/${idPlan}`);
  }

  /**
   * Obtener todos los planes completos (PlanResponse[])
   * Endpoint: GET /api/subscriptions/plans
   *
   * @param includeInactive - Si es true, incluye planes inactivos (por defecto false)
   */
  getAllPlansFull(
    includeInactive: boolean = false
  ): Observable<PlanResponse[]> {
    let params = new HttpParams();
    if (includeInactive) {
      params = params.set('includeInactive', 'true');
    }

    return this.http.get<PlanResponse[]>(`${this.apiUrl}/plans`, { params });
  }

  /**
   * Obtener empresas sin suscripción (disponibles para asignar)
   * Endpoint: GET /api/subscriptions/available-companies
   */
  getCompaniesWithoutSubscription(): Observable<
    Array<{ idCompany: number; businessName: string; ruc: string }>
  > {
    return this.http.get<
      Array<{ idCompany: number; businessName: string; ruc: string }>
    >(`${this.apiUrl}/available-companies`);
  }

  /**
   * Asignar una nueva suscripción a una empresa
   * Endpoint: POST /api/subscriptions
   * ⚠️ Pendiente de implementación en backend
   */
  assignSubscription(request: AssignSubscriptionRequest): Observable<void> {
    return this.http.post<void>(this.apiUrl, request);
  }
}
