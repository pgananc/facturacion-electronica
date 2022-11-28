package com.wallparisoft.service.impl;

import com.wallparisoft.entity.Token;
import com.wallparisoft.repository.ITokenRepository;
import com.wallparisoft.service.ITokenService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

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
        Token resetToken= findByToken(token);
       return Objects.nonNull(resetToken)?resetToken.isExpired():false;
    }

    @Override
    public Token findByToken(String token) {
        return tokenRepository.findByToken(token);
    }
}
