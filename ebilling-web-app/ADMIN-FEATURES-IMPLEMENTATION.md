# 🔐 Implementación de Features de Administración

## 📋 Overview

Este documento detalla la implementación de dos features exclusivas para **super_admin**:

1. **Gestión de Suscripciones** - Administrar planes de las empresas
2. **Gestión de Permisos de Roles** - Configurar permisos dinámicamente

---

## 🎯 1. FEATURE: Gestión de Suscripciones

### Backend Requirements (Ya en desarrollo)

**Feature Code:** `SUBSCRIPTION_MANAGEMENT`

- **allowed_roles:** [1] (solo super_admin)
- **required_plans:** ['FREE', 'BASIC', 'PROFESSIONAL', 'ENTERPRISE']

**Endpoints esperados** (ebill-billing:8480):

```
GET    /api/subscriptions                      - Listar todas las suscripciones
GET    /api/subscriptions/company/{id}          - Ver detalle de una suscripción
PUT    /api/subscriptions/company/{id}/plan     - Cambiar plan de una empresa
PUT    /api/subscriptions/company/{id}/extend   - Extender vigencia
DELETE /api/subscriptions/company/{id}          - Cancelar suscripción
```

### Frontend Implementation

#### 1.1. Crear Módulo de Suscripciones

```bash
# Estructura de archivos a crear
src/app/pages/admin/subscriptions/
├── subscriptions.module.ts
├── subscriptions-routing.module.ts
├── subscription-list/
│   ├── subscription-list.component.ts
│   ├── subscription-list.component.html
│   └── subscription-list.component.css
├── subscription-detail/
│   ├── subscription-detail.component.ts
│   ├── subscription-detail.component.html
│   └── subscription-detail.component.css
└── dialogs/
    ├── upgrade-plan-dialog/
    │   ├── upgrade-plan-dialog.component.ts
    │   ├── upgrade-plan-dialog.component.html
    │   └── upgrade-plan-dialog.component.css
    └── extend-subscription-dialog/
        ├── extend-subscription-dialog.component.ts
        ├── extend-subscription-dialog.component.html
        └── extend-subscription-dialog.component.css
```

#### 1.2. Routing

```typescript
// subscriptions-routing.module.ts
const routes: Routes = [
  {
    path: "",
    component: SubscriptionListComponent,
    canActivate: [SessionValidationGuard, PermissionGuard],
    data: { permission: "SUBSCRIPTION_MANAGEMENT" },
  },
  {
    path: "company/:id",
    component: SubscriptionDetailComponent,
    canActivate: [SessionValidationGuard, PermissionGuard],
    data: { permission: "SUBSCRIPTION_MANAGEMENT" },
  },
];
```

#### 1.3. Models

```typescript
// src/app/_model/subscription/subscription-admin.ts
export interface SubscriptionAdmin {
  idCompany: number;
  companyName: string;
  businessName: string;
  plan: {
    code: string;
    name: string;
    documentLimit: number;
  };
  status: "ACTIVE" | "TRIAL" | "EXPIRED" | "CANCELLED";
  currentYearUsage: number;
  percentUsed: number;
  remainingDocuments: number;
  startDate: Date;
  expiresAt: Date;
}

export interface PlanChangeRequest {
  newPlanCode: string;
  reason?: string;
}

export interface SubscriptionExtendRequest {
  additionalMonths: number;
  reason?: string;
}
```

#### 1.4. Service

