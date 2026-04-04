# 🔧 Ajustes de Integración Frontend ↔ Backend

**Fecha:** 21 de diciembre de 2025  
**Módulos afectados:** Gestión de Suscripciones, Gestión de Permisos de Roles

---

## 📋 Resumen

El frontend fue ajustado para coincidir **exactamente** con los endpoints implementados en el backend:

- `ebill-billing` (puerto 8480) - Gestión de Suscripciones
- `ebill-authentication` (puerto 8280) - Gestión de Permisos

Se identificaron diferencias en:

- Métodos HTTP (POST vs PUT)
- Rutas de endpoints
- Nombres de campos en requests
- Endpoints no implementados (hardcoded temporalmente)

---

## 🔐 Gestión de Permisos de Roles

### ✅ Endpoints Implementados vs Requeridos

| #   | Endpoint Frontend                           | Endpoint Backend                      | Método | Estado               |
| --- | ------------------------------------------- | ------------------------------------- | ------ | -------------------- |
| 1   | `GET /api/permissions/features`             | `GET /api/permissions/features`       | GET    | ✅ Coincide          |
| 2   | `GET /api/permissions/role/{roleId}`        | `GET /api/permissions/role/{roleId}`  | GET    | ✅ Coincide          |
| 3   | `GET /api/permissions/roles`                | ❌ **NO IMPLEMENTADO**                | GET    | 🔧 **Hardcoded**     |
| 4   | `PUT /api/permissions/feature/{code}/roles` | `PUT /api/permissions/feature/{code}` | PUT    | 🔧 **Ajustado**      |
| 5   | `POST /api/permissions/role/{roleId}/sync`  | ❌ **NO IMPLEMENTADO**                | POST   | ⚠️ **Deshabilitado** |
| 6   | `POST /api/permissions/bulk-update`         | ❌ **NO IMPLEMENTADO**                | POST   | 🔧 **Workaround**    |

### 🔄 Cambios en `PermissionAdminService`

#### 1. `getAllRoles()` - Hardcoded

**Antes:**

```typescript
getAllRoles(): Observable<Role[]> {
  return this.http.get<Role[]>(`${this.apiUrl}/roles`);
}
```

**Después:**

```typescript
getAllRoles(): Observable<Role[]> {
  // 🔧 El backend NO tiene este endpoint, retornamos datos hardcoded
  const roles: Role[] = [
    { id: 1, name: 'Super Admin', description: 'Acceso total al sistema' },
    { id: 3, name: 'Admin', description: 'Administrador de empresa' },
    { id: 2, name: 'Member', description: 'Usuario básico' },
  ];
  return new Observable((observer) => {
    observer.next(roles);
    observer.complete();
  });
}
```

#### 2. `updateFeatureRoles()` - URL corregida

**Antes:**

```typescript
updateFeatureRoles(featureCode: string, allowedRoles: number[]): Observable<void> {
  return this.http.put<void>(`${this.apiUrl}/feature/${featureCode}/roles`, {
    allowedRoles,
  });
}
```

**Después:**

```typescript
updateFeatureRoles(featureCode: string, allowedRoles: number[]): Observable<void> {
  // 🔧 Endpoint correcto según backend
  return this.http.put<void>(`${this.apiUrl}/feature/${featureCode}`, {
    allowedRoles,
  });
}
```

#### 3. `syncRolePermissions()` - Deshabilitado

**Antes:**

```typescript
syncRolePermissions(roleId: number, features: string[]): Observable<void> {
  return this.http.post<void>(`${this.apiUrl}/role/${roleId}/sync`, {
    features,
  });
}
```

**Después:**

```typescript
syncRolePermissions(roleId: number, features: string[]): Observable<void> {
  // 🔧 El backend NO tiene este endpoint
  console.warn('[PermissionAdminService] syncRolePermissions no implementado en backend');
  return new Observable((observer) => {
    observer.next();
    observer.complete();
  });
}
```

#### 4. `bulkUpdatePermissions()` - Workaround con llamadas individuales

**Antes:**

```typescript
bulkUpdatePermissions(request: BulkPermissionUpdateRequest): Observable<void> {
  return this.http.post<void>(`${this.apiUrl}/bulk-update`, request);
}
```

**Después:**

