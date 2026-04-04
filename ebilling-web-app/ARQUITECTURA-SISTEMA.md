# 🏗️ Arquitectura del Sistema - eBilling Platform

## 📋 Resumen

Sistema de facturación electrónica basado en arquitectura de microservicios con frontend Angular y múltiples servicios backend REST.

---

## 🎯 Componentes Principales

### 🌐 Frontend Angular SPA
- **Puerto**: `4200` (desarrollo)
- **Tecnologías**: Angular 18, RxJS, Angular Material, TypeScript
- **Características**:
  - Lazy Loading (6 módulos)
  - Reactive Forms
  - JWT Authentication
  - Material Design UI
  - Responsive Design

---

## 🔌 Microservicios Backend

### 1. 🔐 ebill-authentication (Puerto 8280)

**Responsabilidades:**
- Autenticación y autorización
- Gestión de usuarios y roles
- Gestión de permisos (granulares)
- Gestión de menús
- Sesiones de puntos de emisión

**Endpoints Principales:**
```
POST   /api/login
GET    /api/session/validate
GET    /api/permission/user/features
POST   /api/permission/validate-feature
GET    /api/permission/permissions/features
GET    /api/permission/permissions/role/{idRole}
PUT    /api/permission/permissions/feature/{featureCode}
PUT    /api/permission/permissions/bulk
GET    /api/permission/permissions/feature/{featureCode}/plans
PUT    /api/permission/permissions/feature/{featureCode}/plans
GET    /api/menu/{userName}
GET    /api/admin/menu
POST   /api/admin/menu
PUT    /api/admin/menu/{id}
DELETE /api/admin/menu/{id}/hard
GET    /api/user/userName/{userName}
GET    /api/user/{idUser}
POST   /api/user
PATCH  /api/user/{idUser}
GET    /api/role
GET    /api/user-company/company/{idCompany}
POST   /api/user-company
DELETE /api/user-company/company/{idCompany}/user/{idUser}
POST   /api/invoice-points/session
DELETE /api/invoice-points/session
GET    /api/invoice-points/session/current-session
POST   /api/invoice-points/session/heartbeat
GET    /api/token/restore-password/validate/{token}
POST   /api/mail/send
```

---

### 2. 👥 ebill-customers (Puerto 8180)

**Responsabilidades:**
- Gestión de clientes
- Gestión de compañías
- Gestión de establecimientos
- Gestión de puntos de emisión
- Relaciones cliente-compañía

**Endpoints Principales:**
```
GET    /api/client
POST   /api/client
PATCH  /api/client/{id}
DELETE /api/client/{idClient}
POST   /api/client/search
POST   /api/client/pageable
GET    /api/company/companies/{idCompany}
GET    /api/company/{idCompany}/logo/image
GET    /api/establishments
POST   /api/establishments
GET    /api/invoice-issuance-points
POST   /api/invoice-issuance-points
GET    /api/company-client
POST   /api/company-client
DELETE /api/company-client/company/{idCompany}/client/{idClient}
```

---

### 3. 📦 ebill-products (Puerto 8380)

**Responsabilidades:**
- Gestión de productos
- Gestión de tipos de impuestos

**Endpoints Principales:**
```
GET    /api/product
POST   /api/product
PATCH  /api/product/{id}
DELETE /api/product/{id}
POST   /api/product/search
POST   /api/product/pageable
GET    /api/tax-type
```

---

### 4. 🧾 ebill-billing (Puerto 8480)

**Responsabilidades:**
- Facturación electrónica
- Notas de crédito y débito
- Borradores de documentos
- Suscripciones y planes
- Permisos de planes
- Dashboard
- Métodos de pago
- Razones de nota de crédito

