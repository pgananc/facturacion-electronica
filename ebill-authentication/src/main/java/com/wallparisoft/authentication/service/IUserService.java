package com.wallparisoft.authentication.service;

import com.wallparisoft.authentication.dto.RoleDto;
import com.wallparisoft.authentication.dto.UserDto;
import com.wallparisoft.authentication.dto.UserRequestDto;
import com.wallparisoft.authentication.entity.User;
import com.wallparisoft.authentication.entity.UserCompany;
import com.wallparisoft.authentication.response.UserDtoResponse;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IUserService extends ICRUD<User> {
  void delete(Long id);

  Page<UserDto> findUserByIdCompanyUserNameOrNameOrStatus(
      Long idCompany, String name, String userName, Boolean status, Pageable pageable);

  UserDtoResponse findUsersByCompanyAndStatusActive(Long idCompany);


  void registerUserWithRoles(UserRequestDto userRequestDto);

  void update(UserDto userDto, Long idUser);

  Boolean existsByUserName(String userName);

  User findByMailAndStatus(String userName);

  Integer sendMailUser(String mail, String clientIp);

  boolean resetPassword(String token, String password);

  void changePassword(String password, Long idUser) throws Exception;

  UserDtoResponse findActiveUsersAndNotInCompany(Long idCompany);

  Boolean existsByMail(String mail);

  UserDtoResponse findUserByUserNameAndPassword(String userName, String password);

  UserDtoResponse findUserByUserName(String userName);

  List<RoleDto> findRoleByUserCompany(UserCompany userCompany);
}
