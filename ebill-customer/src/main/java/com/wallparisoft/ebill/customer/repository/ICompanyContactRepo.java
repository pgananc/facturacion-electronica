package com.wallparisoft.ebill.customer.repository;

import com.wallparisoft.ebill.customer.entity.CompanyContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ICompanyContactRepo extends JpaRepository<CompanyContact, Long> {

    @Query(value = "SELECT c FROM CompanyContact  c where c.company.idCompany = :idCompany and c.contact.idContact =:idContact")
    Optional<CompanyContact> findByIdCompanyAndIdContact(@Param("idCompany") Long idCompany, @Param("idContact") Long idContact);

}
