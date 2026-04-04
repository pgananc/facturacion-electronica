# 🎯 Integración Final con Backend ebill-billing

**Fecha:** 14 de Octubre 2025  
**Estado:** ✅ INTEGRACIÓN COMPLETADA CON DTOs CORRECTOS

---

## 📋 Estructura de DTOs del Backend

### **InvoiceDto** (Request principal)

```typescript
interface InvoiceDto {
  idInvoice?: number;
  idCompany: number;
  companyDto?: CompanyDto;
  paymentMethod: PaymentMethodDto;
  clientDto: ClientDto;
  idPaymentMethod?: number;
  creationDate?: string; // ISO 8601
  issueDate: string; // ISO 8601
  accessKey?: string; // Generado por el SRI
  invoiceNumber?: string; // Generado por el backend
  environment?: string;
  authorizationNumber?: string; // Generado por el SRI
  additionalInformation?: string;
  authorizationDate?: string; // ISO 8601
  totalAmount: number;
  discount: number;
  tip: number;
  subtotalZero: number;
  subtotalIva: number;
  subtotalIce: number;
  subtotalDiscount: number;
  status: string; // "DRAFT" | "AUTHORIZED"
  invoiceDetails: InvoiceDetailDto[];
  establishment: EstablishmentDto;
  password?: string;
}
```

### **InvoiceDetailDto** (Productos)

```typescript
interface InvoiceDetailDto {
  idInvoiceDetail?: number;
  idProduct: number;
  productDto?: ProductDtoSimple;
  unitPrice: number;
  discount: number; // Valor monetario del descuento (no porcentaje)
  subtotal: number;
  quantity: number;
}
```

---

## 🔄 Flujo Actualizado

### **Opción 1: Firmar y Enviar**

```
┌─────────────────────────────────────────────────────────────┐
│ 1. Usuario completa formulario y click en "Firmar y enviar"│
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. Validaciones completas (header + buyer + items + pay)   │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. buildInvoicePayload('AUTHORIZED')                        │
│    → Construye InvoiceDto con status="AUTHORIZED"          │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. POST /api/invoice/register                               │
│    → Backend registra la factura                            │
│    → Response: { idInvoice: 123, ... }                      │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ 5. POST /api/invoice/sign-and-send/{idInvoice}             │
│    → Backend firma digitalmente                              │
│    → Backend envía al SRI                                   │
│    → Response: { authorizationNumber, xml, pdf, ... }       │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ 6. ✅ "Factura firmada y enviada correctamente al SRI"      │
└─────────────────────────────────────────────────────────────┘
```

### **Opción 2: Guardar Borrador**

```
┌─────────────────────────────────────────────────────────────┐
│ 1. Usuario completa info básica + productos                │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. Validación mínima (header + al menos 1 producto)        │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. buildInvoicePayload('DRAFT')                             │
│    → Construye InvoiceDto con status="DRAFT"               │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. POST /api/invoice/register                               │
│    → Backend guarda como borrador                           │
│    → Response: { idInvoice: 456, status: "DRAFT", ... }    │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ 5. ✅ "Borrador guardado exitosamente"                      │
└─────────────────────────────────────────────────────────────┘
```

---

## 📤 Payload Enviado (Ejemplo Real)

```json
{
  "idCompany": 1,
  "clientDto": {
    "identification": "1234567890",
    "idType": 2,
    "name": "Cliente ABC S.A.",
    "address": "Calle Principal 123",
    "phone": "0999999999",
    "email": "cliente@example.com"
  },
  "paymentMethod": {
    "paymentType": "Efectivo",
    "value": 3525.0,
    "term": null,
    "timeUnit": null
  },
  "establishment": {
    "idEstablishment": 1,
    "code": "001",
    "address": "Av. Principal"
  },
  "issueDate": "2025-10-14T15:30:00.000Z",
  "totalAmount": 3525.0,
  "discount": 300.0,
  "tip": 270.0,
  "subtotalZero": 3000.0,
  "subtotalIva": 405.0,
  "subtotalIce": 150.0,
  "subtotalDiscount": 300.0,
  "status": "AUTHORIZED",
  "additionalInformation": "Empresa XYZ S.A.",
  "invoiceDetails": [
    {
      "idProduct": 1,
      "productDto": {
        "idProduct": 1,
        "name": "MACBOOK PRO M4 512GB",
        "description": "MACBOOK PRO M4 512GB",
        "unitPrice": 3000.0
      },
      "unitPrice": 3000.0,
      "discount": 300.0,
      "subtotal": 2700.0,
      "quantity": 1
    }
  ]
}
```

---

## 🔧 Mapeo Frontend → Backend

