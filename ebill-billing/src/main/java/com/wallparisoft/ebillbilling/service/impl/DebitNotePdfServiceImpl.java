package com.wallparisoft.ebillbilling.service.impl;

import com.wallparisoft.ebillbilling.dto.DebitNotePdfData;
import com.wallparisoft.ebillbilling.service.DebitNotePdfService;
import com.wallparisoft.ebillbilling.service.ParamsService;
import com.wallparisoft.integration.sri.model.ImpuestoNotaDebito;
import com.wallparisoft.integration.sri.model.InfoNotaDebito;
import com.wallparisoft.integration.sri.model.MotivoNotaDebito;
import com.wallparisoft.integration.sri.model.NotaDebito;
import com.wallparisoft.integration.sri.model.Pago;
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
 * Servicio encargado de construir el RIDE (PDF) a partir del XML de autorización del SRI para notas
 * de débito. Incluye la recopilación de parámetros, motivos, formas de pago y totales, así como la
 * generación de la estructura de carpetas donde se almacena el archivo final.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DebitNotePdfServiceImpl implements DebitNotePdfService {

  private final ParamsService paramsService;

  private static final DateTimeFormatter DISPLAY_DATE =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
  private static final DateTimeFormatter DIRECTORY_DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

  private static final Map<String, String> AMBIENTE_LABELS =
      Map.of("1", "PRUEBAS", "2", "PRODUCCIÓN");

  private static final Map<String, String> TIPO_EMISION_LABELS =
      Map.of("1", "EMISIÓN NORMAL", "2", "EMISIÓN POR INDISPONIBILIDAD DEL SISTEMA");

  private static final Map<String, String> FORMA_PAGO_LABELS =
      Map.of(
          "01", "SIN UTILIZACION DEL SISTEMA FINANCIERO",
          "15", "COMPENSACION DE DEUDAS",
          "16", "TARJETA DE DÉBITO",
          "17", "DINERO ELECTRÓNICO",
          "18", "TARJETA PREPAGO",
          "19", "TARJETA DE CRÉDITO",
          "20", "OTROS CON UTILIZACION DEL SISTEMA FINANCIERO",
          "21", "ENDOSO DE TÍTULOS");

  private static final String SUMMARY_TOTALS_LABELS_PARAM = "SUMMARY_TOTALS_LABELS";

  public static class MotivoRow {
    private final String razon;
    private final String valor;

    MotivoRow(String razon, String valor) {
      this.razon = razon;
      this.valor = valor;
    }

    public String getRazon() {
      return razon;
    }

    public String getValor() {
      return valor;
    }
  }

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

  private static class TaxKey {
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

  public DebitNotePdfData readDebitNoteData(String authorizationXml) {
    return readDebitNoteData(authorizationXml, null, null, null);
  }

  public DebitNotePdfData readDebitNoteData(String authorizationXml, String logoPath) {
    return readDebitNoteData(authorizationXml, logoPath, null, null);
  }

  public DebitNotePdfData readDebitNoteData(
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

      NotaDebito notaDebito =
          XmlUtil.unmarshal(
              new ByteArrayInputStream(comprobanteXml.getBytes(StandardCharsets.UTF_8)),
              NotaDebito.class);

      LocalDateTime fechaAutorizacion = parseAuthorizationDate(fechaAutorizacionText);

      return new DebitNotePdfData(
          notaDebito,
          estado,
          numeroAutorizacion,
          fechaAutorizacion,
          ambiente,
          logoPath,
          regime,
          specialTaxpayerResolution);

    } catch (Exception ex) {
      throw new IllegalStateException("No se pudo leer el XML de autorización/nota de débito", ex);
    }
  }

  private String getNodeText(Document document, String tagName) {
    Node node = document.getElementsByTagName(tagName).item(0);
    return node != null ? node.getTextContent().trim() : "";
  }

  private LocalDateTime parseAuthorizationDate(String rawDate) {
    if (!StringUtils.hasText(rawDate)) {
      return LocalDateTime.now();
    }

    DateTimeFormatter[] formatters = {
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"),
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
    };

    for (DateTimeFormatter formatter : formatters) {
      try {
        if (rawDate.contains("T") && rawDate.contains("+")
            || rawDate.contains("-") && rawDate.length() > 19) {
          return OffsetDateTime.parse(rawDate, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
              .toLocalDateTime();
        }
        return LocalDateTime.parse(rawDate, formatter);
      } catch (Exception ignored) {
        // Intentar siguiente formato
      }
    }

    throw new IllegalStateException("Formato de fecha de autorización no soportado: " + rawDate);
  }

  public Map<String, Object> buildDebitNoteParameters(DebitNotePdfData data) {
    NotaDebito notaDebito = data.notaDebito();

    Map<String, Object> params = new HashMap<>();
    params.put("razonSocial", notaDebito.getInfoTributaria().getRazonSocial());
    params.put("nombreComercial", notaDebito.getInfoTributaria().getNombreComercial());
    params.put("ruc", notaDebito.getInfoTributaria().getRuc());
    params.put("numeroAutorizacion", data.numeroAutorizacion());
    params.put("fechaAutorizacion", DISPLAY_DATE.format(data.fechaAutorizacion()));
    params.put("estadoAutorizacion", data.estadoAutorizacion());
    params.put("ambienteAutorizacion", Optional.ofNullable(data.ambienteAutorizacion()).orElse(""));
    params.put("direccionMatriz", notaDebito.getInfoTributaria().getDirMatriz());
    params.put("direccionEstablecimiento", notaDebito.getInfoNotaDebito().getDirEstablecimiento());
    params.put("fechaEmision", notaDebito.getInfoNotaDebito().getFechaEmision());
    params.put(
        "ambienteAutorizacion",
        AMBIENTE_LABELS.getOrDefault(
            notaDebito.getInfoTributaria().getAmbiente().name(),
            notaDebito.getInfoTributaria().getAmbiente().name()));
    params.put(
        "tipoEmision",
        TIPO_EMISION_LABELS.getOrDefault(
            notaDebito.getInfoTributaria().getTipoEmision().name(),
            notaDebito.getInfoTributaria().getTipoEmision().name()));
    params.put("razonSocialComprador", notaDebito.getInfoNotaDebito().getRazonSocialComprador());
    params.put(
        "identificacionComprador", notaDebito.getInfoNotaDebito().getIdentificacionComprador());
    params.put("direccionComprador", ""); // Nota de débito no tiene dirección del comprador
    params.put(
        "obligadoContabilidad",
        Optional.ofNullable(notaDebito.getInfoNotaDebito().getObligadoContabilidad())
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
        "totalSinImpuestos", formatCurrency(notaDebito.getInfoNotaDebito().getTotalSinImpuestos()));
    params.put("valorTotal", formatCurrency(notaDebito.getInfoNotaDebito().getValorTotal()));
    params.put("claveAcceso", data.getClaveAcceso());
    params.put("barcodeImage", generateBarcodeImage(data.getClaveAcceso()));

    String numeroNotaDebito =
        notaDebito.getInfoTributaria().getEstab()
            + "-"
            + notaDebito.getInfoTributaria().getPtoEmi()
            + "-"
            + notaDebito.getInfoTributaria().getSecuencial();
    params.put("numeroNotaDebito", numeroNotaDebito);

    // Campos específicos de nota de débito
    params.put("codDocModificado", notaDebito.getInfoNotaDebito().getCodDocModificado());
    params.put("numDocModificado", notaDebito.getInfoNotaDebito().getNumDocModificado());
    params.put(
        "fechaEmisionDocSustento", notaDebito.getInfoNotaDebito().getFechaEmisionDocSustento());

    params.put("logoUrl", loadLogoUrl(data.logoPath()));
    params.put("paymentsDataSource", buildPaymentDataSource(notaDebito));
    params.put("motivosDataSource", buildMotivosDataSource(notaDebito));

    Map<TaxKey, TaxAccumulator> groupedTotals = buildTaxAccumulatorMap(notaDebito);
    params.put("taxesDataSource", toTaxesDataSource(groupedTotals));
    params.put("summaryTotalsDataSource", buildSummaryTotalsDataSource(notaDebito, groupedTotals));

    String infoAdicional =
        Optional.ofNullable(notaDebito.getInfoAdicional()).orElse(Collections.emptyList()).stream()
            .map(ca -> ca.getNombre() + ": " + ca.getValue())
            .collect(Collectors.joining("\n"));
    params.put("infoAdicional", infoAdicional);

    return params;
  }

  private String mapRegimeToLabel(String regime) {
    if (!StringUtils.hasText(regime)) {
      return null;
    }

    String mappingParam = paramsService.getParamValueOrDefault("COMPANY_REGIME_MAPPING", "");
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
    } catch (Exception ex) {
      try {
        return getClass().getClassLoader().getResource("jasper/no_tiene_logo.png").toURI().toURL();
      } catch (Exception e) {
        throw new IllegalStateException("No se pudo cargar el logo por defecto", e);
      }
    }
  }

  public JRBeanCollectionDataSource buildMotivosDataSource(NotaDebito notaDebito) {
    List<MotivoRow> motivos =
        Optional.ofNullable(notaDebito.getMotivos()).orElse(Collections.emptyList()).stream()
            .map(this::toMotivoRow)
            .toList();
    return new JRBeanCollectionDataSource(motivos);
  }

  private MotivoRow toMotivoRow(MotivoNotaDebito motivo) {
    return new MotivoRow(
        motivo.getRazon() != null ? motivo.getRazon() : "", formatCurrency(motivo.getValor()));
  }

  public JRBeanCollectionDataSource buildPaymentDataSource(NotaDebito notaDebito) {
    List<PaymentRow> rows =
        Optional.ofNullable(notaDebito.getInfoNotaDebito())
            .map(InfoNotaDebito::getPagos)
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

  private Map<TaxKey, TaxAccumulator> buildTaxAccumulatorMap(NotaDebito notaDebito) {
    Map<TaxKey, TaxAccumulator> groupedTotals = new LinkedHashMap<>();

    Optional.ofNullable(notaDebito.getInfoNotaDebito())
        .map(InfoNotaDebito::getImpuestos)
        .orElse(Collections.emptyList())
        .forEach(
            impuesto -> {
              TaxKey key =
                  new TaxKey(
                      impuesto.getCodigo(),
                      impuesto.getCodigoPorcentaje(),
                      resolveRateLabel(impuesto));
              groupedTotals
                  .computeIfAbsent(key, k -> new TaxAccumulator())
                  .accumulate(
                      parseAmount(impuesto.getBaseImponible()), parseAmount(impuesto.getValor()));
            });

    return groupedTotals;
  }

  private String resolveRateLabel(ImpuestoNotaDebito impuesto) {
    if (impuesto == null) {
      return "";
    }

    String tarifa = impuesto.getTarifa();
    if (StringUtils.hasText(tarifa)) {
      String trimmed = tarifa.trim();
      return trimmed.endsWith("%") ? trimmed : trimmed + "%";
    }

    String codigoPorcentaje = impuesto.getCodigoPorcentaje();
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
            rows.add(
                new TaxRow(
                    taxName,
                    formatCurrency(accumulator.baseSum()),
                    formatCurrency(accumulator.taxSum())));
          }
        });
    return new JRBeanCollectionDataSource(rows);
  }

  private JRBeanCollectionDataSource buildSummaryTotalsDataSource(
      NotaDebito notaDebito, Map<TaxKey, TaxAccumulator> groupedTotals) {

    InfoNotaDebito info = notaDebito.getInfoNotaDebito();
    BigDecimal totalSinImpuestos = parseAmount(info.getTotalSinImpuestos());
    BigDecimal valorTotal = parseAmount(info.getValorTotal());

    // Construir un Map con todos los SummaryRow disponibles
    Map<String, SummaryRow> rowsMap = new HashMap<>();

    // Subtotal sin impuestos
    rowsMap.put(
        "SUBTOTAL SIN IMPUESTOS",
        new SummaryRow("Subtotal Sin Impuestos", formatCurrency(totalSinImpuestos)));

    // Agregar dinámicamente todos los subtotales de IVA que existan en la nota de débito
    Set<String> ivaTarifasEncontradas = extractIvaTarifasFromImpuestos(notaDebito);
    for (String tarifa : ivaTarifasEncontradas) {
      BigDecimal baseIva = calculateIvaBaseFromTotalImpuestos(groupedTotals, tarifa);
      String label = "Subtotal " + tarifa + "%";
      String key = "SUBTOTAL " + tarifa + "%";
      rowsMap.put(key, new SummaryRow(label, formatCurrency(baseIva)));
    }

    // Agregar dinámicamente todos los IVA que existan en la nota de débito
    for (String tarifa : ivaTarifasEncontradas) {
      BigDecimal ivaValor = getIvaTaxByTarifa(groupedTotals, tarifa);
      String label = "IVA " + tarifa + "%";
      String key = "IVA " + tarifa + "%";
      rowsMap.put(key, new SummaryRow(label, formatCurrency(ivaValor)));
    }

    // Agregar descuento, ICE, propina y valor total (las ND no manejan descuento/propina en XML)
    rowsMap.put("DESCUENTO", new SummaryRow("Descuento", formatCurrency(BigDecimal.ZERO)));
    rowsMap.put(
        "ICE", new SummaryRow("ICE", formatCurrency(getAccumulatorTaxByCode(groupedTotals, "3"))));
    rowsMap.put("PROPINA", new SummaryRow("Propina", formatCurrency(BigDecimal.ZERO)));
    rowsMap.put("PROPINA 10%", new SummaryRow("Propina 10%", formatCurrency(BigDecimal.ZERO)));
    rowsMap.put("VALOR A PAGAR", new SummaryRow("Valor a pagar", formatCurrency(valorTotal)));

    // Leer el orden desde el parámetro de BD para que coincida con la factura / nota de crédito
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
    String upperKey = labelKey.toUpperCase(Locale.ROOT);
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
   * Extrae todas las tarifas de IVA únicas que existen en los impuestos de la nota de débito,
   * ordenadas de mayor a menor.
   */
  private Set<String> extractIvaTarifasFromImpuestos(NotaDebito notaDebito) {
    return Optional.ofNullable(notaDebito.getInfoNotaDebito())
        .map(InfoNotaDebito::getImpuestos)
        .orElse(Collections.emptyList())
        .stream()
        .filter(impuesto -> "2".equals(impuesto.getCodigo())) // Solo IVA
        .map(
            impuesto -> {
              try {
                BigDecimal tarifa = new BigDecimal(impuesto.getTarifa());
                return tarifa.stripTrailingZeros().toPlainString();
              } catch (NumberFormatException e) {
                return null;
              }
            })
        .filter(Objects::nonNull)
        .collect(
            Collectors.toCollection(
                () ->
                    new TreeSet<>(
                        (a, b) -> {
                          try {
                            BigDecimal aNum = new BigDecimal(a);
                            BigDecimal bNum = new BigDecimal(b);
                            return bNum.compareTo(aNum); // mayor a menor
                          } catch (NumberFormatException e) {
                            return b.compareTo(a);
                          }
                        })));
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
      Map<TaxKey, TaxAccumulator> groupedTotals, String codigo) {
    return groupedTotals.entrySet().stream()
        .filter(entry -> codigo.equals(entry.getKey().codigo()))
        .map(Map.Entry::getValue)
        .map(TaxAccumulator::taxSum)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
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

  private String formatEnumName(String name) {
    if (!StringUtils.hasText(name)) {
      return "";
    }
    return java.util.Arrays.stream(name.toLowerCase(Locale.ROOT).split("_"))
        .filter(part -> !part.isBlank())
        .map(part -> part.substring(0, 1).toUpperCase(Locale.ROOT) + part.substring(1))
        .collect(Collectors.joining(" "));
  }

  public JasperPrint buildJasperPrint(DebitNotePdfData data) {
    try (InputStream jrxml =
        new ClassPathResource("jasper/debit_note_template.jrxml").getInputStream()) {
      JasperReport jasperReport = JasperCompileManager.compileReport(jrxml);
      Map<String, Object> parameters = buildDebitNoteParameters(data);
      JRBeanCollectionDataSource motivosDataSource = buildMotivosDataSource(data.notaDebito());
      return JasperFillManager.fillReport(jasperReport, parameters, motivosDataSource);
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

  @Override
  public Path generateAndSaveDebitNotePdf(String authorizationXml, Path outputDirectory) {
    return generateAndSaveDebitNotePdf(authorizationXml, outputDirectory, null);
  }

  @Override
  public Path generateAndSaveDebitNotePdf(
      String authorizationXml, Path outputDirectory, String logoPath) {
    return generateAndSaveDebitNotePdf(authorizationXml, outputDirectory, logoPath, null, null);
  }

  @Override
  public Path generateAndSaveDebitNotePdf(
      String authorizationXml,
      Path outputDirectory,
      String logoPath,
      String regime,
      String specialTaxpayerResolution) {
    DebitNotePdfData data =
        readDebitNoteData(authorizationXml, logoPath, regime, specialTaxpayerResolution);
    JasperPrint jasperPrint = buildJasperPrint(data);
    byte[] pdfBytes = exportPdf(jasperPrint);

    try {
      Path targetDirectory = outputDirectory;
      Files.createDirectories(targetDirectory);

      String fileName =
          "%s-%s-%s.pdf"
              .formatted(
                  data.notaDebito().getInfoTributaria().getEstab(),
                  data.notaDebito().getInfoTributaria().getPtoEmi(),
                  data.notaDebito().getInfoTributaria().getSecuencial());

      Path pdfPath = targetDirectory.resolve(fileName);
      Files.write(pdfPath, pdfBytes);

      log.info("✅ PDF de nota de débito generado: {}", pdfPath);
      return pdfPath;
    } catch (Exception ex) {
      throw new IllegalStateException("No se pudo guardar el PDF de la nota de débito", ex);
    }
  }
}
