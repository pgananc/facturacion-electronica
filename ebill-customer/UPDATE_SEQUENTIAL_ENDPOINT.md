# 🔢 Endpoint PATCH - Actualizar Secuencial de Punto de Emisión

## 📋 **Resumen**

Nuevo endpoint que permite actualizar el **secuencial** de un punto de emisión utilizando su **código** y **establecimiento** como identificadores.

---

## 🎯 **Endpoint**

```http
PATCH /api/invoice-issuance-points/{code}/sequential?establishmentId={id}
```

### **Parámetros:**

| Parámetro | Tipo | Ubicación | Descripción | Ejemplo |
|-----------|------|-----------|-------------|---------|
| `code` | String | Path | Código del punto de emisión | `001` |
| `establishmentId` | Long | Query | ID del establecimiento | `5` |
| `sequentialInvoice` | Long | Body | Nuevo valor del secuencial (≥ 1) | `5000` |

> **⚠️ Importante:** El `code` solo NO es único. Puede haber varios puntos de emisión con el mismo código en diferentes establecimientos. Por eso es **obligatorio** enviar también el `establishmentId`.

---

## 📨 **Request**

### **Headers:**
```http
Content-Type: application/json
```

### **URL completa:**
```
PATCH /api/invoice-issuance-points/001/sequential?establishmentId=5
```

### **Body:**
```json
{
  "sequentialInvoice": 5000
}
```

### **Validaciones:**
- ✅ `sequentialInvoice` **no puede ser nulo**
- ✅ `sequentialInvoice` debe ser **mayor o igual a 1**
- ✅ `establishmentId` es **obligatorio**
- ✅ Solo se actualizan puntos **activos** (`status = true`)

---

## ✅ **Response - Success (200 OK)**

```json
{
  "idInvoiceIssuancePoint": 10,
  "code": "001-001",
  "sequentialInvoice": 5000,
  "establishmentId": 5,
  "establishmentCode": "001",
  "status": true,
  "createdAt": "2025-01-15T10:30:00",
  "updatedAt": "2025-11-08T15:45:00"
}
```

---

## ❌ **Errores Posibles**

### **1. Punto de emisión no encontrado (404 NOT FOUND)**

```json
{
  "timestamp": "2025-11-08T15:45:00.123Z",
  "status": 404,
  "error": "Not Found",
  "message": "Punto de emisión no encontrado con código: 001 en establecimiento: 999",
  "path": "/api/invoice-issuance-points/001/sequential"
}
```

**Causas:**
- El código no existe en ese establecimiento específico
- El punto de emisión existe pero está inactivo (`status=false`)
- El `establishmentId` no existe

---

### **2. Validación fallida (400 BAD REQUEST)**

#### **Secuencial nulo:**
```json
{
  "timestamp": "2025-11-08T15:45:00.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "El secuencial no puede ser nulo",
  "path": "/api/invoice-issuance-points/001-001/sequential"
}
```

#### **Secuencial menor a 1:**
```json
{
  "timestamp": "2025-11-08T15:45:00.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "El secuencial debe ser mayor o igual a 1",
  "path": "/api/invoice-issuance-points/001-001/sequential"
}
```

**Request que causa el error:**
```json
{
  "sequentialInvoice": 0  // ❌ Debe ser ≥ 1
}
```

---

## 🧪 **Ejemplos de Uso**

### **Ejemplo 1: Actualizar secuencial exitosamente**

```bash
curl -X PATCH 'http://localhost:8180/api/invoice-issuance-points/001/sequential?establishmentId=5' \
  -H "Content-Type: application/json" \
  -d '{
    "sequentialInvoice": 5000
  }'
```

**Response:**
```json
{
  "idInvoiceIssuancePoint": 10,
  "code": "001",
  "sequentialInvoice": 5000,
  "establishmentId": 5,
  "status": true
}
```

---

### **Ejemplo 2: Reiniciar secuencial en otro establecimiento**