**Endpoints Principales:**
```
GET    /api/invoice
POST   /api/invoice
GET    /api/invoice/{id}
POST   /api/invoice/query
GET    /api/invoice/{id}/pdf
GET    /api/invoice/{id}/xml
POST   /api/invoice/draft
GET    /api/invoice/drafts
GET    /api/invoice/draft/{id}
POST   /api/invoice/draft/{id}/process
DELETE /api/invoice/draft/{id}
POST   /api/invoice/search-for-credit-note
POST   /api/invoice/search-for-debit-note
POST   /api/invoice/documents/query
GET    /api/invoice/{id}/available-for-credit-note

GET    /api/credit-notes
POST   /api/credit-notes
GET    /api/credit-notes/{id}
PUT    /api/credit-notes/{id}
DELETE /api/credit-notes/{id}
GET    /api/credit-notes/search
POST   /api/credit-notes/search-by-company
GET    /api/credit-notes/by-invoice/{originalInvoiceId}
POST   /api/credit-notes/draft
POST   /api/credit-notes/{id}/send
GET    /api/credit-notes/{id}/pdf
GET    /api/credit-notes/{id}/xml
POST   /api/credit-notes/{id}/send-email

GET    /api/debit-notes
POST   /api/debit-notes
GET    /api/debit-notes/{id}
PUT    /api/debit-notes/{id}
DELETE /api/debit-notes/{id}
GET    /api/debit-notes/search
POST   /api/debit-notes/search-by-company
GET    /api/debit-notes/by-invoice/{originalInvoiceId}
POST   /api/debit-notes/draft
POST   /api/debit-notes/{id}/send
GET    /api/debit-notes/{id}/pdf
GET    /api/debit-notes/{id}/xml
POST   /api/debit-notes/{id}/send-email

GET    /api/subscription/{idCompany}/current
GET    /api/subscription/{idCompany}/usage
POST   /api/subscription/{idCompany}/can-emit
POST   /api/subscription/{idCompany}/upgrade

GET    /api/subscriptions
GET    /api/subscriptions/company/{idCompany}
POST   /api/subscriptions/company/{idCompany}/upgrade
POST   /api/subscriptions/company/{idCompany}/extend
DELETE /api/subscriptions/company/{idCompany}/cancel
GET    /api/subscriptions/available-companies
POST   /api/subscriptions
GET    /api/subscriptions/plans
POST   /api/subscriptions/plans
PUT    /api/subscriptions/plans/{idPlan}
DELETE /api/subscriptions/plans/{idPlan}
GET    /api/subscriptions/plans/{idPlan}

GET    /api/permissions/features/plans
PUT    /api/permissions/plans/bulk

GET    /api/dashboard/welcome
GET    /api/payment-method
GET    /api/credit-note-reason
```

---

### 5. 📊 ebill-reports (Puerto 8580)

**Responsabilidades:**
- Reportes de ventas
- Reportes de notas de crédito
- Reportes de facturas pendientes
- Dashboards analíticos

**Endpoints Principales:**
```
POST   /api/report/sales/by-period
POST   /api/report/sales/by-client
POST   /api/report/sales/by-product
POST   /api/report/credit-notes
POST   /api/report/invoices/pending
POST   /api/report/sales/dashboard
POST   /api/report/branch-comparison
POST   /api/report/payment-methods
```

---

## 🔄 Flujos de Comunicación

### 1. Flujo de Autenticación
```
1. Usuario → Frontend (Login)
2. Frontend → ebill-authentication (POST /api/login)
3. ebill-authentication → Frontend (JWT Token)
4. Frontend → localStorage (Guardar token)
5. Frontend → Todos los MS (Headers: Authorization: Bearer {token})
```

### 2. Flujo de Validación de Permisos
```
1. Frontend → ebill-authentication (GET /api/permission/user/features)
2. ebill-authentication → Frontend (Lista de features permitidas)
3. Frontend → localStorage (Cachear features)
4. Frontend → Validación local (canAccess(featureCode))
5. Frontend → ebill-authentication (POST /api/permission/validate-feature) [si es necesario]
```

### 3. Flujo de Facturación
```
1. Frontend → ebill-customers (GET /api/client) [Obtener cliente]
2. Frontend → ebill-products (GET /api/product) [Obtener productos]
3. Frontend → ebill-billing (POST /api/invoice) [Crear factura]
4. ebill-billing → SRI [Enviar factura]
5. ebill-billing → Frontend (Respuesta con autorización)
```

### 4. Flujo de Menús
```
1. Frontend → ebill-authentication (GET /api/menu/{userName})
2. ebill-authentication → Frontend (Árbol de menús completo)
3. Frontend → MenuFilterService (Filtrar por permisos)
4. Frontend → UI (Mostrar menús filtrados)
```

### 5. Comunicación Interna entre Microservicios
```
ebill-authentication ←→ ebill-billing
  • Validación de permisos (ROL + PLAN)
  
ebill-billing ←→ ebill-customers
  • Obtener datos de empresa para suscripciones
```

---

## 🔐 Sistema de Permisos

### Roles
- `super_admin`: Acceso total, bypass de validaciones
- `admin`: Administrador de compañía
- `member`: Usuario estándar

