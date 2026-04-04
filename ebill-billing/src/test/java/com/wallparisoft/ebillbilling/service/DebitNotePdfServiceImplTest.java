package com.wallparisoft.ebillbilling.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.wallparisoft.ebillbilling.service.impl.DebitNotePdfServiceImpl;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DebitNotePdfServiceImplTest {

  private final ParamsService paramsService = mock(ParamsService.class);
  private final DebitNotePdfServiceImpl debitNotePdfService;

  @TempDir Path outputDirectory;

  DebitNotePdfServiceImplTest() {
    // Configurar el mock para retornar valores por defecto
    when(paramsService.getParamValueOrDefault("SUMMARY_TOTALS_LABELS", "")).thenReturn("");
    when(paramsService.getParamValueOrDefault("COMPANY_REGIME_MAPPING", "")).thenReturn("");

    debitNotePdfService = new DebitNotePdfServiceImpl(paramsService);
  }

  @Test
  void shouldGeneratePdfFromAuthorizationXml() throws Exception {
    // Leer el XML de autorización de ejemplo
    Path xmlPath = Paths.get("src/test/resources/debit_note_autorizada_ejemplo.xml");
    String authorizationXml = Files.readString(xmlPath, StandardCharsets.UTF_8);

    // Generar el PDF
    Path pdfPath =
        debitNotePdfService.generateAndSaveDebitNotePdf(authorizationXml, outputDirectory);

    // Validar que el PDF se generó correctamente
    assertTrue(Files.exists(pdfPath), "El archivo PDF debería generarse");
    assertTrue(Files.size(pdfPath) > 0, "El PDF generado no puede estar vacío");

    System.out.println("✅ PDF generado exitosamente en: " + pdfPath.toAbsolutePath());
  }

  @Test
  void shouldGeneratePdfWithLogo() throws Exception {
    // Leer el XML de autorización de ejemplo
    Path xmlPath = Paths.get("src/test/resources/debit_note_autorizada_ejemplo.xml");
    String authorizationXml = Files.readString(xmlPath, StandardCharsets.UTF_8);

    // Ruta del logo (si existe)
    Path logoPath = Paths.get("src/test/resources/invoice_logo.png");
    String logoPathStr = Files.exists(logoPath) ? logoPath.toAbsolutePath().toString() : null;

    // Generar el PDF con logo
    Path pdfPath =
        debitNotePdfService.generateAndSaveDebitNotePdf(
            authorizationXml, outputDirectory, logoPathStr);

    // Validar que el PDF se generó correctamente
    assertTrue(Files.exists(pdfPath), "El archivo PDF debería generarse");
    assertTrue(Files.size(pdfPath) > 0, "El PDF generado no puede estar vacío");

    System.out.println("✅ PDF con logo generado exitosamente en: " + pdfPath.toAbsolutePath());
  }

  @Test
  void shouldGeneratePdfWithAllParameters() throws Exception {
    // Leer el XML de autorización de ejemplo
    Path xmlPath = Paths.get("src/test/resources/debit_note_autorizada_ejemplo.xml");
    String authorizationXml = Files.readString(xmlPath, StandardCharsets.UTF_8);

    // Ruta del logo (si existe)
    Path logoPath = Paths.get("src/test/resources/invoice_logo.png");
    String logoPathStr = Files.exists(logoPath) ? logoPath.toAbsolutePath().toString() : null;

    // Generar el PDF con todos los parámetros
    Path pdfPath =
        debitNotePdfService.generateAndSaveDebitNotePdf(
            authorizationXml, outputDirectory, logoPathStr, "SPECIAL", "1344");

    // Validar que el PDF se generó correctamente
    assertTrue(Files.exists(pdfPath), "El archivo PDF debería generarse");
    assertTrue(Files.size(pdfPath) > 0, "El PDF generado no puede estar vacío");

    System.out.println(
        "✅ PDF con todos los parámetros generado exitosamente en: " + pdfPath.toAbsolutePath());
  }
}
