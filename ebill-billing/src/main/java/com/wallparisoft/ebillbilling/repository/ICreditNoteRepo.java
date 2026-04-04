package com.wallparisoft.ebillbilling.repository;

import com.wallparisoft.ebillbilling.entity.CreditNote;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ICreditNoteRepo extends JpaRepository<CreditNote, Long> {

  @Query(
      "SELECT COALESCE(MAX(cn.sequential), 0) FROM CreditNote cn WHERE cn.idIssuancePoint = :idIssuancePoint")
  Long findMaxSequentialByIssuancePoint(@Param("idIssuancePoint") Long idIssuancePoint);

  @Query(
      value =
          "SELECT cn.* FROM credit_note cn "
              + "LEFT JOIN authorization_sri a ON cn.id_authorization_sri = a.id_authorization_sri "
              + "WHERE (COALESCE(:creditNoteNumber, '') = '' OR cn.credit_note_number ILIKE '%' || :creditNoteNumber || '%') "
              + "AND (COALESCE(:idClient, 0) = 0 OR cn.id_client = :idClient) "
              + "AND (COALESCE(:creationDateFrom, '1900-01-01'::timestamp) = '1900-01-01'::timestamp OR cn.creation_date >= :creationDateFrom) "
              + "AND (COALESCE(:creationDateTo, '2099-12-31'::timestamp) = '2099-12-31'::timestamp OR cn.creation_date <= :creationDateTo) "
              + "AND (COALESCE(:receptionStatusSri, '') = '' OR a.reception_status_sri = :receptionStatusSri) "
              + "AND (COALESCE(:authorizationStatusSri, '') = '' OR a.authorization_status_sri = :authorizationStatusSri) "
              + "ORDER BY cn.creation_date DESC",
      countQuery =
          "SELECT COUNT(*) FROM credit_note cn "
              + "LEFT JOIN authorization_sri a ON cn.id_authorization_sri = a.id_authorization_sri "
              + "WHERE (COALESCE(:creditNoteNumber, '') = '' OR cn.credit_note_number ILIKE '%' || :creditNoteNumber || '%') "
              + "AND (COALESCE(:idClient, 0) = 0 OR cn.id_client = :idClient) "
              + "AND (COALESCE(:creationDateFrom, '1900-01-01'::timestamp) = '1900-01-01'::timestamp OR cn.creation_date >= :creationDateFrom) "
              + "AND (COALESCE(:creationDateTo, '2099-12-31'::timestamp) = '2099-12-31'::timestamp OR cn.creation_date <= :creationDateTo) "
              + "AND (COALESCE(:receptionStatusSri, '') = '' OR a.reception_status_sri = :receptionStatusSri) "
              + "AND (COALESCE(:authorizationStatusSri, '') = '' OR a.authorization_status_sri = :authorizationStatusSri)",
      nativeQuery = true)
  Page<CreditNote> findCreditNotesByCriteria(
      @Param("creditNoteNumber") String creditNoteNumber,
      @Param("idClient") Long idClient,
      @Param("creationDateFrom") LocalDateTime creationDateFrom,
      @Param("creationDateTo") LocalDateTime creationDateTo,
      @Param("receptionStatusSri") String receptionStatusSri,
      @Param("authorizationStatusSri") String authorizationStatusSri,
      Pageable pageable);

  /** Busca notas de crédito autorizadas que aún no han sido notificadas por correo */
  @Query(
      "SELECT cn FROM CreditNote cn WHERE "
          + "cn.authorizationSri.authorizationStatusSri = 'AUTORIZADO' AND "
          + "(cn.emailSent IS NULL OR cn.emailSent = false) AND "
          + "cn.status = true")
  List<CreditNote> findAuthorizedCreditNotesNotSentByEmail();

  /** Busca notas de crédito por factura original */
  List<CreditNote> findByOriginalInvoiceId(Long originalInvoiceId);

  /** Busca notas de crédito sin autorización del SRI (borradores) */
  List<CreditNote> findByAuthorizationSriIsNull();

  /**
   * Busca notas de crédito autorizadas y activas por número de comprobante o clave de acceso.
   * Requiere idCompany obligatorio y al menos uno de los criterios de búsqueda.
   *
   * @param idCompany ID de la compañía (obligatorio)
   * @param numeroComprobante Número de comprobante (opcional)
   * @param claveAcceso Clave de acceso (opcional)
   * @return Lista de notas de crédito que cumplen los criterios
   */
  @Query(
      value =
          "SELECT cn.* FROM credit_note cn "
              + "INNER JOIN authorization_sri a ON cn.id_authorization_sri = a.id_authorization_sri "
              + "WHERE cn.id_company = :idCompany "
              + "AND cn.status = true "
              + "AND a.authorization_status_sri = 'AUTORIZADO' "
              + "AND ("
              + "  (:numeroComprobante IS NOT NULL AND :numeroComprobante != '' AND cn.credit_note_number = :numeroComprobante) "
              + "  OR "
              + "  (:claveAcceso IS NOT NULL AND :claveAcceso != '' AND a.access_key = :claveAcceso)"
              + ") "
              + "ORDER BY cn.issue_date DESC",
      nativeQuery = true)
  List<CreditNote> findAuthorizedActiveCreditNotesByNumberOrAccessKey(
      @Param("idCompany") Long idCompany,
      @Param("numeroComprobante") String numeroComprobante,
      @Param("claveAcceso") String claveAcceso);

  @Query(
      "SELECT DISTINCT cn FROM CreditNote cn "
          + "LEFT JOIN FETCH cn.creditNoteDetails "
          + "LEFT JOIN FETCH cn.authorizationSri "
          + "WHERE cn.status = true AND "
          + "((cn.authorizationSri.receptionStatusSri IS NULL OR cn.authorizationSri.receptionStatusSri <> :receptionStatus) "
          + "OR (cn.authorizationSri.authorizationStatusSri IS NULL OR cn.authorizationSri.authorizationStatusSri <> :authorizationStatus))")
  List<CreditNote> findCreditNotesExcludingSpecificSriStatuses(
      @Param("receptionStatus") String receptionStatus,
      @Param("authorizationStatus") String authorizationStatus);

  /**
   * Busca notas de crédito por idCompany (obligatorio) y filtros opcionales (número de nota de
   * crédito o factura original).
   */
  @Query(
      value =
          "SELECT cn.* FROM credit_note cn "
              + "LEFT JOIN authorization_sri a ON cn.id_authorization_sri = a.id_authorization_sri "
              + "WHERE cn.id_company = :idCompany "
              + "AND (COALESCE(:creditNoteNumber, '') = '' OR cn.credit_note_number ILIKE '%' || :creditNoteNumber || '%') "
              + "AND (COALESCE(:originalInvoiceNumber, '') = '' OR cn.original_invoice_number ILIKE '%' || :originalInvoiceNumber || '%') "
              + "AND (a.authorization_status_sri IS NULL OR a.authorization_status_sri = 'AUTORIZADO') "
              + "ORDER BY cn.creation_date DESC",
      countQuery =
          "SELECT COUNT(*) FROM credit_note cn "
              + "LEFT JOIN authorization_sri a ON cn.id_authorization_sri = a.id_authorization_sri "
              + "WHERE cn.id_company = :idCompany "
              + "AND (COALESCE(:creditNoteNumber, '') = '' OR cn.credit_note_number ILIKE '%' || :creditNoteNumber || '%') "
              + "AND (COALESCE(:originalInvoiceNumber, '') = '' OR cn.original_invoice_number ILIKE '%' || :originalInvoiceNumber || '%') "
              + "AND (a.authorization_status_sri IS NULL OR a.authorization_status_sri = 'AUTORIZADO')",
      nativeQuery = true)
  Page<CreditNote> findCreditNotesByCompanyAndFilters(
      @Param("idCompany") Long idCompany,
      @Param("creditNoteNumber") String creditNoteNumber,
      @Param("originalInvoiceNumber") String originalInvoiceNumber,
      Pageable pageable);
}
