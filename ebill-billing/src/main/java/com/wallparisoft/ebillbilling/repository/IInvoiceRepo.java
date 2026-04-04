package com.wallparisoft.ebillbilling.repository;

import com.wallparisoft.ebillbilling.entity.Invoice;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IInvoiceRepo extends JpaRepository<Invoice, Long> {
  List<Invoice>
      findByAuthorizationSri_ReceptionStatusSriIsNotAndAuthorizationSri_AuthorizationStatusSriIsNot(
          String receptionStatus, String authorizationStatus);

  @Query(
      "SELECT i FROM Invoice i WHERE "
          + "i.status = true AND "
          + "((i.authorizationSri.receptionStatusSri IS NULL OR i.authorizationSri.receptionStatusSri <> :receptionStatus) "
          + "OR (i.authorizationSri.authorizationStatusSri IS NULL OR i.authorizationSri.authorizationStatusSri <> :authorizationStatus))")
  List<Invoice> findInvoicesExcludingSpecificSriStatuses(
      @Param("receptionStatus") String receptionStatus,
      @Param("authorizationStatus") String authorizationStatus);

  @Query(
      "SELECT COALESCE(MAX(i.sequential), 0) FROM Invoice i WHERE i.idIssuancePoint = :idIssuancePoint")
  Long findMaxSequentialByIssuancePoint(@Param("idIssuancePoint") Long idIssuancePoint);

  @Query(
      value =
          "SELECT i.* FROM invoice i "
              + "LEFT JOIN authorization_sri a ON i.id_authorization_sri = a.id_authorization_sri "
              + "WHERE (COALESCE(:invoiceNumber, '') = '' OR i.invoice_number ILIKE '%' || :invoiceNumber || '%') "
              + "AND (COALESCE(:idClient, 0) = 0 OR i.id_client = :idClient) "
              + "AND (COALESCE(:creationDateFrom, '1900-01-01'::timestamp) = '1900-01-01'::timestamp OR i.creation_date >= :creationDateFrom) "
              + "AND (COALESCE(:creationDateTo, '2099-12-31'::timestamp) = '2099-12-31'::timestamp OR i.creation_date <= :creationDateTo) "
              + "AND (COALESCE(:receptionStatusSri, '') = '' OR a.reception_status_sri = :receptionStatusSri)"
              + "AND (COALESCE(:authorizationStatusSri, '') = '' OR a.authorization_status_sri = :authorizationStatusSri)",
      countQuery =
          "SELECT COUNT(*) FROM invoice i "
              + "LEFT JOIN authorization_sri a ON i.id_authorization_sri = a.id_authorization_sri "
              + "WHERE (COALESCE(:invoiceNumber, '') = '' OR i.invoice_number ILIKE '%' || :invoiceNumber || '%') "
              + "AND (COALESCE(:idClient, 0) = 0 OR i.id_client = :idClient) "
              + "AND (COALESCE(:creationDateFrom, '1900-01-01'::timestamp) = '1900-01-01'::timestamp OR i.creation_date >= :creationDateFrom) "
              + "AND (COALESCE(:creationDateTo, '2099-12-31'::timestamp) = '2099-12-31'::timestamp OR i.creation_date <= :creationDateTo) "
              + "AND (COALESCE(:receptionStatusSri, '') = '' OR a.reception_status_sri = :receptionStatusSri)"
              + "AND (COALESCE(:authorizationStatusSri, '') = '' OR a.authorization_status_sri = :authorizationStatusSri)",
      nativeQuery = true)
  Page<Invoice> findInvoicesByCriteria(
      @Param("invoiceNumber") String invoiceNumber,
      @Param("idClient") Long idClient,
      @Param("creationDateFrom") LocalDateTime creationDateFrom,
      @Param("creationDateTo") LocalDateTime creationDateTo,
      @Param("receptionStatusSri") String receptionStatusSri,
      @Param("authorizationStatusSri") String authorizationStatusSri,
      Pageable pageable);

  /**
   * Busca facturas por idCompany (obligatorio) y filtros opcionales para crear notas de débito.
   * Solo retorna facturas autorizadas. El filtro por cliente se hace por idClient (obtenido del
   * microservicio de clientes).
   */
  @Query(
      value =
          "SELECT i.* FROM invoice i "
              + "LEFT JOIN authorization_sri a ON i.id_authorization_sri = a.id_authorization_sri "
              + "WHERE i.id_company = :idCompany "
              + "AND (COALESCE(:numeroAutorizacion, '') = '' OR a.authorization_number ILIKE '%' || :numeroAutorizacion || '%') "
              + "AND (COALESCE(:numeroFactura, '') = '' OR i.invoice_number ILIKE '%' || :numeroFactura || '%') "
              + "AND (COALESCE(:idClient, 0) = 0 OR i.id_client = :idClient) "
              + "AND a.authorization_status_sri = 'AUTORIZADO' "
              + "ORDER BY i.creation_date DESC",
      countQuery =
          "SELECT COUNT(*) FROM invoice i "
              + "LEFT JOIN authorization_sri a ON i.id_authorization_sri = a.id_authorization_sri "
              + "WHERE i.id_company = :idCompany "
              + "AND (COALESCE(:numeroAutorizacion, '') = '' OR a.authorization_number ILIKE '%' || :numeroAutorizacion || '%') "
              + "AND (COALESCE(:numeroFactura, '') = '' OR i.invoice_number ILIKE '%' || :numeroFactura || '%') "
              + "AND (COALESCE(:idClient, 0) = 0 OR i.id_client = :idClient) "
              + "AND a.authorization_status_sri = 'AUTORIZADO'",
      nativeQuery = true)
  Page<Invoice> findInvoicesForDebitNote(
      @Param("idCompany") Long idCompany,
      @Param("numeroAutorizacion") String numeroAutorizacion,
      @Param("numeroFactura") String numeroFactura,
      @Param("idClient") Long idClient,
      Pageable pageable);

  /**
   * Busca facturas por idCompany (obligatorio) y filtros opcionales. Retorna todas las facturas
   * (autorizadas y borradores). El filtro por cliente se hace por idClient (obtenido del
   * microservicio de clientes).
   */
  @Query(
      value =
          "SELECT i.* FROM invoice i "
              + "LEFT JOIN authorization_sri a ON i.id_authorization_sri = a.id_authorization_sri "
              + "WHERE i.id_company = :idCompany "
              + "AND (COALESCE(:numeroAutorizacion, '') = '' OR a.authorization_number ILIKE '%' || :numeroAutorizacion || '%') "
              + "AND (COALESCE(:numeroFactura, '') = '' OR i.invoice_number ILIKE '%' || :numeroFactura || '%') "
              + "AND (COALESCE(:idClient, 0) = 0 OR i.id_client = :idClient) "
              + "ORDER BY i.creation_date DESC",
      countQuery =
          "SELECT COUNT(*) FROM invoice i "
              + "LEFT JOIN authorization_sri a ON i.id_authorization_sri = a.id_authorization_sri "
              + "WHERE i.id_company = :idCompany "
              + "AND (COALESCE(:numeroAutorizacion, '') = '' OR a.authorization_number ILIKE '%' || :numeroAutorizacion || '%') "
              + "AND (COALESCE(:numeroFactura, '') = '' OR i.invoice_number ILIKE '%' || :numeroFactura || '%') "
              + "AND (COALESCE(:idClient, 0) = 0 OR i.id_client = :idClient)",
      nativeQuery = true)
  Page<Invoice> findInvoicesByCompanyAndFilters(
      @Param("idCompany") Long idCompany,
      @Param("numeroAutorizacion") String numeroAutorizacion,
      @Param("numeroFactura") String numeroFactura,
      @Param("idClient") Long idClient,
      Pageable pageable);

  /**
   * Busca facturas por idCompany (obligatorio), rango de fechas y filtros opcionales con
   * paginación. Retorna todas las facturas (autorizadas y borradores).
   */
  @Query(
      value =
          "SELECT i.* FROM invoice i "
              + "LEFT JOIN authorization_sri a ON i.id_authorization_sri = a.id_authorization_sri "
              + "WHERE i.id_company = :idCompany "
              + "AND (COALESCE(:creationDateFrom, '1900-01-01'::timestamp) = '1900-01-01'::timestamp OR i.creation_date >= :creationDateFrom) "
              + "AND (COALESCE(:creationDateTo, '2099-12-31'::timestamp) = '2099-12-31'::timestamp OR i.creation_date <= :creationDateTo) "
              + "AND (COALESCE(:numeroAutorizacion, '') = '' OR a.authorization_number ILIKE '%' || :numeroAutorizacion || '%') "
              + "AND (COALESCE(:numeroFactura, '') = '' OR i.invoice_number ILIKE '%' || :numeroFactura || '%') "
              + "AND (COALESCE(:idClient, 0) = 0 OR i.id_client = :idClient) "
              + "AND (COALESCE(:receptionStatusSri, '') = '' OR a.reception_status_sri = :receptionStatusSri) "
              + "AND (COALESCE(:authorizationStatusSri, '') = '' OR a.authorization_status_sri = :authorizationStatusSri) "
              + "ORDER BY i.creation_date DESC",
      countQuery =
          "SELECT COUNT(*) FROM invoice i "
              + "LEFT JOIN authorization_sri a ON i.id_authorization_sri = a.id_authorization_sri "
              + "WHERE i.id_company = :idCompany "
              + "AND (COALESCE(:creationDateFrom, '1900-01-01'::timestamp) = '1900-01-01'::timestamp OR i.creation_date >= :creationDateFrom) "
              + "AND (COALESCE(:creationDateTo, '2099-12-31'::timestamp) = '2099-12-31'::timestamp OR i.creation_date <= :creationDateTo) "
              + "AND (COALESCE(:numeroAutorizacion, '') = '' OR a.authorization_number ILIKE '%' || :numeroAutorizacion || '%') "
              + "AND (COALESCE(:numeroFactura, '') = '' OR i.invoice_number ILIKE '%' || :numeroFactura || '%') "
              + "AND (COALESCE(:idClient, 0) = 0 OR i.id_client = :idClient) "
              + "AND (COALESCE(:receptionStatusSri, '') = '' OR a.reception_status_sri = :receptionStatusSri) "
              + "AND (COALESCE(:authorizationStatusSri, '') = '' OR a.authorization_status_sri = :authorizationStatusSri)",
      nativeQuery = true)
  Page<Invoice> findInvoicesByCompanyAndDateRange(
      @Param("idCompany") Long idCompany,
      @Param("creationDateFrom") java.time.LocalDateTime creationDateFrom,
      @Param("creationDateTo") java.time.LocalDateTime creationDateTo,
      @Param("numeroAutorizacion") String numeroAutorizacion,
      @Param("numeroFactura") String numeroFactura,
      @Param("idClient") Long idClient,
      @Param("receptionStatusSri") String receptionStatusSri,
      @Param("authorizationStatusSri") String authorizationStatusSri,
      Pageable pageable);

  // Find all draft invoices (invoices without authorization from SRI)
  List<Invoice> findByAuthorizationSriIsNull();

  /**
   * Busca facturas autorizadas y activas por número de factura o clave de acceso. Requiere
   * idCompany obligatorio y al menos uno de los criterios de búsqueda.
   *
   * @param idCompany ID de la compañía (obligatorio)
   * @param numeroFactura Número de factura (opcional)
   * @param claveAcceso Clave de acceso (opcional)
   * @return Lista de facturas que cumplen los criterios
   */
  @Query(
      value =
          "SELECT i.* FROM invoice i "
              + "INNER JOIN authorization_sri a ON i.id_authorization_sri = a.id_authorization_sri "
              + "WHERE i.id_company = :idCompany "
              + "AND i.status = true "
              + "AND a.authorization_status_sri = 'AUTORIZADO' "
              + "AND ("
              + "  (:numeroFactura IS NOT NULL AND :numeroFactura != '' AND i.invoice_number = :numeroFactura) "
              + "  OR "
              + "  (:claveAcceso IS NOT NULL AND :claveAcceso != '' AND a.access_key = :claveAcceso)"
              + ") "
              + "ORDER BY i.issue_date DESC",
      nativeQuery = true)
  List<Invoice> findAuthorizedActiveInvoicesByNumberOrAccessKey(
      @Param("idCompany") Long idCompany,
      @Param("numeroFactura") String numeroFactura,
      @Param("claveAcceso") String claveAcceso);

  /** Busca facturas autorizadas que aún no han sido notificadas por correo */
  @Query(
      "SELECT i FROM Invoice i WHERE "
          + "i.authorizationSri.authorizationStatusSri = 'AUTORIZADO' AND "
          + "(i.emailSent IS NULL OR i.emailSent = false) AND "
          + "i.status = true")
  List<Invoice> findAuthorizedInvoicesNotSentByEmail();

  /** Busca facturas autorizadas que ya fueron notificadas por correo */
  @Query(
      "SELECT i FROM Invoice i WHERE "
          + "i.authorizationSri.authorizationStatusSri = 'AUTORIZADO' AND "
          + "i.emailSent = true AND "
          + "i.status = true")
  List<Invoice> findAuthorizedInvoicesSentByEmail();

  // ===============================================================================
  // QUERIES PARA DASHBOARD WELCOME
  // ===============================================================================

  /** KPI: Monto de ventas del día actual */
  @Query(
      value =
          "SELECT COALESCE(SUM(i.total_amount), 0) "
              + "FROM invoice i "
              + "INNER JOIN authorization_sri a ON i.id_authorization_sri = a.id_authorization_sri "
              + "WHERE i.id_company = :companyId "
              + "AND DATE(i.issue_date) = CURRENT_DATE "
              + "AND a.authorization_status_sri = 'AUTORIZADO' "
              + "AND i.status = true",
      nativeQuery = true)
  java.math.BigDecimal getTodaySalesAmount(@Param("companyId") Long companyId);

  /** KPI: Conteo de facturas del día actual */
  @Query(
      value =
          "SELECT COUNT(*) "
              + "FROM invoice i "
              + "INNER JOIN authorization_sri a ON i.id_authorization_sri = a.id_authorization_sri "
              + "WHERE i.id_company = :companyId "
              + "AND DATE(i.issue_date) = CURRENT_DATE "
              + "AND a.authorization_status_sri = 'AUTORIZADO' "
              + "AND i.status = true",
      nativeQuery = true)
  Long getTodaySalesCount(@Param("companyId") Long companyId);

  /** KPI: Ventas del día anterior (para calcular % cambio) */
  @Query(
      value =
          "SELECT COALESCE(SUM(i.total_amount), 0) "
              + "FROM invoice i "
              + "INNER JOIN authorization_sri a ON i.id_authorization_sri = a.id_authorization_sri "
              + "WHERE i.id_company = :companyId "
              + "AND DATE(i.issue_date) = CURRENT_DATE - INTERVAL '1 day' "
              + "AND a.authorization_status_sri = 'AUTORIZADO' "
              + "AND i.status = true",
      nativeQuery = true)
  java.math.BigDecimal getYesterdaySales(@Param("companyId") Long companyId);

  /** KPI: Monto de ventas de la semana (últimos 7 días) */
  @Query(
      value =
          "SELECT COALESCE(SUM(i.total_amount), 0) "
              + "FROM invoice i "
              + "INNER JOIN authorization_sri a ON i.id_authorization_sri = a.id_authorization_sri "
              + "WHERE i.id_company = :companyId "
              + "AND i.issue_date >= CURRENT_DATE - INTERVAL '7 days' "
              + "AND i.issue_date < CURRENT_DATE + INTERVAL '1 day' "
              + "AND a.authorization_status_sri = 'AUTORIZADO' "
              + "AND i.status = true",
      nativeQuery = true)
  java.math.BigDecimal getWeekSalesAmount(@Param("companyId") Long companyId);

  /** KPI: Conteo de facturas de la semana */
  @Query(
      value =
          "SELECT COUNT(*) "
              + "FROM invoice i "
              + "INNER JOIN authorization_sri a ON i.id_authorization_sri = a.id_authorization_sri "
              + "WHERE i.id_company = :companyId "
              + "AND i.issue_date >= CURRENT_DATE - INTERVAL '7 days' "
              + "AND i.issue_date < CURRENT_DATE + INTERVAL '1 day' "
              + "AND a.authorization_status_sri = 'AUTORIZADO' "
              + "AND i.status = true",
      nativeQuery = true)
  Long getWeekSalesCount(@Param("companyId") Long companyId);

  /** KPI: Monto de ventas del mes actual */
  @Query(
      value =
          "SELECT COALESCE(SUM(i.total_amount), 0) "
              + "FROM invoice i "
              + "INNER JOIN authorization_sri a ON i.id_authorization_sri = a.id_authorization_sri "
              + "WHERE i.id_company = :companyId "
              + "AND DATE_TRUNC('month', i.issue_date) = DATE_TRUNC('month', CURRENT_DATE) "
              + "AND a.authorization_status_sri = 'AUTORIZADO' "
              + "AND i.status = true",
      nativeQuery = true)
  java.math.BigDecimal getMonthSalesAmount(@Param("companyId") Long companyId);

  /** KPI: Conteo de facturas del mes actual */
  @Query(
      value =
          "SELECT COUNT(*) "
              + "FROM invoice i "
              + "INNER JOIN authorization_sri a ON i.id_authorization_sri = a.id_authorization_sri "
              + "WHERE i.id_company = :companyId "
              + "AND DATE_TRUNC('month', i.issue_date) = DATE_TRUNC('month', CURRENT_DATE) "
              + "AND a.authorization_status_sri = 'AUTORIZADO' "
              + "AND i.status = true",
      nativeQuery = true)
  Long getMonthSalesCount(@Param("companyId") Long companyId);

  /** KPI: Ventas del mes anterior (para calcular % cambio) */
  @Query(
      value =
          "SELECT COALESCE(SUM(i.total_amount), 0) as amount "
              + "FROM invoice i "
              + "INNER JOIN authorization_sri a ON i.id_authorization_sri = a.id_authorization_sri "
              + "WHERE i.id_company = :companyId "
              + "AND DATE_TRUNC('month', i.issue_date) = DATE_TRUNC('month', CURRENT_DATE - INTERVAL '1 month') "
              + "AND a.authorization_status_sri = 'AUTORIZADO' "
              + "AND i.status = true",
      nativeQuery = true)
  java.math.BigDecimal getPreviousMonthSales(@Param("companyId") Long companyId);

  /** KPI: Monto pendiente de cobro (total_amount - suma de pagos) */
  @Query(
      value =
          "SELECT COALESCE(SUM(i.total_amount - COALESCE(paid.total_paid, 0)), 0) "
              + "FROM invoice i "
              + "INNER JOIN authorization_sri a ON i.id_authorization_sri = a.id_authorization_sri "
              + "LEFT JOIN (SELECT id_invoice, SUM(total) as total_paid FROM invoice_payment WHERE status = true GROUP BY id_invoice) paid "
              + "  ON i.id_invoice = paid.id_invoice "
              + "WHERE i.id_company = :companyId "
              + "AND (i.total_amount - COALESCE(paid.total_paid, 0)) > 0 "
              + "AND a.authorization_status_sri = 'AUTORIZADO' "
              + "AND i.status = true",
      nativeQuery = true)
  java.math.BigDecimal getPendingCollectionAmount(@Param("companyId") Long companyId);

  /** KPI: Conteo de facturas pendientes de cobro */
  @Query(
      value =
          "SELECT COUNT(*) "
              + "FROM invoice i "
              + "INNER JOIN authorization_sri a ON i.id_authorization_sri = a.id_authorization_sri "
              + "LEFT JOIN (SELECT id_invoice, SUM(total) as total_paid FROM invoice_payment WHERE status = true GROUP BY id_invoice) paid "
              + "  ON i.id_invoice = paid.id_invoice "
              + "WHERE i.id_company = :companyId "
              + "AND (i.total_amount - COALESCE(paid.total_paid, 0)) > 0 "
              + "AND a.authorization_status_sri = 'AUTORIZADO' "
              + "AND i.status = true",
      nativeQuery = true)
  Long getPendingCollectionCount(@Param("companyId") Long companyId);

  /** KPI: Conteo de facturas vencidas (sin campo due_date, se omite por ahora) */
  @Query(value = "SELECT 0", nativeQuery = true)
  Long getOverdueCollectionCount(@Param("companyId") Long companyId);

  /** Tendencia: Ventas por día (últimos 7 días) */
  @Query(
      value =
          """
            SELECT DATE(i.issue_date) as date,
                   COALESCE(SUM(i.total_amount), 0) as amount,
                   COUNT(*) as invoiceCount
            FROM invoice i
            INNER JOIN authorization_sri a ON i.id_authorization_sri = a.id_authorization_sri
            WHERE i.id_company = :companyId
            AND i.issue_date >= CURRENT_DATE - INTERVAL '7 days'
            AND i.issue_date < CURRENT_DATE + INTERVAL '1 day'
            AND a.authorization_status_sri = 'AUTORIZADO'
            AND i.status = true
            GROUP BY DATE(i.issue_date)
            ORDER BY date ASC
            """,
      nativeQuery = true)
  List<Object[]> getSalesTrendLast7Days(@Param("companyId") Long companyId);

  /** Últimas facturas: Top 5 más recientes */
  @Query(
      value =
          "SELECT "
              + "i.id_invoice, "
              + "i.invoice_number, "
              + "i.total_amount, "
              + "i.issue_date, "
              + "a.authorization_status_sri, "
              + "i.id_client "
              + "FROM invoice i "
              + "LEFT JOIN authorization_sri a ON i.id_authorization_sri = a.id_authorization_sri "
              + "WHERE i.id_company = :companyId "
              + "AND i.status = true "
              + "ORDER BY i.issue_date DESC "
              + "LIMIT 5",
      nativeQuery = true)
  List<Object[]> getRecentInvoices(@Param("companyId") Long companyId);

  /** Alerta: Contar facturas rechazadas en los últimos 7 días */
  @Query(
      value =
          "SELECT COUNT(*) "
              + "FROM invoice i "
              + "INNER JOIN authorization_sri a ON i.id_authorization_sri = a.id_authorization_sri "
              + "WHERE i.id_company = :companyId "
              + "AND a.authorization_status_sri = 'DEVUELTA' "
              + "AND DATE(i.issue_date) >= CURRENT_DATE - INTERVAL '7 days' "
              + "AND i.status = true",
      nativeQuery = true)
  Long getRejectedInvoicesCount(@Param("companyId") Long companyId);

  /** Alerta: Ventas de la semana actual para comparar con récord */
  @Query(
      value =
          "SELECT COALESCE(SUM(i.total_amount), 0) "
              + "FROM invoice i "
              + "INNER JOIN authorization_sri a ON i.id_authorization_sri = a.id_authorization_sri "
              + "WHERE i.id_company = :companyId "
              + "AND i.issue_date >= DATE_TRUNC('week', CURRENT_DATE) "
              + "AND a.authorization_status_sri = 'AUTORIZADO' "
              + "AND i.status = true",
      nativeQuery = true)
  java.math.BigDecimal getCurrentWeekSales(@Param("companyId") Long companyId);

  /** Alerta: Mejor semana del mes anterior (para comparar récord) */
  @Query(
      value =
          "SELECT COALESCE(MAX(week_total), 0) "
              + "FROM ( "
              + "  SELECT DATE_TRUNC('week', i.issue_date) as week, SUM(i.total_amount) as week_total "
              + "  FROM invoice i "
              + "  INNER JOIN authorization_sri a ON i.id_authorization_sri = a.id_authorization_sri "
              + "  WHERE i.id_company = :companyId "
              + "  AND DATE_TRUNC('month', i.issue_date) = DATE_TRUNC('month', CURRENT_DATE) "
              + "  AND DATE_TRUNC('week', i.issue_date) < DATE_TRUNC('week', CURRENT_DATE) "
              + "  AND a.authorization_status_sri = 'AUTORIZADO' "
              + "  AND i.status = true "
              + "  GROUP BY DATE_TRUNC('week', i.issue_date) "
              + ") weekly_sales",
      nativeQuery = true)
  java.math.BigDecimal getPreviousBestWeekSales(@Param("companyId") Long companyId);
}
