package com.wallparisoft.service.impl;

import com.wallparisoft.dto.UserCompanyDto;
import com.wallparisoft.dto.UserDto;
import com.wallparisoft.ebill.utils.exception.ModelNotFoundException;
import com.wallparisoft.entity.User;
import com.wallparisoft.entity.UserCompany;
import com.wallparisoft.mapper.UserCompanyMapper;
import com.wallparisoft.repository.IUserCompanyRepo;
import com.wallparisoft.service.IUserCompanyService;
import com.wallparisoft.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserCompanyServiceImpl implements IUserCompanyService {

    @Autowired
    private IUserCompanyRepo userCompanyRepo;
    @Autowired
    private IUserService userService;
    @Autowired
    private UserCompanyMapper userCompanyMapper;

    @Override
    public UserCompany save(UserCompany entity) {
        return userCompanyRepo.save(entity);
    }

    @Override
    public void delete(Long idCompany, Long idUser) {
        UserCompany UserCompany = userCompanyRepo.findByIdCompanyAndUser_IdUser(idCompany, idUser)
                .orElseThrow(() -> new ModelNotFoundException("Company Client not found for this id company :: " + idCompany + ", " +
                        "and id client :: " + idUser));
        userCompanyRepo.delete(UserCompany);
    }

    @Override
    public UserCompany saveUserCompany(Long idCompany, Long idUser) {
        User user = userService.findById(idUser);
        try{
            findByIdCompanyAndIdUser(idCompany, idUser);
            return null;
        }catch (ModelNotFoundException me){
            UserCompany userCompany = UserCompany.builder()
                    .idCompany(idCompany).user(user).build();
            return save(userCompany);
        }
    }

    @Override
    public Page<UserDto> getUsersFromACompanyPageable(Long idCompany, Pageable pageable) {
        Page<UserCompany> userCompanies = userCompanyRepo.findByIdCompany(idCompany, pageable);
        if (userCompanies == null) {
            throw new ModelNotFoundException("User Company not found for this company id :: " + idCompany);
        }
        UserCompanyDto userCompanyDto=  UserCompanyDto.builder().userDtos(new ArrayList<>()).build();
        if(!userCompanies.isEmpty()){
            userCompanyDto = userCompanyMapper.getUsersFromCompany(userCompanies.getContent());
        }

        return new PageImpl<>(userCompanyDto.getUserDtos(), pageable, userCompanyDto.getUserDtos().size());
    }

    @Override
    public UserCompanyDto getUsersFromACompany(Long idCompany) {
        Optional<List<UserCompany>> UserCompanysOpt = userCompanyRepo.findByIdCompany(idCompany);
        if (UserCompanysOpt.isPresent()) {
            return userCompanyMapper.getUsersFromCompany(UserCompanysOpt.get());
        }
        throw new ModelNotFoundException("User Company not found for this company id :: " + idCompany);
    }

    @Override
    public List<UserCompany> findAll() {
        return userCompanyRepo.findAll();
    }

    @Override
    public UserCompany findById(Long id) {
        return this.userCompanyRepo.findById(id)
                .orElseThrow(() -> new ModelNotFoundException("User Company not found for this id :: " + id));
    }

    @Override
    public UserCompanyDto findByIdCompanyAndIdUser(Long idCompany, Long idUser) {
        UserCompany UserCompany = userCompanyRepo.findByIdCompanyAndUser_IdUser(idCompany, idUser)
                .orElseThrow(() -> new ModelNotFoundException("User Company not found for this id company :: " + idCompany + ", " +
                        "and id client :: " + idUser));
        return userCompanyMapper.convertUserCompanyToUserCompanyDto(UserCompany);
    }
}