```typescript
// src/app/_service/subscription/subscription-admin.service.ts
@Injectable({ providedIn: "root" })
export class SubscriptionAdminService {
  private readonly apiUrl = `${environment.HOST_EBILLING}/api/subscriptions`;

  constructor(private http: HttpClient) {}

  getAllSubscriptions(): Observable<SubscriptionAdmin[]> {
    return this.http.get<SubscriptionAdmin[]>(this.apiUrl);
  }

  getCompanySubscription(idCompany: number): Observable<SubscriptionAdmin> {
    return this.http.get<SubscriptionAdmin>(`${this.apiUrl}/company/${idCompany}`);
  }

  changePlan(idCompany: number, request: PlanChangeRequest): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/company/${idCompany}/plan`, request);
  }

  extendSubscription(idCompany: number, request: SubscriptionExtendRequest): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/company/${idCompany}/extend`, request);
  }

  cancelSubscription(idCompany: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/company/${idCompany}`);
  }
}
```

#### 1.5. UI Features

**SubscriptionListComponent:**

- Tabla con todas las empresas y sus suscripciones
- Columnas: Empresa, Plan, Estado, Uso (%), Documentos restantes, Fecha expiración, Acciones
- Filtros: Por plan, por estado, por fecha de expiración
- Acciones: Ver detalle, Cambiar plan, Extender, Cancelar

**SubscriptionDetailComponent:**

- Información completa de la suscripción
- Historial de cambios de plan
- Gráfico de uso de documentos
- Botones de acción: Cambiar plan, Extender, Cancelar

---

## 👥 2. FEATURE: Gestión de Permisos de Roles

### Backend Requirements (Ya en desarrollo)

**Feature Code:** `ROLE_PERMISSION_MANAGEMENT`

- **allowed_roles:** [1] (solo super_admin)
- **required_plans:** ['FREE', 'BASIC', 'PROFESSIONAL', 'ENTERPRISE']

**Endpoints esperados** (ebill-authentication:8280):

```
GET  /api/permissions/features              - Listar todos los features
GET  /api/permissions/role/{id}             - Ver permisos de un rol
PUT  /api/permissions/feature/{code}/roles  - Actualizar allowed_roles de un feature
POST /api/permissions/role/{id}/sync        - Sincronizar múltiples features a un rol
```

### Frontend Implementation

#### 2.1. Crear Módulo de Permisos

```bash
# Estructura de archivos a crear
src/app/pages/admin/role-permissions/
├── role-permissions.module.ts
├── role-permissions-routing.module.ts
├── permission-matrix/
│   ├── permission-matrix.component.ts
│   ├── permission-matrix.component.html
│   └── permission-matrix.component.css
└── feature-list/
    ├── feature-list.component.ts
    ├── feature-list.component.html
    └── feature-list.component.css
```

#### 2.2. Models

```typescript
// src/app/_model/permission/feature-permission-admin.ts
export interface FeaturePermissionAdmin {
  featureCode: string;
  name: string;
  module: string;
  allowedRoles: number[];
  requiredPlans: string[];
  description?: string;
}

export interface RolePermissionUpdate {
  featureCode: string;
  allowedRoles: number[];
}

export interface Role {
  id: number;
  name: string;
  description: string;
}
```

#### 2.3. Service

```typescript
// src/app/_service/permission/permission-admin.service.ts
@Injectable({ providedIn: "root" })
export class PermissionAdminService {
  private readonly apiUrl = `${environment.HOST_AUTH}/api/permissions`;

  constructor(private http: HttpClient) {}

  getAllFeatures(): Observable<FeaturePermissionAdmin[]> {
    return this.http.get<FeaturePermissionAdmin[]>(`${this.apiUrl}/features`);
  }

  getRolePermissions(roleId: number): Observable<FeaturePermissionAdmin[]> {
    return this.http.get<FeaturePermissionAdmin[]>(`${this.apiUrl}/role/${roleId}`);
  }

  updateFeatureRoles(featureCode: string, allowedRoles: number[]): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/feature/${featureCode}/roles`, { allowedRoles });
  }

  syncRolePermissions(roleId: number, features: string[]): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/role/${roleId}/sync`, { features });
  }
}
```

#### 2.4. UI Features

**PermissionMatrixComponent:**

- Tabla matricial: Features (filas) x Roles (columnas)
- Checkboxes para activar/desactivar permisos
- Filtros: Por módulo, por plan requerido
- Botón "Guardar cambios" (guarda todas las modificaciones en lote)
- Indicador visual de cambios pendientes

**Ejemplo de UI:**

```
┌─────────────────────────────┬───────────┬─────────┬──────────┬────────┐
│ Feature                     │ Super Admin│ Admin  │ Accountant│ Member │
├─────────────────────────────┼───────────┼─────────┼──────────┼────────┤
│ INVOICE_CREATE              │     ✓     │    ✓    │    ✓     │   ☐    │
│ CREDIT_NOTE_CREATE          │     ✓     │    ✓    │    ✓     │   ☐    │
│ CLIENT_CREATE               │     ✓     │    ✓    │    ☐     │   ✓    │
│ USER_MANAGEMENT             │     ✓     │    ✓    │    ☐     │   ☐    │
│ COMPANY_SETTINGS            │     ✓     │    ✓    │    ☐     │   ☐    │
│ REPORT_ADVANCED             │     ✓     │    ✓    │    ☐     │   ☐    │
└─────────────────────────────┴───────────┴─────────┴──────────┴────────┘

