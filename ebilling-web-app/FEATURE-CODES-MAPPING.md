# 🗺️ Mapeo de Feature Codes - Frontend ↔️ Backend

## 📋 Feature Codes Disponibles en Backend (ebill-authentication)

### 🧾 INVOICING (Facturación)

- `INVOICE_CREATE` - Crear facturas
- `INVOICE_VOID` - Anular facturas
- `INVOICE_EXPORT` - Exportar facturas

### 💳 CREDIT_NOTES (Notas de Crédito)

- `CREDIT_NOTE_CREATE` - Crear notas de crédito
- `CREDIT_NOTE_VOID` - Anular notas de crédito

### 👥 CLIENTS (Clientes)

- `CLIENT_CREATE` - Crear clientes
- `CLIENT_IMPORT` - Importar clientes

### 📊 REPORTS (Reportes)

- `REPORT_OPERATIONAL` - Reportes operativos
- `REPORT_SALES` - Reportes comerciales
- `REPORT_ADVANCED` - Reportes avanzados

### ⚙️ ADMINISTRATION (Administración)

- `USER_MANAGEMENT` - Gestión de usuarios
- `COMPANY_SETTINGS` - Configuración empresa
- `ESTABLISHMENT_MANAGEMENT` - Gestión de establecimientos

### 🔌 API

- `API_ACCESS` - Acceso API REST
- `WEBHOOK_CONFIG` - Configuración webhooks

---

## 🎯 Mapeo de Rutas Frontend → Feature Codes Backend

| Ruta Frontend                | Feature Code               | Roles Permitidos        | Planes Requeridos                  | Estado |
| ---------------------------- | -------------------------- | ----------------------- | ---------------------------------- | ------ |
| `/welcome`                   | -                          | Todos                   | Todos                              | ✅ OK  |
| `/invoices`                  | `INVOICE_CREATE`           | 1,2,3 (Admin,Cont,Oper) | FREE,BASIC,PROFESSIONAL,ENTERPRISE | ✅ OK  |
| `/invoices/drafts`           | `INVOICE_CREATE`           | 1,2,3 (Admin,Cont,Oper) | FREE,BASIC,PROFESSIONAL,ENTERPRISE | ✅ OK  |
| `/credit-notes`              | `CREDIT_NOTE_CREATE`       | 1,2,3 (Admin,Cont,Oper) | BASIC,PROFESSIONAL,ENTERPRISE      | ✅ OK  |
| `/reports/sales-dashboard`   | `REPORT_ADVANCED`          | 1,2,3 (Admin,Cont,Oper) | PROFESSIONAL,ENTERPRISE            | ✅ OK  |
| `/reports/branch-comparison` | `REPORT_ADVANCED`          | 1,2,3 (Admin,Cont,Oper) | PROFESSIONAL,ENTERPRISE            | ✅ OK  |
| `/reports/sales-by-period`   | -                          | Todos                   | Todos                              | ✅ OK  |
| `/reports/pending-invoices`  | -                          | Todos                   | Todos                              | ✅ OK  |
| `/clients`                   | `CLIENT_CREATE`            | 1,2,3 (Admin,Cont,Oper) | FREE,BASIC,PROFESSIONAL,ENTERPRISE | ✅ OK  |
| `/products`                  | -                          | Todos                   | Todos                              | ✅ OK  |
| `/users`                     | `USER_MANAGEMENT`          | 1,2 (Admin,Cont)        | BASIC,PROFESSIONAL,ENTERPRISE      | ✅ OK  |
| `/companies`                 | `COMPANY_SETTINGS`         | 1,2 (Admin,Cont)        | FREE,BASIC,PROFESSIONAL,ENTERPRISE | ✅ OK  |
| `/establishments`            | `ESTABLISHMENT_MANAGEMENT` | 1,2 (Admin,Cont)        | BASIC,PROFESSIONAL,ENTERPRISE      | ✅ OK  |
| `/invoice-points`            | `ESTABLISHMENT_MANAGEMENT` | 1,2 (Admin,Cont)        | BASIC,PROFESSIONAL,ENTERPRISE      | ✅ OK  |

### 📝 Leyenda de Roles:

- **1** = ADMIN
- **2** = ACCOUNTANT (Contador)
- **3** = OPERATOR (Operador)

### 📦 Leyenda de Planes:

- **FREE** = Plan gratuito
- **BASIC** = Plan básico
- **PROFESSIONAL** = Plan profesional
- **ENTERPRISE** = Plan empresarial

---

## 📝 Estado de Implementación

### ✅ COMPLETADO - Fase 1

- [x] `/welcome` - Accesible para todos (sin validación de feature)
- [x] `/invoices` - Protegido con `INVOICE_CREATE`
- [x] `/invoices/drafts` - Protegido con `INVOICE_CREATE`
- [x] `/credit-notes` - Protegido con `CREDIT_NOTE_CREATE`
- [x] `/clients` - Protegido con `CLIENT_CREATE`
- [x] `/users` - Protegido con `USER_MANAGEMENT`
- [x] `/companies` - Protegido con `COMPANY_SETTINGS`
- [x] `/establishments` - Protegido con `ESTABLISHMENT_MANAGEMENT`
- [x] `/invoice-points` - Protegido con `ESTABLISHMENT_MANAGEMENT`
- [x] `/reports/sales-dashboard` - Protegido con `REPORT_ADVANCED`
- [x] `/reports/branch-comparison` - Protegido con `REPORT_ADVANCED`
- [x] DTO actualizado (removido `idUser`)
- [x] Bypass temporal de admin removido
- [x] Guard actualizado para manejar respuestas del backend

