# Draft Invoice Endpoints - Documentación

## 📝 Resumen

Se han implementado **5 endpoints** para gestionar borradores de facturas (draft invoices). Los borradores son facturas que se guardan en la base de datos **sin firmar digitalmente** y **sin enviar al SRI**, permitiendo que sean completadas posteriormente.

### 🔑 Identificación de Borradores

Un borrador se identifica porque:

- ✅ `authorizationSri = NULL` (no tiene autorización del SRI)
- ✅ `sequential = NULL` (no tiene secuencial asignado)
- ✅ `idIssuancePoint = NULL` (puede o no tener punto de emisión)

---

## 🚀 Endpoints Implementados

### 1. **POST /api/invoice/draft** - Guardar Borrador

**Descripción:** Guarda una factura como borrador sin validar firma digital ni enviar al SRI.

**Request Body:** `InvoiceDto` (mismo formato que el endpoint normal)

**Response:** `InvoiceResponse`

```json
{
  "code": 200,
  "status": "OK",
  "message": "Factura guardada como borrador exitosamente",
  "invoiceDto": { ... }
}
```

**Características:**

- ✅ Guarda `Invoice` + `InvoiceDetails` (cascada)
- ✅ Inicializa `retryCount = 0`
- ❌ NO genera secuencial
- ❌ NO valida firma digital
- ❌ NO envía al SRI
- ❌ NO asigna `authorizationSri` (queda NULL)

**Logs:** Se registran en el sistema de logs con `EventType.REQUEST` y `EventType.RESPONSE`

---

### 2. **GET /api/invoice/drafts** - Listar Borradores

**Descripción:** Retorna todas las facturas que están en estado borrador.

**Response:** `ResponseEntity<List<InvoiceDraftResponseDto>>`

```json
[
  {
    "idInvoice": 123,
    "idCompany": 1,
    "idClient": 100,
    "issueDate": "2025-10-22T10:00:00",
    "creationDate": "2025-10-22T10:00:00",
    "invoiceNumber": "001-001-123456789",
    "environment": "PRODUCCION",
    "additionalInformation": "Información adicional",
    "totalAmount": 150.00,
    "tip": 0.00,
    "subtotalZero": 0.00,
    "subtotalIva": 150.00,
    "subtotalIce": 0.00,
    "discount": 0.00,
    "retryCount": 0,
    "paymentMethodName": "EFECTIVO",
    "status": true
  },
  ...
]
```

**Características:**

- ✅ Filtra facturas donde `authorizationSri IS NULL`
- ✅ Retorna lista de DTOs (sin relaciones circulares)
- ✅ Incluye información básica de la factura sin entidades anidadas
- ✅ Incluye nombre del método de pago (paymentMethodType)

**Logs:** Registra cantidad de borradores encontrados

---

### 3. **GET /api/invoice/draft/{id}** - Obtener Borrador Específico

**Descripción:** Retorna los datos completos de un borrador específico por su ID para permitir su edición.

**Path Parameter:** `id` - ID de la factura borrador (Long)

**Response:** `ResponseEntity<InvoiceDto>`

```json
{
  "idInvoice": 21,
  "idCompany": 1,
  "idClient": 5,
  "status": "DRAFT",
  "totalAmount": 1250.50,
  "invoiceDetails": [...],
  "clientDto": {...},
  "paymentMethod": {...}
}
```

**Características:**

- ✅ Valida que la factura exista
- ✅ Valida que sea un borrador (`authorizationSri == NULL`)
- ✅ Retorna DTO completo con todas las relaciones
- ✅ Listo para cargar en formulario de edición

**Casos de Error:**

- **400:** Factura no encontrada o ya fue procesada

**Logs:** Registra la consulta y detalles encontrados

**Documentación detallada:** Ver `ENDPOINT-GET-DRAFT-BY-ID.md`

---

### 4. **POST /api/invoice/draft/{id}/process** - Procesar Borrador

**Descripción:** Convierte un borrador en factura final: genera secuencial, firma digitalmente y envía al SRI.

**Path Parameter:** `id` - ID de la factura borrador

**Response:** `InvoiceResponse`

**Caso Exitoso:**

```json
{
  "code": 200,
  "status": "OK",
  "message": "Borrador procesado y enviado al SRI exitosamente",
  "invoiceDto": { ... }
}
```

**Casos de Error:**

1. **Factura no encontrada:**

```json
{
  "code": 400,
  "status": "BAD_REQUEST",
  "message": "Factura con ID {id} no encontrada"
}
```

