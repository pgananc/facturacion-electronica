package com.wallparisoft.ebillbilling.service;

import com.wallparisoft.ebillbilling.dto.subscription.AssignSubscriptionRequestDto;
import com.wallparisoft.ebillbilling.dto.subscription.CanEmitRequestDto;
import com.wallparisoft.ebillbilling.dto.subscription.CanEmitResponseDto;
import com.wallparisoft.ebillbilling.dto.subscription.CancelSubscriptionRequestDto;
import com.wallparisoft.ebillbilling.dto.subscription.CancelSubscriptionResponseDto;
import com.wallparisoft.ebillbilling.dto.subscription.CompanyWithoutSubscriptionDto;
import com.wallparisoft.ebillbilling.dto.subscription.ExtendSubscriptionRequestDto;
import com.wallparisoft.ebillbilling.dto.subscription.ExtendSubscriptionResponseDto;
import com.wallparisoft.ebillbilling.dto.subscription.SubscriptionListDto;
import com.wallparisoft.ebillbilling.dto.subscription.SubscriptionPlanDto;
import com.wallparisoft.ebillbilling.dto.subscription.SubscriptionResponseDto;
import com.wallparisoft.ebillbilling.dto.subscription.UpgradeRequestDto;
import com.wallparisoft.ebillbilling.dto.subscription.UpgradeResponseDto;
import com.wallparisoft.ebillbilling.dto.subscription.UsageResponseDto;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ISubscriptionService {

  /**
   * Obtiene la suscripción actual de una compañía.
   *
   * @param idCompany ID de la compañía
   * @return Datos de la suscripción actual
   */
  SubscriptionResponseDto getCurrentSubscription(Long idCompany);

  /**
   * Obtiene el uso mensual y anual de una compañía.
   *
   * @param idCompany ID de la compañía
   * @return Datos de uso
   */
  UsageResponseDto getUsage(Long idCompany);

  /**
   * Verifica si una compañía puede emitir un documento.
   *
   * @param idCompany ID de la compañía
   * @param request Tipo de documento a emitir
   * @return Resultado de la verificación
   */
  CanEmitResponseDto canEmitDocument(Long idCompany, CanEmitRequestDto request);

  /**
   * Actualiza el plan de suscripción de una compañía.
   *
   * @param idCompany ID de la compañía
   * @param request Datos del nuevo plan
   * @return Resultado de la actualización
   */
  UpgradeResponseDto upgradePlan(Long idCompany, UpgradeRequestDto request);

  /**
   * Valida si una compañía puede emitir un documento específico. Lanza excepción si no puede.
   *
   * @param idCompany ID de la compañía
   * @param documentType Tipo de documento
   * @throws IllegalStateException si no puede emitir el documento
   */
  void validateCanEmit(Long idCompany, String documentType);

  /**
   * Valida si una compañía puede crear un nuevo usuario.
   *
   * @param idCompany ID de la compañía
   * @param currentUserCount Cantidad actual de usuarios
   * @throws IllegalStateException si alcanzó el límite
   */
  void validateUserLimit(Long idCompany, int currentUserCount);

  /**
   * Valida si una compañía puede crear un nuevo establecimiento.
   *
   * @param idCompany ID de la compañía
   * @param currentEstablishmentCount Cantidad actual de establecimientos
   * @throws IllegalStateException si alcanzó el límite
   */
  void validateEstablishmentLimit(Long idCompany, int currentEstablishmentCount);

  /**
   * Valida si una compañía puede crear un nuevo punto de emisión.
   *
   * @param idCompany ID de la compañía
   * @param currentIssuancePointCount Cantidad actual de puntos de emisión
   * @throws IllegalStateException si alcanzó el límite
   */
  void validateIssuancePointLimit(Long idCompany, int currentIssuancePointCount);

  // =====================================================
  // SUBSCRIPTION_MANAGEMENT - Métodos para gestión administrativa (SUPER_ADMIN)
  // =====================================================

  /**
   * Lista todas las suscripciones con filtros opcionales (solo SUPER_ADMIN).
   *
   * @param status Estado a filtrar (opcional)
   * @param planCode Código de plan a filtrar (opcional)
   * @param pageable Configuración de paginación
   * @return Página de suscripciones
   */
  Page<SubscriptionListDto> listAllSubscriptions(String status, String planCode, Pageable pageable);

  /**
   * Extiende la vigencia de una suscripción (solo SUPER_ADMIN).
   *
   * @param idCompany ID de la compañía
   * @param request Datos de extensión
   * @return Resultado de la operación
   */
  ExtendSubscriptionResponseDto extendSubscription(
      Long idCompany, ExtendSubscriptionRequestDto request);

  /**
   * Cancela una suscripción (solo SUPER_ADMIN).
   *
   * @param idCompany ID de la compañía
   * @param request Datos de cancelación
   * @return Resultado de la operación
   */
  CancelSubscriptionResponseDto cancelSubscription(
      Long idCompany, CancelSubscriptionRequestDto request);

  /**
   * Obtiene la lista de empresas sin suscripción activa (solo SUPER_ADMIN).
   *
   * @return Lista de empresas disponibles para asignar suscripción
   */
  List<CompanyWithoutSubscriptionDto> getCompaniesWithoutSubscription();

  /**
   * Asigna una nueva suscripción a una empresa (solo SUPER_ADMIN).
   *
   * @param request Datos de la suscripción a asignar
   */
  void assignSubscription(AssignSubscriptionRequestDto request);

  /**
   * Obtiene la lista de planes de suscripción disponibles.
   *
   * @param includeInactive Incluir planes inactivos
   * @return Lista de planes disponibles
   */
  List<SubscriptionPlanDto> getAvailablePlans(boolean includeInactive);

  /**
   * Procesa cancelaciones programadas de suscripciones que ya vencieron. Este método es llamado por
   * la tarea programada para cancelar automáticamente las suscripciones que fueron marcadas para
   * cancelación al vencer.
   *
   * @return Número de suscripciones canceladas
   */
  int processScheduledSubscriptionCancellations();
}
