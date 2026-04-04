# 📋 Propuesta Completa: Implementación de Guías de Remisión

**Fecha:** Propuesta completa  
**Feature:** Guías de Remisión (Guide/GuiaRemision)  
**Código SRI:** 06  
**Estado:** ⏳ Pendiente de implementación

---

## 🎯 Resumen Ejecutivo

Esta propuesta incluye todos los componentes necesarios para implementar guías de remisión:
- ✅ Base de datos (tablas, trigger de suscripciones)
- ✅ Parámetros de configuración (ruta de almacenamiento, plantilla de email)
- ✅ Plantilla Jasper para PDF RIDE
- ✅ Servicios de notificación, email y PDF
- ✅ Integración con el sistema existente

---

## 📦 Fase 1: Base de Datos - Migración Principal

### 1.1 Migración: Crear tablas y activar trigger

**Archivo:** `V{timestamp}__create_guide_tables_and_trigger.sql`

**Contenido:**

```sql
-- =====================================================
-- GUÍAS DE REMISIÓN - Tablas y Trigger
-- =====================================================

-- 1. Crear secuencias
CREATE SEQUENCE IF NOT EXISTS guide_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS guide_detail_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS guide_document_reference_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS carrier_seq START WITH 1 INCREMENT BY 1;

-- 2. Crear tabla carrier (transportistas)
CREATE TABLE carrier (
    id_carrier BIGINT NOT NULL PRIMARY KEY,
    id_company BIGINT NOT NULL,
    identification_type VARCHAR(2) NOT NULL,
    identification VARCHAR(13) NOT NULL,
    name VARCHAR(300) NOT NULL,
    address VARCHAR(300),
    phone VARCHAR(20),
    email VARCHAR(100),
    license_number VARCHAR(50),
    vehicle_type VARCHAR(50),
    status BOOLEAN DEFAULT TRUE,
    creation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_carrier_company_identification 
        UNIQUE (id_company, identification)
);

-- 3. Crear tabla guide (guías de remisión)
CREATE TABLE guide (
    id_guide BIGINT NOT NULL PRIMARY KEY,
    -- Campos comunes
    id_company BIGINT NOT NULL,
    id_client BIGINT,
    id_establishment BIGINT,
    id_issuance_point BIGINT,
    sequential BIGINT,
    retry_count INTEGER DEFAULT 0,
    issue_date TIMESTAMP NOT NULL,
    guide_number VARCHAR(17),
    environment VARCHAR(10),
    additional_information TEXT,
    email_sent BOOLEAN DEFAULT FALSE,
    email_sent_date TIMESTAMP,
    id_authorization_sri BIGINT,
    -- Campos específicos de guía
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    transport_type VARCHAR(50),
    transport_plate VARCHAR(20),
    id_carrier BIGINT NOT NULL,
    origin_address VARCHAR(300) NOT NULL,
    destination_address VARCHAR(300) NOT NULL,
    reason_for_transfer VARCHAR(300),
    route VARCHAR(300),
    -- Campos BaseEntity
    status BOOLEAN DEFAULT TRUE,
    creation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT guide_authorization_sri_id_authorization_sri_fk 
        FOREIGN KEY (id_authorization_sri) REFERENCES authorization_sri(id_authorization_sri),
    CONSTRAINT guide_carrier_fk 
        FOREIGN KEY (id_carrier) REFERENCES carrier(id_carrier)
);

-- 4. Crear tabla guide_detail
CREATE TABLE guide_detail (
    id_guide_detail BIGINT NOT NULL PRIMARY KEY,
    id_guide BIGINT NOT NULL,
    id_product BIGINT,
    description VARCHAR(300) NOT NULL,
    quantity NUMERIC(19, 2) NOT NULL,
    unit VARCHAR(10),
    weight NUMERIC(19, 2),
    additional_info TEXT,
    status BOOLEAN DEFAULT TRUE,
    creation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_guide_detail_guide 
        FOREIGN KEY (id_guide) REFERENCES guide(id_guide) ON DELETE CASCADE
);

-- 5. Crear tabla guide_document_reference
CREATE TABLE guide_document_reference (
    id_guide_document_reference BIGINT NOT NULL PRIMARY KEY,
    id_guide BIGINT NOT NULL,
    document_type VARCHAR(2) NOT NULL,
    document_number VARCHAR(17) NOT NULL,
    access_key VARCHAR(49),
    issue_date TIMESTAMP,
    status BOOLEAN DEFAULT TRUE,
    creation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_guide_document_reference_guide 
        FOREIGN KEY (id_guide) REFERENCES guide(id_guide) ON DELETE CASCADE
);

-- 6. Crear índices
CREATE INDEX idx_guide_company ON guide(id_company);
CREATE INDEX idx_guide_client ON guide(id_client);
CREATE INDEX idx_guide_establishment ON guide(id_establishment);
CREATE INDEX idx_guide_issuance_point ON guide(id_issuance_point, sequential);
CREATE INDEX idx_guide_issue_date ON guide(issue_date);
CREATE INDEX idx_guide_number ON guide(guide_number);
CREATE INDEX idx_guide_authorization ON guide(id_authorization_sri);
CREATE INDEX idx_guide_carrier ON guide(id_carrier);
CREATE INDEX idx_guide_detail_guide ON guide_detail(id_guide);
CREATE INDEX idx_guide_document_reference_guide ON guide_document_reference(id_guide);
CREATE INDEX idx_guide_document_reference_type ON guide_document_reference(document_type, document_number);
CREATE INDEX idx_carrier_company ON carrier(id_company);
CREATE INDEX idx_carrier_identification ON carrier(identification);

-- 7. Activar trigger de suscripciones para guide
DROP TRIGGER IF EXISTS trigger_guide_usage ON guide;

CREATE TRIGGER trigger_guide_usage
AFTER INSERT ON guide
FOR EACH ROW
WHEN (NEW.id_authorization_sri IS NOT NULL)
EXECUTE FUNCTION increment_document_usage();

COMMENT ON TRIGGER trigger_guide_usage ON guide IS 
'Incrementa contadores de suscripción cuando se autoriza una guía de remisión';

-- 8. Comentarios
COMMENT ON TABLE guide IS 'Tabla de guías de remisión';
COMMENT ON TABLE guide_detail IS 'Tabla de detalles/productos a transportar en la guía de remisión';
COMMENT ON TABLE guide_document_reference IS 'Tabla de documentos relacionados con la guía (facturas, notas de crédito, etc.)';
COMMENT ON TABLE carrier IS 'Catálogo de transportistas';
```

