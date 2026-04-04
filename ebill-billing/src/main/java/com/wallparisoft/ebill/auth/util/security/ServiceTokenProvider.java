package com.wallparisoft.ebill.auth.util.security;

import com.wallparisoft.ebill.auth.util.config.ServiceAuthProperties;
import com.wallparisoft.ebill.auth.util.model.LoginTokenResponse;
import com.wallparisoft.ebill.auth.util.model.OAuthTokenResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class ServiceTokenProvider {

  private static final String GRANT_TYPE = "client_credentials";
  private static final Duration SAFETY_WINDOW = Duration.ofSeconds(60);

  private final ServiceAuthProperties properties;
  private final RestTemplate restTemplate;
  private final ReentrantLock lock = new ReentrantLock();

  private volatile TokenHolder tokenHolder;

  public ServiceTokenProvider(
      ServiceAuthProperties properties, RestTemplateBuilder restTemplateBuilder) {
    this.properties = properties;
    this.restTemplate =
        restTemplateBuilder
            .setConnectTimeout(Duration.ofSeconds(10))
            .setReadTimeout(Duration.ofSeconds(10))
            .build();
  }

  public String getToken() {
    if (!properties.isFullyConfigured() && !properties.hasLoginCredentials()) {
      throw new IllegalStateException(
          "Service token provider is not configured. Set either token or login properties.");
    }

    TokenHolder current = tokenHolder;
    if (current != null && !current.isExpired()) {
      return current.token();
    }

    return refreshToken();
  }

  private String refreshToken() {
    lock.lock();
    try {
      TokenHolder current = tokenHolder;
      if (current != null && !current.isExpired()) {
        return current.token();
      }

      if (properties.isFullyConfigured()) {
        try {
          OAuthTokenResponse response = requestToken();
          String accessToken = response != null ? response.getAccessToken() : null;
          if (!StringUtils.hasText(accessToken)) {
            throw new IllegalStateException("Token service returned an empty access token");
          }
          tokenHolder =
              new TokenHolder(
                  accessToken, calculateExpiry(resolveExpirySeconds(response.getExpiresIn())));
          log.debug("Obtained new service token via client credentials");
          return accessToken;
        } catch (RuntimeException ex) {
          log.warn("Client credentials flow failed: {}", ex.getMessage());
          if (!properties.hasLoginCredentials()) {
            throw ex;
          }
        }
      }

      if (properties.hasLoginCredentials()) {
        LoginTokenResponse response = requestTokenViaLogin();
        String accessToken = response != null ? response.extractToken() : null;
        if (!StringUtils.hasText(accessToken)) {
          throw new IllegalStateException("Login response did not contain a token");
        }
        tokenHolder =
            new TokenHolder(
                accessToken, calculateExpiry(resolveExpirySeconds(response.getExpiresIn())));
        log.debug("Obtained new service token via login");
        return accessToken;
      }

      throw new IllegalStateException(
          "Service token provider could not determine a valid authentication strategy.");
    } finally {
      lock.unlock();
    }
  }

  private boolean hasValidToken() {
    TokenHolder current = tokenHolder;
    return current != null && !current.isExpired();
  }

  private OAuthTokenResponse requestToken() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", GRANT_TYPE);
    form.add("client_id", properties.getClientId());
    form.add("client_secret", properties.getClientSecret());
    if (StringUtils.hasText(properties.getScope())) {
      form.add("scope", properties.getScope());
    }

    HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);

    try {
      return restTemplate.postForObject(
          Objects.requireNonNull(properties.getTokenUrl()), entity, OAuthTokenResponse.class);
    } catch (RestClientResponseException ex) {
      log.error(
          "Failed to obtain service token. Status: {}, Response: {}",
          ex.getRawStatusCode(),
          ex.getResponseBodyAsString());
      throw ex;
    }
  }

  private LoginTokenResponse requestTokenViaLogin() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    Map<String, String> payload = new HashMap<>();
    payload.put("userName", properties.getUsername());
    payload.put("password", properties.getPassword());

    HttpEntity<Map<String, String>> entity = new HttpEntity<>(payload, headers);
    try {
      return restTemplate.postForObject(properties.getLoginUrl(), entity, LoginTokenResponse.class);
    } catch (RestClientResponseException ex) {
      log.error(
          "Failed to obtain service token via login. Status: {}, Response: {}",
          ex.getRawStatusCode(),
          ex.getResponseBodyAsString());
      throw ex;
    }
  }

  private Instant calculateExpiry(long ttlSeconds) {
    Instant expiry = Instant.now().plusSeconds(ttlSeconds);
    if (ttlSeconds > SAFETY_WINDOW.getSeconds()) {
      expiry = expiry.minus(SAFETY_WINDOW);
    }
    return expiry;
  }

  private long resolveExpirySeconds(Long expiresIn) {
    if (expiresIn != null && expiresIn > 0) {
      return expiresIn;
    }
    Duration fallback = properties.getFallbackTtl();
    return (fallback != null ? fallback : Duration.ofHours(2)).getSeconds();
  }

  private record TokenHolder(String token, Instant expiresAt) {
    private boolean isExpired() {
      return expiresAt == null || Instant.now().isAfter(expiresAt);
    }
  }
}