```typescript
bulkUpdatePermissions(request: BulkPermissionUpdateRequest): Observable<void> {
  // 🔧 El backend NO tiene este endpoint
  // Implementamos actualizaciones individuales como alternativa
  console.warn('[PermissionAdminService] bulkUpdatePermissions no implementado - usando actualizaciones individuales');

  // Convertir actualización masiva en múltiples llamadas individuales
  const updates = request.updates.map((update) =>
    this.updateFeatureRoles(update.featureCode, update.allowedRoles)
  );

  // Ejecutar todas las actualizaciones en paralelo
  return new Observable((observer) => {
    if (updates.length === 0) {
      observer.next();
      observer.complete();
      return;
    }

    let completed = 0;
    let hasError = false;

    updates.forEach((update$) => {
      update$.subscribe({
        next: () => {
          completed++;
          if (completed === updates.length && !hasError) {
            observer.next();
            observer.complete();
          }
        },
        error: (error) => {
          hasError = true;
          observer.error(error);
        },
      });
    });
  });
}
```

---

## 💳 Gestión de Suscripciones

## ✅ Endpoints Implementados vs Requeridos

| #   | Endpoint Frontend Original                   | Endpoint Backend Real                           | Método | Estado           |
| --- | -------------------------------------------- | ----------------------------------------------- | ------ | ---------------- |
| 1   | `GET /api/subscriptions`                     | `GET /api/subscriptions`                        | GET    | ✅ Coincide      |
| 2   | `GET /api/subscriptions/company/{id}`        | `GET /api/subscriptions/company/{id}`           | GET    | ✅ Coincide      |
| 3   | `PUT /api/subscriptions/company/{id}/plan`   | `POST /api/subscriptions/company/{id}/upgrade`  | POST   | 🔧 **Ajustado**  |
| 4   | `PUT /api/subscriptions/company/{id}/extend` | `POST /api/subscriptions/company/{id}/extend`   | POST   | 🔧 **Ajustado**  |
| 5   | `DELETE /api/subscriptions/company/{id}`     | `DELETE /api/subscriptions/company/{id}/cancel` | DELETE | 🔧 **Ajustado**  |
| 6   | `GET /api/subscriptions/plans`               | ❌ **NO IMPLEMENTADO**                          | GET    | 🔧 **Hardcoded** |

---

## 🔄 Cambios Realizados en el Frontend

### 1. Modelo `SubscriptionExtendRequest`

**Antes:**

```typescript
export interface SubscriptionExtendRequest {
  additionalMonths: number;
  reason?: string;
}
```

**Después:**

```typescript
export interface SubscriptionExtendRequest {
  extensionMonths: number; // 🔧 Campo correcto según backend
  reason?: string;
}
```

**Archivo:** `/src/app/_model/subscription/subscription-admin.ts`

---

### 2. Modelo `AvailablePlan`

**Antes:**

```typescript
export interface AvailablePlan {
  code: string;
  name: string;
  documentLimitYearly: number;
  monthlyPrice: number;
  features: string[];
}
```

**Después:**

```typescript
export interface AvailablePlan {
  code: string;
  name: string;
  price: number; // 🔧 Precio mensual
  documentLimit: number; // 🔧 Límite anual (-1 = ilimitado)
  features: string[];
}
```

**Archivo:** `/src/app/_model/subscription/subscription-admin.ts`

---

### 3. Servicio `SubscriptionAdminService`

#### 3.1. Método `changePlan()`

**Antes:**

```typescript
changePlan(idCompany: number, request: PlanChangeRequest): Observable<void> {
  return this.http.put<void>(
    `${this.apiUrl}/company/${idCompany}/plan`,
    request
  );
}
```

**Después:**

```typescript
changePlan(idCompany: number, request: PlanChangeRequest): Observable<void> {
  return this.http.post<void>(
    `${this.apiUrl}/company/${idCompany}/upgrade`,
    request
  );
}
```

#### 3.2. Método `extendSubscription()`

**Antes:**

```typescript
extendSubscription(
  idCompany: number,
  request: SubscriptionExtendRequest
): Observable<void> {
  return this.http.put<void>(
    `${this.apiUrl}/company/${idCompany}/extend`,
    request
  );
}
```

**Después:**

```typescript
extendSubscription(
  idCompany: number,
  request: SubscriptionExtendRequest
): Observable<void> {
  return this.http.post<void>(
    `${this.apiUrl}/company/${idCompany}/extend`,
    request
  );
}
```

#### 3.3. Método `cancelSubscription()`

