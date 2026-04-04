package com.wallparisoft.ebill.customer.repository;

import com.wallparisoft.ebill.customer.entity.CompanyClient;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ICompanyClientRepo extends JpaRepository<CompanyClient, Long> {
  Optional<CompanyClient> findByCompanyIdentificationAndClient_IdClient(
      @Param("companyIdentification") String companyIdentification,
      @Param("idClient") Long idClient);

  Page<CompanyClient> findByCompanyIdentification(
      @Param("companyIdentification") String companyIdentification, Pageable pageable);

  Optional<List<CompanyClient>> findByCompanyIdentification(
      @Param("companyIdentification") String companyIdentification);

  /**
   * Verifica si existe un cliente con la identificación especificada para una compañía. Busca en la
   * relación CompanyClient y verifica el campo identification del Client asociado.
   */
  @Query(
      "SELECT CASE WHEN COUNT(cc) > 0 THEN true ELSE false END "
          + "FROM CompanyClient cc "
          + "WHERE cc.companyIdentification = :companyIdentification "
          + "AND cc.client.identification = :clientIdentification")
  boolean existsByCompanyIdentificationAndClientIdentification(
      @Param("companyIdentification") String companyIdentification,
      @Param("clientIdentification") String clientIdentification);

  /**
   * Busca la relación CompanyClient por companyIdentification y la identificación del cliente
   * (cédula/RUC). Útil para encontrar clientes existentes (activos o inactivos) antes de crear
   * duplicados.
   */
  @Query(
      "SELECT cc FROM CompanyClient cc "
          + "WHERE cc.companyIdentification = :companyIdentification "
          + "AND cc.client.identification = :clientIdentification")
  Optional<CompanyClient> findByCompanyIdentificationAndClientIdentification(
      @Param("companyIdentification") String companyIdentification,
      @Param("clientIdentification") String clientIdentification);

  Optional<CompanyClient> findByClient_IdClient(@Param("idClient") Long idClient);
}
