package com.wallparisoft.ebill.customer.repository;

import com.wallparisoft.ebill.customer.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IContactRepo extends JpaRepository<Contact, Long> {
    @Query(value = "SELECT c FROM ClientContact  cc inner join cc.contact c inner join cc.client cl where cl.idClient = :idClient and cc.status=true and  c.status=true")
    List<Contact> findClientContactActiveByIdClient(@Param("idClient") Long idClient);

    @Query(value = "SELECT c FROM CompanyContact  cc inner join cc.contact c inner join cc.company co where co.idCompany = :idCompany and cc.status=true and  c.status=true")
    List<Contact> findCompanyContactActiveByIdCompany(@Param("idCompany") Long idCompany);
}
