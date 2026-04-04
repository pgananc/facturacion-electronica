# 📊 Estado del Proyecto - Actualización Enero 2026

**Última actualización:** Enero 2026  
**Estado General:** ✅ Frontend implementado | ✅ Tests de Integración completados | ⏳ Tests unitarios en progreso | 🟡 Features pendientes

---

## ✅ LO QUE ESTÁ COMPLETADO

### 1. Testing Unitario (Cobertura: ~55-65%)

#### ✅ Tests Implementados (36 archivos)

**Servicios (15 archivos):**

- ✅ `permission.service.spec.ts` - Validación de features (15+ tests)
- ✅ `client.service.spec.ts` - CRUD de clientes
- ✅ `invoice.service.spec.ts` - Gestión de facturas
- ✅ `product.service.spec.ts` - CRUD de productos ⭐ NUEVO
- ✅ `user.service.spec.ts` - CRUD de usuarios ⭐ NUEVO
- ✅ `company.service.spec.ts` - CRUD de empresas ⭐ NUEVO
- ✅ `menu.service.spec.ts` - Carga de menús ⭐ NUEVO
- ✅ `credit-note.service.spec.ts` - Notas de crédito ⭐ NUEVO
- ✅ `debit-note.service.spec.ts` - Notas de débito ⭐ NUEVO
- ✅ `draft-invoice.service.spec.ts` - Borradores ⭐ NUEVO
- ✅ `menu-filter.service.spec.ts` - Filtrado de menús por permisos ⭐ NUEVO
- ✅ `permission-admin.service.spec.ts` - Gestión de permisos (admin) ⭐ NUEVO
- ✅ `login.service.spec.ts` - Autenticación ✅ Ya existe
- ✅ `token.service.spec.ts` - Gestión de tokens ✅ Ya existe
- ✅ `subscription-admin.service.spec.ts` - Gestión de suscripciones (admin) ✅ Ya existe

**Componentes (12 archivos):**

- ✅ `client.component.spec.ts` - CRUD de clientes (25+ tests) ⭐ NUEVO
- ✅ `product.component.spec.ts` - CRUD de productos (20+ tests) ⭐ NUEVO
- ✅ `user.component.spec.ts` - CRUD de usuarios (18+ tests) ⭐ NUEVO
- ✅ `company.component.spec.ts` - CRUD de empresas (20+ tests) ⭐ NUEVO
- ✅ `menu-list.component.spec.ts` - Lista de menús (25+ tests) ⭐ NUEVO
- ✅ `sales-dashboard.component.spec.ts` - Dashboard de ventas (20+ tests) ⭐ NUEVO
- ✅ `sales-by-period.component.spec.ts` - Reporte ventas por período (30+ tests) ⭐ NUEVO ✅ **COMPLETADO Y FUNCIONANDO**
- ✅ `draft-invoices.component.spec.ts` - Lista de borradores (20+ tests) ⭐ NUEVO
- ✅ `viewing-invoices.component.spec.ts` - Lista de facturas (25+ tests) ⭐ NUEVO
- ✅ `create-credit-note.component.spec.ts` - Creación de NC (30+ tests) ⭐ NUEVO
- ✅ `create-debit-note.component.spec.ts` - Creación de ND (30+ tests) ⭐ NUEVO

**Guards (4 archivos):**

- ✅ `permission.guard.spec.ts` - Protección de rutas (10+ tests)
- ✅ `session-validation.guard.spec.ts` - Validación de sesión ⭐ NUEVO
- ✅ `can-active-user.guard.spec.ts` - Validación de usuario activo ⭐ NUEVO
- ✅ `admin.guard.spec.ts` - Validación de super_admin ⭐ NUEVO

**Directivas (1 archivo):**

- ✅ `has-permission.directive.spec.ts` - Mostrar/ocultar elementos

**Componentes (4 archivos):**

