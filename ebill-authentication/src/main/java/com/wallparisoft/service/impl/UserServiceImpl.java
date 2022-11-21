package com.wallparisoft.service.impl;

import com.wallparisoft.dto.RoleDto;
import com.wallparisoft.dto.UserDto;
import com.wallparisoft.ebill.utils.exception.ModelNotFoundException;
import com.wallparisoft.entity.Role;
import com.wallparisoft.entity.User;
import com.wallparisoft.entity.UserRole;
import com.wallparisoft.mapper.RoleMapper;
import com.wallparisoft.mapper.UserMapper;
import com.wallparisoft.repository.IRoleRepository;
import com.wallparisoft.repository.IUserRepository;
import com.wallparisoft.repository.IUserRoleRepository;
import com.wallparisoft.response.UserDtoResponse;
import com.wallparisoft.service.IUserService;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements IUserService {

    private final IUserRepository userRepository;

    private final IRoleRepository roleRepository;
    private final IUserRoleRepository userRoleRepository;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;

    public UserServiceImpl(IUserRepository userRepository, IRoleRepository roleRepository, IUserRoleRepository userRoleRepository, UserMapper userMapper, RoleMapper roleMapper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
    }

    @Override
    public User save(User entity) {
        return userRepository.save(entity);
    }


    @Override
    public List<User> findAll() {
        return userRepository.findAll();
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
                findUserByUserNameOrNameOrStatus("%" + name.toUpperCase() + "%", "%" + userName.toUpperCase() + "%", status, pageable);
        List<UserDto> userDtos = new ArrayList<>();
        users.getContent().parallelStream().forEach(user -> {
            UserDto userDto = userMapper.convertUserToUserDto(user);
            userDtos.add(userDto);
        });
        return new PageImpl<>(userDtos, pageable, users.getTotalElements());
    }

    @Override
    public UserDtoResponse findUsersActive() {
        List<UserDto> userDtoList = new ArrayList<>();
        List<User> userList = this.userRepository.findUsersActive();
        userList.parallelStream().forEach(user -> {
            UserDto clientDto = userMapper.convertUserToUserDto(user);
            userDtoList.add(clientDto);
        });
        return UserDtoResponse.builder()
                .status(HttpStatus.OK.getReasonPhrase())
                .userDtos(userDtoList)
                .build();
    }

    @Override
    public UserDtoResponse findUserWithRolById(Long idUser) {
        List<UserDto> userDtoList = new ArrayList<>();
        User user = findById(idUser);
        UserDto userDto = userMapper.convertUserToUserDto(user);
        List<Role> roles = roleRepository.findRoleActiveByIdUser(user.getIdUser());
        List<RoleDto> roleDtos = roleMapper.convertRoleListToRoleDtoList(roles);
        userDto.setRoleDtos(roleDtos);
        userDtoList.add(userDto);

        return UserDtoResponse.builder()
                .status(HttpStatus.OK.getReasonPhrase())
                .userDtos(userDtoList)
                .build();
    }

    @Transactional
    @Override
    public void saveUserAndRole(UserDto userDto) {
        User user = userMapper.convertUserDtoToUser(userDto);
        List<Role> roles = roleMapper.convertRoleDtoListToRoleList(userDto.getRoleDtos());
        User userSave = save(user);
        roles.forEach(role -> {
            UserRole userRole = UserRole.builder()
                    .user(userSave)
                    .role(role)
                    .status(userSave.getStatus()).build();
            userRoleRepository.save(userRole);

        });
    }

    @Transactional
    @Override
    public void update(UserDto userDto, Long idUser) {
        User userSave = findById(idUser);
        User user = userMapper.convertUserDtoToUser(userDto);
        List<Role> roles = roleMapper.convertRoleDtoListToRoleList(userDto.getRoleDtos());
        user.setCreationDate(userSave.getCreationDate());
        user.setUpdateDate(LocalDateTime.now());
        save(user);
        roles.forEach(role -> {
            Optional<UserRole> userRoleOptional = userRoleRepository.findByIdUserAndIdRole(user.getIdUser(), role.getIdRole());
            if (!userRoleOptional.isPresent()) {
                userRoleRepository.deleteUserRoleByUser(user);
                UserRole userRole = UserRole.builder()
                        .user(userSave)
                        .role(role)
                        .status(user.getStatus()).build();
                userRoleRepository.save(userRole);
            }
        });
    }

    @Override
    public Boolean existsByUserName(String userName) {
        return userRepository.existsByUserName(userName);
    }
}
