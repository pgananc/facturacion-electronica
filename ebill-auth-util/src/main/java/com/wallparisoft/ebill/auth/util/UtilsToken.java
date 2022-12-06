package com.wallparisoft.ebill.auth.util;

import com.wallparisoft.ebill.auth.util.constant.Constants;
import io.jsonwebtoken.*;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;


@Service
@Log4j2
public class UtilsToken {
  Key hmacKey = new SecretKeySpec(Base64.getDecoder().decode(Constants.SECRET_PASSWORD),
          SignatureAlgorithm.HS256.getJcaName());
  public String doGenerateToken(Map<String, Object> claims, int expiration) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(new Date());
    calendar.add(Calendar.MINUTE,expiration);
    return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(new Date(System.currentTimeMillis()))
            .setExpiration(calendar.getTime())
            .signWith(SignatureAlgorithm.HS256, hmacKey)
            .compact();
  }

  public boolean checkTokenValidity(String token) {
    try {
      Jwts.parser()
              .setSigningKey(hmacKey)
              .parseClaimsJws(token);

      return true;
    } catch (SignatureException | UnsupportedJwtException | ExpiredJwtException | MalformedJwtException | IllegalArgumentException exception) {
      return false;
    }
  }

  public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = getAllClaimsFromToken(token);
    log.info("Claims response cell: {}, value: {}, token: {}", claims, token);
    return claimsResolver.apply(claims);
  }

  private Claims getAllClaimsFromToken(String token) {
    return Jwts.parser()
            .parseClaimsJws(token)
        .getBody();
  }
}
