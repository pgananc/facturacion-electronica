package com.wallparisoft.authentication.controller;

import static com.wallparisoft.ebill.utils.LogUtil.setLogs;
import static com.wallparisoft.ebill.utils.log.EventType.REQUEST;
import static com.wallparisoft.ebill.utils.log.EventType.RESPONSE;
import static com.wallparisoft.ebill.utils.log.Level.LEVEL_001;
import static com.wallparisoft.ebill.utils.log.Level.LEVEL_002;
import static lombok.AccessLevel.PRIVATE;

import com.wallparisoft.authentication.dto.BulkUpdateRolesDto;
import com.wallparisoft.authentication.dto.FeatureValidationRequestDto;
import com.wallparisoft.authentication.dto.FeatureValidationResponseDto;
import com.wallparisoft.authentication.dto.UpdateFeaturePlansDto;
import com.wallparisoft.authentication.exception.ModelNotFoundException;
import com.wallparisoft.authentication.service.IFeaturePermissionService;
import com.wallparisoft.ebill.auth.util.UtilsToken;
import com.wallparisoft.ebill.utils.log.EventLog;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for feature permission validation
 * Handles both role-based (Layer 1) and complete (Role + Plan) validations
 */
@RestController
@RequestMapping("api/permission")
@FieldDefaults(level = PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class PermissionController {

  IFeaturePermissionService featurePermissionService;
  UtilsToken utilsToken;

  /**
   * Validate ROLE + PLAN (Coordinator - Complete validation)
   * Performs complete validation:
   * 1. Checks role permission (local)
   * 2. Calls ebill-billing to check subscription
   * 3. Verifies plan includes the feature
   *
   * @param request Contains featureCode, idCompany and idUser
   * @return FeatureValidationResponseDto with allowed flag, reason, and upgrade URL if denied
   */
  @PostMapping("/validate-feature")
  public ResponseEntity<FeatureValidationResponseDto> validateFeatureAccess(
      @Valid @RequestBody FeatureValidationRequestDto request) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(request),
        REQUEST,
        LEVEL_001);

    FeatureValidationResponseDto response =
        featurePermissionService.validateFeatureAccess(request);

    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(response),
        RESPONSE,
        LEVEL_001);

    HttpStatus status = Boolean.TRUE.equals(response.getAllowed()) ? HttpStatus.OK : HttpStatus.FORBIDDEN;
    return new ResponseEntity<>(response, status);
  }

  /**
   * Get all features allowed for the authenticated user in their current company
   * Extracts idUser and idCompany from JWT token (no database queries needed)
   *
   * @param principal Authenticated user (contains idUser in sub claim)
   * @param request HttpServletRequest to extract Authorization header
   * @return List of feature codes that the user can access
   */
  @GetMapping("/user/features")
  public ResponseEntity<List<String>> getUserAllowedFeatures(
      Principal principal, HttpServletRequest request) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information("Getting allowed features for authenticated user"),
        REQUEST,
        LEVEL_001);

    try {
      // 1. Extract token from SecurityContext or HttpServletRequest
      String token = extractTokenFromRequest(request);
      if (token == null) {
        setLogs(
            EventLog.builder()
                .service(traceElement.getClassName())
                .method(traceElement.getMethodName())
                .information("Token not found in request"),
            RESPONSE,
            LEVEL_002);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(Collections.emptyList());
      }

      // 2. Decode token to get claims
      Claims claims = utilsToken.getClaimFromToken(token, Function.identity());

      // 3. Extract idUser from sub claim
      String idUserStr = claims.getSubject();
      Long idUser;
      try {
        idUser = Long.parseLong(idUserStr);
      } catch (NumberFormatException e) {
        setLogs(
            EventLog.builder()
                .service(traceElement.getClassName())
                .method(traceElement.getMethodName())
                .information("Invalid idUser format in token: " + idUserStr),
            RESPONSE,
            LEVEL_002);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(Collections.emptyList());
      }

      // 4. Extract idCompany from companyEstablishments claim
      Long idCompany = extractIdCompanyFromClaims(claims);
      if (idCompany == null) {
        setLogs(
            EventLog.builder()
                .service(traceElement.getClassName())
                .method(traceElement.getMethodName())
                .information("idCompany not found in token claims"),
            RESPONSE,
            LEVEL_002);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(Collections.emptyList());
      }

      setLogs(
          EventLog.builder()
              .service(traceElement.getClassName())
              .method(traceElement.getMethodName())
              .information(
                  String.format("Using company %d for user %d", idCompany, idUser)),
          REQUEST,
          LEVEL_001);

      // 5. Get allowed features
      List<String> allowedFeatures =
          featurePermissionService.getUserAllowedFeatures(idUser, idCompany);

      setLogs(
          EventLog.builder()
              .service(traceElement.getClassName())
              .method(traceElement.getMethodName())
              .information(
                  String.format("Found %d allowed features", allowedFeatures.size())),
          RESPONSE,
          LEVEL_001);

      return ResponseEntity.ok(allowedFeatures);

    } catch (ModelNotFoundException e) {
      setLogs(
          EventLog.builder()
              .service(traceElement.getClassName())
              .method(traceElement.getMethodName())
              .information(e.getMessage()),
          RESPONSE,
          LEVEL_002);
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(Collections.emptyList());
    } catch (Exception e) {
      log.error("Error getting user features", e);
      setLogs(
          EventLog.builder()
              .service(traceElement.getClassName())
              .method(traceElement.getMethodName())
              .information("Error: " + e.getMessage()),
          RESPONSE,
          LEVEL_002);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Collections.emptyList());
    }
  }

  /**
   * Extract JWT token from HttpServletRequest Authorization header
   */
  private String extractTokenFromRequest(HttpServletRequest request) {
    // Try to get from SecurityContext first (as used by FeignClientConfig)
    try {
      org.springframework.security.core.Authentication authentication =
          SecurityContextHolder.getContext().getAuthentication();
      if (authentication != null && authentication.getCredentials() instanceof String token) {
        return token;
      }
    } catch (Exception e) {
      log.debug("Could not get token from SecurityContext: {}", e.getMessage());
    }

    // Fallback: extract from Authorization header
    String authHeader = request.getHeader("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      return authHeader.substring(7);
    }

    return null;
  }

  /**
   * Extract idCompany from JWT claims (companyEstablishments)
   * Returns the first company's idCompany from the token
   */
  @SuppressWarnings("unchecked")
  private Long extractIdCompanyFromClaims(Claims claims) {
    try {
      Object companyEstablishmentsObj = claims.get("companyEstablishments");
      if (companyEstablishmentsObj == null) {
        return null;
      }

      // companyEstablishments is a List<Map<String, Object>>
      if (companyEstablishmentsObj instanceof List) {
        List<Map<String, Object>> companyEstablishments =
            (List<Map<String, Object>>) companyEstablishmentsObj;
        if (!companyEstablishments.isEmpty()) {
          Map<String, Object> firstCompany = companyEstablishments.get(0);
          Object idCompanyObj = firstCompany.get("idCompany");
          if (idCompanyObj != null) {
            if (idCompanyObj instanceof Number) {
              return ((Number) idCompanyObj).longValue();
            } else if (idCompanyObj instanceof String) {
              return Long.parseLong((String) idCompanyObj);
            }
          }
        }
      }
    } catch (Exception e) {
      log.warn("Error extracting idCompany from claims: {}", e.getMessage());
    }

    return null;
  }

  // ============================================
  // ROLE PERMISSION MANAGEMENT ENDPOINTS
  // Require ROLE_PERMISSION_MANAGEMENT feature (super_admin only)
  // ============================================

  /**
   * Get all features with their permission details
   * Endpoint: GET /api/permissions/features
   */
  @GetMapping("/permissions/features")
  public ResponseEntity<List<com.wallparisoft.authentication.dto.FeaturePermissionDto>>
      getAllFeaturesForManagement() {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        REQUEST,
        LEVEL_001);

    List<com.wallparisoft.authentication.dto.FeaturePermissionDto> features =
        featurePermissionService.getAllFeaturesForManagement();

    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(String.format("Found %d features", features.size())),
        RESPONSE,
        LEVEL_001);

    return ResponseEntity.ok(features);
  }

  /**
   * Get all permissions for a specific role
   * Endpoint: GET /api/permissions/role/{idRole}
   */
  @GetMapping("/permissions/role/{idRole}")
  public ResponseEntity<com.wallparisoft.authentication.dto.RolePermissionsDto>
      getRolePermissions(@PathVariable Integer idRole) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information("idRole: " + idRole),
        REQUEST,
        LEVEL_001);

    com.wallparisoft.authentication.dto.RolePermissionsDto rolePermissions =
        featurePermissionService.getRolePermissions(idRole);

    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(rolePermissions),
        RESPONSE,
        LEVEL_001);

    return ResponseEntity.ok(rolePermissions);
  }

  /**
   * Update allowed roles for a specific feature
   * Endpoint: PUT /api/permissions/feature/{featureCode}
   */
  @org.springframework.web.bind.annotation.PutMapping("/permissions/feature/{featureCode}")
  public ResponseEntity<com.wallparisoft.authentication.dto.FeaturePermissionDto>
      updateFeatureRoles(
          @PathVariable String featureCode,
          @Valid
              @RequestBody
              com.wallparisoft.authentication.dto.UpdateFeatureRolesDto updateDto) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(String.format("featureCode: %s, update: %s", featureCode, updateDto)),
        REQUEST,
        LEVEL_001);

    com.wallparisoft.authentication.dto.FeaturePermissionDto updated =
        featurePermissionService.updateFeatureRoles(featureCode, updateDto);

    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(updated),
        RESPONSE,
        LEVEL_001);

    return ResponseEntity.ok(updated);
  }

  /**
   * Bulk update allowed roles for multiple features
   * Endpoint: PUT /api/permission/permissions/bulk
   */
  @org.springframework.web.bind.annotation.PutMapping("/permissions/bulk")
  public ResponseEntity<List<com.wallparisoft.authentication.dto.FeaturePermissionDto>>
      bulkUpdateRoles(@Valid @RequestBody BulkUpdateRolesDto bulkDto) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(String.format("Bulk updating %d features", bulkDto.getFeatureRoles().size())),
        REQUEST,
        LEVEL_001);

    List<com.wallparisoft.authentication.dto.FeaturePermissionDto> updated =
        featurePermissionService.bulkUpdateRoles(bulkDto);

    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(String.format("Updated %d features", updated.size())),
        RESPONSE,
        LEVEL_001);

    return ResponseEntity.ok(updated);
  }

  // ============================================
  // PLAN PERMISSION MANAGEMENT ENDPOINTS
  // ============================================

  /**
   * Get required plans for a specific feature
   * Endpoint: GET /api/permission/permissions/feature/{featureCode}/plans
   */
  @GetMapping("/permissions/feature/{featureCode}/plans")
  public ResponseEntity<com.wallparisoft.authentication.dto.FeaturePermissionDto>
      getFeaturePlans(@PathVariable String featureCode) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information("featureCode: " + featureCode),
        REQUEST,
        LEVEL_001);

    com.wallparisoft.authentication.dto.FeaturePermissionDto feature =
        featurePermissionService.getFeaturePlans(featureCode);

    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(feature),
        RESPONSE,
        LEVEL_001);

    return ResponseEntity.ok(feature);
  }

  /**
   * Update required plans for a specific feature
   * Endpoint: PUT /api/permission/permissions/feature/{featureCode}/plans
   */
  @org.springframework.web.bind.annotation.PutMapping("/permissions/feature/{featureCode}/plans")
  public ResponseEntity<com.wallparisoft.authentication.dto.FeaturePermissionDto>
      updateFeaturePlans(
          @PathVariable String featureCode,
          @Valid @RequestBody UpdateFeaturePlansDto updateDto) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(
                String.format("featureCode: %s, plans: %s", featureCode, updateDto.getRequiredPlans())),
        REQUEST,
        LEVEL_001);

    com.wallparisoft.authentication.dto.FeaturePermissionDto updated =
        featurePermissionService.updateFeaturePlans(featureCode, updateDto);

    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(updated),
        RESPONSE,
        LEVEL_001);

    return ResponseEntity.ok(updated);
  }

}
