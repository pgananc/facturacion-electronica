package com.wallparisoft.authentication.controller;

import static com.wallparisoft.ebill.utils.LogUtil.setLogs;
import static com.wallparisoft.ebill.utils.log.EventType.REQUEST;
import static com.wallparisoft.ebill.utils.log.EventType.RESPONSE;
import static com.wallparisoft.ebill.utils.log.Level.LEVEL_001;

import com.wallparisoft.authentication.dto.ErrorResponseDto;
import com.wallparisoft.authentication.dto.FeatureValidationRequestDto;
import com.wallparisoft.authentication.dto.FeatureValidationResponseDto;
import com.wallparisoft.authentication.dto.MenuRequestDto;
import com.wallparisoft.authentication.dto.MenuResponseDto;
import com.wallparisoft.authentication.service.IFeaturePermissionService;
import com.wallparisoft.authentication.service.IMenuAdminService;
import com.wallparisoft.authentication.exception.ModelNotFoundException;
import com.wallparisoft.ebill.auth.util.UtilsToken;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import com.wallparisoft.ebill.utils.log.EventLog;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/menu")
@RequiredArgsConstructor
@Slf4j
public class MenuAdminController {

  private final IMenuAdminService menuAdminService;
  private final IFeaturePermissionService featurePermissionService;
  private final UtilsToken utilsToken;

