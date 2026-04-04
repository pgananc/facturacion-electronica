package com.wallparisoft.ebillbilling.controller;

import static com.wallparisoft.ebill.utils.LogUtil.setLogs;
import static lombok.AccessLevel.PRIVATE;

import com.wallparisoft.ebill.utils.log.EventType;
import com.wallparisoft.ebill.utils.log.Level;
import com.wallparisoft.ebillbilling.dto.DocumentQueryDto;
import com.wallparisoft.ebillbilling.dto.DocumentSearchResponseDto;
import com.wallparisoft.ebillbilling.dto.InvoiceAvailableForCreditNoteDto;
import com.wallparisoft.ebillbilling.dto.InvoiceDraftResponseDto;
import com.wallparisoft.ebillbilling.dto.InvoiceDto;
import com.wallparisoft.ebillbilling.dto.InvoiceListResponseDto;
import com.wallparisoft.ebillbilling.dto.InvoiceQueryDto;
import com.wallparisoft.ebillbilling.dto.InvoiceSearchResponseDto;
import com.wallparisoft.ebillbilling.response.InvoiceResponse;
import com.wallparisoft.ebillbilling.service.IInvoiceFileService;
import com.wallparisoft.ebillbilling.service.IInvoiceService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/invoice")
@FieldDefaults(level = PRIVATE)
@Slf4j
public class InvoiceController {

  final IInvoiceService invoiceService;
  final IInvoiceFileService invoiceFileService;
  final com.wallparisoft.ebillbilling.service.GenericRateLimitService genericRateLimitService;

  @GetMapping
  public ResponseEntity<List<InvoiceListResponseDto>> getAllInvoice() {
    List<InvoiceListResponseDto> invoices = invoiceService.listWithClientData();
    return new ResponseEntity<>(invoices, HttpStatus.OK);
  }

