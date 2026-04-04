package com.wallparisoft.ebillbilling.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallparisoft.ebillbilling.dto.subscription.AssignSubscriptionRequestDto;
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
import com.wallparisoft.ebillbilling.service.ISubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador para gestión administrativa de suscripciones. Solo accesible para SUPER_ADMIN.
 * Feature: SUBSCRIPTION_MANAGEMENT
 */
@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
@Slf4j
@Tag(
    name = "Subscription Management",
    description = "Gestión administrativa de suscripciones (Solo SUPER_ADMIN)")
@SecurityRequirement(name = "bearerAuth")
public class SubscriptionManagementController {

  private final ISubscriptionService subscriptionService;

  /**
   * Lista todas las suscripciones del sistema con filtros opcionales.
   *
   * <p>GET /api/subscriptions
   */
  @GetMapping
  @Operation(
      summary = "Listar todas las suscripciones",
      description =
          "Lista todas las suscripciones del sistema con filtros opcionales (Solo SUPER_ADMIN)")
  public ResponseEntity<Page<SubscriptionListDto>> listAll(
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String planCode,
      @PageableDefault(size = 20) Pageable pageable) {

    log.info(
        "🔍 SUPER_ADMIN listado de suscripciones (filtros: status={}, planCode={})",
        status != null ? status : "todos",
        planCode != null ? planCode : "todos");

    Page<SubscriptionListDto> subscriptions =
        subscriptionService.listAllSubscriptions(status, planCode, pageable);

    return ResponseEntity.ok(subscriptions);
  }

  /**
   * Obtiene la suscripción de una empresa específica.
   *
   * <p>GET /api/subscriptions/company/{idCompany}
   */
  @GetMapping("/company/{idCompany}")
  @Operation(
      summary = "Ver suscripción de una empresa",
      description =
          "Obtiene los detalles completos de la suscripción de una empresa (Solo SUPER_ADMIN)")
  public ResponseEntity<SubscriptionResponseDto> getCompanySubscription(
      @PathVariable Long idCompany) {

    log.info("🔍 SUPER_ADMIN consultando suscripción de company {}", idCompany);

    SubscriptionResponseDto subscription = subscriptionService.getCurrentSubscription(idCompany);

    return ResponseEntity.ok(subscription);
  }

