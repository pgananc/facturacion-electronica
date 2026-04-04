package com.wallparisoft.authentication.service;

import com.wallparisoft.authentication.dto.UserLoginRequestDto;
import com.wallparisoft.authentication.response.TokenResponse;

public interface ILoginService {
  TokenResponse authentication(UserLoginRequestDto userLoginRequestDto) throws Exception;

  /**
   * Autenticación con fingerprint de sesión.
   *
   * @param userLoginRequestDto Credenciales del usuario
   * @param ipAddress Dirección IP del cliente
   * @param userAgent User-Agent del navegador
   * @return TokenResponse con token JWT y sessionId
   */
  TokenResponse authentication(
      UserLoginRequestDto userLoginRequestDto, String ipAddress, String userAgent) throws Exception;

  void logoutUser(String userName);
}
