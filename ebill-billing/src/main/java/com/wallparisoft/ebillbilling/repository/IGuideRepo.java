package com.wallparisoft.ebillbilling.repository;

import com.wallparisoft.ebillbilling.entity.Guide;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IGuideRepo extends JpaRepository<Guide, Long> {

  @Query(
      "SELECT COALESCE(MAX(g.sequential), 0) FROM Guide g WHERE g.idIssuancePoint = :idIssuancePoint")
  Long findMaxSequentialByIssuancePoint(@Param("idIssuancePoint") Long idIssuancePoint);

  List<Guide> findByIdCompany(Long idCompany);

  Optional<Guide> findByGuideNumber(String guideNumber);

  List<Guide> findByIdCompanyAndIdClient(Long idCompany, Long idClient);

  Optional<Guide> findByIdIssuancePointAndSequential(Long idIssuancePoint, Long sequential);

  List<Guide> findByIdCompanyAndIssueDateBetween(
      Long idCompany, LocalDateTime start, LocalDateTime end);

  /** Busca guías autorizadas que aún no han sido notificadas por correo */
  @Query(
      "SELECT g FROM Guide g WHERE "
          + "g.authorizationSri.authorizationStatusSri = 'AUTORIZADO' AND "
          + "(g.emailSent IS NULL OR g.emailSent = false) AND "
          + "g.status = true")
  List<Guide> findAuthorizedGuidesNotSentByEmail();

  /** Busca guías sin autorización del SRI (borradores) */
  List<Guide> findByAuthorizationSriIsNull();

  /**
   * Busca guías de remisión que no tienen los estados específicos de SRI (excluye RECIBIDA y
   * AUTORIZADO para procesar borradores y devueltas).
   */
  @Query(
      "SELECT DISTINCT g FROM Guide g "
          + "LEFT JOIN FETCH g.guideDetails "
          + "LEFT JOIN FETCH g.authorizationSri "
          + "WHERE g.status = true AND "
          + "((g.authorizationSri.receptionStatusSri IS NULL OR g.authorizationSri.receptionStatusSri <> :receptionStatus) "
          + "OR (g.authorizationSri.authorizationStatusSri IS NULL OR g.authorizationSri.authorizationStatusSri <> :authorizationStatus))")
  List<Guide> findGuidesExcludingSpecificSriStatuses(
      @Param("receptionStatus") String receptionStatus,
      @Param("authorizationStatus") String authorizationStatus);

  @Query(
      value =
          "SELECT g.* FROM guide g "
              + "LEFT JOIN authorization_sri a ON g.id_authorization_sri = a.id_authorization_sri "
              + "WHERE g.id_company = :idCompany "
              + "AND (COALESCE(:guideNumber, '') = '' OR g.guide_number ILIKE '%' || :guideNumber || '%') "
              + "AND (COALESCE(:idClient, 0) = 0 OR g.id_client = :idClient) "
              + "AND (COALESCE(:creationDateFrom, '1900-01-01'::timestamp) = '1900-01-01'::timestamp OR g.creation_date >= :creationDateFrom) "
              + "AND (COALESCE(:creationDateTo, '2099-12-31'::timestamp) = '2099-12-31'::timestamp OR g.creation_date <= :creationDateTo) "
              + "AND (COALESCE(:receptionStatusSri, '') = '' OR a.reception_status_sri = :receptionStatusSri) "
              + "AND (COALESCE(:authorizationStatusSri, '') = '' OR a.authorization_status_sri = :authorizationStatusSri) "
              + "ORDER BY g.creation_date DESC",
      countQuery =
          "SELECT COUNT(*) FROM guide g "
              + "LEFT JOIN authorization_sri a ON g.id_authorization_sri = a.id_authorization_sri "
              + "WHERE g.id_company = :idCompany "
              + "AND (COALESCE(:guideNumber, '') = '' OR g.guide_number ILIKE '%' || :guideNumber || '%') "
              + "AND (COALESCE(:idClient, 0) = 0 OR g.id_client = :idClient) "
              + "AND (COALESCE(:creationDateFrom, '1900-01-01'::timestamp) = '1900-01-01'::timestamp OR g.creation_date >= :creationDateFrom) "
              + "AND (COALESCE(:creationDateTo, '2099-12-31'::timestamp) = '2099-12-31'::timestamp OR g.creation_date <= :creationDateTo) "
              + "AND (COALESCE(:receptionStatusSri, '') = '' OR a.reception_status_sri = :receptionStatusSri) "
              + "AND (COALESCE(:authorizationStatusSri, '') = '' OR a.authorization_status_sri = :authorizationStatusSri)",
      nativeQuery = true)
  Page<Guide> findGuidesByCriteria(
      @Param("idCompany") Long idCompany,
      @Param("guideNumber") String guideNumber,
      @Param("idClient") Long idClient,
      @Param("creationDateFrom") LocalDateTime creationDateFrom,
      @Param("creationDateTo") LocalDateTime creationDateTo,
      @Param("receptionStatusSri") String receptionStatusSri,
      @Param("authorizationStatusSri") String authorizationStatusSri,
      Pageable pageable);

  /** Busca guías de remisión por idCompany (obligatorio) y filtros opcionales (número de guía). */
  @Query(
      value =
          "SELECT g.* FROM guide g "
              + "LEFT JOIN authorization_sri a ON g.id_authorization_sri = a.id_authorization_sri "
              + "LEFT JOIN carrier c ON g.id_carrier = c.id_carrier "
              + "WHERE g.id_company = :idCompany "
              + "AND (COALESCE(:guideNumber, '') = '' OR g.guide_number ILIKE '%' || :guideNumber || '%') "
              + "ORDER BY g.creation_date DESC",
      countQuery =
          "SELECT COUNT(*) FROM guide g "
              + "LEFT JOIN authorization_sri a ON g.id_authorization_sri = a.id_authorization_sri "
              + "LEFT JOIN carrier c ON g.id_carrier = c.id_carrier "
              + "WHERE g.id_company = :idCompany "
              + "AND (COALESCE(:guideNumber, '') = '' OR g.guide_number ILIKE '%' || :guideNumber || '%')",
      nativeQuery = true)
  Page<Guide> findGuidesByCompanyAndFilters(
      @Param("idCompany") Long idCompany,
      @Param("guideNumber") String guideNumber,
      Pageable pageable);
}
