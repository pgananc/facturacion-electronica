package com.wallparisoft.ebillbilling.controller;

import static com.wallparisoft.ebill.utils.LogUtil.setLogs;
import static lombok.AccessLevel.PRIVATE;

import com.wallparisoft.ebill.utils.log.EventType;
import com.wallparisoft.ebill.utils.log.Level;
import com.wallparisoft.ebillbilling.dto.GuideDto;
import com.wallparisoft.ebillbilling.entity.Guide;
import com.wallparisoft.ebillbilling.response.GuideResponse;
import com.wallparisoft.ebillbilling.service.IGuideService;
import java.nio.file.Path;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/guides")
@FieldDefaults(level = PRIVATE)
@Slf4j
public class GuideController {

  final IGuideService guideService;

  /**
   * Lista todas las guías de remisión
   *
   * @return Lista de guías de remisión
   */
  @GetMapping
  public ResponseEntity<List<com.wallparisoft.ebillbilling.dto.GuideListResponseDto>>
      getAllGuides() {
    List<com.wallparisoft.ebillbilling.dto.GuideListResponseDto> guides =
        guideService.listWithClientData();

    log.info("📋 [GET /api/guides] Total registros: {}", guides.size());
    for (var g : guides) {
      log.info(
          "📄 GR ID={}, number={}, status={}, originAddress={}, destinationAddress={}, "
              + "buyerName={}, carrierName={}, authorizationSri={}",
          g.getIdGuide(),
          g.getGuideNumber(),
          g.getGuideStatus(),
          g.getOriginAddress(),
          g.getDestinationAddress(),
          g.getBuyerName(),
          g.getCarrierName(),
          g.getGuideStatus());
    }

    return new ResponseEntity<>(guides, HttpStatus.OK);
  }

  /**
   * Obtiene una guía de remisión por ID
   *
   * @param id ID de la guía
   * @return Guía de remisión
   */
  @GetMapping("/{id}")
  public ResponseEntity<GuideDto> getGuideById(@PathVariable("id") Long id) {
    GuideDto guideDto = guideService.getGuideById(id);
    if (guideDto == null) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(guideDto, HttpStatus.OK);
  }

