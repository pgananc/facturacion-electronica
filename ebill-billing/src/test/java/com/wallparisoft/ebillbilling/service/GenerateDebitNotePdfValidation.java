package com.wallparisoft.ebillbilling.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.wallparisoft.ebillbilling.service.impl.DebitNotePdfServiceImpl;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utilidad para generar un PDF de nota de débito de prueba para validar la plantilla.
 *
 * <p>Ejecutar desde el IDE o con: mvn test-compile exec:java
 * -Dexec.mainClass="com.wallparisoft.ebillbilling.service.GenerateDebitNotePdfValidation"
 */
public class GenerateDebitNotePdfValidation {

  public static void main(String[] args) {
    try {
      System.out.println("🔄 Iniciando generación de PDF de nota de débito...\n");

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
        System.err.println("   Por favor, asegúrate de que el archivo existe.");
        return;
      }

      System.out.println("📄 Leyendo XML de ejemplo: " + xmlPath.toAbsolutePath());
      String authorizationXml = Files.readString(xmlPath, StandardCharsets.UTF_8);

      // Crear directorio de salida en target para fácil acceso
      Path outputDir = Paths.get("target/debit-note-pdf-validation");
      Files.createDirectories(outputDir);
      System.out.println("📁 Directorio de salida: " + outputDir.toAbsolutePath());

      // Generar el PDF
      System.out.println("🔄 Generando PDF usando plantilla debit_note_template.jrxml...");
      Path pdfPath =
          debitNotePdfService.generateAndSaveDebitNotePdf(
              authorizationXml, outputDir, null, "SPECIAL", "1344");

      System.out.println("\n✅ PDF generado exitosamente!");
      System.out.println(
          "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
      System.out.println("📄 Ubicación del PDF:");
      System.out.println("   " + pdfPath.toAbsolutePath());
      System.out.println(
          "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
      System.out.println("📊 Tamaño: " + Files.size(pdfPath) + " bytes");
      System.out.println("📋 Nombre: " + pdfPath.getFileName());
      System.out.println("\n✨ El PDF está listo para validación!");

      // Intentar abrir el PDF (Windows)
      if (System.getProperty("os.name").toLowerCase().contains("win")) {
        try {
          Runtime.getRuntime().exec("cmd /c start \"\" \"" + pdfPath.toAbsolutePath() + "\"");
          System.out.println("🚀 PDF abierto automáticamente en el visor predeterminado");
        } catch (Exception e) {
          System.out.println(
              "⚠️  No se pudo abrir el PDF automáticamente. Por favor, ábrelo manualmente.");
        }
      } else {
        System.out.println("💡 Para abrir el PDF, navega a: " + pdfPath.toAbsolutePath());
      }

    } catch (Exception e) {
      System.err.println("\n❌ Error al generar el PDF:");
      System.err.println("   " + e.getMessage());
      e.printStackTrace();
    }
  }
}
