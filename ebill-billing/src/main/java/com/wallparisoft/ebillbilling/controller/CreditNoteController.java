package com.wallparisoft.ebillbilling.controller;

import static com.wallparisoft.ebill.utils.LogUtil.setLogs;
import static lombok.AccessLevel.PRIVATE;

import com.wallparisoft.ebill.utils.log.EventType;
import com.wallparisoft.ebill.utils.log.Level;
import com.wallparisoft.ebillbilling.dto.CreditNoteDto;
import com.wallparisoft.ebillbilling.entity.CreditNote;
import com.wallparisoft.ebillbilling.response.CreditNoteResponse;
import com.wallparisoft.ebillbilling.service.ICreditNoteService;
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
@RequestMapping("/api/credit-notes")
@FieldDefaults(level = PRIVATE)
@Slf4j
public class CreditNoteController {

  final ICreditNoteService creditNoteService;

  /**
   * Lista todas las notas de crédito
   *
   * @return Lista de notas de crédito
   */
  @GetMapping
  public ResponseEntity<List<com.wallparisoft.ebillbilling.dto.CreditNoteListResponseDto>>
      getAllCreditNotes() {
    List<com.wallparisoft.ebillbilling.dto.CreditNoteListResponseDto> creditNotes =
        creditNoteService.listWithClientData();

    // Log detallado de la respuesta
    log.info("📋 [GET /api/credit-notes] Total registros: {}", creditNotes.size());
    for (var cn : creditNotes) {
      log.info(
          "📄 NC ID={}, number={}, status={}, totalAmount={}, originalInvoiceNumber={}, "
              + "buyerName={}, buyerIdentification={}, authorizationSri={}",
          cn.getIdCreditNote(),
          cn.getCreditNoteNumber(),
          cn.getCreditNoteStatus(),
          cn.getTotalAmount(),
          cn.getOriginalInvoiceNumber(),
          cn.getBuyerName(),
          cn.getBuyerIdentification(),
          cn.getAuthorizationSri() != null
              ? cn.getAuthorizationSri().getAuthorizationStatusSri()
              : "null");
    }

    return new ResponseEntity<>(creditNotes, HttpStatus.OK);
  }

  /**
   * Obtiene un borrador de nota de crédito por ID. Solo retorna notas de crédito que no han sido
   * autorizadas por el SRI.
   *
   * @param id ID de la nota de crédito borrador
   * @return DTO con datos completos de la nota de crédito borrador
   */
  @GetMapping("/draft/{id}")
  public ResponseEntity<?> getDraftById(@PathVariable("id") Long id) {
    try {
      CreditNoteDto creditNoteDto = creditNoteService.getDraftCreditNoteById(id);
      return new ResponseEntity<>(creditNoteDto, HttpStatus.OK);
    } catch (IllegalArgumentException e) {
      log.error("Error obteniendo borrador de NC: {}", e.getMessage());
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("code", 400);
      errorResponse.put("status", "ERROR");
      errorResponse.put("message", e.getMessage());
      return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
  }

  /**
   * Obtiene una nota de crédito por ID
   *
   * @param id ID de la nota de crédito
   * @return Nota de crédito
   */
  @GetMapping("/{id}")
  public ResponseEntity<CreditNoteDto> getCreditNoteById(@PathVariable("id") Long id) {
    CreditNoteDto creditNoteDto = creditNoteService.getCreditNoteById(id);
    if (creditNoteDto == null) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(creditNoteDto, HttpStatus.OK);
  }

  /**
   * Crea y envía una nota de crédito al SRI
   *
   * @param creditNoteDto DTO con los datos de la nota de crédito
   * @return CreditNoteResponse con el resultado
   */
  @PostMapping
  public CreditNoteResponse createCreditNote(@Validated @RequestBody CreditNoteDto creditNoteDto) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        EventType.REQUEST,
        Level.LEVEL_001);

