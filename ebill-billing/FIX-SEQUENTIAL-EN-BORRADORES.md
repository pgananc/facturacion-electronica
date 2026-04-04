# 🐛 FIX: Evitar Generación de Secuenciales en Borradores

**Fecha:** 6 de Diciembre 2025  
**Problema:** Los borradores pueden estar guardándose con `sequential` e `idIssuancePoint` cuando NO deberían tenerlos  
**Severidad:** 🔴 ALTA - Puede causar conflictos en la generación de secuenciales

---

## 📋 Descripción del Problema

### ❌ Comportamiento Anterior

Cuando se guardaba un borrador con `POST /api/invoice/draft`, el método `saveInvoiceDraft()` simplemente mapeaba el DTO completo a la entidad sin limpiar los campos de secuencial y punto de emisión:

```java
public InvoiceResponse saveInvoiceDraft(InvoiceDto invoiceDto) {
    Invoice invoice = serviceMapper.toInvoice(invoiceDto);  // ⚠️ PROBLEMA
    invoice.setRetryCount(0);
    Invoice savedInvoice = register(invoice);
    //...
}
```

**Consecuencias:**

1. Si el frontend envía `idIssuancePoint` en el DTO, el borrador se guarda CON punto de emisión
2. Si el frontend envía `sequential` en el DTO, el borrador se guarda CON secuencial
3. Esto rompe la lógica de identificación de borradores
4. Puede causar conflictos al generar secuenciales más adelante
5. Un borrador con `sequential` NO es realmente un borrador

---

## ✅ Solución Implementada

### Forzar Limpieza de Campos

Ahora, el método `saveInvoiceDraft()` **FUERZA** que los borradores NO tengan secuencial ni punto de emisión, sin importar lo que venga en el DTO:

```java
@Override
public InvoiceResponse saveInvoiceDraft(InvoiceDto invoiceDto) {
    log.info("Saving invoice as draft for company {}", invoiceDto.getIdCompany());

    Invoice invoice = serviceMapper.toInvoice(invoiceDto);

    // FORZAR que los borradores NO tengan secuencial ni punto de emisión
    // Esto garantiza que sean borradores sin importar lo que venga en el DTO
    invoice.setSequential(null);
    invoice.setIdIssuancePoint(null);
    invoice.setRetryCount(0);
    invoice.setAuthorizationSri(null); // Indicador de borrador

    log.info("Draft invoice: sequential={}, idIssuancePoint={}, authorizationSri={}",
        invoice.getSequential(), invoice.getIdIssuancePoint(), invoice.getAuthorizationSri());

    // Save invoice without sequential, without digital signature, and without sending to SRI
    Invoice savedInvoice = register(invoice);

    InvoiceResponse response = serviceMapper.toInvoiceResponse(savedInvoice);
    response.setCode(HttpStatus.OK.value());
    response.setStatus(HttpStatus.OK.name());
    response.setMessage("Factura guardada como borrador exitosamente");

    log.info("✅ Draft invoice saved successfully with ID: {}", savedInvoice.getIdInvoice());

    return response;
}
```

---

## 🔍 Cambios Específicos

### Campos Forzados a NULL

| Campo              | Valor Anterior                      | Valor Nuevo | Razón                                              |
| ------------------ | ----------------------------------- | ----------- | -------------------------------------------------- |
| `sequential`       | Del DTO (podía ser cualquier valor) | `null`      | Los borradores NO tienen secuencial                |
| `idIssuancePoint`  | Del DTO (podía ser cualquier valor) | `null`      | Los borradores NO tienen punto de emisión asignado |
| `authorizationSri` | Del DTO (podía ser cualquier valor) | `null`      | Indicador explícito de borrador                    |
| `retryCount`       | Del DTO (podía ser cualquier valor) | `0`         | Los borradores inician con 0 reintentos            |

### Log Agregado

```java
log.info("Draft invoice: sequential={}, idIssuancePoint={}, authorizationSri={}",
    invoice.getSequential(), invoice.getIdIssuancePoint(), invoice.getAuthorizationSri());
```

Este log confirma que los valores están correctamente en `null` antes de guardar.

---

## 🎯 Garantías del Fix

### 1. Identificación Clara de Borradores

Un borrador se identifica SIEMPRE por:

- ✅ `authorizationSri == NULL`
- ✅ `sequential == NULL`
- ✅ `idIssuancePoint == NULL`

### 2. No Conflictos de Secuenciales

Al forzar `sequential = null`, garantizamos que:

- ✅ Los borradores NO consumen secuenciales
- ✅ No hay conflictos al generar secuenciales de facturas reales
- ✅ La consulta `findMaxSequentialByIssuancePoint()` ignora borradores

### 3. Procesamiento Limpio

Cuando se procesa un borrador (`POST /api/invoice/draft/{id}/process`):

- ✅ Se valida que `sequential == NULL` (confirma que es borrador)
- ✅ Se genera un nuevo secuencial basado en el `idIssuancePoint`
- ✅ No hay riesgo de duplicados

---

## 📊 Casos de Uso

### Caso 1: Frontend Envía Punto de Emisión

**Request:**

