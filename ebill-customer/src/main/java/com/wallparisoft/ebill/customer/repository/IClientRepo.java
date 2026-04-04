package com.wallparisoft.ebill.customer.repository;

import com.wallparisoft.ebill.customer.entity.Client;
import com.wallparisoft.ebill.customer.entity.Contact;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IClientRepo extends JpaRepository<Client, Long> {

  List<Client> findByIdClientOrderByIdentification(@Param(value = "idClient") Long idClient);

  /**
   * Query optimizada con batch loading manual usando subconsulta Como no tenemos @OneToMany
   * mapeado, usamos este approach
   */
  @Query(
      value =
          "SELECT DISTINCT c FROM CompanyClient cc "
              + "INNER JOIN cc.client c "
              + "WHERE cc.companyIdentification = :companyIdentification "
              + "AND c.identification LIKE :identification "
              + "AND UPPER(c.name) LIKE :name "
              + "AND (:clientType IS NULL OR :clientType = 0 OR c.clientType = :clientType) "
              + "AND c.status = :status "
              + "AND (:email IS NULL OR :email = '' OR EXISTS ("
              + "    SELECT 1 FROM ClientContact ccEmail "
              + "    INNER JOIN ccEmail.contact contEmail "
              + "    WHERE ccEmail.client = c "
              + "    AND contEmail.contactType = 'MAI' "
              + "    AND contEmail.value LIKE :email"
              + ")) "
              + "AND (:phone IS NULL OR :phone = '' OR EXISTS ("
              + "    SELECT 1 FROM ClientContact ccPhone "
              + "    INNER JOIN ccPhone.contact contPhone "
              + "    WHERE ccPhone.client = c "
              + "    AND contPhone.contactType = 'TEL' "
              + "    AND contPhone.value LIKE :phone"
              + ")) "
              + "AND (:mobile IS NULL OR :mobile = '' OR EXISTS ("
              + "    SELECT 1 FROM ClientContact ccMobile "
              + "    INNER JOIN ccMobile.contact contMobile "
              + "    WHERE ccMobile.client = c "
              + "    AND contMobile.contactType = 'CEL' "
              + "    AND contMobile.value LIKE :mobile"
              + ")) "
              + "AND (:address IS NULL OR :address = '' OR EXISTS ("
              + "    SELECT 1 FROM ClientContact ccAddress "
              + "    INNER JOIN ccAddress.contact contAddress "
              + "    WHERE ccAddress.client = c "
              + "    AND contAddress.contactType = 'DIR' "
              + "    AND UPPER(contAddress.value) LIKE :address"
              + "))")
  Page<Client> findClientByCompanyAndIdentificationOrNameOrType(
      @Param("companyIdentification") String companyIdentification,
      @Param("identification") String identification,
      @Param("name") String name,
      @Param("clientType") Integer clientType,
      @Param("status") Boolean status,
      @Param("email") String email,
      @Param("phone") String phone,
      @Param("mobile") String mobile,
      @Param("address") String address,
      Pageable pageable);

  @Query(
      value =
          "SELECT c FROM CompanyClient  cc inner join cc.client c "
              + " where  cc.companyIdentification = :companyIdentification "
              + " and c.status= :status "
              + " order by c.idClient asc")
  Optional<List<Client>> findByCompanyIdentificationAndClientStatusOrderByClientAsc(
      @Param("companyIdentification") String companyIdentification,
      @Param("status") boolean status);

  /** Query to find a client by client identification and email */
  @Query(
      value =
          "SELECT DISTINCT c FROM Client c "
              + "WHERE c.identification = :identification "
              + "AND c.status = true "
              + "AND EXISTS ("
              + "    SELECT 1 FROM ClientContact ccEmail "
              + "    INNER JOIN ccEmail.contact contEmail "
              + "    WHERE ccEmail.client = c "
              + "    AND contEmail.contactType = 'MAI' "
              + "    AND contEmail.value = :email "
              + "    AND contEmail.status = true "
              + "    AND ccEmail.status = true"
              + ")")
  Optional<Client> findByIdentificationAndEmail(
      @Param("identification") String identification, @Param("email") String email);

  /** Alternative query to find client by identification only (for debugging) */
  @Query("SELECT c FROM Client c WHERE c.identification = :identification AND c.status = true")
  Optional<Client> findByIdentification(@Param("identification") String identification);

  /** Query to find contacts by client ID and email */
  @Query(
      "SELECT cont FROM Contact cont "
          + "INNER JOIN ClientContact cc ON cc.contact = cont "
          + "WHERE cc.client.idClient = :clientId "
          + "AND cont.contactType = 'MAI' "
          + "AND cont.value = :email "
          + "AND cont.status = true "
          + "AND cc.status = true")
  List<Contact> findContactByClientIdAndEmail(
      @Param("clientId") Long clientId, @Param("email") String email);

  @Query("SELECT c FROM Client c WHERE c.identification = :identification AND c.status = true")
  Optional<List<Client>> findByIdentificationList(@Param("identification") String identification);
}