- ✅ `app.component.spec.ts` - Componente raíz
- ✅ `billing.component.spec.ts` - Creación de facturas ⭐ NUEVO
- ✅ `subscription-list.component.spec.ts` - Lista de suscripciones ⭐ NUEVO
- ✅ `permission-matrix.component.spec.ts` - Matriz de permisos ⭐ NUEVO
- ✅ `add-payment-dialog.component.spec.ts` - Dialog de pagos
- ✅ `select-product-dialog.component.spec.ts` - Dialog de productos
- ✅ `select-invoice-point-dialog.component.spec.ts` - Dialog de puntos

**Cobertura estimada:** ~55-65% del código crítico (mejorado desde ~50-60%)

---

### 2. Sistema de Permisos y Seguridad

#### ✅ Protección de Rutas

**Rutas protegidas con PermissionGuard:**

- ✅ `/products` → `PRODUCT_MANAGEMENT`
- ✅ `/clients` → `CLIENT_CREATE`
- ✅ `/users` → `USER_MANAGEMENT`
- ✅ `/companies` → `COMPANY_SETTINGS`
- ✅ `/establishments` → `ESTABLISHMENT_MANAGEMENT`
- ✅ `/invoice-points` → `ESTABLISHMENT_MANAGEMENT`
- ✅ `/invoices` → `INVOICE_CREATE`
- ✅ `/invoices/drafts` → `INVOICE_CREATE`
- ✅ `/invoices/credit-notes` → `CREDIT_NOTE_CREATE`
- ✅ `/invoices/debit-notes` → `DEBIT_NOTE_CREATE`
- ✅ `/reports/sales-dashboard` → `REPORT_ADVANCED`
- ✅ `/reports/branch-comparison` → `REPORT_ADVANCED`
- ✅ `/admin/subscriptions` → `SUBSCRIPTION_MANAGEMENT`
- ✅ `/admin/role-permissions` → `ROLE_PERMISSION_MANAGEMENT`
- ✅ `/admin/menus` → `MENU_MANAGEMENT`

**Todas las rutas críticas están protegidas** ✅

#### ✅ Componentes de Seguridad

- ✅ `PermissionService` - Validación de features
- ✅ `PermissionGuard` - Protección de rutas
- ✅ `HasPermissionDirective` - Mostrar/ocultar elementos
- ✅ `SessionValidationGuard` - Validación de sesión
- ✅ `CanActiveUserGuard` - Validación de usuario activo
- ✅ `AdminGuard` - Validación de super_admin

---

### 3. UI/UX - Sistema de Suscripciones

#### ✅ Componente Subscription Badge

- ✅ Badge de plan actual en navbar y sidebar
- ✅ Barra de progreso de documentos usados
- ✅ Alertas automáticas (80% y 100% de uso)
- ✅ Refresco automático cada minuto
- ✅ Responsive (se oculta en móviles pequeños)
- ✅ Colores dinámicos según plan (FREE/BASIC/PRO/ENT)

**Componente:** `subscription-badge.component.ts` ✅

---

### 4. Módulos Core Implementados

- ✅ Facturación (invoices, credit notes, debit notes)
- ✅ Clientes (CRUD completo)
- ✅ Productos (CRUD completo)
- ✅ Usuarios (CRUD completo)
- ✅ Empresas (CRUD completo)
- ✅ Establecimientos y Puntos de Emisión
- ✅ Reportes (7 tipos de reportes)
- ✅ Admin (Suscripciones, Permisos, Menús)

---

## 🟡 LO QUE ESTÁ EN PROGRESO / PENDIENTE

### 1. Testing (Pendiente: ~45-55% más)

#### ⏳ Tests Unitarios Faltantes

**Servicios pendientes (~10 servicios):**