    CreditNoteResponse response = creditNoteService.saveCreditNote(creditNoteDto);
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
   * Actualiza una nota de crédito (solo borradores)
   *
   * @param id ID de la nota de crédito
   * @param creditNoteDto DTO con los datos actualizados
   * @return CreditNoteResponse con el resultado
   */
  @PutMapping("/{id}")
  public ResponseEntity<CreditNoteResponse> updateCreditNote(
      @PathVariable("id") Long id, @Validated @RequestBody CreditNoteDto creditNoteDto) {
    try {
      CreditNoteResponse response = creditNoteService.updateCreditNote(id, creditNoteDto);
      return new ResponseEntity<>(response, HttpStatus.OK);
    } catch (IllegalArgumentException | IllegalStateException e) {
      log.error("Error updating credit note: {}", e.getMessage());
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
  }

  /**
   * Elimina una nota de crédito (solo borradores)
   *
   * @param id ID de la nota de crédito
   * @return ResponseEntity con el resultado
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteCreditNote(@PathVariable("id") Integer id) {
    try {
      boolean deleted = creditNoteService.delete(id);
      if (deleted) {
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
      } else {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
      }
    } catch (IllegalStateException e) {
      log.error("Error deleting credit note: {}", e.getMessage());
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
  }

  /**
   * Busca notas de crédito con filtros
   *
   * @param creditNoteNumber Número de nota de crédito (opcional)
   * @param idClient ID del cliente (opcional)
   * @param originalInvoiceId ID de la factura original (opcional)
   * @param pageable Paginación
   * @return Página de notas de crédito
   */
  @GetMapping("/search")
  public ResponseEntity<Page<CreditNote>> searchCreditNotes(
      @RequestParam(required = false) String creditNoteNumber,
      @RequestParam(required = false) Long idClient,
      @RequestParam(required = false) Long originalInvoiceId,
      Pageable pageable) {
    Page<CreditNote> creditNotes =
        creditNoteService.searchCreditNotes(
            creditNoteNumber, idClient, originalInvoiceId, pageable);
    return new ResponseEntity<>(creditNotes, HttpStatus.OK);
  }

  /**
   * Busca notas de crédito por idCompany (obligatorio) y filtros opcionales con paginación. Retorna
   * un DTO optimizado para listado con datos del cliente.
   *
   * @param searchDto DTO con los criterios de búsqueda (idCompany obligatorio)
   * @param pageable Paginación
   * @return Página de CreditNoteListResponseDto
   */
  @PostMapping("/search-by-company")
  public ResponseEntity<?> searchCreditNotesByCompany(
      @Validated @RequestBody
          com.wallparisoft.ebillbilling.dto.CreditNoteSearchByCompanyDto searchDto,
      Pageable pageable) {
    try {
      Page<com.wallparisoft.ebillbilling.dto.CreditNoteListResponseDto> result =
          creditNoteService.searchCreditNotesByCompany(searchDto, pageable);

      log.info(
          "📋 [POST /api/credit-notes/search-by-company] idCompany: {}, creditNoteNumber: {}, originalInvoiceNumber: {}, Total: {}, Página: {}",
          searchDto.getIdCompany(),
          searchDto.getCreditNoteNumber(),
          searchDto.getOriginalInvoiceNumber(),
          result.getTotalElements(),
          result.getNumber());

      return new ResponseEntity<>(result, HttpStatus.OK);
    } catch (IllegalArgumentException e) {
      log.error("Error en búsqueda de notas de crédito: {}", e.getMessage());
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("code", 400);
      errorResponse.put("status", "ERROR");
      errorResponse.put("message", e.getMessage());
      return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
  }

  /**
   * Obtiene todas las notas de crédito de una factura original
   *
   * @param originalInvoiceId ID de la factura original
   * @return Lista de notas de crédito
   */
  @GetMapping("/by-invoice/{originalInvoiceId}")
  public ResponseEntity<List<CreditNote>> getCreditNotesByOriginalInvoice(
      @PathVariable("originalInvoiceId") Long originalInvoiceId) {
    List<CreditNote> creditNotes =
        creditNoteService.getCreditNotesByOriginalInvoice(originalInvoiceId);
    return new ResponseEntity<>(creditNotes, HttpStatus.OK);
  }

  /**
   * Guarda una nota de crédito como borrador (sin enviar al SRI)
   *
   * @param creditNoteDto DTO con los datos de la nota de crédito
   * @return CreditNoteResponse con el resultado
   */
  @PostMapping("/draft")
  public CreditNoteResponse saveDraft(@Validated @RequestBody CreditNoteDto creditNoteDto) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information("Guardando NC como borrador"),
        EventType.REQUEST,
        Level.LEVEL_001);

    CreditNoteResponse response = creditNoteService.saveCreditNoteDraft(creditNoteDto);

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
   * Envía un borrador de nota de crédito al SRI
   *
   * @param id ID de la nota de crédito
   * @param requestBody Map con la contraseña de la firma digital
   * @return CreditNoteResponse con el resultado
   */
  @PostMapping("/{id}/send")
  public ResponseEntity<CreditNoteResponse> sendToSri(
      @PathVariable("id") Long id, @RequestBody Map<String, String> requestBody) {
    String password = requestBody.get("password");
    if (password == null || password.isBlank()) {
      CreditNoteResponse errorResponse =
          CreditNoteResponse.builder()
              .code(HttpStatus.BAD_REQUEST.value())
              .status(HttpStatus.BAD_REQUEST.name())
              .message("La contraseña de la firma digital es requerida")
              .build();
      return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    CreditNoteResponse response = creditNoteService.sendCreditNoteToSri(id, password);
    HttpStatus status =
        response.getCode() == HttpStatus.OK.value() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
    return new ResponseEntity<>(response, status);
  }

  /**
   * Descarga el archivo PDF (RIDE) de una nota de crédito autorizada
   *
   * @param id ID de la nota de crédito
   * @return ResponseEntity con el archivo PDF
   */
  @GetMapping("/{id}/pdf")
  public ResponseEntity<?> downloadPdf(@PathVariable("id") Long id) {
    try {
      Path pdfPath = creditNoteService.getPdfFilePath(id);
      Resource resource = new PathResource(pdfPath);

      String fileName = pdfPath.getFileName().toString();
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_PDF);
      headers.setContentDispositionFormData("attachment", fileName);
      headers.setContentLength(resource.contentLength());

      log.info("Descargando PDF de NC ID: {}", id);
      return ResponseEntity.ok().headers(headers).body(resource);

    } catch (IllegalArgumentException e) {
      log.error("Error descargando PDF de NC: {}", e.getMessage());
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("code", 400);
      errorResponse.put("status", "ERROR");
      errorResponse.put("message", e.getMessage());
      return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    } catch (IOException e) {
      log.error("Error IO descargando PDF de NC: {}", e.getMessage());
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("code", 500);
      errorResponse.put("status", "ERROR");
      errorResponse.put("message", "Error al leer el archivo PDF");
      return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Descarga el archivo XML autorizado de una nota de crédito
   *
   * @param id ID de la nota de crédito
   * @return ResponseEntity con el archivo XML
   */
  @GetMapping("/{id}/xml")
  public ResponseEntity<?> downloadXml(@PathVariable("id") Long id) {
    try {
      Path xmlPath = creditNoteService.getXmlFilePath(id);
      Resource resource = new PathResource(xmlPath);

      String fileName = xmlPath.getFileName().toString();
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_XML);
      headers.setContentDispositionFormData("attachment", fileName);
      headers.setContentLength(resource.contentLength());

      log.info("Descargando XML de NC ID: {}", id);
      return ResponseEntity.ok().headers(headers).body(resource);

    } catch (IllegalArgumentException e) {
      log.error("Error descargando XML de NC: {}", e.getMessage());
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("code", 400);
      errorResponse.put("status", "ERROR");
      errorResponse.put("message", e.getMessage());
      return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    } catch (IOException e) {
      log.error("Error IO descargando XML de NC: {}", e.getMessage());
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("code", 500);
      errorResponse.put("status", "ERROR");
      errorResponse.put("message", "Error al leer el archivo XML");
      return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Envía la nota de crédito por email al cliente
   *
   * @param id ID de la nota de crédito
   * @param requestBody Map opcional con email alternativo
   * @return ResponseEntity con el resultado
   */
  @PostMapping("/{id}/send-email")
  public ResponseEntity<Map<String, Object>> sendEmail(
      @PathVariable("id") Long id, @RequestBody(required = false) Map<String, String> requestBody) {
    Map<String, Object> response = new HashMap<>();
    try {
      String alternativeEmail = requestBody != null ? requestBody.get("email") : null;
      boolean sent = creditNoteService.sendCreditNoteByEmail(id, alternativeEmail);

      response.put("success", sent);
      response.put("message", sent ? "Email enviado exitosamente" : "No se pudo enviar el email");
      return new ResponseEntity<>(response, HttpStatus.OK);

    } catch (IllegalArgumentException e) {
      log.error("Error enviando email de NC: {}", e.getMessage());
      response.put("success", false);
      response.put("message", e.getMessage());
      return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    } catch (Exception e) {
      log.error("Error enviando email de NC: {}", e.getMessage());
      response.put("success", false);
      response.put("message", "Error al enviar email: " + e.getMessage());
      return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Busca notas de crédito autorizadas y activas por número de comprobante o clave de acceso.
   * Requiere idCompany obligatorio y al menos uno de los criterios de búsqueda. Retorna información
   * básica: clave de acceso, número de autorización, fecha de emisión y número de comprobante.
   *
   * @param searchDto DTO con los criterios de búsqueda (idCompany obligatorio, creditNoteNumber o
   *     accessKey)
   * @return Lista de notas de crédito encontradas con información básica
   */
  @PostMapping("/search-authorized-active")
  public ResponseEntity<?> searchAuthorizedActiveCreditNotes(
      @Validated @RequestBody
          com.wallparisoft.ebillbilling.dto.CreditNoteSearchByNumberOrAccessKeyDto searchDto) {
    try {
      List<com.wallparisoft.ebillbilling.dto.CreditNoteBasicInfoDto> result =
          creditNoteService.searchAuthorizedActiveCreditNotesByNumberOrAccessKey(searchDto);

      log.info(
          "📋 [POST /api/credit-notes/search-authorized-active] idCompany: {}, creditNoteNumber: {}, accessKey: {}, Total encontradas: {}",
          searchDto.getIdCompany(),
          searchDto.getCreditNoteNumber(),
          searchDto.getAccessKey(),
          result.size());

      return new ResponseEntity<>(result, HttpStatus.OK);
    } catch (IllegalArgumentException e) {
      log.error("Error en búsqueda de notas de crédito autorizadas y activas: {}", e.getMessage());
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("code", 400);
      errorResponse.put("status", "ERROR");
      errorResponse.put("message", e.getMessage());
      return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
  }
}
