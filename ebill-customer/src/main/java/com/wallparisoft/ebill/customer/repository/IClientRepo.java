package com.wallparisoft.ebill.customer.repository;

import com.wallparisoft.ebill.customer.entity.Client;
import com.wallparisoft.ebill.customer.entity.CompanyClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IClientRepo extends JpaRepository<Client, Long> {

    List<Client> findByIdClientOrderByIdentification(@Param(value = "idClient") Long idClient);

    @Query(value = "SELECT c FROM CompanyClient  cc inner join cc.client c where  cc.companyIdentification = :companyIdentification and " +
                   "c.identification like :identification and UPPER(c.name) like :name and " +
                   "(c.clientType =:clientType or :clientType=0 ) and c.status= :status " +
                   "order by c.idClient asc")
    Page<Client> findClientByCompanyAndIdentificationOrNameOrType(String companyIdentification, String identification, String name
            , Integer clientType, Boolean status, Pageable pageable);

    @Query(value = "SELECT c FROM CompanyClient  cc inner join cc.client c " +
                     " where  cc.companyIdentification = :companyIdentification " +
                     " and c.status= :status " +
                     " order by c.idClient asc")
    Optional<List<Client>> findByCompanyIdentificationAndClientStatusOrderByClientAsc (@Param("companyIdentification") String companyIdentification, @Param("status") boolean status);


}
