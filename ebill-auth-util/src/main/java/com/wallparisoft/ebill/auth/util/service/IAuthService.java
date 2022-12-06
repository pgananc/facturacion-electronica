package com.wallparisoft.ebill.auth.util.service;


import com.wallparisoft.ebill.auth.util.model.TokenInformation;
import com.wallparisoft.ebill.auth.util.model.TokenSession;
import org.springframework.security.core.Authentication;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;


public interface IAuthService {
    TokenSession generateToken(TokenInformation tokenInformation, boolean verified);
    void validateToken(TokenInformation tokenInformation);
    public Optional<Authentication> authenticate(HttpServletRequest request);
}
