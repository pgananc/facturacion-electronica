package com.wallparisoft.authentication.service;

import com.wallparisoft.authentication.entity.User;
import com.wallparisoft.ebill.auth.util.model.CompanyEstablishment;
import java.util.List;

public interface IUserCompanyEstablishmentService {
  List<CompanyEstablishment> getCompanyEstablishmentsByUser(User user);
}
