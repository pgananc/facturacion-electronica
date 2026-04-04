package com.wallparisoft.ebillbilling.repository;

import com.wallparisoft.ebillbilling.entity.ProformaDetail;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IProformaDetailRepo extends JpaRepository<ProformaDetail, Long> {

  List<ProformaDetail> findByProformaIdProformaOrderByLineOrder(Long idProforma);

  @Query(
      "SELECT COALESCE(SUM(d.subtotal), 0) FROM ProformaDetail d WHERE d.proforma.idProforma = :idProforma")
  BigDecimal sumSubtotalByProformaId(@Param("idProforma") Long idProforma);
}
