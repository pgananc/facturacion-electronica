package com.wallparisoft.ebill.customer.repository;

import com.wallparisoft.ebill.customer.entity.ClientContact;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IClientContactRepo extends JpaRepository<ClientContact, Long> {

  @Query(
      value =
          "SELECT c FROM ClientContact  c where c.client.idClient = :idClient and c.contact.idContact =:idContact")
  Optional<ClientContact> findByIdClientAndIdContact(
      @Param("idClient") Long idClient, @Param("idContact") Long idContact);
}
