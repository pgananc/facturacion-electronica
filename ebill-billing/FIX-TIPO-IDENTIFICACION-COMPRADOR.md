# 🔧 Fix: Tipo de Identificación del Comprador

**Fecha:** 11 de Noviembre 2025  
**Archivo:** `GenerateInvoiceSri.java`  
**Línea modificada:** 182

---

## 🔴 Problema

Al enviar facturas al SRI, se recibía el siguiente error:

```
ERROR EN LA IDENTIFICACION DEL RECEPTOR
La longitud del número de cédula debe ser 10
```

### Causa Raíz

El tipo de identificación del comprador estaba **hardcodeado como CEDULA** (código SRI `05`) para todos los casos:

```java
infoFactura.setTipoIdentificacionComprador(TipoIdentificacionCompradorEnum.CEDULA); // REVISAR
```

Esto causaba que:

- **Clientes con RUC** (13 dígitos) se enviaban como CEDULA (10 dígitos) → ❌ Error de validación
- **Clientes con Pasaporte** se enviaban como CEDULA → ❌ Error de validación
- **Consumidor Final** se enviaba como CEDULA → ❌ Error semántico

---

## ✅ Solución Implementada

### 1. Mapeo Dinámico del `idType`

Se creó el método `mapIdTypeToTipoIdentificacion()` que mapea el `idType` del cliente al enum del SRI:

```java
private TipoIdentificacionCompradorEnum mapIdTypeToTipoIdentificacion(Integer idType) {
  if (idType == null) {
    log.warn("⚠️ idType es null, usando CONSUMIDOR_FINAL por defecto");
    return TipoIdentificacionCompradorEnum.CONSUMIDOR_FINAL;
  }

  return switch (idType) {
    case 1 -> TipoIdentificacionCompradorEnum.CEDULA;              // Cédula (10 dígitos)
    case 2 -> TipoIdentificacionCompradorEnum.RUC;                 // RUC (13 dígitos)
    case 3 -> TipoIdentificacionCompradorEnum.PASAPORTE;           // Pasaporte
    case 4 -> TipoIdentificacionCompradorEnum.CONSUMIDOR_FINAL;    // Consumidor final
    case 5 -> TipoIdentificacionCompradorEnum.IDENTIFICACION_DEL_EXTERIOR; // ID del exterior
    default -> {
      log.warn("⚠️ idType {} no reconocido, usando CONSUMIDOR_FINAL por defecto", idType);
      yield TipoIdentificacionCompradorEnum.CONSUMIDOR_FINAL;
    }
  };
}
```

### 2. Uso del Método en `crearInfoFactura()`

Se reemplazó el valor hardcodeado por el mapeo dinámico:

**ANTES:**

```java
infoFactura.setTipoIdentificacionComprador(TipoIdentificacionCompradorEnum.CEDULA); // REVISAR
```

**DESPUÉS:**

```java
infoFactura.setTipoIdentificacionComprador(
    mapIdTypeToTipoIdentificacion(invoiceDto.getClientDto().getIdType()));
```

---

## 📊 Códigos del SRI

| idType | Tipo de Identificación      | Código SRI | Longitud   |
| ------ | --------------------------- | ---------- | ---------- |
| 1      | CEDULA                      | 05         | 10 dígitos |
| 2      | RUC                         | 04         | 13 dígitos |
| 3      | PASAPORTE                   | 06         | Variable   |
| 4      | CONSUMIDOR_FINAL            | 07         | 13 dígitos |
| 5      | IDENTIFICACION_DEL_EXTERIOR | 08         | Variable   |
| -      | PLACA (no usado)            | 09         | Variable   |

---

## 🧪 Casos de Prueba

### ✅ Caso 1: Cliente con Cédula

```json
{
  "idClient": 1,
  "idType": 1,
  "identification": "1234567890"
}
```

**Resultado:** `TipoIdentificacionComprador = 05 (CEDULA)` ✅

---

### ✅ Caso 2: Cliente con RUC

```json
{
  "idClient": 2,
  "idType": 2,
  "identification": "1234567890001"
}
```

**Resultado:** `TipoIdentificacionComprador = 04 (RUC)` ✅

---

### ✅ Caso 3: Cliente con Pasaporte

```json
{
  "idClient": 3,
  "idType": 3,
  "identification": "AB123456"
}
```

**Resultado:** `TipoIdentificacionComprador = 06 (PASAPORTE)` ✅

---

### ✅ Caso 4: Consumidor Final

```json
{
  "idClient": 4,
  "idType": 4,
  "identification": "9999999999999"
}
```

**Resultado:** `TipoIdentificacionComprador = 07 (CONSUMIDOR_FINAL)` ✅

---

## 🔍 Validación del SRI

El SRI valida:

1. ✅ **Código correcto:** El tipo de identificación debe coincidir con el formato del número
2. ✅ **Longitud correcta:**
   - CEDULA: 10 dígitos
   - RUC: 13 dígitos
   - CONSUMIDOR_FINAL: 13 dígitos (9999999999999)
   - PASAPORTE: Variable
3. ✅ **Formato correcto:** Cada tipo tiene reglas específicas de validación

---

## 📌 Notas Importantes

1. **Logging:** Se agregó `@Slf4j` a `GenerateInvoiceSri` para tener logs de fallback
2. **Fallback:** Si `idType` es `null` o no reconocido, se usa `CONSUMIDOR_FINAL` por defecto
3. **Compatibilidad:** El mapeo sigue los códigos oficiales del SRI según el anexo del SRI

---

## 🚀 Impacto

- ✅ Ahora las facturas con clientes RUC se envían correctamente al SRI
- ✅ Se respeta el tipo de identificación configurado en `ebill-customer`
- ✅ Se eliminan errores de validación por longitud incorrecta
- ✅ Logging mejorado para identificar problemas con `idType`

---

**Fin del documento**
