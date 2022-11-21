package com.wallparisoft.service.impl;

import com.wallparisoft.dto.RoleDto;
import com.wallparisoft.entity.Role;
import com.wallparisoft.mapper.RoleMapper;
import com.wallparisoft.repository.IRoleRepository;
import com.wallparisoft.response.RoleDtoResponse;
import com.wallparisoft.service.IRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RoleServiceImpl implements IRoleService {

    @Autowired
    private IRoleRepository roleRepository;
    @Autowired
    private RoleMapper roleMapper;


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
}
