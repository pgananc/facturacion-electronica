package com.wallparisoft.ebill.customer.repository;

import com.wallparisoft.ebill.customer.dto.ClientContactPairDto;
import com.wallparisoft.ebill.customer.entity.Contact;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IContactRepo extends JpaRepository<Contact, Long> {
  @Query(
      value =
          "SELECT c FROM ClientContact  cc inner join cc.contact c inner join cc.client cl where cl.idClient = :idClient and cc.status=true and  c.status=true")
  List<Contact> findClientContactActiveByIdClient(@Param("idClient") Long idClient);

  @Query(
      value =
          "SELECT c FROM ClientContact  cc inner join cc.contact c inner join cc.client cl where cl.idClient = :idClient")
  List<Contact> findAllClientContactsByIdClient(@Param("idClient") Long idClient);

  @Query(
      value =
          "SELECT c FROM CompanyContact  cc inner join cc.contact c inner join cc.company co where co.idCompany = :idCompany and cc.status=true and  c.status=true")
  List<Contact> findCompanyContactActiveByIdCompany(@Param("idCompany") Long idCompany);

  /**
   * Método para batch loading - carga contactos de múltiples clientes en UNA sola query. Evita N+1
   * queries devolviendo pares (clientId, contact) que se agrupan en memoria.
   *
   * @param clientIds Lista de IDs de clientes
   * @return Lista de pares clientId-contacto de TODOS los clientes especificados
   */
  @Query(
      value =
          "SELECT new com.wallparisoft.ebill.customer.dto.ClientContactPairDto(cl.idClient, c) "
              + "FROM ClientContact cc "
              + "INNER JOIN cc.contact c "
              + "INNER JOIN cc.client cl "
              + "WHERE cl.idClient IN :clientIds "
              + "AND cc.status = true "
              + "AND c.status = true")
  List<ClientContactPairDto> findClientContactsActiveByClientIds(
      @Param("clientIds") List<Long> clientIds);
}
