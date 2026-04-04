package com.wallparisoft.authentication.service;

import com.wallparisoft.authentication.entity.Token;

public interface ITokenService extends ICRUD<Token> {

  boolean validateActiveToken(String token);

  Token findByToken(String token);
}
