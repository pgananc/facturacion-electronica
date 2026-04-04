# Cambios Propuestos: Template de Nota de Crédito

## Resumen

Se propone agregar campos debajo de "Fecha de Emisión:" en el template `credit_note_template.jrxml` para mostrar información sobre el comprobante que se modifica:

- **Comprobante que se modifica**: FACTURA 001-002-031837657
- **Fecha Emisión (Comprobante a modificar)**: 05/08/2024
- **Razón de Modificación**: REVERSO DETALLE : ASEGURADOID: 2471981988068

## Cambios en el Template JRXML

### Archivo: `src/main/resources/jasper/credit_note_template.jrxml`

#### 1. Agregar nuevos parámetros (después de la línea 50)

```xml
<parameter name="comprobanteModificado" class="java.lang.String"/>
<parameter name="fechaEmisionComprobanteModificado" class="java.lang.String"/>
<parameter name="razonModificacion" class="java.lang.String"/>
```

#### 2. Modificar la sección "Fecha de Emisión" (líneas 331-341)

**ANTES:**

```xml
<staticText>
    <reportElement x="0" y="60" width="150" height="15" uuid="76ef650c-6c22-4251-870f-bbdc5705035e"/>
    <textElement>
        <font isBold="true"/>
    </textElement>
    <text><![CDATA[Fecha de Emisión:]]></text>
</staticText>
<textField>
    <reportElement x="151" y="60" width="360" height="15" uuid="db291ffa-6ef0-41f2-b9b6-7d207c3af0b1"/>
    <textFieldExpression><![CDATA[$P{fechaEmision}]]></textFieldExpression>
</textField>
```

**DESPUÉS:**

```xml
<staticText>
    <reportElement x="0" y="60" width="150" height="15" uuid="76ef650c-6c22-4251-870f-bbdc5705035e"/>
    <textElement>
        <font isBold="true"/>
    </textElement>
    <text><![CDATA[Fecha de Emisión:]]></text>
</staticText>
<textField>
    <reportElement x="151" y="60" width="360" height="15" uuid="db291ffa-6ef0-41f2-b9b6-7d207c3af0b1"/>
    <textFieldExpression><![CDATA[$P{fechaEmision}]]></textFieldExpression>
</textField>
<!-- NUEVOS CAMPOS: Información del comprobante modificado -->
<staticText>
    <reportElement x="0" y="75" width="200" height="15" uuid="a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5f">
        <property name="com.jaspersoft.studio.unit.height" value="px"/>
    </reportElement>
    <textElement>
        <font isBold="true"/>
    </textElement>
    <text><![CDATA[Comprobante que se modifica:]]></text>
</staticText>
<textField>
    <reportElement x="201" y="75" width="310" height="15" uuid="b2c3d4e5-f6a7-4b8c-9d0e-1f2a3b4c5d6e">
        <property name="com.jaspersoft.studio.unit.height" value="px"/>
    </reportElement>
    <textFieldExpression><![CDATA[$P{comprobanteModificado} != null ? $P{comprobanteModificado} : ""]]></textFieldExpression>
</textField>
<staticText>
    <reportElement x="0" y="90" width="200" height="15" uuid="c3d4e5f6-a7b8-4c9d-0e1f-2a3b4c5d6e7f">
        <property name="com.jaspersoft.studio.unit.height" value="px"/>
    </reportElement>
    <textElement>
        <font isBold="true"/>
    </textElement>
    <text><![CDATA[Fecha Emisión (Comprobante a modificar):]]></text>
</staticText>
<textField>
    <reportElement x="201" y="90" width="310" height="15" uuid="d4e5f6a7-b8c9-4d0e-1f2a-3b4c5d6e7f8a">
        <property name="com.jaspersoft.studio.unit.height" value="px"/>
    </reportElement>
    <textFieldExpression><![CDATA[$P{fechaEmisionComprobanteModificado} != null ? $P{fechaEmisionComprobanteModificado} : ""]]></textFieldExpression>
</textField>
<staticText>
    <reportElement x="0" y="105" width="200" height="15" uuid="e5f6a7b8-c9d0-4e1f-2a3b-4c5d6e7f8a9b">
        <property name="com.jaspersoft.studio.unit.height" value="px"/>
    </reportElement>
    <textElement>
        <font isBold="true"/>
    </textElement>
    <text><![CDATA[Razón de Modificación:]]></text>
</staticText>
<textField isStretchWithOverflow="true">
    <reportElement x="201" y="105" width="310" height="15" uuid="f6a7b8c9-d0e1-4f2a-3b4c-5d6e7f8a9b0c">
        <property name="com.jaspersoft.studio.unit.height" value="px"/>
    </reportElement>
    <textFieldExpression><![CDATA[$P{razonModificacion} != null ? $P{razonModificacion} : ""]]></textFieldExpression>
</textField>
```