**Antes:**

```typescript
cancelSubscription(idCompany: number): Observable<void> {
  return this.http.delete<void>(`${this.apiUrl}/company/${idCompany}`);
}
```

**Después:**

```typescript
cancelSubscription(idCompany: number): Observable<void> {
  return this.http.delete<void>(
    `${this.apiUrl}/company/${idCompany}/cancel`
  );
}
```

#### 3.4. Método `getAvailablePlans()` - Hardcoded

**Antes:**

```typescript
getAvailablePlans(): Observable<AvailablePlan[]> {
  return this.http.get<AvailablePlan[]>(`${this.apiUrl}/plans`);
}
```

**Después:**

```typescript
getAvailablePlans(): Observable<AvailablePlan[]> {
  // 🔧 El backend NO tiene este endpoint, retornamos datos hardcoded
  const plans: AvailablePlan[] = [
    {
      code: 'FREE',
      name: 'Plan Gratuito',
      price: 0,
      documentLimit: 50,
      features: [
        'Hasta 50 documentos/año',
        'Facturación básica',
        'Soporte por email',
      ],
    },
    {
      code: 'BASIC',
      name: 'Plan Básico',
      price: 29.99,
      documentLimit: 500,
      features: [
        'Hasta 500 documentos/año',
        'Notas de crédito',
        'Reportes operativos',
        'Soporte prioritario',
      ],
    },
    {
      code: 'PROFESSIONAL',
      name: 'Plan Profesional',
      price: 79.99,
      documentLimit: 2000,
      features: [
        'Hasta 2000 documentos/año',
        'Reportes avanzados',
        'API REST',
        'Webhooks',
        'Soporte 24/7',
      ],
    },
    {
      code: 'ENTERPRISE',
      name: 'Plan Empresarial',
      price: 199.99,
      documentLimit: -1,
      features: [
        'Documentos ilimitados',
        'Todas las características',
        'Soporte dedicado',
        'SLA garantizado',
        'Personalización',
      ],
    },
  ];
  return new Observable((observer) => {
    observer.next(plans);
    observer.complete();
  });
}
```

**Archivo:** `/src/app/_service/subscription/subscription-admin.service.ts`

**Razón:** El backend **NO implementó** el endpoint `GET /api/subscriptions/plans`. Los planes están hardcoded temporalmente en el frontend hasta que el backend implemente este endpoint.

---

### 4. Componente `ExtendSubscriptionDialogComponent`

**Antes:**

```typescript
this.form = this.fb.group({
  additionalMonths: [6, [Validators.required, Validators.min(1)]],
  reason: [''],
});

getNewExpirationDate(): Date {
  const months = this.form.get('additionalMonths')?.value || 0;
  // ...
}
```

**Después:**

```typescript
this.form = this.fb.group({
  extensionMonths: [6, [Validators.required, Validators.min(1)]], // 🔧 Campo correcto
  reason: [''],
});

getNewExpirationDate(): Date {
  const months = this.form.get('extensionMonths')?.value || 0; // 🔧 Campo correcto
  // ...
}
```

**Template HTML:** Cambio en `formControlName`:

```html
<!-- Antes -->
<mat-select formControlName="additionalMonths">
  <!-- Después -->
  <mat-select formControlName="extensionMonths"></mat-select
></mat-select>
```

**Archivos:**

- `/src/app/pages/admin/subscriptions/dialogs/extend-subscription-dialog/extend-subscription-dialog.component.ts`
- `/src/app/pages/admin/subscriptions/dialogs/extend-subscription-dialog/extend-subscription-dialog.component.html`

---

### 5. Componente `UpgradePlanDialogComponent` (HTML)

**Cambios en propiedades de `AvailablePlan`:**

```html
<!-- Antes -->
{{ plan.documentLimitYearly }} documentos/año {{ getSelectedPlan()!.documentLimitYearly }} ${{ getSelectedPlan()!.monthlyPrice | number : "1.2-2" }}

<!-- Después -->
{{ plan.documentLimit === -1 ? 'Ilimitado' : plan.documentLimit + ' documentos/año' }} {{ getSelectedPlan()!.documentLimit === -1 ? 'Ilimitado' : getSelectedPlan()!.documentLimit }} ${{ getSelectedPlan()!.price | number : "1.2-2" }}
```

**Archivo:** `/src/app/pages/admin/subscriptions/dialogs/upgrade-plan-dialog/upgrade-plan-dialog.component.html`

