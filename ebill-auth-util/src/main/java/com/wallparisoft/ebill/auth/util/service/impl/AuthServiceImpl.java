package com.wallparisoft.ebill.auth.util.service.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallparisoft.ebill.auth.util.constant.Constants;
import com.wallparisoft.ebill.auth.util.UtilsRedis;
import com.wallparisoft.ebill.auth.util.UtilsToken;
import com.wallparisoft.ebill.auth.util.exception.InternalErrorException;
import com.wallparisoft.ebill.auth.util.exception.TokenException;
import com.wallparisoft.ebill.auth.util.model.RedisEntity;
import com.wallparisoft.ebill.auth.util.model.TokenInformation;
import com.wallparisoft.ebill.auth.util.model.TokenSession;
import com.wallparisoft.ebill.auth.util.service.IAuthService;

import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.nonNull;

@Component
@Log4j2
public class AuthServiceImpl implements IAuthService {

    private static final String USER_NAME = "userName";
    private static final String BEARER_PREFIX = "Bearer ";

    private RedisTemplate<String, String> redisTemplate;
    private final UtilsToken utilsToken;
    private final UtilsRedis utilsRedis;
    private UtilsRedis redis;
    private Environment environment;
    ObjectMapper objectMapper = new ObjectMapper().configure(
            DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public AuthServiceImpl(RedisTemplate<String, String> redisTemplate, UtilsToken utilsToken, UtilsRedis utilsRedis) {
        this.redisTemplate = redisTemplate;
        this.utilsToken = utilsToken;
        this.utilsRedis = utilsRedis;
    }

    @Override
    public TokenSession generateToken(TokenInformation informationEntity, boolean verified) {
        try {
            Map<String, Object> claims = new HashMap<>();
            claims.put(USER_NAME, informationEntity.getUserName());
            claims.put("roles", informationEntity.getRoles());
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
            throw new InternalErrorException("Error al generar token");
        }

    }

    @Override
    public void validateToken(TokenInformation informationEntity) {
        log.info("Token state user name ({}), state: {}", informationEntity.getUserName(), 1);
        Map<String, Object> claims = new HashMap<>();
        claims.put(USER_NAME, informationEntity.getUserName());
        String token = utilsToken.doGenerateToken(claims, Constants.EXPIRATION_TOKEN_JWT);
        TokenSession tokenSession = new TokenSession();
        tokenSession.setToken(token);
        throw new TokenException(tokenSession, 1);
    }


    @Override
    public Optional<Authentication> authenticate(HttpServletRequest request) {
        return extractBearerTokenHeader(request).flatMap(this::lookup);
    }

    private Optional<Authentication> lookup(String token) {
        try {
            String jsonUser = this.redisTemplate.opsForValue().get(token);
            ObjectMapper objectMapper = new ObjectMapper();
            TokenInformation tokenInformation = objectMapper.readValue(jsonUser, TokenInformation.class);
            if (nonNull(tokenInformation)) {
                Authentication authentication = createAuthentication(tokenInformation.getUserName(), tokenInformation.getRoles());
                return Optional.of(authentication);
            }
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Unknown error while trying to look up Redis token", e);
            return Optional.empty();
        }
    }

    private static Optional<String> extractBearerTokenHeader(@NonNull HttpServletRequest request) {
        try {
            String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (nonNull(authorization)) {
                if (authorization.startsWith(BEARER_PREFIX)) {
                    String token = authorization.substring(BEARER_PREFIX.length()).trim();
                    if (!token.isBlank()) {
                        return Optional.of(token);
                    }
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("An unknown error occurred while trying to extract bearer token", e);
            return Optional.empty();
        }
    }

    private static Authentication createAuthentication(String actor, @NonNull List<String> roles) {
        List<GrantedAuthority> authorities = Stream.of(roles)
                .distinct()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
        return new UsernamePasswordAuthenticationToken(nonNull(actor) ? actor : "N/A", "N/A", authorities);
    }

}
