package com.wallparisoft.service;

import com.wallparisoft.dto.UserDto;
import com.wallparisoft.entity.User;
import com.wallparisoft.response.UserDtoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;



public interface IUserService extends ICRUD<User> {

    void delete(Long id);

    Page<UserDto> findUserByUserNameOrNameOrStatus(String name, String userName,
                                                   Boolean status, Pageable pageable);

    UserDtoResponse findUsersActive();

    UserDtoResponse findUserWithRolById( Long idUser);

    void saveUserAndRole(UserDto userDto);

    void update(UserDto userDto, Long idUser);

    Boolean existsByUserName(String userName);
}
