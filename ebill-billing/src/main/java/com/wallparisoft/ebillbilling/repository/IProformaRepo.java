package com.wallparisoft.ebillbilling.repository;

import com.wallparisoft.ebillbilling.entity.Proforma;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IProformaRepo extends JpaRepository<Proforma, Long> {

  Optional<Proforma> findByIdProformaAndIdCompany(Long idProforma, Long idCompany);

  boolean existsByIdCompanyAndProformaNumber(Long idCompany, String proformaNumber);

  @Query(
      "SELECT p FROM Proforma p "
          + "WHERE p.idCompany = :idCompany "
          + "AND p.issueDate >= COALESCE(:issueDateFrom, p.issueDate) "
          + "AND p.issueDate <= COALESCE(:issueDateTo, p.issueDate) "
          + "AND p.status = COALESCE(:status, p.status) "
          + "AND p.idClient = COALESCE(:idClient, p.idClient)")
  Page<Proforma> findByFilters(
      @Param("idCompany") Long idCompany,
      @Param("issueDateFrom") LocalDate issueDateFrom,
      @Param("issueDateTo") LocalDate issueDateTo,
      @Param("status") Proforma.Status status,
      @Param("idClient") Long idClient,
      Pageable pageable);

  @Query("SELECT p FROM Proforma p LEFT JOIN FETCH p.details WHERE p.idProforma = :id")
  Optional<Proforma> findByIdWithDetails(@Param("id") Long idProforma);

  /**
   * Next sequential number for proforma_number (format PRO-000001). Uses numeric part after "PRO-".
   */
  @Query(
      value =
          "SELECT COALESCE(MAX(CAST(SUBSTRING(proforma_number FROM 5) AS INTEGER)), 0) + 1 FROM proforma WHERE id_company = :idCompany",
      nativeQuery = true)
  Long findNextSequentialForCompany(@Param("idCompany") Long idCompany);

  /**
   * Marca como EXPIRED todas las proformas vencidas (valid_until &lt;= :today), sin factura
   * asociada (id_invoice IS NULL) y que aún estén en estado DRAFT o SENT.
   *
   * @param today fecha de corte (típicamente LocalDate.now()).
   * @return número de filas actualizadas.
   */
  @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
  @org.springframework.transaction.annotation.Transactional
  @Query(
      "UPDATE Proforma p "
          + "SET p.status =:status, "
          + "    p.updatedAt = CURRENT_TIMESTAMP "
          + "WHERE p.validUntil IS NOT NULL "
          + "  AND p.validUntil <= :today "
          + "  AND p.idInvoice IS NULL "
          + "  AND p.status IN (com.wallparisoft.ebillbilling.entity.Proforma.Status.DRAFT, "
          + "                   com.wallparisoft.ebillbilling.entity.Proforma.Status.SENT)")
  int markExpiredProformas(
      @Param("today") LocalDate today, @Param("status") Proforma.Status status);
}
