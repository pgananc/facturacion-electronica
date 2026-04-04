package com.wallparisoft.ebill.customer.controller;

import static com.wallparisoft.ebill.utils.LogUtil.setLogs;
import static com.wallparisoft.ebill.utils.log.EventType.REQUEST;
import static com.wallparisoft.ebill.utils.log.EventType.RESPONSE;
import static com.wallparisoft.ebill.utils.log.Level.LEVEL_001;
import static lombok.AccessLevel.PRIVATE;

import com.wallparisoft.ebill.customer.dto.CompanyDetailsDto;
import com.wallparisoft.ebill.customer.dto.CompanyDto;
import com.wallparisoft.ebill.customer.dto.SearchCompanyDto;
import com.wallparisoft.ebill.customer.response.CompanyDetailDtoResponse;
import com.wallparisoft.ebill.customer.service.ICompanyService;
import com.wallparisoft.ebill.utils.log.EventLog;
import com.wallparisoft.ebill.utils.response.BasicResponse;
import jakarta.validation.Valid;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/company")
@FieldDefaults(level = PRIVATE)
@Slf4j
public class CompanyController {

  final ICompanyService companyService;

  @PostMapping
  public ResponseEntity<CompanyDto> save(@Valid @RequestBody CompanyDto companyDto) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        REQUEST,
        LEVEL_001);
    CompanyDto savedCompany = companyService.saveCompany(companyDto);
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information("Company created with ID: " + savedCompany.getIdCompany()),
        RESPONSE,
        LEVEL_001);
    return new ResponseEntity<>(savedCompany, HttpStatus.CREATED);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<BasicResponse> delete(@PathVariable("id") Long id) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(id),
        REQUEST,
        LEVEL_001);
    companyService.delete(id);
    BasicResponse response = getBasicResponse();
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(response.getMessage()),
        RESPONSE,
        LEVEL_001);
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @GetMapping("/companies-contacts")
  public ResponseEntity<CompanyDetailDtoResponse> findCompaniesActiveAndContactActive() {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        REQUEST,
        LEVEL_001);
    List<CompanyDetailsDto> companies = companyService.findCompaniesActiveAndContactActive();
    CompanyDetailDtoResponse response =
        CompanyDetailDtoResponse.builder()
            .status(HttpStatus.OK.getReasonPhrase())
            .companyDetailsDtos(companies)
            .build();
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(response.getCompanyDetailsDtos()),
        RESPONSE,
        LEVEL_001);
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @PatchMapping("/{idCompany}")
  public ResponseEntity<BasicResponse> update(
      @Valid @RequestBody CompanyDto companyDto, @PathVariable Long idCompany) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(
                "ID client: "
                    .concat(idCompany.toString())
                    .concat(", Object company: ")
                    .concat(companyDto.toString())),
        REQUEST,
        LEVEL_001);
    companyDto.setIdCompany(idCompany);
    companyService.updateCompany(companyDto);
    BasicResponse response = getBasicResponse();
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(response.getMessage()),
        RESPONSE,
        LEVEL_001);
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @PostMapping("/pageable")
  public ResponseEntity<Page<CompanyDetailsDto>> listPageable(
      Pageable pageable, @Valid @RequestBody SearchCompanyDto searchCompanyDto) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information("Object company: ".concat(searchCompanyDto.toString())),
        REQUEST,
        LEVEL_001);
    Page<CompanyDetailsDto> companies =
        companyService.findCompanyByIdentificationOrName(
            searchCompanyDto.getIdentification(), searchCompanyDto.getName(),
            searchCompanyDto.getStatus(), searchCompanyDto.getCompaniesIds(),
            searchCompanyDto.getIsSuperAdmin(), pageable);
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        RESPONSE,
        LEVEL_001);
    return new ResponseEntity<>(companies, HttpStatus.OK);
  }

  @GetMapping("exist/{identification}")
  public ResponseEntity<Boolean> existsByIdentification(
      @PathVariable(value = "identification") String identification) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        REQUEST,
        LEVEL_001);
    Boolean exists = companyService.existsByIdentification(identification);
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(exists),
        RESPONSE,
        LEVEL_001);
    return new ResponseEntity<>(exists, HttpStatus.OK);
  }

  @GetMapping("/{idCompany}")
  public ResponseEntity<CompanyDetailsDto> getCompanyById(
      @PathVariable(value = "idCompany") Long idCompany) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        REQUEST,
        LEVEL_001);
    CompanyDetailsDto companyDetailsDto = companyService.findCompanyById(idCompany);
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(companyDetailsDto),
        RESPONSE,
        LEVEL_001);
    return new ResponseEntity<>(companyDetailsDto, HttpStatus.OK);
  }

  @GetMapping
  @ResponseStatus(HttpStatus.OK)
  public CompanyDetailDtoResponse getAll() {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        REQUEST,
        LEVEL_001);
    CompanyDetailDtoResponse response = companyService.getAllCompanies();
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(response.getCompanyDetailsDtos()),
        RESPONSE,
        LEVEL_001);
    return response;
  }

  @GetMapping("/companies/{idCompanies}")
  @ResponseStatus(HttpStatus.OK)
  public CompanyDetailDtoResponse getCompaniesByIdCompanies(
      @PathVariable("idCompanies") String idCompanies) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information("IDs recibidos: " + idCompanies),
        REQUEST,
        LEVEL_001);

    // Validar que el parámetro no esté vacío
    if (!StringUtils.hasText(idCompanies)) {
      log.error("IDs de compañías vacío o null");
      setLogs(
          EventLog.builder()
              .service(traceElement.getClassName())
              .method(traceElement.getMethodName())
              .information("Error: IDs vacío o null"),
          RESPONSE,
          LEVEL_001);
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "IDs de compañías no pueden estar vacíos");
    }

    // Parsear el string "1,2" o "1" a List<Long>
    List<Long> ids;
    try {
      ids =
          Arrays.stream(idCompanies.split(","))
              .map(String::trim)
              .filter(s -> !s.isEmpty())
              .map(Long::parseLong)
              .toList();

      if (ids.isEmpty()) {
        log.error("No se pudieron parsear IDs válidos de: {}", idCompanies);
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "No se encontraron IDs válidos en: " + idCompanies);
      }
    } catch (NumberFormatException e) {
      log.error("Error al parsear IDs de compañías: {}", idCompanies, e);
      setLogs(
          EventLog.builder()
              .service(traceElement.getClassName())
              .method(traceElement.getMethodName())
              .information("Error al parsear IDs: " + e.getMessage()),
          RESPONSE,
          LEVEL_001);
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Formato inválido de IDs. Se espera: 1,2,3");
    }

    try {
      CompanyDetailDtoResponse response = companyService.getCompaniesByIdCompanies(ids);
      setLogs(
          EventLog.builder()
              .service(traceElement.getClassName())
              .method(traceElement.getMethodName())
              .information(
                  "Compañías encontradas: "
                      + (response.getCompanyDetailsDtos() != null
                          ? response.getCompanyDetailsDtos().size()
                          : 0)),
          RESPONSE,
          LEVEL_001);
      return response;
    } catch (Exception e) {
      log.error("Error al obtener compañías por IDs: {}", ids, e);
      setLogs(
          EventLog.builder()
              .service(traceElement.getClassName())
              .method(traceElement.getMethodName())
              .information("Error: " + e.getMessage()),
          RESPONSE,
          LEVEL_001);
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Error al obtener compañías: " + e.getMessage());
    }
  }

  /** Sube el logo de una compañía POST /api/company/{idCompany}/logo */
  @PostMapping("/{idCompany}/logo")
  public ResponseEntity<?> uploadLogo(
      @PathVariable Long idCompany, @RequestParam("file") MultipartFile file) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information("ID Company: " + idCompany + ", File: " + file.getOriginalFilename()),
        REQUEST,
        LEVEL_001);
    try {
      String logoPath = companyService.uploadLogo(idCompany, file);
      Map<String, String> response = new HashMap<>();
      response.put("message", "Logo subido exitosamente");
      response.put("logoPath", logoPath);
      setLogs(
          EventLog.builder()
              .service(traceElement.getClassName())
              .method(traceElement.getMethodName())
              .information("Logo subido: " + logoPath),
          RESPONSE,
          LEVEL_001);
      return ResponseEntity.ok().body(response);
    } catch (IllegalArgumentException e) {
      Map<String, String> error = new HashMap<>();
      error.put("error", e.getMessage());
      setLogs(
          EventLog.builder()
              .service(traceElement.getClassName())
              .method(traceElement.getMethodName())
              .information("Error: " + e.getMessage()),
          RESPONSE,
          LEVEL_001);
      return ResponseEntity.badRequest().body(error);
    } catch (Exception e) {
      Map<String, String> error = new HashMap<>();
      error.put("error", "Error al subir el logo: " + e.getMessage());
      setLogs(
          EventLog.builder()
              .service(traceElement.getClassName())
              .method(traceElement.getMethodName())
              .information("Error: " + e.getMessage()),
          RESPONSE,
          LEVEL_001);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
  }

  /** Elimina el logo de una compañía DELETE /api/company/{idCompany}/logo */
  @DeleteMapping("/{idCompany}/logo")
  public ResponseEntity<?> deleteLogo(@PathVariable Long idCompany) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information("ID Company: " + idCompany),
        REQUEST,
        LEVEL_001);
    try {
      companyService.deleteLogo(idCompany);
      Map<String, String> response = new HashMap<>();
      response.put("message", "Logo eliminado exitosamente");
      setLogs(
          EventLog.builder()
              .service(traceElement.getClassName())
              .method(traceElement.getMethodName())
              .information("Logo eliminado"),
          RESPONSE,
          LEVEL_001);
      return ResponseEntity.ok().body(response);
    } catch (Exception e) {
      Map<String, String> error = new HashMap<>();
      error.put("error", "Error al eliminar el logo: " + e.getMessage());
      setLogs(
          EventLog.builder()
              .service(traceElement.getClassName())
              .method(traceElement.getMethodName())
              .information("Error: " + e.getMessage()),
          RESPONSE,
          LEVEL_001);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
  }

  /** Obtiene la ruta del logo de una compañía GET /api/company/{idCompany}/logo */
  @GetMapping("/{idCompany}/logo")
  public ResponseEntity<?> getLogo(@PathVariable Long idCompany) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information("ID Company: " + idCompany),
        REQUEST,
        LEVEL_001);
    try {
      String logoPath = companyService.getLogoPath(idCompany);
      if (logoPath != null && new File(logoPath).exists()) {
        Map<String, String> response = new HashMap<>();
        response.put("logoPath", logoPath);
        setLogs(
            EventLog.builder()
                .service(traceElement.getClassName())
                .method(traceElement.getMethodName())
                .information("Logo encontrado: " + logoPath),
            RESPONSE,
            LEVEL_001);
        return ResponseEntity.ok().body(response);
      }
      setLogs(
          EventLog.builder()
              .service(traceElement.getClassName())
              .method(traceElement.getMethodName())
              .information("Logo no encontrado"),
          RESPONSE,
          LEVEL_001);
      return ResponseEntity.notFound().build();
    } catch (Exception e) {
      Map<String, String> error = new HashMap<>();
      error.put("error", "Error al obtener el logo: " + e.getMessage());
      setLogs(
          EventLog.builder()
              .service(traceElement.getClassName())
              .method(traceElement.getMethodName())
              .information("Error: " + e.getMessage()),
          RESPONSE,
          LEVEL_001);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
  }

  /**
   * Obtiene la imagen del logo de una compañía (para mostrar en el frontend) GET
   * /api/company/{idCompany}/logo/image
   */
  @GetMapping("/{idCompany}/logo/image")
  public ResponseEntity<?> getLogoImage(@PathVariable Long idCompany) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information("ID Company: " + idCompany),
        REQUEST,
        LEVEL_001);
    try {
      String logoPath = companyService.getLogoPath(idCompany);
      if (logoPath != null) {
        File logoFile = new File(logoPath);
        if (logoFile.exists()) {
          // Determinar el tipo de contenido basado en la extensión
          String contentType = "image/jpeg"; // Por defecto
          String fileName = logoFile.getName().toLowerCase();
          if (fileName.endsWith(".png")) {
            contentType = "image/png";
          } else if (fileName.endsWith(".gif")) {
            contentType = "image/gif";
          } else if (fileName.endsWith(".webp")) {
            contentType = "image/webp";
          }

          // Leer el archivo y retornarlo como respuesta
          byte[] imageBytes = Files.readAllBytes(logoFile.toPath());
          setLogs(
              EventLog.builder()
                  .service(traceElement.getClassName())
                  .method(traceElement.getMethodName())
                  .information("Imagen servida: " + logoPath),
              RESPONSE,
              LEVEL_001);
          return ResponseEntity.ok()
              .contentType(MediaType.parseMediaType(contentType))
              .body(imageBytes);
        }
      }
      setLogs(
          EventLog.builder()
              .service(traceElement.getClassName())
              .method(traceElement.getMethodName())
              .information("Logo no encontrado"),
          RESPONSE,
          LEVEL_001);
      return ResponseEntity.notFound().build();
    } catch (Exception e) {
      setLogs(
          EventLog.builder()
              .service(traceElement.getClassName())
              .method(traceElement.getMethodName())
              .information("Error: " + e.getMessage()),
          RESPONSE,
          LEVEL_001);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Obtiene empresas excluyendo las que tienen IDs en la lista proporcionada GET
   * /api/company/without-ids?excludeIds=1,2,3
   */
  @GetMapping("/without-ids")
  @ResponseStatus(HttpStatus.OK)
  public List<CompanyDetailsDto> getCompaniesWithoutIds(
      @RequestParam(value = "excludeIds", required = false) List<Long> excludeIds) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information("Exclude IDs: " + excludeIds),
        REQUEST,
        LEVEL_001);
    List<CompanyDetailsDto> companies = companyService.getCompaniesExcludingIds(excludeIds);
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information("Companies found: " + companies.size()),
        RESPONSE,
        LEVEL_001);
    return companies;
  }

  private static BasicResponse getBasicResponse() {
    return BasicResponse.builder().status(HttpStatus.OK.getReasonPhrase()).build();
  }
}
