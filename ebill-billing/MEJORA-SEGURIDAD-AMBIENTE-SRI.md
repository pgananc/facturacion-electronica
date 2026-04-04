# 🔐 Mejora de Seguridad: Ambiente SRI en Configuración

**Fecha:** 11 de Noviembre 2025  
**Archivos creados:**

- `SriConfiguration.java`

**Archivos modificados:**

- `application.properties`
- `application-localhost.properties`
- `GenerateInvoiceSri.java`

---

## 🔴 Problema Anterior (CRÍTICO)

El ambiente del SRI (pruebas/producción) venía del **request del usuario**:

```java
// ❌ INSEGURO - El frontend decide el ambiente
EnvironmentCode environmentCode = EnvironmentCode.fromCode(invoiceDto.getEnvironment());
infoTributaria.setAmbiente(
    environmentCode.isPruebas() ? AmbienteEnum.PRUEBAS : AmbienteEnum.PRODUCCION
);
```

### ❌ Riesgos de Seguridad:

1. **Usuario malicioso** podría cambiar el request y enviar facturas a producción
2. **Error del frontend** podría enviar "2" (producción) por accidente
3. **Sin control centralizado** del ambiente
4. **Difícil de auditar** qué ambiente se usó en cada factura

---

## ✅ Solución Implementada

### 1️⃣ Ambiente en `application.properties`

**Archivo:** `src/main/resources/application.properties`

```properties
# SRI Configuration
# Ambiente: 1=PRUEBAS, 2=PRODUCCION
# ⚠️ IMPORTANTE: Cambiar a "2" solo cuando se vaya a producción
sri.environment=1
```

**Archivo:** `src/main/resources/application-localhost.properties`

```properties
# SRI Configuration
# Ambiente: 1=PRUEBAS, 2=PRODUCCION
# ⚠️ En desarrollo siempre usar PRUEBAS
sri.environment=1
```

### 2️⃣ Clase de Configuración `SriConfiguration`

**Archivo:** `src/main/java/com/wallparisoft/ebillbilling/config/SriConfiguration.java`

```java
@Slf4j
@Getter
@Configuration
public class SriConfiguration {

  @Value("${sri.environment:1}")
  private String environment;

  public boolean isPruebas() {
    return "1".equals(environment);
  }

  public boolean isProduccion() {
    return "2".equals(environment);
  }

  public String getEnvironmentName() {
    return isPruebas() ? "PRUEBAS" : "PRODUCCION";
  }

  @EventListener(ApplicationReadyEvent.class)
  public void logEnvironment() {
    if (isProduccion()) {
      log.warn("🚨 ⚠️  SRI CONFIGURADO EN AMBIENTE DE PRODUCCIÓN ⚠️  🚨");
      log.warn("🚨 Las facturas se enviarán al SRI REAL");
    } else {
      log.info("🧪 SRI configurado en ambiente de PRUEBAS");
      log.info("🧪 Las facturas se enviarán al SRI de pruebas");
    }
  }
}
```

### 3️⃣ Uso en `GenerateInvoiceSri`

```java
@Component
@RequiredArgsConstructor
public class GenerateInvoiceSri {

  private final SriConfiguration sriConfiguration;

  public InfoTributaria crearInfoTributaria(InvoiceDto invoiceDto) {
    // ✅ SEGURO - El servidor decide el ambiente
    infoTributaria.setAmbiente(
        sriConfiguration.isPruebas() ? AmbienteEnum.PRUEBAS : AmbienteEnum.PRODUCCION
    );
  }
}
```

---

## 🎯 Beneficios

### ✅ 1. **Seguridad**

| Antes (Request)                   | Después (Configuración)                          |
| --------------------------------- | ------------------------------------------------ |
| ❌ Usuario puede cambiar ambiente | ✅ Solo administrador del servidor puede cambiar |
| ❌ Sin autenticación para cambiar | ✅ Requiere acceso a `application.properties`    |
| ❌ Vulnerable a ataques           | ✅ Protegido por permisos del servidor           |

### ✅ 2. **Control Centralizado**

```bash
# Para cambiar a producción, solo modificar una línea:
sri.environment=2

# Reiniciar la aplicación y LISTO
# Todas las facturas se enviarán a producción
```

### ✅ 3. **Logging Claro**

Al iniciar la aplicación, se muestra claramente el ambiente:

```
🧪 SRI configurado en ambiente de PRUEBAS
🧪 Las facturas se enviarán al SRI de pruebas
```

O en producción:

```
🚨 ⚠️  SRI CONFIGURADO EN AMBIENTE DE PRODUCCIÓN ⚠️  🚨
🚨 Las facturas se enviarán al SRI REAL
```

**Imposible no darse cuenta** si está mal configurado.

### ✅ 4. **Entornos Separados**