#### 3. Ajustar altura del frame (línea 294)

**ANTES:**

```xml
<frame>
    <reportElement mode="Opaque" x="0" y="8" width="535" height="75" backcolor="#EFEFEF" uuid="a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5e">
```

**DESPUÉS:**

```xml
<frame>
    <reportElement mode="Opaque" x="0" y="8" width="535" height="120" backcolor="#EFEFEF" uuid="a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5e">
```

#### 4. Ajustar posición del frame de encabezado de columnas (línea 343)

**ANTES:**

```xml
<frame>
    <reportElement mode="Opaque" x="0" y="93" width="535" height="25" backcolor="#EFEFEF" uuid="46917105-5f54-4a4c-916e-89a1799512c4"/>
```

**DESPUÉS:**

```xml
<frame>
    <reportElement mode="Opaque" x="0" y="138" width="535" height="25" backcolor="#EFEFEF" uuid="46917105-5f54-4a4c-916e-89a1799512c4"/>
```

#### 5. Ajustar altura del band pageHeader (línea 290)

**ANTES:**

```xml
<pageHeader>
    <band height="118">
```

**DESPUÉS:**

```xml
<pageHeader>
    <band height="163">
```

## Cambios en el Código Java

### Archivo: `src/main/java/com/wallparisoft/ebillbilling/service/impl/InvoicePdfServiceImpl.java`

#### Modificar método `readInvoiceData()` para detectar NotaCredito

El método actual lee `Factura`, pero para notas de crédito necesitamos leer `NotaCredito`. Se debe modificar para detectar el tipo de comprobante:

```java
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

    // Detectar si es NotaCredito o Factura
    boolean esNotaCredito = comprobanteXml.contains("<notaCredito")
                         || comprobanteXml.contains("notaCredito");

    Factura factura;
    if (esNotaCredito) {
      // Leer como NotaCredito y convertir a Factura para compatibilidad
      NotaCredito notaCredito =
          XmlUtil.unmarshal(
              new ByteArrayInputStream(comprobanteXml.getBytes(StandardCharsets.UTF_8)),
              NotaCredito.class);
      factura = convertNotaCreditoToFactura(notaCredito);
    } else {
      factura =
          XmlUtil.unmarshal(
              new ByteArrayInputStream(comprobanteXml.getBytes(StandardCharsets.UTF_8)),
              Factura.class);
    }

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
```

#### Agregar método para convertir NotaCredito a Factura (temporal)

```java
private Factura convertNotaCreditoToFactura(NotaCredito notaCredito) {
    // Convertir NotaCredito a Factura para reutilizar la lógica existente
    // Esto es temporal hasta que se cree un método específico para notas de crédito
    Factura factura = new Factura();
    factura.setInfoTributaria(notaCredito.getInfoTributaria());

    // Convertir InfoNotaCredito a InfoFactura
    InfoFactura infoFactura = new InfoFactura();
    InfoNotaCredito infoNotaCredito = notaCredito.getInfoNotaCredito();

    infoFactura.setFechaEmision(infoNotaCredito.getFechaEmision());
    infoFactura.setDirEstablecimiento(infoNotaCredito.getDirEstablecimiento());
    infoFactura.setTipoIdentificacionComprador(infoNotaCredito.getTipoIdentificacionComprador());
    infoFactura.setRazonSocialComprador(infoNotaCredito.getRazonSocialComprador());
    infoFactura.setIdentificacionComprador(infoNotaCredito.getIdentificacionComprador());
    infoFactura.setObligadoContabilidad(infoNotaCredito.getObligadoContabilidad());
    infoFactura.setTotalSinImpuestos(infoNotaCredito.getTotalSinImpuestos());
    infoFactura.setMoneda(infoNotaCredito.getMoneda());
    infoFactura.setTotalConImpuestos(infoNotaCredito.getTotalConImpuestos());

    factura.setInfoFactura(infoFactura);
    factura.setDetalles(convertDetallesNotaCredito(notaCredito.getDetalles()));
    factura.setInfoAdicional(notaCredito.getInfoAdicional());

    return factura;
}
```