- [x] `menu-filter.service.ts` - Filtrado de menús por permisos ⭐ NUEVO
- [x] `login.service.ts` - Autenticación ✅ Ya existe
- [x] `token.service.ts` - Gestión de tokens ✅ Ya existe
- [x] `permission-admin.service.ts` - Gestión de permisos (admin) ⭐ NUEVO
- [x] `subscription-admin.service.ts` - Gestión de suscripciones (admin) ✅ Ya existe
- [x] `menu-admin.service.ts` - Gestión de menús (admin) ✅ Ya existe
- [x] `dashboard.service.ts` - Dashboard de ventas ✅ Ya existe
- [x] `report.service.ts` - Generación de reportes ✅ Ya existe
- [x] `tax-type.service.ts` - Tipos de impuestos ✅ Ya existe
- [x] `product-cache.service.ts` - Cache de productos ✅ Ya existe
- [x] `company-client.service.ts` - Clientes por empresa ✅ Ya existe
- [x] `company-user.service.ts` - Usuarios por empresa ✅ Ya existe
- [x] `payment-method.service.ts` - Métodos de pago ✅ Ya existe
- [x] `establishment.service.ts` - Establecimientos ✅ Ya existe
- [x] `issuance-point.service.ts` - Puntos de emisión ✅ Ya existe

**Componentes pendientes (~0 componentes):**

- ✅ `create-credit-note.component.ts` - Creación de NC ⭐ NUEVO
- ✅ `create-debit-note.component.ts` - Creación de ND ⭐ NUEVO
- [x] `draft-invoices.component.ts` - Lista de borradores ⭐ NUEVO
- [x] `viewing-invoices.component.ts` - Lista de facturas ⭐ NUEVO
- [x] `client.component.ts` - CRUD de clientes ⭐ NUEVO
- [x] `product.component.ts` - CRUD de productos ⭐ NUEVO
- [x] `user.component.ts` - CRUD de usuarios ⭐ NUEVO
- [x] `company.component.ts` - CRUD de empresas ⭐ NUEVO
- [x] `menu-list.component.ts` - Lista de menús ⭐ NUEVO
- [x] `sales-dashboard.component.ts` - Dashboard de ventas ⭐ NUEVO
- [x] `sales-by-period.component.ts` - Reporte de Ventas por Período ⭐ NUEVO ✅ **COMPLETADO Y FUNCIONANDO**

#### ✅ Tests de Integración (100% implementado y funcionando)

- [x] Flujo completo de facturación ⭐ NUEVO ✅ **COMPLETADO Y FUNCIONANDO**
- [x] Flujo de notas de crédito ⭐ NUEVO ✅ **COMPLETADO Y FUNCIONANDO**
- [x] Flujo de permisos ⭐ NUEVO ✅ **COMPLETADO Y FUNCIONANDO**
- [x] Flujo de suscripciones ⭐ NUEVO ✅ **COMPLETADO Y FUNCIONANDO**
- [x] Flujo de menús ⭐ NUEVO ✅ **COMPLETADO Y FUNCIONANDO**

**Archivos de tests:**

- `src/app/integration-tests/billing-flow.integration.spec.ts` ✅
- `src/app/integration-tests/credit-note-flow.integration.spec.ts` ✅
- `src/app/integration-tests/permissions-flow.integration.spec.ts` ✅
- `src/app/integration-tests/subscriptions-flow.integration.spec.ts` ✅
- `src/app/integration-tests/menus-flow.integration.spec.ts` ✅

**Comando para ejecutar:**

```bash
ng test --include='**/*.integration.spec.ts' --watch=false
```

#### ⏳ Tests E2E (0% implementado)

- [ ] Configurar Cypress o Playwright
- [ ] Tests E2E para flujos críticos

---

### 2. Features Pendientes

#### 🔴 PRIORIDAD ALTA

**1. Modal de Upgrade** ❌ DESCARTADO

