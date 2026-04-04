package com.wallparisoft.authentication.repository;

import com.wallparisoft.authentication.entity.ClientToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IClientTokenRepository extends JpaRepository<ClientToken, Long> {

  /**
   * Finds a client token by token value
   *
   * @param token the token value to search for
   * @return Optional containing the ClientToken if found
   */
  Optional<ClientToken> findByToken(String token);

  /**
   * Finds an active and valid token by client number and token value
   *
   * @param clientNumber the client number
   * @param token the token value
   * @return Optional containing the ClientToken if found and valid
   */
  @Query(
      "SELECT ct FROM ClientToken ct WHERE ct.clientNumber = :clientNumber "
          + "AND ct.token = :token AND ct.status = true "
          + "AND ct.expiryDate > CURRENT_TIMESTAMP")
  Optional<ClientToken> findValidTokenByClientNumberAndToken(
      @Param("clientNumber") String clientNumber, @Param("token") String token);

  /**
   * Finds tokens by client number
   *
   * @param clientNumber the client number
   * @return list of ClientToken entities
   */
  java.util.List<ClientToken> findByClientNumber(String clientNumber);

  /**
   * Finds all active tokens for a specific client
   *
   * @param clientNumber the client number
   * @return list of active ClientToken entities
   */
  @Query("SELECT ct FROM ClientToken ct WHERE ct.clientNumber = :clientNumber AND ct.status = true")
  java.util.List<ClientToken> findActiveTokensByClientNumber(
      @Param("clientNumber") String clientNumber);

  /**
   * Verifica si el cliente tiene tokens activos
   *
   * @param clientNumber the client number
   * @return true si tiene tokens activos, false si no tiene tokens activos
   */
  @Query(
      "SELECT COUNT(ct) > 0 FROM ClientToken ct WHERE ct.clientNumber = :clientNumber AND ct.status = true")
  boolean hasActiveTokens(@Param("clientNumber") String clientNumber);
}