---

## 📝 Fase 2: Parámetros de Configuración

### 2.1 Migración: Parámetro de ruta de almacenamiento

**Archivo:** `V{timestamp}__add_guide_storage_path_param.sql`

**Contenido:**

```sql
-- Agregar parámetro para la ruta de almacenamiento de guías de remisión
INSERT INTO params (
    id_param,
    code_param,
    value_param,
    description_param,
    status,
    creation_date
)
VALUES (
    (
        SELECT COALESCE(MAX(id_param), 0) + 1
        FROM params
    ),
    'GUIDE_STORAGE_BASE_DIR',
    '/opt/ebilling/guides',
    'Ruta base donde se almacenan los archivos PDF y XML de las guías de remisión',
    TRUE,
    CURRENT_TIMESTAMP
) ON CONFLICT (code_param) DO NOTHING;
```

---

### 2.2 Migración: Plantilla de email

**Archivo:** `V{timestamp}__add_guide_email_template.sql`

**Contenido:**

```sql
-- Agregar parámetro para la plantilla de email de guías de remisión
INSERT INTO params (
    id_param,
    code_param,
    value_param,
    description_param,
    status,
    creation_date
)
VALUES (
    (
        SELECT COALESCE(MAX(id_param), 0) + 1
        FROM params
    ),
    'GUIDE_EMAIL_TEMPLATE',
    $$<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html lang="es" xmlns="http://www.w3.org/1999/xhtml">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width" />
  <title>Guía de remisión electrónica autorizada</title>
  <style>
    body {
      background-color: #f6f6f6;
      font-family: Arial, sans-serif;
      font-size: 14px;
      line-height: 1.6;
      margin: 0;
      padding: 0;
      color: #343a40;
    }
    .body {
      background-color: #f0f3f7;
      width: 100%;
    }
    table {
      border-collapse: separate;
      width: 100%;
    }
    table td {
      font-family: inherit;
      font-size: inherit;
      vertical-align: top;
    }
    .container {
      display: block;
      margin: 0 auto !important;
      max-width: 600px;
      padding: 16px;
      width: 600px;
    }
    .content {
      box-sizing: border-box;
      display: block;
      margin: 0 auto;
      max-width: 600px;
      padding: 0;
    }
    .main {
      background: #ffffff;
      border-radius: 12px;
      border-top: 4px solid #003a75;
      box-shadow: 0 6px 18px rgba(0, 0, 0, 0.08);
      overflow: hidden;
    }
    .wrapper {
      box-sizing: border-box;
      padding: 32px 32px 24px 32px;
    }
    .header-brand {
      background: linear-gradient(135deg, #003a75 0%, #00508a 100%);
      padding: 24px 32px;
      text-align: center;
      color: #ffffff;
    }
    .header-brand h1 {
      margin: 0;
      font-size: 20px;
      font-weight: 600;
      letter-spacing: 0.4px;
      text-transform: uppercase;
      color: #ffffff;
    }
    .header-brand p {
      margin: 12px 0 0;
      font-size: 15px;
      color: #ffffff;
      font-weight: 500;
      opacity: 1;
    }
    .info-icon {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 42px;
      height: 42px;
      border-radius: 50%;
      background: #e8f1fa;
      color: #003a75;
      margin-bottom: 12px;
      font-size: 20px;
    }
    h2 {
      font-size: 18px;
      font-weight: 600;
      color: #003a75;
      margin: 16px 0 12px;
    }
    p {
      margin: 0 0 16px 0;
      color: #495057;
    }
    p strong {
      color: #003a75;
    }
    .details {
      background: #f8fbff;
      border: 1px solid #d8e3f2;
      border-radius: 10px;
      padding: 16px 20px;
      margin-bottom: 20px;
    }
    .details p {
      margin: 8px 0;
    }
    .badge {
      display: inline-block;
      padding: 6px 12px;
      border-radius: 999px;
      background: #e8f1fa;
      color: #003a75;
      font-weight: 600;
      font-size: 12px;
      letter-spacing: 0.3px;
    }
    ul {
      padding-left: 20px;
      color: #495057;
      margin: 0 0 16px 0;
    }
    ul li {
      margin-bottom: 8px;
    }
    .footer {
      text-align: center;
      width: 100%;
      margin-top: 16px;
    }
    .footer td {
      color: #6c757d;
      font-size: 12px;
    }
    .footer a {
      color: #003a75;
      text-decoration: none;
    }
    .footer-bar {
      background: #003a75;
      color: #ffffff;
      padding: 12px;
      font-size: 12px;
      text-align: center;
    }
    @media only screen and (max-width: 620px) {
      .container,
      .content {
        width: 100% !important;
        padding: 0 12px !important;
      }
      .wrapper {
        padding: 24px 20px 20px 20px !important;
      }
      h2 {
        font-size: 17px !important;
      }
      p {
        font-size: 15px !important;
      }
    }
  </style>
</head>
<body>
  <table role="presentation" border="0" cellpadding="0" cellspacing="0" class="body">
    <tr>
      <td></td>
      <td class="container">
        <div class="content">
          <table role="presentation" class="main">
            <tr>
              <td class="header-brand">
                <h1>{{companyName}}</h1>
                <p>Guía de remisión electrónica autorizada</p>
              </td>
            </tr>
            <tr>
              <td class="wrapper">
                <div class="info-icon">🚚</div>
                <h2>Estimado/a <strong>{{buyerName}}</strong>,</h2>
                <p>
                  Adjuntamos su guía de remisión electrónica número <strong>{{guideNumber}}</strong>,
                  autorizada por el SRI.
                </p>
                <div class="details">
                  <p><strong>Fecha de autorización:</strong> {{authorizationDate}}</p>
                  <p><strong>Número de autorización:</strong> {{authorizationNumber}}</p>
                  <p><strong>Clave de acceso:</strong> {{accessKey}}</p>
                  <p><strong>Fecha de emisión:</strong> {{issueDate}}</p>
                  <p><strong>Fecha inicio traslado:</strong> {{startDate}}</p>
                  <p><strong>Fecha fin traslado:</strong> {{endDate}}</p>
                  <p><strong>Transportista:</strong> {{carrierName}}</p>
                  <p><strong>Placa:</strong> {{transportPlate}}</p>
                  <p><strong>Origen:</strong> {{originAddress}}</p>
                  <p><strong>Destino:</strong> {{destinationAddress}}</p>
                  <p><strong>Motivo traslado:</strong> {{reasonForTransfer}}</p>
                </div>
                <p>Le enviamos dos archivos adjuntos:</p>
                <ul>
                  <li>Guía de remisión en formato PDF (RIDE).</li>
                  <li>XML autorizado por el SRI.</li>
                </ul>
                <p>
                  Este documento ya está disponible para su visualización y descarga visitando 
                  <a href="{{consultationUrl}}" style="color: #003a75; text-decoration: underline;">aquí</a>.
                </p>
                <p>
                  Gracias por confiar en nosotros. Si requiere información adicional,
                  por favor contacte a nuestro equipo de soporte.
                </p>
                <p>Saludos cordiales<br /></p>
              </td>
            </tr>
          </table>
          <div class="footer">
            <table role="presentation">
              <tr>
                <td>Quito, Ecuador</td>
              </tr>
              <tr>
                <td>&copy; Wallparisoft. Todos los derechos reservados.</td>
              </tr>
            </table>
          </div>
          <div class="footer-bar">
            Este correo fue enviado automáticamente. Por favor, no respondas a este mensaje.
          </div>
        </div>
      </td>
      <td></td>
    </tr>
  </table>
</body>
</html>$$,
    'Plantilla HTML para el correo de notificación de guía de remisión autorizada',
    TRUE,
    CURRENT_TIMESTAMP
) ON CONFLICT (code_param) DO NOTHING;
```

