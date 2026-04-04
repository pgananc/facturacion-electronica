package com.wallparisoft.ebillbilling.repository;

import com.wallparisoft.ebillbilling.entity.CompanySubscription;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ICompanySubscriptionRepo extends JpaRepository<CompanySubscription, Long> {

  @Query(
      "SELECT cs FROM CompanySubscription cs "
          + "LEFT JOIN FETCH cs.plan "
          + "WHERE cs.idCompany = :idCompany "
          + "AND cs.subscriptionStatus = 'ACTIVE' "
          + "ORDER BY cs.creationDate DESC "
          + "LIMIT 1")
  Optional<CompanySubscription> findActiveByIdCompany(@Param("idCompany") Long idCompany);

  /**
   * Busca la última suscripción de una compañía (activa o cancelada) para permitir reactivación.
   * Útil para SUPER_ADMIN que quiere reactivar suscripciones canceladas.
   *
   * @param idCompany ID de la empresa
   * @return Última suscripción encontrada (puede estar cancelada o activa)
   */
  @Query(
      "SELECT cs FROM CompanySubscription cs "
          + "LEFT JOIN FETCH cs.plan "
          + "WHERE cs.idCompany = :idCompany "
          + "ORDER BY cs.creationDate DESC "
          + "LIMIT 1")
  Optional<CompanySubscription> findLastByIdCompany(@Param("idCompany") Long idCompany);

  @Query(
      "SELECT cs FROM CompanySubscription cs "
          + "LEFT JOIN FETCH cs.plan "
          + "WHERE cs.idSubscription = :idSubscription")
  Optional<CompanySubscription> findByIdWithPlan(@Param("idSubscription") Long idSubscription);

  @Query(
      "SELECT cs FROM CompanySubscription cs "
          + "WHERE cs.idCompany = :idCompany "
          + "AND cs.subscriptionStatus = 'ACTIVE'")
  Optional<CompanySubscription> findActiveSubscriptionByCompany(@Param("idCompany") Long idCompany);

  // =====================================================
  // SUBSCRIPTION_MANAGEMENT - Métodos para gestión administrativa
  // =====================================================

  /** Listar todas las suscripciones con filtros opcionales (para SUPER_ADMIN) */
  @Query(
      "SELECT cs FROM CompanySubscription cs "
          + "LEFT JOIN FETCH cs.plan "
          + "WHERE (:status IS NULL OR cs.subscriptionStatus = :status) "
          + "AND (:idPlan IS NULL OR cs.idPlan = :idPlan) "
          + "ORDER BY cs.creationDate DESC")
  Page<CompanySubscription> findAllWithFilters(
      @Param("status") String status, @Param("idPlan") Long idPlan, Pageable pageable);

  /** Contar suscripciones por estado */
  @Query("SELECT COUNT(cs) FROM CompanySubscription cs " + "WHERE cs.subscriptionStatus = :status")
  Long countByStatus(@Param("status") String status);

  /** Contar suscripciones por plan */
  @Query("SELECT COUNT(cs) FROM CompanySubscription cs " + "WHERE cs.idPlan = :idPlan")
  Long countByPlan(@Param("idPlan") Long idPlan);

  /**
   * Verifica si una empresa tiene una suscripción activa.
   *
   * @param idCompany ID de la empresa
   * @return true si tiene suscripción activa y no vencida
   */
  @Query(
      "SELECT CASE WHEN COUNT(cs) > 0 THEN true ELSE false END "
          + "FROM CompanySubscription cs "
          + "WHERE cs.idCompany = :idCompany "
          + "AND cs.subscriptionStatus = 'ACTIVE' "
          + "AND cs.expiresAt > CURRENT_TIMESTAMP")
  boolean existsActiveSubscriptionForCompany(@Param("idCompany") Long idCompany);

  /**
   * Obtiene los IDs de las empresas que tienen suscripción activa. Esto se usa para excluirlas al
   * consultar empresas sin suscripción desde ebill-customer.
   *
   * @return Lista de IDs de empresas con suscripción activa
   */
  @Query(
      "SELECT DISTINCT cs.idCompany FROM CompanySubscription cs "
          + "WHERE cs.subscriptionStatus = 'ACTIVE' "
          + "AND cs.expiresAt > CURRENT_TIMESTAMP")
  List<Long> findCompanyIdsWithActiveSubscription();

  /**
   * Busca suscripciones programadas para cancelación que ya vencieron. Condiciones: - Tiene
   * cancel_reason (fue programada para cancelar) - NO tiene cancelled_at (aún no se canceló) -
   * expires_at <= ahora (ya venció) - subscription_status = 'ACTIVE' (aún está activa)
   *
   * @return Lista de suscripciones que deben ser canceladas
   */
  @Query(
      "SELECT cs FROM CompanySubscription cs "
          + "WHERE cs.cancelReason IS NOT NULL "
          + "AND cs.cancelledAt IS NULL "
          + "AND cs.expiresAt <= CURRENT_TIMESTAMP "
          + "AND cs.subscriptionStatus = 'ACTIVE'")
  List<CompanySubscription> findScheduledCancellationsToProcess();
}
