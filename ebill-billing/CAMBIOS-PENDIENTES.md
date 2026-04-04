# 📋 Lista de Cambios Necesarios - ebill-billing

**Fecha:** 11 de Noviembre 2025  
**Estado:** En progreso

---

## ✅ CAMBIOS COMPLETADOS

### 1️⃣ Fix Crítico: SendInvoiceImpl usa GenerateInvoiceSri correctamente

- **Problema:** `new GenerateInvoiceSri()` no inyectaba `SriConfiguration` → NullPointerException
- **Solución:**
  - Usar `this.createInvoiceSri()` en lugar de crear nueva instancia
  - Agregar constructor explícito que pasa `SriConfiguration` al padre
- **Archivos modificados:** `SendInvoiceImpl.java`
- **Estado:** ✅ COMPLETADO

### 2️⃣ Fix: Tipo de Identificación del Comprador

- **Estado:** ✅ COMPLETADO
- **Documentación:** `FIX-TIPO-IDENTIFICACION-COMPRADOR.md`

### 3️⃣ Fix: Sincronización con ebill-customer (PATCH → PUT)

- **Estado:** ✅ COMPLETADO

### 4️⃣ Refactorización: Enums y Constantes

- **Estado:** ✅ COMPLETADO
- **Documentación:** `REFACTOR-ENUM-TIPO-COMPROBANTE.md`, `REFACTOR-ELIMINACION-HARDCODED-VALUES.md`

### 5️⃣ Seguridad: Ambiente SRI en Configuración

- **Estado:** ✅ COMPLETADO
- **Documentación:** `MEJORA-SEGURIDAD-AMBIENTE-SRI.md`

---

## 🔴 CAMBIOS PENDIENTES (Críticos)

### 1️⃣ CRÍTICO: Eliminar campo `environment` de `InvoiceDto`

**Problema:** El campo `environment` en `InvoiceDto` ya NO se usa (el ambiente se lee de `application.properties`), pero sigue presente en el DTO.

**Opciones:**

#### Opción A: Deprecar (Recomendado)

```java
@Deprecated
@JsonIgnore
private String environment; // ⚠️ OBSOLETO - El ambiente se lee de application.properties
```

#### Opción B: Eliminar (Breaking Change)

- Eliminar completamente el campo
- Requiere actualizar el frontend

**Decisión:** ¿Qué prefieres?

---

### 2️⃣ IMPORTANTE: Rutas de archivos hardcodeadas (Métodos de testing)

**Problema:** En `GenerateInvoiceSri.java`:

```java
// Línea 60
XmlUtil.validarQueUnXmlCumpleConXSD(is, "src/test/resources/xsd/Factura_V_2_1_0.xsd");

// Línea 73
new FirmaXadesBesUtil("src/test/resources/p12/yaja.p12", ...);

// Línea 82
Files.readAllLines(Paths.get("src/test/resources/p12/password.txt"), ...);
```

**Impacto:**

- ❌ Estos métodos (`deberiaValidarFacturaFirmadaConEsquema`, `crearFacturaFirmada`) son para TESTING
- ❌ **NO deberían estar en código de producción**
- ❌ Las rutas `src/test/resources/` no existen en el JAR compilado

**Solución Recomendada:**

1. Mover estos métodos a una clase de test separada
2. O configurar rutas en `application.properties`:

```properties
sri.xsd.path=file:/path/to/xsd/Factura_V_2_1_0.xsd
sri.p12.path=file:/path/to/p12/certificado.p12
sri.p12.password.path=file:/path/to/p12/password.txt
```

**Pregunta:** ¿Estos métodos se usan en producción o solo en tests?

---

### 3️⃣ MEDIA: Formato de fecha incorrecto

**Problema:** En `GenerateInvoiceSri.java` línea 167:

```java
new SimpleDateFormat("dd/MM/YYYY") // ❌ YYYY es incorrecto
```

**Fix:**

```java
new SimpleDateFormat("dd/MM/yyyy") // ✅ yyyy para año de 4 dígitos
```

**Impacto:**

- `YYYY` = Año de la semana (ISO 8601)
- `yyyy` = Año del calendario

**Ejemplo del bug:**

- Fecha real: 31/12/2024
- Con `YYYY`: `31/12/2025` (porque esa semana pertenece a 2025)
- Con `yyyy`: `31/12/2024` ✅

**¿Aplicar fix?**

---

### 4️⃣ MEDIA: Constantes para valores por defecto

**Valores hardcodeados pendientes:**

```java
// Línea 80 - Versión XML
factura.setVersion("2.1.0"); // ❌

// Línea 81 - ID del comprobante
factura.setId("comprobante"); // ❌

// Línea 104 - Código auxiliar
detalle.setCodigoAuxiliar("0"); // ❌

// Línea 173, 367 - Dirección por defecto
"S/N" // ❌

// Línea 175 - Obligado a llevar contabilidad
infoFactura.setObligadoContabilidad(ObligadoContabilidadEnum.NO); // ❌ Debería ser configurable

// Línea 249 - Moneda
infoFactura.setMoneda(MonedaEnum.DOLAR); // ❌ Aunque en Ecuador siempre es USD

// Línea 257 - Forma de pago por defecto
pago.setFormaPago(FormaPagoEnum.SIN_UTILIZACION_DEL_SISTEMA_FINANCIERO); // ❌

// Línea 405 - Código numérico clave de acceso
"12345678" // ❌ Magic number

// Línea 406 - Longitud clave de acceso
if (accessKey.length() == 49) // ❌ Magic number
```

