package com.wallparisoft.service;

import com.wallparisoft.dto.UserDto;
import com.wallparisoft.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IUserService extends ICRUD<User> {

    void delete(Long id);

    Page<UserDto> findUserByUserNameOrNameOrStatus(String name, String userName,
                                                   Boolean status, Pageable pageable);

    List<UserDto> findUsersActive();

    List<UserDto> findUserWithRolById( Long idUser);
}
