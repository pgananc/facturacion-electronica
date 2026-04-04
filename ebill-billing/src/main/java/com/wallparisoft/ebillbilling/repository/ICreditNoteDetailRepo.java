package com.wallparisoft.ebillbilling.repository;

import com.wallparisoft.ebillbilling.entity.CreditNoteDetail;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repositorio para detalles de notas de crédito. */
public interface ICreditNoteDetailRepo extends JpaRepository<CreditNoteDetail, Long> {

  /**
   * Obtiene la suma de cantidades devueltas por cada detalle de factura. SOLO considera NC
   * AUTORIZADAS (authorizationStatusSri = 'AUTORIZADO'). Las NC devueltas/fallidas NO bloquean los
   * productos.
   *
   * @param idInvoice ID de la factura original
   * @return Lista de arrays [idInvoiceDetail, sumQuantity]
   */
  @Query(
      value =
          "SELECT cnd.id_invoice_detail, COALESCE(SUM(cnd.quantity), 0) as total_returned "
              + "FROM credit_note_detail cnd "
              + "INNER JOIN credit_note cn ON cnd.id_credit_note = cn.id_credit_note "
              + "LEFT JOIN authorization_sri auth ON cn.id_authorization_sri = auth.id_authorization_sri "
              + "WHERE cn.original_invoice_id = :idInvoice "
              + "AND cn.status = true "
              + "AND cnd.id_invoice_detail IS NOT NULL "
              + "AND auth.authorization_status_sri = 'AUTORIZADO' "
              + "GROUP BY cnd.id_invoice_detail",
      nativeQuery = true)
  List<Object[]> findQuantitiesReturnedByInvoice(@Param("idInvoice") Long idInvoice);

  /**
   * Obtiene el total de NC aplicadas a una factura. SOLO considera NC AUTORIZADAS
   * (authorizationStatusSri = 'AUTORIZADO').
   *
   * @param idInvoice ID de la factura original
   * @return Suma de total_amount de todas las NC AUTORIZADAS
   */
  @Query(
      "SELECT COALESCE(SUM(cn.totalAmount), 0) FROM CreditNote cn "
          + "WHERE cn.originalInvoiceId = :idInvoice "
          + "AND cn.status = true "
          + "AND cn.authorizationSri.authorizationStatusSri = 'AUTORIZADO'")
  java.math.BigDecimal findTotalCreditNotesApplied(@Param("idInvoice") Long idInvoice);
}
