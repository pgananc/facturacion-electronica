package com.wallparisoft.response;

import com.wallparisoft.ebill.auth.util.model.TokenSession;
import com.wallparisoft.ebill.utils.response.BasicResponse;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
public class TokenResponse extends BasicResponse {
    private TokenSession token;
}
