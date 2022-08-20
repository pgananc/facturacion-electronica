package com.wallparisoft.ebill.customer.repository;

import com.wallparisoft.ebill.customer.entity.CompanyContact;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ICompanyContactRepo extends JpaRepository<CompanyContact, Long> {

}