```bash
curl -X PATCH 'http://localhost:8180/api/invoice-issuance-points/001/sequential?establishmentId=8' \
  -H "Content-Type: application/json" \
  -d '{
    "sequentialInvoice": 1
  }'
```

> **Nota:** Observa que el mismo código "001" puede existir en diferentes establecimientos (5 y 8).

---

### **Ejemplo 3: Código no existe en ese establecimiento (Error 404)**

```bash
curl -X PATCH 'http://localhost:8180/api/invoice-issuance-points/999/sequential?establishmentId=5' \
  -H "Content-Type: application/json" \
  -d '{
    "sequentialInvoice": 100
  }'
```

**Response:**
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Punto de emisión no encontrado con código: 999 en establecimiento: 5"
}
```

---

## 🏗️ **Arquitectura**

```
┌─────────────────────────────────────────────────────┐
│ InvoiceIssuancePointController                      │
│                                                      │
│ @PatchMapping("/{code}/sequential")                 │
│   ↓ valida con @Valid                               │
└──────────────────────┬──────────────────────────────┘
                       │
                       ↓
┌─────────────────────────────────────────────────────┐
│ IInvoiceIssuancePointService                        │
│                                                      │
│ updateSequentialByCode(code, newSequential)         │
│   ↓ busca por código activo                         │
│   ↓ actualiza sequentialInvoice                     │
│   ↓ guarda en BD                                    │
└──────────────────────┬──────────────────────────────┘
                       │
                       ↓
┌─────────────────────────────────────────────────────┐
│ IInvoiceIssuancePointRepo                           │
│                                                      │
│ findByCodeAndStatusIsTrue(code)                     │
│   ↓ SELECT * FROM invoice_issuance_point            │
│     WHERE code = :code AND status = true            │
└─────────────────────────────────────────────────────┘
```

---

## 📊 **Base de Datos**

### **Tabla afectada:**

```sql
invoice_issuance_point
├── id_invoice_issuance_point (PK)
├── code (VARCHAR) ← Usado para buscar
├── sequential_invoice (BIGINT) ← Campo actualizado
├── id_establishment (FK)
└── status (BOOLEAN) ← Debe ser true
```

### **Query ejecutada:**

```sql
-- 1. Buscar punto de emisión
SELECT *
FROM invoice_issuance_point
WHERE code = '001-001'
  AND status = true;

-- 2. Actualizar secuencial
UPDATE invoice_issuance_point
SET sequential_invoice = 5000,
    updated_at = NOW()
