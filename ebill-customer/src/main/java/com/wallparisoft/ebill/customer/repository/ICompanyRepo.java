package com.wallparisoft.ebill.customer.repository;

import com.wallparisoft.ebill.customer.entity.Company;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ICompanyRepo extends JpaRepository<Company, Long> {

  Optional<List<Company>> findByStatusIsTrueOrderByIdentification();

  @Query(
      value =
          "SELECT c "
              + "FROM Company c "
              + "WHERE "
              + "(:identification    IS NULL OR :identification    = '' OR c.identification     LIKE CONCAT('%', :identification,    '%')) "
              + "AND (:name              IS NULL OR :name              = '' OR UPPER(c.name)               LIKE CONCAT('%', UPPER(:name),             '%')) "
              + "AND (:status            IS NULL OR c.status = :status) "
              + "ORDER BY c.idCompany ASC")
  Page<Company> findCompanyByIdentificationOrName(
      String identification, String name, boolean status, Pageable pageable);

  @Query(
      value =
          "SELECT c FROM Company c "
              + "WHERE "
              + "  (:identification    IS NULL OR :identification    = '' OR c.identification     LIKE CONCAT('%', :identification,    '%')) "
              + "AND (:name              IS NULL OR :name              = '' OR UPPER(c.name)               LIKE CONCAT('%', UPPER(:name),             '%')) "
              + "AND (:status            IS NULL OR c.status = :status) "
              + "AND (:companiesIds      IS NULL OR c.idCompany IN :companiesIds) "
              + "ORDER BY c.idCompany ASC")
  Page<Company> findCompanyByIdentificationOrName(
      String identification,
      String name,
      boolean status,
      List<Long> companiesIds,
      Pageable pageable);

  boolean existsByIdentification(String identification);

  @Query("SELECT c FROM Company c WHERE c.idCompany IN :idCompany AND c.status = :status")
  List<Company> findByIdCompanyInAndStatus(
      @Param("idCompany") List<Long> idCompany, @Param("status") boolean status);

  Company findByIdentification(@Param("identification") String identification);
}