**Placeholders del template:**
- `{{companyName}}` - Nombre de la empresa
- `{{buyerName}}` - Nombre del destinatario
- `{{guideNumber}}` - Número de guía (formato: estab-ptoEmi-secuencial)
- `{{authorizationDate}}` - Fecha de autorización
- `{{authorizationNumber}}` - Número de autorización
- `{{accessKey}}` - Clave de acceso
- `{{issueDate}}` - Fecha de emisión
- `{{startDate}}` - Fecha inicio de traslado
- `{{endDate}}` - Fecha fin de traslado
- `{{carrierName}}` - Nombre del transportista
- `{{transportPlate}}` - Placa del vehículo
- `{{originAddress}}` - Dirección de origen
- `{{destinationAddress}}` - Dirección de destino
- `{{reasonForTransfer}}` - Motivo del traslado
- `{{consultationUrl}}` - URL de consulta (desde parámetro CONSULTATION_URL)

---

## 📄 Fase 3: Plantilla Jasper para PDF RIDE

### 3.1 Crear plantilla Jasper

**Archivo:** `src/main/resources/jasper/guide_template.jrxml`

**Características:**
- ✅ Basada en la estructura del RIDE de guía de remisión (ver imagen de referencia)
- ✅ Similar a `invoice_template.jrxml` y `debit_note_template.jrxml`
- ✅ Incluye todas las secciones del RIDE:
  - Información del transportista
  - Información del comprobante de venta (si aplica)
  - Motivo de traslado y destinatario
  - Tabla de productos/items a transportar
  - Información adicional

