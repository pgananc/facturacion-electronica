package com.wallparisoft.ebill.auth.util.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LoginTokenResponse {

  private int code;
  private String status;
  private String message;

  @JsonProperty("token")
  private TokenSession tokenSession;

  @JsonProperty("expires_in")
  private Long expiresIn;

  public int getCode() {
    return code;
  }

  public void setCode(int code) {
    this.code = code;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public TokenSession getTokenSession() {
    return tokenSession;
  }

  public void setTokenSession(TokenSession tokenSession) {
    this.tokenSession = tokenSession;
  }

  public Long getExpiresIn() {
    return expiresIn;
  }

  public void setExpiresIn(Long expiresIn) {
    this.expiresIn = expiresIn;
  }

  public String extractToken() {
    return tokenSession != null ? tokenSession.getToken() : null;
  }
}
