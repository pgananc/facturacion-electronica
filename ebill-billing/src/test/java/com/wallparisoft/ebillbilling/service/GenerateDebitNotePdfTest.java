package com.wallparisoft.ebillbilling.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.wallparisoft.ebillbilling.service.impl.DebitNotePdfServiceImpl;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Clase de prueba para generar un PDF de nota de débito usando la plantilla
 * debit_note_template.jrxml
 *
 * <p>Para ejecutar: mvn exec:java
 * -Dexec.mainClass="com.wallparisoft.ebillbilling.service.GenerateDebitNotePdfTest"
 */
public class GenerateDebitNotePdfTest {

  public static void main(String[] args) {
    try {
      // Configurar el servicio con mocks
      ParamsService paramsService = mock(ParamsService.class);
      when(paramsService.getParamValueOrDefault("SUMMARY_TOTALS_LABELS", "")).thenReturn("");
      when(paramsService.getParamValueOrDefault("COMPANY_REGIME_MAPPING", "")).thenReturn("");

      DebitNotePdfServiceImpl debitNotePdfService = new DebitNotePdfServiceImpl(paramsService);

      // Leer el XML de autorización de ejemplo
      Path xmlPath = Paths.get("src/test/resources/debit_note_autorizada_ejemplo.xml");
      if (!Files.exists(xmlPath)) {
        System.err.println(
            "❌ No se encontró el archivo XML de ejemplo: " + xmlPath.toAbsolutePath());
        return;
      }

      String authorizationXml = Files.readString(xmlPath, StandardCharsets.UTF_8);

      // Crear directorio de salida
      Path outputDir = Paths.get("target/test-pdf-output");
      Files.createDirectories(outputDir);

      // Generar el PDF
      System.out.println("🔄 Generando PDF de nota de débito...");
      Path pdfPath = debitNotePdfService.generateAndSaveDebitNotePdf(authorizationXml, outputDir);

      System.out.println("✅ PDF generado exitosamente!");
      System.out.println("📄 Ubicación: " + pdfPath.toAbsolutePath());
      System.out.println("📊 Tamaño: " + Files.size(pdfPath) + " bytes");

      // Intentar abrir el PDF (Windows)
      if (System.getProperty("os.name").toLowerCase().contains("win")) {
        try {
          Runtime.getRuntime().exec("cmd /c start \"\" \"" + pdfPath.toAbsolutePath() + "\"");
          System.out.println("🚀 PDF abierto automáticamente");
        } catch (Exception e) {
          System.out.println("⚠️  No se pudo abrir el PDF automáticamente");
        }
      }

    } catch (Exception e) {
      System.err.println("❌ Error al generar el PDF: " + e.getMessage());
      e.printStackTrace();
    }
  }
}
