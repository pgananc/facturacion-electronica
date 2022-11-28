package com.wallparisoft.service.impl;

import com.wallparisoft.dto.RoleDto;
import com.wallparisoft.dto.UserDto;
import com.wallparisoft.ebill.utils.exception.ModelNotFoundException;
import com.wallparisoft.entity.ResetToken;
import com.wallparisoft.entity.Role;
import com.wallparisoft.entity.User;
import com.wallparisoft.entity.UserRole;
import com.wallparisoft.repository.IRestTokenRepository;
import com.wallparisoft.response.UserDtoResponse;
import com.wallparisoft.service.IRestTokenService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class RestTokenServiceImpl implements IRestTokenService {

    private final IRestTokenRepository restTokenRepository;

    public RestTokenServiceImpl(IRestTokenRepository restTokenRepository) {
        this.restTokenRepository = restTokenRepository;
    }


    @Override
    public ResetToken save(ResetToken entity) {
        return restTokenRepository.save(entity);
    }

    @Override
    public List<ResetToken> findAll() {
        return restTokenRepository.findAll();
    }

    @Override
    public ResetToken findById(Long id) {
        return null;
    }

    @Override
    public boolean validateActiveToken(String token) {
        ResetToken resetToken= findByToken(token);
        if(Objects.isNull(resetToken)){
            return false;
        }else{
            return resetToken.isExpired();
        }
    }

    @Override
    public ResetToken findByToken(String token) {
        return restTokenRepository.findByToken(token);
    }
}
