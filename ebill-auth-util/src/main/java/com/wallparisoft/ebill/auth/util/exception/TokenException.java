package com.wallparisoft.ebill.auth.util.exception;

import com.wallparisoft.ebill.auth.util.model.TokenSession;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class TokenException extends RuntimeException {

    @Getter
    final TokenSession token;
    @Getter
    final int status;

    public TokenException(TokenSession token, int status) {
        this.token = token;
        this.status = status;
    }

    public TokenException(TokenSession token) {
        this.token = token;
        this.status = 200;
    }
}
