package com.wallparisoft.ebill.customer.repository;

import com.wallparisoft.ebill.customer.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ICompanyRepo extends JpaRepository<Company, Long> {

    @Query(value = "SELECT c FROM Company  c where c.status=true order by c.identification")
    List<Company> findCompaniesActive();
}
