package com.wallparisoft.ebill.auth.util.service;

import com.wallparisoft.ebill.auth.util.model.TokenInformation;
import com.wallparisoft.ebill.auth.util.model.TokenSession;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.security.core.Authentication;

public interface IAuthService {
  TokenSession generateToken(TokenInformation tokenInformation, boolean verified);

  Optional<Authentication> authenticate(HttpServletRequest request);

  TokenSession generateTokenForCron(TokenInformation tokenInformation, int expirationMinutes);
}