  /**
   * Obtiene una factura con las cantidades disponibles para crear nota de crédito. Solo facturas
   * autorizadas por el SRI pueden tener notas de crédito.
   *
   * @param idInvoice ID de la factura
   * @return DTO con datos de factura y cantidades disponibles por item
   */
  @GetMapping("/{id}/available-for-credit-note")
  public ResponseEntity<?> getInvoiceAvailableForCreditNote(@PathVariable("id") Long idInvoice) {
    try {
      InvoiceAvailableForCreditNoteDto result =
          invoiceService.getInvoiceAvailableForCreditNote(idInvoice);
      return new ResponseEntity<>(result, HttpStatus.OK);
    } catch (IllegalArgumentException e) {
      log.error("Error obteniendo factura para NC: {}", e.getMessage());
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("code", 400);
      errorResponse.put("status", "ERROR");
      errorResponse.put("message", e.getMessage());
      return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
  }

  /**
   * Busca facturas por idCompany (obligatorio) y filtros opcionales para crear notas de débito.
   * Retorna facturas autorizadas con datos del cliente.
   *
   * @param searchDto DTO con los criterios de búsqueda
   * @param pageable Paginación
   * @return Página de InvoiceForDebitNoteResponseDto
   */
  @PostMapping("/search-for-debit-note")
  public ResponseEntity<?> searchInvoicesForDebitNote(
      @Validated @RequestBody
          com.wallparisoft.ebillbilling.dto.InvoiceSearchForDebitNoteDto searchDto,
      Pageable pageable) {
    try {
      Page<com.wallparisoft.ebillbilling.dto.InvoiceForDebitNoteResponseDto> result =
          invoiceService.searchInvoicesForDebitNote(searchDto, pageable);

      log.info(
          "📋 [POST /api/invoice/search-for-debit-note] idCompany: {}, numeroAutorizacion: {}, numeroFactura: {}, nombreCliente: {}, identificacionCliente: {}, Total: {}, Página: {}",
          searchDto.getIdCompany(),
          searchDto.getNumeroAutorizacion(),
          searchDto.getNumeroFactura(),
          searchDto.getNombreCliente(),
          searchDto.getIdentificacionCliente(),
          result.getTotalElements(),
          result.getNumber());

      return new ResponseEntity<>(result, HttpStatus.OK);
    } catch (IllegalArgumentException e) {
      log.error("Error en búsqueda de facturas para nota de débito: {}", e.getMessage());
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("code", 400);
      errorResponse.put("status", "ERROR");
      errorResponse.put("message", e.getMessage());
      return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
  }

  /**
   * Busca facturas por idCompany (obligatorio) y filtros opcionales con paginación. Retorna un DTO
   * optimizado para listado con datos del cliente.
   *
   * @param searchDto DTO con los criterios de búsqueda (idCompany obligatorio)
   * @param pageable Paginación
   * @return Página de InvoiceListResponseDto
   */
  @PostMapping("/search-for-credit-note")
  public ResponseEntity<?> searchInvoicesByCompany(
      @Validated @RequestBody
          com.wallparisoft.ebillbilling.dto.InvoiceSearchForCreditNoteDto searchDto,
      Pageable pageable) {
    try {
      Page<InvoiceListResponseDto> result =
          invoiceService.searchInvoicesByCompany(searchDto, pageable);

      log.info(
          "📋 [POST /api/invoice/search-by-company] idCompany: {}, numeroAutorizacion: {}, numeroFactura: {}, nombreCliente: {}, identificacionCliente: {}, Total: {}, Página: {}",
          searchDto.getIdCompany(),
          searchDto.getNumeroAutorizacion(),
          searchDto.getNumeroFactura(),
          searchDto.getNombreCliente(),
          searchDto.getIdentificacionCliente(),
          result.getTotalElements(),
          result.getNumber());

      return new ResponseEntity<>(result, HttpStatus.OK);
    } catch (IllegalArgumentException e) {
      log.error("Error en búsqueda de facturas: {}", e.getMessage());
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("code", 400);
      errorResponse.put("status", "ERROR");
      errorResponse.put("message", e.getMessage());
      return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
  }

  /**
   * Busca facturas por idCompany (obligatorio), rango de fechas y filtros opcionales con
   * paginación. Retorna un DTO optimizado para listado con datos del cliente.
   *
   * @param searchDto DTO con los criterios de búsqueda (idCompany obligatorio, fechas opcionales)
   * @param pageable Paginación
   * @return Página de InvoiceListResponseDto
   */
  @PostMapping("/search")
  public ResponseEntity<?> searchInvoicesByFilters(
      @Validated @RequestBody
          com.wallparisoft.ebillbilling.dto.InvoiceSearchByCompanyAndDateDto searchDto,
      Pageable pageable) {
    try {
      Page<InvoiceListResponseDto> result =
          invoiceService.searchInvoicesByFilters(searchDto, pageable);

      log.info(
          "📋 [POST /api/invoice/search-by-company-and-date] idCompany: {}, creationDateFrom: {}, creationDateTo: {}, numeroAutorizacion: {}, numeroFactura: {}, nombreCliente: {}, identificacionCliente: {}, receptionStatusSri: {}, authorizationStatusSri: {}, Total: {}, Página: {}",
          searchDto.getIdCompany(),
          searchDto.getCreationDateFrom(),
          searchDto.getCreationDateTo(),
          searchDto.getNumeroAutorizacion(),
          searchDto.getNumeroFactura(),
          searchDto.getNombreCliente(),
          searchDto.getIdentificacionCliente(),
          searchDto.getReceptionStatusSri(),
          searchDto.getAuthorizationStatusSri(),
          result.getTotalElements(),
          result.getNumber());

      return new ResponseEntity<>(result, HttpStatus.OK);
    } catch (IllegalArgumentException e) {
      log.error("Error en búsqueda de facturas por compañía y fechas: {}", e.getMessage());
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("code", 400);
      errorResponse.put("status", "ERROR");
      errorResponse.put("message", e.getMessage());
      return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
  }

  @PostMapping
  public InvoiceResponse register(@Validated @RequestBody InvoiceDto invoiceDto) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        EventType.REQUEST,
        Level.LEVEL_001);

    // Log detallado del request de factura
    log.info("📥 [INVOICE REQUEST] ====================================");
    log.info(
        "📥 idCompany: {}, idClient: {}, idIssuancePoint: {}",
        invoiceDto.getIdCompany(),
        invoiceDto.getIdClient(),
        invoiceDto.getIdIssuancePoint());
    log.info(
        "📥 subtotalZero: {}, subtotalIva: {}, subtotalIce: {}",
        invoiceDto.getSubtotalZero(),
        invoiceDto.getSubtotalIva(),
        invoiceDto.getSubtotalIce());
    log.info(
        "📥 discount: {}, tip: {}, totalAmount: {}",
        invoiceDto.getDiscount(),
        invoiceDto.getTip(),
        invoiceDto.getTotalAmount());
    log.info(
        "📥 environment: {}, issueDate: {}",
        invoiceDto.getEnvironment(),
        invoiceDto.getIssueDate());
    if (invoiceDto.getInvoiceDetails() != null) {
      log.info("📥 Detalles ({} items):", invoiceDto.getInvoiceDetails().size());
      for (int i = 0; i < invoiceDto.getInvoiceDetails().size(); i++) {
        var detail = invoiceDto.getInvoiceDetails().get(i);
        log.info(
            "📥   [{}] idProduct: {}, qty: {}, unitPrice: {}, subtotal: {}, ivaPercentage: {}",
            i,
            detail.getIdProduct(),
            detail.getQuantity(),
            detail.getUnitPrice(),
            detail.getSubtotal(),
            detail.getIvaPercentage());
      }
    }
    log.info("📥 =====================================================");

    InvoiceResponse response = invoiceService.saveInvoice(invoiceDto);

    // Log detallado del response
    if (response.getInvoiceDto() != null) {
      var respDto = response.getInvoiceDto();
      log.info("📤 [INVOICE RESPONSE] ====================================");
      log.info(
          "📤 idInvoice: {}, invoiceNumber: {}",
          respDto.getIdInvoice(),
          respDto.getInvoiceNumber());
      log.info(
          "📤 subtotalZero: {}, subtotalIva: {}, subtotalIce: {}",
          respDto.getSubtotalZero(),
          respDto.getSubtotalIva(),
          respDto.getSubtotalIce());
      log.info(
          "📤 discount: {}, tip: {}, totalAmount: {}",
          respDto.getDiscount(),
          respDto.getTip(),
          respDto.getTotalAmount());
      log.info("📤 issueDate: {}", respDto.getIssueDate());
      log.info("📤 =====================================================");
    }

    setLogs(
        com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(response.getInvoiceDto()),
        EventType.RESPONSE,
        Level.LEVEL_001);
    return response;
  }

  /**
   * Consulta facturas por número de factura, identificación del comprador o fecha de creación. Solo
   * retorna datos básicos de la factura (número, fecha, cliente, total, estado, path).
   *
   * @param queryDto Criterios de búsqueda
   * @param pageable Paginación
   * @return Página de facturas encontradas
   */
  @PostMapping("/query")
  public ResponseEntity<Page<InvoiceSearchResponseDto>> queryInvoices(
      @RequestBody InvoiceQueryDto queryDto,
      Pageable pageable,
      jakarta.servlet.http.HttpServletRequest request) {

    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information("Query criteria: " + queryDto + ", pageable: " + pageable),
        EventType.REQUEST,
        Level.LEVEL_001);

    Page<InvoiceSearchResponseDto> invoices =
        invoiceService.queryInvoices(queryDto, pageable, request);

    setLogs(
        com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information("Found " + invoices.getTotalElements() + " invoices"),
        EventType.RESPONSE,
        Level.LEVEL_001);

    return new ResponseEntity<>(invoices, HttpStatus.OK);
  }

  @PostMapping("/draft")
  public InvoiceResponse saveDraft(@Validated @RequestBody InvoiceDto invoiceDto) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information("Saving invoice as draft"),
        EventType.REQUEST,
        Level.LEVEL_001);

