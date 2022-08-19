package com.wallparisoft.ebill.customer.repository;

import com.wallparisoft.ebill.customer.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IClientRepo extends JpaRepository<Client, Long> {

}