- [x] ~~Crear `upgrade-modal.component.ts`~~ ❌ No necesario
- [x] ~~Comparación de planes (actual vs recomendado)~~ ❌ No necesario
- [x] ~~Botón de upgrade con link externo o integración con backend~~ ❌ No necesario
- [x] ~~Integrar en `PermissionGuard` cuando acceso denegado~~ ❌ No necesario

**Razón:** El menú lateral ya filtra las opciones según el plan del usuario, por lo que los usuarios no ven features premium que no pueden usar. Los casos de acceso directo por URL son edge cases poco comunes y ya hay mensajes de error claros cuando se deniega acceso.

**2. Cache de Validaciones de Features**

- [x] Cache en `PermissionService` con TTL de 5 minutos ✅
- [x] Invalidar cache cuando cambian permisos ✅
- [x] Cache en localStorage como fallback ✅

**3. Validación de Límites de Documentos**

- [x] Validación en `BillingComponent` antes de crear factura ✅
- [x] Validación en `CreateCreditNoteComponent` antes de crear NC ✅
- [x] Validación en `CreateDebitNoteComponent` antes de crear ND ✅
- [x] Usar endpoint `/api/subscription/{id}/can-emit` ✅

#### 🟡 PRIORIDAD MEDIA

**4. Variables CSS Compartidas**

- [x] Crear `_shared/variables.css` con:
  - Espaciado (`--spacing-xs` a `--spacing-2xl`) ✅
  - Sombras (`--shadow-sm` a `--shadow-xl`) ✅
  - Border radius (`--radius-sm` a `--radius-full`) ✅
- [x] Migrar componentes principales a usar variables ✅
  - [x] `client.component.css` ✅
  - [x] `product.component.css` ✅
  - [x] `company.component.css` ✅
  - [x] `user.component.css` ✅
  - [x] `billing.component.css` ✅
- [ ] Migrar componentes restantes (diálogos, reportes, admin)

**5. Grid Responsive en Formularios**

- [x] `company.component` - Grid responsive ✅
- [x] `product.component` - Grid responsive ✅
- [x] `user.component` - Grid responsive ✅
- [x] `client.component` - Grid responsive (search-grid-horizontal) ✅
- [x] `admin-issuance-point-list.component` - Grid responsive ✅
- [x] `admin-establishment-list.component` - Grid responsive ✅
- [x] `issuance-point-list.component` - Hereda de responsive-styles.css ✅
- [x] `establishment-list.component` - Hereda de responsive-styles.css ✅
- [x] `subscription-list.component` - Grid responsive ✅
- [x] `company-user.component` - Grid 1 columna (ya responsive) ✅

**6. Tablas Responsive**

- [x] Agregar scroll horizontal en todas las tablas ✅
- [x] Ocultar columnas menos importantes en mobile ✅
- [x] Aplicar a todos los componentes de lista ✅
  - [x] `product.component` ✅
  - [x] `company.component` ✅
  - [x] `user.component` ✅
  - [x] `issuance-point-list.component` ✅
  - [x] `establishment-list.component` ✅
  - [x] `admin-issuance-point-list.component` ✅
  - [x] `admin-establishment-list.component` ✅
  - [x] `subscription-list.component` ✅
  - [x] `company-user.component` ✅
  - [x] `client.component` (ya tenía) ✅

#### 🟢 PRIORIDAD BAJA

**7. Preview de Features Premium**

- [ ] Componente `FeaturePreviewComponent`
- [ ] Overlay borroso con CTA de upgrade

**8. Notificaciones de Expiración**

- [ ] Alerta cuando suscripción está por vencer (7 días antes)
- [ ] Alerta cuando suscripción expira

---

### 3. Backend Pendiente

#### ✅ Endpoints de Dashboard de Ventas

- [x] `GET /api/report/dashboard/sales` - Dashboard completo ✅ IMPLEMENTADO
- [x] DTOs: `SalesDashboardDto`, `SalesTrendItemDto`, `TopProductDto`, `BranchDistributionDto` ✅
- [x] Queries SQL optimizadas ✅
- [ ] Caching (opcional, TTL 15 minutos) ⏳ Opcional