**Solución:** Crear `SriConstants.java`:

```java
public final class SriConstants {
  // Versión del esquema XML del SRI
  public static final String FACTURA_VERSION = "2.1.0";

  // ID del comprobante según esquema XSD
  public static final String COMPROBANTE_ID = "comprobante";

  // Código auxiliar por defecto
  public static final String DEFAULT_AUXILIARY_CODE = "0";

  // Dirección por defecto
  public static final String DEFAULT_ADDRESS = "S/N";

  // Longitud esperada de la clave de acceso
  public static final int ACCESS_KEY_LENGTH = 49;

  // Código numérico para generación de clave de acceso
  public static final String ACCESS_KEY_NUMERIC_CODE = "12345678";
}
```

**¿Implementar?**

---

### 5️⃣ BAJA: Configurar "Obligado a llevar contabilidad" por empresa

**Problema:** Está hardcodeado a `NO` para todas las empresas.

**Solución:** Agregar campo en la tabla `company`:

```sql
ALTER TABLE company ADD COLUMN forced_to_accounting BOOLEAN DEFAULT false;
```

Y leer desde `CompanyDto`:

```java
infoFactura.setObligadoContabilidad(
    invoiceDto.getCompanyDto().getForcedToAccounting()
        ? ObligadoContabilidadEnum.SI
        : ObligadoContabilidadEnum.NO
);
```

**Impacto:** Mejora para empresas que SÍ están obligadas a llevar contabilidad.

**¿Implementar?**

---

### 6️⃣ BAJA: Logging en lugar de System.out.println

**Problema:** Hay varios `System.out.println()` en el código:

- Líneas 230-242: Logs de totales calculados
- Líneas 332-364: Logs de dirección matriz

**Solución:** Reemplazar por `log.debug()`:

```java
// ANTES
System.out.println("=== TOTALES CALCULADOS ===");

// DESPUÉS
log.debug("=== TOTALES CALCULADOS ===");
```

**Beneficio:** Logs estructurados, configurables por nivel.

**¿Implementar?**

---

## 🟢 MEJORAS OPCIONALES (No críticas)

### 1️⃣ Endpoint para consultar ambiente actual

```java
@GetMapping("/api/invoice/environment")
public ResponseEntity<Map<String, String>> getEnvironment() {
    return ResponseEntity.ok(Map.of(
        "environment", sriConfiguration.getEnvironment(),
        "name", sriConfiguration.getEnvironmentName()
    ));
}
```

**Beneficio:** Frontend puede mostrar badge de ambiente actual.

### 2️⃣ Validación de clave de acceso en bucle infinito

**Problema:** Línea 406-410:

```java
if (accessKey.length() == 49) {
  return accessKey;
} else {
  return generateAccessKey(infoTributaria); // ❌ Recursión infinita si falla
}
```

**Solución:** Agregar contador de intentos:

```java
public String generateAccessKey(InfoTributaria infoTributaria) {
  return generateAccessKeyWithRetry(infoTributaria, 0);
}

private String generateAccessKeyWithRetry(InfoTributaria infoTributaria, int attempt) {
  if (attempt > 10) {
    throw new IllegalStateException("No se pudo generar clave de acceso después de 10 intentos");
  }

  Calendar cal = Calendar.getInstance();
  String accessKey = infoTributaria.generarClaveAcceso(cal.getTime(), TipoComprobanteEnum.FACTURA, "12345678");

  if (accessKey.length() == 49) {
    return accessKey;
  } else {
    return generateAccessKeyWithRetry(infoTributaria, attempt + 1);
  }
}
```

### 3️⃣ Tests unitarios

- Crear tests para `GenerateInvoiceSri`
- Crear tests para `SriConfiguration`
- Crear tests para enums y constantes

---

## 📊 Resumen de Prioridades

| Cambio                             | Prioridad     | Riesgo | Esfuerzo |
| ---------------------------------- | ------------- | ------ | -------- |
| SendInvoiceImpl fix                | 🔴 CRÍTICO    | Alto   | ✅ HECHO |
| Formato de fecha (YYYY → yyyy)     | 🔴 CRÍTICO    | Medio  | 5 min    |
| Rutas de archivos hardcodeadas     | 🟡 IMPORTANTE | Alto   | 1 hora   |
| Deprecar `environment` en DTO      | 🟡 IMPORTANTE | Bajo   | 5 min    |
| Constantes `SriConstants`          | 🟡 MEDIA      | Bajo   | 30 min   |
| Obligado contabilidad configurable | 🟢 BAJA       | Bajo   | 1 hora   |
| System.out → log.debug             | 🟢 BAJA       | Bajo   | 15 min   |
| Endpoint ambiente                  | 🟢 OPCIONAL   | Bajo   | 15 min   |
| Fix recursión infinita             | 🟢 OPCIONAL   | Medio  | 30 min   |

---

## ❓ Decisiones Pendientes

**Por favor confirma qué cambios quieres que implemente:**

1. ¿Fix formato de fecha (YYYY → yyyy)? **[Sí/No]**
2. ¿Deprecar campo `environment` en InvoiceDto? **[Sí/No/Eliminar]**
3. ¿Mover métodos de testing o configurar rutas? **[Mover/Configurar/Dejar]**
4. ¿Crear SriConstants.java? **[Sí/No]**
5. ¿Hacer obligado_contabilidad configurable? **[Sí/No]**
6. ¿Reemplazar System.out por log.debug? **[Sí/No]**

---

**Fin del documento**