**Secciones principales:**
1. **Header:** Información tributaria (razón social, RUC, dirección)
2. **Transportista:** Identificación, nombre, placa, fechas
3. **Comprobante de Venta:** Número, autorización, fecha (si aplica)
4. **Destinatario:** Identificación, nombre, dirección destino
5. **Detalles:** Tabla con productos (cantidad, descripción, código, etc.)
6. **Footer:** Información de autorización, clave de acceso, código de barras

**Parámetros del reporte:**
- `razonSocial`, `ruc`, `direccionMatriz`
- `numeroAutorizacion`, `fechaAutorizacion`, `claveAcceso`
- `guideNumber`, `issueDate`
- `startDate`, `endDate`
- `carrierIdentification`, `carrierName`, `transportPlate`
- `originAddress`, `destinationAddress`, `reasonForTransfer`
- `route` (opcional)
- `detailsDataSource` (JRBeanCollectionDataSource con los productos)

---

## 🔧 Fase 4: Servicios Java

### 4.1 Actualizar `ParamsService`

**Archivo:** `src/main/java/com/wallparisoft/ebillbilling/service/ParamsService.java`

**Agregar método:**
```java
/**
 * Obtiene el directorio base donde se almacenan los archivos relacionados a las guías de remisión
 * (XML y PDF).
 *
 * <p>Se obtiene desde el parámetro {@code GUIDE_STORAGE_BASE_DIR} en la tabla params. Si no
 * existe en BD, se usa un valor por defecto.
 *
 * @return Ruta del directorio base de almacenamiento para guías de remisión
 */
String getGuideBaseDir();
```

