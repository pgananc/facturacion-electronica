package com.wallparisoft.ebill.customer.repository;

import com.wallparisoft.ebill.customer.entity.InvoiceIssuancePoint;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IInvoiceIssuancePointRepo extends JpaRepository<InvoiceIssuancePoint, Long> {

  Optional<InvoiceIssuancePoint>
      findInvoiceIssuancePointByIdInvoiceIssuancePointAndEstablishment_IdEstablishment(
          Long idInvoiceIssuancePoint, Long idEstablishment);

  Page<InvoiceIssuancePoint> findInvoiceIssuancePointsByEstablishment_IdEstablishment(
      Long idEstablishment, Pageable pageable);

  Page<InvoiceIssuancePoint>
      findInvoiceIssuancePointsByEstablishment_IdEstablishmentAndStatusIsTrue(
          Long idEstablishment, Pageable pageable);

  @Query(
      "SELECT p FROM InvoiceIssuancePoint p "
          + "WHERE p.establishment.idEstablishment IN :ids "
          + "AND p.status = true")
  List<InvoiceIssuancePoint> findAllActiveByEstablishments(
      @Param("ids") List<Long> establishmentIds);

  /**
   * Busca un punto de emisión por su código (code), establecimiento y estado activo.
   *
   * @param code código del punto de emisión
   * @param idEstablishment ID del establecimiento
   * @return punto de emisión si existe y está activo
   */
  Optional<InvoiceIssuancePoint> findByCodeAndEstablishment_IdEstablishmentAndStatusIsTrue(
      String code, Long idEstablishment);

  /**
   * Cuenta el número de puntos de emisión activos para un establecimiento.
   *
   * @param idEstablishment ID del establecimiento
   * @return cantidad de puntos activos
   */
  long countByEstablishment_IdEstablishmentAndStatusIsTrue(Long idEstablishment);

  /**
   * Valida si existe un punto de emisión con el código especificado para el establecimiento.
   *
   * @param code código del punto (3 dígitos)
   * @param idEstablishment ID del establecimiento
   * @return true si existe, false si no
   */
  boolean existsByCodeAndEstablishment_IdEstablishment(String code, Long idEstablishment);

  /**
   * Busca un punto de emisión por código y establecimiento, excluyendo un ID específico. Útil para
   * validaciones en actualización.
   *
   * @param code código del punto
   * @param idEstablishment ID del establecimiento
   * @param idInvoiceIssuancePoint ID del punto a excluir
   * @return Optional con el punto si existe
   */
  Optional<InvoiceIssuancePoint>
      findByCodeAndEstablishment_IdEstablishmentAndIdInvoiceIssuancePointNot(
          String code, Long idEstablishment, Long idInvoiceIssuancePoint);

  /**
   * Busca todos los puntos de emisión de una compañía (todos los establecimientos).
   *
   * @param idCompany ID de la compañía
   * @return lista de puntos de emisión
   */
  @Query(
      "SELECT p FROM InvoiceIssuancePoint p "
          + "WHERE p.establishment.company.idCompany = :idCompany")
  List<InvoiceIssuancePoint> findAllByCompanyId(@Param("idCompany") Long idCompany);

  /**
   * Busca todos los puntos de emisión de un establecimiento sin paginación.
   *
   * @param idEstablishment ID del establecimiento
   * @return lista de puntos de emisión
   */
  List<InvoiceIssuancePoint> findByEstablishment_IdEstablishment(Long idEstablishment);

  /**
   * Busca un punto de emisión por su ID.
   *
   * @param idInvoiceIssuancePoint ID del punto de emisión
   * @return Optional con el punto de emisión si existe
   */
  Optional<InvoiceIssuancePoint> findById(Long idInvoiceIssuancePoint);
}
