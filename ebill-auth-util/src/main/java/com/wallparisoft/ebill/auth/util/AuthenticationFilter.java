package com.wallparisoft.ebill.auth.util;

import com.wallparisoft.ebill.auth.util.service.IAuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class AuthenticationFilter extends OncePerRequestFilter {

  private final IAuthService authService;

  public AuthenticationFilter(IAuthService authService) {
    this.authService = authService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {

    // 1. Manejar solicitudes OPTIONS (preflight)
    // Spring Security ya debería haber permitido estas con .permitAll(),
    // pero es buena práctica no aplicar la lógica de autenticación aquí.
    if (HttpMethod.OPTIONS.matches(request.getMethod())) {
      chain.doFilter(request, response);
      return; // Detiene la ejecución de este filtro para OPTIONS
    }

    // 2. Manejar rutas públicas que NO requieren autenticación
    // Ya tienes esta lógica de exclusión. Asegúrate de que cubre todas las rutas publicas.
    String path = request.getServletPath();
    if (path.equals("/api/login")
        || path.startsWith("/api/mail/")
        || path.startsWith("/api/token/")
        || path.startsWith("/api/user/restore/")
        || path.startsWith("/api/client-token")
        || path.startsWith("/api/client/search")) {
      chain.doFilter(request, response);
      return; // Permite que las solicitudes a estas rutas públicas pasen sin autenticación
    }

    // 3. Lógica de autenticación para rutas protegidas
    // Si no es OPTIONS y no es una ruta pública, intenta autenticar
    Optional<Authentication> authentication = authService.authenticate(request);
    authentication.ifPresent(SecurityContextHolder.getContext()::setAuthentication);

    // Continuar con la cadena de filtros
    chain.doFilter(request, response);
  }
}
