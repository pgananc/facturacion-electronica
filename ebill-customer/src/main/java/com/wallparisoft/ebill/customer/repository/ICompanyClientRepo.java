package com.wallparisoft.ebill.customer.repository;

import com.wallparisoft.ebill.customer.entity.CompanyClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ICompanyClientRepo extends JpaRepository<CompanyClient, Long> {
    Optional<CompanyClient> findByCompany_IdCompanyAndClient_IdClient(@Param("idCompany") Long idCompany, @Param("idClient") Long idClient);

    Page<CompanyClient> findByCompany_IdCompany(@Param("idCompany") Long idCompany, Pageable pageable);

    Optional<List<CompanyClient>> findByCompany_IdCompany(@Param("idCompany") Long idCompany);
}
