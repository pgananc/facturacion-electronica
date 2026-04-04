package com.wallparisoft.ebill.auth.util.service.impl;

import static java.util.Objects.nonNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallparisoft.ebill.auth.util.UtilsRedis;
import com.wallparisoft.ebill.auth.util.UtilsToken;
import com.wallparisoft.ebill.auth.util.constant.Constants;
import com.wallparisoft.ebill.auth.util.exception.InternalErrorException;
import com.wallparisoft.ebill.auth.util.model.RedisEntity;
import com.wallparisoft.ebill.auth.util.model.TokenInformation;
import com.wallparisoft.ebill.auth.util.model.TokenSession;
import com.wallparisoft.ebill.auth.util.service.IAuthService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

  private static final String USER_NAME = "userName";
  private static final String BEARER_PREFIX = "Bearer ";

  private final RedisTemplate<String, String> redisTemplate;
  private final UtilsToken utilsToken;
  private final UtilsRedis utilsRedis;
  private final ObjectMapper objectMapper;

  @Override
  public TokenSession generateToken(TokenInformation informationEntity, boolean verified) {
    try {
      Map<String, Object> claims = new HashMap<>();
      claims.put("sub", informationEntity.getIdUser());
      claims.put(USER_NAME, informationEntity.getUserName());
      claims.put("roles", informationEntity.getRoles());
      claims.put("companyEstablishments", informationEntity.getCompanyEstablishments());
      String token = utilsToken.doGenerateToken(claims, Constants.EXPIRATION_TOKEN_JWT);
      RedisEntity redisEntity = new RedisEntity();
      redisEntity.setKey(token);
      redisEntity.setValue(objectMapper.writeValueAsString(informationEntity));
      redisEntity.setDuration(Constants.EXPIRATION_TOKEN_REDIS);
      utilsRedis.registerKey(redisEntity);
      TokenSession tokenSession = new TokenSession();
      tokenSession.setToken(token);
      return tokenSession;
    } catch (Exception exception) {
      log.error("Exception al generar token: ", exception);
      throw new InternalErrorException("Error al generar token");
    }
  }

  @Override
  public Optional<Authentication> authenticate(HttpServletRequest request) {
    return extractBearerTokenHeader(request).flatMap(this::lookup);
  }

  private Optional<Authentication> lookup(String token) {
    try {
      String jsonUser = this.redisTemplate.opsForValue().get(token);

      // ← ADD A NULL/EMPTY CHECK
      if (jsonUser == null || jsonUser.isEmpty()) {
        log.debug("No entry in Redis for token={}", token);
        return Optional.empty();
      }

      // Use a shared, injected ObjectMapper instead of new’ing one each time
      TokenInformation info = objectMapper.readValue(jsonUser, TokenInformation.class);

      Authentication auth = createAuthentication(token, info.getUserName(), info.getRoles());
      return Optional.of(auth);
    } catch (JsonProcessingException e) {
      log.warn("Failed to parse user JSON from Redis for token={}", token, e);
      return Optional.empty();
    } catch (Exception e) {
      log.warn("Unknown error while looking up Redis token={}", token, e);
      return Optional.empty();
    }
  }

  private static Optional<String> extractBearerTokenHeader(@NonNull HttpServletRequest request) {
    try {
      String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
      if (nonNull(authorization) && authorization.startsWith(BEARER_PREFIX)) {
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (!token.isBlank()) {
          return Optional.of(token);
        }
      }
      return Optional.empty();
    } catch (Exception e) {
      log.error("An unknown error occurred while trying to extract bearer token", e);
      return Optional.empty();
    }
  }

  private static Authentication createAuthentication(
      String token, String actor, @NonNull List<String> roles) {
    List<GrantedAuthority> authorities =
        Stream.of(roles)
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .collect(Collectors.toList());
    return new UsernamePasswordAuthenticationToken(actor, token, authorities);
  }

  @Override
  public TokenSession generateTokenForCron(
      TokenInformation informationEntity, int expirationMinutes) {

    try {
      Map<String, Object> claims = new HashMap<>();
      claims.put("sub", informationEntity.getIdUser());
      claims.put(USER_NAME, informationEntity.getUserName());
      claims.put("roles", informationEntity.getRoles());
      claims.put("companyEstablishments", informationEntity.getCompanyEstablishments());
      String token = utilsToken.doGenerateToken(claims, expirationMinutes);

      // Guardar el token en Redis para que pueda ser validado por otros servicios
      RedisEntity redisEntity = new RedisEntity();
      redisEntity.setKey(token);
      redisEntity.setValue(objectMapper.writeValueAsString(informationEntity));
      // Convertir minutos a segundos para Redis (expirationMinutes * 60)
      redisEntity.setDuration(expirationMinutes * 60);
      utilsRedis.registerKey(redisEntity);

      TokenSession tokenSession = new TokenSession();
      tokenSession.setToken(token);
      return tokenSession;
    } catch (Exception exception) {
      log.error("Exception al generar token: ", exception);
      throw new InternalErrorException("Error al generar token");
    }
  }
}