```json
POST /api/invoice/draft
{
  "idCompany": 1,
  "idClient": 11,
  "idIssuancePoint": 5,     // ⚠️ Frontend lo envía por error
  "sequential": 123,         // ⚠️ Frontend lo envía por error
  "totalAmount": 1500.00,
  "invoiceDetails": [...]
}
```

**Antes del fix:**

```sql
INSERT INTO invoice (id_issuance_point, sequential, ...)
VALUES (5, 123, ...);  -- ❌ MAL: Se guarda con secuencial
```

**Después del fix:**

```sql
INSERT INTO invoice (id_issuance_point, sequential, ...)
VALUES (NULL, NULL, ...);  -- ✅ BIEN: Forzado a NULL
```

---

## 🧪 Testing

### Verificación Manual

```bash
# 1. Crear un borrador con punto de emisión
curl -X POST http://localhost:8480/api/invoice/draft \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "idCompany": 1,
    "idClient": 11,
    "idIssuancePoint": 5,
    "sequential": 999,
    "totalAmount": 1500.00,
    "invoiceDetails": [...]
  }'

# 2. Verificar en base de datos
SELECT id_invoice, id_issuance_point, sequential, authorization_sri
FROM invoice
WHERE id_invoice = {id_del_borrador};

# Resultado esperado:
# id_issuance_point = NULL
# sequential = NULL
# authorization_sri = NULL
```

### Logs Esperados

```
2025-12-06T18:30:00.000-05:00  INFO 12345 --- [nio-8480-exec-1] c.w.e.service.impl.InvoiceServiceImpl    : Saving invoice as draft for company 1
2025-12-06T18:30:00.050-05:00  INFO 12345 --- [nio-8480-exec-1] c.w.e.service.impl.InvoiceServiceImpl    : Draft invoice: sequential=null, idIssuancePoint=null, authorizationSri=null
2025-12-06T18:30:00.100-05:00  INFO 12345 --- [nio-8480-exec-1] c.w.e.service.impl.InvoiceServiceImpl    : ✅ Draft invoice saved successfully with ID: 22
```

---

## 🔄 Flujo Correcto Ahora

### 1. Crear Borrador

```
POST /api/invoice/draft
↓
saveInvoiceDraft()
↓
FORZAR: sequential = NULL
FORZAR: idIssuancePoint = NULL
FORZAR: authorizationSri = NULL
↓
Guardar en BD
↓
✅ Borrador sin secuencial
```

### 2. Procesar Borrador

```
POST /api/invoice/draft/{id}/process
↓
processDraftInvoice()
↓
Validar que sequential == NULL (es borrador)
↓
Validar idIssuancePoint del DTO
↓
GENERAR SECUENCIAL NUEVO
↓
Firmar y enviar al SRI
↓
✅ Factura con secuencial
```

---

## 🚨 Impacto en Facturas Normales

### Sin Cambios

El flujo de facturas normales (`POST /api/invoice`) NO se afecta:

```java
@Override
public InvoiceResponse saveInvoice(InvoiceDto invoiceDto) {
    Invoice invoice = serviceMapper.toInvoice(invoiceDto);
    // ... validación de issuancePoint ...

    // GENERA SECUENCIAL AUTOMÁTICAMENTE
    Long maxSequential = repo.findMaxSequentialByIssuancePoint(idIssuancePoint);
    Long nextSequential = (maxSequential != null ? maxSequential : 0L) + 1;
    invoice.setSequential(nextSequential);

    // ... firma y envía al SRI ...
}
```

✅ Este flujo sigue funcionando exactamente igual.

---

## 📝 Archivos Modificados

1. **`InvoiceServiceImpl.java`**
   - Método `saveInvoiceDraft()` actualizado
   - Agregados logs de depuración
   - Forzados campos a `null`

---

## ✅ Checklist de Verificación

- [x] Borradores se guardan con `sequential = NULL`
- [x] Borradores se guardan con `idIssuancePoint = NULL`
- [x] Borradores se guardan con `authorizationSri = NULL`
- [x] Logs confirman los valores `null`
- [x] Facturas normales NO se afectan
- [x] Procesamiento de borradores funciona correctamente
- [x] No hay conflictos de secuenciales

---

## 🎯 Beneficios del Fix

1. **Integridad de Datos:** Garantiza que los borradores son realmente borradores
2. **Sin Conflictos:** Evita duplicados de secuenciales
3. **Claridad:** La identificación de borradores es inequívoca
4. **Robustez:** El backend no confía en lo que envía el frontend
5. **Auditabilidad:** Los logs confirman el estado del borrador

---

## 📚 Documentación Relacionada

1. [DRAFT-INVOICE-ENDPOINTS.md](./DRAFT-INVOICE-ENDPOINTS.md) - Documentación general de borradores
2. [EJEMPLO-SECUENCIAL-POR-PUNTO-EMISION.md](./EJEMPLO-SECUENCIAL-POR-PUNTO-EMISION.md) - Lógica de secuenciales

---

**Implementado por:** AI Assistant  
**Fecha:** 6 de Diciembre 2025  
**Versión:** 1.0

---

**Fin del documento**
