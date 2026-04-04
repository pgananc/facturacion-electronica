package com.wallparisoft.authentication.service.impl;

import com.wallparisoft.authentication.dto.RoleDto;
import com.wallparisoft.authentication.dto.UserCompanyDto;
import com.wallparisoft.authentication.dto.UserDto;
import com.wallparisoft.authentication.entity.Role;
import com.wallparisoft.authentication.entity.User;
import com.wallparisoft.authentication.entity.UserCompany;
import com.wallparisoft.authentication.mapper.RoleMapper;
import com.wallparisoft.authentication.mapper.UserCompanyMapper;
import com.wallparisoft.authentication.mapper.UserMapper;
import com.wallparisoft.authentication.repository.IRoleRepository;
import com.wallparisoft.authentication.repository.IUserCompanyRepo;
import com.wallparisoft.authentication.response.UserDtoResponse;
import com.wallparisoft.authentication.service.IUserCompanyService;
import com.wallparisoft.authentication.service.IUserService;
import com.wallparisoft.ebill.utils.exception.ModelNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UserCompanyServiceImpl implements IUserCompanyService {

  private final IUserCompanyRepo userCompanyRepo;
  private final IUserService userService;
  private final UserCompanyMapper userCompanyMapper;
  private final IRoleRepository roleRepository;
  private final UserMapper userMapper;
  private final RoleMapper roleMapper;

  @Override
  public UserCompany save(UserCompany entity) {
    return userCompanyRepo.save(entity);
  }

  @Override
  public void delete(Long idCompany, Long idUser) {
    UserCompany userCompany =
        userCompanyRepo
            .findByIdCompanyAndUser_IdUser(idCompany, idUser)
            .orElseThrow(
                () ->
                    new ModelNotFoundException(
                        "Company Client not found for this id company :: "
                            + idCompany
                            + ", "
                            + "and id client :: "
                            + idUser));
    userCompanyRepo.delete(userCompany);
  }

  @Override
  public UserCompany saveUserCompany(Long idCompany, Long idUser) {
    User user = userService.findById(idUser);
    try {
      findByIdCompanyAndIdUser(idCompany, idUser);
      return null;
    } catch (ModelNotFoundException me) {
      UserCompany userCompany = UserCompany.builder().idCompany(idCompany).user(user).build();
      return save(userCompany);
    }
  }

  @Override
  public Page<UserDto> getUsersFromACompanyPageable(Long idCompany, Pageable pageable) {
    Page<UserCompany> userCompanies = userCompanyRepo.findByIdCompany(idCompany, pageable);
    if (userCompanies == null) {
      throw new ModelNotFoundException(
          "User Company not found for this company id :: " + idCompany);
    }
    UserCompanyDto userCompanyDto =
        userCompanyMapper.getUsersFromCompany(userCompanies.getContent());

    return new PageImpl<>(
        userCompanyDto.getUserDtos(), pageable, userCompanyDto.getUserDtos().size());
  }

  @Override
  public UserCompanyDto getUsersFromACompany(Long idCompany) {
    Optional<List<UserCompany>> userCompaniesOpt = userCompanyRepo.findByIdCompany(idCompany);
    if (userCompaniesOpt.isPresent()) {
      return userCompanyMapper.getUsersFromCompany(userCompaniesOpt.get());
    }
    throw new ModelNotFoundException("User Company not found for this company id :: " + idCompany);
  }

  @Override
  public List<UserCompany> findAll() {
    return userCompanyRepo.findAll();
  }

  @Override
  public UserCompany findById(Long id) {
    return this.userCompanyRepo
        .findById(id)
        .orElseThrow(
            () -> new ModelNotFoundException("User Company not found for this id :: " + id));
  }

  @Override
  public UserCompanyDto findByIdCompanyAndIdUser(Long idCompany, Long idUser) {
    UserCompany userCompany =
        userCompanyRepo
            .findByIdCompanyAndUser_IdUser(idCompany, idUser)
            .orElseThrow(
                () ->
                    new ModelNotFoundException(
                        "User Company not found for this id company :: "
                            + idCompany
                            + ", "
                            + "and id client :: "
                            + idUser));
    return userCompanyMapper.convertUserCompanyToUserCompanyDto(userCompany);
  }

  @Override
  public UserDtoResponse findUserWithRolesByUserIdAndCompanyId(Long idUser, Long idCompany) {
    List<UserDto> userDtoList = new ArrayList<>();
    User user = userService.findById(idUser);

    // Verificar que el usuario pertenezca a la compañía
    userCompanyRepo
        .findByIdCompanyAndUser_IdUser(idCompany, idUser)
        .orElseThrow(
            () ->
                new ModelNotFoundException(
                    "Usuario no encontrado en la compañía especificada"));

    UserDto userDto = userMapper.convertUserToUserDto(user);
    // Buscar roles solo de la compañía especificada
    List<Role> roles =
        roleRepository.findActiveRolesByUserIdAndCompanyId(idUser, idCompany);
    List<RoleDto> roleDtos = roleMapper.convertRoleListToRoleDtoList(roles);
    userDto.setRoleDtos(roleDtos);
    userDtoList.add(userDto);

    return UserDtoResponse.builder()
        .status(HttpStatus.OK.getReasonPhrase())
        .userDtos(userDtoList)
        .build();
  }
}
