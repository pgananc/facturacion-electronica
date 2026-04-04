package com.wallparisoft.authentication.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;

/** Utilidad para extraer información de las peticiones HTTP. */
@UtilityClass
public class RequestUtils {

  /**
   * Extrae la IP real del cliente considerando proxies y load balancers.
   *
   * <p>Verifica los siguientes headers en orden:
   *
   * <ol>
   *   <li>X-Forwarded-For: Puede contener múltiples IPs si hay varios proxies. El formato es:
   *       client, proxy1, proxy2, ...
   *   <li>X-Real-IP: Usado por Nginx y otros proxies
   *   <li>RemoteAddr: IP remota directa como fallback
   * </ol>
   *
   * @param request HttpServletRequest
   * @return IP del cliente
   */
  public static String extractClientIp(HttpServletRequest request) {
    // X-Forwarded-For puede contener múltiples IPs si hay varios proxies
    // El formato es: client, proxy1, proxy2, ...
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
      // Tomamos la primera IP (el cliente original)
      String[] ips = xForwardedFor.split(",");
      return ips[0].trim();
    }

    // X-Real-IP es usado por Nginx
    String xRealIp = request.getHeader("X-Real-IP");
    if (xRealIp != null && !xRealIp.isEmpty()) {
      return xRealIp.trim();
    }

    // Fallback a la IP remota directa
    return request.getRemoteAddr();
  }
}
