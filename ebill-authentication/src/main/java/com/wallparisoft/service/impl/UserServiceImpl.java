package com.wallparisoft.service.impl;

import com.wallparisoft.dto.RoleDto;
import com.wallparisoft.dto.UserDto;
import com.wallparisoft.ebill.utils.exception.ModelNotFoundException;
import com.wallparisoft.entity.Role;
import com.wallparisoft.entity.User;
import com.wallparisoft.mapper.RoleMapper;
import com.wallparisoft.mapper.UserMapper;
import com.wallparisoft.repository.IRoleRepository;
import com.wallparisoft.repository.IUserRepository;
import com.wallparisoft.service.IUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserServiceImpl implements IUserService {

    private final IUserRepository userRepository;

    private final IRoleRepository roleRepository;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;

    public UserServiceImpl(IUserRepository userRepository, IRoleRepository roleRepository, UserMapper userMapper, RoleMapper roleMapper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
    }

    @Override
    public User save(User entity) {
        return null;
    }

    @Override
    public User update(User entity, Long id) {
        return null;
    }

    @Override
    public List<User> findAll() {
        return null;
    }

    @Override
    public User findById(Long id) {
        return this.userRepository.findById(id)
                .orElseThrow(() -> new ModelNotFoundException("User not found for this id :: " + id));
    }

    @Override
    public void delete(Long id) {
        User user = findById(id);
        user.setStatus(false);
        userRepository.save(user);
    }

    @Override
    public Page<UserDto> findUserByUserNameOrNameOrStatus(String name, String userName, Boolean status, Pageable pageable) {
        Page<User> users = userRepository.
                findUserByUserNameOrNameOrStatus("%"+name.toUpperCase()+"%", "%"+userName.toUpperCase()+"%", status, pageable);
        List<UserDto> userDtos = new ArrayList<>();
        users.getContent().parallelStream().forEach(user -> {
            UserDto userDto = userMapper.convertUserToUserDto(user);
            userDtos.add(userDto);
        });
        return new PageImpl<>(userDtos, pageable, users.getTotalElements());
    }

    @Override
    public List<UserDto> findUsersActive() {
        List<UserDto> userDtoList = new ArrayList<>();
        List<User> userList = this.userRepository.findUsersActive();
        userList.parallelStream().forEach(user -> {
            UserDto clientDto = userMapper.convertUserToUserDto(user);
            userDtoList.add(clientDto);
        });
        return userDtoList;
    }

    @Override
    public List<UserDto> findUserWithRolById(Long idUser) {
        List<UserDto> userDtoList = new ArrayList<>();
        List<User> clients = this.userRepository.findUserById(idUser);
        clients.parallelStream().forEach(user -> {
            UserDto userDto = userMapper.convertUserToUserDto(user);
            List<Role> contacts = roleRepository.findRoleActiveByIdUser(user.getIdUser());
            List<RoleDto> roleDtos = roleMapper.convertRoleListToRoleDtoList(contacts);
            userDto.setRoleDtos(roleDtos);
            userDtoList.add(userDto);
        });
        return userDtoList;
    }
}
