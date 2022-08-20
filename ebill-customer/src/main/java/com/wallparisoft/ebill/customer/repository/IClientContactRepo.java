package com.wallparisoft.ebill.customer.repository;

import com.wallparisoft.ebill.customer.entity.ClientContact;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IClientContactRepo extends JpaRepository<ClientContact, Long> {

}
