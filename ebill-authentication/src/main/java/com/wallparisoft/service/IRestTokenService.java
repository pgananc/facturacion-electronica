package com.wallparisoft.service;

import com.wallparisoft.dto.UserDto;
import com.wallparisoft.entity.ResetToken;
import com.wallparisoft.entity.User;
import com.wallparisoft.response.UserDtoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface IRestTokenService extends ICRUD<ResetToken> {

    boolean validateActiveToken(String token);
    ResetToken findByToken(String token);
}
