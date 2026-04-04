package com.wallparisoft.ebillbilling.service.impl;

import com.wallparisoft.ebillbilling.dto.InvoicePdfData;
import com.wallparisoft.ebillbilling.service.InvoicePdfService;
import com.wallparisoft.ebillbilling.service.ParamsService;
import com.wallparisoft.integration.sri.enums.FormaPagoEnum;
import com.wallparisoft.integration.sri.model.Detalle;
import com.wallparisoft.integration.sri.model.Factura;
import com.wallparisoft.integration.sri.model.InfoFactura;
import com.wallparisoft.integration.sri.model.Pago;
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
 * Servicio encargado de construir el RIDE (PDF) a partir del XML de autorización del SRI. Incluye
 * la recopilación de parámetros, detalles, formas de pago y totales, así como la generación de la
 * estructura de carpetas donde se almacena el archivo final.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InvoicePdfServiceImpl implements InvoicePdfService {

  private static final String SUMMARY_TOTALS_LABELS_PARAM = "SUMMARY_TOTALS_LABELS";
  private static final String COMPANY_REGIME_MAPPING_PARAM = "COMPANY_REGIME_MAPPING";

  private final ParamsService paramsService;

  private static final DateTimeFormatter AUTHORIZATION_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private static final DateTimeFormatter AUTHORIZATION_ISO_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
  private static final DateTimeFormatter AUTHORIZATION_ISO_OFFSET_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
  private static final DateTimeFormatter DISPLAY_DATE =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
  private static final DateTimeFormatter DIRECTORY_DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

  private static final Map<String, String> AMBIENTE_LABELS =
      Map.of("1", "PRUEBAS", "2", "PRODUCCIÓN");

  private static final Map<String, String> TIPO_EMISION_LABELS =
      Map.of("1", "EMISIÓN NORMAL", "2", "EMISIÓN POR INDISPONIBILIDAD DEL SISTEMA");

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

  private static final Map<FormaPagoEnum, String> FORMA_PAGO_LABELS =
      Map.ofEntries(
          Map.entry(
              FormaPagoEnum.SIN_UTILIZACION_DEL_SISTEMA_FINANCIERO,
              "Sin utilización del sistema financiero"),
          Map.entry(FormaPagoEnum.COMPENSACION_DE_DEUDAS, "Compensación de deudas"),
          Map.entry(FormaPagoEnum.TARJETA_DE_DEBITO, "Tarjeta de débito"),
          Map.entry(FormaPagoEnum.DINERO_ELECTRONICO, "Dinero electrónico"),
          Map.entry(FormaPagoEnum.TARJETA_PREPAGO, "Tarjeta prepago"),
          Map.entry(FormaPagoEnum.TARJETA_DE_CREDITO, "Tarjeta de crédito"),
          Map.entry(
              FormaPagoEnum.OTROS_CON_UTILIZACION_DEL_SISTEMA_FINANCIERO,
              "Otros con utilización del sistema financiero"),
          Map.entry(FormaPagoEnum.ENDOSO_DE_TITULOS, "Endoso de títulos"));

  public static class PaymentRow {
    private final String formaPago;
    private final String total;
    private final String plazo;
    private final String unidadTiempo;

    PaymentRow(String formaPago, String total, String plazo, String unidadTiempo) {
      this.formaPago = formaPago;
      this.total = total;
      this.plazo = plazo;
      this.unidadTiempo = unidadTiempo;
    }

    public String getFormaPago() {
      return formaPago;
    }

    public String getTotal() {
      return total;
    }

    public String getPlazo() {
      return plazo;
    }

    public String getUnidadTiempo() {
      return unidadTiempo;
    }
  }

  public static class TaxRow {
    private final String codigo;
    private final String baseImponible;
    private final String valor;

    TaxRow(String codigo, String baseImponible, String valor) {
      this.codigo = codigo;
      this.baseImponible = baseImponible;
      this.valor = valor;
    }

    public String getCodigo() {
      return codigo;
    }

    public String getBaseImponible() {
      return baseImponible;
    }

    public String getValor() {
      return valor;
    }
  }

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

  private static final Map<String, String> TAX_CODE_LABELS =
      Map.of("2", "IVA", "3", "ICE", "5", "IRBPNR");

  private static final Map<String, String> IVA_RATE_LABELS =
      Map.of("0", "0%", "2", "12%", "3", "14%", "4", "15%", "5", "5%");

  private static final List<TaxKey> DEFAULT_IVA_KEYS =
      List.of(
          new TaxKey("2", "0", IVA_RATE_LABELS.get("0")),
          new TaxKey("2", "4", IVA_RATE_LABELS.get("4")));

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

  public InvoicePdfData readInvoiceData(String authorizationXml) {
    return readInvoiceData(authorizationXml, null, null, null);
  }

  public InvoicePdfData readInvoiceData(String authorizationXml, String logoPath) {
    return readInvoiceData(authorizationXml, logoPath, null, null);
  }

  public InvoicePdfData readInvoiceData(
      String authorizationXml, String logoPath, String regime, String specialTaxpayerResolution) {
    try {
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

      Factura factura =
          XmlUtil.unmarshal(
              new ByteArrayInputStream(comprobanteXml.getBytes(StandardCharsets.UTF_8)),
              Factura.class);

      LocalDateTime fechaAutorizacion = parseAuthorizationDate(fechaAutorizacionText);

      return new InvoicePdfData(
          factura,
          estado,
          numeroAutorizacion,
          fechaAutorizacion,
          ambiente,
          logoPath,
          regime,
          specialTaxpayerResolution);

    } catch (Exception ex) {
      throw new IllegalStateException("No se pudo leer el XML de autorización/factura", ex);
    }
  }

  private String getNodeText(Document document, String tagName) {
    Node node = document.getElementsByTagName(tagName).item(0);
    return node != null ? node.getTextContent().trim() : "";
  }

  public Map<String, Object> buildInvoiceParameters(InvoicePdfData data) {
    Factura factura = data.factura();

    Map<String, Object> params = new HashMap<>();
    params.put("razonSocial", factura.getInfoTributaria().getRazonSocial());
    params.put("nombreComercial", factura.getInfoTributaria().getNombreComercial());
    params.put("ruc", factura.getInfoTributaria().getRuc());
    params.put("numeroAutorizacion", data.numeroAutorizacion());
    params.put("fechaAutorizacion", DISPLAY_DATE.format(data.fechaAutorizacion()));
    params.put("estadoAutorizacion", data.estadoAutorizacion());
    params.put("ambienteAutorizacion", Optional.ofNullable(data.ambienteAutorizacion()).orElse(""));
    params.put("direccionMatriz", factura.getInfoTributaria().getDirMatriz());
    params.put("direccionEstablecimiento", factura.getInfoFactura().getDirEstablecimiento());
    params.put("fechaEmision", factura.getInfoFactura().getFechaEmision());
    params.put(
        "ambienteAutorizacion",
        AMBIENTE_LABELS.getOrDefault(
            factura.getInfoTributaria().getAmbiente().name(),
            factura.getInfoTributaria().getAmbiente().name()));
    params.put(
        "tipoEmision",
        TIPO_EMISION_LABELS.getOrDefault(
            factura.getInfoTributaria().getTipoEmision().name(),
            factura.getInfoTributaria().getTipoEmision().name()));
    params.put("razonSocialComprador", factura.getInfoFactura().getRazonSocialComprador());
    params.put("identificacionComprador", factura.getInfoFactura().getIdentificacionComprador());
    params.put(
        "direccionComprador",
        Objects.isNull(factura.getInfoFactura().getDireccionComprador())
                || factura.getInfoFactura().getDireccionComprador().isEmpty()
            ? ""
            : factura.getInfoFactura().getDireccionComprador());
    params.put(
        "obligadoContabilidad",
        Optional.ofNullable(factura.getInfoFactura().getObligadoContabilidad())
            .map(Enum::name)
            .orElse(""));

    // Mapear régimen tributario usando el parámetro de BD
    String regimeLabel = mapRegimeToLabel(data.regime());
    params.put("contribuyente", regimeLabel != null ? regimeLabel : "");

    // Contribuyente Especial Resolución (solo si tiene valor)
    String specialTaxpayerResolution = data.specialTaxpayerResolution();
    params.put(
        "contribuyenteEspecialResolucion",
        StringUtils.hasText(specialTaxpayerResolution) ? specialTaxpayerResolution : "");

    params.put(
        "totalSinImpuestos", formatCurrency(factura.getInfoFactura().getTotalSinImpuestos()));
    params.put("totalDescuento", formatCurrency(factura.getInfoFactura().getTotalDescuento()));
    params.put("importeTotal", formatCurrency(factura.getInfoFactura().getImporteTotal()));
    params.put(
        "moneda",
        Optional.ofNullable(factura.getInfoFactura().getMoneda()).map(Enum::name).orElse(""));
    params.put("propina", formatCurrency(factura.getInfoFactura().getPropina()));
    params.put("claveAcceso", data.getClaveAcceso());
    params.put("barcodeImage", generateBarcodeImage(data.getClaveAcceso()));

    String numeroFactura =
        factura.getInfoTributaria().getEstab()
            + "-"
            + factura.getInfoTributaria().getPtoEmi()
            + "-"
            + factura.getInfoTributaria().getSecuencial();
    params.put("numeroFactura", numeroFactura);

    params.put("logoUrl", loadLogoUrl(data.logoPath()));
    params.put("paymentsDataSource", buildPaymentDataSource(factura));

    Map<TaxKey, TaxAccumulator> groupedTotals = buildTaxAccumulatorMap(factura);
    params.put("taxesDataSource", toTaxesDataSource(groupedTotals));
    params.put("summaryTotalsDataSource", buildSummaryTotalsDataSource(factura, groupedTotals));

    String infoAdicional =
        Optional.ofNullable(factura.getInfoAdicional()).orElse(Collections.emptyList()).stream()
            .map(ca -> ca.getNombre() + ": " + ca.getValue())
            .collect(Collectors.joining("\n"));
    params.put("infoAdicional", infoAdicional);

    return params;
  }

  private URL loadLogoUrl(String logoPath) {
    try {
      // Si tiene logoPath, intentar cargarlo como URL o archivo
      if (StringUtils.hasText(logoPath)) {
        try {
          // Intentar como URL primero (puede ser http://, https://, o file://)
          return new URL(logoPath);
        } catch (Exception e) {
          // Si no es URL válida, intentar como ruta de archivo local
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
      // Si no tiene logo o falla, usar logo por defecto
      return new ClassPathResource("jasper/no_tiene_logo.png").getURL();
    } catch (Exception e) {
      // Si incluso el logo por defecto falla, intentar crear una URL vacía o lanzar excepción
      log.error("Error crítico: No se pudo cargar el logo por defecto", e);
      try {
        // Último recurso: intentar cargar desde classpath de otra manera
        return getClass().getClassLoader().getResource("jasper/no_tiene_logo.png").toURI().toURL();
      } catch (Exception ex) {
        log.error("Error fatal: No se puede cargar ningún logo", ex);
        throw new RuntimeException("No se pudo cargar el logo por defecto", ex);
      }
    }
  }

  public JRBeanCollectionDataSource buildInvoiceDetailsDataSource(Factura factura) {
    List<DetailRow> detalles =
        Optional.ofNullable(factura.getDetalles()).orElse(Collections.emptyList()).stream()
            .map(this::toDetailRow)
            .toList();
    return new JRBeanCollectionDataSource(detalles);
  }

  public JRBeanCollectionDataSource buildPaymentDataSource(Factura factura) {
    List<PaymentRow> rows =
        Optional.ofNullable(factura.getInfoFactura())
            .map(InfoFactura::getPagos)
            .orElse(Collections.emptyList())
            .stream()
            .map(this::toPaymentRow)
            .toList();
    return new JRBeanCollectionDataSource(rows);
  }

  private PaymentRow toPaymentRow(Pago pago) {
    String totalFormat = formatCurrency(pago.getTotal());
    String plazo = Optional.ofNullable(pago.getPlazo()).map(Object::toString).orElse("0");
    String unidad =
        Optional.ofNullable(pago.getUnidadTiempo()).filter(StringUtils::hasText).orElse("días");
    String formaPago =
        Optional.ofNullable(pago.getFormaPago())
            .map(
                formaPagoEnum ->
                    FORMA_PAGO_LABELS.getOrDefault(
                        formaPagoEnum, formatEnumName(formaPagoEnum.name())))
            .orElse("");
    return new PaymentRow(formaPago, totalFormat, plazo, unidad);
  }

  public JRBeanCollectionDataSource buildTaxesDataSource(Factura factura) {
    Map<TaxKey, TaxAccumulator> groupedTotals = buildTaxAccumulatorMap(factura);
    return toTaxesDataSource(groupedTotals);
  }

  private void ensureDefaultIvaEntries(Map<TaxKey, TaxAccumulator> groupedTotals) {
    for (TaxKey key : DEFAULT_IVA_KEYS) {
      groupedTotals.computeIfAbsent(key, k -> new TaxAccumulator());
    }
  }

  private void ensureDefaultIceEntry(Map<TaxKey, TaxAccumulator> groupedTotals) {
    boolean hasIce = groupedTotals.keySet().stream().anyMatch(key -> "3".equals(key.codigo()));
    if (!hasIce) {
      groupedTotals.put(new TaxKey("3", "", "ICE"), new TaxAccumulator());
    }
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
      bean.setModuleWidth(0.35); // barras más anchas
      bean.doQuietZone(true); // deja margen a los lados
      bean.setQuietZone(2.0); // opcional: margen equivalente a ~2 módulos
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

  private DetailRow toDetailRow(Detalle detalle) {
    return new DetailRow(
        detalle.getCodigoPrincipal(),
        detalle.getDescripcion(),
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
      return normalized.scale() <= 0 ? normalized.toPlainString() : normalized.toPlainString();
    } catch (NumberFormatException ex) {
      return value;
    }
  }

  private String formatEnumName(String name) {
    if (!StringUtils.hasText(name)) {
      return "";
    }
    return Arrays.stream(name.toLowerCase(Locale.ROOT).split("_"))
        .filter(part -> !part.isBlank())
        .map(part -> part.substring(0, 1).toUpperCase(Locale.ROOT) + part.substring(1))
        .collect(Collectors.joining(" "));
  }

  public JasperPrint buildJasperPrint(InvoicePdfData data) {
    try (InputStream jrxml =
        new ClassPathResource("jasper/invoice_template.jrxml").getInputStream()) {
      JasperReport jasperReport = JasperCompileManager.compileReport(jrxml);
      Map<String, Object> parameters = buildInvoiceParameters(data);
      JRBeanCollectionDataSource invoiceDetailsDataSource =
          buildInvoiceDetailsDataSource(data.factura());
      return JasperFillManager.fillReport(jasperReport, parameters, invoiceDetailsDataSource);
    } catch (Exception ex) {
      throw new IllegalStateException("Error al compilar/llenar el reporte Jasper", ex);
    }
  }

  public byte[] exportPdf(JasperPrint jasperPrint) {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      JasperExportManager.exportReportToPdfStream(jasperPrint, baos);
      return baos.toByteArray();
    } catch (Exception ex) {
      throw new IllegalStateException("No se pudo exportar el reporte a PDF", ex);
    }
  }

  public Path generateAndSaveInvoicePdf(String authorizationXml, Path outputDirectory) {
    return generateAndSaveInvoicePdf(authorizationXml, outputDirectory, null);
  }

  public Path generateAndSaveInvoicePdf(
      String authorizationXml, Path outputDirectory, String logoPath) {
    return generateAndSaveInvoicePdf(authorizationXml, outputDirectory, logoPath, null, null);
  }

  public Path generateAndSaveInvoicePdf(
      String authorizationXml,
      Path outputDirectory,
      String logoPath,
      String regime,
      String specialTaxpayerResolution) {
    InvoicePdfData data =
        readInvoiceData(authorizationXml, logoPath, regime, specialTaxpayerResolution);
    JasperPrint jasperPrint = buildJasperPrint(data);
    byte[] pdfBytes = exportPdf(jasperPrint);

    try {
      Path targetDirectory = outputDirectory;
      Files.createDirectories(targetDirectory);

      String fileName =
          "%s-%s-%s.pdf"
              .formatted(
                  data.factura().getInfoTributaria().getEstab(),
                  data.factura().getInfoTributaria().getPtoEmi(),
                  data.factura().getInfoTributaria().getSecuencial());

      Path outputPath = targetDirectory.resolve(fileName);
      Files.write(outputPath, pdfBytes);
      return outputPath;
    } catch (Exception ex) {
      throw new IllegalStateException("No se pudo guardar el PDF generado en disco", ex);
    }
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

  private JRBeanCollectionDataSource buildSummaryTotalsDataSource(
      Factura factura, Map<TaxKey, TaxAccumulator> groupedTotals) {

    // IMPORTANTE: totalSinImpuestos en el XML es DESPUÉS de descuentos
    // Para mostrar "Subtotal SIN Impuestos" (ANTES de descuentos), sumamos los descuentos
    BigDecimal totalSinImpuestos = parseAmount(factura.getInfoFactura().getTotalSinImpuestos());
    BigDecimal totalDescuento = parseAmount(factura.getInfoFactura().getTotalDescuento());
    BigDecimal subtotalAntesDeDescuentos = totalSinImpuestos.add(totalDescuento);

    // Construir un Map con todos los SummaryRow disponibles
    Map<String, SummaryRow> rowsMap = new HashMap<>();

    // Subtotal sin impuestos
    rowsMap.put(
        "SUBTOTAL SIN IMPUESTOS",
        new SummaryRow("Subtotal Sin Impuestos", formatCurrency(subtotalAntesDeDescuentos)));

    // Agregar dinámicamente todos los subtotales de IVA (sin ICE) que existan en la factura
    Set<String> ivaTarifasEncontradas = extractIvaTarifasFromDetalles(factura);
    for (String tarifa : ivaTarifasEncontradas) {
      BigDecimal baseIvaSinIce = calculateIvaBaseWithoutIce(factura, tarifa);
      String label = "Subtotal " + tarifa + "%";
      String key = "SUBTOTAL " + tarifa + "%";
      rowsMap.put(key, new SummaryRow(label, formatCurrency(baseIvaSinIce)));
    }

    // Agregar dinámicamente todos los IVA que existan en la factura
    for (String tarifa : ivaTarifasEncontradas) {
      BigDecimal ivaValor = getIvaTaxByTarifa(groupedTotals, tarifa);
      String label = "IVA " + tarifa + "%";
      String key = "IVA " + tarifa + "%";
      rowsMap.put(key, new SummaryRow(label, formatCurrency(ivaValor)));
    }

    // Agregar descuento, ICE, propina y valor total
    rowsMap.put(
        "DESCUENTO",
        new SummaryRow("Descuento", formatCurrency(factura.getInfoFactura().getTotalDescuento())));
    rowsMap.put(
        "ICE", new SummaryRow("ICE", formatCurrency(getAccumulatorTaxByCode(groupedTotals, "3"))));
    rowsMap.put(
        "PROPINA",
        new SummaryRow("Propina", formatCurrency(factura.getInfoFactura().getPropina())));
    rowsMap.put(
        "PROPINA 10%",
        new SummaryRow("Propina 10%", formatCurrency(factura.getInfoFactura().getPropina())));
    rowsMap.put(
        "VALOR A PAGAR",
        new SummaryRow(
            "Valor a pagar", formatCurrency(factura.getInfoFactura().getImporteTotal())));

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
   * Extrae todas las tarifas de IVA únicas que existen en los detalles de la factura, ordenadas de
   * mayor a menor.
   */
  private Set<String> extractIvaTarifasFromDetalles(Factura factura) {
    return Optional.ofNullable(factura.getDetalles()).orElse(Collections.emptyList()).stream()
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
                    new TreeSet<>((a, b) -> new BigDecimal(b).compareTo(new BigDecimal(a))))); // De
    // mayor
    // a
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

  /**
   * Calcula la base de IVA SIN incluir el ICE, sumando directamente los precioTotalSinImpuesto de
   * los detalles con la tarifa especificada.
   */
  private BigDecimal calculateIvaBaseWithoutIce(Factura factura, String ivaTarifa) {
    return Optional.ofNullable(factura.getDetalles()).orElse(Collections.emptyList()).stream()
        .filter(
            detalle -> {
              // Buscar si este detalle tiene un impuesto IVA con la tarifa especificada
              return Optional.ofNullable(detalle.getImpuestos())
                  .orElse(Collections.emptyList())
                  .stream()
                  .anyMatch(
                      impuesto -> {
                        if (!"2".equals(impuesto.getCodigo())) {
                          return false;
                        }
                        // Comparación numérica para evitar problemas con "15" vs "15.0"
                        try {
                          BigDecimal tarifaImpuesto = new BigDecimal(impuesto.getTarifa());
                          BigDecimal tarifaBuscada = new BigDecimal(ivaTarifa);
                          return tarifaImpuesto.compareTo(tarifaBuscada) == 0;
                        } catch (NumberFormatException e) {
                          return false;
                        }
                      });
            })
        .map(detalle -> parseAmount(detalle.getPrecioTotalSinImpuesto()))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private BigDecimal getIvaBase(Map<TaxKey, TaxAccumulator> groupedTotals, String rateLabel) {
    return groupedTotals.entrySet().stream()
        .filter(entry -> "2".equals(entry.getKey().codigo()))
        .filter(entry -> Objects.equals(entry.getKey().rateLabel(), rateLabel))
        .map(entry -> entry.getValue().baseSum())
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private BigDecimal getIvaTax(Map<TaxKey, TaxAccumulator> groupedTotals, String rateLabel) {
    return groupedTotals.entrySet().stream()
        .filter(entry -> "2".equals(entry.getKey().codigo()))
        .filter(entry -> Objects.equals(entry.getKey().rateLabel(), rateLabel))
        .map(entry -> entry.getValue().taxSum())
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private BigDecimal getAccumulatorTaxByCode(
      Map<TaxKey, TaxAccumulator> groupedTotals, String taxCode) {
    return groupedTotals.entrySet().stream()
        .filter(entry -> taxCode.equals(entry.getKey().codigo()))
        .map(entry -> entry.getValue().taxSum())
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private Map<TaxKey, TaxAccumulator> buildTaxAccumulatorMap(Factura factura) {
    Map<TaxKey, TaxAccumulator> groupedTotals = new LinkedHashMap<>();

    Optional.ofNullable(factura.getInfoFactura())
        .map(InfoFactura::getTotalConImpuestos)
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

    ensureDefaultIvaEntries(groupedTotals);
    ensureDefaultIceEntry(groupedTotals);

    return groupedTotals;
  }

  private JRBeanCollectionDataSource toTaxesDataSource(Map<TaxKey, TaxAccumulator> groupedTotals) {
    List<TaxRow> rows = new ArrayList<>();
    groupedTotals.forEach(
        (key, accumulator) -> {
          String taxName = TAX_CODE_LABELS.getOrDefault(key.codigo(), "Impuesto " + key.codigo());
          if ("2".equals(key.codigo())) {
            String rate = key.rateLabel();
            rows.add(new TaxRow("Subtotal " + rate, formatCurrency(accumulator.baseSum()), ""));
            rows.add(new TaxRow(taxName + " " + rate, "", formatCurrency(accumulator.taxSum())));
          } else {
            rows.add(new TaxRow("Subtotal " + taxName, formatCurrency(accumulator.baseSum()), ""));
            rows.add(new TaxRow(taxName, "", formatCurrency(accumulator.taxSum())));
          }
        });
    return new JRBeanCollectionDataSource(rows);
  }

  /**
   * Mapea el código del régimen tributario a su etiqueta usando el parámetro de BD.
   *
   * @param regime Código del régimen (GENERAL, RIMPE_POPULAR, RIMPE_STARTUP, SPECIAL)
   * @return Etiqueta del régimen o null si no se encuentra
   */
  private String mapRegimeToLabel(String regime) {
    if (!StringUtils.hasText(regime)) {
      return null;
    }

    String mappingParam = paramsService.getParamValueOrDefault(COMPANY_REGIME_MAPPING_PARAM, "");
    if (!StringUtils.hasText(mappingParam)) {
      return regime; // Si no hay parámetro, devolver el código original
    }

    // Parsear el formato: clave|valor,clave|valor
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

    // Si no se encuentra, devolver el código original
    return regime;
  }
}
