# Reglas de Creación de Guía de Remisión vs Nota de Crédito
## Manejo de Errores del SRI y Persistencia en Base de Datos

---

## 📋 **RESUMEN EJECUTIVO**

Cuando el SRI genera un error o no autoriza un comprobante, **ambos servicios (Guía de Remisión y Nota de Crédito) guardan el registro en la base de datos**, pero con diferencias importantes en cómo manejan los estados y errores.

---

## 🔄 **FLUJO GENERAL: GUÍA DE REMISIÓN**

### **1. Creación Inicial (`saveGuide`)**

#### **Escenario A: Envío Exitoso al SRI**
```java
// Líneas 288-307 de GuideServiceImpl.java
AuthorizationSri authorizationSri = sendGuideService.sendGuideSri(...);
guide.setAuthorizationSri(authorizationSri);
Guide savedGuide = register(guide);  // ✅ SIEMPRE SE GUARDA
```

**Resultado:**
- ✅ La guía se **guarda en BD** con `AuthorizationSri` asociado
- ✅ Estado: `receptionStatusSri = "RECIBIDA"` → `authorizationStatusSri = "AUTORIZADO"`
- ✅ Se genera PDF y se envía email automáticamente

#### **Escenario B: Error de Conexión/Firma Digital**
```java
// Líneas 309-325 de GuideServiceImpl.java
catch (ResourceAccessException e) {
    response = serviceMapper.toGuideResponse(register(guide));  // ✅ SE GUARDA COMO BORRADOR
    response.setCode(HttpStatus.BAD_REQUEST.value());
    response.setMessage("Error al obtener la firma digital... la guía se registro en borrador.");
}
catch (CertificateException | SAXException | IOException | JAXBException | InterruptedException e) {
    response = serviceMapper.toGuideResponse(register(guide));  // ✅ SE GUARDA COMO BORRADOR
    response.setMessage("Hubo problemas para conectarse con el SRI, la guía se registro en borrador.");
}
```

**Resultado:**
- ✅ La guía se **guarda en BD** pero **SIN `AuthorizationSri`** (es un borrador)
- ⚠️ Estado: `authorizationSri = null` → Se considera **BORRADOR**
- ⚠️ No se puede generar PDF ni enviar email hasta que se autorice

#### **Escenario C: SRI Devuelve Error (DEVUELTA)**
```java
// Líneas 126-149 de SendGuideImpl.java
else if (estado.compareToIgnoreCase("DEVUELTA") == 0) {
    // Verificar si es "CLAVE ACCESO REGISTRADA" (código 43)
    boolean isClaveRegistrada = receptionXml.contains("CLAVE ACCESO REGISTRADA");
    
    if (isClaveRegistrada) {
        // Intentar consultar autorización existente
        return authorizeXMLBySri(guiaRemision, authorization);
    }
    
    // Si no es clave registrada, guardar con estado DEVUELTA
    AuthorizationSri authorization = getOrCreateAuthorization(
        existingAuthorizationSri,
        accessKey,
        estado,  // "DEVUELTA"
        receptionXml,
        "EL XML HA SIDO DEVUELTO"  // Mensaje de error
    );
    return authorizationRepo.save(authorization);  // ✅ SE GUARDA CON ERROR
}
```

**Resultado:**
- ✅ La guía se **guarda en BD** con `AuthorizationSri` asociado
- ❌ Estado: `receptionStatusSri = "DEVUELTA"`, `authorizationStatusSri = null` o diferente de "AUTORIZADO"
- ❌ `receptionError = "EL XML HA SIDO DEVUELTO"` → Contiene el XML con los errores del SRI
- ⚠️ No se genera PDF ni se envía email
- ⚠️ El usuario puede corregir y reenviar

#### **Escenario D: SRI No Responde**
```java
// Líneas 105-113 de SendGuideImpl.java
if (respuestaSolicitud == null) {
    AuthorizationSri authorization = getOrCreateAuthorization(
        existingAuthorizationSri,
        accessKey,
        null,  // Sin estado de recepción
        null,
        "ERROR: El SRI no retornó respuesta después de 5 intentos..."
    );
    return authorizationRepo.save(authorization);  // ✅ SE GUARDA CON ERROR
}
```