### Features (Granulares)
- Cada feature tiene un código único (ej: `INVOICE_CREATE`, `CLIENT_CREATE`)
- Validación: **ROL + PLAN**
- Caché en frontend (localStorage) para optimización

### Endpoints de Permisos
```
GET  /api/permission/user/features
POST /api/permission/validate-feature
GET  /api/permission/permissions/features
GET  /api/permission/permissions/role/{idRole}
PUT  /api/permission/permissions/feature/{featureCode}
PUT  /api/permission/permissions/bulk
GET  /api/permission/permissions/feature/{featureCode}/plans
PUT  /api/permission/permissions/feature/{featureCode}/plans
```

---

## ⚙️ Configuración

### Proxy (proxy.conf.json)
El proxy de Angular enruta las peticiones `/api/*` a los microservicios correspondientes:

```json
{
  "/api/login": "http://localhost:8280",
  "/api/menu": "http://localhost:8280",
  "/api/permission": "http://localhost:8280",
  "/api/user": "http://localhost:8280",
  "/api/role": "http://localhost:8280",
  "/api/token": "http://localhost:8280",
  "/api/company": "http://localhost:8180",
  "/api/client": "http://localhost:8180",
  "/api/establishments": "http://localhost:8180",
  "/api/invoice-issuance-points": "http://localhost:8180",
  "/api/product": "http://localhost:8380",
  "/api/invoice": "http://localhost:8480",
  "/api/credit-notes": "http://localhost:8480",
  "/api/debit-notes": "http://localhost:8480",
  "/api/subscription": "http://localhost:8480",
  "/api/subscriptions": "http://localhost:8480",
  "/api/permissions": "http://localhost:8480",
  "/api/dashboard": "http://localhost:8480",
  "/api/payment-method": "http://localhost:8480",
  "/api/report": "http://localhost:8580"
}
```

### Environment Variables
```typescript
HOST_AUTH: 'http://localhost:8280'
HOST_CUSTOMERS: 'http://localhost:8180'
HOST_PRODUCTS: 'http://localhost:8380'
HOST_EBILLING: 'http://localhost:8480'
HOST_REPORT: 'http://localhost:8580'
```

---

## 📊 Resumen de Puertos

| Componente | Puerto | Ambiente Desarrollo | Ambiente Producción |
|------------|--------|---------------------|---------------------|
| Frontend | 4200 | localhost:4200 | wallparisoft.com |
| ebill-authentication | 8280 | localhost:8280 | wallparisoft.com |
| ebill-customers | 8180 | localhost:8180 | wallparisoft.com |
| ebill-products | 8380 | localhost:8380 | wallparisoft.com |
| ebill-billing | 8480 | localhost:8480 | wallparisoft.com |
| ebill-reports | 8580 | localhost:8580 | wallparisoft.com |

---

## 🛠️ Stack Tecnológico

### Frontend
- **Framework**: Angular 18
- **Estado**: RxJS (Observables)
- **UI**: Angular Material
- **Lenguaje**: TypeScript
- **Build**: Angular CLI
- **Testing**: Jasmine + Karma

### Backend (Asumido)
- **Framework**: Spring Boot (Java)
- **API**: REST
- **Autenticación**: JWT
- **Base de Datos**: PostgreSQL (asumido)

### Protocolos
- **HTTP/REST**: Comunicación entre componentes
- **JWT Bearer Tokens**: Autenticación
- **JSON**: Formato de datos

---

## 📝 Notas Importantes

1. **Autenticación**: Todos los endpoints (excepto `/api/login`) requieren JWT en el header `Authorization: Bearer {token}`

2. **Caché**: El frontend cachea permisos en localStorage para reducir llamadas al backend

3. **Interceptors**: El frontend tiene interceptors para:
   - Autenticación (agregar JWT token)
   - Manejo de errores
   - Timeout de peticiones

4. **Lazy Loading**: El frontend usa lazy loading para cargar módulos bajo demanda

5. **Validación de Permisos**: Se valida en dos niveles:
   - **ROL**: Verificado en `ebill-authentication`
   - **PLAN**: Verificado en `ebill-billing` (coordinado por `ebill-authentication`)

---

## 📄 Archivo Diagrama

Para visualizar el diagrama de arquitectura:
1. Abre [draw.io](https://app.diagrams.net/) o [diagrams.net](https://diagrams.net/)
2. Selecciona "Open Existing Diagram"
3. Carga el archivo `ARQUITECTURA-SISTEMA.drawio`
4. El diagrama mostrará todos los componentes, conexiones y flujos

---

**Última actualización**: Enero 2025