  /**
   * Crea y envía una guía de remisión al SRI
   *
   * @param guideDto DTO con los datos de la guía
   * @return GuideResponse con el resultado
   */
  @PostMapping
  public GuideResponse createGuide(@Validated @RequestBody GuideDto guideDto) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        EventType.REQUEST,
        Level.LEVEL_001);

    GuideResponse response = guideService.saveGuide(guideDto);
    setLogs(
        com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(response),
        EventType.RESPONSE,
        Level.LEVEL_001);
    return response;
  }

  /**
   * Actualiza una guía de remisión (solo borradores)
   *
   * @param id ID de la guía
   * @param guideDto DTO con los datos actualizados
   * @return GuideResponse con el resultado
   */
  @PutMapping("/{id}")
  public ResponseEntity<GuideResponse> updateGuide(
      @PathVariable("id") Long id, @Validated @RequestBody GuideDto guideDto) {
    try {
      GuideResponse response = guideService.updateGuide(id, guideDto);
      return new ResponseEntity<>(response, HttpStatus.OK);
    } catch (IllegalArgumentException | IllegalStateException e) {
      log.error("Error updating guide: {}", e.getMessage());
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
  }

  /**
   * Elimina una guía de remisión (solo borradores)
   *
   * @param id ID de la guía
   * @return ResponseEntity con el resultado
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteGuide(@PathVariable("id") Long id) {
    try {
      boolean deleted = guideService.delete(id.intValue());
      if (deleted) {
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
      } else {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
      }
    } catch (IllegalStateException e) {
      log.error("Error deleting guide: {}", e.getMessage());
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
  }

  /**
   * Busca guías de remisión con filtros
   *
   * @param guideNumber Número de guía (opcional)
   * @param idClient ID del cliente (opcional)
   * @param idCarrier ID del transportista (opcional)
   * @param pageable Paginación
   * @return Página de guías
   */
  @GetMapping("/search")
  public ResponseEntity<Page<Guide>> searchGuides(
      @RequestParam(required = false) String guideNumber,
      @RequestParam(required = false) Long idClient,
      @RequestParam(required = false) Long idCarrier,
      Pageable pageable) {
    Page<Guide> guides = guideService.searchGuides(guideNumber, idClient, idCarrier, pageable);
    return new ResponseEntity<>(guides, HttpStatus.OK);
  }

  /**
   * Busca guías de remisión por idCompany (obligatorio) y filtros opcionales con paginación.
   * Retorna un DTO optimizado para listado con datos del cliente (destinatario) y transportista.
   *
   * @param searchDto DTO con los criterios de búsqueda (idCompany obligatorio)
   * @param pageable Paginación
   * @return Página de GuideSearchResponseDto
   */
  @PostMapping("/search-by-company")
  public ResponseEntity<Page<com.wallparisoft.ebillbilling.dto.GuideSearchResponseDto>>
      searchGuidesByCompany(
          @Validated @RequestBody
              com.wallparisoft.ebillbilling.dto.GuideSearchByCompanyDto searchDto,
          Pageable pageable) {
    try {
      Page<com.wallparisoft.ebillbilling.dto.GuideSearchResponseDto> result =
          guideService.searchGuidesByCompany(searchDto, pageable);

      log.info(
          "📋 [POST /api/guide/search-by-company] idCompany: {}, guideNumber: {}, Total: {}, Página: {}",
          searchDto.getIdCompany(),
          searchDto.getGuideNumber(),
          result.getTotalElements(),
          result.getNumber());

      return new ResponseEntity<>(result, HttpStatus.OK);
    } catch (IllegalArgumentException e) {
      log.error("Error en búsqueda de guías de remisión: {}", e.getMessage());
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
  }

  /**
   * Guarda una guía de remisión como borrador (sin enviar al SRI)
   *
   * @param guideDto DTO con los datos de la guía
   * @return GuideResponse con el resultado
   */
  @PostMapping("/draft")
  public GuideResponse saveDraft(@Validated @RequestBody GuideDto guideDto) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        EventType.REQUEST,
        Level.LEVEL_001);

    GuideResponse response = guideService.saveGuideDraft(guideDto);
    setLogs(
        com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(response),
        EventType.RESPONSE,
        Level.LEVEL_001);
    return response;
  }

  /**
   * Envía un borrador de guía de remisión al SRI
   *
   * @param id ID de la guía
   * @param password Contraseña de la firma digital
   * @return GuideResponse con el resultado
   */
  @PostMapping("/{id}/send-to-sri")
  public ResponseEntity<GuideResponse> sendGuideToSri(
      @PathVariable("id") Long id, @RequestParam String password) {
    try {
      GuideResponse response = guideService.sendGuideToSri(id, password);
      return new ResponseEntity<>(response, HttpStatus.OK);
    } catch (UnsupportedOperationException e) {
      log.error("Envío al SRI no implementado: {}", e.getMessage());
      return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    } catch (Exception e) {
      log.error("Error sending guide to SRI: {}", e.getMessage());
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Descarga el PDF de una guía de remisión
   *
   * @param id ID de la guía
   * @return Archivo PDF
   */
  @GetMapping("/{id}/pdf")
  public ResponseEntity<Resource> downloadPdf(@PathVariable("id") Long id) {
    try {
      Path pdfPath = guideService.getPdfFilePath(id);
      if (pdfPath == null || !java.nio.file.Files.exists(pdfPath)) {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
      }

      Resource resource = new PathResource(pdfPath);
      return ResponseEntity.ok()
          .header(
              HttpHeaders.CONTENT_DISPOSITION,
              "attachment; filename=\"" + resource.getFilename() + "\"")
          .contentType(MediaType.APPLICATION_PDF)
          .body(resource);
    } catch (Exception e) {
      log.error("Error downloading PDF: {}", e.getMessage());
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Descarga el XML de una guía de remisión
   *
   * @param id ID de la guía
   * @return Archivo XML
   */
  @GetMapping("/{id}/xml")
  public ResponseEntity<Resource> downloadXml(@PathVariable("id") Long id) {
    try {
      Path xmlPath = guideService.getXmlFilePath(id);
      if (xmlPath == null || !java.nio.file.Files.exists(xmlPath)) {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
      }

      Resource resource = new PathResource(xmlPath);
      return ResponseEntity.ok()
          .header(
              HttpHeaders.CONTENT_DISPOSITION,
              "attachment; filename=\"" + resource.getFilename() + "\"")
          .contentType(MediaType.APPLICATION_XML)
          .body(resource);
    } catch (Exception e) {
      log.error("Error downloading XML: {}", e.getMessage());
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Envía la guía de remisión por email al cliente
   *
   * @param id ID de la guía
   * @param email Email alternativo (opcional)
   * @return ResponseEntity con el resultado
   */
  @PostMapping("/{id}/send-email")
  public ResponseEntity<Void> sendGuideByEmail(
      @PathVariable("id") Long id, @RequestParam(required = false) String email) {
    try {
      boolean sent = guideService.sendGuideByEmail(id, email);
      if (sent) {
        return new ResponseEntity<>(HttpStatus.OK);
      } else {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
      }
    } catch (Exception e) {
      log.error("Error sending guide by email: {}", e.getMessage());
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