**Resultado:**
- ✅ La guía se **guarda en BD** con `AuthorizationSri` asociado
- ❌ Estado: `receptionStatusSri = null`, `authorizationStatusSri = null`
- ❌ `receptionError = "ERROR: El SRI no retornó respuesta..."` → Error de conectividad
- ⚠️ No se genera PDF ni se envía email

---

### **2. Cálculo de Estado para Listado**

```java
// Líneas 185-199 de GuideServiceImpl.java
private String calculateGuideStatus(Guide g) {
    if (g.getAuthorizationSri() == null) {
        return "BORRADOR";  // Sin AuthorizationSri = Borrador
    }
    String authStatus = g.getAuthorizationSri().getAuthorizationStatusSri();
    if ("AUTORIZADO".equalsIgnoreCase(authStatus)) {
        return "AUTORIZADO";
    }
    String receptionStatus = g.getAuthorizationSri().getReceptionStatusSri();
    if ("RECIBIDA".equalsIgnoreCase(receptionStatus) && !"AUTORIZADO".equalsIgnoreCase(authStatus)) {
        return "EN_PROCESO";  // Recibida pero aún no autorizada
    }
    return "DEVUELTA";  // Cualquier otro caso (DEVUELTA, ERROR, etc.)
}
```

**Estados Posibles:**
- `BORRADOR`: `authorizationSri == null`
- `AUTORIZADO`: `authorizationStatusSri == "AUTORIZADO"`
- `EN_PROCESO`: `receptionStatusSri == "RECIBIDA"` pero aún no autorizado
- `DEVUELTA`: `receptionStatusSri == "DEVUELTA"` o cualquier otro error

---

## 🔄 **FLUJO GENERAL: NOTA DE CRÉDITO**

### **1. Creación Inicial (`saveCreditNote`)**

#### **Escenario A: Envío Exitoso al SRI**
```java
// Líneas 418-435 de CreditNoteServiceImpl.java
AuthorizationSri authorizationSri = sendCreditNoteService.sendCreditNoteSri(...);
creditNote.setAuthorizationSri(authorizationSri);
CreditNote savedCreditNote = register(creditNote);  // ✅ SIEMPRE SE GUARDA
```

**Resultado:**
- ✅ La nota de crédito se **guarda en BD** con `AuthorizationSri` asociado
- ✅ Estado: `receptionStatusSri = "RECIBIDA"` → `authorizationStatusSri = "AUTORIZADO"`
- ✅ Se genera PDF y se envía email automáticamente

#### **Escenario B: Error de Conexión/Firma Digital**
```java
// Líneas 437-453 de CreditNoteServiceImpl.java
catch (ResourceAccessException e) {
    response = serviceMapper.toCreditNoteResponse(register(creditNote));  // ✅ SE GUARDA COMO BORRADOR
    response.setCode(HttpStatus.BAD_REQUEST.value());
    response.setMessage("Error al obtener la firma digital... la nota de crédito se registro en borrador.");
}
catch (CertificateException | SAXException | IOException | JAXBException | InterruptedException e) {
    response = serviceMapper.toCreditNoteResponse(register(creditNote));  // ✅ SE GUARDA COMO BORRADOR
    response.setMessage("Hubo problemas para conectarse con el SRI, la nota de crédito se registro en borrador.");
}
```

**Resultado:**
- ✅ La nota de crédito se **guarda en BD** pero **SIN `AuthorizationSri`** (es un borrador)
- ⚠️ Estado: `authorizationSri = null` → Se considera **BORRADOR**
- ⚠️ No se puede generar PDF ni enviar email hasta que se autorice