2. **No es un borrador:**

```json
{
  "code": 400,
  "status": "BAD_REQUEST",
  "message": "La factura no es un borrador, ya fue procesada"
}
```

3. **Falta punto de emisión:**

```json
{
  "code": 400,
  "status": "BAD_REQUEST",
  "message": "El ID del punto de emisión es requerido para generar el secuencial de la factura."
}
```

4. **Error firma digital:**

```json
{
  "code": 400,
  "status": "BAD_REQUEST",
  "message": "La firma digital caduca y/o no está configurada comuníquese con el administrador."
}
```

5. **Error SRI:**

```json
{
  "code": 400,
  "status": "BAD_REQUEST",
  "message": "Hubo problemas para conectarse con el SRI."
}
```

**Características:**

- ✅ Valida que sea un borrador (`authorizationSri == NULL`)
- ✅ Valida `idIssuancePoint`
- ✅ Genera secuencial automático
- ✅ Firma digitalmente
- ✅ Envía al SRI
- ✅ Actualiza `authorizationSri` (ya no es borrador)
- ✅ Transaccional (`@Transactional`)

**Logs:** Registra todo el proceso con detalles de éxito/error

---

### 5. **DELETE /api/invoice/draft/{id}** - Eliminar Borrador

**Descripción:** Elimina un borrador de factura que NO ha sido autorizado por el SRI.

**Path Parameter:** `id` (Long) - ID del borrador a eliminar

**Response:** `Map<String, Object>`

```json
{
  "code": 200,
  "status": "OK",
  "message": "Borrador eliminado exitosamente"
}
```

**Errores:**

- **400 BAD REQUEST:** Si el borrador no existe o ya fue autorizado

```json
{
  "code": 400,
  "status": "ERROR",
  "message": "No se puede eliminar esta factura porque ya fue autorizada por el SRI"
}
```

**Características:**

- ✅ Valida que el borrador exista
- ✅ Valida que sea un borrador (`authorizationSri == NULL`)
- ✅ Elimina en cascada los detalles de la factura
- ✅ Operación permanente (no reversible)
- ✅ Logs con emojis (🗑️ ✅ ❌)

**Logs:** Registra la eliminación con información detallada

**Ver documentación completa:** [ENDPOINT-DELETE-DRAFT.md](./ENDPOINT-DELETE-DRAFT.md)

---

## 🗂️ Cambios en el Código

### Archivos Modificados:

1. **`IInvoiceRepo.java`**

   - ➕ Método: `List<Invoice> findByAuthorizationSriIsNull()`

2. **`InvoiceDraftResponseDto.java`** _(nuevo)_

   - ➕ DTO para respuesta de borradores (evita referencias circulares)
   - Incluye: idInvoice, idCompany, idClient, fechas, números, montos, retryCount, paymentMethodName, status

3. **`IInvoiceService.java`**

   - ➕ Método: `InvoiceResponse saveInvoiceDraft(InvoiceDto invoiceDto)`
   - ➕ Método: `List<InvoiceDraftResponseDto> getDraftInvoices()`
   - ➕ Método: `InvoiceDto getDraftInvoiceById(Long idInvoice)`
   - ➕ Método: `InvoiceResponse processDraftInvoice(Long idInvoice)`
   - ➕ Método: `void deleteDraft(Long idInvoice)` **[NUEVO]**

4. **`InvoiceServiceImpl.java`**

   - ➕ Implementación: `saveInvoiceDraft()` - Guarda borrador sin procesar
   - ➕ Implementación: `getDraftInvoices()` - Lista borradores y mapea a DTO
   - ➕ Implementación: `getDraftInvoiceById()` - Obtiene borrador por ID con datos completos
   - ➕ Método privado: `mapToDraftResponseDto()` - Mapea Invoice a InvoiceDraftResponseDto
   - ➕ Implementación: `processDraftInvoice()` - Procesa borrador completo
   - ➕ Implementación: `deleteDraft()` - Elimina borrador con validaciones **[NUEVO]**

5. **`InvoiceController.java`**

   - ➕ Endpoint: `POST /api/invoice/draft`
   - ➕ Endpoint: `GET /api/invoice/drafts` (retorna List<InvoiceDraftResponseDto>)
   - ➕ Endpoint: `GET /api/invoice/draft/{id}` (retorna InvoiceDto completo)
   - ➕ Endpoint: `POST /api/invoice/draft/{id}/process`
   - ➕ Endpoint: `DELETE /api/invoice/draft/{id}` (elimina borrador) **[NUEVO]**

