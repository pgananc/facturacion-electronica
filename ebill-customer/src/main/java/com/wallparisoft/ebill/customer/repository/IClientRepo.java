package com.wallparisoft.ebill.customer.repository;

import com.wallparisoft.ebill.customer.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface IClientRepo extends JpaRepository<Client, Long> {
    @Query(value = "SELECT c FROM Client  c where c.status=true order by c.identification")
    List<Client> findClientsActive();
}