  /**
   * Lista todos los menús en estructura de árbol completo
   *
   * @param principal Usuario autenticado
   * @return Lista de menús raíz con sus submenús
   */
  @GetMapping
  public ResponseEntity<?> getAllMenus(Principal principal) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        REQUEST,
        LEVEL_001);

    if (!validateSuperAdmin(principal)) {
      ErrorResponseDto error =
          ErrorResponseDto.builder()
              .message("No tiene permisos para gestionar menús")
              .error("FORBIDDEN")
              .build();
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    List<MenuResponseDto> menus = menuAdminService.getAllMenus();
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information("Returned " + menus.size() + " root menus"),
        RESPONSE,
        LEVEL_001);

    return ResponseEntity.ok(menus);
  }

  /**
   * Obtiene un menú específico por ID
   *
   * @param id ID del menú
   * @param principal Usuario autenticado
   * @return MenuResponseDto con la información del menú
   */
  @GetMapping("/{id}")
  public ResponseEntity<?> getMenuById(@PathVariable Integer id, Principal principal) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        REQUEST,
        LEVEL_001);

    if (!validateSuperAdmin(principal)) {
      ErrorResponseDto error =
          ErrorResponseDto.builder()
              .message("No tiene permisos para gestionar menús")
              .error("FORBIDDEN")
              .build();
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    try {
      MenuResponseDto menu = menuAdminService.getMenuById(id);
      setLogs(
          EventLog.builder()
              .service(traceElement.getClassName())
              .method(traceElement.getMethodName())
              .information(menu),
          RESPONSE,
          LEVEL_001);
      return ResponseEntity.ok(menu);
    } catch (ModelNotFoundException e) {
      ErrorResponseDto error =
          ErrorResponseDto.builder()
              .message("Menú no encontrado")
              .error("NOT_FOUND")
              .build();
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
  }

  /**
   * Crea un nuevo menú
   *
   * @param request Datos del menú a crear
   * @param principal Usuario autenticado
   * @return MenuResponseDto con el menú creado
   */
  @PostMapping
  public ResponseEntity<?> createMenu(
      @Valid @RequestBody MenuRequestDto request, Principal principal) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(request),
        REQUEST,
        LEVEL_001);

    if (!validateSuperAdmin(principal)) {
      ErrorResponseDto error =
          ErrorResponseDto.builder()
              .message("No tiene permisos para gestionar menús")
              .error("FORBIDDEN")
              .build();
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    try {
      MenuResponseDto createdMenu = menuAdminService.createMenu(request);
      setLogs(
          EventLog.builder()
              .service(traceElement.getClassName())
              .method(traceElement.getMethodName())
              .information(createdMenu),
          RESPONSE,
          LEVEL_001);
      return ResponseEntity.status(HttpStatus.CREATED).body(createdMenu);
    } catch (ModelNotFoundException e) {
      ErrorResponseDto error =
          ErrorResponseDto.builder()
              .message(e.getMessage())
              .error("VALIDATION_ERROR")
              .build();
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    } catch (IllegalArgumentException e) {
      ErrorResponseDto error =
          ErrorResponseDto.builder()
              .message(e.getMessage())
              .error("VALIDATION_ERROR")
              .build();
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
  }

  /**
   * Actualiza un menú existente
   *
   * @param id ID del menú a actualizar
   * @param request Datos actualizados del menú
   * @param principal Usuario autenticado
   * @return MenuResponseDto con el menú actualizado
   */
  @PutMapping("/{id}")
  public ResponseEntity<?> updateMenu(
      @PathVariable Integer id,
      @Valid @RequestBody MenuRequestDto request,
      Principal principal) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(request),
        REQUEST,
        LEVEL_001);

    if (!validateSuperAdmin(principal)) {
      ErrorResponseDto error =
          ErrorResponseDto.builder()
              .message("No tiene permisos para gestionar menús")
              .error("FORBIDDEN")
              .build();
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    try {
      MenuResponseDto updatedMenu = menuAdminService.updateMenu(id, request);
      setLogs(
          EventLog.builder()
              .service(traceElement.getClassName())
              .method(traceElement.getMethodName())
              .information(updatedMenu),
          RESPONSE,
          LEVEL_001);
      return ResponseEntity.ok(updatedMenu);
    } catch (ModelNotFoundException e) {
      ErrorResponseDto error =
          ErrorResponseDto.builder()
              .message("Menú no encontrado")
              .error("NOT_FOUND")
              .build();
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    } catch (IllegalArgumentException e) {
      ErrorResponseDto error =
          ErrorResponseDto.builder()
              .message(e.getMessage())
              .error("VALIDATION_ERROR")
              .build();
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
  }

  /**
   * Elimina físicamente un menú y todos sus registros relacionados en cascada.
   * Elimina:
   * - Todos los registros en menu_role asociados al menú
   * - El feature_permission si solo este menú lo está usando
   * - El menú mismo
   * - Todos los submenús recursivamente
   *
   * @param id ID del menú a eliminar físicamente
   * @param principal Usuario autenticado
   * @return ResponseEntity sin contenido
   */
  @DeleteMapping("/{id}/hard")
  public ResponseEntity<?> hardDeleteMenu(@PathVariable Integer id, Principal principal) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        REQUEST,
        LEVEL_001);

    if (!validateSuperAdmin(principal)) {
      ErrorResponseDto error =
          ErrorResponseDto.builder()
              .message("No tiene permisos para gestionar menús")
              .error("FORBIDDEN")
              .build();
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    try {
      menuAdminService.hardDeleteMenu(id);
      setLogs(
          EventLog.builder()
              .service(traceElement.getClassName())
              .method(traceElement.getMethodName())
              .information("Menu hard deleted successfully"),
          RESPONSE,
          LEVEL_001);
      return ResponseEntity.noContent().build();
    } catch (ModelNotFoundException e) {
      ErrorResponseDto error =
          ErrorResponseDto.builder()
              .message("Menú no encontrado")
              .error("NOT_FOUND")
              .build();
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    } catch (Exception e) {
      log.error("Error al eliminar físicamente el menú ID: {}", id, e);
      ErrorResponseDto error =
          ErrorResponseDto.builder()
              .message("Error al eliminar el menú: " + e.getMessage())
              .error("INTERNAL_ERROR")
              .build();
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
  }

  /**
   * Check if the authenticated user has access to a specific feature
   * Extracts idUser and idCompany from JWT token
   *
   * @param featureCode Feature code to validate
   * @param principal Authenticated user
   * @param request HttpServletRequest to extract token
   * @return FeatureValidationResponseDto with allowed flag and reason
   */
  @GetMapping("/check-feature/{featureCode}")
  public ResponseEntity<?> checkFeatureAccess(
      @PathVariable String featureCode,
      Principal principal,
      HttpServletRequest request) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information("Checking feature access: " + featureCode),
        REQUEST,
        LEVEL_001);

    try {
      // Extract token from request
      String token = extractTokenFromRequest(request);
      if (token == null) {
        ErrorResponseDto error =
            ErrorResponseDto.builder()
                .message("Token not found in request")
                .error("UNAUTHORIZED")
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
      }

      // Decode token to get claims
      Claims claims = utilsToken.getClaimFromToken(token, Function.identity());

      // Extract idUser from sub claim
      String idUserStr = claims.getSubject();
      Long idUser;
      try {
        idUser = Long.parseLong(idUserStr);
      } catch (NumberFormatException e) {
        ErrorResponseDto error =
            ErrorResponseDto.builder()
                .message("Invalid idUser format in token: " + idUserStr)
                .error("UNAUTHORIZED")
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
      }

      // Extract idCompany from companyEstablishments claim
      Long idCompany = extractIdCompanyFromClaims(claims);
      if (idCompany == null) {
        ErrorResponseDto error =
            ErrorResponseDto.builder()
                .message("idCompany not found in token claims")
                .error("FORBIDDEN")
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
      }

      // Validate feature access
      FeatureValidationRequestDto validationRequest =
          FeatureValidationRequestDto.builder()
              .featureCode(featureCode)
              .idUser(idUser)
              .idCompany(idCompany)
              .build();

      FeatureValidationResponseDto response =
          featurePermissionService.validateFeatureAccess(validationRequest);

      setLogs(
          EventLog.builder()
              .service(traceElement.getClassName())
              .method(traceElement.getMethodName())
              .information(response),
          RESPONSE,
          LEVEL_001);

      HttpStatus status =
          Boolean.TRUE.equals(response.getAllowed()) ? HttpStatus.OK : HttpStatus.FORBIDDEN;
      return new ResponseEntity<>(response, status);

    } catch (Exception e) {
      log.error("Error checking feature access", e);
      ErrorResponseDto error =
          ErrorResponseDto.builder()
              .message("Error checking feature access: " + e.getMessage())
              .error("INTERNAL_ERROR")
              .build();
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
  }

  /**
   * Extract JWT token from HttpServletRequest Authorization header
   */
  private String extractTokenFromRequest(HttpServletRequest request) {
    // Try to get from SecurityContext first
    try {
      org.springframework.security.core.Authentication authentication =
          org.springframework.security.core.context.SecurityContextHolder.getContext()
              .getAuthentication();
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
   */
  @SuppressWarnings("unchecked")
  private Long extractIdCompanyFromClaims(Claims claims) {
    try {
      Object companyEstablishmentsObj = claims.get("companyEstablishments");
      if (companyEstablishmentsObj == null) {
        return null;
      }

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

  /**
   * Valida que el usuario tenga rol super_admin
   *
   * @param principal Usuario autenticado
   * @return true si es super_admin, false en caso contrario
   */
  private boolean validateSuperAdmin(Principal principal) {
    String userName = principal.getName();
    return menuAdminService.isSuperAdmin(userName);
  }
}
