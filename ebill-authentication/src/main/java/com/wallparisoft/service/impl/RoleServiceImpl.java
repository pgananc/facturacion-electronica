package com.wallparisoft.service.impl;

import com.wallparisoft.dto.RoleDto;
import com.wallparisoft.entity.Role;
import com.wallparisoft.mapper.RoleMapper;
import com.wallparisoft.repository.IRoleRepository;
import com.wallparisoft.repository.IUserRoleRepository;
import com.wallparisoft.response.RoleDtoResponse;
import com.wallparisoft.service.IRoleService;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoleServiceImpl implements IRoleService {


    private final IRoleRepository roleRepository;
    private final IUserRoleRepository userRoleRepository;
    private final RoleMapper roleMapper;

    public RoleServiceImpl(IRoleRepository roleRepository, IUserRoleRepository userRoleRepository, RoleMapper roleMapper) {
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleMapper = roleMapper;
    }


    @Override
    public Role save(Role entity) {
        return roleRepository.save(entity);
    }


    @Override
    public List<Role> findAll() {
        return roleRepository.findAll();
    }

    @Override
    public Role findById(Long id) {
        return null;
    }

    @Override
    public RoleDtoResponse findRoleActive() {
        List<RoleDto> roleDtos = new ArrayList<>();
        List<Role> clients = this.roleRepository.findRoleActive();
        clients.parallelStream().forEach(role -> {
            RoleDto clientDto = roleMapper.convertRoleToRoleDto(role);
            roleDtos.add(clientDto);
        });
        return RoleDtoResponse.builder()
                .status(HttpStatus.OK.getReasonPhrase())
                .roleDtos(roleDtos)
                .build();

    }

    @Override
    public List<Role> findByUserNameAndStatus(String userName, Boolean status) {
        return  userRoleRepository.findByUser_UserNameAndStatus(userName,status).stream().map(userRole->{
            return userRole.getRole();
         }).collect(Collectors.toList());
    }
}