### 🎯 Próximas Tareas (Fase 2)

- [ ] Usar `*hasPermission` en templates (welcome, menú)
- [ ] Crear componente de badge de suscripción
- [ ] Crear indicador de uso de documentos
- [ ] Crear modal de upgrade
- [ ] Implementar cache de validaciones

---

## 🔧 Cómo Usar

### En Routing Modules

```typescript
// Ruta CON validación de feature
{
  path: 'invoices',
  component: BillingComponent,
  canActivate: [SessionValidationGuard, PermissionGuard],
  data: { permission: 'INVOICE_CREATE' }, // ✅ Feature code del backend
}

// Ruta SIN validación de feature (solo sesión)
{
  path: 'welcome',
  component: WelcomeComponent,
  canActivate: [SessionValidationGuard], // ❌ Sin PermissionGuard
}
```

### En Componentes (TypeScript)

```typescript
// Verificar si tiene acceso a una feature
this.permissionService.canAccess("REPORT_ADVANCED").subscribe((allowed) => {
  if (allowed) {
    this.loadAdvancedReports();
  }
});

// Con detalles (para mostrar mensaje de upgrade)
this.permissionService.canAccessWithDetails("INVOICE_CREATE").subscribe((response) => {
  if (!response.allowed) {
    console.log("Motivo:", response.reason);
    console.log("Mensaje:", response.message);
    console.log("URL upgrade:", response.upgradeUrl);
  }
});
```

### En Templates (HTML)

```html
<!-- Mostrar solo si tiene permiso -->
<button *hasPermission="'INVOICE_CREATE'">Crear Factura</button>

<!-- Con template alternativo -->
<div *hasPermission="'REPORT_ADVANCED'; else upgradeTpl">
  <app-advanced-dashboard></app-advanced-dashboard>
</div>
<ng-template #upgradeTpl>
  <button class="upgrade-btn">🔒 Actualizar Plan</button>
</ng-template>
```

---

## 📡 Endpoint de Validación

### Request

```bash
curl -X POST http://localhost:8280/api/permission/validate-feature \
  -H "Content-Type: application/json" \
  -d '{
    "featureCode": "INVOICE_CREATE",
    "idCompany": 1
  }'
```

### Response (Éxito)

```json
{
  "allowed": true,
  "reason": "ACCESS_GRANTED",
  "message": "Access granted to feature INVOICE_CREATE",
  "upgradeUrl": null
}
```

### Response (Denegado)

```json
{
  "allowed": false,
  "reason": "PLAN_DOES_NOT_INCLUDE_FEATURE",
  "message": "Your plan does not include this feature",
  "upgradeUrl": "https://upgrade-url.com"
}
```

---

## 🎨 Códigos de Reason del Backend

| Reason                          | Significado                | Acción Frontend         |
| ------------------------------- | -------------------------- | ----------------------- |
| `ACCESS_GRANTED`                | ✅ Acceso permitido        | Permitir navegación     |
| `PLAN_DOES_NOT_INCLUDE_FEATURE` | 🔒 Plan no incluye feature | Toast warning + upgrade |
| `NO_ACTIVE_SUBSCRIPTION`        | ❌ Sin suscripción activa  | Toast error + upgrade   |
| `INSUFFICIENT_PERMISSIONS`      | 🚫 Sin rol requerido       | Toast error + redirigir |
| `LIMIT_EXCEEDED`                | ⚠️ Límite alcanzado        | Toast warning + upgrade |

---

## 🧪 Testing

### Test 1: Usuario con Plan BASIC

```bash
# Debería permitir INVOICE_CREATE
curl -X POST http://localhost:8280/api/permission/validate-feature \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"featureCode": "INVOICE_CREATE", "idCompany": 1}'

# Debería DENEGAR REPORT_ADVANCED (requiere plan PRO+)
curl -X POST http://localhost:8280/api/permission/validate-feature \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"featureCode": "REPORT_ADVANCED", "idCompany": 1}'
```

### Test 2: Usuario con Plan PRO

```bash
# Debería permitir REPORT_ADVANCED
curl -X POST http://localhost:8280/api/permission/validate-feature \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"featureCode": "REPORT_ADVANCED", "idCompany": 1}'
```

---

## 🔗 Referencias

- **Backend API:** `http://localhost:8280/api/permission/`
- **Billing Service:** `http://localhost:8480/api/subscription/`
- **Documentación Frontend:** `FRONTEND-PERMISSIONS-IMPLEMENTATION.md`
- **Arquitectura:** `ROLES-SUBSCRIPTION-ARCHITECTURE.md`