#### **Escenario C: SRI Devuelve Error (DEVUELTA)**
```java
// Líneas 126-151 de SendCreditNoteImpl.java
else if (estado.compareToIgnoreCase("DEVUELTA") == 0) {
    // Verificar si es "CLAVE ACCESO REGISTRADA" (código 43)
    boolean isClaveRegistrada = receptionXml.contains("CLAVE ACCESO REGISTRADA");
    
    if (isClaveRegistrada) {
        // Intentar consultar autorización existente
        return authorizeXMLBySri(notaCredito, authorization);
    }
    
    // Si no es clave registrada, guardar con estado DEVUELTA
    AuthorizationSri authorization = getOrCreateAuthorization(
        existingAuthorizationSri,
        accessKey,
        estado,  // "DEVUELTA"
        receptionXml,
        "EL XML HA SIDO DEVUELTO"  // Mensaje de error
    );
    return authorizationRepo.save(authorization);  // ✅ SE GUARDA CON ERROR
}
```

**Resultado:**
- ✅ La nota de crédito se **guarda en BD** con `AuthorizationSri` asociado
- ❌ Estado: `receptionStatusSri = "DEVUELTA"`, `authorizationStatusSri = null` o diferente de "AUTORIZADO"
- ❌ `receptionError = "EL XML HA SIDO DEVUELTO"` → Contiene el XML con los errores del SRI
- ⚠️ No se genera PDF ni se envía email
- ⚠️ El usuario puede corregir y reenviar

---

### **2. Procesamiento por Tarea Programada (Scheduled Task)**

```java
// Líneas 888-910 de CreditNoteServiceImpl.java
boolean isAuthorized = Objects.nonNull(authorizationSri)
    && StringUtils.hasText(authorizationSri.getAuthorizationStatusSri())
    && "AUTORIZADO".equalsIgnoreCase(authorizationSri.getAuthorizationStatusSri());

boolean isReceived = Objects.nonNull(authorizationSri)
    && StringUtils.hasText(authorizationSri.getReceptionStatusSri())
    && "RECIBIDA".equalsIgnoreCase(authorizationSri.getReceptionStatusSri());

if (isAuthorized && isReceived) {
    // ✅ Éxito: Resetear contador de reintentos y guardar
    repo.save(creditNote);
    notifyAuthorizedCreditNote(...);
} else {
    // ❌ Falla: Incrementar contador de reintentos
    String failureReason = buildFailureReason(authorizationSri, isAuthorized, isReceived);
    handleCreditNoteFailure(creditNote, failureReason);
}
```

**Diferencia Clave:**
- Las notas de crédito tienen un **sistema de reintentos** (`retryCount`) para procesar borradores automáticamente
- Las guías de remisión **NO tienen** este sistema de reintentos automáticos

---

## 📊 **COMPARACIÓN: GUÍA DE REMISIÓN vs NOTA DE CRÉDITO**

| Aspecto | Guía de Remisión | Nota de Crédito |
|---------|------------------|-----------------|
| **Guardado en BD cuando hay error** | ✅ **SIEMPRE** | ✅ **SIEMPRE** |
| **Estado cuando no hay AuthorizationSri** | `BORRADOR` | `BORRADOR` |
| **Estado cuando SRI devuelve error** | `DEVUELTA` (con `AuthorizationSri` y `receptionError`) | `DEVUELTA` (con `AuthorizationSri` y `receptionError`) |
| **Sistema de reintentos automáticos** | ❌ **NO** | ✅ **SÍ** (tarea programada) |
| **Manejo de "CLAVE ACCESO REGISTRADA"** | ✅ Intenta consultar autorización existente | ✅ Intenta consultar autorización existente |
| **Filtrado en listado** | Solo muestra `BORRADOR` y `AUTORIZADO` | Solo muestra `BORRADOR` y `AUTORIZADO` |
| **Persistencia de errores** | ✅ Se guarda `receptionError` y `receptionXml` | ✅ Se guarda `receptionError` y `receptionXml` |

---

## 🔍 **ESTADOS DE `AuthorizationSri`**

### **Campos Clave:**
```java
// AuthorizationSri.java
private String receptionStatusSri;      // "RECIBIDA", "DEVUELTA", null
private String authorizationStatusSri; // "AUTORIZADO", null, otros
private String receptionError;          // Mensaje de error del SRI
private String receptionXml;            // XML de respuesta del SRI
private String authorizationError;      // Error en autorización
private String authorizationXml;         // XML de autorización (si fue autorizado)
```

### **Combinaciones de Estados:**

