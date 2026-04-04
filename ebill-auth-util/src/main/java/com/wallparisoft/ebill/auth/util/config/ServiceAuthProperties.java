package com.wallparisoft.ebill.auth.util.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@ConfigurationProperties(prefix = "service.auth")
public class ServiceAuthProperties {

  private String tokenUrl;
  private String clientId;
  private String clientSecret;
  private String scope;
  private Duration fallbackTtl = Duration.ofHours(2);
  private String loginUrl;
  private String username;
  private String password;

  public String getTokenUrl() {
    return tokenUrl;
  }

  public void setTokenUrl(String tokenUrl) {
    this.tokenUrl = tokenUrl;
  }

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public String getClientSecret() {
    return clientSecret;
  }

  public void setClientSecret(String clientSecret) {
    this.clientSecret = clientSecret;
  }

  public String getScope() {
    return scope;
  }

  public void setScope(String scope) {
    this.scope = scope;
  }

  public Duration getFallbackTtl() {
    return fallbackTtl;
  }

  public void setFallbackTtl(Duration fallbackTtl) {
    this.fallbackTtl = fallbackTtl;
  }

  public boolean isFullyConfigured() {
    return StringUtils.hasText(tokenUrl)
        && StringUtils.hasText(clientId)
        && StringUtils.hasText(clientSecret);
  }

  public String getLoginUrl() {
    return loginUrl;
  }

  public void setLoginUrl(String loginUrl) {
    this.loginUrl = loginUrl;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public boolean hasLoginCredentials() {
    return StringUtils.hasText(loginUrl)
        && StringUtils.hasText(username)
        && StringUtils.hasText(password);
  }
}
