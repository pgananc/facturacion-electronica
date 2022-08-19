package com.wallparisoft.ebill.customer.repository;

import com.wallparisoft.ebill.customer.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ICompanyRepo extends JpaRepository<Company, Long> {

}
