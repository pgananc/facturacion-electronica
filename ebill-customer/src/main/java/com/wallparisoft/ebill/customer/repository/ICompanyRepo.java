package com.wallparisoft.ebill.customer.repository;


import com.wallparisoft.ebill.customer.entity.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface ICompanyRepo extends JpaRepository<Company, Long> {

    Optional<List<Company>> findByStatusIsTrueOrderByIdentification();

    @Query(value = "SELECT c FROM Company c where " +
            "c.identification like :identification and UPPER(c.name) like :name and " +
            "UPPER(c.branchOfficeCode) like :branchOfficeCode and c.status= :status " +
            "order by c.idCompany asc")
   Page<Company> findCompanyByIdentificationOrNameOrBranchOfficeCode(String identification, String name
            , String branchOfficeCode, boolean status, Pageable pageable);

    boolean existsByIdentificationAndBranchOfficeCode(String identification, String branchOfficeCode);

    List<Company> findByIdCompanyInAndStatus(@Param("idCompany") List<Long> idCompany, @Param("status") boolean status);

    Company findByIdentification(@Param("identification") String identification);

}