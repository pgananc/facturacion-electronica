package com.wallparisoft.ebillbilling.util;

import com.wallparisoft.ebill.auth.util.security.AuthenticationResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Utilidad compartida para validación de seguridad y permisos. Usa AuthenticationResolver de
 * ebill-auth-util para obtener la autenticación y proporciona métodos helper para validaciones
 * comunes.
 *
 * <p><b>Modelo de roles:</b> super_admin es global (aplica a todas las empresas). admin, member,
 * etc. son por empresa; cuando no hay super_admin, debe validarse el rol para la empresa concreta
 * (idCompany).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityUtils {

  private final AuthenticationResolver authenticationResolver;

  /**
   * Verifica si el usuario actual es SUPER_ADMIN. Reutiliza la lógica de SubscriptionServiceImpl
   * para mantener consistencia.
   *
   * @return true si el usuario es SUPER_ADMIN, false en caso contrario
   */
  public boolean isSuperAdmin() {
    try {
      Authentication authentication = authenticationResolver.getAuthentication();
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

      // Log para debugging
      log.debug(
          "🔍 Verificando si es SUPER_ADMIN - Principal: {}, Authorities: {}",
          authentication.getPrincipal(),
          authentication.getAuthorities());

      // Reconocer super_admin en listas tipo ROLE_[super_admin, admin] (roles por empresa)
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
        log.debug(
            "⚠️ Usuario no es SUPER_ADMIN - Authorities: {}", authentication.getAuthorities());
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

  /**
   * Verifica si el usuario está autenticado y tiene al menos un rol (authority).
   *
   * @return true si el usuario está autenticado y tiene authorities, false en caso contrario
   */
  public boolean isAuthenticatedWithRole() {
    try {
      Authentication auth = authenticationResolver.getAuthentication();
      return auth != null
          && auth.isAuthenticated()
          && auth.getAuthorities() != null
          && !auth.getAuthorities().isEmpty();
    } catch (Exception e) {
      log.error("❌ Error al verificar autenticación: {}", e.getMessage(), e);
      return false;
    }
  }

  /**
   * Obtiene el objeto Authentication actual del SecurityContext.
   *
   * @return Authentication actual o null si no está disponible
   */
  public Authentication getCurrentAuthentication() {
    return authenticationResolver.getAuthentication();
  }
}
