package com.wallparisoft.authentication.service;

import com.wallparisoft.authentication.entity.ClientToken;
import com.wallparisoft.authentication.response.TokenResponse;

/** Service interface for ClientToken operations */
public interface IClientTokenService extends ICRUD<ClientToken> {

  /**
   * Saves a new client token with automatic expiry date set to 5 minutes
   *
   * @param clientNumber the client number
   * @param clientEmail the client email
   * @return the saved ClientToken entity
   */
  ClientToken saveClientToken(String clientNumber, String clientEmail, String clientIp);

  /**
   * Resends a token to the client, deactivating all existing active tokens for the client and
   * creating a new one
   *
   * @param clientNumber the client number
   * @param clientEmail the client email
   * @param clientIp the client IP address
   * @return the new ClientToken entity
   */
  ClientToken resendClientToken(String clientNumber, String clientEmail, String clientIp);

  /**
   * Valida el token del cliente y genera un token de acceso JWT si es válido
   *
   * @param clientNumber el número de cliente
   * @param token el token del cliente
   * @param clientIp la IP del cliente
   * @return ClientTokenValidationResponse con el token JWT si es válido
   */
  TokenResponse validateTokenAndGenerateAccessToken(
      String clientNumber, String token, String clientIp);
}
