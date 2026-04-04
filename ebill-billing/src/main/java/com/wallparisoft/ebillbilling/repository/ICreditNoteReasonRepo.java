package com.wallparisoft.ebillbilling.repository;

import com.wallparisoft.ebillbilling.entity.CreditNoteReason;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio para el catálogo de motivos de nota de crédito. */
public interface ICreditNoteReasonRepo extends JpaRepository<CreditNoteReason, Long> {

  /**
   * Busca un motivo por su código.
   *
   * @param code Código del motivo (01-05)
   * @return Optional con el motivo si existe
   */
  Optional<CreditNoteReason> findByCode(String code);

  /**
   * Lista todos los motivos activos.
   *
   * @return Lista de motivos activos
   */
  List<CreditNoteReason> findByStatusTrue();
}
