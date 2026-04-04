package com.wallparisoft.ebillbilling.service.impl;

import com.wallparisoft.ebillbilling.dto.GuidePdfData;
import com.wallparisoft.ebillbilling.service.GuidePdfService;
import com.wallparisoft.ebillbilling.service.ParamsService;
import com.wallparisoft.integration.sri.model.Destinatario;
import com.wallparisoft.integration.sri.model.DetalleGuiaRemision;
import com.wallparisoft.integration.sri.model.GuiaRemision;
import com.wallparisoft.integration.sri.model.InfoGuiaRemision;
import com.wallparisoft.integration.sri.util.XmlUtil;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import org.w3c.dom.NodeList;

/**
 * Servicio encargado de construir el RIDE (PDF) a partir del XML de autorización del SRI para guías
 * de remisión. Incluye la recopilación de parámetros y la generación de la estructura de carpetas
 * donde se almacena el archivo final.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GuidePdfServiceImpl implements GuidePdfService {

  private final ParamsService paramsService;

  private static final DateTimeFormatter DISPLAY_DATE =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
  private static final DateTimeFormatter DIRECTORY_DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

  private static final Map<String, String> AMBIENTE_LABELS =
      Map.of("1", "PRUEBAS", "2", "PRODUCCIÓN");

  private static final Map<String, String> TIPO_EMISION_LABELS =
      Map.of("1", "EMISIÓN NORMAL", "2", "EMISIÓN POR INDISPONIBILIDAD DEL SISTEMA");

  @Override
  public Path generateAndSaveGuidePdf(String authorizationXml, Path outputDirectory) {
    return generateAndSaveGuidePdf(authorizationXml, outputDirectory, null, null, null);
  }

  @Override
  public Path generateAndSaveGuidePdf(
      String authorizationXml, Path outputDirectory, String logoPath) {
    return generateAndSaveGuidePdf(authorizationXml, outputDirectory, logoPath, null, null);
  }

  @Override
  public Path generateAndSaveGuidePdf(
      String authorizationXml,
      Path outputDirectory,
      String logoPath,
      String regime,
      String specialTaxpayerResolution) {
    try {
      GuidePdfData pdfData =
          readGuideData(authorizationXml, logoPath, regime, specialTaxpayerResolution);

      Map<String, Object> parameters = buildGuideParameters(pdfData);

      // TODO: Crear plantilla Jasper guide_template.jrxml
      // Por ahora, lanzamos una excepción indicando que la plantilla no existe
      String templatePath = "jasper/guide_template.jrxml";
      InputStream templateStream = getClass().getClassLoader().getResourceAsStream(templatePath);
      if (templateStream == null) {
        throw new IllegalStateException(
            "La plantilla Jasper para guías de remisión no existe: "
                + templatePath
                + ". Por favor, cree la plantilla antes de generar PDFs.");
      }

      JasperReport jasperReport = JasperCompileManager.compileReport(templateStream);

      // Crear DataSource para detalles de productos
      JRBeanCollectionDataSource guideDetailsDataSource = buildGuideDetailsDataSource(pdfData);

      JasperPrint jasperPrint =
          JasperFillManager.fillReport(jasperReport, parameters, guideDetailsDataSource);

      // Usar el número de guía (estab-ptoEmi-secuencial) en lugar de la clave de acceso
      GuiaRemision guiaRemision = pdfData.guiaRemision();
      String fileName =
          "%s-%s-%s.pdf"
              .formatted(
                  guiaRemision.getInfoTributaria().getEstab(),
                  guiaRemision.getInfoTributaria().getPtoEmi(),
                  guiaRemision.getInfoTributaria().getSecuencial());
      Path pdfPath = outputDirectory.resolve(fileName);

      Files.createDirectories(outputDirectory);
      JasperExportManager.exportReportToPdfFile(jasperPrint, pdfPath.toString());

      log.info("PDF de guía de remisión generado exitosamente: {}", pdfPath);
      return pdfPath;

    } catch (Exception ex) {
      log.error("Error al generar PDF de guía de remisión", ex);
      throw new IllegalStateException("No se pudo generar el PDF de la guía de remisión", ex);
    }
  }

  public GuidePdfData readGuideData(
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

      // Parsear el XML del comprobante (guía de remisión) usando JAXB
      GuiaRemision guiaRemision =
          XmlUtil.unmarshal(
              new ByteArrayInputStream(comprobanteXml.getBytes(StandardCharsets.UTF_8)),
              GuiaRemision.class);

      LocalDateTime fechaAutorizacion = parseAuthorizationDate(fechaAutorizacionText);

      return new GuidePdfData(
          guiaRemision,
          estado,
          numeroAutorizacion,
          fechaAutorizacion,
          ambiente,
          logoPath,
          regime,
          specialTaxpayerResolution);

    } catch (Exception ex) {
      throw new IllegalStateException(
          "No se pudo leer el XML de autorización/guía de remisión", ex);
    }
  }

  private String getNodeText(Document document, String tagName) {
    NodeList nodeList = document.getElementsByTagName(tagName);
    if (nodeList != null && nodeList.getLength() > 0) {
      Node node = nodeList.item(0);
      return node != null ? node.getTextContent().trim() : "";
    }
    return "";
  }

  private LocalDateTime parseAuthorizationDate(String rawDate) {
    if (!StringUtils.hasText(rawDate)) {
      return LocalDateTime.now();
    }

    try {
      // Formato: 2024-01-15T10:30:00-05:00
      if (rawDate.contains("T")) {
        return LocalDateTime.parse(rawDate.substring(0, 19));
      }
      // Formato: 2024-01-15 10:30:00
      return LocalDateTime.parse(rawDate.substring(0, 19).replace(" ", "T"));
    } catch (Exception e) {
      log.warn("No se pudo parsear la fecha de autorización: {}", rawDate, e);
      return LocalDateTime.now();
    }
  }

  public Map<String, Object> buildGuideParameters(GuidePdfData data) {
    GuiaRemision guiaRemision = data.guiaRemision();

    Map<String, Object> params = new HashMap<>();

    // Información tributaria
    if (guiaRemision.getInfoTributaria() != null) {
      params.put("razonSocial", guiaRemision.getInfoTributaria().getRazonSocial());
      params.put("nombreComercial", guiaRemision.getInfoTributaria().getNombreComercial());
      params.put("ruc", guiaRemision.getInfoTributaria().getRuc());
      params.put("direccionMatriz", guiaRemision.getInfoTributaria().getDirMatriz());

      // Número de guía: estab-ptoEmi-secuencial
      String numeroGuia = "";
      if (guiaRemision.getInfoTributaria().getEstab() != null
          && guiaRemision.getInfoTributaria().getPtoEmi() != null
          && guiaRemision.getInfoTributaria().getSecuencial() != null) {
        numeroGuia =
            guiaRemision.getInfoTributaria().getEstab()
                + "-"
                + guiaRemision.getInfoTributaria().getPtoEmi()
                + "-"
                + guiaRemision.getInfoTributaria().getSecuencial();
      }
      params.put("numeroGuia", numeroGuia);
    }

    params.put("numeroAutorizacion", data.numeroAutorizacion());
    params.put("fechaAutorizacion", DISPLAY_DATE.format(data.fechaAutorizacion()));
    params.put("estadoAutorizacion", data.estadoAutorizacion());

    // Ambiente (debe obtenerse del objeto GuiaRemision, no del XML de autorización)
    if (guiaRemision.getInfoTributaria() != null
        && guiaRemision.getInfoTributaria().getAmbiente() != null) {
      params.put(
          "ambienteAutorizacion",
          AMBIENTE_LABELS.getOrDefault(
              guiaRemision.getInfoTributaria().getAmbiente().getCodigo(),
              guiaRemision.getInfoTributaria().getAmbiente().name()));
    } else {
      params.put("ambienteAutorizacion", "");
    }

    // Tipo de emisión
    if (guiaRemision.getInfoTributaria() != null
        && guiaRemision.getInfoTributaria().getTipoEmision() != null) {
      params.put(
          "tipoEmision",
          TIPO_EMISION_LABELS.getOrDefault(
              guiaRemision.getInfoTributaria().getTipoEmision().name(),
              guiaRemision.getInfoTributaria().getTipoEmision().name()));
    } else {
      params.put("tipoEmision", "");
    }

    // Información de la guía
    params.put("claveAcceso", data.getClaveAcceso());
    params.put("barcodeImage", generateBarcodeImage(data.getClaveAcceso()));

    // Información de la guía de remisión (InfoGuiaRemision)
    if (guiaRemision.getInfoGuiaRemision() != null) {
      InfoGuiaRemision infoGuia = guiaRemision.getInfoGuiaRemision();
      params.put("direccionEstablecimiento", infoGuia.getDirEstablecimiento());
      params.put("direccionPartida", infoGuia.getDirPartida());
      params.put("razonSocialTransportista", infoGuia.getRazonSocialTransportista());
      params.put("rucTransportista", infoGuia.getRucTransportista());
      params.put("fechaIniTransporte", infoGuia.getFechaIniTransporte());
      params.put("fechaFinTransporte", infoGuia.getFechaFinTransporte());
      params.put("placa", infoGuia.getPlaca());

      // Obligado a llevar contabilidad
      params.put(
          "obligadoContabilidad",
          Optional.ofNullable(infoGuia.getObligadoContabilidad()).map(Enum::name).orElse(""));
    } else {
      params.put("obligadoContabilidad", "");
    }

    // Información del destinatario (tomar el primero si existe)
    if (guiaRemision.getDestinatarios() != null && !guiaRemision.getDestinatarios().isEmpty()) {
      Destinatario destinatario = guiaRemision.getDestinatarios().get(0);
      params.put("identificacionDestinatario", destinatario.getIdentificacionDestinatario());
      params.put("razonSocialDestinatario", destinatario.getRazonSocialDestinatario());
      params.put("direccionDestinatario", destinatario.getDirDestinatario());
      params.put("motivoTraslado", destinatario.getMotivoTraslado());
      params.put("ruta", destinatario.getRuta());
      params.put(
          "docAduaneroUnico",
          StringUtils.hasText(destinatario.getDocAduaneroUnico())
              ? destinatario.getDocAduaneroUnico()
              : "");
      params.put(
          "codEstabDestino",
          StringUtils.hasText(destinatario.getCodEstabDestino())
              ? destinatario.getCodEstabDestino()
              : "");
      // Documento de sustento
      params.put(
          "numDocSustento",
          StringUtils.hasText(destinatario.getNumDocSustento())
              ? destinatario.getNumDocSustento()
              : "");
      params.put(
          "numAutDocSustento",
          StringUtils.hasText(destinatario.getNumAutDocSustento())
              ? destinatario.getNumAutDocSustento()
              : "");
      params.put(
          "fechaEmisionDocSustento",
          StringUtils.hasText(destinatario.getFechaEmisionDocSustento())
              ? destinatario.getFechaEmisionDocSustento()
              : "");
    } else {
      params.put("docAduaneroUnico", "");
      params.put("codEstabDestino", "");
      params.put("numDocSustento", "");
      params.put("numAutDocSustento", "");
      params.put("fechaEmisionDocSustento", "");
    }

    // Logo
    params.put("logoUrl", loadLogoUrl(data.logoPath()));

    // Régimen tributario
    String regimeLabel = mapRegimeToLabel(data.regime());
    params.put("contribuyente", regimeLabel != null ? regimeLabel : "");

    // Contribuyente Especial Resolución
    String specialTaxpayerResolution = data.specialTaxpayerResolution();
    params.put(
        "contribuyenteEspecialResolucion",
        StringUtils.hasText(specialTaxpayerResolution) ? specialTaxpayerResolution : "");

    // Información adicional (campos adicionales)
    String infoAdicional =
        Optional.ofNullable(guiaRemision.getInfoAdicional())
            .orElse(Collections.emptyList())
            .stream()
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

      return ImageIO.read(new ByteArrayInputStream(out.toByteArray()));
    } catch (Exception ex) {
      log.warn("No se pudo generar el código de barras para: {}", code, ex);
      return null;
    }
  }

  /**
   * Construye un DataSource de JasperReports para los detalles de productos de la guía de remisión.
   */
  private JRBeanCollectionDataSource buildGuideDetailsDataSource(GuidePdfData data) {
    GuiaRemision guiaRemision = data.guiaRemision();

    List<GuideDetailRow> detalles = Collections.emptyList();

    if (guiaRemision.getDestinatarios() != null && !guiaRemision.getDestinatarios().isEmpty()) {
      Destinatario destinatario = guiaRemision.getDestinatarios().get(0);
      if (destinatario.getDetalles() != null && !destinatario.getDetalles().isEmpty()) {
        detalles = destinatario.getDetalles().stream().map(this::toGuideDetailRow).toList();
      }
    }

    return new JRBeanCollectionDataSource(detalles);
  }

  /** Convierte un DetalleGuiaRemision a GuideDetailRow para JasperReports. */
  private GuideDetailRow toGuideDetailRow(DetalleGuiaRemision detalle) {
    String codigoInterno =
        StringUtils.hasText(detalle.getCodigoInterno()) ? detalle.getCodigoInterno() : "";
    String codigoAdicional =
        StringUtils.hasText(detalle.getCodigoAdicional()) ? detalle.getCodigoAdicional() : "";
    String descripcion =
        StringUtils.hasText(detalle.getDescripcion()) ? detalle.getDescripcion() : "";
    String cantidad = StringUtils.hasText(detalle.getCantidad()) ? detalle.getCantidad() : "0";

    return new GuideDetailRow(codigoInterno, codigoAdicional, descripcion, cantidad);
  }

  /** Clase para representar una fila de detalle de guía de remisión en JasperReports. */
  public static class GuideDetailRow {
    private final String codigoInterno;
    private final String codigoAdicional;
    private final String descripcion;
    private final String cantidad;

    GuideDetailRow(
        String codigoInterno, String codigoAdicional, String descripcion, String cantidad) {
      this.codigoInterno = codigoInterno;
      this.codigoAdicional = codigoAdicional;
      this.descripcion = descripcion;
      this.cantidad = cantidad;
    }

    public String getCodigoInterno() {
      return codigoInterno;
    }

    public String getCodigoAdicional() {
      return codigoAdicional;
    }

    public String getDescripcion() {
      return descripcion;
    }

    public String getCantidad() {
      return cantidad;
    }
  }
}
