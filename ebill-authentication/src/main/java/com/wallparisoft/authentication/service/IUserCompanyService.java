package com.wallparisoft.authentication.service;

import com.wallparisoft.authentication.dto.UserCompanyDto;
import com.wallparisoft.authentication.dto.UserDto;
import com.wallparisoft.authentication.entity.UserCompany;
import com.wallparisoft.authentication.response.UserDtoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IUserCompanyService extends ICRUD<UserCompany> {
  UserCompanyDto findByIdCompanyAndIdUser(Long idCompany, Long idUser);

  void delete(Long idCompany, Long idClient);

  UserCompany saveUserCompany(Long idCompany, Long idClient);

  Page<UserDto> getUsersFromACompanyPageable(Long idCompany, Pageable pageable);

  UserCompanyDto getUsersFromACompany(Long idCompany);

  UserDtoResponse findUserWithRolesByUserIdAndCompanyId(Long idUser, Long idCompany);
}
