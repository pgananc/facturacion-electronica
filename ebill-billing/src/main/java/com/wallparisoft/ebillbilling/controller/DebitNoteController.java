package com.wallparisoft.ebillbilling.controller;

import static com.wallparisoft.ebill.utils.LogUtil.setLogs;
import static lombok.AccessLevel.PRIVATE;

import com.wallparisoft.ebill.utils.log.EventType;
import com.wallparisoft.ebill.utils.log.Level;
import com.wallparisoft.ebillbilling.dto.DebitNoteDto;
import com.wallparisoft.ebillbilling.entity.DebitNote;
import com.wallparisoft.ebillbilling.response.DebitNoteResponse;
import com.wallparisoft.ebillbilling.service.IDebitNoteService;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
@RequestMapping("/api/debit-notes")
@FieldDefaults(level = PRIVATE)
@Slf4j
public class DebitNoteController {

  final IDebitNoteService debitNoteService;

  /**
   * Lista todas las notas de débito
   *
   * @return Lista de notas de débito
   */
  @GetMapping
  public ResponseEntity<List<com.wallparisoft.ebillbilling.dto.DebitNoteListResponseDto>>
      getAllDebitNotes() {
    List<com.wallparisoft.ebillbilling.dto.DebitNoteListResponseDto> debitNotes =
        debitNoteService.listWithClientData();

    log.info("📋 [GET /api/debit-notes] Total registros: {}", debitNotes.size());
    for (var dn : debitNotes) {
      log.info(
          "📄 ND ID={}, number={}, status={}, totalAmount={}, originalInvoiceNumber={}, "
              + "buyerName={}, buyerIdentification={}, authorizationSri={}",
          dn.getIdDebitNote(),
          dn.getDebitNoteNumber(),
          dn.getDebitNoteStatus(),
          dn.getTotalAmount(),
          dn.getOriginalInvoiceNumber(),
          dn.getBuyerName(),
          dn.getBuyerIdentification(),
          dn.getAuthorizationSri() != null
              ? dn.getAuthorizationSri().getAuthorizationStatusSri()
              : "null");
    }

    return new ResponseEntity<>(debitNotes, HttpStatus.OK);
  }

  /**
   * Obtiene un borrador de nota de débito por ID. Solo retorna notas de débito que no han sido
   * autorizadas por el SRI.
   *
   * @param id ID de la nota de débito borrador
   * @return DTO con datos completos de la nota de débito borrador
   */
  @GetMapping("/draft/{id}")
  public ResponseEntity<?> getDraftById(@PathVariable("id") Long id) {
    try {
      DebitNoteDto debitNoteDto = debitNoteService.getDraftDebitNoteById(id);
      return new ResponseEntity<>(debitNoteDto, HttpStatus.OK);
    } catch (IllegalArgumentException e) {
      log.error("Error obteniendo borrador de ND: {}", e.getMessage());
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("code", 400);
      errorResponse.put("status", "ERROR");
      errorResponse.put("message", e.getMessage());
      return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
  }

  /**
   * Obtiene una nota de débito por ID
   *
   * @param id ID de la nota de débito
   * @return Nota de débito
   */
  @GetMapping("/{id}")
  public ResponseEntity<DebitNoteDto> getDebitNoteById(@PathVariable("id") Long id) {
    DebitNoteDto debitNoteDto = debitNoteService.getDebitNoteById(id);
    if (debitNoteDto == null) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(debitNoteDto, HttpStatus.OK);
  }

  /**
   * Crea y envía una nota de débito al SRI
   *
   * @param debitNoteDto DTO con los datos de la nota de débito
   * @return DebitNoteResponse con el resultado
   */
  @PostMapping
  public DebitNoteResponse createDebitNote(@Validated @RequestBody DebitNoteDto debitNoteDto) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        EventType.REQUEST,
        Level.LEVEL_001);