[Guardar Cambios]  [Descartar]
```

---

## 🔄 3. Sistema de Revalidación (OPCIONAL)

### Opción A: Forzar Re-login

Cuando el super_admin cambia permisos de un rol:

1. Backend guarda registro en tabla `session_invalidation`
2. Próxima petición de usuarios con ese rol → detecta invalidación → cierra sesión
3. Usuario debe hacer login nuevamente para obtener nuevos permisos

### Opción B: Notificación con Recarga Manual

1. Backend NO invalida sesiones automáticamente
2. Frontend muestra notificación: "Los permisos de tu rol han sido actualizados. Recarga la página para ver los cambios."
3. Usuario decide cuándo recargar

**Recomendación:** Opción A (más segura, evita confusión)

---

## 🎨 4. Integración en el Menú

Agregar entrada al menú lateral (solo visible para super_admin):

```sql
-- Script SQL para el backend
INSERT INTO menu (id_menu, parent_id_menu, icon, name, url, status, feature_code)
VALUES
  (40, 16, 'admin_panel_settings', 'Suscripciones', '/admin/subscriptions', true, 'SUBSCRIPTION_MANAGEMENT'),
  (41, 16, 'security', 'Permisos de Roles', '/admin/role-permissions', true, 'ROLE_PERMISSION_MANAGEMENT');

-- Asignar al rol super_admin (id=1)
INSERT INTO menu_role (id_menu_role, id_menu, id_role, status)
VALUES
  (nextval('menu_role_seq'), 40, 1, true),
  (nextval('menu_role_seq'), 41, 1, true);
```

---

## 📝 5. TODO List Frontend

### FASE 1: Gestión de Suscripciones

- [ ] Crear módulo `/admin/subscriptions`
- [ ] Crear modelos de datos
- [ ] Crear servicio `SubscriptionAdminService`
- [ ] Implementar `SubscriptionListComponent`
- [ ] Implementar `SubscriptionDetailComponent`
- [ ] Crear `UpgradePlanDialogComponent`
- [ ] Crear `ExtendSubscriptionDialogComponent`
- [ ] Agregar entrada al menú lateral
- [ ] Proteger rutas con `PermissionGuard`

### FASE 2: Gestión de Permisos de Roles

- [ ] Crear módulo `/admin/role-permissions`
- [ ] Crear modelos de datos
- [ ] Crear servicio `PermissionAdminService`
- [ ] Implementar `PermissionMatrixComponent`
- [ ] Implementar lógica de guardado en lote
- [ ] Agregar entrada al menú lateral
- [ ] Proteger rutas con `PermissionGuard`

### FASE 3: Limpieza (✅ COMPLETADO)

- [x] Remover logs de debugging de `permission.service.ts`
- [x] Remover logs de debugging de `app.component.ts`
- [x] Verificar que `menu-filter.service.ts` esté limpio

---

## 🚀 Orden de Implementación Recomendado

1. **AHORA:** Esperar a que backend complete los endpoints
2. **DÍA 1:** Crear estructura de módulos y modelos
3. **DÍA 2:** Implementar servicios y conectar con backend
4. **DÍA 3:** Implementar UI de gestión de permisos (más crítico)
5. **DÍA 4:** Implementar UI de gestión de suscripciones
6. **DÍA 5:** Testing y ajustes finales

---

## 🔒 Seguridad

- ✅ Todas las rutas protegidas con `PermissionGuard`
- ✅ Solo super_admin puede acceder a estas features
- ✅ Backend valida nuevamente los permisos en cada petición
- ✅ Logs de auditoría en backend para todos los cambios

---

**Estado:** Documentación completada, esperando endpoints de backend para iniciar implementación.
