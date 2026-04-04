package com.wallparisoft.ebillbilling.controller;

import com.wallparisoft.ebillbilling.dto.proforma.ProformaConvertToInvoiceResponseDto;
import com.wallparisoft.ebillbilling.dto.proforma.ProformaCreateRequestDto;
import com.wallparisoft.ebillbilling.dto.proforma.ProformaDto;
import com.wallparisoft.ebillbilling.dto.proforma.ProformaListResponseDto;
import com.wallparisoft.ebillbilling.dto.proforma.ProformaUpdateRequestDto;
import com.wallparisoft.ebillbilling.service.IProformaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
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

@RestController
@RequestMapping("/api/proforma")
@RequiredArgsConstructor
@Slf4j
@Tag(
    name = "Proforma",
    description = "Proformas (quotes / pre-invoices). Can be converted to invoice draft.")
@SecurityRequirement(name = "bearerAuth")
public class ProformaController {

  private final IProformaService proformaService;

  @PostMapping
  @Operation(summary = "Create proforma", description = "Creates a new proforma with status DRAFT.")
  public ResponseEntity<ProformaDto> create(
      @Validated @RequestBody ProformaCreateRequestDto request) {
    log.info("Creating proforma for company {}", request.getIdCompany());
    ProformaDto created = proformaService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @PutMapping("/{id}")
  @Operation(
      summary = "Update proforma",
      description = "Updates a proforma. Only allowed when status is DRAFT.")
  public ResponseEntity<ProformaDto> update(
      @PathVariable Long id, @Validated @RequestBody ProformaUpdateRequestDto request) {
    log.info("Updating proforma {}", id);
    ProformaDto updated = proformaService.update(id, request);
    return ResponseEntity.ok(updated);
  }

  @GetMapping("/{id}")
  @Operation(
      summary = "Get proforma by id",
      description = "Returns full proforma with details and totals.")
  public ResponseEntity<ProformaDto> getById(@PathVariable Long id, @RequestParam Long idCompany) {
    ProformaDto dto = proformaService.getById(id, idCompany);
    return ResponseEntity.ok(dto);
  }

  @GetMapping
  @Operation(
      summary = "List/search proformas",
      description = "Paginated list with optional filters.")
  public ResponseEntity<Page<ProformaListResponseDto>> search(
      @RequestParam Long idCompany,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate issueDateFrom,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate issueDateTo,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String identification,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String sort) {
    Sort sortObj = Sort.by(Sort.Direction.DESC, "issueDate");
    if (sort != null && !sort.isBlank()) {
      String[] parts = sort.split(",");
      if (parts.length >= 2) {
        sortObj =
            Sort.by(
                "desc".equalsIgnoreCase(parts[1].trim()) ? Sort.Direction.DESC : Sort.Direction.ASC,
                parts[0].trim());
      } else if (parts.length == 1) {
        sortObj = Sort.by(Sort.Direction.DESC, parts[0].trim());
      }
    }
    Pageable pageable = PageRequest.of(page, size, sortObj);
    Page<ProformaListResponseDto> result =
        proformaService.search(
            idCompany, issueDateFrom, issueDateTo, status, identification, pageable);
    return ResponseEntity.ok(result);
  }

  @DeleteMapping("/{id}")
  @Operation(
      summary = "Delete proforma",
      description = "Deletes a proforma. Only allowed when status is DRAFT.")
  public ResponseEntity<Void> delete(@PathVariable Long id, @RequestParam Long idCompany) {
    proformaService.delete(id, idCompany);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/convert-to-invoice")
  @Operation(
      summary = "Convert to invoice",
      description = "Creates an invoice draft from the proforma and links it. Only DRAFT or SENT.")
  public ResponseEntity<ProformaConvertToInvoiceResponseDto> convertToInvoice(
      @PathVariable Long id, @RequestParam Long idCompany) {
    ProformaConvertToInvoiceResponseDto response = proformaService.convertToInvoice(id, idCompany);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/{id}/pdf")
  @Operation(
      summary = "Generar PDF de proforma",
      description = "Genera y devuelve el PDF de la proforma indicada.")
  public ResponseEntity<byte[]> generatePdf(@PathVariable Long id, @RequestParam Long idCompany) {
    ProformaDto dto = proformaService.getById(id, idCompany);
    byte[] pdfBytes = proformaService.generatePdf(id, idCompany);

    String fileName =
        (dto.getProformaNumber() != null ? dto.getProformaNumber() : "proforma-" + id) + ".pdf";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);
    headers.setContentDispositionFormData("attachment", fileName);

    return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
  }
}