#### Modificar método `buildInvoiceParameters()` para incluir datos de NotaCredito

**OPCIÓN 1: Agregar parámetros específicos de NotaCredito al método existente**

```java
public Map<String, Object> buildInvoiceParameters(InvoicePdfData data) {
    Factura factura = data.factura();

    Map<String, Object> params = new HashMap<>();
    // ... parámetros existentes ...

    // NUEVOS PARÁMETROS PARA NOTA DE CRÉDITO
    // Estos parámetros se obtienen del XML de NotaCredito
    // Si el XML es de NotaCredito, extraer de infoAdicional o del XML original

    // Comprobante que se modifica (ej: "FACTURA 001-002-031837657")
    // Formato: TIPO_DOC + " " + numDocModificado
    String comprobanteModificado = extractComprobanteModificado(data);
    params.put("comprobanteModificado", comprobanteModificado != null ? comprobanteModificado : "");

    // Fecha de emisión del comprobante modificado (ej: "05/08/2024")
    String fechaEmisionComprobanteModificado = extractFechaEmisionComprobanteModificado(data);
    params.put("fechaEmisionComprobanteModificado", fechaEmisionComprobanteModificado != null ? fechaEmisionComprobanteModificado : "");

    // Razón de modificación (ej: "REVERSO DETALLE : ASEGURADOID: 2471981988068")
    String razonModificacion = extractRazonModificacion(data);
    params.put("razonModificacion", razonModificacion != null ? razonModificacion : "");

    return params;
}

// Métodos auxiliares - necesitan acceso al XML original o a NotaCredito
private String extractComprobanteModificado(InvoicePdfData data) {
    // Si tenemos acceso al XML original, parsearlo como NotaCredito
    // O buscar en infoAdicional
    if (data.factura().getInfoAdicional() != null) {
        return data.factura().getInfoAdicional().stream()
            .filter(ca -> "numDocModificado".equalsIgnoreCase(ca.getNombre()))
            .findFirst()
            .map(ca -> {
                // Formatear como "FACTURA 001-002-031837657"
                String codDoc = extractCodDocModificado(data);
                String tipoDoc = mapCodDocToTipo(codDoc); // "01" -> "FACTURA"
                return tipoDoc + " " + ca.getValue();
            })
            .orElse(null);
    }
    return null;
}

private String extractFechaEmisionComprobanteModificado(InvoicePdfData data) {
    if (data.factura().getInfoAdicional() != null) {
        return data.factura().getInfoAdicional().stream()
            .filter(ca -> "fechaEmisionDocSustento".equalsIgnoreCase(ca.getNombre()))
            .findFirst()
            .map(ca -> ca.getValue())
            .orElse(null);
    }
    return null;
}

private String extractRazonModificacion(InvoicePdfData data) {
    if (data.factura().getInfoAdicional() != null) {
        return data.factura().getInfoAdicional().stream()
            .filter(ca -> "motivo".equalsIgnoreCase(ca.getNombre()))
            .findFirst()
            .map(ca -> ca.getValue())
            .orElse(null);
    }
    return null;
}

private String mapCodDocToTipo(String codDoc) {
    // Mapear código de documento a tipo
    if ("01".equals(codDoc)) return "FACTURA";
    if ("04".equals(codDoc)) return "NOTA DE CRÉDITO";
    if ("05".equals(codDoc)) return "NOTA DE DÉBITO";
    return "COMPROBANTE";
}
```

**OPCIÓN 2: Crear método específico para NotaCredito (RECOMENDADO)**

Crear un nuevo método `buildCreditNoteParameters()` que lea directamente del XML de NotaCredito:

