package com.wallparisoft.ebillbilling.service.impl;

import com.wallparisoft.ebillbilling.dto.subscription.AssignSubscriptionRequestDto;
import com.wallparisoft.ebillbilling.dto.subscription.CanEmitRequestDto;
import com.wallparisoft.ebillbilling.dto.subscription.CanEmitResponseDto;
import com.wallparisoft.ebillbilling.dto.subscription.CancelSubscriptionRequestDto;
import com.wallparisoft.ebillbilling.dto.subscription.CancelSubscriptionResponseDto;
import com.wallparisoft.ebillbilling.dto.subscription.CompanyWithoutSubscriptionDto;
import com.wallparisoft.ebillbilling.dto.subscription.ExtendSubscriptionRequestDto;
import com.wallparisoft.ebillbilling.dto.subscription.ExtendSubscriptionResponseDto;
import com.wallparisoft.ebillbilling.dto.subscription.MonthlyUsageDto;
import com.wallparisoft.ebillbilling.dto.subscription.SubscriptionListDto;
import com.wallparisoft.ebillbilling.dto.subscription.SubscriptionPlanDto;
import com.wallparisoft.ebillbilling.dto.subscription.SubscriptionResponseDto;
import com.wallparisoft.ebillbilling.dto.subscription.UpgradeRequestDto;
import com.wallparisoft.ebillbilling.dto.subscription.UpgradeResponseDto;
import com.wallparisoft.ebillbilling.dto.subscription.UsageResponseDto;
import com.wallparisoft.ebillbilling.dto.subscription.YearlyUsageDto;
import com.wallparisoft.ebillbilling.entity.CompanySubscription;
import com.wallparisoft.ebillbilling.entity.SubscriptionPlan;
import com.wallparisoft.ebillbilling.entity.SubscriptionUsageHistory;
import com.wallparisoft.ebillbilling.enums.DocumentType;
import com.wallparisoft.ebillbilling.exception.BadRequestException;
import com.wallparisoft.ebillbilling.exception.ConflictException;
import com.wallparisoft.ebillbilling.exception.ForbiddenException;
import com.wallparisoft.ebillbilling.exception.NotFoundException;
import com.wallparisoft.ebillbilling.repository.ICompanySubscriptionRepo;
import com.wallparisoft.ebillbilling.repository.ISubscriptionPlanRepo;
import com.wallparisoft.ebillbilling.repository.ISubscriptionUsageHistoryRepo;
import com.wallparisoft.ebillbilling.service.CustomerClient;
import com.wallparisoft.ebillbilling.service.ISubscriptionService;
import feign.FeignException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionServiceImpl implements ISubscriptionService {

  private final ICompanySubscriptionRepo companySubscriptionRepo;
  private final ISubscriptionPlanRepo subscriptionPlanRepo;
  private final ISubscriptionUsageHistoryRepo usageHistoryRepo;
  private final CustomerClient customerClient; // Feign Client para ebill-customer

  private static final DateTimeFormatter MONTH_YEAR_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM");

  @Override
  public SubscriptionResponseDto getCurrentSubscription(Long idCompany) {
    log.info("📋 Obteniendo suscripción actual para company: {}", idCompany);

    CompanySubscription subscription =
        companySubscriptionRepo
            .findActiveByIdCompany(idCompany)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "No se encontró suscripción activa para la compañía"));

    return mapToResponseDto(subscription);
  }

  @Override
  public UsageResponseDto getUsage(Long idCompany) {
    log.info("📊 Obteniendo uso para company: {}", idCompany);

    CompanySubscription subscription =
        companySubscriptionRepo
            .findActiveByIdCompany(idCompany)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "No se encontró suscripción activa para la compañía"));

    String currentMonth = LocalDateTime.now().format(MONTH_YEAR_FORMATTER);

    // Uso del mes actual
    SubscriptionUsageHistory currentMonthUsage =
        usageHistoryRepo
            .findByIdCompanyAndMonthYear(idCompany, currentMonth)
            .orElse(
                SubscriptionUsageHistory.builder()
                    .idCompany(idCompany)
                    .monthYear(currentMonth)
                    .invoicesCount(0)
                    .creditNotesCount(0)
                    .debitNotesCount(0)
                    .retentionsCount(0)
                    .guidesCount(0)
                    .totalDocuments(0)
                    .build());

    MonthlyUsageDto currentMonthDto =
        MonthlyUsageDto.builder()
            .month(currentMonthUsage.getMonthYear())
            .invoices(currentMonthUsage.getInvoicesCount())
            .creditNotes(currentMonthUsage.getCreditNotesCount())
            .debitNotes(currentMonthUsage.getDebitNotesCount())
            .retentions(currentMonthUsage.getRetentionsCount())
            .guides(currentMonthUsage.getGuidesCount())
            .total(currentMonthUsage.getTotalDocuments())
            .build();

    // Uso anual
    Integer yearlyTotal = subscription.getCurrentYearUsage();
    Integer yearlyLimit = subscription.getPlan().getDocumentLimitYearly();
    BigDecimal percentUsed = calculatePercentage(yearlyTotal, yearlyLimit);
    Integer remaining = Math.max(0, yearlyLimit - yearlyTotal);

    YearlyUsageDto yearlyDto =
        YearlyUsageDto.builder()
            .year(LocalDateTime.now().getYear())
            .total(yearlyTotal)
            .limit(yearlyLimit)
            .percentUsed(percentUsed)
            .remaining(remaining)
            .build();

    // Últimos 12 meses
    List<SubscriptionUsageHistory> recentHistory =
        usageHistoryRepo.findRecentByIdCompany(idCompany, 12);

    List<MonthlyUsageDto> lastMonths =
        recentHistory.stream()
            .map(
                history ->
                    MonthlyUsageDto.builder()
                        .month(history.getMonthYear())
                        .invoices(history.getInvoicesCount())
                        .creditNotes(history.getCreditNotesCount())
                        .debitNotes(history.getDebitNotesCount())
                        .retentions(history.getRetentionsCount())
                        .guides(history.getGuidesCount())
                        .total(history.getTotalDocuments())
                        .build())
            .collect(Collectors.toList());

    return UsageResponseDto.builder()
        .currentMonth(currentMonthDto)
        .currentYear(yearlyDto)
        .lastMonths(lastMonths)
        .build();
  }

  @Override
  public CanEmitResponseDto canEmitDocument(Long idCompany, CanEmitRequestDto request) {
    log.info("🔍 Verificando si company {} puede emitir {}", idCompany, request.getDocumentType());

    // Verificar si es super admin
    if (isSuperAdmin()) {
      return CanEmitResponseDto.builder()
          .allowed(true)
          .message("Super administrador: sin restricciones")
          .reason("ALLOWED")
          .build();
    }

    CompanySubscription subscription =
        companySubscriptionRepo
            .findActiveByIdCompany(idCompany)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "No se encontró suscripción activa para la compañía"));

    SubscriptionPlan plan = subscription.getPlan();

    // Verificar si el documento está disponible en el plan
    if (!isFeatureAvailable(plan, request.getDocumentType())) {
      return CanEmitResponseDto.builder()
          .allowed(false)
          .currentUsage(subscription.getCurrentYearUsage())
          .limit(plan.getDocumentLimitYearly())
          .remaining(0)
          .message(
              "El tipo de documento "
                  + request.getDocumentType()
                  + " no está disponible en su plan actual")
          .reason("FEATURE_NOT_AVAILABLE")
          .build();
    }

    // Verificar si está expirada
    if (subscription.getExpiresAt() != null
        && subscription.getExpiresAt().isBefore(LocalDateTime.now())) {
      return CanEmitResponseDto.builder()
          .allowed(false)
          .currentUsage(subscription.getCurrentYearUsage())
          .limit(plan.getDocumentLimitYearly())
          .remaining(0)
          .message("La suscripción ha expirado")
          .reason("EXPIRED")
          .build();
    }

    // Verificar límite de documentos
    Integer currentUsage = subscription.getCurrentYearUsage();
    Integer limit = plan.getDocumentLimitYearly();
    Integer remaining = limit - currentUsage;

    if (currentUsage >= limit) {
      return CanEmitResponseDto.builder()
          .allowed(false)
          .currentUsage(currentUsage)
          .limit(limit)
          .remaining(0)
          .message("Ha alcanzado el límite de documentos de su plan")
          .reason("LIMIT_REACHED")
          .build();
    }

    return CanEmitResponseDto.builder()
        .allowed(true)
        .currentUsage(currentUsage)
        .limit(limit)
        .remaining(remaining)
        .message("Puede emitir el documento")
        .reason("ALLOWED")
        .build();
  }

  @Override
  @Transactional
  public UpgradeResponseDto upgradePlan(Long idCompany, UpgradeRequestDto request) {
    log.info("⬆️ Actualizando plan de company {} a {}", idCompany, request.getNewPlanCode());

    // Verificar permisos (solo SUPER_ADMIN en este servicio)
    if (!isSuperAdmin()) {
      log.error("❌ Acceso denegado: Se requiere rol SUPER_ADMIN para actualizar plan");
      return UpgradeResponseDto.builder()
          .success(false)
          .message("No tiene permisos para actualizar el plan")
          .build();
    }

    // Buscar cualquier suscripción (activa o cancelada) para permitir reactivación
    CompanySubscription currentSubscription =
        companySubscriptionRepo
            .findLastByIdCompany(idCompany)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "No se encontró ninguna suscripción para la compañía"));

    // Si la suscripción está cancelada o expirada, reactivarla automáticamente
    if (!"ACTIVE".equals(currentSubscription.getSubscriptionStatus())) {
      log.info(
          "🔄 Reactivando suscripción {} para company {} - Status anterior: {}",
          currentSubscription.getIdSubscription(),
          idCompany,
          currentSubscription.getSubscriptionStatus());
      currentSubscription.setSubscriptionStatus("ACTIVE");
      currentSubscription.setCancelledAt(null);
      currentSubscription.setCancelReason(null);
    }

    // Obtener nuevo plan
    SubscriptionPlan newPlan =
        subscriptionPlanRepo
            .findByCodeAndIsActiveTrue(request.getNewPlanCode())
            .orElseThrow(() -> new IllegalArgumentException("Plan no encontrado o inactivo"));

    // Actualizar suscripción
    currentSubscription.setIdPlan(newPlan.getIdPlan());
    currentSubscription.setExpiresAt(LocalDateTime.now().plusYears(1));
    currentSubscription.setLastPaymentDate(request.getPaymentDate());
    currentSubscription.setLastPaymentAmount(request.getPaymentAmount());
    currentSubscription.setPaymentReference(request.getPaymentReference());
    currentSubscription.setPaymentMethod("MANUAL");
    currentSubscription.setNotes(request.getNotes());
    currentSubscription.setUpdateDate(LocalDateTime.now());
    currentSubscription.setStatus(true); // Asegurar que el status de BaseEntity esté activo

    CompanySubscription updatedSubscription = companySubscriptionRepo.save(currentSubscription);

    log.info("✅ Plan actualizado exitosamente para company {}", idCompany);

    return UpgradeResponseDto.builder()
        .success(true)
        .message("Plan actualizado exitosamente")
        .subscription(mapToResponseDto(updatedSubscription))
        .build();
  }

  @Override
  public void validateCanEmit(Long idCompany, String documentType) {
    CanEmitRequestDto request = new CanEmitRequestDto();
    request.setDocumentType(documentType);

    CanEmitResponseDto response = canEmitDocument(idCompany, request);

    if (!response.getAllowed()) {
      log.error("❌ No se puede emitir documento: {}", response.getMessage());
      throw new IllegalStateException(response.getMessage());
    }
  }

  @Override
  public void validateUserLimit(Long idCompany, int currentUserCount) {
    if (isSuperAdmin()) {
      return;
    }

    CompanySubscription subscription =
        companySubscriptionRepo
            .findActiveByIdCompany(idCompany)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "No se encontró suscripción activa para la compañía"));

    Integer userLimit = subscription.getPlan().getUserLimit();

    if (currentUserCount >= userLimit) {
      throw new IllegalStateException(
          "Ha alcanzado el límite de usuarios de su plan ("
              + userLimit
              + "). Actualice su plan para agregar más usuarios.");
    }
  }

  @Override
  public void validateEstablishmentLimit(Long idCompany, int currentEstablishmentCount) {
    if (isSuperAdmin()) {
      return;
    }

    CompanySubscription subscription =
        companySubscriptionRepo
            .findActiveByIdCompany(idCompany)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "No se encontró suscripción activa para la compañía"));

    Integer establishmentLimit = subscription.getPlan().getEstablishmentLimit();

    if (currentEstablishmentCount >= establishmentLimit) {
      throw new IllegalStateException(
          "Ha alcanzado el límite de establecimientos de su plan ("
              + establishmentLimit
              + "). Actualice su plan para agregar más establecimientos.");
    }
  }

  @Override
  public void validateIssuancePointLimit(Long idCompany, int currentIssuancePointCount) {
    if (isSuperAdmin()) {
      return;
    }

    CompanySubscription subscription =
        companySubscriptionRepo
            .findActiveByIdCompany(idCompany)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "No se encontró suscripción activa para la compañía"));

    Integer issuancePointLimit = subscription.getPlan().getIssuancePointLimit();

    if (currentIssuancePointCount >= issuancePointLimit) {
      throw new IllegalStateException(
          "Ha alcanzado el límite de puntos de emisión de su plan ("
              + issuancePointLimit
              + "). Actualice su plan para agregar más puntos de emisión.");
    }
  }

  // =====================================================
  // MÉTODOS PRIVADOS
  // =====================================================

  private SubscriptionResponseDto mapToResponseDto(CompanySubscription subscription) {
    SubscriptionPlan plan = subscription.getPlan();

    SubscriptionPlanDto planDto =
        SubscriptionPlanDto.builder()
            .idPlan(plan.getIdPlan())
            .code(plan.getCode())
            .name(plan.getName())
            .description(plan.getDescription())
            .priceAnnual(plan.getPriceAnnual())
            .priceMonthly(plan.getPriceMonthly())
            .documentLimitYearly(plan.getDocumentLimitYearly())
            .userLimit(plan.getUserLimit())
            .establishmentLimit(plan.getEstablishmentLimit())
            .issuancePointLimit(plan.getIssuancePointLimit())
            .hasApiAccess(plan.getHasApiAccess())
            .hasMultiUser(plan.getHasMultiUser())
            .hasAdvancedReports(plan.getHasAdvancedReports())
            .hasElectronicInvoice(plan.getHasElectronicInvoice())
            .hasCreditNote(plan.getHasCreditNote())
            .hasDebitNote(plan.getHasDebitNote())
            .hasRetention(plan.getHasRetention())
            .hasGuide(plan.getHasGuide())
            .hasPrioritySupport(plan.getHasPrioritySupport())
            .hasCustomBranding(plan.getHasCustomBranding())
            .hasAutomatedBackups(plan.getHasAutomatedBackups())
            .isActive(plan.getIsActive())
            .displayOrder(plan.getDisplayOrder())
            .build();

    Integer currentUsage = subscription.getCurrentYearUsage();
    Integer limit = plan.getDocumentLimitYearly();
    BigDecimal percentUsed = calculatePercentage(currentUsage, limit);
    Integer remaining = Math.max(0, limit - currentUsage);
    Boolean isExpired =
        subscription.getExpiresAt() != null
            && subscription.getExpiresAt().isBefore(LocalDateTime.now());
    Boolean isLimitReached = currentUsage >= limit;

    return SubscriptionResponseDto.builder()
        .idSubscription(subscription.getIdSubscription())
        .idCompany(subscription.getIdCompany())
        .plan(planDto)
        .subscriptionStatus(subscription.getSubscriptionStatus())
        .startDate(subscription.getStartDate())
        .expiresAt(subscription.getExpiresAt())
        .currentYearUsage(currentUsage)
        .lastResetDate(subscription.getLastResetDate())
        .percentUsed(percentUsed)
        .documentsRemaining(remaining)
        .isExpired(isExpired)
        .isLimitReached(isLimitReached)
        .paymentMethod(subscription.getPaymentMethod())
        .lastPaymentDate(subscription.getLastPaymentDate())
        .lastPaymentAmount(subscription.getLastPaymentAmount())
        .paymentReference(subscription.getPaymentReference())
        .notes(subscription.getNotes())
        .build();
  }

  private BigDecimal calculatePercentage(Integer used, Integer limit) {
    if (limit == null || limit == 0) {
      return BigDecimal.ZERO;
    }
    return BigDecimal.valueOf(used)
        .multiply(BigDecimal.valueOf(100))
        .divide(BigDecimal.valueOf(limit), 2, RoundingMode.HALF_UP);
  }

  private boolean isFeatureAvailable(SubscriptionPlan plan, String documentType) {
    return DocumentType.from(documentType).map(dt -> dt.isAvailableIn(plan)).orElse(false);
  }

  /**
   * Indica si el usuario tiene rol SUPER_ADMIN (global).
   *
   * <p><b>Modelo de roles:</b> El usuario puede tener una lista de roles por empresa (admin,
   * member, etc.). El rol <b>super_admin</b> es especial: es <b>global</b> y aplica a todas las
   * empresas sin validar por empresa. Si tiene super_admin, se omiten validaciones por idCompany.
   * Los demás roles (admin, member, etc.) son por empresa: cuando no hay super_admin, debe
   * validarse el rol del usuario para la empresa concreta (idCompany).
   */
  private boolean isSuperAdmin() {
    try {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication == null) {
        log.warn("⚠️ Authentication es null en isSuperAdmin()");
        return false;
      }
      if (authentication.getAuthorities() == null) {
        log.warn(
            "⚠️ Authorities es null en isSuperAdmin() - Principal: {}",
            authentication.getPrincipal());
        return false;
      }

      // Log para diagnóstico cuando se valida permiso (cancelar/upgrade/etc.)
      log.info(
          "🔍 [isSuperAdmin] Principal: {}, Authorities: {}",
          authentication.getPrincipal(),
          authentication.getAuthorities());

      // Si en la lista de roles (p. ej. ROLE_[super_admin, admin]) aparece super_admin → global
      boolean isSuperAdmin =
          authentication.getAuthorities().stream()
              .anyMatch(
                  auth -> {
                    String authority = auth.getAuthority();
                    if (authority == null) return false;
                    return "ROLE_SUPER_ADMIN".equals(authority)
                        || "SUPER_ADMIN".equals(authority)
                        || "ROLE_[super_admin]".equals(authority)
                        || "super_admin".equalsIgnoreCase(authority)
                        || "ROLE_super_admin".equalsIgnoreCase(authority)
                        || authority.contains("super_admin");
                  });

      if (!isSuperAdmin) {
        log.warn(
            "⚠️ [isSuperAdmin] Usuario NO es SUPER_ADMIN - Authorities: {}",
            authentication.getAuthorities());
      }

      return isSuperAdmin;
    } catch (Exception e) {
      log.error(
          "❌ Error al obtener información de autenticación en isSuperAdmin(): {}",
          e.getMessage(),
          e);
      return false;
    }
  }

  private boolean isAdminOrSuperAdmin() {
    try {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication == null) {
        log.warn("⚠️ Authentication es null en isAdminOrSuperAdmin()");
        return false;
      }
      if (authentication.getAuthorities() == null) {
        log.warn(
            "⚠️ Authorities es null en isAdminOrSuperAdmin() - Principal: {}",
            authentication.getPrincipal());
        return false;
      }

      // Log para debugging
      log.debug(
          "🔍 Verificando permisos - Principal: {}, Authorities: {}",
          authentication.getPrincipal(),
          authentication.getAuthorities());

      // super_admin = global; admin/member = por empresa (validar por idCompany cuando aplique)
      boolean hasPermission =
          authentication.getAuthorities().stream()
              .anyMatch(
                  auth -> {
                    String authority = auth.getAuthority();
                    if (authority == null) return false;
                    return "ROLE_SUPER_ADMIN".equals(authority)
                        || "SUPER_ADMIN".equals(authority)
                        || "ROLE_[super_admin]".equals(authority)
                        || "super_admin".equalsIgnoreCase(authority)
                        || "ROLE_super_admin".equalsIgnoreCase(authority)
                        || authority.contains("super_admin")
                        || "ROLE_ADMIN".equals(authority)
                        || "ADMIN".equals(authority)
                        || "ROLE_[admin]".equals(authority)
                        || "admin".equalsIgnoreCase(authority)
                        || "ROLE_admin".equalsIgnoreCase(authority)
                        || (authority.contains("[") && authority.contains("admin"));
                  });

      if (!hasPermission) {
        log.warn(
            "⚠️ Usuario sin permisos ADMIN/SUPER_ADMIN - Authorities: {}",
            authentication.getAuthorities());
      }

      return hasPermission;
    } catch (Exception e) {
      log.error("❌ Error al obtener información de autenticación: {}", e.getMessage(), e);
      return false;
    }
  }

  // =====================================================
  // SUBSCRIPTION_MANAGEMENT - Métodos administrativos (SUPER_ADMIN)
  // =====================================================

  @Override
  public Page<SubscriptionListDto> listAllSubscriptions(
      String status, String planCode, Pageable pageable) {
    log.info(
        "📋 Listando suscripciones (filtros: status={}, planCode={})",
        status != null ? status : "todos",
        planCode != null ? planCode : "todos");

    // Obtener ID del plan si se especificó un código
    Long idPlan = null;
    if (planCode != null && !planCode.isBlank()) {
      idPlan =
          subscriptionPlanRepo
              .findByCodeAndIsActiveTrue(planCode)
              .map(SubscriptionPlan::getIdPlan)
              .orElse(null);
    }

    Page<CompanySubscription> subscriptions =
        companySubscriptionRepo.findAllWithFilters(status, idPlan, pageable);

    return subscriptions.map(this::mapToListDto);
  }

  @Override
  @Transactional
  public ExtendSubscriptionResponseDto extendSubscription(
      Long idCompany, ExtendSubscriptionRequestDto request) {
    log.info(
        "⬆️ Extendiendo suscripción de company {} por {} meses",
        idCompany,
        request.getExtensionMonths());

    // Verificar permisos
    if (!isSuperAdmin()) {
      return ExtendSubscriptionResponseDto.builder()
          .success(false)
          .message("No tiene permisos para extender suscripciones")
          .build();
    }

    // Obtener suscripción activa
    CompanySubscription subscription =
        companySubscriptionRepo
            .findActiveByIdCompany(idCompany)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "No se encontró suscripción activa para la compañía"));

    // Validar que la suscripción puede ser extendida
    if (!"ACTIVE".equals(subscription.getSubscriptionStatus())
        && !"EXPIRED".equals(subscription.getSubscriptionStatus())) {
      return ExtendSubscriptionResponseDto.builder()
          .success(false)
          .message("Solo se pueden extender suscripciones ACTIVE o EXPIRED")
          .build();
    }

    // Guardar fecha anterior
    LocalDateTime previousExpiryDate = subscription.getExpiresAt();

    // Calcular nueva fecha de expiración
    // Siempre calcular desde la fecha de inicio original para mantener el mismo día del mes
    // Esto evita propagar errores de fechas de expiración previas que pudieron ser calculadas
    // incorrectamente
    LocalDateTime startDate = subscription.getStartDate();
    LocalDateTime newExpiryDate;

    if (startDate != null) {
      // Calcular meses totales desde el inicio hasta la fecha de expiración previa (si existe)
      // y luego agregar los meses de extensión
      long totalMonths;
      if (previousExpiryDate != null) {
        // Calcular meses desde inicio hasta expiración previa + meses de extensión
        // Si la fecha previa tiene el error del minusDays(1), compensamos sumando 1 día
        // para calcular correctamente los meses desde el mismo día del mes
        LocalDateTime correctedExpiryDate = previousExpiryDate.plusDays(1);
        long monthsUntilPreviousExpiry =
            java.time.temporal.ChronoUnit.MONTHS.between(
                startDate.toLocalDate(), correctedExpiryDate.toLocalDate());
        totalMonths = monthsUntilPreviousExpiry + request.getExtensionMonths();
      } else {
        // Si no hay fecha de expiración previa, solo usar los meses de extensión
        totalMonths = request.getExtensionMonths();
      }

      // Calcular nueva fecha desde el inicio (mantiene el mismo día del mes)
      // Ejemplo: Si inicio el 23 de diciembre y tengo 3 meses totales → expira el 23 de marzo
      newExpiryDate = startDate.plusMonths(totalMonths);
    } else {
      // Fallback: si no hay startDate (no debería pasar), usar fecha de expiración previa o fecha
      // actual
      if (previousExpiryDate != null) {
        // Compensar el posible error del minusDays(1) sumando 1 día antes de extender
        // Luego extender los meses desde la fecha corregida
        newExpiryDate = previousExpiryDate.plusDays(1).plusMonths(request.getExtensionMonths());
      } else {
        newExpiryDate = LocalDateTime.now().plusMonths(request.getExtensionMonths());
      }
    }

    // Actualizar suscripción
    subscription.setExpiresAt(newExpiryDate);
    subscription.setSubscriptionStatus("ACTIVE");
    subscription.setLastPaymentDate(request.getPaymentDate());
    subscription.setLastPaymentAmount(request.getPaymentAmount());
    subscription.setPaymentReference(request.getPaymentReference());
    if (request.getNotes() != null) {
      subscription.setNotes(request.getNotes());
    }
    subscription.setUpdateDate(LocalDateTime.now());

    CompanySubscription updated = companySubscriptionRepo.save(subscription);

    log.info("✅ Suscripción extendida exitosamente - Nueva fecha: {}", newExpiryDate);

    return ExtendSubscriptionResponseDto.builder()
        .success(true)
        .message("Suscripción extendida exitosamente")
        .previousExpiryDate(previousExpiryDate)
        .newExpiryDate(newExpiryDate)
        .extensionMonths(request.getExtensionMonths())
        .subscription(mapToResponseDto(updated))
        .build();
  }

  @Override
  @Transactional
  public CancelSubscriptionResponseDto cancelSubscription(
      Long idCompany, CancelSubscriptionRequestDto request) {
    log.info("❌ Cancelando suscripción de company {} - Razón: {}", idCompany, request.getReason());

    // Verificar permisos
    if (!isSuperAdmin()) {
      return CancelSubscriptionResponseDto.builder()
          .success(false)
          .message("No tiene permisos para cancelar suscripciones")
          .build();
    }

    // Obtener suscripción activa
    CompanySubscription subscription =
        companySubscriptionRepo
            .findActiveByIdCompany(idCompany)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "No se encontró suscripción activa para la compañía"));

    // Validar que la suscripción puede ser cancelada
    if (!"ACTIVE".equals(subscription.getSubscriptionStatus())) {
      return CancelSubscriptionResponseDto.builder()
          .success(false)
          .message("Solo se pueden cancelar suscripciones ACTIVE")
          .build();
    }

    // Cancelar inmediatamente o programar cancelación
    LocalDateTime cancelledAt = LocalDateTime.now();

    if (request.getCancelImmediately()) {
      subscription.setSubscriptionStatus("CANCELLED");
      subscription.setCancelledAt(cancelledAt);
      subscription.setCancelReason(request.getReason());
      if (request.getNotes() != null) {
        subscription.setNotes(subscription.getNotes() + "\nCancelación: " + request.getNotes());
      }
      subscription.setUpdateDate(LocalDateTime.now());
    } else {
      // Si no es inmediato, solo marcar para cancelación al vencer
      subscription.setCancelReason(request.getReason());
      if (request.getNotes() != null) {
        subscription.setNotes(
            subscription.getNotes() + "\nProgramada cancelación: " + request.getNotes());
      }
      subscription.setUpdateDate(LocalDateTime.now());
    }

    CompanySubscription updated = companySubscriptionRepo.save(subscription);

    log.info(
        "✅ Suscripción {} - Estado: {}",
        request.getCancelImmediately() ? "cancelada" : "programada para cancelación",
        updated.getSubscriptionStatus());

    return CancelSubscriptionResponseDto.builder()
        .success(true)
        .message(
            request.getCancelImmediately()
                ? "Suscripción cancelada exitosamente"
                : "Suscripción programada para cancelación al vencer")
        .cancelledAt(cancelledAt)
        .cancelReason(request.getReason())
        .subscription(
            CancelSubscriptionResponseDto.SubscriptionCancelledDto.builder()
                .idSubscription(updated.getIdSubscription())
                .idCompany(updated.getIdCompany())
                .subscriptionStatus(updated.getSubscriptionStatus())
                .cancelledAt(updated.getCancelledAt())
                .cancelReason(updated.getCancelReason())
                .build())
        .build();
  }

  // =====================================================
  // MÉTODOS PRIVADOS PARA MAPEO
  // =====================================================

  private SubscriptionListDto mapToListDto(CompanySubscription subscription) {
    SubscriptionPlan plan = subscription.getPlan();

    Integer currentUsage = subscription.getCurrentYearUsage();
    Integer limit = plan.getDocumentLimitYearly();
    BigDecimal percentUsed = calculatePercentage(currentUsage, limit);
    Integer remaining = Math.max(0, limit - currentUsage);
    Boolean isExpired =
        subscription.getExpiresAt() != null
            && subscription.getExpiresAt().isBefore(LocalDateTime.now());
    Boolean isLimitReached = currentUsage >= limit;

    return SubscriptionListDto.builder()
        .idSubscription(subscription.getIdSubscription())
        .idCompany(subscription.getIdCompany())
        .plan(
            SubscriptionListDto.PlanBasicDto.builder()
                .idPlan(plan.getIdPlan())
                .code(plan.getCode())
                .name(plan.getName())
                .documentLimitYearly(plan.getDocumentLimitYearly())
                .priceAnnual(plan.getPriceAnnual())
                .priceMonthly(plan.getPriceMonthly())
                .build())
        .subscriptionStatus(subscription.getSubscriptionStatus())
        .startDate(subscription.getStartDate())
        .expiresAt(subscription.getExpiresAt())
        .currentYearUsage(currentUsage)
        .percentUsed(percentUsed)
        .documentsRemaining(remaining)
        .isExpired(isExpired)
        .isLimitReached(isLimitReached)
        .cancelledAt(subscription.getCancelledAt())
        .cancelReason(subscription.getCancelReason())
        .build();
  }

  // =====================================================
  // Métodos para asignación de suscripciones (SUPER_ADMIN)
  // =====================================================

  @Override
  public List<CompanyWithoutSubscriptionDto> getCompaniesWithoutSubscription() {
    log.info("🔍 Obteniendo empresas sin suscripción activa");

    // Verificar permisos SUPER_ADMIN
    if (!isSuperAdmin()) {
      throw new IllegalStateException(
          "Solo los super administradores pueden consultar empresas sin suscripción");
    }

    try {
      // 1. Obtener IDs de empresas que SÍ tienen suscripción activa
      List<Long> companyIdsWithSubscription =
          companySubscriptionRepo.findCompanyIdsWithActiveSubscription();

      log.info(
          "🔍 Encontradas {} empresas CON suscripción activa (serán excluidas)",
          companyIdsWithSubscription.size());

      // 2. Llamar a ebill-customer para obtener empresas excluyendo las que tienen suscripción
      List<CompanyWithoutSubscriptionDto> companies =
          customerClient.getCompaniesWithoutIds(companyIdsWithSubscription);

      log.info("✅ Encontradas {} empresas sin suscripción activa", companies.size());

      return companies;
    } catch (Exception e) {
      log.error("❌ Error al obtener empresas sin suscripción: {}", e.getMessage(), e);
      throw new RuntimeException(
          "Error al comunicarse con el servicio de empresas: " + e.getMessage(), e);
    }
  }

  @Override
  @Transactional
  public void assignSubscription(AssignSubscriptionRequestDto request) {
    log.info(
        "📝 Asignando suscripción: empresa={}, plan={}, duración={} meses",
        request.getIdCompany(),
        request.getPlanCode(),
        request.getDurationMonths());

    // Verificar permisos SUPER_ADMIN
    if (!isSuperAdmin()) {
      throw new ForbiddenException(
          "No tienes permisos para realizar esta acción. Se requiere rol SUPER_ADMIN");
    }

    // 1. Validar que la empresa existe
    try {
      customerClient.getCompanyById(request.getIdCompany());
    } catch (FeignException.NotFound e) {
      throw new NotFoundException("Empresa con ID " + request.getIdCompany() + " no encontrada");
    } catch (FeignException e) {
      log.error("❌ Error al consultar empresa {}: {}", request.getIdCompany(), e.getMessage());
      throw new RuntimeException("Error al comunicarse con el servicio de empresas", e);
    }

    // 2. Validar que NO tenga suscripción activa
    boolean hasActiveSubscription =
        companySubscriptionRepo.existsActiveSubscriptionForCompany(request.getIdCompany());

    if (hasActiveSubscription) {
      // Obtener detalles de la suscripción existente para el error 409
      CompanySubscription existingSubscription =
          companySubscriptionRepo.findActiveByIdCompany(request.getIdCompany()).orElse(null);

      Long existingSubscriptionId =
          existingSubscription != null ? existingSubscription.getIdSubscription() : null;
      LocalDateTime expiresAt =
          existingSubscription != null ? existingSubscription.getExpiresAt() : null;

      throw new ConflictException(
          "La empresa ya tiene una suscripción activa",
          request.getIdCompany(),
          existingSubscriptionId,
          expiresAt);
    }

    // 3. Buscar el plan
    SubscriptionPlan plan =
        subscriptionPlanRepo
            .findByCode(request.getPlanCode())
            .orElseThrow(
                () -> new NotFoundException("Plan '" + request.getPlanCode() + "' no existe"));

    // 4. Validar que la fecha de inicio no sea pasada (ya validado por @FutureOrPresent, pero
    // agregamos validación adicional)
    if (request.getStartDate().isBefore(java.time.LocalDate.now())) {
      throw new BadRequestException("La fecha de inicio no puede ser anterior a hoy");
    }

    // 5. Validar duración (ya validado por @Min/@Max, pero agregamos validación adicional)
    if (request.getDurationMonths() < 1 || request.getDurationMonths() > 60) {
      throw new BadRequestException("La duración debe estar entre 1 y 60 meses");
    }

    // 6. Calcular fecha de expiración (mismo día del mes después de la duración)
    // Ejemplo: Si inicia el 15 de enero con 1 mes de duración, expira el 15 de febrero
    LocalDateTime expiresAt =
        request.getStartDate().atStartOfDay().plusMonths(request.getDurationMonths());

    // 7. Crear la suscripción
    CompanySubscription subscription = new CompanySubscription();
    subscription.setIdCompany(request.getIdCompany());
    subscription.setIdPlan(plan.getIdPlan());
    subscription.setSubscriptionStatus("ACTIVE");
    subscription.setStartDate(request.getStartDate().atStartOfDay());
    subscription.setExpiresAt(expiresAt);
    subscription.setCurrentYearUsage(0);
    subscription.setLastResetDate(request.getStartDate().atStartOfDay()); // Fecha de inicio como
    // fecha de reset
    subscription.setStatus(true); // Campo heredado de BaseEntity (soft delete flag)
    subscription.setCreationDate(LocalDateTime.now());
    subscription.setUpdateDate(LocalDateTime.now());

    companySubscriptionRepo.save(subscription);

    log.info(
        "✅ Suscripción asignada exitosamente: empresa={}, plan={}, expira={}",
        request.getIdCompany(),
        request.getPlanCode(),
        expiresAt);
  }

  @Override
  public List<SubscriptionPlanDto> getAvailablePlans(boolean includeInactive) {
    log.info("🔍 Obteniendo planes disponibles (includeInactive={})", includeInactive);

    List<SubscriptionPlan> plans =
        includeInactive
            ? subscriptionPlanRepo.findAll()
            : subscriptionPlanRepo.findAll().stream()
                .filter(p -> p.getIsActive() != null && p.getIsActive())
                .toList();

    // Ordenar por displayOrder
    plans =
        plans.stream()
            .sorted(
                (p1, p2) ->
                    Integer.compare(
                        p1.getDisplayOrder() != null ? p1.getDisplayOrder() : Integer.MAX_VALUE,
                        p2.getDisplayOrder() != null ? p2.getDisplayOrder() : Integer.MAX_VALUE))
            .toList();

    List<SubscriptionPlanDto> planDtos =
        plans.stream()
            .map(
                plan -> {
                  // Construir lista de features dinámicamente
                  List<String> features = new java.util.ArrayList<>();

                  // Límite de documentos
                  if (plan.getDocumentLimitYearly() == -1) {
                    features.add("Documentos ilimitados");
                  } else {
                    features.add("Hasta " + plan.getDocumentLimitYearly() + " documentos/año");
                  }

                  // Tipos de documentos
                  if (plan.getHasElectronicInvoice()) features.add("Facturación electrónica");
                  if (plan.getHasCreditNote()) features.add("Notas de crédito");
                  if (plan.getHasDebitNote()) features.add("Notas de débito");
                  if (plan.getHasRetention()) features.add("Retenciones");
                  if (plan.getHasGuide()) features.add("Guías de remisión");

                  // Features adicionales
                  if (plan.getHasMultiUser()) features.add("Múltiples usuarios");
                  if (plan.getHasAdvancedReports()) features.add("Reportes avanzados");
                  if (plan.getHasApiAccess()) features.add("Acceso API REST");
                  if (plan.getHasPrioritySupport()) features.add("Soporte prioritario");
                  if (plan.getHasCustomBranding()) features.add("Personalización de marca");
                  if (plan.getHasAutomatedBackups()) features.add("Respaldos automáticos");

                  return SubscriptionPlanDto.builder()
                      .idPlan(plan.getIdPlan())
                      .code(plan.getCode())
                      .name(plan.getName())
                      .description(plan.getDescription())
                      .priceAnnual(plan.getPriceAnnual())
                      .priceMonthly(plan.getPriceMonthly())
                      .documentLimitYearly(plan.getDocumentLimitYearly())
                      .userLimit(plan.getUserLimit())
                      .establishmentLimit(plan.getEstablishmentLimit())
                      .issuancePointLimit(plan.getIssuancePointLimit())
                      .hasApiAccess(plan.getHasApiAccess())
                      .hasMultiUser(plan.getHasMultiUser())
                      .hasAdvancedReports(plan.getHasAdvancedReports())
                      .hasElectronicInvoice(plan.getHasElectronicInvoice())
                      .hasCreditNote(plan.getHasCreditNote())
                      .hasDebitNote(plan.getHasDebitNote())
                      .hasRetention(plan.getHasRetention())
                      .hasGuide(plan.getHasGuide())
                      .hasPrioritySupport(plan.getHasPrioritySupport())
                      .hasCustomBranding(plan.getHasCustomBranding())
                      .hasAutomatedBackups(plan.getHasAutomatedBackups())
                      .isActive(plan.getIsActive())
                      .displayOrder(plan.getDisplayOrder())
                      .features(features)
                      .build();
                })
            .toList();

    log.info("✅ Retornando {} planes", planDtos.size());

    return planDtos;
  }

  // =====================================================
  // TAREA PROGRAMADA: Procesar cancelaciones programadas
  // =====================================================

  @Override
  @Transactional
  public int processScheduledSubscriptionCancellations() {
    log.info("🔄 Iniciando procesamiento de cancelaciones programadas de suscripciones");

    // Buscar suscripciones programadas para cancelar que ya vencieron
    List<CompanySubscription> scheduledCancellations =
        companySubscriptionRepo.findScheduledCancellationsToProcess();

    if (scheduledCancellations.isEmpty()) {
      log.info("✅ No hay suscripciones programadas para cancelar");
      return 0;
    }

    log.info(
        "📋 Encontradas {} suscripción(es) programada(s) para cancelar",
        scheduledCancellations.size());

    LocalDateTime now = LocalDateTime.now();
    int cancelledCount = 0;

    for (CompanySubscription subscription : scheduledCancellations) {
      try {
        log.info(
            "❌ Cancelando suscripción {} de company {} - Razón: {}",
            subscription.getIdSubscription(),
            subscription.getIdCompany(),
            subscription.getCancelReason());

        // Cambiar estado a CANCELLED
        subscription.setSubscriptionStatus("CANCELLED");
        subscription.setCancelledAt(now);
        subscription.setUpdateDate(now);

        // Actualizar notas si es necesario
        String notes = subscription.getNotes();
        if (notes == null || notes.isEmpty()) {
          notes = "";
        }
        subscription.setNotes(
            notes
                + "\nCancelación automática procesada: "
                + now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        companySubscriptionRepo.save(subscription);
        cancelledCount++;

        log.info("✅ Suscripción {} cancelada exitosamente", subscription.getIdSubscription());
      } catch (Exception e) {
        log.error(
            "❌ Error al cancelar suscripción {}: {}",
            subscription.getIdSubscription(),
            e.getMessage(),
            e);
        // Continuar con las siguientes aunque una falle
      }
    }

    log.info(
        "✅ Procesamiento completado: {} de {} suscripción(es) cancelada(s) exitosamente",
        cancelledCount,
        scheduledCancellations.size());

    return cancelledCount;
  }
}