---

### 4.2 Actualizar `ParamsServiceImpl`

**Archivo:** `src/main/java/com/wallparisoft/ebillbilling/service/impl/ParamsServiceImpl.java`

**Agregar:**
```java
private static final String DEFAULT_GUIDE_BASE_DIR = "/opt/ebilling/guides";
private static final String PARAM_GUIDE_STORAGE_BASE_DIR = "GUIDE_STORAGE_BASE_DIR";

@Override
public String getGuideBaseDir() {
  String baseDir = getParamValueOrDefault(PARAM_GUIDE_STORAGE_BASE_DIR, DEFAULT_GUIDE_BASE_DIR);
  if (baseDir == null || baseDir.isBlank()) {
    log.warn(
        "Parámetro {} no configurado en BD, usando valor por defecto: {}",
        PARAM_GUIDE_STORAGE_BASE_DIR,
        DEFAULT_GUIDE_BASE_DIR);
    return DEFAULT_GUIDE_BASE_DIR;
  }
  return baseDir;
}
```

---

### 4.3 Crear `GuidePdfService`

**Archivo:** `src/main/java/com/wallparisoft/ebillbilling/service/GuidePdfService.java`

**Interfaz:**
```java
public interface GuidePdfService {
  GuidePdfData readGuideData(String authorizationXml, String logoPath);
  GuidePdfData readGuideData(String authorizationXml, String logoPath, String regime, String specialTaxpayerResolution);
  Path generateAndSaveGuidePdf(String authorizationXml, Path outputDirectory);
  Path generateAndSaveGuidePdf(String authorizationXml, Path outputDirectory, String logoPath);
  Path generateAndSaveGuidePdf(String authorizationXml, Path outputDirectory, String logoPath, String regime, String specialTaxpayerResolution);
}
```

**Implementación:** `src/main/java/com/wallparisoft/ebillbilling/service/impl/GuidePdfServiceImpl.java`

**Características:**
- ✅ Similar a `InvoicePdfServiceImpl` y `DebitNotePdfServiceImpl`
- ✅ Lee XML de autorización del SRI
- ✅ Genera PDF usando `guide_template.jrxml`
- ✅ Guarda PDF en el directorio especificado
- ✅ Incluye código de barras de la clave de acceso

---

### 4.4 Crear `GuideEmailService`

**Archivo:** `src/main/java/com/wallparisoft/ebillbilling/service/GuideEmailService.java`

**Interfaz:**
```java
public interface GuideEmailService {
  Integer sendAuthorizedGuide(
      String recipientEmail,
      Guide guide,
      GuideDto guideDto,
      AuthorizationSri authorizationSri,
      byte[] pdfBytes,
      byte[] authorizationXmlBytes);
}
```

**Implementación:** `src/main/java/com/wallparisoft/ebillbilling/service/impl/GuideEmailServiceImpl.java`

**Características:**
- ✅ Similar a `InvoiceEmailServiceImpl` y `DebitNoteEmailServiceImpl`
- ✅ Carga plantilla desde parámetro `GUIDE_EMAIL_TEMPLATE`
- ✅ Reemplaza placeholders con valores reales
- ✅ Envía email con PDF y XML adjuntos
- ✅ Retorna código de resultado (1 = éxito, 0 = error)

---

### 4.5 Crear `GuideDeliveryService`

**Archivo:** `src/main/java/com/wallparisoft/ebillbilling/service/GuideDeliveryService.java`