```bash
# Desarrollo local (application-localhost.properties)
sri.environment=1  # Siempre pruebas

# Servidor de desarrollo (application.properties)
sri.environment=1  # Pruebas

# Servidor de producción (application.properties)
sri.environment=2  # Producción
```

### ✅ 5. **Auditabilidad**

```bash
# Consultar logs del servidor para ver qué ambiente se usó:
grep "SRI configurado" application.log
```

### ✅ 6. **Prevención de Errores**

- ❌ **Antes:** Frontend podría enviar `environment: "2"` por error
- ✅ **Ahora:** Servidor ignora el request, siempre usa su configuración

---

## 📊 Comparación Antes/Después

### Cambio de Ambiente (Pruebas → Producción)

| Aspecto              | Antes (Request)             | Después (Configuración)            |
| -------------------- | --------------------------- | ---------------------------------- |
| **Quién decide**     | Frontend (usuario)          | Servidor (administrador)           |
| **Cómo cambiar**     | Modificar request HTTP      | Modificar `application.properties` |
| **Acceso requerido** | Ninguno (cualquier usuario) | SSH al servidor                    |
| **Seguridad**        | ⭐ (1/5) - Muy inseguro     | ⭐⭐⭐⭐⭐ (5/5) - Muy seguro      |
| **Auditabilidad**    | ❌ Difícil                  | ✅ Fácil (logs del servidor)       |
| **Centralizado**     | ❌ No (cada request)        | ✅ Sí (un solo lugar)              |
| **Logging**          | ❌ No claro                 | ✅ Muy claro al iniciar app        |

---

## 🔄 Migración

### DTO `InvoiceDto` - Campo `environment` OBSOLETO

El campo `environment` en `InvoiceDto` **ya no se usa** para determinar el ambiente del SRI.

**Opciones:**

1. **Mantener el campo** (por compatibilidad con frontend)

   - El backend lo ignora
   - No causa error si el frontend lo envía

2. **Deprecar el campo**

   ```java
   @Deprecated
   private String environment; // ⚠️ OBSOLETO - El ambiente se lee de application.properties
   ```

3. **Eliminar el campo** (breaking change)
   - Requiere actualizar el frontend

**Recomendación:** Opción 1 (mantener) o 2 (deprecar) para evitar breaking changes.

---

## 🚀 Despliegue

### Paso 1: Configurar `application.properties` en cada servidor

```bash
# Servidor de desarrollo
sri.environment=1

# Servidor de producción
sri.environment=2
```

### Paso 2: Desplegar la nueva versión

```bash
./mvnw clean package
# Desplegar el JAR en el servidor
```

### Paso 3: Verificar logs al iniciar

```bash
tail -f logs/application.log | grep "SRI configurado"
```

Debe aparecer:

- **Desarrollo:** `🧪 SRI configurado en ambiente de PRUEBAS`
- **Producción:** `🚨 ⚠️  SRI CONFIGURADO EN AMBIENTE DE PRODUCCIÓN ⚠️  🚨`

---

## 🎓 Principios de Seguridad Aplicados

1. ✅ **Principle of Least Privilege**

   - Solo administradores del servidor pueden cambiar el ambiente

2. ✅ **Defense in Depth**

   - No confiar en el frontend para decisiones críticas

3. ✅ **Secure by Default**

   - Default a `PRUEBAS` si no está configurado

4. ✅ **Audit Trail**

   - Logs claros del ambiente usado

5. ✅ **Fail-Safe Defaults**
   - Si falla la configuración, usa `PRUEBAS` (nunca `PRODUCCION` por error)

---

## ⚠️ Consideraciones

### Frontend

El frontend **ya no necesita enviar `environment`**, pero si lo envía, no causa error (se ignora).

**Opcional:** El frontend podría consultar el ambiente actual vía un endpoint GET:

```java
@GetMapping("/api/invoice/environment")
public ResponseEntity<Map<String, String>> getEnvironment() {
    return ResponseEntity.ok(Map.of(
        "environment", sriConfiguration.getEnvironment(),
        "name", sriConfiguration.getEnvironmentName()
    ));
}
```

Esto permitiría al frontend mostrar un badge:

- 🧪 "Ambiente: PRUEBAS"
- 🚨 "Ambiente: PRODUCCIÓN"

---

## 🔮 Mejoras Futuras

1. **Variable de entorno** en lugar de `application.properties`:

   ```bash
   SRI_ENVIRONMENT=2 java -jar ebill-billing.jar
   ```

2. **Endpoint admin para cambiar ambiente** (requiere autenticación):

   ```bash
   POST /api/admin/sri/environment
   Body: { "environment": "2" }
   ```

3. **Validación adicional** al cambiar a producción:
   - Requerir confirmación
   - Verificar certificado digital
   - Verificar RUC configurado

---

**Fin del documento**
