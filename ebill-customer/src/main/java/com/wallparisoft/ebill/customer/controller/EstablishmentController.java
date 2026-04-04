package com.wallparisoft.ebill.customer.controller;

import com.wallparisoft.ebill.customer.dto.EstablishmentRequestDto;
import com.wallparisoft.ebill.customer.dto.EstablishmentResponseDto;
import com.wallparisoft.ebill.customer.service.IEstablishmentService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/establishments")
@RequiredArgsConstructor
public class EstablishmentController {

  private final IEstablishmentService establishmentService;

  @PostMapping
  public ResponseEntity<EstablishmentResponseDto> create(
      @Valid @RequestBody EstablishmentRequestDto dto) {
    EstablishmentResponseDto created = establishmentService.createEstablishment(dto);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @PutMapping("/{id}")
  public ResponseEntity<EstablishmentResponseDto> update(
      @PathVariable Long id,
      @RequestParam Long companyId,
      @Valid @RequestBody EstablishmentRequestDto dto) {
    EstablishmentResponseDto updated = establishmentService.updateEstablishment(id, companyId, dto);
    return ResponseEntity.ok(updated);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deactivate(
      @PathVariable Long id,
      @RequestParam Long companyId,
      @RequestParam(value = "cascade", required = false, defaultValue = "true") boolean cascade) {
    establishmentService.deactivateEstablishment(id, companyId, cascade);
    return ResponseEntity.noContent().build();
  }

  /**
   * GET /api/establishments?companyId=1&page=0&size=10 Lista todos los establecimientos de una
   * compañía (paginado).
   */
  @GetMapping
  public ResponseEntity<Page<EstablishmentResponseDto>> getAllByCompany(
      @RequestParam Long companyId, Pageable pageable) {

    Page<EstablishmentResponseDto> page =
        establishmentService.getAllEstablishmentsByCompanyId(companyId, pageable);
    return ResponseEntity.ok(page);
  }

  /**
   * GET /api/establishments/active?companyId=1&page=0&size=10 Lista establecimientos activos de una
   * compañía (paginado).
   */
  @GetMapping("/active")
  public ResponseEntity<Page<EstablishmentResponseDto>> getActiveByCompany(
      @RequestParam Long companyId, Pageable pageable) {

    Page<EstablishmentResponseDto> page =
        establishmentService.getActiveEstablishmentsByCompanyId(companyId, pageable);
    return ResponseEntity.ok(page);
  }

  /**
   * GET /api/establishments/{id}?companyId=1 Obtiene un establecimiento por su ID y el ID de su
   * compañía.
   */
  @GetMapping("/{id}")
  public ResponseEntity<EstablishmentResponseDto> getByIdAndCompany(
      @PathVariable Long id, @RequestParam Long companyId) {

    EstablishmentResponseDto dto =
        establishmentService.findEstablishmentByIdEstablishmentAndIdCompany(id, companyId);
    return ResponseEntity.ok(dto);
  }

  /**
   * GET /api/establishments/company/{idCompany} Obtiene todas las sucursales de una compañía sin
   * paginación.
   */
  @GetMapping("/company/{idCompany}")
  public ResponseEntity<List<EstablishmentResponseDto>> getAllByCompany(
      @PathVariable Long idCompany) {

    List<EstablishmentResponseDto> establishments =
        establishmentService.getAllEstablishmentsByCompanyId(idCompany);
    return ResponseEntity.ok(establishments);
  }

  /**
   * GET /api/establishments/company/{idCompany}/exists/{code} Valida si existe un establecimiento
   * con el código especificado para la compañía.
   */
  @GetMapping("/company/{idCompany}/exists/{code}")
  public ResponseEntity<Boolean> existsByCodeAndCompany(
      @PathVariable Long idCompany, @PathVariable String code) {

    boolean exists = establishmentService.existsByCodeAndCompany(idCompany, code);
    return ResponseEntity.ok(exists);
  }
}