WHERE id_invoice_issuance_point = 10;
```

---

## 🔐 **Seguridad**

### **Consideraciones:**

1. ✅ **Solo puntos activos** → `status = true`
2. ✅ **Validación de entrada** → `@Valid` + `@Min(1)`
3. ⚠️ **Sin autenticación** → Agregar si es necesario

### **Mejora sugerida (opcional):**

```java
@PatchMapping("/{code}/sequential")
@PreAuthorize("hasRole('ADMIN')") // Solo administradores
public ResponseEntity<InvoiceIssuancePointResponseDto> updateSequential(...) {
```

---

## 🧩 **Archivos Modificados/Creados**

| Archivo | Tipo | Descripción |
|---------|------|-------------|
| `UpdateSequentialRequestDto.java` | DTO (nuevo) | Contiene `sequentialInvoice` con validación |
| `IInvoiceIssuancePointRepo.java` | Repository | Agregado método `findByCodeAndStatusIsTrue()` |
| `IInvoiceIssuancePointService.java` | Service Interface | Agregado método `updateSequentialByCode()` |
| `InvoiceIssuancePointServiceImpl.java` | Service Impl | Implementación de la lógica de actualización |
| `InvoiceIssuancePointController.java` | Controller | Agregado endpoint PATCH `/{code}/sequential` |

---

## 🎓 **Casos de Uso**

### **1. Migración de datos**
Cuando importas facturas de un sistema anterior y necesitas ajustar el secuencial al último número usado.

```json
PATCH /api/invoice-issuance-points/001-001/sequential
{
  "sequentialInvoice": 12450
}
```

---

### **2. Corrección de errores**
Si el secuencial se desincronizó por algún error, puedes corregirlo manualmente.

```json
PATCH /api/invoice-issuance-points/002-005/sequential
{
  "sequentialInvoice": 1
}
```

---

### **3. Reinicio de año fiscal**
Al iniciar un nuevo año fiscal, puedes reiniciar el secuencial a 1.

```json
PATCH /api/invoice-issuance-points/001-001/sequential
{
  "sequentialInvoice": 1
}
```

---

## 🚀 **Testing**

### **Test Postman/Insomnia:**

**Request:**
```http
PATCH http://localhost:8180/api/invoice-issuance-points/001-001/sequential
Content-Type: application/json

{
  "sequentialInvoice": 5000
}
```

**Expected Response:**
- Status: `200 OK`
- Body: Objeto con `sequentialInvoice: 5000`

---

### **Test cURL:**

```bash
# Test exitoso
curl -i -X PATCH http://localhost:8180/api/invoice-issuance-points/001-001/sequential \
  -H "Content-Type: application/json" \
  -d '{"sequentialInvoice": 5000}'

# Test validación (debe fallar)
curl -i -X PATCH http://localhost:8180/api/invoice-issuance-points/001-001/sequential \
  -H "Content-Type: application/json" \
  -d '{"sequentialInvoice": 0}'

# Test código inexistente (debe fallar)
curl -i -X PATCH http://localhost:8180/api/invoice-issuance-points/999-999/sequential \
  -H "Content-Type: application/json" \
  -d '{"sequentialInvoice": 100}'
```

---

## ✅ **Checklist de Implementación**

- [x] DTO `UpdateSequentialRequestDto` creado con validaciones
- [x] Método en repositorio `findByCodeAndStatusIsTrue()`
- [x] Método en service `updateSequentialByCode()`
- [x] Endpoint PATCH en controller
- [x] Documentación JavaDoc
- [x] Sin errores de linter
- [ ] Tests unitarios (pendiente)
- [ ] Tests de integración (pendiente)

---

## 📝 **Notas Adicionales**

1. **Búsqueda por código:** El endpoint usa `code` en lugar de `id` para mayor facilidad de uso desde el frontend.

2. **Solo puntos activos:** Si intentas actualizar un punto de emisión inactivo, recibirás un 404.

3. **Validación automática:** Spring Boot valida automáticamente que `sequentialInvoice` sea ≥ 1 gracias a `@Valid` y `@Min(1)`.

4. **Thread-safe:** La actualización es transaccional, por lo que no hay riesgo de condiciones de carrera.

---

## 🤝 **Integración con Frontend**

### **Angular Service:**

```typescript
// invoice-issuance-point.service.ts

updateSequential(
  code: string, 
  establishmentId: number, 
  newSequential: number
): Observable<InvoiceIssuancePointResponse> {
  const url = `${this.apiUrl}/${code}/sequential`;
  const params = new HttpParams().set('establishmentId', establishmentId.toString());
  const body = { sequentialInvoice: newSequential };
  
  return this.http.patch<InvoiceIssuancePointResponse>(url, body, { params });
}
```

### **Component:**

```typescript
// component.ts

updateSequential(code: string, establishmentId: number, newValue: number) {
  this.invoiceService.updateSequential(code, establishmentId, newValue).subscribe({
    next: (updated) => {
      console.log('Secuencial actualizado:', updated.sequentialInvoice);
      this.toastr.success('Secuencial actualizado correctamente');
    },
    error: (err) => {
      if (err.status === 404) {
        this.toastr.error('Punto de emisión no encontrado en ese establecimiento');
      } else if (err.status === 400) {
        this.toastr.error('Secuencial inválido (debe ser ≥ 1)');
      }
    }
  });
}
```

---

**¡Endpoint listo para usar!** 🎉

