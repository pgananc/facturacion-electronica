package com.wallparisoft.authentication.repository;

import com.wallparisoft.authentication.entity.UserCompanyEstablishment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IUserCompanyEstablishmentRepository
    extends JpaRepository<UserCompanyEstablishment, Long> {

  List<UserCompanyEstablishment> findByUserCompany_IdUserCompany(Long idUserCompany);
}