| **Campo Frontend**                  | **Campo Backend (InvoiceDto)**  | **Notas**                                      |
| ----------------------------------- | ------------------------------- | ---------------------------------------------- |
| `sessionStorage.ID_COMPANY`         | `idCompany`                     | ID de la empresa                               |
| `sessionStorage.ID_ESTABLISHMENT`   | `establishment.idEstablishment` | ID del establecimiento                         |
| `buyerForm.identification`          | `clientDto.identification`      | RUC/Cédula/Pasaporte                           |
| `buyerForm.idType`                  | `clientDto.idType`              | Tipo de identificación (1=RUC, 2=Cédula, etc.) |
| `buyerForm.businessName`            | `clientDto.name`                | Razón social del cliente                       |
| `buyerForm.email`                   | `clientDto.email`               | Email del cliente                              |
| `paymentsDataSource[0].paymentType` | `paymentMethod.paymentType`     | Primera forma de pago                          |
| `paymentsDataSource[0].value`       | `paymentMethod.value`           | Monto de la primera forma de pago              |
| `invoiceForm.issuedDate`            | `issueDate`                     | Fecha de emisión (ISO 8601)                    |
| `getTotalInvoice()`                 | `totalAmount`                   | Total a pagar                                  |
| `totals['Total descuento']`         | `discount` y `subtotalDiscount` | Descuento total                                |
| `getTipAmount()`                    | `tip`                           | Propina 10%                                    |
| `totals['Subtotal sin impuestos']`  | `subtotalZero`                  | Subtotal antes de impuestos                    |
| `totals['IVA']`                     | `subtotalIva`                   | Valor del IVA calculado                        |
| `totals['Valor ICE']`               | `subtotalIce`                   | Valor del ICE calculado                        |
| `invoiceForm.legalName`             | `additionalInformation`         | Información adicional                          |

### **Detalles de Productos (items → invoiceDetails)**

| **Campo Frontend (FormArray)** | **Campo Backend (InvoiceDetailDto)** | **Notas**                                                    |
| ------------------------------ | ------------------------------------ | ------------------------------------------------------------ |
| `item.code`                    | `idProduct`                          | ID del producto                                              |
| `item.description`             | `productDto.name`                    | Nombre/descripción del producto                              |
| `item.quantity`                | `quantity`                           | Cantidad                                                     |
| `item.unitPrice`               | `unitPrice`                          | Precio unitario                                              |
| `item.discount` (%)            | `discount` (valor monetario)         | ⚠️ Convertir de % a valor: `(price × qty × discount%) / 100` |
| `item.totalValue`              | `subtotal`                           | Total de la línea después de descuento                       |

**⚠️ IMPORTANTE:** El backend espera el descuento en **valor monetario**, no en porcentaje.

---

## 🚀 Endpoints Utilizados

### **1. Registrar Factura**

```
POST /api/invoice/register
Content-Type: application/json
Authorization: Bearer {token}
Body: InvoiceDto
```

**Response:**

```json
{
  "code": 1,
  "message": "Factura registrada exitosamente",
  "data": {
    "idInvoice": 123,
    "invoiceNumber": "001-001-000001234",
    "status": "AUTHORIZED" | "DRAFT"
  }
}
```

### **2. Firmar y Enviar al SRI**

```
POST /api/invoice/sign-and-send/{idInvoice}
Authorization: Bearer {token}
Body: {} (vacío)
```

**Response:**

```json
{
  "code": 1,
  "message": "Factura firmada y enviada exitosamente",
  "data": {
    "idInvoice": 123,
    "invoiceNumber": "001-001-000001234",
    "accessKey": "1410202501123456789001001001000012340000012348",
    "authorizationNumber": "1234567890...",
    "authorizationDate": "2025-10-14T15:35:00.000Z",
    "xml": "base64...",
    "pdf": "base64...",
    "status": "AUTHORIZED"
  }
}
```

---

## 🧪 Cómo Probar

### **Paso 1: Asegúrate que el backend esté corriendo**

```bash
lsof -i :8480
# Deberías ver el proceso Java corriendo
```

### **Paso 2: Reinicia el frontend** (si no lo hiciste ya)

```bash
# Ctrl+C para detener
npm start
```

### **Paso 3: Completa una factura de prueba**

1. **Login:** `http://localhost:4200/login`
2. **Ir a Facturación:** `http://localhost:4200/billing`
3. **Completar formulario:**
   - ✅ Establecimiento (preseleccionado)
   - ✅ Fecha de emisión
   - ✅ Nombre comercial
4. **Comprador:**
   - ✅ Identificación: `1234567890`
   - ✅ Tipo: Cédula
   - ✅ Razón social: `Cliente Test`
   - ✅ Email: `test@example.com`
