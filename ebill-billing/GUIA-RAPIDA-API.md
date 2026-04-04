# 🚀 Guía Rápida de Endpoints - EBill Billing

**URL Base:** `http://localhost:8480`

---

## 📋 Resumen de Endpoints

| Método | Endpoint              | Descripción                 | Auth             |
| ------ | --------------------- | --------------------------- | ---------------- |
| GET    | `/api/invoice`        | Listar todas las facturas   | 🔒 JWT Requerido |
| POST   | `/api/invoice/search` | Buscar facturas con filtros | 🔒 JWT Requerido |
| POST   | `/api/invoice`        | Crear nueva factura         | 🔒 JWT Requerido |
| GET    | `/api/payment-method` | Listar formas de pago       | ✅ Público       |

---

## 🔐 Autenticación

- **Endpoints protegidos:** Requieren JWT token en header `Authorization: Bearer <token>`
- **Endpoints públicos:** No requieren autenticación

---

## 1️⃣ Listar Facturas

### GET `/api/invoice`

🔒 **Requiere JWT**

**Request:**

```bash
# Requiere JWT token
curl http://localhost:8480/api/invoice \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Response:**

```json
[
  {
    "idInvoice": 1,
    "invoiceNumber": "001-001-000000001",
    "totalAmount": 112.0,
    "issueDate": "2025-10-12T18:21:45",
    "paymentMethod": {
      "paymentMethodCode": "01"
    }
  }
]
```

---

## 2️⃣ Buscar Facturas

### POST `/api/invoice/search`

🔒 **Requiere JWT**

**Request:**

```bash
# Buscar por número de factura
curl -X POST "http://localhost:8480/api/invoice/search" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{"invoiceNumber": "001"}'

# Buscar por cliente con paginación
curl -X POST "http://localhost:8480/api/invoice/search?page=0&size=10" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{"idClient": 500}'

# Buscar por rango de fechas (solo desde)
curl -X POST "http://localhost:8480/api/invoice/search" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{"creationDateFrom": "2025-10-01T00:00:00"}'

# Buscar por rango de fechas (solo hasta)
curl -X POST "http://localhost:8480/api/invoice/search" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{"creationDateTo": "2025-10-31T23:59:59"}'

# Buscar con múltiples filtros y paginación
curl -X POST "http://localhost:8480/api/invoice/search?page=0&size=5&sort=createdAt,desc" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "invoiceNumber": "001",
    "idClient": 500,
    "creationDateFrom": "2025-10-01T00:00:00",
    "creationDateTo": "2025-10-31T23:59:59",
    "receptionStatusSri": "RECIBIDA"
  }'
