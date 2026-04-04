package com.wallparisoft.ebill.customer.repository;

import com.wallparisoft.ebill.customer.entity.Establishment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IEstablishmentRepo extends JpaRepository<Establishment, Long> {

  Optional<Establishment> findEstablishmentByIdEstablishmentAndCompany_IdCompany(
      Long idEstablishment, Long idCompany);

  Page<Establishment> findEstablishmentsByCompany_IdCompany(Long idCompany, Pageable pageable);

  Page<Establishment> findEstablishmentsByCompany_IdCompanyAndStatusIsTrue(
      Long idCompany, Pageable pageable);

  List<Establishment> findByCompany_IdCompany(Long idCompany);

  List<Establishment> findByCompany_IdCompanyIn(List<Long> idCompanies);

  /**
   * Valida si existe un establecimiento con el código especificado para la compañía.
   *
   * @param code código del establecimiento (3 dígitos)
   * @param idCompany ID de la compañía
   * @return true si existe, false si no
   */
  boolean existsByCodeAndCompany_IdCompany(String code, Long idCompany);

  /**
   * Busca un establecimiento por código y compañía, excluyendo un ID específico. Útil para
   * validaciones en actualización.
   *
   * @param code código del establecimiento
   * @param idCompany ID de la compañía
   * @param idEstablishment ID del establecimiento a excluir
   * @return Optional con el establecimiento si existe
   */
  Optional<Establishment> findByCodeAndCompany_IdCompanyAndIdEstablishmentNot(
      String code, Long idCompany, Long idEstablishment);

  /**
   * Busca un establecimiento de tipo MATRIZ para una compañía específica.
   *
   * @param idCompany ID de la compañía
   * @param type tipo del establecimiento ("MATRIZ")
   * @return Optional con la MATRIZ si existe
   */
  Optional<Establishment> findByCompany_IdCompanyAndTypeAndStatusIsTrue(
      Long idCompany, String type);
}
