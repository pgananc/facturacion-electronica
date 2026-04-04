# 📡 API Endpoints - EBill Billing Service

**Base URL:** `http://localhost:8480`  
**Versión:** 1.0.0  
**Documentación generada:** Octubre 2025

---

## 📋 Tabla de Contenidos

- [Información General](#información-general)
- [Endpoints Disponibles](#endpoints-disponibles)
  - [Invoice - Facturas](#1-invoice---facturas)
    - [Listar todas las Facturas](#11-listar-todas-las-facturas)
    - [Registrar/Crear una Factura](#12-registrarcrear-una-factura)
    - [Buscar Facturas con Filtros](#13-buscar-facturas-con-filtros)
  - [Payment Method - Formas de Pago](#2-payment-method---formas-de-pago)
- [Modelos de Datos](#modelos-de-datos)
- [Códigos de Respuesta](#códigos-de-respuesta)
- [Ejemplos de Uso](#ejemplos-de-uso)

---

## 🌐 Información General

### URL Base según Ambiente

| Ambiente       | URL Base                    | Puerto | Profile Spring |
| -------------- | --------------------------- | ------ | -------------- |
| **Localhost**  | `http://localhost:8480`     | 8480   | `localhost`    |
| **Desarrollo** | `http://85.239.237.88:8480` | 8480   | `default`      |

### Headers Comunes

```http
Content-Type: application/json
Accept: application/json
```

### Servicios Externos

El microservicio se comunica con:

- **digital-signature-service:** `http://localhost:8180` (localhost)
- **ebill-customer:** `http://localhost:8180` (localhost) | `https://85.239.237.88:444` (prod)

---

## 🚀 Endpoints Disponibles

### 1. Invoice - Facturas

Base Path: `/api/invoice`

#### 1.1. Listar todas las Facturas

**GET** `/api/invoice`

Obtiene la lista completa de todas las facturas registradas.

**Request:**

```http
GET http://localhost:8480/api/invoice
```

**Response - Success (200):**

```json
[
  {
    "idInvoice": 1,
    "idCompany": 100,
    "idClient": 500,
    "issueDate": "2025-10-12T18:21:45",
    "invoiceNumber": "001-001-000000001",
    "environment": "PRODUCCION",
    "additionalInformation": "Información adicional",
    "totalAmount": 112.0,
    "tip": 0.0,
    "subtotalZero": 0.0,
    "subtotalIva": 100.0,
    "subtotalIce": 0.0,
    "discount": 0.0,
    "paymentMethod": {
      "idPaymentMethod": 1,
      "paymentMethodType": "SIN UTILIZACION DEL SISTEMA FINANCIERO",
      "paymentMethodCode": "01"
    },
    "authorizationSri": {
      "idAuthorizationSri": 1,
      "accessKey": "1210202501...",
      "authorizationNumber": "1210202501...",
      "authorizationDate": "2025-10-12T18:22:00",
      "status": "AUTORIZADO",
      "createdAt": "2025-10-12T18:21:50",
      "updatedAt": "2025-10-12T18:22:00"
    },
    "invoiceDetails": [
      {
        "idInvoiceDetail": 1,
        "unitPrice": 100.0,
        "discount": 0.0,
        "subtotal": 100.0,
        "quantity": 1
      }
    ],
    "createdAt": "2025-10-12T18:21:45",
    "updatedAt": "2025-10-12T18:22:00"
  }
]
```

**Estructura de Respuesta:**
| Campo | Tipo | Descripción |
|-------|------|-------------|
| `idInvoice` | Long | ID único de la factura |
| `idCompany` | Long | ID de la empresa emisora |
| `idClient` | Long | ID del cliente |
| `issueDate` | LocalDateTime | Fecha de emisión |
| `invoiceNumber` | String | Número de factura (formato: 001-001-000000001) |
| `environment` | String | Ambiente (PRODUCCION/PRUEBAS) |
| `totalAmount` | BigDecimal | Monto total de la factura |
| `paymentMethod` | Object | Método de pago |
| `authorizationSri` | Object | Datos de autorización del SRI |
| `invoiceDetails` | Array | Detalles de la factura (productos/servicios) |

---

#### 1.2. Registrar/Crear una Factura

**POST** `/api/invoice`

Crea una nueva factura electrónica y la envía al SRI para su autorización.

**Request:**

```http
POST http://localhost:8480/api/invoice
Content-Type: application/json
```

**Request Body:**

```json
{
  "companyDto": {
    "idCompany": 1,
    "idType": 1,
    "identification": "1234567890001",
    "name": "EMPRESA DE PRUEBA S.A.",
    "nameCommercial": "EMPRESA PRUEBA",
    "branchOfficeCode": "001",
    "forcedToAccounting": "SI",
    "specialTaxpayer": 0,
    "principal": "SI",
    "status": true,
    "contacts": [
      {
        "contactType": "EMAIL",
        "value": "empresa@example.com",
        "status": true
      },
      {
        "contactType": "PHONE",
        "value": "0987654321",
        "status": true
      }
    ]
  },
  "paymentMethod": {
    "idPaymentMethod": 1,
    "paymentMethodType": "SIN UTILIZACION DEL SISTEMA FINANCIERO",
    "paymentMethodCode": "01"
  },
  "clientDto": {
    "idClient": 100,
    "idType": 1,
    "identification": "0987654321001",
    "name": "CLIENTE DE PRUEBA S.A.",
    "clientType": 1,
    "status": true,
    "idCompany": 1,
    "companyIdentification": "1234567890001",
    "contacts": [
      {
        "contactType": "EMAIL",
        "value": "cliente@example.com",
        "status": true
      }
    ]
  },
  "productDto": {
    "idProduct": 50,
    "mainCode": "001",
    "auxiliarCode": "AUX001",
    "productType": 1,
    "name": "PRODUCTO DE PRUEBA",
    "description": "Descripción del producto",
    "unitPrice": 100.0,
    "discount": 0.0,
    "status": true,
    "idCompany": 1,
    "rate": 0.12,
    "taxCode": "2",
    "taxCodPercentage": "12%"
  },
  "issueDate": "2025-10-12T18:21:45",
  "environment": "PRODUCCION",
  "additionalInformation": "Información adicional de la factura",
  "totalAmount": 112.0,
  "discount": 0.0,
  "tip": 0.0,
  "subtotalZero": 0.0,
  "subtotalIva": 100.0,
  "subtotalIce": 0.0,
  "subtotalDiscount": 0.0,
  "invoiceDetails": [
    {
      "idProduct": 50,
      "productDto": {
        "idProduct": 50,
        "mainCode": "001",
        "auxiliarCode": "AUX001",
        "name": "PRODUCTO DE PRUEBA",
        "description": "Descripción del producto",
        "unitPrice": 100.0,
        "discount": 0.0,
        "rate": 0.12,
        "taxCode": "2",
        "taxCodPercentage": "12%"
      },
      "unitPrice": 100.0,
      "discount": 0.0,
      "subtotal": 100.0,
      "quantity": 1
    }
  ],
  "establishment": {
    "name": "MATRIZ",
    "code": "001",
    "address": "Av. Principal 123 y Secundaria",
    "issuancePoint": {
      "code": "001",
      "sequentialInvoice": "000000001"
    }
  },
  "password": "passwordFirmaDigital123"
}
```

**Response - Success (200):**

```json
{
  "code": 200,
  "status": "OK",
  "message": "Factura registrada y autorizada exitosamente",
  "invoiceDto": {
    "idInvoice": 1,
    "idCompany": 1,
    "companyDto": {
      "idCompany": 1,
      "identification": "1234567890001",
      "name": "EMPRESA DE PRUEBA S.A."
    },
    "clientDto": {
      "idClient": 100,
      "identification": "0987654321001",
      "name": "CLIENTE DE PRUEBA S.A."
    },
    "invoiceNumber": "001-001-000000001",
    "accessKey": "1210202501123456789000110010010000000011234567814",
    "authorizationNumber": "1210202501123456789000110010010000000011234567814",
    "authorizationDate": "2025-10-12T18:22:00",
    "totalAmount": 112.0,
    "status": "AUTORIZADO"
  },
  "idInvoice": 1
}
```

**Response - Error (400):**

```json
{
  "code": 400,
  "status": "ERROR",
  "message": "Error de validación: [detalles del error]"
}
```

**Response - Error (500):**

```json
{
  "code": 500,
  "status": "ERROR",
  "message": "Error interno del servidor al procesar la factura"
}
```

**Campos Requeridos (Validación):**
| Campo | Requerido | Descripción |
|-------|-----------|-------------|
| `companyDto` | ✅ | Información de la empresa emisora |
| `paymentMethod` | ✅ | Método de pago de la factura |
| `clientDto` | ✅ | Información del cliente |
| `productDto` | ✅ | Producto principal (legacy) |
| `invoiceDetails` | ✅ | Lista de productos/servicios |
| `establishment` | ✅ | Establecimiento y punto de emisión |
| `password` | ✅ | Contraseña de la firma digital |

---

#### 1.3. Buscar Facturas con Filtros

**POST** `/api/invoice/search`

Busca facturas utilizando múltiples criterios de filtrado con soporte para paginación. Todos los parámetros son opcionales.

**Request Body (JSON):**

```json
{
  "invoiceNumber": "001",
  "idClient": 500,
  "creationDateFrom": "2025-10-01T00:00:00",
  "creationDateTo": "2025-10-31T23:59:59",
  "receptionStatusSri": "RECIBIDA"
}
```

**Estructura del Request Body:**

| Campo                | Tipo          | Requerido | Descripción                                                |
| -------------------- | ------------- | --------- | ---------------------------------------------------------- |
| `invoiceNumber`      | String        | ❌        | Número de factura (búsqueda parcial con LIKE)              |
| `idClient`           | Long          | ❌        | ID del cliente                                             |
| `creationDateFrom`   | LocalDateTime | ❌        | Fecha de creación desde (formato ISO: 2025-10-01T00:00:00) |
| `creationDateTo`     | LocalDateTime | ❌        | Fecha de creación hasta (formato ISO: 2025-10-31T23:59:59) |
| `receptionStatusSri` | String        | ❌        | Estado de recepción en SRI                                 |

**Parámetros de Consulta (Query Parameters) - Paginación:**

Spring Boot automáticamente convierte los siguientes parámetros de consulta en un objeto `Pageable`:

| Parámetro | Tipo    | Requerido | Descripción                                  |
| --------- | ------- | --------- | -------------------------------------------- |
| `page`    | Integer | ❌        | Número de página (default: 0)                |
| `size`    | Integer | ❌        | Tamaño de página (default: 20)               |
| `sort`    | String  | ❌        | Campo de ordenamiento (ej: `idInvoice,desc`) |

**Request:**

```http
POST http://localhost:8480/api/invoice/search?page=0&size=10&sort=idInvoice,desc
Content-Type: application/json

{
  "invoiceNumber": "001",
  "idClient": 500
}
```

**Request con rango de fechas:**

```http
POST http://localhost:8480/api/invoice/search?page=0&size=20
Content-Type: application/json

{
  "creationDateFrom": "2025-10-01T00:00:00",
  "creationDateTo": "2025-10-31T23:59:59",
  "receptionStatusSri": "RECIBIDA"
}
```

**Response - Success (200):**

```json
{
  "content": [
    {
      "invoiceNumber": "001-001-000000001",
      "creationDate": "2025-10-12T18:21:45",
      "idClient": 500,
      "total": 112.0,
      "receptionStatusSri": "RECIBIDA"
    }
  ],
  "pageable": {
    "sort": {
      "sorted": true,
      "unsorted": false,
      "empty": false
    },
    "pageNumber": 0,
    "pageSize": 10,
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "totalElements": 1,
  "totalPages": 1,
  "last": true,
  "first": true,
  "numberOfElements": 1,
  "size": 10,
  "number": 0,
  "sort": {
    "sorted": true,
    "unsorted": false,
    "empty": false
  },
  "empty": false
}
```

**Estructura de Respuesta Paginada:**

| Campo           | Tipo    | Descripción                                      |
| --------------- | ------- | ------------------------------------------------ |
| `content`       | Array   | Lista de facturas encontradas                    |
| `totalElements` | Long    | Total de registros que coinciden con los filtros |
| `totalPages`    | Integer | Total de páginas disponibles                     |
| `size`          | Integer | Tamaño de página solicitado                      |
| `number`        | Integer | Número de página actual (0-based)                |
| `first`         | Boolean | Indica si es la primera página                   |
| `last`          | Boolean | Indica si es la última página                    |
| `empty`         | Boolean | Indica si no hay resultados                      |

**Estructura de Respuesta:**

| Campo                | Tipo          | Descripción                                                  |
| -------------------- | ------------- | ------------------------------------------------------------ |
| `invoiceNumber`      | String        | Número de factura (formato: 001-001-000000001)               |
| `creationDate`       | LocalDateTime | Fecha de creación de la factura                              |
| `idClient`           | Long          | ID del cliente                                               |
| `total`              | BigDecimal    | Monto total de la factura                                    |
| `receptionStatusSri` | String        | Estado de recepción en SRI (RECIBIDA, DEVUELTA, NO RECIBIDA) |

**Ejemplos de Uso:**

```bash
# Buscar por número de factura
curl -X POST "http://localhost:8480/api/invoice/search" \
  -H "Content-Type: application/json" \
  -d '{"invoiceNumber": "001"}'

# Buscar por cliente específico
curl -X POST "http://localhost:8480/api/invoice/search" \
  -H "Content-Type: application/json" \
  -d '{"idClient": 500}'

# Buscar por rango de fechas de creación
curl -X POST "http://localhost:8480/api/invoice/search" \
  -H "Content-Type: application/json" \
  -d '{
    "creationDateFrom": "2025-10-01T00:00:00",
    "creationDateTo": "2025-10-31T23:59:59"
  }'

# Buscar por estado de recepción SRI
curl -X POST "http://localhost:8480/api/invoice/search" \
  -H "Content-Type: application/json" \
  -d '{"receptionStatusSri": "RECIBIDA"}'

# Combinar múltiples filtros con paginación
curl -X POST "http://localhost:8480/api/invoice/search?page=0&size=5&sort=createdAt,desc" \
  -H "Content-Type: application/json" \
  -d '{
    "invoiceNumber": "001",
    "idClient": 500,
    "creationDateFrom": "2025-10-01T00:00:00",
    "creationDateTo": "2025-10-31T23:59:59"
  }'

# Buscar facturas de un cliente específico ordenadas por fecha
curl -X POST "http://localhost:8480/api/invoice/search?sort=createdAt,desc&size=20" \
  -H "Content-Type: application/json" \
  -d '{"idClient": 500}'

# Buscar todas las facturas (sin filtros)
curl -X POST "http://localhost:8480/api/invoice/search" \
  -H "Content-Type: application/json" \
  -d '{}'
```

**Notas:**

- Todos los parámetros de filtro son opcionales
- Si no se especifica ningún filtro, retorna todas las facturas paginadas
- La búsqueda por `invoiceNumber` utiliza LIKE, por lo que admite búsquedas parciales
- Las fechas de creación deben estar en formato ISO 8601: `YYYY-MM-DDTHH:mm:ss`
- Se puede usar solo `creationDateFrom` (desde una fecha), solo `creationDateTo` (hasta una fecha), o ambos para un rango
- Los valores de `receptionStatusSri` comunes son: `RECIBIDA`, `DEVUELTA`, `NO RECIBIDA`
- **Paginación**: Spring Boot automáticamente convierte los query parameters `page`, `size` y `sort` en un objeto `Pageable`
- **Mejores prácticas**: El uso de `Pageable` es la forma estándar y recomendada para manejar paginación en Spring Boot
- **Consulta nativa optimizada**: Se utiliza SQL nativo con COALESCE para manejo robusto de parámetros NULL
- **Performance optimizada**: Consulta eficiente que carga solo los datos necesarios, excluyendo `invoiceDetails` por lazy loading

---

### 2. Payment Method - Formas de Pago

Base Path: `/api/payment-method`

#### 2.1. Listar todas las Formas de Pago

**GET** `/api/payment-method`

Obtiene la lista completa de todos los métodos de pago disponibles registrados en el sistema.

**Request:**

```http
GET http://localhost:8480/api/payment-method
```

**Response - Success (200):**

```json
[
  {
    "idPaymentMethod": 1,
    "paymentMethodType": "SIN UTILIZACION DEL SISTEMA FINANCIERO",
    "paymentMethodCode": "01",
    "createdAt": "2025-01-01T10:00:00",
    "updatedAt": "2025-01-01T10:00:00"
  },
  {
    "idPaymentMethod": 2,
    "paymentMethodType": "COMPENSACIÓN DE DEUDAS",
    "paymentMethodCode": "15",
    "createdAt": "2025-01-01T10:00:00",
    "updatedAt": "2025-01-01T10:00:00"
  },
  {
    "idPaymentMethod": 3,
    "paymentMethodType": "TARJETA DE DÉBITO",
    "paymentMethodCode": "16",
    "createdAt": "2025-01-01T10:00:00",
    "updatedAt": "2025-01-01T10:00:00"
  },
  {
    "idPaymentMethod": 4,
    "paymentMethodType": "DINERO ELECTRÓNICO",
    "paymentMethodCode": "17",
    "createdAt": "2025-01-01T10:00:00",
    "updatedAt": "2025-01-01T10:00:00"
  },
  {
    "idPaymentMethod": 5,
    "paymentMethodType": "TARJETA PREPAGO",
    "paymentMethodCode": "18",
    "createdAt": "2025-01-01T10:00:00",
    "updatedAt": "2025-01-01T10:00:00"
  },
  {
    "idPaymentMethod": 6,
    "paymentMethodType": "TARJETA DE CRÉDITO",
    "paymentMethodCode": "19",
    "createdAt": "2025-01-01T10:00:00",
    "updatedAt": "2025-01-01T10:00:00"
  },
  {
    "idPaymentMethod": 7,
    "paymentMethodType": "OTROS CON UTILIZACIÓN DEL SISTEMA FINANCIERO",
    "paymentMethodCode": "20",
    "createdAt": "2025-01-01T10:00:00",
    "updatedAt": "2025-01-01T10:00:00"
  },
  {
    "idPaymentMethod": 8,
    "paymentMethodType": "ENDOSO DE TÍTULOS",
    "paymentMethodCode": "21",
    "createdAt": "2025-01-01T10:00:00",
    "updatedAt": "2025-01-01T10:00:00"
  }
]
```

**Estructura de Respuesta:**
| Campo | Tipo | Descripción |
|-------|------|-------------|
| `idPaymentMethod` | Long | ID único del método de pago |
| `paymentMethodType` | String | Descripción del método de pago |
| `paymentMethodCode` | String | Código del método según SRI (ej: "01", "16", "19") |
| `createdAt` | LocalDateTime | Fecha de creación del registro |
| `updatedAt` | LocalDateTime | Fecha de última actualización |

**Uso típico:**
Este endpoint se utiliza para:

- Poblar listas desplegables (dropdowns) en formularios de facturación
- Validar códigos de métodos de pago antes de crear facturas
- Mostrar opciones de pago disponibles a los usuarios

**Ejemplo con curl:**

```bash
curl http://localhost:8480/api/payment-method
```

**Ejemplo con JavaScript:**

```javascript
// Obtener todos los métodos de pago
const paymentMethods = await fetch(
  "http://localhost:8480/api/payment-method"
).then((response) => response.json());

// Usar en un select/dropdown
const selectOptions = paymentMethods.map((pm) => ({
  value: pm.idPaymentMethod,
  label: `${pm.paymentMethodCode} - ${pm.paymentMethodType}`,
}));
```

---

## 📦 Modelos de Datos

### InvoiceDto

```typescript
{
  idInvoice?: number,
  idCompany?: number,
  companyDto: CompanyDto,         // REQUERIDO
  paymentMethod: PaymentMethod,   // REQUERIDO
  clientDto: ClientDto,           // REQUERIDO
  productDto: ProductDto,         // REQUERIDO
  idPaymentMethod?: number,
  creationDate?: string (ISO DateTime),
  issueDate?: string (ISO DateTime),
  accessKey?: string,
  invoiceNumber?: string,
  environment?: string,           // "PRODUCCION" | "PRUEBAS"
  authorizationNumber?: string,
  additionalInformation?: string,
  authorizationDate?: string (ISO DateTime),
  totalAmount: number (BigDecimal),
  discount: number (BigDecimal),
  tip: number (BigDecimal),
  subtotalZero: number (BigDecimal),
  subtotalIva: number (BigDecimal),
  subtotalIce: number (BigDecimal),
  subtotalDiscount: number (BigDecimal),
  status?: string,                // "AUTORIZADO" | "NO_AUTORIZADO" | "PENDIENTE"
  invoiceDetails: InvoiceDetailDto[],
  establishment: EstablishmentDto,
  password: string                // REQUERIDO (firma digital)
}
```

### CompanyDto

```typescript
{
  idCompany?: number,
  idType: number,
  identification: string,         // RUC de la empresa
  name: string,                   // Razón social
  nameCommercial?: string,
  branchOfficeCode?: string,      // Código de sucursal (ej: "001")
  forcedToAccounting?: string,    // "SI" | "NO"
  specialTaxpayer?: number,
  principal?: string,             // "SI" | "NO"
  status: boolean,
  contacts?: ContactDto[],
  companiesIds?: number[],
  isSuperAdmin?: boolean
}
```

### ClientDto

```typescript
{
  idClient?: number,
  idType: number,
  identification: string,         // Cédula, RUC, pasaporte
  name: string,                   // Nombre o razón social
  clientType: number,
  status: boolean,
  idCompany?: number,
  companyIdentification?: string,
  contacts?: ContactDto[]
}
```

### ProductDto

```typescript
{
  idProduct?: number,
  mainCode: string,               // Código principal
  auxiliarCode?: string,          // Código auxiliar
  productType: number,
  name: string,                   // Nombre del producto
  description: string,
  unitPrice: number,
  discount: number,
  status: boolean,
  idCompany?: number,
  rate: number (BigDecimal),      // Tasa de impuesto (ej: 0.12 = 12%)
  taxCode: string,                // Código de impuesto SRI
  taxCodPercentage: string        // Porcentaje (ej: "12%")
}
```

### InvoiceDetailDto

```typescript
{
  idInvoiceDetail?: number,
  idProduct?: number,
  productDto: ProductDto,
  unitPrice: number (BigDecimal),
  discount: number (BigDecimal),
  subtotal: number (BigDecimal),
  quantity: number (integer)
}
```

### EstablishmentDto

```typescript
{
  idEstablishment?: number,           // ID del establecimiento
  name: string,                       // Nombre del establecimiento
  code: string,                       // Código (ej: "001")
  address: string,                    // Dirección completa
  type: "MATRIZ" | "SUCURSAL",        // Tipo de establecimiento ⚠️ REQUERIDO para facturas
  parentAddress?: string,             // Dirección del establecimiento padre (solo si type="SUCURSAL")
  issuancePoint: IssuancePointDto
}
```

**NOTA IMPORTANTE:**

- El campo `type` es **REQUERIDO** para generar facturas correctamente
- Si `type = "MATRIZ"`: usa `address` como dirección matriz en el XML del SRI
- Si `type = "SUCURSAL"`: usa `parentAddress` como dirección matriz en el XML del SRI
- Si no se especifica `type` o no coincide, se usa la dirección de los contactos de la empresa (fallback)

### IssuancePointDto

```typescript
{
  code: string,                   // Código punto de emisión (ej: "001")
  sequentialInvoice: string       // Secuencial (ej: "000000001")
}
```

### PaymentMethod

```typescript
{
  idPaymentMethod?: number,
  paymentMethodType: string,      // Descripción del método
  paymentMethodCode: string       // Código SRI (ej: "01", "16", "17", etc.)
}
```

**Códigos de Métodos de Pago SRI:**
| Código | Descripción |
|--------|-------------|
| 01 | SIN UTILIZACION DEL SISTEMA FINANCIERO |
| 15 | COMPENSACIÓN DE DEUDAS |
| 16 | TARJETA DE DÉBITO |
| 17 | DINERO ELECTRÓNICO |
| 18 | TARJETA PREPAGO |
| 19 | TARJETA DE CRÉDITO |
| 20 | OTROS CON UTILIZACIÓN DEL SISTEMA FINANCIERO |
| 21 | ENDOSO DE TÍTULOS |

### ContactDto

```typescript
{
  contactType: string,            // "EMAIL" | "PHONE" | "MOBILE" | "ADDRESS"
  value: string,
  status: boolean
}
```

### InvoiceResponse

```typescript
{
  code: number,                   // HTTP status code (200, 400, 500, etc.)
  status: string,                 // "OK" | "ERROR"
  message: string,                // Mensaje descriptivo
  invoiceDtos?: InvoiceDto[],    // Lista de facturas (para GET)
  invoiceDto?: InvoiceDto,       // Factura individual (para POST)
  idInvoice?: number             // ID de la factura creada
}
```

### Invoice (Entity - Response de GET)

```typescript
{
  idInvoice: number,
  idCompany: number,
  idClient: number,
  issueDate: string (ISO DateTime),
  invoiceNumber: string,
  environment: string,
  additionalInformation?: string,
  totalAmount: number (BigDecimal),
  tip: number (BigDecimal),
  subtotalZero: number (BigDecimal),
  subtotalIva: number (BigDecimal),
  subtotalIce: number (BigDecimal),
  discount: number (BigDecimal),
  paymentMethod: PaymentMethod,
  authorizationSri?: {
    idAuthorizationSri: number,
    accessKey: string,
    authorizationNumber: string,
    authorizationDate: string (ISO DateTime),
    status: string,
    createdAt: string (ISO DateTime),
    updatedAt: string (ISO DateTime)
  },
  invoiceDetails: InvoiceDetail[],
  createdAt: string (ISO DateTime),
  updatedAt: string (ISO DateTime)
}
```

---

## 📊 Códigos de Respuesta

| Código | Estado                | Descripción                                |
| ------ | --------------------- | ------------------------------------------ |
| 200    | OK                    | Operación exitosa                          |
| 400    | Bad Request           | Error de validación en los datos enviados  |
| 401    | Unauthorized          | No autorizado (si se implementa seguridad) |
| 404    | Not Found             | Recurso no encontrado                      |
| 500    | Internal Server Error | Error interno del servidor                 |

---

## 💡 Ejemplos de Uso

### Ejemplo 1: Listar todas las facturas con curl

```bash
curl -X GET http://localhost:8480/api/invoice \
  -H "Content-Type: application/json"
```

### Ejemplo 2: Crear una factura con curl

```bash
curl -X POST http://localhost:8480/api/invoice \
  -H "Content-Type: application/json" \
  -d '{
  "companyDto": {
    "idCompany": 1,
    "idType": 1,
    "identification": "1234567890001",
    "name": "MI EMPRESA S.A.",
    "status": true,
    "contacts": [
      {
        "contactType": "EMAIL",
        "value": "empresa@example.com",
        "status": true
      }
    ]
  },
  "paymentMethod": {
    "idPaymentMethod": 1,
    "paymentMethodType": "SIN UTILIZACION DEL SISTEMA FINANCIERO",
    "paymentMethodCode": "01"
  },
  "clientDto": {
    "idClient": 100,
    "idType": 1,
    "identification": "0987654321",
    "name": "JUAN PEREZ",
    "clientType": 1,
    "status": true,
    "contacts": [
      {
        "contactType": "EMAIL",
        "value": "cliente@example.com",
        "status": true
      }
    ]
  },
  "productDto": {
    "idProduct": 50,
    "mainCode": "PROD001",
    "name": "Servicio de Consultoría",
    "description": "Consultoría técnica",
    "unitPrice": 100.00,
    "discount": 0.00,
    "status": true,
    "rate": 0.12,
    "taxCode": "2",
    "taxCodPercentage": "12%"
  },
  "totalAmount": 112.00,
  "subtotalIva": 100.00,
  "discount": 0.00,
  "tip": 0.00,
  "invoiceDetails": [
    {
      "productDto": {
        "idProduct": 50,
        "mainCode": "PROD001",
        "name": "Servicio de Consultoría",
        "unitPrice": 100.00,
        "rate": 0.12
      },
      "unitPrice": 100.00,
      "quantity": 1,
      "subtotal": 100.00,
      "discount": 0.00
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
  "password": "miPasswordSeguro123"
}'
```

### Ejemplo 3: JavaScript/TypeScript (Fetch)

```typescript
// Listar facturas
async function getAllInvoices() {
  const response = await fetch("http://localhost:8480/api/invoice");
  const invoices = await response.json();
  return invoices;
}

// Crear factura
async function createInvoice(invoiceData) {
  const response = await fetch("http://localhost:8480/api/invoice", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(invoiceData),
  });

  const result = await response.json();

  if (result.code === 200) {
    console.log("Factura creada:", result.invoiceDto);
    console.log("ID:", result.idInvoice);
  } else {
    console.error("Error:", result.message);
  }

  return result;
}
```

### Ejemplo 4: Python (Requests)

```python
import requests
import json

BASE_URL = "http://localhost:8480/api/invoice"

# Listar facturas
def get_all_invoices():
    response = requests.get(BASE_URL)
    return response.json()

# Crear factura
def create_invoice(invoice_data):
    headers = {'Content-Type': 'application/json'}
    response = requests.post(BASE_URL, json=invoice_data, headers=headers)
    return response.json()

# Ejemplo de uso
invoice = {
    "companyDto": {
        "idCompany": 1,
        "identification": "1234567890001",
        "name": "MI EMPRESA",
        "status": True
    },
    "clientDto": {
        "identification": "0987654321",
        "name": "CLIENTE XYZ",
        "status": True
    },
    "paymentMethod": {
        "paymentMethodCode": "01"
    },
    "totalAmount": 112.00,
    "password": "password123"
    # ... resto de campos
}

result = create_invoice(invoice)
print(f"Resultado: {result['status']}")
```

### Ejemplo 5: Postman Collection

Puedes importar esta colección en Postman:

```json
{
  "info": {
    "name": "EBill Billing API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Get All Invoices",
      "request": {
        "method": "GET",
        "header": [],
        "url": {
          "raw": "http://localhost:8480/api/invoice",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8480",
          "path": ["api", "invoice"]
        }
      }
    },
    {
      "name": "Create Invoice",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"companyDto\": {\n    \"idCompany\": 1,\n    \"identification\": \"1234567890001\",\n    \"name\": \"MI EMPRESA\"\n  },\n  \"password\": \"test123\"\n}"
        },
        "url": {
          "raw": "http://localhost:8480/api/invoice",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8480",
          "path": ["api", "invoice"]
        }
      }
    }
  ]
}
```

---

## 🔒 Seguridad

### Autenticación

Actualmente, el servicio genera una contraseña de seguridad en desarrollo:

```
Using generated security password: [password]
```

En producción, se debe configurar Spring Security adecuadamente.

### Firma Digital

- Las facturas requieren una contraseña de firma digital (`password` field)
- La firma se procesa a través del servicio `digital-signature-service`
- Las claves de acceso se generan según normativa del SRI

---

## 🔄 Proceso de Creación de Factura

```
1. Cliente envía POST /api/invoice con datos completos
   ↓
2. Validación de datos (@Validated)
   ↓
3. Generación de número de factura y clave de acceso
   ↓
4. Almacenamiento en base de datos PostgreSQL
   ↓
5. Generación de XML según formato SRI
   ↓
6. Firma digital del XML
   ↓
7. Envío al SRI para autorización
   ↓
8. Actualización del estado de autorización
   ↓
9. Retorno de respuesta al cliente con datos de autorización
```

---

## 🐛 Troubleshooting

### Error: "Cannot connect to database"

- Verifica que PostgreSQL esté corriendo en puerto 5433
- Revisa credenciales en `application-localhost.properties`

### Error: "digital-signature-service not available"

- Asegúrate de que el servicio de firma digital esté corriendo en puerto 8180
- Verifica la configuración en `application-localhost.properties`

### Error: "Validation failed"

- Revisa que todos los campos requeridos estén presentes
- Verifica el formato de los datos (RUC, cédula, montos)

---

## 📝 Notas Importantes

1. **Formato de Números de Factura:** `XXX-YYY-ZZZZZZZZZ`

   - XXX: Código de establecimiento (3 dígitos)
   - YYY: Código punto de emisión (3 dígitos)
   - ZZZZZZZZZ: Secuencial (9 dígitos)

2. **Clave de Acceso:** 49 caracteres generados según normativa SRI

3. **Ambientes:**

   - `PRODUCCION`: Facturas reales enviadas al SRI
   - `PRUEBAS`: Facturas de prueba (ambiente de testing del SRI)

4. **Tarea Programada:**

   - El servicio ejecuta una tarea cada 2 minutos (configurable)
   - Revisa facturas pendientes de autorización

5. **Base de Datos:**
   - Flyway maneja las migraciones automáticamente
   - Schema: `public`
   - Tablas principales: `invoice`, `invoice_detail`, `payment_method`, `authorization_sri`

---

## 📞 Soporte

Para más información sobre la implementación o dudas técnicas, contacta al equipo de desarrollo.

**Última actualización:** Octubre 2025  
**Versión del documento:** 1.0.0