```java
public Map<String, Object> buildCreditNoteParameters(String authorizationXml, String logoPath, String regime, String specialTaxpayerResolution) {
    try {
        Document document = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(new ByteArrayInputStream(authorizationXml.getBytes(StandardCharsets.UTF_8)));

        document.getDocumentElement().normalize();
        String comprobanteXml = getNodeText(document, "comprobante");

        NotaCredito notaCredito = XmlUtil.unmarshal(
            new ByteArrayInputStream(comprobanteXml.getBytes(StandardCharsets.UTF_8)),
            NotaCredito.class);

        InfoNotaCredito infoNotaCredito = notaCredito.getInfoNotaCredito();

        Map<String, Object> params = new HashMap<>();
        // ... parámetros comunes (razonSocial, ruc, etc.) ...

        // NUEVOS PARÁMETROS ESPECÍFICOS DE NOTA DE CRÉDITO
        // Comprobante que se modifica: "FACTURA 001-002-031837657"
        String codDoc = infoNotaCredito.getCodDocModificado();
        String tipoDoc = mapCodDocToTipo(codDoc); // "01" -> "FACTURA"
        String numDoc = infoNotaCredito.getNumDocModificado(); // "001-002-031837657"
        params.put("comprobanteModificado", tipoDoc + " " + numDoc);

        // Fecha de emisión del comprobante modificado: "05/08/2024"
        params.put("fechaEmisionComprobanteModificado",
                   infoNotaCredito.getFechaEmisionDocSustento() != null
                       ? infoNotaCredito.getFechaEmisionDocSustento()
                       : "");

        // Razón de modificación: "REVERSO DETALLE : ASEGURADOID: 2471981988068"
        params.put("razonModificacion",
                   infoNotaCredito.getMotivo() != null
                       ? infoNotaCredito.getMotivo()
                       : "");

        return params;
    } catch (Exception ex) {
        throw new IllegalStateException("No se pudo leer el XML de nota de crédito", ex);
    }
}
```

### Nota sobre NotaCredito vs Factura

**IMPORTANTE**: El modelo `NotaCredito` tiene campos específicos en `InfoNotaCredito`:

- `codDocModificado` - Código del documento modificado (ej: "01" para FACTURA)
- `numDocModificado` - Número del documento modificado (ej: "001-002-031837657")
- `fechaEmisionDocSustento` - Fecha de emisión del documento modificado (ej: "05/08/2024")
- `motivo` - Razón de modificación (ej: "REVERSO DETALLE : ASEGURADOID: 2471981988068")

Estos campos están disponibles directamente en el modelo `NotaCredito` y no necesitan extraerse de `infoAdicional`.

### Modificar CreditNotePdfServiceImpl para usar credit_note_template.jrxml

**RECOMENDADO**: Modificar `CreditNotePdfServiceImpl` para usar específicamente `credit_note_template.jrxml` y los parámetros de NotaCredito:

**Archivo**: `src/main/java/com/wallparisoft/ebillbilling/service/impl/CreditNotePdfServiceImpl.java`

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class CreditNotePdfServiceImpl implements CreditNotePdfService {

  private final InvoicePdfService invoicePdfService;
  private final ParamsService paramsService; // Si se necesita

  @Override
  public Path generateAndSaveCreditNotePdf(
      String authorizationXml,
      Path outputDirectory,
      String logoPath,
      String regime,
      String specialTaxpayerResolution) {

    try {
      // Leer el XML de autorización
      Document document = DocumentBuilderFactory.newInstance()
          .newDocumentBuilder()
          .parse(new ByteArrayInputStream(authorizationXml.getBytes(StandardCharsets.UTF_8)));

      document.getDocumentElement().normalize();

      String estado = getNodeText(document, "estado");
      String numeroAutorizacion = getNodeText(document, "numeroAutorizacion");
      String fechaAutorizacionText = getNodeText(document, "fechaAutorizacion");
      String ambiente = getNodeText(document, "ambiente");
      String comprobanteXml = getNodeText(document, "comprobante");

      // Parsear como NotaCredito
      NotaCredito notaCredito = XmlUtil.unmarshal(
          new ByteArrayInputStream(comprobanteXml.getBytes(StandardCharsets.UTF_8)),
          NotaCredito.class);

      LocalDateTime fechaAutorizacion = parseAuthorizationDate(fechaAutorizacionText);

      // Construir parámetros específicos para nota de crédito
      Map<String, Object> parameters = buildCreditNoteParameters(
          notaCredito, estado, numeroAutorizacion, fechaAutorizacion, ambiente,
          logoPath, regime, specialTaxpayerResolution);

      // Construir datasource de detalles
      JRBeanCollectionDataSource detailsDataSource =
          buildCreditNoteDetailsDataSource(notaCredito);

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

        String fileName = String.format("%s-%s-%s.pdf",
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

  private Map<String, Object> buildCreditNoteParameters(
      NotaCredito notaCredito, String estado, String numeroAutorizacion,
      LocalDateTime fechaAutorizacion, String ambiente, String logoPath,
      String regime, String specialTaxpayerResolution) {

    InfoNotaCredito info = notaCredito.getInfoNotaCredito();
    InfoTributaria infoTrib = notaCredito.getInfoTributaria();

    Map<String, Object> params = new HashMap<>();

    // Parámetros comunes (similar a buildInvoiceParameters)
    params.put("razonSocial", infoTrib.getRazonSocial());
    params.put("nombreComercial", infoTrib.getNombreComercial());
    params.put("ruc", infoTrib.getRuc());
    params.put("numeroAutorizacion", numeroAutorizacion);
    params.put("fechaAutorizacion", DISPLAY_DATE.format(fechaAutorizacion));
    params.put("estadoAutorizacion", estado);
    params.put("ambienteAutorizacion", AMBIENTE_LABELS.getOrDefault(ambiente, ambiente));
    params.put("direccionMatriz", infoTrib.getDirMatriz());
    params.put("direccionEstablecimiento", info.getDirEstablecimiento());
    params.put("fechaEmision", info.getFechaEmision());
    params.put("tipoEmision", TIPO_EMISION_LABELS.getOrDefault(
        infoTrib.getTipoEmision().name(), infoTrib.getTipoEmision().name()));
    params.put("razonSocialComprador", info.getRazonSocialComprador());
    params.put("identificacionComprador", info.getIdentificacionComprador());
    params.put("direccionComprador", info.getDireccionComprador() != null
        ? info.getDireccionComprador() : "");
    params.put("obligadoContabilidad", info.getObligadoContabilidad() != null
        ? info.getObligadoContabilidad().name() : "");

    // NUEVOS PARÁMETROS ESPECÍFICOS DE NOTA DE CRÉDITO
    // Comprobante que se modifica: "FACTURA 001-002-031837657"
    String codDoc = info.getCodDocModificado();
    String tipoDoc = mapCodDocToTipo(codDoc); // "01" -> "FACTURA"
    String numDoc = info.getNumDocModificado(); // "001-002-031837657"
    params.put("comprobanteModificado", tipoDoc + " " + numDoc);

    // Fecha de emisión del comprobante modificado: "05/08/2024"
    params.put("fechaEmisionComprobanteModificado",
               info.getFechaEmisionDocSustento() != null
                   ? info.getFechaEmisionDocSustento()
                   : "");

    // Razón de modificación: "REVERSO DETALLE : ASEGURADOID: 2471981988068"
    params.put("razonModificacion",
               info.getMotivo() != null
                   ? info.getMotivo()
                   : "");

    // ... otros parámetros (totales, logo, etc.) ...

    return params;
  }

  private String mapCodDocToTipo(String codDoc) {
    if ("01".equals(codDoc)) return "FACTURA";
    if ("04".equals(codDoc)) return "NOTA DE CRÉDITO";
    if ("05".equals(codDoc)) return "NOTA DE DÉBITO";
    return "COMPROBANTE";
  }

  private JRBeanCollectionDataSource buildCreditNoteDetailsDataSource(NotaCredito notaCredito) {
    List<DetailRow> detalles = Optional.ofNullable(notaCredito.getDetalles())
        .orElse(Collections.emptyList())
        .stream()
        .map(this::toDetailRow)
        .toList();
    return new JRBeanCollectionDataSource(detalles);
  }

  // ... otros métodos auxiliares ...
}
```

## Resumen de Cambios

### Template JRXML

1. ✅ Agregar 3 nuevos parámetros
2. ✅ Agregar 3 nuevos campos debajo de "Fecha de Emisión:"
3. ✅ Ajustar alturas y posiciones de frames y bands

### Código Java

1. ✅ Agregar métodos para extraer información del comprobante modificado
2. ✅ Agregar parámetros al Map en `buildInvoiceParameters()`
3. ⚠️ Opcional: Modificar `CreditNotePdfServiceImpl` para usar `credit_note_template.jrxml` específicamente

## Notas Importantes

1. **Ubicación de la información**: La información del comprobante modificado puede estar en:

   - El campo `infoAdicional` del XML
   - Campos específicos del modelo `NotaCredito` (si existe)
   - El XML de autorización del SRI

2. **Formato de fecha**: Asegurarse de que la fecha se formatee como "dd/MM/yyyy" para coincidir con el formato mostrado en la imagen.

3. **Template actual**: Actualmente `CreditNotePdfServiceImpl` reutiliza `InvoicePdfService` que usa `invoice_template.jrxml`. Si se quiere usar `credit_note_template.jrxml`, se debe modificar el servicio.

4. **Validación**: Los campos deben ser opcionales (usar `!= null` checks) para que no rompan si la información no está disponible.
