# Análisis del JSON para Crear Nota de Débito

## ❌ Problemas Encontrados

### 1. **FALTA `companyDto` (OBLIGATORIO)**

El campo `companyDto` es **obligatorio** según el código:

```java
// DebitNoteServiceImpl.java línea 695-697
if (debitNoteDto.getCompanyDto() == null
    || debitNoteDto.getCompanyDto().getIdCompany() == null) {
  throw new IllegalArgumentException("CompanyDto es requerido");
}
```

**Solución:** Agregar `companyDto` al JSON:

```json
{
  "companyDto": {
    "idCompany": 1,
    "identification": "1790000000001",
    "name": "EMPRESA DE PRUEBA S.A.",
    "status": true
  },
  // ... resto del JSON
}
```

---

### 2. **FALTA `establishment` (OBLIGATORIO)**

El campo `establishment` es **obligatorio** para extraer el `idIssuancePoint`:

```java
// DebitNoteServiceImpl.java línea 728-748
Long idIssuancePoint = extractIssuancePointId(debitNoteDto);
if (idIssuancePoint != null) {
  // ... carga establishment
} else {
  throw new IllegalArgumentException("...");
}
```

**Solución:** Agregar `establishment` al JSON:

```json
{
  "establishment": {
    "idEstablishment": 1,
    "type": "MATRIZ",
    "code": "001",
    "address": "AV. AMAZONAS Y COREA, QUITO",
    "issuancePoint": {
      "idIssuancePoint": 1,
      "code": "001"
    }
  },
  // ... resto del JSON
}
```

---

### 3. **Campos que NO deberían enviarse directamente**

- ❌ `idCompany` - Se extrae de `companyDto.idCompany`
- ❌ `idIssuancePoint` - Se extrae de `establishment.issuancePoint.idIssuancePoint`

**Solución:** Eliminar estos campos del JSON. El backend los extraerá automáticamente.

---

### 4. **`clientDto` con campos vacíos**

El `clientDto` tiene campos vacíos (`identification: ""`, `name: ""`). Aunque el backend intentará cargar el cliente por `idClient`, es mejor enviar los datos completos o al menos el `idClient` válido.

**Solución:** Asegurar que `idClient` sea válido o enviar datos completos del cliente.

---

### 5. **Campo `details` (deprecado)**

El campo `details` es un alias deprecado de `debitNoteDetails`. Aunque funciona, es mejor usar solo `debitNoteDetails`.

**Solución:** Eliminar `details` y usar solo `debitNoteDetails`.

---

### 6. **Campos opcionales enviados (no es error, pero innecesario)**

Los siguientes campos se calculan automáticamente y no es necesario enviarlos:

- `subtotalIce`, `subtotalIva`, `subtotalZero`
- `totalAmount`, `totalIce`, `totalIva`
- `valorModificacion`, `valorTotal`
- `discount` (si es 0)

**Solución:** Opcional eliminarlos. El backend los recalculará desde `debitNoteDetails`.

---

## ✅ JSON Corregido

```json
{
  "companyDto": {
    "idCompany": 1,
    "identification": "1790000000001",
    "name": "EMPRESA DE PRUEBA S.A.",
    "status": true
  },
  "clientDto": {
    "idClient": 3,
    "identification": "1712345678001",
    "name": "JUAN PEREZ CLIENTE",
    "idType": 4,
    "status": true
  },
  "establishment": {
    "idEstablishment": 1,
    "type": "MATRIZ",
    "code": "001",
    "issuancePoint": {
      "idIssuancePoint": 1,
      "code": "001"
    }
  },
  "issueDate": "2025-12-19",
  "originalInvoiceId": 10,
  "originalInvoiceNumber": "001-001-000000505",
  "originalIssueDate": "2025-12-09",
  "documentTypeModified": "01",
  "observation": "",
  "debitNoteDetails": [
    {
      "idProduct": 5,
      "productDto": {
        "idProduct": 5,
        "mainCode": "002",
        "name": "Impresora Epson l3150",
        "description": "Impresora",
        "unitPrice": 150.5,
        "rate": 0.15,
        "taxCode": "2",
        "taxCodPercentage": "4",
        "status": true
      },
      "unitPrice": 150.5,
      "quantity": 1,
      "discount": 0,
      "subtotal": 150.5
    }
  ],
  "paymentMethods": [
    {
      "paymentMethodCode": "01",
      "total": 173.08,
      "term": null,
      "timeUnit": null
    }
  ]
}
```