**Estado:** ✅ **IMPLEMENTADO** - Endpoint funcional en `ebill-report` (puerto 8580)

**Endpoint:**

```
GET /api/report/dashboard/sales?companyId={id}&dateFrom={date}&dateTo={date}&branchId={id}
```

**Parámetros:**

- `companyId` (Long, requerido)
- `dateFrom` (LocalDateTime, opcional, formato ISO: YYYY-MM-DDTHH:mm:ss)
- `dateTo` (LocalDateTime, opcional, formato ISO: YYYY-MM-DDTHH:mm:ss)
- `branchId` (Long, opcional)

**Response:** `ApiResponse<SalesDashboardDto>` con estructura completa

#### ✅ Endpoints de Gestión de Suscripciones

- [x] `GET /api/subscriptions` - Listar todas las suscripciones ✅ IMPLEMENTADO
- [x] `GET /api/subscriptions/company/{id}` - Ver detalle de una suscripción ✅ IMPLEMENTADO
- [x] `POST /api/subscriptions/company/{id}/upgrade` - Cambiar plan ✅ IMPLEMENTADO
- [x] `POST /api/subscriptions/company/{id}/extend` - Extender vigencia ✅ IMPLEMENTADO
- [x] `DELETE /api/subscriptions/company/{id}/cancel` - Cancelar suscripción ✅ IMPLEMENTADO
- [x] `GET /api/subscriptions/available-companies` - Empresas sin suscripción ✅ IMPLEMENTADO
- [x] `POST /api/subscriptions` - Asignar nueva suscripción ✅ IMPLEMENTADO
- [x] `GET /api/subscriptions/plans` - Planes disponibles ✅ **IMPLEMENTADO Y FUNCIONANDO**

**Estado:** ✅ **IMPLEMENTADO** - Todos los endpoints principales funcionando en `ebill-billing` (puerto 8480)

**Nota sobre `/api/subscriptions/plans`:**

- ✅ **Backend implementado**: Endpoint funcional en `ebill-billing` (puerto 8480)
- ✅ **Frontend integrado**: Los servicios usan el endpoint del backend automáticamente
- ✅ **Mapeo completo**: `PlanResponse` (backend) → `AvailablePlan` (frontend) con todos los campos
- ✅ **Fallback seguro**: Si el endpoint falla, usa datos hardcoded (no rompe nada)
- ✅ **Parámetro opcional**: Soporta `includeInactive=true` para incluir planes inactivos
- ✅ **Ordenamiento**: Los planes se ordenan por `displayOrder` automáticamente
- **Beneficios**: Gestión centralizada de planes, cambios sin redeploy, configuración dinámica

#### ✅ Endpoints de Gestión de Permisos

- [x] `GET /api/permission/permissions/features` - Listar features ✅ IMPLEMENTADO
- [x] `GET /api/permission/permissions/role/{idRole}` - Permisos de rol ✅ IMPLEMENTADO
- [x] `PUT /api/permission/permissions/feature/{featureCode}` - Actualizar roles de feature ✅ IMPLEMENTADO
- [x] `PUT /api/permission/permissions/bulk` - Actualizar múltiples ✅ IMPLEMENTADO
- [ ] `GET /api/permission/permissions/roles` - Listar roles ❌ **NO EXISTE** (usar `GET /api/role` como alternativa)

**Estado:** ✅ **IMPLEMENTADO** - Endpoints principales funcionando en `ebill-authentication` (puerto 8482)

**⚠️ Cambios necesarios en Frontend:**

- Path correcto: `/api/permission/permissions/role/{idRole}` (no `{id}`)
- Path correcto: `/api/permission/permissions/feature/{featureCode}` (no `/feature/{code}/roles`)
- Método correcto: `PUT /api/permission/permissions/bulk` (no `POST /api/permission/permissions/bulk-update`)
- Para listar roles: Usar temporalmente `GET /api/role` o solicitar implementación de `/api/permission/permissions/roles`

