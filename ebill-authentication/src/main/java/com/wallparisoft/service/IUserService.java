package com.wallparisoft.service;

import com.wallparisoft.dto.UserDto;
import com.wallparisoft.entity.User;
import com.wallparisoft.response.UserDtoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface IUserService extends ICRUD<User> {

    void delete(Long id);

    Page<UserDto> findUserByIdCompanyUserNameOrNameOrStatus(Long idCompany, String name, String userName,
                                                   Boolean status, Pageable pageable);

    UserDtoResponse findUsersByCompanyAndStatusActive(Long idCompany);

    UserDtoResponse findUserWithRolById( Long idUser);

    void saveUserAndRole(UserDto userDto);

    void update(UserDto userDto, Long idUser);

    Boolean existsByIdCompanyAndUserUserName(Long idCompany, String userName);

    User findByMailAndStatus(String userName);

    Integer sendMailUser(String mail);

    boolean resetPassword(String token, String password);

    void changePassword(String password, Long idUser) throws Exception;

    UserDtoResponse findActiveUsersAndNotInCompany(Long idCompany);
}
