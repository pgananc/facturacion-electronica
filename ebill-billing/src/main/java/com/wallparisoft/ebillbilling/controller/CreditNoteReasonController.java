package com.wallparisoft.ebillbilling.controller;

import com.wallparisoft.ebillbilling.entity.CreditNoteReason;
import com.wallparisoft.ebillbilling.repository.ICreditNoteReasonRepo;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controlador para el catálogo de motivos de nota de crédito según SRI Ecuador. */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/credit-note-reason")
@Slf4j
public class CreditNoteReasonController {

  private final ICreditNoteReasonRepo creditNoteReasonRepo;

  /**
   * Lista todos los motivos de nota de crédito activos.
   *
   * @return Lista de motivos disponibles
   */
  @GetMapping
  public ResponseEntity<List<CreditNoteReason>> getAllReasons() {
    log.debug("Obteniendo catálogo de motivos de nota de crédito");
    List<CreditNoteReason> reasons = creditNoteReasonRepo.findByStatusTrue();
    return new ResponseEntity<>(reasons, HttpStatus.OK);
  }
}