#### ⚠️ Feature Codes en Base de Datos

- [x] Verificar que todos los menús tienen `featureCode` único en DB ✅ **VERIFICADO**
- [ ] Asignar `featureCode` a menús faltantes ⏳ **PENDIENTE**

**Menús sin `featureCode` (con `null`):**

- `Inicio` (/welcome) - ID: 57
- `Usuario` - ID: 4
- `Reporte Novedades` (/reports/news) - ID: 26
- `Reportes operativos` (/reports/operational) - ID: 21
- `Compañía` - ID: 3
- `Reportes comerciales` (/reports/sales) - ID: 28
- `Reportes` - ID: 20
- `Cliente` - ID: 1
- `Facturación Electrónica` - ID: 5
- `Producto` - ID: 2
- `Administración Global` - ID: 16

**Estado:** ✅ No hay `featureCode` duplicados. ⚠️ Faltan asignar `featureCode` a 11 menús (principalmente menús padre y algunos reportes).

---

## 📊 Métricas de Progreso

### Testing

| Categoría            | Meta     | Actual      | Pendiente   |
| -------------------- | -------- | ----------- | ----------- |
| Servicios            | >80%     | ~60%        | ~20%        |
| Guards               | >80%     | 100%        | 0%          |
| Componentes          | >80%     | ~30%        | ~50%        |
| Tests de Integración | 100%     | 100%        | 0%          |
| Tests E2E            | 100%     | 0%          | 100%        |
| **Total**            | **>80%** | **~45-55%** | **~25-35%** |

### Features

| Categoría          | Meta | Actual | Pendiente  |
| ------------------ | ---- | ------ | ---------- |
| Rutas protegidas   | 100% | 100%   | 0%         |
| UI de suscripción  | 100% | 100%   | 0%         |
| Modal de upgrade   | 100% | N/A    | DESCARTADO |
| Cache de features  | 100% | 100%   | 0%         |
| Validación límites | 100% | 100%   | 0%         |

### UX/UI

| Categoría         | Meta | Actual | Pendiente |
| ----------------- | ---- | ------ | --------- |
| Variables CSS     | 100% | ~40%   | ~60%      |
| Grid responsive   | 100% | ~50%   | ~50%      |
| Tablas responsive | 100% | ~10%   | ~90%      |

---

## 🎯 Plan de Acción Sugerido

### Fase 1: Completar Tests Unitarios (1-2 semanas)

**Semana 1:**

- Día 1-2: Servicios de menú y autenticación (4 servicios)
- Día 3-4: Servicios de admin (3 servicios)
- Día 5-7: Servicios adicionales (5 servicios)

**Semana 2:**

- Día 8-10: Componentes de facturación (4 componentes)
- Día 11-12: Componentes de CRUD (4 componentes)
- Día 13-14: Componentes de admin (2 componentes)

### Fase 2: Features Pendientes (1 semana)

**Día 15-16:** ~~Modal de Upgrade~~ ❌ DESCARTADO

- ~~Crear componente~~ ❌ No necesario
- ~~Integrar con PermissionGuard~~ ❌ No necesario

**Día 17-18:** Cache de Features ✅ COMPLETADO

- Implementar cache en PermissionService
- Cache en localStorage

**Día 19-21:** Validación de Límites ✅ COMPLETADO

- ✅ Validar en componentes de facturación
- ✅ Integrar con endpoint can-emit

### Fase 3: Optimizaciones UX/UI (1 semana)

**Día 22-23:** Variables CSS ✅ COMPLETADO

- ✅ Crear archivo de variables
- ✅ Migrar componentes principales (client, product, company, user, billing)

**Día 24-26:** Responsive Design

- Grid responsive en formularios
- Tablas responsive

**Día 27-28:** Testing y ajustes