    DebitNoteResponse response = debitNoteService.saveDebitNote(debitNoteDto);
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
   * Actualiza una nota de débito (solo borradores)
   *
   * @param id ID de la nota de débito
   * @param debitNoteDto DTO con los datos actualizados
   * @return DebitNoteResponse con el resultado
   */
  @PutMapping("/{id}")
  public ResponseEntity<DebitNoteResponse> updateDebitNote(
      @PathVariable("id") Long id, @Validated @RequestBody DebitNoteDto debitNoteDto) {
    try {
      DebitNoteResponse response = debitNoteService.updateDebitNote(id, debitNoteDto);
      return new ResponseEntity<>(response, HttpStatus.OK);
    } catch (IllegalArgumentException | IllegalStateException e) {
      log.error("Error updating debit note: {}", e.getMessage());
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
  }

  /**
   * Elimina una nota de débito (solo borradores)
   *
   * @param id ID de la nota de débito
   * @return ResponseEntity con el resultado
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteDebitNote(@PathVariable("id") Integer id) {
    try {
      boolean deleted = debitNoteService.delete(id);
      if (deleted) {
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
      } else {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
      }
    } catch (IllegalStateException e) {
      log.error("Error deleting debit note: {}", e.getMessage());
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
  }

  /**
   * Busca notas de débito con filtros
   *
   * @param debitNoteNumber Número de nota de débito (opcional)
   * @param idClient ID del cliente (opcional)
   * @param originalInvoiceId ID de la factura original (opcional)
   * @param pageable Paginación
   * @return Página de notas de débito
   */
  @GetMapping("/search")
  public ResponseEntity<Page<DebitNote>> searchDebitNotes(
      @RequestParam(required = false) String debitNoteNumber,
      @RequestParam(required = false) Long idClient,
      @RequestParam(required = false) Long originalInvoiceId,
      Pageable pageable) {
    Page<DebitNote> debitNotes =
        debitNoteService.searchDebitNotes(debitNoteNumber, idClient, originalInvoiceId, pageable);
    return new ResponseEntity<>(debitNotes, HttpStatus.OK);
  }

  /**
   * Busca notas de débito por idCompany (obligatorio) y filtros opcionales con paginación. Retorna
   * un DTO optimizado para listado con datos del cliente.
   *
   * @param searchDto DTO con los criterios de búsqueda (idCompany obligatorio)
   * @param pageable Paginación
   * @return Página de DebitNoteListResponseDto
   */
  @PostMapping("/search-by-company")
  public ResponseEntity<Page<com.wallparisoft.ebillbilling.dto.DebitNoteListResponseDto>>
      searchDebitNotesByCompany(
          @Validated @RequestBody
              com.wallparisoft.ebillbilling.dto.DebitNoteSearchByCompanyDto searchDto,
          Pageable pageable) {
    try {
      Page<com.wallparisoft.ebillbilling.dto.DebitNoteListResponseDto> result =
          debitNoteService.searchDebitNotesByCompany(searchDto, pageable);

      log.info(
          "📋 [POST /api/debit-notes/search-by-company] idCompany: {}, debitNoteNumber: {}, originalInvoiceNumber: {}, Total: {}, Página: {}",
          searchDto.getIdCompany(),
          searchDto.getDebitNoteNumber(),
          searchDto.getOriginalInvoiceNumber(),
          result.getTotalElements(),
          result.getNumber());

      return new ResponseEntity<>(result, HttpStatus.OK);
    } catch (IllegalArgumentException e) {
      log.error("Error en búsqueda de notas de débito: {}", e.getMessage());
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
  }

  /**
   * Obtiene todas las notas de débito de una factura original
   *
   * @param originalInvoiceId ID de la factura original
   * @return Lista de notas de débito
   */
  @GetMapping("/by-invoice/{originalInvoiceId}")
  public ResponseEntity<List<DebitNote>> getDebitNotesByOriginalInvoice(
      @PathVariable("originalInvoiceId") Long originalInvoiceId) {
    List<DebitNote> debitNotes = debitNoteService.getDebitNotesByOriginalInvoice(originalInvoiceId);
    return new ResponseEntity<>(debitNotes, HttpStatus.OK);
  }

  /**
   * Guarda una nota de débito como borrador (sin enviar al SRI)
   *
   * @param debitNoteDto DTO con los datos de la nota de débito
   * @return DebitNoteResponse con el resultado
   */
  @PostMapping("/draft")
  public DebitNoteResponse saveDraft(@Validated @RequestBody DebitNoteDto debitNoteDto) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information("Guardando ND como borrador"),
        EventType.REQUEST,
        Level.LEVEL_001);

    DebitNoteResponse response = debitNoteService.saveDebitNoteDraft(debitNoteDto);

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
   * Envía un borrador de nota de débito al SRI
   *
   * @param id ID de la nota de débito
   * @param requestBody Map con la contraseña de la firma digital
   * @return DebitNoteResponse con el resultado
   */
  @PostMapping("/{id}/send")
  public ResponseEntity<DebitNoteResponse> sendToSri(
      @PathVariable("id") Long id, @RequestBody Map<String, String> requestBody) {
    String password = requestBody.get("password");
    if (password == null || password.isBlank()) {
      DebitNoteResponse errorResponse =
          DebitNoteResponse.builder()
              .code(HttpStatus.BAD_REQUEST.value())
              .status(HttpStatus.BAD_REQUEST.name())
              .message("La contraseña de la firma digital es requerida")
              .build();
      return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    DebitNoteResponse response = debitNoteService.sendDebitNoteToSri(id, password);
    HttpStatus status =
        response.getCode() == HttpStatus.OK.value() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
    return new ResponseEntity<>(response, status);
  }

  /**
   * Descarga el archivo PDF (RIDE) de una nota de débito autorizada
   *
   * @param id ID de la nota de débito
   * @return ResponseEntity con el archivo PDF
   */
  @GetMapping("/{id}/pdf")
  public ResponseEntity<?> downloadPdf(@PathVariable("id") Long id) {
    try {
      Path pdfPath = debitNoteService.getPdfFilePath(id);
      Resource resource = new PathResource(pdfPath);

      String fileName = pdfPath.getFileName().toString();
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_PDF);
      headers.setContentDispositionFormData("attachment", fileName);
      headers.setContentLength(resource.contentLength());

      log.info("Descargando PDF de ND ID: {}", id);
      return ResponseEntity.ok().headers(headers).body(resource);

    } catch (IllegalArgumentException e) {
      log.error("Error descargando PDF de ND: {}", e.getMessage());
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("code", 400);
      errorResponse.put("status", "ERROR");
      errorResponse.put("message", e.getMessage());
      return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    } catch (IOException e) {
      log.error("Error IO descargando PDF de ND: {}", e.getMessage());
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("code", 500);
      errorResponse.put("status", "ERROR");
      errorResponse.put("message", "Error al leer el archivo PDF");
      return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Descarga el archivo XML autorizado de una nota de débito
   *
   * @param id ID de la nota de débito
   * @return ResponseEntity con el archivo XML
   */
  @GetMapping("/{id}/xml")
  public ResponseEntity<?> downloadXml(@PathVariable("id") Long id) {
    try {
      Path xmlPath = debitNoteService.getXmlFilePath(id);
      Resource resource = new PathResource(xmlPath);

      String fileName = xmlPath.getFileName().toString();
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_XML);
      headers.setContentDispositionFormData("attachment", fileName);
      headers.setContentLength(resource.contentLength());

      log.info("Descargando XML de ND ID: {}", id);
      return ResponseEntity.ok().headers(headers).body(resource);

    } catch (IllegalArgumentException e) {
      log.error("Error descargando XML de ND: {}", e.getMessage());
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("code", 400);
      errorResponse.put("status", "ERROR");
      errorResponse.put("message", e.getMessage());
      return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    } catch (IOException e) {
      log.error("Error IO descargando XML de ND: {}", e.getMessage());
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("code", 500);
      errorResponse.put("status", "ERROR");
      errorResponse.put("message", "Error al leer el archivo XML");
      return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Envía la nota de débito por email al cliente
   *
   * @param id ID de la nota de débito
   * @param requestBody Map opcional con email alternativo
   * @return ResponseEntity con el resultado
   */
  @PostMapping("/{id}/send-email")
  public ResponseEntity<Map<String, Object>> sendEmail(
      @PathVariable("id") Long id, @RequestBody(required = false) Map<String, String> requestBody) {
    Map<String, Object> response = new HashMap<>();
    try {
      String alternativeEmail = requestBody != null ? requestBody.get("email") : null;
      boolean sent = debitNoteService.sendDebitNoteByEmail(id, alternativeEmail);

      response.put("success", sent);
      response.put("message", sent ? "Email enviado exitosamente" : "No se pudo enviar el email");
      return new ResponseEntity<>(response, HttpStatus.OK);

    } catch (IllegalArgumentException e) {
      log.error("Error enviando email de ND: {}", e.getMessage());
      response.put("success", false);
      response.put("message", e.getMessage());
      return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    } catch (Exception e) {
      log.error("Error enviando email de ND: {}", e.getMessage());
      response.put("success", false);
      response.put("message", "Error al enviar email: " + e.getMessage());
      return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Busca notas de débito autorizadas y activas por número de comprobante o clave de acceso.
   * Requiere idCompany obligatorio y al menos uno de los criterios de búsqueda. Retorna información
   * básica: clave de acceso, número de autorización, fecha de emisión y número de comprobante.
   *
   * @param searchDto DTO con los criterios de búsqueda (idCompany obligatorio, debitNoteNumber o
   *     accessKey)
   * @return Lista de notas de débito encontradas con información básica
   */
  @PostMapping("/search-authorized-active")
  public ResponseEntity<?> searchAuthorizedActiveDebitNotes(
      @Validated @RequestBody
          com.wallparisoft.ebillbilling.dto.DebitNoteSearchByNumberOrAccessKeyDto searchDto) {
    try {
      List<com.wallparisoft.ebillbilling.dto.DebitNoteBasicInfoDto> result =
          debitNoteService.searchAuthorizedActiveDebitNotesByNumberOrAccessKey(searchDto);

      log.info(
          "📋 [POST /api/debit-notes/search-authorized-active] idCompany: {}, debitNoteNumber: {}, accessKey: {}, Total encontradas: {}",
          searchDto.getIdCompany(),
          searchDto.getDebitNoteNumber(),
          searchDto.getAccessKey(),
          result.size());

      return new ResponseEntity<>(result, HttpStatus.OK);
    } catch (IllegalArgumentException e) {
      log.error("Error en búsqueda de notas de débito autorizadas y activas: {}", e.getMessage());
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("code", 400);
      errorResponse.put("status", "ERROR");
      errorResponse.put("message", e.getMessage());
      return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
  }
}
