package com.wallparisoft.ebillbilling.repository;

import com.wallparisoft.ebillbilling.entity.DebitNoteDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IDebitNoteDetailRepo extends JpaRepository<DebitNoteDetail, Long> {

  /** Calcula el total de notas de débito aplicadas a una factura */
  @Query(
      "SELECT COALESCE(SUM(dnd.subtotal), 0) FROM DebitNoteDetail dnd "
          + "JOIN dnd.debitNote dn "
          + "WHERE dn.originalInvoiceId = :invoiceId "
          + "AND dn.status = true "
          + "AND dnd.status = true")
  java.math.BigDecimal findTotalDebitNotesApplied(@Param("invoiceId") Long invoiceId);
}
