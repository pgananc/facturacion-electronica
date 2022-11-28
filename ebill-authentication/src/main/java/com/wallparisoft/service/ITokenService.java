package com.wallparisoft.service;

import com.wallparisoft.entity.Token;


public interface ITokenService extends ICRUD<Token> {

    boolean validateActiveToken(String token);
    Token findByToken(String token);
}
