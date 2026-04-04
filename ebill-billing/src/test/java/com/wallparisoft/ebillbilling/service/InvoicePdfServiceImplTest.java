package com.wallparisoft.ebillbilling.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.wallparisoft.ebillbilling.dto.InvoicePdfData;
import com.wallparisoft.ebillbilling.service.impl.InvoicePdfServiceImpl;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class InvoicePdfServiceImplTest {

  private static final Path LOGO_PATH =
      Path.of("D:\\ws_billing\\ebill-billing\\src\\test\\resources\\invoice_logo.png");

  private final ParamsService paramsService = mock(ParamsService.class);
  private final InvoicePdfServiceImpl invoicePdfService;

  @TempDir Path outputDirectory;

  InvoicePdfServiceImplTest() {
    // Configurar el mock para retornar cadena vacía cuando se solicite SUMMARY_TOTALS_LABELS
    when(paramsService.getParamValueOrDefault("SUMMARY_TOTALS_LABELS", "")).thenReturn("");

    invoicePdfService =
        new InvoicePdfServiceImpl(paramsService) {
          @Override
          public Map<String, Object> buildInvoiceParameters(InvoicePdfData data) {
            Map<String, Object> params = super.buildInvoiceParameters(data);
            try {
              params.put("logoUrl", LOGO_PATH.toUri().toURL());
            } catch (MalformedURLException e) {
              throw new IllegalStateException("No se pudo cargar el logo de pruebas", e);
            }
            return params;
          }
        };
  }

  @Test
  void shouldGeneratePdfFromAuthorizationXml() throws Exception {
    //    String authorizationXml =
    //        Files.readString(
    //            Path.of("src/test/resources/factura_autorizada.xml"), StandardCharsets.UTF_8);
    String authorizationXml =
        Files.readString(Path.of("src/test/resources/036102000265218.xml"), StandardCharsets.UTF_8);

    Path pdfPath = invoicePdfService.generateAndSaveInvoicePdf(authorizationXml, outputDirectory);

    assertTrue(Files.exists(pdfPath), "El archivo PDF debería generarse");
    assertTrue(Files.size(pdfPath) > 0, "El PDF generado no puede estar vacío");
  }
}
