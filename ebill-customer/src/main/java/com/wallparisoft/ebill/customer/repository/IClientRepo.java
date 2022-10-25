package com.wallparisoft.ebill.customer.repository;

import com.wallparisoft.ebill.customer.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IClientRepo extends JpaRepository<Client, Long> {
    @Query(value = "SELECT c FROM Client  c where c.status=true order by c.identification")
    List<Client> findClientsActive();

    @Query(value = "SELECT c FROM Client  c where c.idClient= :idClient and c.status=true order by c.identification")
    List<Client> findClientActiveById(@Param(value = "idClient") Long idClient);


}
