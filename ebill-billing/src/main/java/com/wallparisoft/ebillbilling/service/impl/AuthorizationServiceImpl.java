package com.wallparisoft.ebillbilling.service.impl;

import com.wallparisoft.ebillbilling.entity.AuthorizationSri;
import com.wallparisoft.ebillbilling.repository.IAuthorizationRepo;
import com.wallparisoft.ebillbilling.service.IAuthorizationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class AuthorizationServiceImpl implements IAuthorizationService {

  final IAuthorizationRepo authorizationRepo;

  @Override
  public AuthorizationSri register(AuthorizationSri obj) {
    // TODO Auto-generated method stub
    return authorizationRepo.save(obj);
  }

  @Override
  public AuthorizationSri modify(AuthorizationSri obj) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<AuthorizationSri> list() {
    return authorizationRepo.findAll();
  }

  @Override
  public AuthorizationSri getById(Integer id) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean delete(Integer id) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public AuthorizationSri saveAuthorization(AuthorizationSri authorization) {
    // TODO Auto-generated method stub
    return register(authorization);
  }
}