**Interfaz:**
```java
public interface GuideDeliveryService {
  void deliverAuthorizedGuide(
      GuideDto guideDto,
      Guide savedGuide,
      AuthorizationSri authorizationSri);
}
```

**Implementación:** `src/main/java/com/wallparisoft/ebillbilling/service/impl/GuideDeliveryServiceImpl.java`

**Características:**
- ✅ Similar a `InvoiceDeliveryServiceImpl` y `DebitNoteDeliveryServiceImpl`
- ✅ Genera PDF usando `GuidePdfService`
- ✅ Envía email usando `GuideEmailService` (si hay correo del destinatario)
- ✅ Marca `emailSent = true` y `emailSentDate` si el email se envió exitosamente
- ✅ Guarda archivos en `/opt/ebilling/guides/{companyId}/{year}/{month}/`

---

### 4.6 Actualizar `GuideServiceImpl`

**Archivo:** `src/main/java/com/wallparisoft/ebillbilling/service/impl/GuideServiceImpl.java`

**Agregar método de notificación:**
```java
private void notifyAuthorizedGuide(
    GuideDto guideDto,
    Guide savedGuide,
    AuthorizationSri authorizationSri) {

  // Verificar si el correo ya fue enviado
  if (Boolean.TRUE.equals(savedGuide.getEmailSent())) {
    log.info(
        "Email already sent for guide {} on {}. Skipping notification.",
        savedGuide.getIdGuide(),
        savedGuide.getEmailSentDate());
    return;
  }

  // Validar autorización
  if (Objects.isNull(authorizationSri)
      || !StringUtils.hasText(authorizationSri.getAuthorizationStatusSri())
      || !"AUTORIZADO".equalsIgnoreCase(authorizationSri.getAuthorizationStatusSri())) {
    return;
  }

  // Validar ruta de almacenamiento
  if (!StringUtils.hasText(authorizationSri.getAuthorizationStoragePath())) {
    log.warn(
        "Authorization XML path is missing for guide {}. Skipping email notification.",
        savedGuide.getIdGuide());
    return;
  }

  // Obtener ClientDto completo con contactos
  ClientDto clientDtoWithContacts = null;
  try {
    if (guideDto.getClientDto() != null && guideDto.getClientDto().getIdClient() != null) {
      clientDtoWithContacts =
          customerClient
              .getClientById(guideDto.getClientDto().getIdClient())
              .getClientDtos()
              .get(0);
      guideDto.setClientDto(clientDtoWithContacts);
    }
  } catch (Exception e) {
    log.warn("Error loading client for guide notification: {}", e.getMessage());
  }

  // Entregar guía (generar PDF y enviar email)
  guideDeliveryService.deliverAuthorizedGuide(guideDto, savedGuide, authorizationSri);

  // Verificar si se envió email
  String recipientEmail = getRecipientEmail(guideDto, clientDtoWithContacts);
  if (!StringUtils.hasText(recipientEmail)) {
    log.info(
        "No recipient email found for guide {}. PDF and XML generated, but email not sent. Marking emailSent as false.",
        savedGuide.getIdGuide());
    savedGuide.setEmailSent(false);
    repo.save(savedGuide);
  }
}

private String getRecipientEmail(GuideDto guideDto, ClientDto clientDto) {
  // Buscar email en contactos del cliente (tipo EMA)
  if (clientDto != null && clientDto.getContacts() != null) {
    return clientDto.getContacts().stream()
        .filter(c -> "EMA".equalsIgnoreCase(c.getContactType()))
        .map(ContactDto::getValue)
        .filter(StringUtils::hasText)
        .findFirst()
        .orElse(null);
  }
  return null;
}
```

**Llamar desde método `authorize()`:**
```java
// Después de autorizar y guardar
if (authorizationSri != null
    && "AUTORIZADO".equalsIgnoreCase(authorizationSri.getAuthorizationStatusSri())) {
  try {
    notifyAuthorizedGuide(guideDto, savedGuide, authorizationSri);
  } catch (Exception e) {
    log.warn("Error notificando guía autorizada: {}", e.getMessage());
  }
}
```

---

## 🔄 Fase 5: Actualizar `SendGuideSri` (Generación XML)

### 5.1 Actualizar método `resolveStoragePath`