```

**Response:**

```json
{
  "content": [
    {
      "idInvoice": 1,
      "invoiceNumber": "001-001-000000001",
      "totalAmount": 112.0,
      "idClient": 500
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 10,
  "number": 0
}
```

**Nota sobre Paginación:**

- Spring Boot automáticamente convierte los query parameters `page`, `size` y `sort` en un objeto `Pageable`
- Esta es la forma estándar y recomendada para manejar paginación en Spring Boot

---

## 3️⃣ Crear Factura

### POST `/api/invoice`

🔒 **Requiere JWT**

**Request Mínimo:**

```bash
curl -X POST http://localhost:8480/api/invoice \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
  "companyDto": {
    "idCompany": 1,
    "idType": 1,
    "identification": "1234567890001",
    "name": "MI EMPRESA S.A.",
    "status": true
  },
  "paymentMethod": {
    "paymentMethodCode": "01",
    "paymentMethodType": "SIN UTILIZACION DEL SISTEMA FINANCIERO"
  },
  "clientDto": {
    "idClient": 100,
    "idType": 1,
    "identification": "0987654321",
    "name": "JUAN PEREZ",
    "clientType": 1,
    "status": true
  },
  "productDto": {
    "idProduct": 50,
    "mainCode": "PROD001",
    "name": "Producto",
    "unitPrice": 100.00,
    "status": true,
    "rate": 0.12,
    "taxCode": "2"
  },
  "totalAmount": 112.00,
  "subtotalIva": 100.00,
  "discount": 0.00,
  "tip": 0.00,
  "invoiceDetails": [
    {
      "unitPrice": 100.00,
      "quantity": 1,
      "subtotal": 100.00
    }
  ],
  "establishment": {
    "name": "MATRIZ",
    "code": "001",
    "address": "Av. Principal 123",
    "issuancePoint": {
      "code": "001",
      "sequentialInvoice": "000000001"
    }
  },
  "password": "passwordFirmaDigital"
}'
```

**Response Exitosa:**

```json
{
  "code": 200,
  "status": "OK",
  "message": "Factura autorizada",
  "invoiceDto": {
    "idInvoice": 1,
    "invoiceNumber": "001-001-000000001",
    "accessKey": "1210202501...",
    "authorizationNumber": "1210202501...",
    "status": "AUTORIZADO"
  },
  "idInvoice": 1
}
```

**Response Error:**

```json
{
  "code": 400,
  "status": "ERROR",
  "message": "Error de validación"
}
```

---

## 3️⃣ Listar Formas de Pago

### GET `/api/payment-method`

✅ **Público (no requiere JWT)**

**Request:**

```bash
curl http://localhost:8480/api/payment-method
```

**Response:**

```json
[
  {
    "idPaymentMethod": 1,
    "paymentMethodType": "SIN UTILIZACION DEL SISTEMA FINANCIERO",
    "paymentMethodCode": "01"
  },
  {
    "idPaymentMethod": 6,
    "paymentMethodType": "TARJETA DE CRÉDITO",
    "paymentMethodCode": "19"
  }
]
```

**Uso:**

```javascript
// Obtener métodos de pago para un dropdown
const methods = await fetch("http://localhost:8480/api/payment-method").then(
  (r) => r.json()
);

// Buscar por código
const metodoPago01 = methods.find((m) => m.paymentMethodCode === "01");
```

---

## 📦 Campos Obligatorios

### Para Crear Factura:

✅ **companyDto** - Datos de la empresa

- `identification` (RUC)
- `name` (Razón social)
- `status`

✅ **clientDto** - Datos del cliente

- `identification` (Cédula/RUC)
- `name`
- `status`

✅ **paymentMethod** - Método de pago

- `paymentMethodCode` (ej: "01")

✅ **productDto** - Producto principal (legacy)

- `mainCode`
- `name`
- `unitPrice`

✅ **invoiceDetails** - Lista de productos

- `unitPrice`
- `quantity`
- `subtotal`

✅ **establishment** - Establecimiento

- `name`
- `code` (3 dígitos: "001")
- `address`
- `issuancePoint.code` (3 dígitos)
- `issuancePoint.sequentialInvoice` (9 dígitos)

✅ **password** - Contraseña firma digital

✅ **totalAmount** - Monto total

---

## 💰 Cálculo de Montos

```javascript
subtotalIva = suma de productos con IVA
subtotalZero = suma de productos sin IVA
iva = subtotalIva * 0.12
totalAmount = subtotalIva + subtotalZero + iva - discount + tip
```

**Ejemplo:**

- Producto: $100.00
- IVA 12%: $12.00
- **Total:** $112.00

---

## 🔢 Códigos de Métodos de Pago

| Código | Descripción                            |
| ------ | -------------------------------------- |
| `01`   | SIN UTILIZACION DEL SISTEMA FINANCIERO |
| `16`   | TARJETA DE DÉBITO                      |
| `17`   | DINERO ELECTRÓNICO                     |
| `19`   | TARJETA DE CRÉDITO                     |
| `20`   | OTROS CON SISTEMA FINANCIERO           |

---

## 📊 Estados de Factura

| Estado          | Descripción                   |
| --------------- | ----------------------------- |
| `AUTORIZADO`    | Factura autorizada por el SRI |
| `NO_AUTORIZADO` | Rechazada por el SRI          |
| `PENDIENTE`     | En proceso de autorización    |

---

## 🧪 Testing Rápido

### Verificar servicio activo:

```bash
curl http://localhost:8480/api/invoice
```

### Ver logs:

```bash
# En la terminal donde está corriendo la app
# Buscar líneas con "InvoiceController"
```

### Ver facturas en BD:

```sql
SELECT * FROM invoice ORDER BY created_at DESC LIMIT 10;
```

---

## ⚡ Ejemplos JavaScript

### Fetch API:

```javascript
// Listar
const invoices = await fetch("http://localhost:8480/api/invoice").then((r) =>
  r.json()
);

// Crear
const newInvoice = await fetch("http://localhost:8480/api/invoice", {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify(invoiceData),
}).then((r) => r.json());
```

### Axios:

```javascript
// Listar
const { data } = await axios.get("http://localhost:8480/api/invoice");

// Crear
const { data } = await axios.post(
  "http://localhost:8480/api/invoice",
  invoiceData
);
```

---

## 🐛 Errores Comunes

### "Cannot connect"

→ Verifica que la app esté corriendo en puerto 8480

```bash
lsof -i :8480
```

### "Validation failed"

→ Revisa que todos los campos obligatorios estén presentes

### "Digital signature error"

→ Verifica que `digital-signature-service` esté corriendo (puerto 8180)

### "Database error"

→ Confirma que PostgreSQL esté activo (puerto 5433)

```bash
lsof -i :5433
```

---

## 📁 Estructura Típica de Request

```
Invoice
├── companyDto (Empresa emisora)
│   ├── identification (RUC)
│   ├── name
│   └── contacts[]
├── clientDto (Cliente)
│   ├── identification
│   ├── name
│   └── contacts[]
├── paymentMethod (Forma de pago)
│   └── paymentMethodCode
├── invoiceDetails[] (Productos/Servicios)
│   ├── productDto
│   ├── quantity
│   ├── unitPrice
│   └── subtotal
├── establishment (Establecimiento)
│   ├── code
│   └── issuancePoint
│       ├── code
│       └── sequentialInvoice
└── password (Firma digital)
```

---

## 🎯 Checklist Pre-Request

Antes de enviar un POST, verifica:

- [ ] PostgreSQL corriendo (puerto 5433)
- [ ] App corriendo (puerto 8480)
- [ ] Digital Signature Service corriendo (puerto 8180)
- [ ] RUC de empresa válido (13 dígitos)
- [ ] Cédula/RUC de cliente válido
- [ ] Código de establecimiento (001)
- [ ] Código punto de emisión (001)
- [ ] Secuencial con 9 dígitos (000000001)
- [ ] Contraseña de firma digital
- [ ] Cálculo de montos correcto

---

## 🔗 Links Útiles

- **Documentación Completa:** Ver `API-ENDPOINTS.md`
- **Setup Proyecto:** Ver `SETUP.md`
- **Troubleshooting:** Ver `CURSOR-DEBUG.md`
- **Estado Actual:** Ver `ESTADO-ACTUAL.md`

---

**💡 Tip:** Para ver ejemplos más completos y detallados, consulta `API-ENDPOINTS.md`
