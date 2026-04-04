package com.wallparisoft.ebill.auth.util;

import com.wallparisoft.ebill.auth.util.constant.Constants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UtilsToken {
  private final Key hmacKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(Constants.SECRET_PASSWORD));

  public String doGenerateToken(Map<String, Object> claims, int expirationMinutes) {
    log.info("→ Generando JWT. Claims={}, exp en {} min", claims, expirationMinutes);
    try {
      Date now = new Date();
      Date expiry = Date.from(now.toInstant().plus(expirationMinutes, ChronoUnit.MINUTES));

      String token =
          Jwts.builder()
              .setClaims(claims)
              .setIssuedAt(now)
              .setExpiration(expiry)
              .signWith(hmacKey, SignatureAlgorithm.HS256) // <-- sobrecarga correcta
              .compact();

      log.info("← JWT generado ({} chars)", token.length());
      log.debug("Token completo: {}", token);
      return token;
    } catch (JwtException e) {
      log.error("Error generando JWT", e);
      throw e;
    }
  }

  private Claims getAllClaimsFromToken(String token) {
    return Jwts.parser() // ← here, not parserBuilder()
        .setSigningKey(hmacKey)
        .build()
        .parseClaimsJws(token)
        .getBody();
  }

  public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = getAllClaimsFromToken(token);
    log.info("Claims response cell: {}, value: {}, token: {}", claims, token);
    return claimsResolver.apply(claims);
  }
}
