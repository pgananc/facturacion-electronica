package com.wallparisoft.authentication.service.impl;

import com.wallparisoft.authentication.entity.Token;
import com.wallparisoft.authentication.repository.ITokenRepository;
import com.wallparisoft.authentication.service.ITokenService;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class TokenServiceImpl implements ITokenService {

  private final ITokenRepository tokenRepository;

  public TokenServiceImpl(ITokenRepository tokenRepository) {
    this.tokenRepository = tokenRepository;
  }

  @Override
  public Token save(Token entity) {
    return tokenRepository.save(entity);
  }

  @Override
  public List<Token> findAll() {
    return tokenRepository.findAll();
  }

  @Override
  public Token findById(Long id) {
    return null;
  }

  @Override
  public boolean validateActiveToken(String token) {
    Token resetToken = findByToken(token);
    return Objects.nonNull(resetToken) ? resetToken.isExpired() : false;
  }

  @Override
  public Token findByToken(String token) {
    return tokenRepository.findByToken(token);
  }
}
