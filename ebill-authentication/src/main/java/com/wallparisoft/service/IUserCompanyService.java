package com.wallparisoft.service;

import com.wallparisoft.dto.UserCompanyDto;
import com.wallparisoft.dto.UserDto;
import com.wallparisoft.entity.UserCompany;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IUserCompanyService extends ICRUD<UserCompany> {
    UserCompanyDto findByIdCompanyAndIdUser(Long idCompany, Long idUser);

    void delete(Long idCompany, Long idClient);

    UserCompany saveUserCompany(Long idCompany, Long idClient);

    Page<UserDto> getUsersFromACompanyPageable(Long idCompany, Pageable pageable);

    UserCompanyDto getUsersFromACompany(Long idCompany);
}
