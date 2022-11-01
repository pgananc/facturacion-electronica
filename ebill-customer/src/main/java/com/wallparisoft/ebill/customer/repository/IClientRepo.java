package com.wallparisoft.ebill.customer.repository;

import com.wallparisoft.ebill.customer.entity.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IClientRepo extends JpaRepository<Client, Long> {
    @Query(value = "SELECT c FROM Client  c where c.status=true order by c.identification")
    List<Client> findClientsActive();

    @Query(value = "SELECT c FROM Client  c where c.idClient= :idClient order by c.identification")
    List<Client> findClientById(@Param(value = "idClient") Long idClient);

    boolean existsByIdentification(String identification);

    @Query(value = "SELECT c FROM Client  c where " +
                   "c.identification like :identification and UPPER(c.name) like :name and " +
                   "(c.clientType =:clientType or :clientType=0 ) and c.status= :status " +
                   "order by c.idClient asc")
    Page<Client> findClientByIdentificationOrNameOrType(String identification, String name
            , Integer clientType, Boolean status, Pageable pageable);

}