    InvoiceResponse response = invoiceService.saveInvoiceDraft(invoiceDto);

    setLogs(
        com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(response.getInvoiceDto()),
        EventType.RESPONSE,
        Level.LEVEL_001);

    return response;
  }

  @GetMapping("/drafts")
  public ResponseEntity<List<InvoiceDraftResponseDto>> getDraftInvoices() {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information("Fetching all draft invoices"),
        EventType.REQUEST,
        Level.LEVEL_001);

    List<InvoiceDraftResponseDto> drafts = invoiceService.getDraftInvoices();

    setLogs(
        com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information("Found " + drafts.size() + " draft invoices"),
        EventType.RESPONSE,
        Level.LEVEL_001);

    return new ResponseEntity<>(drafts, HttpStatus.OK);
  }

  @GetMapping("/draft/{id}")
  public ResponseEntity<InvoiceDto> getDraftById(@PathVariable("id") Long idInvoice) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information("Fetching draft invoice with ID: " + idInvoice),
        EventType.REQUEST,
        Level.LEVEL_001);

    InvoiceDto draft = invoiceService.getDraftInvoiceById(idInvoice);

    setLogs(
        com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(
                "Draft invoice found with "
                    + (draft.getInvoiceDetails() != null ? draft.getInvoiceDetails().size() : 0)
                    + " details"),
        EventType.RESPONSE,
        Level.LEVEL_001);

    return new ResponseEntity<>(draft, HttpStatus.OK);
  }

  @PostMapping("/draft/{id}/process")
  public InvoiceResponse processDraftInvoice(@PathVariable("id") Long idInvoice) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information("Processing draft invoice with ID: " + idInvoice),
        EventType.REQUEST,
        Level.LEVEL_001);

    InvoiceResponse response = invoiceService.processDraftInvoice(idInvoice);

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
   * Elimina un borrador de factura Solo se pueden eliminar facturas que NO han sido autorizadas por
   * el SRI
   */
  @DeleteMapping("/draft/{id}")
  public ResponseEntity<Map<String, Object>> deleteDraft(@PathVariable("id") Long idInvoice) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information("Eliminando borrador con ID: " + idInvoice),
        EventType.REQUEST,
        Level.LEVEL_001);

    try {
      invoiceService.deleteDraft(idInvoice);

      Map<String, Object> response = new HashMap<>();
      response.put("code", 200);
      response.put("status", "OK");
      response.put("message", "Borrador eliminado exitosamente");

      setLogs(
          com.wallparisoft.ebill.utils.log.EventLog.builder()
              .service(traceElement.getClassName())
              .method(traceElement.getMethodName())
              .information("Borrador " + idInvoice + " eliminado exitosamente"),
          EventType.RESPONSE,
          Level.LEVEL_001);

      return new ResponseEntity<>(response, HttpStatus.OK);

    } catch (IllegalArgumentException e) {
      log.error("❌ Error al eliminar borrador: {}", e.getMessage());

      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("code", 400);
      errorResponse.put("status", "ERROR");
      errorResponse.put("message", e.getMessage());

      return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
  }

  /**
   * Descarga el archivo XML autorizado de una factura.
   *
   * @param idInvoice ID de la factura
   * @return ResponseEntity con el archivo XML
   */
  @GetMapping("/{id}/xml")
  public ResponseEntity<?> downloadXml(
      @PathVariable("id") Long idInvoice, jakarta.servlet.http.HttpServletRequest request) {
    try {
      // Rate limiting
      String clientIp = extractClientIp(request);
      String token = extractToken(request);
      checkDownloadRateLimit(token, clientIp);

      Path xmlPath = invoiceFileService.getXmlFilePath(idInvoice);
      Resource resource = new PathResource(xmlPath);

      String fileName = invoiceFileService.getXmlFileName(idInvoice);
      HttpHeaders headers = buildDownloadHeaders(resource, fileName, MediaType.APPLICATION_XML);

      log.info("Descargando XML de factura ID: {}", idInvoice);

      // Incrementar contadores después de éxito
      incrementDownloadCounters(token, clientIp);

      return ResponseEntity.ok().headers(headers).body(resource);

    } catch (Exception e) {
      log.error("❌ Error Descargando XML de factura: {}", e.getMessage());

      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("code", 400);
      errorResponse.put("status", "ERROR");
      errorResponse.put("message", e.getMessage());

      return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
  }

  /**
   * Descarga el archivo PDF (RIDE) de una factura autorizada.
   *
   * @param idInvoice ID de la factura
   * @return ResponseEntity con el archivo PDF
   */
  @GetMapping("/{id}/pdf")
  public ResponseEntity<?> downloadPdf(
      @PathVariable("id") Long idInvoice, jakarta.servlet.http.HttpServletRequest request) {
    try {
      // Rate limiting
      String clientIp = extractClientIp(request);
      String token = extractToken(request);
      checkDownloadRateLimit(token, clientIp);

      Path pdfPath = invoiceFileService.getPdfFilePath(idInvoice);
      Resource resource = new PathResource(pdfPath);

      String fileName = invoiceFileService.getPdfFileName(idInvoice);
      HttpHeaders headers = buildDownloadHeaders(resource, fileName, MediaType.APPLICATION_PDF);

      log.info("Descargando PDF de factura ID: {}", idInvoice);

      // Incrementar contadores después de éxito
      incrementDownloadCounters(token, clientIp);

      return ResponseEntity.ok().headers(headers).body(resource);

    } catch (Exception e) {
      log.error("❌ Error Descargando PDF de factura: {}", e.getMessage());

      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("code", 400);
      errorResponse.put("status", "ERROR");
      errorResponse.put("message", e.getMessage());

      return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
  }

  /**
   * Obtiene una factura completa por ID (puede ser borrador o autorizada).
   *
   * @param idInvoice ID de la factura
   * @return DTO con datos completos de la factura
   */
  @GetMapping("/{id}")
  public ResponseEntity<?> getInvoiceById(@PathVariable("id") Long idInvoice) {
    try {
      InvoiceDto invoice = invoiceService.getInvoiceById(idInvoice);
      return new ResponseEntity<>(invoice, HttpStatus.OK);
    } catch (IllegalArgumentException e) {
      log.error("Error obteniendo factura: {}", e.getMessage());
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("code", 404);
      errorResponse.put("status", "ERROR");
      errorResponse.put("message", e.getMessage());
      return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }
  }

  private HttpHeaders buildDownloadHeaders(
      Resource resource, String fileName, MediaType contentType) throws IOException {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(contentType);
    headers.setContentDispositionFormData("attachment", fileName);
    headers.setContentLength(resource.contentLength());

    // Headers de cache
    if (resource instanceof PathResource pathResource) {
      try {
        Path path = Paths.get(pathResource.getPath());
        FileTime lastModified = Files.getLastModifiedTime(path);
        headers.setLastModified(lastModified.toInstant());
        headers.setCacheControl(
            CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate().mustRevalidate());
      } catch (IOException ex) {
        log.debug("No se pudo obtener fecha de modificación del archivo: {}", ex.getMessage());
      }
    }

    return headers;
  }

  /** Verifica rate limiting para descargas (PDF/XML). */
  private void checkDownloadRateLimit(String token, String clientIp) {
    // 1. Validar rate limiting por TOKEN
    if (token != null && !token.isBlank()) {
      if (!genericRateLimitService.canMakeRequest(
          "INVOICE_DOWNLOAD",
          com.wallparisoft.ebillbilling.service.GenericRateLimitService.RateLimitDimension.TOKEN,
          token)) {

        long secondsLeft =
            genericRateLimitService.getTimeUntilNextAttempt(
                "INVOICE_DOWNLOAD",
                com.wallparisoft.ebillbilling.service.GenericRateLimitService.RateLimitDimension
                    .TOKEN,
                token);
        long minutesLeft = secondsLeft / 60;
        int currentAttempts =
            genericRateLimitService.getCurrentAttempts(
                "INVOICE_DOWNLOAD",
                com.wallparisoft.ebillbilling.service.GenericRateLimitService.RateLimitDimension
                    .TOKEN,
                token);
        int maxAttempts =
            genericRateLimitService.getMaxRequests(
                "INVOICE_DOWNLOAD",
                com.wallparisoft.ebillbilling.service.GenericRateLimitService.RateLimitDimension
                    .TOKEN);
        int windowMinutes =
            genericRateLimitService.getWindowMinutes(
                "INVOICE_DOWNLOAD",
                com.wallparisoft.ebillbilling.service.GenericRateLimitService.RateLimitDimension
                    .TOKEN);

        String message =
            String.format(
                "Máximo de descargas alcanzado por token (%d/%d). Intente nuevamente en %d minutos",
                currentAttempts, maxAttempts, minutesLeft);

        throw new com.wallparisoft.ebillbilling.exception.RateLimitExceededException(
            message, minutesLeft, currentAttempts, maxAttempts, windowMinutes);
      }
    }

    // 2. Validar rate limiting por IP
    if (clientIp != null && !clientIp.isBlank()) {
      if (!genericRateLimitService.canMakeRequest(
          "INVOICE_DOWNLOAD",
          com.wallparisoft.ebillbilling.service.GenericRateLimitService.RateLimitDimension.IP,
          clientIp)) {

        long secondsLeft =
            genericRateLimitService.getTimeUntilNextAttempt(
                "INVOICE_DOWNLOAD",
                com.wallparisoft.ebillbilling.service.GenericRateLimitService.RateLimitDimension.IP,
                clientIp);
        long minutesLeft = secondsLeft / 60;
        int currentAttempts =
            genericRateLimitService.getCurrentAttempts(
                "INVOICE_DOWNLOAD",
                com.wallparisoft.ebillbilling.service.GenericRateLimitService.RateLimitDimension.IP,
                clientIp);
        int maxAttempts =
            genericRateLimitService.getMaxRequests(
                "INVOICE_DOWNLOAD",
                com.wallparisoft.ebillbilling.service.GenericRateLimitService.RateLimitDimension
                    .IP);
        int windowMinutes =
            genericRateLimitService.getWindowMinutes(
                "INVOICE_DOWNLOAD",
                com.wallparisoft.ebillbilling.service.GenericRateLimitService.RateLimitDimension
                    .IP);

        String message =
            String.format(
                "Máximo de descargas alcanzado por IP (%d/%d). Intente nuevamente en %d minutos",
                currentAttempts, maxAttempts, minutesLeft);

        throw new com.wallparisoft.ebillbilling.exception.RateLimitExceededException(
            message, minutesLeft, currentAttempts, maxAttempts, windowMinutes);
      }
    }
  }

  /** Incrementa contadores de rate limiting para descargas después de éxito. */
  private void incrementDownloadCounters(String token, String clientIp) {
    if (token != null && !token.isBlank()) {
      genericRateLimitService.incrementRequest(
          "INVOICE_DOWNLOAD",
          com.wallparisoft.ebillbilling.service.GenericRateLimitService.RateLimitDimension.TOKEN,
          token);
    }
    if (clientIp != null && !clientIp.isBlank()) {
      genericRateLimitService.incrementRequest(
          "INVOICE_DOWNLOAD",
          com.wallparisoft.ebillbilling.service.GenericRateLimitService.RateLimitDimension.IP,
          clientIp);
    }
  }

  /** Extrae la IP del cliente del request, manejando proxies. */
  private String extractClientIp(jakarta.servlet.http.HttpServletRequest request) {
    if (request == null) {
      return null;
    }
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
      return xForwardedFor.split(",")[0].trim();
    }
    String xRealIp = request.getHeader("X-Real-IP");
    if (xRealIp != null && !xRealIp.isEmpty()) {
      return xRealIp;
    }
    return request.getRemoteAddr();
  }

  /** Extrae el token JWT del header Authorization. */
  private String extractToken(jakarta.servlet.http.HttpServletRequest request) {
    if (request == null) {
      return null;
    }
    String authHeader = request.getHeader("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      return authHeader.substring(7);
    }
    return null;
  }

  /**
   * Consulta unificada de documentos (facturas y/o notas de crédito) según el tipo especificado.
   * Solo retorna datos básicos de los documentos (número, fecha, cliente, total, estado, path).
   *
   * @param queryDto Criterios de búsqueda con filtro de tipo de documento (0=ambos, 1=facturas,
   *     2=notas de crédito)
   * @param pageable Paginación
   * @return Página de documentos encontrados (facturas y/o notas de crédito)
   */
  @PostMapping("/documents/query")
  public ResponseEntity<Page<DocumentSearchResponseDto>> queryDocuments(
      @RequestBody DocumentQueryDto queryDto,
      Pageable pageable,
      jakarta.servlet.http.HttpServletRequest request) {

    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information("Query criteria: " + queryDto + ", pageable: " + pageable),
        EventType.REQUEST,
        Level.LEVEL_001);

    Page<DocumentSearchResponseDto> documents =
        invoiceService.queryDocuments(queryDto, pageable, request);

    setLogs(
        com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information("Found " + documents.getTotalElements() + " documents"),
        EventType.RESPONSE,
        Level.LEVEL_001);

    return new ResponseEntity<>(documents, HttpStatus.OK);
  }

  /**
   * Busca facturas autorizadas y activas por número de factura o clave de acceso. Requiere
   * idCompany obligatorio y al menos uno de los criterios de búsqueda. Retorna información básica:
   * clave de acceso, número de autorización, fecha de emisión y número de comprobante.
   *
   * @param searchDto DTO con los criterios de búsqueda (idCompany obligatorio, invoiceNumber o
   *     accessKey)
   * @return Lista de facturas encontradas con información básica
   */
  @PostMapping("/search-authorized-active")
  public ResponseEntity<?> searchAuthorizedActiveInvoices(
      @Validated @RequestBody
          com.wallparisoft.ebillbilling.dto.InvoiceSearchByNumberOrAccessKeyDto searchDto) {
    try {
      List<com.wallparisoft.ebillbilling.dto.InvoiceBasicInfoDto> result =
          invoiceService.searchAuthorizedActiveInvoicesByNumberOrAccessKey(searchDto);

      log.info(
          "📋 [POST /api/invoice/search-authorized-active] idCompany: {}, invoiceNumber: {}, accessKey: {}, Total encontradas: {}",
          searchDto.getIdCompany(),
          searchDto.getInvoiceNumber(),
          searchDto.getAccessKey(),
          result.size());

      return new ResponseEntity<>(result, HttpStatus.OK);
    } catch (IllegalArgumentException e) {
      log.error("Error en búsqueda de facturas autorizadas y activas: {}", e.getMessage());
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("code", 400);
      errorResponse.put("status", "ERROR");
      errorResponse.put("message", e.getMessage());
      return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
  }
}
