package com.wallparisoft.ebillbilling.service.impl;

import com.wallparisoft.ebillbilling.service.ParamsService;
import com.wallparisoft.ebillbilling.service.SriXmlStorageService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class SriXmlStorageServiceImpl implements SriXmlStorageService {

  private static final String DEFAULT_CLIENT_FOLDER = "DESCONOCIDO";

  private final ParamsService paramsService;

  @Override
  public Path storeAuthorizationXml(
      String xmlContent,
      String buyerIdentification,
      LocalDateTime authorizationDate,
      String accessKey) {
    if (!StringUtils.hasText(accessKey)) {
      throw new IllegalArgumentException("La clave de acceso es obligatoria para guardar el XML.");
    }

    Path basePath = resolveBaseDirectory();
    return storeXml(basePath, xmlContent, buyerIdentification, authorizationDate, accessKey);
  }

  @Override
  public Path storeCreditNoteXml(
      String xmlContent,
      String buyerIdentification,
      LocalDateTime authorizationDate,
      String accessKey) {
    if (!StringUtils.hasText(accessKey)) {
      throw new IllegalArgumentException("La clave de acceso es obligatoria para guardar el XML.");
    }

    Path basePath = resolveCreditNoteBaseDirectory();
    return storeXml(basePath, xmlContent, buyerIdentification, authorizationDate, accessKey);
  }

  @Override
  public Path storeDebitNoteXml(
      String xmlContent,
      String buyerIdentification,
      LocalDateTime authorizationDate,
      String accessKey) {
    if (!StringUtils.hasText(accessKey)) {
      throw new IllegalArgumentException("La clave de acceso es obligatoria para guardar el XML.");
    }

    Path basePath = resolveDebitNoteBaseDirectory();
    return storeXml(basePath, xmlContent, buyerIdentification, authorizationDate, accessKey);
  }

  @Override
  public Path storeGuideXml(
      String xmlContent,
      String buyerIdentification,
      LocalDateTime authorizationDate,
      String accessKey) {
    if (!StringUtils.hasText(accessKey)) {
      throw new IllegalArgumentException("La clave de acceso es obligatoria para guardar el XML.");
    }

    Path basePath = resolveGuideBaseDirectory();
    return storeXml(basePath, xmlContent, buyerIdentification, authorizationDate, accessKey);
  }

  private Path storeXml(
      Path basePath,
      String xmlContent,
      String buyerIdentification,
      LocalDateTime authorizationDate,
      String accessKey) {
    LocalDate effectiveDate =
        Optional.ofNullable(authorizationDate)
            .map(LocalDateTime::toLocalDate)
            .orElse(LocalDate.now());

    String sanitizedIdentification = sanitize(buyerIdentification);

    Path targetDirectory =
        basePath
            .resolve(sanitizedIdentification)
            .resolve(String.format(Locale.ROOT, "%04d", effectiveDate.getYear()))
            .resolve(String.format(Locale.ROOT, "%02d", effectiveDate.getMonthValue()))
            .resolve(String.format(Locale.ROOT, "%02d", effectiveDate.getDayOfMonth()));

    try {
      Files.createDirectories(targetDirectory);
      Path xmlPath = targetDirectory.resolve(accessKey + ".xml");
      String safeContent = Optional.ofNullable(xmlContent).orElse("");
      Files.writeString(xmlPath, safeContent, StandardCharsets.UTF_8);
      return xmlPath;
    } catch (IOException ex) {
      throw new IllegalStateException("No se pudo guardar el XML autorizado en disco.", ex);
    }
  }

  public Optional<String> loadAuthorizationXml(String storedPath) {
    return loadAuthorizationXmlBytes(storedPath)
        .map(bytes -> new String(bytes, StandardCharsets.UTF_8));
  }

  public Optional<byte[]> loadAuthorizationXmlBytes(String storedPath) {
    if (!StringUtils.hasText(storedPath)) {
      return Optional.empty();
    }

    Path xmlPath = Paths.get(storedPath);
    if (!Files.exists(xmlPath)) {
      log.warn("El archivo XML no existe en la ruta {}", xmlPath);
      return Optional.empty();
    }

    try {
      return Optional.of(Files.readAllBytes(xmlPath));
    } catch (IOException ex) {
      log.error("No se pudo leer el archivo XML almacenado en {}", xmlPath, ex);
      return Optional.empty();
    }
  }

  public boolean exists(String storedPath) {
    if (!StringUtils.hasText(storedPath)) {
      return false;
    }
    Path xmlPath = Paths.get(storedPath);
    return Files.isRegularFile(xmlPath);
  }

  private Path resolveBaseDirectory() {
    String configuredBaseDir = paramsService.getBaseDir();
    if (!StringUtils.hasText(configuredBaseDir)) {
      throw new IllegalStateException(
          "El parámetro INVOICE_STORAGE_BASE_DIR no está configurado en la base de datos.");
    }
    return Paths.get(configuredBaseDir);
  }

  private Path resolveCreditNoteBaseDirectory() {
    String configuredBaseDir = paramsService.getCreditNoteBaseDir();
    if (!StringUtils.hasText(configuredBaseDir)) {
      throw new IllegalStateException(
          "El parámetro CREDIT_NOTE_STORAGE_BASE_DIR no está configurado en la base de datos.");
    }
    return Paths.get(configuredBaseDir);
  }

  private Path resolveDebitNoteBaseDirectory() {
    String configuredBaseDir = paramsService.getDebitNoteBaseDir();
    if (!StringUtils.hasText(configuredBaseDir)) {
      throw new IllegalStateException(
          "El parámetro DEBIT_NOTE_STORAGE_BASE_DIR no está configurado en la base de datos.");
    }
    return Paths.get(configuredBaseDir);
  }

  private Path resolveGuideBaseDirectory() {
    String configuredBaseDir = paramsService.getGuideBaseDir();
    if (!StringUtils.hasText(configuredBaseDir)) {
      throw new IllegalStateException(
          "El parámetro GUIDE_STORAGE_BASE_DIR no está configurado en la base de datos.");
    }
    return Paths.get(configuredBaseDir);
  }

  private String sanitize(String identification) {
    String candidate =
        Optional.ofNullable(identification)
            .map(String::trim)
            .filter(StringUtils::hasText)
            .orElse(DEFAULT_CLIENT_FOLDER);

    String sanitized = candidate.replaceAll("[^\\w.-]", "_");
    return StringUtils.hasText(sanitized) ? sanitized : DEFAULT_CLIENT_FOLDER;
  }
}