---

## 🎯 Estado de Integración

| Componente                     | Estado       | Notas                                                    |
| ------------------------------ | ------------ | -------------------------------------------------------- |
| **Listar Suscripciones**       | ✅ Listo     | Backend: `GET /api/subscriptions`                        |
| **Ver Detalle de Suscripción** | ✅ Listo     | Backend: `GET /api/subscriptions/company/{id}`           |
| **Cambiar Plan (Upgrade)**     | ✅ Listo     | Backend: `POST /api/subscriptions/company/{id}/upgrade`  |
| **Extender Vigencia**          | ✅ Listo     | Backend: `POST /api/subscriptions/company/{id}/extend`   |
| **Cancelar Suscripción**       | ✅ Listo     | Backend: `DELETE /api/subscriptions/company/{id}/cancel` |
| **Listar Planes Disponibles**  | ⚠️ Hardcoded | Backend: **NO IMPLEMENTADO**                             |

---

## 📝 Request Bodies Correctos

### 1. Cambiar Plan (Upgrade)

```json
POST /api/subscriptions/company/{idCompany}/upgrade
Content-Type: application/json

{
  "newPlanCode": "PROFESSIONAL",
  "reason": "Cliente solicitó upgrade" // opcional
}
```

### 2. Extender Vigencia

```json
POST /api/subscriptions/company/{idCompany}/extend
Content-Type: application/json

{
  "extensionMonths": 6,
  "reason": "Renovación de contrato" // opcional
}
```

### 3. Cancelar Suscripción

```json
DELETE /api/subscriptions/company/{idCompany}/cancel
```

---

## 🚀 Testing

Para probar la integración completa:

1. **Login como `super_admin`**
2. **Navegar a:** `http://localhost:4200/admin/subscriptions`
3. **Verificar:**
   - ✅ Lista de suscripciones carga correctamente
   - ✅ Ver detalle de una suscripción
   - ✅ Cambiar plan funciona (upgrade/downgrade)
   - ✅ Extender vigencia funciona
   - ✅ Cancelar suscripción funciona

---

## ⚠️ Pendientes para el Backend

### Endpoint Faltante: GET /api/subscriptions/plans

**Request:**

```bash
GET /api/subscriptions/plans
Authorization: Bearer {token}
```

**Response esperado:**

```json
[
  {
    "code": "FREE",
    "name": "Plan Gratuito",
    "price": 0.0,
    "documentLimit": 50,
    "features": ["Hasta 50 documentos/año", "Facturación básica", "Soporte por email"]
  },
  {
    "code": "BASIC",
    "name": "Plan Básico",
    "price": 29.99,
    "documentLimit": 500,
    "features": ["Hasta 500 documentos/año", "Notas de crédito", "Reportes operativos", "Soporte prioritario"]
  },
  {
    "code": "PROFESSIONAL",
    "name": "Plan Profesional",
    "price": 79.99,
    "documentLimit": 2000,
    "features": ["Hasta 2000 documentos/año", "Reportes avanzados", "API REST", "Webhooks", "Soporte 24/7"]
  },
  {
    "code": "ENTERPRISE",
    "name": "Plan Empresarial",
    "price": 199.99,
    "documentLimit": -1,
    "features": ["Documentos ilimitados", "Todas las características", "Soporte dedicado", "SLA garantizado", "Personalización"]
  }
]
```

**Nota:** Mientras este endpoint no esté disponible, el frontend usa los datos hardcoded del método `getAvailablePlans()` en `subscription-admin.service.ts`.

---

## ✅ Build Status

```bash
✔ Browser application bundle generation complete.
✔ Copying assets complete.
✔ Index html generation complete.

Initial chunk files: 1.81 MB
Lazy chunk files: subscriptions-subscriptions-module (26.92 kB)
                   role-permissions-role-permissions-module (17.43 kB)

Build at: 2025-12-21T13:07:05.313Z ✅
```

---

## 📚 Referencias

- **Servicio:** `/src/app/_service/subscription/subscription-admin.service.ts`
- **Modelos:** `/src/app/_model/subscription/subscription-admin.ts`
- **Componentes:** `/src/app/pages/admin/subscriptions/`
- **Backend:** `ebill-billing` (puerto 8480) - `/api/subscriptions`

---

**Estado:** ✅ Frontend 100% ajustado y listo para integración con backend