---

## 📝 Checklist de Completitud

### Testing

- [x] Tests unitarios >40% cobertura ✅
- [ ] Tests unitarios >80% cobertura ⏳
- [x] Tests de integración para flujos principales ✅ **COMPLETADO** (5 flujos implementados y funcionando)
- [ ] Tests E2E para flujos críticos ⏳
- [ ] CI/CD configurado ⏳

### Seguridad

- [x] Todas las rutas protegidas con PermissionGuard ✅
- [x] Sistema de permisos implementado ✅
- [x] Validación de límites de documentos antes de crear ✅
- [x] Cache de features implementado ✅

### UX/UI

- [x] Indicador de plan actual visible ✅
- [x] Barra de progreso de uso de documentos ✅
- [x] Modal de upgrade funcional ❌ DESCARTADO (no necesario)
- [x] Grid responsive en formularios principales (client, product, company, user) ✅
- [ ] Grid responsive en otros componentes de búsqueda
- [ ] Tablas responsive en todos los componentes
- [x] Variables CSS compartidas (componentes principales) ✅

### Backend

- [x] Endpoints de dashboard implementados ✅
- [x] Endpoints de suscripciones implementados ✅ (incluyendo `/api/subscriptions/plans`)
- [x] Endpoints de permisos implementados ✅ (excepto `/api/permission/permissions/roles` - usar alternativa)
- [ ] Feature codes en DB completos ⏳ (11 menús sin `featureCode` asignado)

---

## 🚀 Próximos Pasos Inmediatos

### ✅ COMPLETADO RECIENTEMENTE

- ✅ **Tests de Integración**: 5 flujos completos implementados y funcionando
  - Flujo completo de facturación
  - Flujo de notas de crédito
  - Flujo de permisos
  - Flujo de suscripciones
  - Flujo de menús

### ⏳ PENDIENTE POR PRIORIDAD

#### 🔴 ALTA PRIORIDAD

1. **Tests Unitarios** - Aumentar cobertura de ~45-55% a >80%

   - Pendiente: ~25-35% más de cobertura
   - Componentes adicionales que necesitan tests
   - Servicios adicionales que necesitan tests

2. **Tests E2E** - Configurar y crear tests end-to-end
   - [ ] Configurar Cypress o Playwright
   - [ ] Tests E2E para flujos críticos (login, facturación, permisos)

#### 🟡 MEDIA PRIORIDAD

3. **UX/UI - Variables CSS**

   - [ ] Migrar componentes restantes (diálogos, reportes, admin) a usar variables CSS

4. **UX/UI - Responsive Design**
   - [ ] Grid responsive en otros componentes de búsqueda
   - [ ] Tablas responsive en todos los componentes

#### 🟢 BAJA PRIORIDAD

5. **Features Adicionales**

   - [ ] Componente `FeaturePreviewComponent` (preview de features premium)
   - [ ] Notificaciones de expiración de suscripción (7 días antes y al expirar)

6. **Backend**
   - [ ] Asignar `featureCode` a 11 menús faltantes en DB
   - [ ] Caching opcional para dashboard (TTL 15 minutos)

---

## 🔍 Validación de Endpoints Backend

### 1. Dashboard de Ventas (`ebill-report` - Puerto 8580)

**Endpoint:** `GET /api/report/dashboard/sales`

**cURL de prueba:**

```bash
curl -X GET "http://localhost:8580/api/report/dashboard/sales?companyId=1&dateFrom=2024-01-01T00:00:00&dateTo=2024-12-31T23:59:59&branchId=1" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>"
```

**Versión mínima (solo companyId requerido):**

```bash
curl -X GET "http://localhost:8580/api/report/dashboard/sales?companyId=1" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>"
```

**Response esperado (200 OK):**

