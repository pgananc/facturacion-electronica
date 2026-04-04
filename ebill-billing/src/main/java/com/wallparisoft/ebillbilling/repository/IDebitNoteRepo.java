package com.wallparisoft.ebillbilling.repository;

import com.wallparisoft.ebillbilling.entity.DebitNote;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IDebitNoteRepo extends JpaRepository<DebitNote, Long> {

  @Query(
      "SELECT COALESCE(MAX(dn.sequential), 0) FROM DebitNote dn WHERE dn.idIssuancePoint = :idIssuancePoint")
  Long findMaxSequentialByIssuancePoint(@Param("idIssuancePoint") Long idIssuancePoint);

  @Query(
      value =
          "SELECT dn.* FROM debit_note dn "
              + "LEFT JOIN authorization_sri a ON dn.id_authorization_sri = a.id_authorization_sri "
              + "WHERE (COALESCE(:debitNoteNumber, '') = '' OR dn.debit_note_number ILIKE '%' || :debitNoteNumber || '%') "
              + "AND (COALESCE(:idClient, 0) = 0 OR dn.id_client = :idClient) "
              + "AND (COALESCE(:creationDateFrom, '1900-01-01'::timestamp) = '1900-01-01'::timestamp OR dn.creation_date >= :creationDateFrom) "
              + "AND (COALESCE(:creationDateTo, '2099-12-31'::timestamp) = '2099-12-31'::timestamp OR dn.creation_date <= :creationDateTo) "
              + "AND (COALESCE(:receptionStatusSri, '') = '' OR a.reception_status_sri = :receptionStatusSri) "
              + "AND (COALESCE(:authorizationStatusSri, '') = '' OR a.authorization_status_sri = :authorizationStatusSri) "
              + "ORDER BY dn.creation_date DESC",
      countQuery =
          "SELECT COUNT(*) FROM debit_note dn "
              + "LEFT JOIN authorization_sri a ON dn.id_authorization_sri = a.id_authorization_sri "
              + "WHERE (COALESCE(:debitNoteNumber, '') = '' OR dn.debit_note_number ILIKE '%' || :debitNoteNumber || '%') "
              + "AND (COALESCE(:idClient, 0) = 0 OR dn.id_client = :idClient) "
              + "AND (COALESCE(:creationDateFrom, '1900-01-01'::timestamp) = '1900-01-01'::timestamp OR dn.creation_date >= :creationDateFrom) "
              + "AND (COALESCE(:creationDateTo, '2099-12-31'::timestamp) = '2099-12-31'::timestamp OR dn.creation_date <= :creationDateTo) "
              + "AND (COALESCE(:receptionStatusSri, '') = '' OR a.reception_status_sri = :receptionStatusSri) "
              + "AND (COALESCE(:authorizationStatusSri, '') = '' OR a.authorization_status_sri = :authorizationStatusSri)",
      nativeQuery = true)
  Page<DebitNote> findDebitNotesByCriteria(
      @Param("debitNoteNumber") String debitNoteNumber,
      @Param("idClient") Long idClient,
      @Param("creationDateFrom") LocalDateTime creationDateFrom,
      @Param("creationDateTo") LocalDateTime creationDateTo,
      @Param("receptionStatusSri") String receptionStatusSri,
      @Param("authorizationStatusSri") String authorizationStatusSri,
      Pageable pageable);

  /** Busca notas de débito autorizadas que aún no han sido notificadas por correo */
  @Query(
      "SELECT dn FROM DebitNote dn WHERE "
          + "dn.authorizationSri.authorizationStatusSri = 'AUTORIZADO' AND "
          + "(dn.emailSent IS NULL OR dn.emailSent = false) AND "
          + "dn.status = true")
  List<DebitNote> findAuthorizedDebitNotesNotSentByEmail();

  /** Busca notas de débito por factura original */
  List<DebitNote> findByOriginalInvoiceId(Long originalInvoiceId);

  /** Busca notas de débito sin autorización del SRI (borradores) */
  List<DebitNote> findByAuthorizationSriIsNull();

  /**
   * Busca notas de débito autorizadas y activas por número de comprobante o clave de acceso.
   * Requiere idCompany obligatorio y al menos uno de los criterios de búsqueda.
   *
   * @param idCompany ID de la compañía (obligatorio)
   * @param numeroComprobante Número de comprobante (opcional)
   * @param claveAcceso Clave de acceso (opcional)
   * @return Lista de notas de débito que cumplen los criterios
   */
  @Query(
      value =
          "SELECT dn.* FROM debit_note dn "
              + "INNER JOIN authorization_sri a ON dn.id_authorization_sri = a.id_authorization_sri "
              + "WHERE dn.id_company = :idCompany "
              + "AND dn.status = true "
              + "AND a.authorization_status_sri = 'AUTORIZADO' "
              + "AND ("
              + "  (:numeroComprobante IS NOT NULL AND :numeroComprobante != '' AND dn.debit_note_number = :numeroComprobante) "
              + "  OR "
              + "  (:claveAcceso IS NOT NULL AND :claveAcceso != '' AND a.access_key = :claveAcceso)"
              + ") "
              + "ORDER BY dn.issue_date DESC",
      nativeQuery = true)
  List<DebitNote> findAuthorizedActiveDebitNotesByNumberOrAccessKey(
      @Param("idCompany") Long idCompany,
      @Param("numeroComprobante") String numeroComprobante,
      @Param("claveAcceso") String claveAcceso);

  @Query(
      "SELECT DISTINCT dn FROM DebitNote dn "
          + "LEFT JOIN FETCH dn.debitNoteDetails "
          + "LEFT JOIN FETCH dn.authorizationSri "
          + "WHERE dn.status = true AND "
          + "((dn.authorizationSri.receptionStatusSri IS NULL OR dn.authorizationSri.receptionStatusSri <> :receptionStatus) "
          + "OR (dn.authorizationSri.authorizationStatusSri IS NULL OR dn.authorizationSri.authorizationStatusSri <> :authorizationStatus))")
  List<DebitNote> findDebitNotesExcludingSpecificSriStatuses(
      @Param("receptionStatus") String receptionStatus,
      @Param("authorizationStatus") String authorizationStatus);

  /**
   * Busca notas de débito por idCompany (obligatorio) y filtros opcionales (número de nota de
   * débito o factura original).
   */
  @Query(
      value =
          "SELECT dn.* FROM debit_note dn "
              + "LEFT JOIN authorization_sri a ON dn.id_authorization_sri = a.id_authorization_sri "
              + "WHERE dn.id_company = :idCompany "
              + "AND (COALESCE(:debitNoteNumber, '') = '' OR dn.debit_note_number ILIKE '%' || :debitNoteNumber || '%') "
              + "AND (COALESCE(:originalInvoiceNumber, '') = '' OR dn.original_invoice_number ILIKE '%' || :originalInvoiceNumber || '%') "
              + "AND (a.authorization_status_sri IS NULL OR a.authorization_status_sri = 'AUTORIZADO') "
              + "ORDER BY dn.creation_date DESC",
      countQuery =
          "SELECT COUNT(*) FROM debit_note dn "
              + "LEFT JOIN authorization_sri a ON dn.id_authorization_sri = a.id_authorization_sri "
              + "WHERE dn.id_company = :idCompany "
              + "AND (COALESCE(:debitNoteNumber, '') = '' OR dn.debit_note_number ILIKE '%' || :debitNoteNumber || '%') "
              + "AND (COALESCE(:originalInvoiceNumber, '') = '' OR dn.original_invoice_number ILIKE '%' || :originalInvoiceNumber || '%') "
              + "AND (a.authorization_status_sri IS NULL OR a.authorization_status_sri = 'AUTORIZADO')",
      nativeQuery = true)
  Page<DebitNote> findDebitNotesByCompanyAndFilters(
      @Param("idCompany") Long idCompany,
      @Param("debitNoteNumber") String debitNoteNumber,
      @Param("originalInvoiceNumber") String originalInvoiceNumber,
      Pageable pageable);
}
