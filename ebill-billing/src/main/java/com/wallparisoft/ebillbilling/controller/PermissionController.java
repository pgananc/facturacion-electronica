package com.wallparisoft.ebillbilling.controller;

import com.wallparisoft.ebillbilling.dto.permission.BulkUpdatePlansRequestDto;
import com.wallparisoft.ebillbilling.dto.permission.BulkUpdateResponseDto;
import com.wallparisoft.ebillbilling.dto.permission.PlanFeatureDto;
import com.wallparisoft.ebillbilling.service.IPermissionService;
import com.wallparisoft.ebillbilling.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controlador para gestión de permisos y features de planes de suscripción. */
@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Permissions", description = "Gestión de permisos y features de planes")
@SecurityRequirement(name = "bearerAuth")
public class PermissionController {

  private final IPermissionService permissionService;
  private final SecurityUtils securityUtils;

  /**
   * Obtiene las features de todos los planes de suscripción.
   *
   * <p>GET /api/permissions/features/plans
   *
   * <p>Acceso: Cualquier usuario autenticado con un rol
   */
  @GetMapping("/features/plans")
  @Operation(
      summary = "Ver features de planes",
      description =
          "Obtiene las features disponibles en cada plan de suscripción (cualquier autenticado)")
  public ResponseEntity<?> getPlanFeatures() {
    log.info("📋 Solicitando features de planes");

    // Validar que esté autenticado y tenga un rol
    if (!securityUtils.isAuthenticatedWithRole()) {
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("status", 401);
      errorResponse.put("error", "Unauthorized");
      errorResponse.put("message", "Usuario no autenticado o sin roles asignados");

      return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    try {
      List<PlanFeatureDto> planFeatures = permissionService.getPlanFeatures();

      log.info("✅ Retornando features de {} planes", planFeatures.size());

      return ResponseEntity.ok(planFeatures);

    } catch (Exception e) {
      log.error("❌ Error obteniendo features de planes: {}", e.getMessage(), e);

      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("status", 500);
      errorResponse.put("error", "Internal Server Error");
      errorResponse.put("message", "Error interno al obtener features de planes");

      return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Actualiza las features de múltiples planes en una operación masiva.
   *
   * <p>PUT /api/permissions/plans/bulk
   *
   * <p>Acceso: Solo SUPER_ADMIN
   */
  @PutMapping("/plans/bulk")
  @Operation(
      summary = "Actualizar planes (bulk)",
      description =
          "Actualiza las features de múltiples planes de suscripción en una operación masiva (Solo SUPER_ADMIN)")
  public ResponseEntity<?> updatePlansBulk(@Valid @RequestBody BulkUpdatePlansRequestDto request) {
    log.info("🔄 Solicitando actualización masiva de {} planes", request.getUpdates().size());

    // Validar que sea SUPER_ADMIN
    if (!securityUtils.isSuperAdmin()) {
      log.warn("❌ Acceso denegado: Se requiere rol SUPER_ADMIN para actualizar planes");

      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("status", 403);
      errorResponse.put("error", "Forbidden");
      errorResponse.put("message", "Se requiere rol SUPER_ADMIN para realizar esta operación");

      return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    try {
      BulkUpdateResponseDto response = permissionService.updatePlansBulk(request);

      if (response.getSuccess()) {
        log.info(
            "✅ Actualización masiva completada: {} exitosos, {} fallidos",
            response.getTotalUpdated(),
            response.getTotalFailed());
      } else {
        log.warn(
            "⚠️ Actualización masiva completada con errores: {} exitosos, {} fallidos",
            response.getTotalUpdated(),
            response.getTotalFailed());
      }

      return ResponseEntity.ok(response);

    } catch (Exception e) {
      log.error("❌ Error en actualización masiva de planes: {}", e.getMessage(), e);

      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("status", 500);
      errorResponse.put("error", "Internal Server Error");
      errorResponse.put("message", "Error interno al actualizar planes");

      return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