```json
{
  "status": "OK",
  "message": "Dashboard cargado exitosamente",
  "data": {
    "todayTotal": 1250.50,
    "weekTotal": 8750.25,
    "monthTotal": 35000.00,
    "previousMonthTotal": 32000.75,
    "salesTrend": [...],
    "topProducts": [...],
    "branchDistribution": [...],
    "additionalMetrics": {...}
  }
}
```

**Errores esperados:**

- `400 Bad Request`: Fechas inválidas o `companyId` faltante
- `500 Internal Server Error`: Error en el servidor

---

### 2. Suscripciones (`ebill-billing` - Puerto 8480)

**Endpoints implementados:**

- ✅ `GET /api/subscriptions` - Lista paginada
- ✅ `GET /api/subscriptions/company/{id}` - Detalle
- ✅ `POST /api/subscriptions/company/{id}/upgrade` - Cambiar plan
- ✅ `POST /api/subscriptions/company/{id}/extend` - Extender vigencia
- ✅ `DELETE /api/subscriptions/company/{id}/cancel` - Cancelar
- ✅ `GET /api/subscriptions/available-companies` - Empresas disponibles
- ✅ `POST /api/subscriptions` - Asignar suscripción
- ✅ `GET /api/subscriptions/plans` - **IMPLEMENTADO Y FUNCIONANDO**

**Nota sobre `/api/subscriptions/plans`:**

- ✅ **Backend implementado**: Endpoint funcional y documentado
- ✅ **Frontend integrado**: Servicios usando el endpoint automáticamente
- ✅ **Mapeo completo**: `PlanResponse` → `AvailablePlan` con todos los campos
- ✅ **Parámetro opcional**: Soporta `includeInactive=true` para planes inactivos
- ✅ **Ordenamiento**: Por `displayOrder` automáticamente
- **Beneficios**: Gestión centralizada, cambios sin redeploy, configuración dinámica

---

### 3. Permisos (`ebill-authentication` - Puerto 8482)

**Endpoints implementados:**

- ✅ `GET /api/permission/permissions/features` - Listar features
- ✅ `GET /api/permission/permissions/role/{idRole}` - Permisos de rol
- ✅ `PUT /api/permission/permissions/feature/{featureCode}` - Actualizar roles de feature
- ✅ `PUT /api/permission/permissions/bulk` - Actualización masiva
- ❌ `GET /api/permission/permissions/roles` - **NO EXISTE** (usar `GET /api/role` como alternativa)

**⚠️ Cambios necesarios en Frontend:**

1. Path: `/api/permission/permissions/role/{idRole}` (no `{id}`)
2. Path: `/api/permission/permissions/feature/{featureCode}` (no `/feature/{code}/roles`)
3. Método: `PUT /api/permission/permissions/bulk` (no `POST /api/permission/permissions/bulk-update`)
4. Listar roles: Usar `GET /api/role` temporalmente o solicitar implementación

---

### 4. Feature Codes en Base de Datos

**Estado de verificación:**

- ✅ No hay `featureCode` duplicados
- ⚠️ 11 menús sin `featureCode` asignado

**Menús pendientes de `featureCode`:**
| ID | Nombre | URL | Tipo |
|----|--------|-----|------|
| 57 | Inicio | /welcome | Hoja |
| 4 | Usuario | null | Padre |
| 26 | Reporte Novedades | /reports/news | Hoja |
| 21 | Reportes operativos | /reports/operational | Hoja |
| 3 | Compañía | null | Padre |
| 28 | Reportes comerciales | /reports/sales | Hoja |
| 20 | Reportes | null | Padre |
| 1 | Cliente | null | Padre |
| 5 | Facturación Electrónica | null | Padre |
| 2 | Producto | null | Padre |
| 16 | Administración Global | null | Padre |

**Acción requerida:** Asignar `featureCode` único a cada menú (especialmente los que tienen URL).

---

**Última actualización:** Enero 2026  
**Próxima revisión:** Después de completar Fase 1 (Tests Unitarios)
