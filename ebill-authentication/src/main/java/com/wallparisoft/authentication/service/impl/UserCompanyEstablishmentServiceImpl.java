package com.wallparisoft.authentication.service.impl;

import com.wallparisoft.authentication.entity.User;
import com.wallparisoft.authentication.entity.UserCompany;
import com.wallparisoft.authentication.entity.UserCompanyEstablishment;
import com.wallparisoft.authentication.repository.IUserCompanyEstablishmentRepository;
import com.wallparisoft.authentication.repository.IUserCompanyRepo;
import com.wallparisoft.authentication.service.IUserCompanyEstablishmentService;
import com.wallparisoft.ebill.auth.util.model.CompanyEstablishment;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserCompanyEstablishmentServiceImpl implements IUserCompanyEstablishmentService {

  private final IUserCompanyRepo userCompanyRepo;
  private final IUserCompanyEstablishmentRepository userCompanyEstablishmentRepo;

  @Override
  public List<CompanyEstablishment> getCompanyEstablishmentsByUser(User user) {
    List<UserCompany> userCompanies =
        userCompanyRepo.findByUser_IdUser(user.getIdUser()).orElse(Collections.emptyList());

    return userCompanies.stream()
        .map(
            userCompany -> {
              List<UserCompanyEstablishment> uceList =
                  userCompanyEstablishmentRepo.findByUserCompany_IdUserCompany(
                      userCompany.getIdUserCompany());

              List<Long> establishments =
                  uceList.stream().map(UserCompanyEstablishment::getIdEstablishment).toList();

              return CompanyEstablishment.builder()
                  .idCompany(userCompany.getIdCompany())
                  .establishments(establishments)
                  .build();
            })
        .toList();
  }
}