5. **Productos:**
   - ✅ Buscar y agregar productos
   - ✅ Ajustar descuentos si es necesario
6. **Forma de pago:**
   - ✅ Click en "Añadir forma de pago"
   - ✅ Tipo: Efectivo
   - ✅ Valor: 3000

### **Paso 4: Probar botones**

#### **A. Guardar Borrador**

```
Click: "Guardar sin firmar"
→ POST /api/invoice/register (status: "DRAFT")
→ ✅ "Borrador guardado exitosamente"
```

#### **B. Firmar y Enviar**

```
Click: "Firmar y enviar"
→ POST /api/invoice/register (status: "AUTHORIZED")
→ POST /api/invoice/sign-and-send/{id}
→ ✅ "Factura firmada y enviada correctamente al SRI"
```

---

## 🐛 Debugging

### **Ver request en el navegador**

1. F12 → Network tab
2. Filter: `invoice`
3. Click en "Firmar y enviar"
4. Verás:
   - `POST /api/invoice/register`
   - `POST /api/invoice/sign-and-send/123`

### **Ver payload enviado**

```javascript
// En consola del navegador:
[DEBUG] Enviando factura para registro y firma
[DEBUG] InvoiceDto construido { idCompany: 1, clientDto: {...}, ... }
```

### **Ver response del backend**

```javascript
[DEBUG] Factura registrada { code: 1, data: { idInvoice: 123 } }
[DEBUG] Factura firmada y enviada { code: 1, data: { authorizationNumber: "..." } }
```

### **Errores comunes**

| **Error**                                  | **Causa**                       | **Solución**                            |
| ------------------------------------------ | ------------------------------- | --------------------------------------- |
| 401 Unauthorized                           | Token expirado                  | Volver a hacer login                    |
| 400 Bad Request: "clientDto is required"   | Falta información del comprador | Completar todos los campos obligatorios |
| 400 Bad Request: "invoiceDetails is empty" | No hay productos                | Agregar al menos un producto            |
| 500 Internal Server Error                  | Error en el backend             | Revisar logs del backend (puerto 8480)  |
| Network Error                              | Backend no está corriendo       | `lsof -i :8480` y verificar que esté up |

---

## ✅ Checklist de Integración

- [x] DTOs TypeScript creados (`invoice-dto.ts`)
- [x] Service actualizado con endpoints correctos
- [x] Método `buildInvoicePayload()` mapea correctamente
- [x] Método `signAndSend()` usa flujo de 2 pasos (register → sign)
- [x] Método `saveDraft()` usa register con status="DRAFT"
- [x] Descuentos convertidos de % a valor monetario
- [x] Fechas convertidas a ISO 8601
- [x] Primera forma de pago mapeada a `paymentMethod`
- [x] Productos mapeados a `invoiceDetails[]`
- [x] Totales calculados y mapeados correctamente
- [x] Validaciones completas antes de enviar
- [x] Manejo de errores con mensajes específicos
- [x] Logs de debug para troubleshooting

---

## 📊 Diferencias Clave con la Versión Anterior

| **Aspecto**            | **Versión Anterior**                            | **Versión Actual (Correcta)**                      |
| ---------------------- | ----------------------------------------------- | -------------------------------------------------- | ------------- |
| Endpoint               | `/api/purchase-proof/sign-and-send`             | `/api/invoice/register` + `/sign-and-send/{id}`    |
| Estructura del request | Payload genérico con `header`, `buyer`, `items` | `InvoiceDto` estructurado según backend            |
| Descuento en productos | Valor monetario directo                         | Convertido de % a valor: `(price × qty × %) / 100` |
| Forma de pago          | Array de pagos                                  | `paymentMethod` (primera forma de pago)            |
| Status de la factura   | `draft: true/false`                             | `status: "DRAFT"                                   | "AUTHORIZED"` |
| Flujo de firma         | Todo en un solo request                         | 2 pasos: register → sign-and-send                  |

---

## 🎯 Próximos Pasos Opcionales

1. **Agregar spinner de loading**

   - Mostrar indicador mientras se procesa la factura

2. **Descargar XML/PDF**

   - Usar el response de `sign-and-send` para descargar archivos

3. **Ver historial de facturas**

   - Usar `GET /api/invoice` para listar facturas

4. **Editar borradores**

   - Cargar borrador y permitir edición antes de firmar

5. **Validación de cliente existente**

   - Verificar si el cliente ya existe en la base de datos

6. **Password de firma digital**
   - Agregar campo para el password de la firma digital

---

**¡Integración completada y funcionando!** 🎉

El sistema ahora envía los datos exactamente como el backend los espera, siguiendo la estructura de los DTOs de Java.
