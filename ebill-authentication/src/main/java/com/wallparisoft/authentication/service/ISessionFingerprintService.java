package com.wallparisoft.authentication.service;

import com.wallparisoft.authentication.dto.SessionValidationResponseDto;
import java.util.UUID;

public interface ISessionFingerprintService {

  /**
   * Crea un nuevo fingerprint de sesión al momento del login.
   *
   * @param userName Nombre de usuario
   * @param ipAddress Dirección IP del cliente
   * @param userAgent User-Agent del navegador
   * @return UUID de la sesión creada
   */
  UUID createSessionFingerprint(String userName, String ipAddress, String userAgent);

  /**
   * Valida que la sesión actual coincide con el fingerprint guardado.
   *
   * @param userName Nombre de usuario (extraído del JWT)
   * @param currentIp IP actual del request
   * @param currentUserAgent User-Agent actual del request
   * @return Response con el resultado de la validación
   */
  SessionValidationResponseDto validateSession(
      String userName, String currentIp, String currentUserAgent);

  /**
   * Elimina el fingerprint de sesión del usuario (logout).
   *
   * @param userName Nombre de usuario
   */
  void deleteSessionFingerprint(String userName);

  /**
   * Actualiza el último timestamp de actividad de la sesión.
   *
   * @param sessionId UUID de la sesión
   */
  void updateLastActivity(UUID sessionId);

  /**
   * Extrae el nombre del navegador base del User-Agent.
   *
   * @param userAgent User-Agent completo
   * @return Nombre del navegador (Chrome, Firefox, Safari, Edge, etc.)
   */
  String extractBrowserBase(String userAgent);
}
