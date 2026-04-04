package com.wallparisoft.ebill.customer.controller;

import com.wallparisoft.ebill.customer.dto.EstablishmentWithIssuancePointDto;
import com.wallparisoft.ebill.customer.dto.InvoiceIssuancePointRequestDto;
import com.wallparisoft.ebill.customer.dto.InvoiceIssuancePointResponseDto;
import com.wallparisoft.ebill.customer.dto.InvoiceIssuancePointWithEstablishmentDto;
import com.wallparisoft.ebill.customer.dto.UpdateSequentialRequestDto;
import com.wallparisoft.ebill.customer.service.IInvoiceIssuancePointService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invoice-issuance-points")
@RequiredArgsConstructor
public class InvoiceIssuancePointController {

  private final IInvoiceIssuancePointService invoiceIssuancePointService;

  @PostMapping
  public ResponseEntity<InvoiceIssuancePointResponseDto> create(
      @Valid @RequestBody InvoiceIssuancePointRequestDto dto) {
    return ResponseEntity.status(HttpStatus.CREATED).body(invoiceIssuancePointService.create(dto));
  }

  @PutMapping("/{id}")
  public ResponseEntity<InvoiceIssuancePointResponseDto> update(
      @PathVariable Long id,
      @RequestParam Long establishmentId,
      @Valid @RequestBody InvoiceIssuancePointRequestDto dto) {
    return ResponseEntity.ok(invoiceIssuancePointService.update(id, establishmentId, dto));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deactivate(
      @PathVariable Long id, @RequestParam Long establishmentId) {
    invoiceIssuancePointService.deactivate(id, establishmentId);
    return ResponseEntity.noContent().build();
  }

  /**
   * GET /api/invoice-issuance-points/{id}?establishmentId={id} Obtiene un punto de emisión por ID y
   * ID del establecimiento.
   */
  @GetMapping("/{id}")
  public ResponseEntity<InvoiceIssuancePointResponseDto> getByIdAndEstablishment(
      @PathVariable Long id, @RequestParam Long establishmentId) {

    return invoiceIssuancePointService
        .findByIdAndEstablishment(id, establishmentId)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * GET /api/invoice-issuance-points?establishmentId={id}&page=0&size=10 Lista todos los puntos de
   * emisión por establecimiento (paginado).
   */
  @GetMapping
  public ResponseEntity<Page<InvoiceIssuancePointResponseDto>> getAllByEstablishment(
      @RequestParam Long establishmentId, Pageable pageable) {

    Page<InvoiceIssuancePointResponseDto> result =
        invoiceIssuancePointService.findAllByEstablishment(establishmentId, pageable);
    return ResponseEntity.ok(result);
  }

  /**
   * GET /api/invoice-issuance-points/active?establishmentId={id}&page=0&size=10 Lista puntos de
   * emisión activos por establecimiento (paginado).
   */
  @GetMapping("/active")
  public ResponseEntity<Page<InvoiceIssuancePointResponseDto>> getActiveByEstablishment(
      @RequestParam Long establishmentId, Pageable pageable) {

    Page<InvoiceIssuancePointResponseDto> result =
        invoiceIssuancePointService.findAllActiveByEstablishment(establishmentId, pageable);
    return ResponseEntity.ok(result);
  }

  /**
   * GET /api/invoice-issuance-points/available Devuelve puntos de emisión disponibles por lista de
   * sucursales.
   */
  @GetMapping("/available")
  public ResponseEntity<List<InvoiceIssuancePointResponseDto>> findAvailableByEstablishments(
      @RequestParam("idEstablishments") List<Long> idEstablishments) {

    List<InvoiceIssuancePointResponseDto> available =
        invoiceIssuancePointService.findAvailableByEstablishments(idEstablishments);
    return ResponseEntity.ok(available);
  }

  @GetMapping("/current-session")
  public ResponseEntity<InvoiceIssuancePointResponseDto> getCurrentSession(
      @RequestParam String userName) {
    return ResponseEntity.ok(invoiceIssuancePointService.getCurrentSession(userName));
  }

  /**
   * PUT /api/invoice-issuance-points/{code}/sequential?establishmentId={id} Actualiza el secuencial
   * de un punto de emisión basado en su código y establecimiento.
   *
   * @param code código del punto de emisión (ej: "001")
   * @param establishmentId ID del establecimiento al que pertenece
   * @param dto contiene el nuevo valor del secuencial
   * @return punto de emisión actualizado
   */
  @PutMapping("/{code}/sequential")
  public ResponseEntity<InvoiceIssuancePointResponseDto> updateSequential(
      @PathVariable String code,
      @RequestParam Long establishmentId,
      @Valid @RequestBody UpdateSequentialRequestDto dto) {

    InvoiceIssuancePointResponseDto updated =
        invoiceIssuancePointService.updateSequentialByCode(
            code, establishmentId, dto.getSequentialInvoice());

    return ResponseEntity.ok(updated);
  }

  /**
   * GET /api/invoice-issuance-points/establishment/{establishmentId} Obtiene todos los puntos de
   * emisión de un establecimiento sin paginación.
   */
  @GetMapping("/establishment/{establishmentId}")
  public ResponseEntity<List<InvoiceIssuancePointResponseDto>> getAllByEstablishment(
      @PathVariable Long establishmentId) {

    List<InvoiceIssuancePointResponseDto> points =
        invoiceIssuancePointService.findAllByEstablishment(establishmentId);
    return ResponseEntity.ok(points);
  }

  /**
   * GET /api/invoice-issuance-points/company/{idCompany} Obtiene todos los puntos de emisión de una
   * compañía (todos los establecimientos) con información del establecimiento.
   */
  @GetMapping("/company/{idCompany}")
  public ResponseEntity<List<InvoiceIssuancePointWithEstablishmentDto>> getAllByCompany(
      @PathVariable Long idCompany) {

    List<InvoiceIssuancePointWithEstablishmentDto> points =
        invoiceIssuancePointService.findAllByCompany(idCompany);
    return ResponseEntity.ok(points);
  }

  /**
   * GET /api/invoice-issuance-points/establishment/{establishmentId}/exists/{code} Valida si existe
   * un punto de emisión con el código especificado para el establecimiento.
   */
  @GetMapping("/establishment/{establishmentId}/exists/{code}")
  public ResponseEntity<Boolean> existsByCodeAndEstablishment(
      @PathVariable Long establishmentId, @PathVariable String code) {

    boolean exists =
        invoiceIssuancePointService.existsByCodeAndEstablishment(establishmentId, code);
    return ResponseEntity.ok(exists);
  }

  /**
   * GET /api/invoice-issuance-points/{idIssuancePoint}/with-establishment Obtiene un Establishment
   * con su InvoiceIssuancePoint asociado por el ID del punto de emisión. Esta estructura es
   * compatible con EstablishmentDto de ebill-billing.
   *
   * @param idIssuancePoint ID del punto de emisión
   * @return DTO con la información del establecimiento y su punto de emisión
   */
  @GetMapping("/{idIssuancePoint}/with-establishment")
  public ResponseEntity<EstablishmentWithIssuancePointDto> getEstablishmentWithIssuancePoint(
      @PathVariable Long idIssuancePoint) {

    EstablishmentWithIssuancePointDto dto =
        invoiceIssuancePointService.getEstablishmentWithIssuancePointById(idIssuancePoint);
    return ResponseEntity.ok(dto);
  }
}
