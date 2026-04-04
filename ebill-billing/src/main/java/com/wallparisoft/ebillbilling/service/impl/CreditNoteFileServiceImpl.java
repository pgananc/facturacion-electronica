package com.wallparisoft.ebillbilling.service.impl;

import com.wallparisoft.ebillbilling.entity.AuthorizationSri;
import com.wallparisoft.ebillbilling.entity.CreditNote;
import com.wallparisoft.ebillbilling.repository.ICreditNoteRepo;
import com.wallparisoft.ebillbilling.service.ICreditNoteFileService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditNoteFileServiceImpl implements ICreditNoteFileService {

  private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB
  private static final String DEFAULT_PDF_PREFIX = "credit-note";

  private final ICreditNoteRepo creditNoteRepo;
  private final SriXmlStorageServiceImpl sriXmlStorageService;

  /**
   * Obtiene el contenido del archivo XML de una nota de crédito autorizada.
   *
   * @param creditNoteId ID de la nota de crédito
   * @return Bytes del archivo XML
   */
  @Override
  public byte[] getXmlFile(Long creditNoteId) {
    CreditNote creditNote = findCreditNoteWithAuthorization(creditNoteId);
    AuthorizationSri authorization = creditNote.getAuthorizationSri();

    validateAuthorizationStatus(authorization);

    String storagePath = authorization.getAuthorizationStoragePath();
    String accessKey = authorization.getAccessKey();

    validateStoragePath(storagePath, "XML");
    validateAccessKey(accessKey);

    Path xmlPath = validateAndResolvePath(storagePath, accessKey + ".xml");
    validateFileExists(xmlPath, "XML");
    validateFileSize(xmlPath);

    logFileAccess(creditNoteId, "XML", xmlPath.getFileName().toString(), getCurrentUser());
    return readFile(xmlPath);
  }

  /**
   * Obtiene la ruta del archivo XML (para streaming).
   *
   * @param creditNoteId ID de la nota de crédito
   * @return Path del archivo XML
   */
  @Override
  public Path getXmlFilePath(Long creditNoteId) {
    CreditNote creditNote = findCreditNoteWithAuthorization(creditNoteId);
    AuthorizationSri authorization = creditNote.getAuthorizationSri();

    validateAuthorizationStatus(authorization);

    String storagePath = authorization.getAuthorizationStoragePath();
    String accessKey = authorization.getAccessKey();

    validateStoragePath(storagePath, "XML");
    validateAccessKey(accessKey);

    Path xmlPath = validateAndResolvePath(storagePath, accessKey + ".xml");
    validateFileExists(xmlPath, "XML");

    return xmlPath;
  }

  /**
   * Obtiene el contenido del archivo PDF de una nota de crédito autorizada.
   *
   * @param creditNoteId ID de la nota de crédito
   * @return Bytes del archivo PDF
   */
  @Override
  public byte[] getPdfFile(Long creditNoteId) {
    CreditNote creditNote = findCreditNoteWithAuthorization(creditNoteId);
    AuthorizationSri authorization = creditNote.getAuthorizationSri();

    validateAuthorizationStatus(authorization);

    String storagePath = authorization.getAuthorizationStoragePath();
    validateStoragePath(storagePath, "PDF");

    String fileName = buildPdfFileName(creditNote, authorization);
    Path pdfPath = validateAndResolvePath(storagePath, fileName);
    validateFileExists(pdfPath, "PDF");
    validateFileSize(pdfPath);

    logFileAccess(creditNoteId, "PDF", fileName, getCurrentUser());
    return readFile(pdfPath);
  }

  /**
   * Obtiene la ruta del archivo PDF (para streaming).
   *
   * @param creditNoteId ID de la nota de crédito
   * @return Path del archivo PDF
   */
  @Override
  public Path getPdfFilePath(Long creditNoteId) {
    CreditNote creditNote = findCreditNoteWithAuthorization(creditNoteId);
    AuthorizationSri authorization = creditNote.getAuthorizationSri();

    validateAuthorizationStatus(authorization);

    String storagePath = authorization.getAuthorizationStoragePath();
    validateStoragePath(storagePath, "PDF");

    String fileName = buildPdfFileName(creditNote, authorization);
    Path pdfPath = validateAndResolvePath(storagePath, fileName);
    validateFileExists(pdfPath, "PDF");

    return pdfPath;
  }

  /**
   * Obtiene el nombre del archivo XML para descarga.
   *
   * @param creditNoteId ID de la nota de crédito
   * @return Nombre del archivo XML
   */
  public String getXmlFileName(Long creditNoteId) {
    CreditNote creditNote = findCreditNoteWithAuthorization(creditNoteId);
    return creditNote.getAuthorizationSri().getAccessKey() + ".xml";
  }

  /**
   * Obtiene el nombre del archivo PDF para descarga.
   *
   * @param creditNoteId ID de la nota de crédito
   * @return Nombre del archivo PDF
   */
  public String getPdfFileName(Long creditNoteId) {
    CreditNote creditNote = findCreditNoteWithAuthorization(creditNoteId);
    AuthorizationSri authorization = creditNote.getAuthorizationSri();
    return buildPdfFileName(creditNote, authorization);
  }

  // ========== Métodos privados de validación ==========

  private CreditNote findCreditNoteWithAuthorization(Long creditNoteId) {
    CreditNote creditNote =
        creditNoteRepo
            .findById(creditNoteId)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "No se encontró la nota de crédito con ID: " + creditNoteId));

    if (creditNote.getAuthorizationSri() == null) {
      throw new IllegalArgumentException(
          "La nota de crédito con ID " + creditNoteId + " no ha sido autorizada por el SRI");
    }

    return creditNote;
  }

  private void validateAuthorizationStatus(AuthorizationSri authorization) {
    if (!"AUTORIZADO".equalsIgnoreCase(authorization.getAuthorizationStatusSri())) {
      throw new IllegalStateException(
          String.format(
              "La nota de crédito no está autorizada. Estado actual: %s",
              authorization.getAuthorizationStatusSri()));
    }
  }

  private void validateStoragePath(String storagePath, String fileType) {
    if (!StringUtils.hasText(storagePath)) {
      throw new IllegalStateException(
          String.format(
              "La nota de crédito no tiene ruta de almacenamiento configurada para %s", fileType));
    }
  }

  private void validateAccessKey(String accessKey) {
    if (!StringUtils.hasText(accessKey)) {
      throw new IllegalStateException("La nota de crédito no tiene clave de acceso");
    }
  }

  private Path validateAndResolvePath(String basePath, String fileName) {
    try {
      Path base = Paths.get(basePath).normalize().toAbsolutePath();
      Path resolved = base.resolve(fileName).normalize();

      // Validar que el archivo resuelto esté dentro del directorio base (protección path traversal)
      if (!resolved.startsWith(base)) {
        throw new SecurityException(
            "Intento de acceso a ruta fuera del directorio permitido: " + resolved);
      }

      return resolved;
    } catch (Exception ex) {
      throw new IllegalStateException(
          "Error al resolver la ruta del archivo: " + ex.getMessage(), ex);
    }
  }

  private void validateFileExists(Path filePath, String fileType) {
    if (!Files.exists(filePath)) {
      log.error("El archivo {} no existe en la ruta: {}", fileType, filePath);
      throw new IllegalStateException(
          String.format("El archivo %s no existe en la ruta: %s", fileType, filePath));
    }

    if (!Files.isRegularFile(filePath)) {
      log.error("La ruta {} no es un archivo regular: {}", fileType, filePath);
      throw new IllegalStateException(
          String.format("La ruta no es un archivo válido: %s", filePath));
    }
  }

  private void validateFileSize(Path filePath) {
    try {
      long fileSize = Files.size(filePath);
      if (fileSize > MAX_FILE_SIZE) {
        throw new IllegalStateException(
            String.format(
                "El archivo excede el tamaño máximo permitido (%d bytes): %d bytes",
                MAX_FILE_SIZE, fileSize));
      }
    } catch (IOException ex) {
      throw new IllegalStateException("No se pudo obtener el tamaño del archivo", ex);
    }
  }

  private String buildPdfFileName(CreditNote creditNote, AuthorizationSri authorization) {
    // Opción 1: Usar creditNoteNumber si está disponible
    if (StringUtils.hasText(creditNote.getCreditNoteNumber())) {
      return creditNote.getCreditNoteNumber() + ".pdf";
    }

    // Opción 2: Intentar leer desde el XML almacenado
    try {
      String storagePath = authorization.getAuthorizationStoragePath();
      String accessKey = authorization.getAccessKey();
      String xmlFilePath = storagePath + "/" + accessKey + ".xml";

      Optional<String> xmlContent = sriXmlStorageService.loadAuthorizationXml(xmlFilePath);

      if (xmlContent.isPresent()) {
        // El nombre del PDF se construye como: {estab}-{ptoEmi}-{secuencial}.pdf
        // Esto se puede extraer del XML, pero por simplicidad usamos fallback
        // Si necesitas parsear el XML, puedes usar la misma lógica que CreditNotePdfServiceImpl
      }
    } catch (Exception ex) {
      log.debug("No se pudo leer XML para construir nombre PDF: {}", ex.getMessage());
    }

    // Opción 3: Fallback usando ID de nota de crédito
    return String.format("%s-%d.pdf", DEFAULT_PDF_PREFIX, creditNote.getIdCreditNote());
  }

  private byte[] readFile(Path filePath) {
    try {
      return Files.readAllBytes(filePath);
    } catch (IOException ex) {
      log.error("Error al leer el archivo desde: {}", filePath, ex);
      throw new IllegalStateException(
          String.format("No se pudo leer el archivo: %s", ex.getMessage()), ex);
    }
  }

  private String getCurrentUser() {
    try {
      org.springframework.security.core.Authentication authentication =
          org.springframework.security.core.context.SecurityContextHolder.getContext()
              .getAuthentication();
      if (authentication != null && authentication.isAuthenticated()) {
        return authentication.getName();
      }
    } catch (Exception ex) {
      log.debug("No se pudo obtener usuario actual: {}", ex.getMessage());
    }
    return "unknown";
  }

  private void logFileAccess(Long creditNoteId, String fileType, String fileName, String user) {
    log.info(
        "File access - CreditNote: {}, Type: {}, File: {}, User: {}, Timestamp: {}",
        creditNoteId,
        fileType,
        fileName,
        user,
        Instant.now());
  }
}
