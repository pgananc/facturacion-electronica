package com.wallparisoft.ebillbilling.controller;

import com.wallparisoft.ebillbilling.dto.CarrierDto;
import com.wallparisoft.ebillbilling.service.ICarrierService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para gestionar transportistas (carriers).
 *
 * <p>Los transportistas son entidades asociadas a empresas que se utilizan en las Guías de Remisión
 * para identificar quién transporta los productos.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/carrier")
@Slf4j
public class CarrierController {

  private final ICarrierService carrierService;

  /**
   * Obtiene todos los transportistas de una empresa con paginación.
   *
   * @param idCompany ID de la empresa (obligatorio)
   * @param activeOnly Si es true, solo retorna transportistas activos
   * @param pageable Paginación (page, size, sort)
   * @return Página de transportistas
   */
  @GetMapping
  public ResponseEntity<Page<CarrierDto>> getCarriers(
      @RequestParam("idCompany") Long idCompany,
      @RequestParam(value = "activeOnly", defaultValue = "false") Boolean activeOnly,
      @PageableDefault(size = 10) Pageable pageable) {
    log.info(
        "Obteniendo transportistas para empresa: {}, activeOnly: {}, page: {}, size: {}",
        idCompany,
        activeOnly,
        pageable.getPageNumber(),
        pageable.getPageSize());

    Page<CarrierDto> carriers;
    if (Boolean.TRUE.equals(activeOnly)) {
      carriers = carrierService.getActiveCarriersByCompany(idCompany, pageable);
    } else {
      carriers = carrierService.getCarriersByCompany(idCompany, pageable);
    }

    log.info(
        "Encontrados {} transportistas (total: {})",
        carriers.getNumberOfElements(),
        carriers.getTotalElements());
    return new ResponseEntity<>(carriers, HttpStatus.OK);
  }

  /**
   * Obtiene un transportista por ID.
   *
   * @param idCarrier ID del transportista
   * @return Transportista o 404 si no existe
   */
  @GetMapping("/{id}")
  public ResponseEntity<CarrierDto> getCarrierById(@PathVariable("id") Long idCarrier) {
    log.info("Obteniendo transportista por ID: {}", idCarrier);

    CarrierDto carrier = carrierService.getCarrierById(idCarrier);
    if (carrier == null) {
      log.warn("Transportista no encontrado: {}", idCarrier);
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    return new ResponseEntity<>(carrier, HttpStatus.OK);
  }

  /**
   * Crea un nuevo transportista.
   *
   * @param carrierDto DTO con los datos del transportista
   * @return Transportista creado
   */
  @PostMapping
  public ResponseEntity<?> createCarrier(@Valid @RequestBody CarrierDto carrierDto) {
    log.info("Creando nuevo transportista: {}", carrierDto.getName());

    try {
      CarrierDto created = carrierService.createCarrier(carrierDto);
      log.info("Transportista creado con ID: {}", created.getIdCarrier());
      return new ResponseEntity<>(created, HttpStatus.CREATED);
    } catch (IllegalArgumentException e) {
      log.error("Error creando transportista: {}", e.getMessage());
      Map<String, Object> errorResponse = new java.util.HashMap<>();
      errorResponse.put("code", HttpStatus.BAD_REQUEST.value());
      errorResponse.put("status", "ERROR");
      errorResponse.put("message", e.getMessage());
      return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
  }

  /**
   * Actualiza un transportista existente.
   *
   * @param idCarrier ID del transportista
   * @param carrierDto DTO con los datos actualizados
   * @return Transportista actualizado
   */
  @PutMapping("/{id}")
  public ResponseEntity<?> updateCarrier(
      @PathVariable("id") Long idCarrier, @Valid @RequestBody CarrierDto carrierDto) {
    log.info("Actualizando transportista ID: {}", idCarrier);

    try {
      CarrierDto updated = carrierService.updateCarrier(idCarrier, carrierDto);
      return new ResponseEntity<>(updated, HttpStatus.OK);
    } catch (IllegalArgumentException e) {
      log.error("Error actualizando transportista: {}", e.getMessage());
      Map<String, Object> errorResponse = new java.util.HashMap<>();
      errorResponse.put("code", HttpStatus.BAD_REQUEST.value());
      errorResponse.put("status", "ERROR");
      errorResponse.put("message", e.getMessage());
      return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
  }

  /**
   * Elimina (desactiva) un transportista.
   *
   * @param idCarrier ID del transportista
   * @return 204 No Content si se eliminó correctamente
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteCarrier(@PathVariable("id") Long idCarrier) {
    log.info("Eliminando transportista ID: {}", idCarrier);

    try {
      boolean deleted = carrierService.deleteCarrier(idCarrier);
      if (deleted) {
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
      } else {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
      }
    } catch (IllegalArgumentException e) {
      log.error("Error eliminando transportista: {}", e.getMessage());
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }
}