6. **`ENDPOINT-GET-DRAFT-BY-ID.md`** _(nuevo)_

   - Documentación detallada del endpoint de obtención de borrador por ID

7. **`ENDPOINT-DELETE-DRAFT.md`** _(nuevo)_ **[NUEVO]**

   - Documentación detallada del endpoint de eliminación de borradores

8. **`ClientDtoResponse.java`** _(nuevo)_ **[NUEVO]**

   - DTO para deserializar respuesta de `ebill-customer`

9. **`CustomerClient.java`** _(nuevo)_ **[NUEVO]**

   - Feign Client para comunicación con `ebill-customer`

10. **`FIX-CLIENT-DATA-DESERIALIZATION.md`** _(nuevo)_ **[NUEVO]**
    - Documentación del fix para deserialización de datos del cliente

---

## 🔄 Flujo de Uso

```
1. GUARDAR BORRADOR
   Cliente → POST /api/invoice/draft
   ↓
   Se guarda Invoice + InvoiceDetails
   ↓
   authorizationSri = NULL (indicador de borrador)
   sequential = NULL

2. CONSULTAR BORRADORES
   Cliente → GET /api/invoice/drafts
   ↓
   Retorna lista de facturas con authorizationSri = NULL

3. OBTENER BORRADOR PARA EDICIÓN [NUEVO]
   Cliente → GET /api/invoice/draft/{id}
   ↓
   Retorna datos completos del borrador
   ↓
   Frontend carga datos en formulario

4. PROCESAR BORRADOR
   Cliente → POST /api/invoice/draft/{id}/process
   ↓
   Valida que sea borrador
   ↓
   Genera secuencial
   ↓
   Firma digitalmente
   ↓
   Envía al SRI
   ↓
   Actualiza authorizationSri (ya NO es borrador)

5. ELIMINAR BORRADOR (OPCIONAL)
   Cliente → DELETE /api/invoice/draft/{id}
   ↓
   Valida que sea borrador
   ↓
   Elimina factura y detalles
   ↓
   Retorna confirmación
```

---

## ✅ Ventajas de esta Implementación

1. **Separación de responsabilidades:** Código limpio y específico para borradores
2. **Reutilización:** Usa la lógica existente de secuenciales y envío al SRI
3. **Identificación simple:** `authorizationSri == NULL` = borrador
4. **No invasivo:** No modifica funcionalidad existente
5. **Transaccional:** Procesamiento atómico de borradores
6. **Logs completos:** Trazabilidad total del flujo
7. **Manejo de errores:** Validaciones robustas en cada paso
8. **Sin referencias circulares:** Uso de DTOs evita problemas de serialización JSON
9. **Performance optimizado:** Solo retorna datos necesarios sin relaciones profundas

---

## 🧪 Testing

### Ejemplo de uso con cURL:

**1. Guardar borrador:**

```bash
curl -X POST http://localhost:8080/api/invoice/draft \
  -H "Content-Type: application/json" \
  -d '{
    "idCompany": 1,
    "idClient": 100,
    "companyDto": {...},
    "clientDto": {...},
    "invoiceDetails": [...]
  }'
```

**2. Listar borradores:**

```bash
curl -X GET http://localhost:8080/api/invoice/drafts
```

**3. Obtener borrador específico para edición:**

```bash
curl -X GET http://localhost:8080/api/invoice/draft/123 \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

**4. Procesar borrador:**

```bash
curl -X POST http://localhost:8080/api/invoice/draft/123/process \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

**5. Eliminar borrador:** **[NUEVO]**

```bash
curl -X DELETE http://localhost:8080/api/invoice/draft/123 \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

---

## 📊 Base de Datos

### Campos relevantes en tabla `invoice`:

| Campo              | Borrador       | Factura Final      |
| ------------------ | -------------- | ------------------ |
| `authorizationSri` | `NULL`         | `NOT NULL`         |
| `sequential`       | `NULL`         | `NOT NULL`         |
| `idIssuancePoint`  | `NULL` o valor | `NOT NULL`         |
| `retryCount`       | `0`            | `0` o incrementado |

---

## 🔐 Seguridad y Validaciones

- ✅ Validación de existencia de factura
- ✅ Validación de estado borrador
- ✅ Validación de punto de emisión
- ✅ Validación de firma digital
- ✅ Manejo de excepciones completo
- ✅ Logs de auditoría

---

**Fecha de implementación:** Octubre 2025  
**Versión:** 1.0
