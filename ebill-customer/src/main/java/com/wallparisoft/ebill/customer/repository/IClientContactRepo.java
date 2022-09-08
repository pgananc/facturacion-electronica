package com.wallparisoft.ebill.customer.repository;

import com.wallparisoft.ebill.customer.dto.ClientDto;
import com.wallparisoft.ebill.customer.entity.Client;
import com.wallparisoft.ebill.customer.entity.ClientContact;
import com.wallparisoft.ebill.customer.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IClientContactRepo extends JpaRepository<ClientContact, Long> {

    @Query(value = "SELECT c FROM ClientContact  c where c.client.idClient = :idClient and c.contact.idContact =:idContact")
    Optional<ClientContact> findByIdClientAndIdContact(@Param("idClient") Long idClient, @Param("idContact") Long idContact);


}
