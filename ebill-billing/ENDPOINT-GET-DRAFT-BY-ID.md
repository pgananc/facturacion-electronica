# Endpoint GET /api/invoice/draft/{id} - Obtener Borrador por ID

## 📋 Resumen

Se ha implementado el endpoint para **obtener un borrador específico por ID** que permite al frontend cargar un borrador existente para continuar editándolo.

---

## 🚀 Endpoint Implementado

### **GET /api/invoice/draft/{id}** - Obtener Borrador Específico

**Descripción:** Retorna los datos completos de un borrador de factura específico por su ID, incluyendo todos los detalles y relaciones necesarias para edición.

**Path Parameter:** `id` - ID de la factura borrador (Long)

**Response:** `ResponseEntity<InvoiceDto>`

```json
{
  "idInvoice": 21,
  "idCompany": 1,
  "idClient": 5,
  "totalAmount": 1250.50,
  "discount": 50.00,
  "tip": 0.00,
  "subtotalZero": 1000.00,
  "subtotalIva": 200.50,
  "subtotalIce": 0.00,
  "subtotalDiscount": 50.00,
  "status": "DRAFT",
  "issueDate": "2025-10-18T15:30:00",
  "creationDate": "2025-10-18T10:15:00",
  "additionalInformation": "Información adicional",
  "environment": "PRUEBAS",
  "idPaymentMethod": 1,
  "paymentMethod": {
    "idPaymentMethod": 1,
    "paymentMethodType": "EFECTIVO",
    "paymentMethodCode": "01",
    "status": true
  },
  "clientDto": {
    "idClient": 5,
    "identification": "1234567890001",
    "idType": 1,
    "name": "Cliente ABC S.A.",
    "address": "Av. Principal 123",
    "phone": "0999999999",
    "email": "cliente@example.com"
  },
  "companyDto": {
    "idCompany": 1,
    "name": "Mi Empresa S.A.",
    ...
  },
  "establishment": {
    "idEstablishment": 1,
    "code": "001",
    "address": "Matriz - Av. Principal"
  },
  "invoiceDetails": [
    {
      "idInvoiceDetail": 101,
      "idProduct": 1,
      "unitPrice": 100.00,
      "discount": 5.00,
      "subtotal": 95.00,
      "quantity": 10,
      "productDto": {
        "idProduct": 1,
        "name": "Producto de prueba",
        "description": "Descripción del producto",
        "unitPrice": 100.00
      }
    }
  ]
}
```

**Características:**

- ✅ Valida que la factura exista
- ✅ Valida que sea un borrador (`authorizationSri == NULL`)
- ✅ Retorna DTO completo con todas las relaciones
- ✅ Incluye detalles de productos (`invoiceDetails`)
- ✅ Incluye datos del cliente (`clientDto`)
- ✅ Incluye método de pago (`paymentMethod`)
- ✅ Incluye establecimiento (`establishment`)

**Logs:** Registra la consulta y cantidad de detalles encontrados

---

## 📝 Casos de Respuesta

### 1. **Éxito (200 OK)**

```json
{
  "idInvoice": 21,
  "status": "DRAFT",
  "invoiceDetails": [...],
  ...
}
```

### 2. **Borrador no encontrado (400 Bad Request)**

**Condición:** El ID no existe en la base de datos

```json
{
  "timestamp": "2025-10-18T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Factura con ID 99999 no encontrada",
  "path": "/api/invoice/draft/99999"
}
```

### 3. **No es un borrador (400 Bad Request)**

**Condición:** La factura ya fue procesada y tiene `authorizationSri != NULL`

```json
{
  "timestamp": "2025-10-18T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "La factura no es un borrador, ya fue procesada y autorizada por el SRI",
  "path": "/api/invoice/draft/15"
}
```

---

## 🗂️ Cambios Implementados

### 1. **IInvoiceService.java**

```java
InvoiceDto getDraftInvoiceById(Long idInvoice);
```

### 2. **InvoiceServiceImpl.java**

- ✅ Método `getDraftInvoiceById()` implementado
- Busca factura por ID
- Valida que sea borrador (`authorizationSri == NULL`)
- Usa `serviceMapper.toInvoiceDto()` como base
- Enriquece con campos adicionales:
  - `idClient`, `idCompany`
  - `creationDate`, `issueDate`
  - `idPaymentMethod`, `paymentMethod`
  - `status = "DRAFT"`

### 3. **InvoiceController.java**

```java
@GetMapping("/draft/{id}")
public ResponseEntity<InvoiceDto> getDraftById(@PathVariable("id") Long idInvoice)
```

- Path: `GET /api/invoice/draft/{id}`
- Logging completo de request/response
- Retorna `InvoiceDto` con status 200 OK

---

## 🧪 Testing

### Prueba 1: Borrador Existente

```bash
curl -X GET http://localhost:8480/api/invoice/draft/21 \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

**Resultado esperado:** HTTP 200 con JSON completo del borrador

### Prueba 2: Borrador No Encontrado

```bash
curl -X GET http://localhost:8480/api/invoice/draft/99999 \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

**Resultado esperado:** HTTP 400 con mensaje "Factura con ID 99999 no encontrada"

### Prueba 3: Factura Ya Procesada

```bash
curl -X GET http://localhost:8480/api/invoice/draft/15 \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

(Donde ID 15 es una factura con `authorizationSri != NULL`)

**Resultado esperado:** HTTP 400 con mensaje "La factura no es un borrador..."

---

## 🔄 Flujo de Uso

```
1. LISTAR BORRADORES
   GET /api/invoice/drafts
   ↓
   Retorna lista resumida de borradores

2. SELECCIONAR BORRADOR
   Usuario selecciona un borrador de la lista
   ↓
   GET /api/invoice/draft/{id}
   ↓
   Retorna datos completos para edición

3. EDITAR EN FRONTEND
   Frontend carga datos en formulario
   ↓
   Usuario modifica datos
   ↓
   PUT /api/invoice/draft (guardar cambios) <- Futura implementación

4. PROCESAR BORRADOR
   POST /api/invoice/draft/{id}/process
   ↓
   Genera secuencial, firma y envía al SRI
```

---

## ⚠️ Nota Importante - Estructura Real vs Requerimiento Original

El requerimiento original asumía una estructura de base de datos con entidad `Payment` separada y campos adicionales en `InvoiceDto` que NO existen en la estructura real del proyecto.

**Estructura REAL del proyecto:**

- ✅ `Invoice` tiene un `PaymentMethod` directo (ManyToOne)
- ✅ NO hay entidad `Payment` separada
- ✅ NO hay array de `payments` en `Invoice`
- ✅ `InvoiceDto` NO tiene campos de buyer separados (usa `ClientDto`)

**Implementación realizada:**

- ✅ Adaptada a la estructura real
- ✅ Retorna `PaymentMethod` directo
- ✅ Usa `ClientDto` para información del cliente
- ✅ Funcional con el modelo de datos existente

---

## ✅ Ventajas de esta Implementación

1. **Compatible con estructura existente:** No requiere cambios en BD
2. **Reutiliza mapper existente:** Usa `serviceMapper.toInvoiceDto()`
3. **Validaciones robustas:** Verifica existencia y estado de borrador
4. **Logging completo:** Trazabilidad de todas las operaciones
5. **Manejo de errores claro:** Mensajes descriptivos
6. **Listo para usar:** Sin dependencias adicionales

---

**Fecha de implementación:** Octubre 2025  
**Estado:** ✅ Completado y funcional  
**Version:** 1.0