---

## 📋 Checklist de Validación

### Campos Obligatorios:
- [x] `companyDto` - ❌ FALTA
- [x] `clientDto` - ✅ Presente (pero con campos vacíos)
- [x] `establishment` - ❌ FALTA
- [x] `issueDate` - ✅ Presente
- [x] `originalInvoiceId` - ✅ Presente
- [x] `originalInvoiceNumber` - ✅ Presente
- [x] `originalIssueDate` - ✅ Presente
- [x] `debitNoteDetails` - ✅ Presente (mínimo 1 elemento)

### Campos Opcionales (se calculan automáticamente):
- [x] `totalAmount` - ✅ Presente (opcional)
- [x] `valorModificacion` - ✅ Presente (opcional)
- [x] `valorTotal` - ✅ Presente (opcional)
- [x] `subtotalZero`, `subtotalIva`, `subtotalIce` - ✅ Presentes (opcionales)
- [x] `totalIva`, `totalIce` - ✅ Presentes (opcionales)
- [x] `discount` - ✅ Presente (opcional)
- [x] `originalAccessKey` - ✅ Presente (opcional)
- [x] `observation` - ✅ Presente (opcional)
- [x] `documentTypeModified` - ✅ Presente (opcional, default "01")
- [x] `paymentMethods` - ✅ Presente (opcional)

### Campos que NO deben enviarse:
- [x] `idCompany` - ❌ NO enviar (se extrae de `companyDto`)
- [x] `idIssuancePoint` - ❌ NO enviar (se extrae de `establishment`)
- [x] `details` - ⚠️ Deprecado (usar solo `debitNoteDetails`)

---

## 🔍 Validaciones que Realiza el Backend

1. **`companyDto` es requerido** (línea 695-697)
2. **`clientDto` es requerido** (línea 713-714)
3. **`establishment` es requerido** para extraer `idIssuancePoint` (línea 728-748)
4. **`debitNoteDetails` debe tener al menos 1 elemento** (línea 442-449)
5. **Cada detalle debe tener `productDto`** (línea 456-459)
6. **Cada producto debe tener `name` o `description`** (línea 460-464)
7. **Cada detalle debe tener `subtotal > 0`** (línea 465-468)
8. **Validación de fecha:** `issueDate >= originalIssueDate` (método `validateAgainstOriginalInvoice`)
9. **Validación de total:** `valorTotal = valorModificacion + totalIva + totalIce` (método `validateTotalEqualsMotivosPlusImpuestos`)

---

## ⚠️ Errores que Obtendrás con el JSON Actual

1. **Error:** `"CompanyDto es requerido"`
   - **Causa:** Falta `companyDto`
   - **Solución:** Agregar `companyDto` con al menos `idCompany`

2. **Error:** `"Error al cargar datos del establecimiento"`
   - **Causa:** Falta `establishment` o no se puede extraer `idIssuancePoint`
   - **Solución:** Agregar `establishment` con `issuancePoint.idIssuancePoint`

---

## 💡 Recomendaciones

1. **Eliminar campos calculables:** No es necesario enviar `totalAmount`, `valorModificacion`, etc. El backend los calculará automáticamente desde `debitNoteDetails`.

2. **Eliminar campos directos:** No enviar `idCompany` ni `idIssuancePoint` directamente. El backend los extraerá de los objetos anidados.

3. **Eliminar alias deprecado:** No usar `details`, solo `debitNoteDetails`.

4. **Completar `clientDto`:** Aunque el backend intentará cargar el cliente por `idClient`, es mejor enviar los datos completos o al menos asegurar que `idClient` sea válido.

---

## 📝 Resumen

**El JSON actual tiene 2 problemas críticos:**
1. ❌ Falta `companyDto` (obligatorio)
2. ❌ Falta `establishment` (obligatorio)

**El JSON actual tiene 3 problemas menores:**
1. ⚠️ `idCompany` y `idIssuancePoint` no deberían enviarse directamente
2. ⚠️ `details` es un alias deprecado
3. ⚠️ `clientDto` tiene campos vacíos

**Con el JSON corregido, el servicio debería funcionar correctamente.**






