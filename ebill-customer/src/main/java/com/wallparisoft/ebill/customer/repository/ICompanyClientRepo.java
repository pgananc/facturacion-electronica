package com.wallparisoft.ebill.customer.repository;

import com.wallparisoft.ebill.customer.entity.Client;
import com.wallparisoft.ebill.customer.entity.CompanyClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ICompanyClientRepo extends JpaRepository<CompanyClient, Long> {
    Optional<CompanyClient> findByCompanyIdentificationAndClient_IdClient(@Param("companyIdentification") String companyIdentification, @Param("idClient") Long idClient);

    Page<CompanyClient> findByCompanyIdentification(@Param("companyIdentification") String companyIdentification, Pageable pageable);

    Optional<List<CompanyClient>> findByCompanyIdentification(@Param("companyIdentification") String companyIdentification);

    boolean existsByCompanyIdentificationAndClientIdentification(@Param("companyIdentification") String companyIdentification, @Param("identification") String identification);

    Optional<CompanyClient> findByClient_IdClient(@Param("idClient") Long idClient);
}