  /**
   * Cambia el plan de una empresa.
   *
   * <p>POST /api/subscriptions/company/{idCompany}/upgrade
   */
  @PostMapping("/company/{idCompany}/upgrade")
  @Operation(
      summary = "Cambiar plan de suscripción",
      description = "Actualiza el plan de suscripción de una empresa (Solo SUPER_ADMIN)")
  public ResponseEntity<UpgradeResponseDto> upgradePlan(
      @PathVariable Long idCompany,
      @Valid @RequestBody UpgradeRequestDto request,
      HttpServletRequest httpRequest) {

    // Decodificar y mostrar información del token JWT
    decodeAndLogJwtToken(httpRequest);

    log.info(
        "⬆️ SUPER_ADMIN actualizando plan de company {} a {} - paymentDate: {}",
        idCompany,
        request.getNewPlanCode(),
        request.getPaymentDate());

    UpgradeResponseDto response = subscriptionService.upgradePlan(idCompany, request);

    if (response.getSuccess()) {
      log.info("✅ Plan actualizado exitosamente para company {}", idCompany);
      return ResponseEntity.ok(response);
    } else {
      log.warn("❌ Error al actualizar plan para company {}: {}", idCompany, response.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
  }

  /**
   * Extiende la vigencia de una suscripción.
   *
   * <p>POST /api/subscriptions/company/{idCompany}/extend
   */
  @PostMapping("/company/{idCompany}/extend")
  @Operation(
      summary = "Extender vigencia de suscripción",
      description = "Extiende la fecha de vencimiento de una suscripción (Solo SUPER_ADMIN)")
  public ResponseEntity<ExtendSubscriptionResponseDto> extendSubscription(
      @PathVariable Long idCompany, @Validated @RequestBody ExtendSubscriptionRequestDto request) {

    log.info(
        "⏰ SUPER_ADMIN extendiendo suscripción de company {} por {} meses",
        idCompany,
        request.getExtensionMonths());

    ExtendSubscriptionResponseDto response =
        subscriptionService.extendSubscription(idCompany, request);

    if (response.getSuccess()) {
      return ResponseEntity.ok(response);
    } else {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
  }

  /**
   * Cancela una suscripción.
   *
   * <p>DELETE /api/subscriptions/company/{idCompany}/cancel
   */
  @DeleteMapping("/company/{idCompany}/cancel")
  @Operation(
      summary = "Cancelar suscripción",
      description = "Cancela la suscripción de una empresa (Solo SUPER_ADMIN)")
  public ResponseEntity<CancelSubscriptionResponseDto> cancelSubscription(
      @PathVariable Long idCompany,
      @Validated @RequestBody CancelSubscriptionRequestDto request,
      HttpServletRequest httpRequest) {

    // Log del token para diagnóstico cuando falla permiso
    decodeAndLogJwtToken(httpRequest);

    log.info(
        "❌ SUPER_ADMIN cancelando suscripción de company {} - Razón: {}",
        idCompany,
        request.getReason());

    CancelSubscriptionResponseDto response =
        subscriptionService.cancelSubscription(idCompany, request);

    if (response.getSuccess()) {
      return ResponseEntity.ok(response);
    } else {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
  }

  // =====================================================
  // Endpoints para asignación de suscripciones
  // =====================================================
  /**
   * Obtiene empresas sin suscripción activa.
   *
   * <p>GET /api/subscriptions/available-companies
   */
  @GetMapping("/available-companies")
  @Operation(
      summary = "Obtener empresas sin suscripción",
      description = "Lista empresas disponibles para asignar suscripción (Solo SUPER_ADMIN)")
  public ResponseEntity<List<CompanyWithoutSubscriptionDto>> getAvailableCompanies() {

    log.info("🔍 SUPER_ADMIN solicitando empresas sin suscripción");

    List<CompanyWithoutSubscriptionDto> companies =
        subscriptionService.getCompaniesWithoutSubscription();

    return ResponseEntity.ok(companies);
  }

  /**
   * Asigna una nueva suscripción a una empresa.
   *
   * <p>POST /api/subscriptions
   */
  @PostMapping
  @Operation(
      summary = "Asignar suscripción",
      description = "Crea y asigna una nueva suscripción a una empresa (Solo SUPER_ADMIN)")
  public ResponseEntity<Void> assignSubscription(
      @Valid @RequestBody AssignSubscriptionRequestDto request) {

    log.info(
        "📝 SUPER_ADMIN asignando suscripción: empresa={}, plan={}",
        request.getIdCompany(),
        request.getPlanCode());

    subscriptionService.assignSubscription(request);

    // Agregar header Location según especificación
    HttpHeaders headers = new HttpHeaders();
    headers.setLocation(
        org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/company/{idCompany}")
            .buildAndExpand(request.getIdCompany())
            .toUri());

    return ResponseEntity.status(HttpStatus.CREATED).headers(headers).build();
  }

  /**
   * Obtiene los planes de suscripción disponibles.
   *
   * <p>GET /api/subscriptions/plans
   */
  @GetMapping("/plans")
  @Operation(
      summary = "Listar planes disponibles",
      description = "Obtiene la lista de planes de suscripción")
  public ResponseEntity<List<SubscriptionPlanDto>> getAvailablePlans(
      @RequestParam(defaultValue = "false") boolean includeInactive) {

    log.info("📋 Solicitando planes de suscripción (includeInactive={})", includeInactive);

    List<SubscriptionPlanDto> plans = subscriptionService.getAvailablePlans(includeInactive);

    return ResponseEntity.ok(plans);
  }

  /**
   * Decodifica el token JWT del request y muestra los roles y claims en los logs.
   *
   * @param request HttpServletRequest con el header Authorization
   */
  private void decodeAndLogJwtToken(HttpServletRequest request) {
    try {
      String authHeader = request.getHeader("Authorization");
      if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        log.warn("⚠️ No se encontró header Authorization con Bearer token");
        return;
      }

      String token = authHeader.substring(7); // Remover "Bearer "

      // Un JWT tiene 3 partes: header.payload.signature
      String[] parts = token.split("\\.");
      if (parts.length != 3) {
        log.warn("⚠️ Token JWT inválido: no tiene 3 partes separadas por puntos");
        return;
      }

      // Decodificar el payload (segunda parte)
      String payload = new String(Base64.getUrlDecoder().decode(parts[1]));

      // Parsear el JSON del payload
      ObjectMapper objectMapper = new ObjectMapper();
      @SuppressWarnings("unchecked")
      Map<String, Object> claims = objectMapper.readValue(payload, Map.class);

      log.info("🔐 Token JWT decodificado:");
      log.info("   - Subject (sub): {}", claims.get("sub"));
      log.info("   - Username: {}", claims.get("username"));
      log.info("   - Roles: {}", claims.get("roles"));
      log.info("   - Authorities: {}", claims.get("authorities"));
      log.info("   - All claims: {}", claims.keySet());

      // Mostrar roles específicamente
      Object rolesObj = claims.get("roles");
      if (rolesObj != null) {
        log.info("   - Roles encontrados en token: {}", rolesObj);
      }

      Object authoritiesObj = claims.get("authorities");
      if (authoritiesObj != null) {
        log.info("   - Authorities encontradas en token: {}", authoritiesObj);
      }

    } catch (Exception e) {
      log.error("❌ Error al decodificar token JWT: {}", e.getMessage(), e);
    }
  }
}