| `receptionStatusSri` | `authorizationStatusSri` | Significado |
|---------------------|-------------------------|-------------|
| `null` | `null` | Borrador (no enviado al SRI) |
| `"RECIBIDA"` | `null` | Recibida por SRI, esperando autorización |
| `"RECIBIDA"` | `"AUTORIZADO"` | ✅ **Autorizada exitosamente** |
| `"DEVUELTA"` | `null` | ❌ **Rechazada por el SRI** (errores en el XML) |
| `null` | `null` + `receptionError` | Error de conectividad o timeout |

---

## ⚠️ **REGLAS IMPORTANTES**

### **1. SIEMPRE se guarda en BD**
- ✅ **Tanto guías como notas de crédito** se guardan en la base de datos **incluso cuando hay errores**
- ✅ Esto permite:
  - Corregir errores y reenviar
  - Auditar intentos fallidos
  - Mantener historial completo

### **2. Diferencias en el manejo de errores**

#### **Guía de Remisión:**
- ❌ **NO tiene** sistema de reintentos automáticos
- ✅ El usuario debe **reenviar manualmente** usando `POST /api/guides/{id}/send`
- ✅ Los errores se guardan en `AuthorizationSri.receptionError`

#### **Nota de Crédito:**
- ✅ **SÍ tiene** sistema de reintentos automáticos (tarea programada)
- ✅ Los borradores se procesan automáticamente en segundo plano
- ✅ Se incrementa `retryCount` en cada intento fallido
- ✅ Los errores se guardan en `AuthorizationSri.receptionError`

### **3. Filtrado en listados**

Ambos servicios filtran de la misma manera:
```java
// Solo muestran BORRADORES y AUTORIZADAS
.filter(cn -> {
    if (cn.getAuthorizationSri() == null) {
        return true;  // Borrador
    }
    String authStatus = cn.getAuthorizationSri().getAuthorizationStatusSri();
    return "AUTORIZADO".equalsIgnoreCase(authStatus);  // Solo autorizadas
})
```

**Esto significa:**
- ❌ Las guías/notas con estado `DEVUELTA` o `EN_PROCESO` **NO aparecen** en el listado principal
- ✅ Solo aparecen `BORRADOR` y `AUTORIZADO`

### **4. Manejo especial: "CLAVE ACCESO REGISTRADA"**

Ambos servicios manejan el caso especial cuando el SRI devuelve `DEVUELTA` pero con el código 43 (clave ya registrada):

```java
if (isClaveRegistrada) {
    // La clave ya existe en el SRI, intentar consultar autorización
    AuthorizationSri authorization = getOrCreateAuthorization(
        existingAuthorizationSri, accessKey, "RECIBIDA", receptionXml, null);
    return authorizeXMLBySri(guiaRemision, authorization);  // Consultar autorización
}
```

**Comportamiento:**
- ✅ Si la clave ya está registrada, se intenta consultar la autorización existente
- ✅ Si existe autorización, se asocia y se completa el proceso
- ✅ Si no existe, se guarda con estado `DEVUELTA`

---

## 📝 **CONCLUSIONES**

1. **Ambos servicios guardan SIEMPRE en BD**, incluso con errores
2. **La diferencia principal** es que las notas de crédito tienen reintentos automáticos
3. **Los errores del SRI** se guardan en `AuthorizationSri.receptionError` y `receptionXml`
4. **Los listados** solo muestran borradores y autorizados (ocultan DEVUELTAS)
5. **El usuario puede corregir y reenviar** cualquier comprobante con errores

---

## 🔧 **RECOMENDACIONES**

1. **Para Guías de Remisión:**
   - Considerar implementar un sistema de reintentos similar al de notas de crédito
   - O mantener el flujo manual actual (más control para el usuario)

2. **Para Notas de Crédito:**
   - El sistema de reintentos automáticos está bien implementado
   - Considerar límite máximo de reintentos para evitar loops infinitos

3. **Mejoras Generales:**
   - Exponer un endpoint para consultar comprobantes con estado `DEVUELTA`
   - Mostrar los errores del SRI en la interfaz de usuario
   - Permitir descargar el XML de recepción con los errores



