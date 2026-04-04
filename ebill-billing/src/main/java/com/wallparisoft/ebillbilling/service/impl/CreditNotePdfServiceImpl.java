package com.wallparisoft.ebillbilling.service.impl;

import com.wallparisoft.ebillbilling.service.CreditNotePdfService;
import com.wallparisoft.ebillbilling.service.InvoicePdfService;
import com.wallparisoft.ebillbilling.service.ParamsService;
import com.wallparisoft.integration.sri.model.DetalleNotaCredito;
import com.wallparisoft.integration.sri.model.InfoNotaCredito;
import com.wallparisoft.integration.sri.model.InfoTributaria;
import com.wallparisoft.integration.sri.model.NotaCredito;
import com.wallparisoft.integration.sri.model.TotalImpuesto;
import com.wallparisoft.integration.sri.util.XmlUtil;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.krysalis.barcode4j.impl.code128.Code128Bean;
import org.krysalis.barcode4j.output.bitmap.BitmapCanvasProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Servicio para generar PDF de notas de crédito usando el template específico
 * credit_note_template.jrxml
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CreditNotePdfServiceImpl implements CreditNotePdfService {

  private final InvoicePdfService invoicePdfService;
  private final ParamsService paramsService;

  private static final String SUMMARY_TOTALS_LABELS_PARAM = "SUMMARY_TOTALS_LABELS";

  private static final Map<String, String> TAX_CODE_LABELS =
      Map.of("2", "IVA", "3", "ICE", "5", "IRBPNR");

  private static final Map<String, String> IVA_RATE_LABELS =
      Map.of("0", "0%", "2", "12%", "3", "14%", "4", "15%", "5", "5%");

  private static final DateTimeFormatter AUTHORIZATION_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private static final DateTimeFormatter AUTHORIZATION_ISO_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
  private static final DateTimeFormatter AUTHORIZATION_ISO_OFFSET_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
  private static final DateTimeFormatter DISPLAY_DATE =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

  private static final Map<String, String> AMBIENTE_LABELS =
      Map.of("1", "PRUEBAS", "2", "PRODUCCIÓN");

  private static final Map<String, String> TIPO_EMISION_LABELS =
      Map.of("1", "EMISIÓN NORMAL", "2", "EMISIÓN POR INDISPONIBILIDAD DEL SISTEMA");

  // Clases auxiliares para manejo de impuestos y totales
  private static final class TaxKey {

    private final String codigo;
    private final String codigoPorcentaje;
    private final String rateLabel;

    private TaxKey(String codigo, String codigoPorcentaje, String rateLabel) {
      this.codigo = codigo != null ? codigo : "";
      this.codigoPorcentaje = codigoPorcentaje != null ? codigoPorcentaje : "";
      this.rateLabel = rateLabel != null ? rateLabel : "";
    }

    String codigo() {
      return codigo;
    }

    String codigoPorcentaje() {
      return codigoPorcentaje;
    }

    String rateLabel() {
      return rateLabel;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      TaxKey taxKey = (TaxKey) o;
      return Objects.equals(codigo, taxKey.codigo)
          && Objects.equals(codigoPorcentaje, taxKey.codigoPorcentaje)
          && Objects.equals(rateLabel, taxKey.rateLabel);
    }

    @Override
    public int hashCode() {
      return Objects.hash(codigo, codigoPorcentaje, rateLabel);
    }
  }

  private static class TaxAccumulator {

    private BigDecimal baseSum = BigDecimal.ZERO;
    private BigDecimal taxSum = BigDecimal.ZERO;

    void accumulate(BigDecimal base, BigDecimal tax) {
      if (base != null) {
        baseSum = baseSum.add(base);
      }
      if (tax != null) {
        taxSum = taxSum.add(tax);
      }
    }

    BigDecimal baseSum() {
      return baseSum;
    }

    BigDecimal taxSum() {
      return taxSum;
    }
  }

  public static class SummaryRow {

    private final String label;
    private final String amount;

    SummaryRow(String label, String amount) {
      this.label = label;
      this.amount = amount;
    }

    public String getLabel() {
      return label;
    }

    public String getAmount() {
      return amount;
    }
  }

  // Reutilizar DetailRow de InvoicePdfService
  public static class DetailRow {

    private final String codigoPrincipal;
    private final String descripcion;
    private final String cantidad;
    private final String precioUnitario;
    private final String descuento;
    private final String precioTotalSinImpuesto;

    DetailRow(
        String codigoPrincipal,
        String descripcion,
        String cantidad,
        String precioUnitario,
        String descuento,
        String precioTotalSinImpuesto) {
      this.codigoPrincipal = codigoPrincipal;
      this.descripcion = descripcion;
      this.cantidad = cantidad;
      this.precioUnitario = precioUnitario;
      this.descuento = descuento;
      this.precioTotalSinImpuesto = precioTotalSinImpuesto;
    }

    public String getCodigoPrincipal() {
      return codigoPrincipal;
    }

    public String getDescripcion() {
      return descripcion;
    }

    public String getCantidad() {
      return cantidad;
    }

    public String getPrecioUnitario() {
      return precioUnitario;
    }

    public String getDescuento() {
      return descuento;
    }

    public String getPrecioTotalSinImpuesto() {
      return precioTotalSinImpuesto;
    }
  }

  @Override
  public Path generateAndSaveCreditNotePdf(String authorizationXml, Path outputDirectory) {
    return generateAndSaveCreditNotePdf(authorizationXml, outputDirectory, null);
  }

  @Override
  public Path generateAndSaveCreditNotePdf(
      String authorizationXml, Path outputDirectory, String logoPath) {
    return generateAndSaveCreditNotePdf(authorizationXml, outputDirectory, logoPath, null, null);
  }

  @Override
  public Path generateAndSaveCreditNotePdf(
      String authorizationXml,
      Path outputDirectory,
      String logoPath,
      String regime,
      String specialTaxpayerResolution) {
    try {
      // Leer el XML de autorización
      Document document =
          DocumentBuilderFactory.newInstance()
              .newDocumentBuilder()
              .parse(new ByteArrayInputStream(authorizationXml.getBytes(StandardCharsets.UTF_8)));

      document.getDocumentElement().normalize();

      String estado = getNodeText(document, "estado");
      String numeroAutorizacion = getNodeText(document, "numeroAutorizacion");
      String fechaAutorizacionText = getNodeText(document, "fechaAutorizacion");
      String ambiente = getNodeText(document, "ambiente");
      String comprobanteXml = getNodeText(document, "comprobante");

      // Parsear como NotaCredito
      NotaCredito notaCredito =
          XmlUtil.unmarshal(
              new ByteArrayInputStream(comprobanteXml.getBytes(StandardCharsets.UTF_8)),
              NotaCredito.class);

      LocalDateTime fechaAutorizacion = parseAuthorizationDate(fechaAutorizacionText);

      // Construir parámetros específicos para nota de crédito
      Map<String, Object> parameters =
          buildCreditNoteParameters(
              notaCredito,
              estado,
              numeroAutorizacion,
              fechaAutorizacion,
              ambiente,
              logoPath,
              regime,
              specialTaxpayerResolution);

      // Construir datasource de detalles
      JRBeanCollectionDataSource detailsDataSource = buildCreditNoteDetailsDataSource(notaCredito);

      // Compilar y usar credit_note_template.jrxml
      try (InputStream jrxml =
          new ClassPathResource("jasper/credit_note_template.jrxml").getInputStream()) {
        JasperReport jasperReport = JasperCompileManager.compileReport(jrxml);
        JasperPrint jasperPrint =
            JasperFillManager.fillReport(jasperReport, parameters, detailsDataSource);

        // Exportar PDF
        byte[] pdfBytes = exportPdf(jasperPrint);

        // Guardar archivo
        Path targetDirectory = outputDirectory;
        Files.createDirectories(targetDirectory);

        String fileName =
            String.format(
                "%s-%s-%s.pdf",
                notaCredito.getInfoTributaria().getEstab(),
                notaCredito.getInfoTributaria().getPtoEmi(),
                notaCredito.getInfoTributaria().getSecuencial());

        Path outputPath = targetDirectory.resolve(fileName);
        Files.write(outputPath, pdfBytes);
        return outputPath;
      }
    } catch (Exception ex) {
      throw new IllegalStateException("Error al generar PDF de nota de crédito", ex);
    }
  }

  private String getNodeText(Document document, String tagName) {
    Node node = document.getElementsByTagName(tagName).item(0);
    return node != null ? node.getTextContent().trim() : "";
  }

  private LocalDateTime parseAuthorizationDate(String rawDate) {
    if (!StringUtils.hasText(rawDate)) {
      throw new IllegalStateException("La fecha de autorización está vacía");
    }

    List<DateTimeFormatter> formatters =
        List.of(AUTHORIZATION_FORMAT, AUTHORIZATION_ISO_FORMAT, AUTHORIZATION_ISO_OFFSET_FORMAT);

    for (DateTimeFormatter formatter : formatters) {
      try {
        if (formatter == AUTHORIZATION_ISO_OFFSET_FORMAT) {
          return OffsetDateTime.parse(rawDate, formatter).toLocalDateTime();
        }
        return LocalDateTime.parse(rawDate, formatter);
      } catch (Exception ignored) {
        // Intentar siguiente formato
      }
    }

    throw new IllegalStateException("Formato de fecha de autorización no soportado: " + rawDate);
  }

  private Map<String, Object> buildCreditNoteParameters(
      NotaCredito notaCredito,
      String estado,
      String numeroAutorizacion,
      LocalDateTime fechaAutorizacion,
      String ambiente,
      String logoPath,
      String regime,
      String specialTaxpayerResolution) {

    InfoNotaCredito info = notaCredito.getInfoNotaCredito();
    InfoTributaria infoTrib = notaCredito.getInfoTributaria();

    Map<String, Object> params = new HashMap<>();

    // Parámetros comunes
    params.put("razonSocial", infoTrib.getRazonSocial());
    params.put("nombreComercial", infoTrib.getNombreComercial());
    params.put("ruc", infoTrib.getRuc());
    params.put("numeroAutorizacion", numeroAutorizacion);
    params.put("fechaAutorizacion", DISPLAY_DATE.format(fechaAutorizacion));
    params.put("estadoAutorizacion", estado);
    params.put(
        "ambienteAutorizacion",
        AMBIENTE_LABELS.getOrDefault(infoTrib.getAmbiente().name(), infoTrib.getAmbiente().name()));
    params.put("direccionMatriz", infoTrib.getDirMatriz());
    params.put("direccionEstablecimiento", info.getDirEstablecimiento());
    params.put("fechaEmision", info.getFechaEmision());
    params.put(
        "tipoEmision",
        TIPO_EMISION_LABELS.getOrDefault(
            infoTrib.getTipoEmision().name(), infoTrib.getTipoEmision().name()));
    params.put("razonSocialComprador", info.getRazonSocialComprador());
    params.put("identificacionComprador", info.getIdentificacionComprador());
    params.put("direccionComprador", "");
    params.put(
        "obligadoContabilidad",
        info.getObligadoContabilidad() != null ? info.getObligadoContabilidad().name() : "");

    // Mapear régimen tributario usando el parámetro de BD (reutilizar método de InvoicePdfService)
    String regimeLabel = mapRegimeToLabel(regime);
    params.put("contribuyente", regimeLabel != null ? regimeLabel : "");

    // Contribuyente Especial Resolución
    params.put(
        "contribuyenteEspecialResolucion",
        StringUtils.hasText(specialTaxpayerResolution) ? specialTaxpayerResolution : "");

    // Totales
    params.put("totalSinImpuestos", formatCurrency(parseAmount(info.getTotalSinImpuestos())));
    params.put(
        "totalDescuento", formatCurrency(BigDecimal.ZERO)); // Nota de crédito no tiene descuento
    params.put("importeTotal", formatCurrency(parseAmount(info.getValorModificacion())));
    params.put("moneda", info.getMoneda() != null ? info.getMoneda().name() : "");
    params.put("propina", formatCurrency(BigDecimal.ZERO)); // Nota de crédito no tiene propina

    String numeroNotaCredito =
        infoTrib.getEstab() + "-" + infoTrib.getPtoEmi() + "-" + infoTrib.getSecuencial();
    params.put("numeroNotaCredito", numeroNotaCredito);

    String claveAcceso = infoTrib.getClaveAcceso();
    params.put("claveAcceso", claveAcceso);
    params.put("barcodeImage", generateBarcodeImage(claveAcceso));

    params.put("logoUrl", loadLogoUrl(logoPath));

    // Para notas de crédito, no hay pagos ni propina
    params.put("paymentsDataSource", new JRBeanCollectionDataSource(Collections.emptyList()));
    params.put("taxesDataSource", new JRBeanCollectionDataSource(Collections.emptyList()));

    // Construir el datasource de totales resumidos
    Map<TaxKey, TaxAccumulator> groupedTotals = buildTaxAccumulatorMap(notaCredito);
    params.put("summaryTotalsDataSource", buildSummaryTotalsDataSource(notaCredito, groupedTotals));

    String infoAdicional =
        Optional.ofNullable(notaCredito.getInfoAdicional()).orElse(Collections.emptyList()).stream()
            .map(ca -> ca.getNombre() + ": " + ca.getValue())
            .collect(java.util.stream.Collectors.joining("\n"));
    params.put("infoAdicional", infoAdicional);

    // NUEVOS PARÁMETROS ESPECÍFICOS DE NOTA DE CRÉDITO
    // Comprobante que se modifica: "FACTURA 001-002-031837657"
    String codDoc = info.getCodDocModificado();
    String tipoDoc = mapCodDocToTipo(codDoc); // "01" -> "FACTURA"
    String numDoc = info.getNumDocModificado(); // "001-002-031837657"
    params.put("comprobanteModificado", tipoDoc + " " + numDoc);

    // Fecha de emisión del comprobante modificado: "05/08/2024"
    params.put(
        "fechaEmisionComprobanteModificado",
        StringUtils.hasText(info.getFechaEmisionDocSustento())
            ? info.getFechaEmisionDocSustento()
            : "");

    // Razón de modificación: "REVERSO DETALLE : ASEGURADOID: 2471981988068"
    params.put("razonModificacion", StringUtils.hasText(info.getMotivo()) ? info.getMotivo() : "");

    return params;
  }

  private String mapCodDocToTipo(String codDoc) {
    if ("01".equals(codDoc)) {
      return "FACTURA";
    }
    if ("04".equals(codDoc)) {
      return "NOTA DE CRÉDITO";
    }
    if ("05".equals(codDoc)) {
      return "NOTA DE DÉBITO";
    }
    return "COMPROBANTE";
  }

  private JRBeanCollectionDataSource buildCreditNoteDetailsDataSource(NotaCredito notaCredito) {
    List<DetailRow> detalles =
        Optional.ofNullable(notaCredito.getDetalles()).orElse(Collections.emptyList()).stream()
            .map(this::toDetailRow)
            .toList();
    return new JRBeanCollectionDataSource(detalles);
  }

  private DetailRow toDetailRow(DetalleNotaCredito detalle) {
    return new DetailRow(
        detalle.getCodigoInterno() != null ? detalle.getCodigoInterno() : "",
        detalle.getDescripcion() != null ? detalle.getDescripcion() : "",
        formatQuantity(detalle.getCantidad()),
        formatCurrency(detalle.getPrecioUnitario()),
        formatCurrency(detalle.getDescuento()),
        formatCurrency(detalle.getPrecioTotalSinImpuesto()));
  }

  private String formatQuantity(String value) {
    if (!StringUtils.hasText(value)) {
      return "0";
    }
    try {
      BigDecimal quantity = new BigDecimal(value);
      BigDecimal normalized = quantity.stripTrailingZeros();
      return normalized.toPlainString();
    } catch (NumberFormatException ex) {
      return value;
    }
  }

  private BigDecimal parseAmount(String value) {
    if (!StringUtils.hasText(value)) {
      return BigDecimal.ZERO;
    }
    try {
      return new BigDecimal(value);
    } catch (NumberFormatException ex) {
      return BigDecimal.ZERO;
    }
  }

  private String formatCurrency(BigDecimal amount) {
    BigDecimal safeAmount = amount != null ? amount : BigDecimal.ZERO;
    return String.format(Locale.US, "%,.2f", safeAmount);
  }

  private String formatCurrency(String value) {
    return formatCurrency(parseAmount(value));
  }

  private BufferedImage generateBarcodeImage(String code) {
    if (!StringUtils.hasText(code)) {
      return null;
    }
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Code128Bean bean = new Code128Bean();
      bean.setModuleWidth(0.35);
      bean.doQuietZone(true);
      bean.setQuietZone(2.0);
      bean.setBarHeight(12.0);

      BitmapCanvasProvider canvas =
          new BitmapCanvasProvider(out, "image/png", 300, BufferedImage.TYPE_BYTE_BINARY, false, 0);

      bean.generateBarcode(canvas, code);
      canvas.finish();

      try (ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray())) {
        return ImageIO.read(in);
      }
    } catch (Exception e) {
      throw new IllegalStateException("No se pudo generar el código de barras", e);
    }
  }

  private URL loadLogoUrl(String logoPath) {
    try {
      if (StringUtils.hasText(logoPath)) {
        try {
          return new URL(logoPath);
        } catch (Exception e) {
          try {
            Path logoFilePath = Path.of(logoPath);
            if (Files.exists(logoFilePath)) {
              return logoFilePath.toUri().toURL();
            }
          } catch (Exception ex) {
            log.warn("No se pudo cargar el logo desde la ruta: {}", logoPath, ex);
          }
        }
      }
      return new ClassPathResource("jasper/no_tiene_logo.png").getURL();
    } catch (Exception e) {
      log.error("Error crítico: No se pudo cargar el logo por defecto", e);
      try {
        return getClass().getClassLoader().getResource("jasper/no_tiene_logo.png").toURI().toURL();
      } catch (Exception ex) {
        log.error("Error fatal: No se puede cargar ningún logo", ex);
        throw new RuntimeException("No se pudo cargar el logo por defecto", ex);
      }
    }
  }

  private byte[] exportPdf(JasperPrint jasperPrint) {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      JasperExportManager.exportReportToPdfStream(jasperPrint, baos);
      return baos.toByteArray();
    } catch (Exception ex) {
      throw new IllegalStateException("No se pudo exportar el reporte a PDF", ex);
    }
  }

  private String mapRegimeToLabel(String regime) {
    if (!StringUtils.hasText(regime)) {
      return null;
    }

    String mappingParam = paramsService.getParamValueOrDefault("COMPANY_REGIME_MAPPING", "");
    if (!StringUtils.hasText(mappingParam)) {
      return regime;
    }

    String[] mappings = mappingParam.split(",");
    for (String mapping : mappings) {
      String[] parts = mapping.split("\\|");
      if (parts.length == 2) {
        String key = parts[0].trim();
        String value = parts[1].trim();
        if (key.equalsIgnoreCase(regime)) {
          return value;
        }
      }
    }

    return regime;
  }

  private Map<TaxKey, TaxAccumulator> buildTaxAccumulatorMap(NotaCredito notaCredito) {
    Map<TaxKey, TaxAccumulator> groupedTotals = new LinkedHashMap<>();

    Optional.ofNullable(notaCredito.getInfoNotaCredito())
        .map(InfoNotaCredito::getTotalConImpuestos)
        .orElse(Collections.emptyList())
        .forEach(
            total -> {
              TaxKey key =
                  new TaxKey(
                      total.getCodigo(), total.getCodigoPorcentaje(), resolveRateLabel(total));
              groupedTotals
                  .computeIfAbsent(key, k -> new TaxAccumulator())
                  .accumulate(parseAmount(total.getBaseImponible()), parseAmount(total.getValor()));
            });

    return groupedTotals;
  }

  private String resolveRateLabel(TotalImpuesto total) {
    if (total == null) {
      return "";
    }

    String tarifa = total.getTarifa();
    if (StringUtils.hasText(tarifa)) {
      String trimmed = tarifa.trim();
      return trimmed.endsWith("%") ? trimmed : trimmed + "%";
    }

    String codigoPorcentaje = total.getCodigoPorcentaje();
    String mapped = IVA_RATE_LABELS.get(codigoPorcentaje);
    if (Objects.nonNull(mapped)) {
      return mapped;
    }

    if (!StringUtils.hasText(codigoPorcentaje)) {
      return "0%";
    }

    if (codigoPorcentaje.matches("\\d+(\\.\\d+)?")) {
      return codigoPorcentaje + "%";
    }

    return codigoPorcentaje;
  }

  private JRBeanCollectionDataSource buildSummaryTotalsDataSource(
      NotaCredito notaCredito, Map<TaxKey, TaxAccumulator> groupedTotals) {

    InfoNotaCredito info = notaCredito.getInfoNotaCredito();
    BigDecimal totalSinImpuestos = parseAmount(info.getTotalSinImpuestos());

    // Construir un Map con todos los SummaryRow disponibles
    Map<String, SummaryRow> rowsMap = new HashMap<>();

    // Subtotal sin impuestos
    rowsMap.put(
        "SUBTOTAL SIN IMPUESTOS",
        new SummaryRow("Subtotal Sin Impuestos", formatCurrency(totalSinImpuestos)));

    // Agregar dinámicamente todos los subtotales de IVA que existan en la nota de crédito
    Set<String> ivaTarifasEncontradas = extractIvaTarifasFromDetalles(notaCredito);
    for (String tarifa : ivaTarifasEncontradas) {
      BigDecimal baseIva = calculateIvaBaseFromTotalImpuestos(groupedTotals, tarifa);
      String label = "Subtotal " + tarifa + "%";
      String key = "SUBTOTAL " + tarifa + "%";
      rowsMap.put(key, new SummaryRow(label, formatCurrency(baseIva)));
    }

    // Agregar dinámicamente todos los IVA que existan en la nota de crédito
    for (String tarifa : ivaTarifasEncontradas) {
      BigDecimal ivaValor = getIvaTaxByTarifa(groupedTotals, tarifa);
      String label = "IVA " + tarifa + "%";
      String key = "IVA " + tarifa + "%";
      rowsMap.put(key, new SummaryRow(label, formatCurrency(ivaValor)));
    }

    // Agregar descuento, ICE y valor total
    // Nota: Las notas de crédito no tienen descuento ni propina en el XML
    rowsMap.put("DESCUENTO", new SummaryRow("Descuento", formatCurrency(BigDecimal.ZERO)));
    rowsMap.put(
        "ICE", new SummaryRow("ICE", formatCurrency(getAccumulatorTaxByCode(groupedTotals, "3"))));
    rowsMap.put("PROPINA", new SummaryRow("Propina", formatCurrency(BigDecimal.ZERO)));
    rowsMap.put("PROPINA 10%", new SummaryRow("Propina 10%", formatCurrency(BigDecimal.ZERO)));
    rowsMap.put(
        "VALOR A PAGAR",
        new SummaryRow("Valor a pagar", formatCurrency(parseAmount(info.getValorModificacion()))));

    // Leer el orden desde el parámetro de BD
    String labelsOrderParam = paramsService.getParamValueOrDefault(SUMMARY_TOTALS_LABELS_PARAM, "");
    List<String> labelsOrderOriginal =
        Arrays.stream(labelsOrderParam.split("\\|"))
            .map(String::trim)
            .filter(StringUtils::hasText)
            .toList();

    List<String> labelsOrderUpper = labelsOrderOriginal.stream().map(String::toUpperCase).toList();

    // Construir la lista final ordenada según el parámetro
    List<SummaryRow> orderedRows = new ArrayList<>();
    for (int i = 0; i < labelsOrderOriginal.size(); i++) {
      String originalLabel = labelsOrderOriginal.get(i);
      String upperLabel = labelsOrderUpper.get(i);
      SummaryRow row = findMatchingRow(rowsMap, upperLabel);
      if (row != null) {
        // Usar la etiqueta original del parámetro, pero mantener el valor del row encontrado
        orderedRows.add(new SummaryRow(originalLabel, row.getAmount()));
      } else {
        // Si no se encuentra, crear un row con valor cero usando la etiqueta original
        orderedRows.add(new SummaryRow(originalLabel, formatCurrency(BigDecimal.ZERO)));
      }
    }

    return new JRBeanCollectionDataSource(orderedRows);
  }

  /** Busca un SummaryRow que coincida con la etiqueta, manejando variaciones en el formato. */
  private SummaryRow findMatchingRow(Map<String, SummaryRow> rowsMap, String labelKey) {
    // Buscar coincidencia exacta
    SummaryRow exactMatch = rowsMap.get(labelKey);
    if (exactMatch != null) {
      return exactMatch;
    }

    // Buscar coincidencia parcial (normalizada)
    String normalizedKey = labelKey.replaceAll("\\s+", " ").trim();
    for (Map.Entry<String, SummaryRow> entry : rowsMap.entrySet()) {
      String normalizedEntry = entry.getKey().replaceAll("\\s+", " ").trim();
      if (normalizedKey.equalsIgnoreCase(normalizedEntry)) {
        return entry.getValue();
      }
    }

    // Buscar por palabras clave
    String upperKey = labelKey.toUpperCase();
    if (upperKey.contains("PROPINA")) {
      return rowsMap.get("PROPINA") != null ? rowsMap.get("PROPINA") : rowsMap.get("PROPINA 10%");
    }
    if (upperKey.contains("VALOR") && upperKey.contains("PAGAR")) {
      return rowsMap.get("VALOR A PAGAR");
    }
    if (upperKey.contains("SIN IMPUESTOS")) {
      return rowsMap.get("SUBTOTAL SIN IMPUESTOS");
    }

    return null;
  }

  /**
   * Extrae todas las tarifas de IVA únicas que existen en los detalles de la nota de crédito,
   * ordenadas de mayor a menor.
   */
  private Set<String> extractIvaTarifasFromDetalles(NotaCredito notaCredito) {
    return Optional.ofNullable(notaCredito.getDetalles()).orElse(Collections.emptyList()).stream()
        .flatMap(
            detalle ->
                Optional.ofNullable(detalle.getImpuestos())
                    .orElse(Collections.emptyList())
                    .stream())
        .filter(impuesto -> "2".equals(impuesto.getCodigo())) // Solo IVA
        .map(
            impuesto -> {
              try {
                // Normalizar la tarifa a un número sin decimales innecesarios
                BigDecimal tarifa = new BigDecimal(impuesto.getTarifa());
                return tarifa.stripTrailingZeros().toPlainString();
              } catch (NumberFormatException e) {
                return impuesto.getTarifa();
              }
            })
        .collect(
            Collectors.toCollection(
                () ->
                    new TreeSet<>(
                        (a, b) -> new BigDecimal(b).compareTo(new BigDecimal(a))))); // De mayor a
    // menor
  }

  /** Obtiene el valor del IVA para una tarifa específica, comparando numéricamente. */
  private BigDecimal getIvaTaxByTarifa(Map<TaxKey, TaxAccumulator> groupedTotals, String tarifa) {
    return groupedTotals.entrySet().stream()
        .filter(entry -> "2".equals(entry.getKey().codigo()))
        .filter(
            entry -> {
              try {
                String rateLabel = entry.getKey().rateLabel();
                if (!StringUtils.hasText(rateLabel)) {
                  return false;
                }
                // Extraer el número de "15%" -> "15"
                String numericPart = rateLabel.replaceAll("[^0-9.]", "");
                BigDecimal rateLabelNumeric = new BigDecimal(numericPart);
                BigDecimal tarifaNumeric = new BigDecimal(tarifa);
                return rateLabelNumeric.compareTo(tarifaNumeric) == 0;
              } catch (NumberFormatException e) {
                return false;
              }
            })
        .map(entry -> entry.getValue().taxSum())
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /** Calcula la base de IVA desde totalConImpuestos para una tarifa específica. */
  private BigDecimal calculateIvaBaseFromTotalImpuestos(
      Map<TaxKey, TaxAccumulator> groupedTotals, String ivaTarifa) {
    return groupedTotals.entrySet().stream()
        .filter(entry -> "2".equals(entry.getKey().codigo()))
        .filter(
            entry -> {
              try {
                String rateLabel = entry.getKey().rateLabel();
                if (!StringUtils.hasText(rateLabel)) {
                  return false;
                }
                // Extraer el número de "15%" -> "15"
                String numericPart = rateLabel.replaceAll("[^0-9.]", "");
                BigDecimal rateLabelNumeric = new BigDecimal(numericPart);
                BigDecimal tarifaNumeric = new BigDecimal(ivaTarifa);
                return rateLabelNumeric.compareTo(tarifaNumeric) == 0;
              } catch (NumberFormatException e) {
                return false;
              }
            })
        .map(entry -> entry.getValue().baseSum())
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private BigDecimal getAccumulatorTaxByCode(
      Map<TaxKey, TaxAccumulator> groupedTotals, String taxCode) {
    return groupedTotals.entrySet().stream()
        .filter(entry -> taxCode.equals(entry.getKey().codigo()))
        .map(entry -> entry.getValue().taxSum())
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