**Archivo:** `src/main/java/com/wallparisoft/ebillbilling/service/impl/SendGuideSri.java`

**Cambiar:**
```java
private String resolveStoragePath(Path xmlPath) {
  // Usar ParamsService para obtener ruta base de guías
  String baseDir = paramsService.getGuideBaseDir();
  
  // Estructura: /opt/ebilling/guides/{companyId}/{year}/{month}/
  // Similar a como se hace en SendInvoiceImpl y SendDebitNoteImpl
  
  // Extraer companyId, year, month del path o del guideDto
  // Construir path completo
  // Retornar como string
}
```

---

## 📊 Resumen de Archivos a Crear/Modificar

### Migraciones SQL (3 archivos)
1. ✅ `V{timestamp}__create_guide_tables_and_trigger.sql` - Tablas y trigger
2. ✅ `V{timestamp}__add_guide_storage_path_param.sql` - Parámetro de ruta
3. ✅ `V{timestamp}__add_guide_email_template.sql` - Plantilla de email

### Plantillas (1 archivo)
1. ✅ `src/main/resources/jasper/guide_template.jrxml` - Plantilla PDF RIDE

### Servicios Java (5 archivos nuevos + 2 modificaciones)
1. ✅ `GuidePdfService.java` + `GuidePdfServiceImpl.java` - Generación PDF
2. ✅ `GuideEmailService.java` + `GuideEmailServiceImpl.java` - Envío de emails
3. ✅ `GuideDeliveryService.java` + `GuideDeliveryServiceImpl.java` - Entrega completa
4. ✅ Modificar `ParamsService.java` - Agregar `getGuideBaseDir()`
5. ✅ Modificar `ParamsServiceImpl.java` - Implementar `getGuideBaseDir()`
6. ✅ Modificar `GuideServiceImpl.java` - Agregar `notifyAuthorizedGuide()`
7. ✅ Modificar `SendGuideSri.java` - Usar `getGuideBaseDir()`

---

## ✅ Checklist de Implementación

### Base de Datos
- [ ] Migración de tablas creada y probada
- [ ] Trigger de suscripciones activo
- [ ] Parámetro `GUIDE_STORAGE_BASE_DIR` creado
- [ ] Parámetro `GUIDE_EMAIL_TEMPLATE` creado

### Plantillas
- [ ] Plantilla Jasper `guide_template.jrxml` creada
- [ ] Plantilla probada con datos de ejemplo
- [ ] PDF generado correctamente con todas las secciones

### Servicios
- [ ] `ParamsService.getGuideBaseDir()` implementado
- [ ] `GuidePdfService` implementado y probado
- [ ] `GuideEmailService` implementado y probado
- [ ] `GuideDeliveryService` implementado y probado
- [ ] `GuideServiceImpl.notifyAuthorizedGuide()` implementado
- [ ] `SendGuideSri.resolveStoragePath()` actualizado

### Integración
- [ ] Notificación se envía cuando guía es autorizada
- [ ] PDF se genera correctamente
- [ ] Email se envía con PDF y XML adjuntos
- [ ] Archivos se guardan en `/opt/ebilling/guides/`
- [ ] `emailSent` y `emailSentDate` se actualizan correctamente

---

## 🎯 Flujo Completo de Notificación

```
1. Guía autorizada por SRI
   ↓
2. GuideServiceImpl.authorize() guarda guide con id_authorization_sri
   ↓
3. notifyAuthorizedGuide() es llamado
   ↓
4. GuideDeliveryService.deliverAuthorizedGuide()
   ├─ Genera PDF usando GuidePdfService
   ├─ Lee XML de autorización
   └─ Si hay email del destinatario:
      ├─ GuideEmailService.sendAuthorizedGuide()
      │  ├─ Carga plantilla GUIDE_EMAIL_TEMPLATE
      │  ├─ Reemplaza placeholders
      │  └─ Envía email con PDF y XML adjuntos
      └─ Marca emailSent = true, emailSentDate = now()
   ↓
5. Archivos guardados en /opt/ebilling/guides/{companyId}/{year}/{month}/
```

---

**Estado:** ⏳ Listo para implementación  
**Próximo paso:** Crear migraciones SQL y comenzar implementación de servicios




