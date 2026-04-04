# 🎨 Frontend - Sistema de Permisos y Suscripciones

**Fecha:** 14 de Diciembre, 2025  
**Estado:** ✅ Implementado completamente

---

## 📋 **Resumen de Implementación**

Se han implementado todos los componentes del frontend para el sistema de permisos basado en **ROL + SUSCRIPCIÓN**.

### **✅ Archivos Creados:**

1. **Interfaces TypeScript** (`src/app/_model/subscription/`):

   - `subscription-plan.ts` - Define el plan de suscripción
   - `company-subscription.ts` - Suscripción de la compañía
   - `subscription-usage.ts` - Uso de documentos y estadísticas
   - `feature-validation-response.ts` - Respuestas de validación

2. **Servicio Principal** (`src/app/_service/permission/`):

   - `permission.service.ts` - Gestión completa de permisos

3. **Directiva** (`src/app/_directives/`):

   - `has-permission.directive.ts` - Directiva para templates

4. **Guard** (`src/app/_guard/`):
   - `permission.guard.ts` - Protección de rutas

### **✅ Archivos Modificados:**

1. `shared.module.ts` - Exporta `HasPermissionDirective`
2. `app-routing.module.ts` - Agrega `PermissionGuard` a `/welcome`
3. `reports.module.ts` - Protege reportes avanzados
4. `billing.module.ts` - Protege draft invoices

---

## 🚀 **Uso del Sistema de Permisos**

### **1. PermissionService**

Inyectar el servicio en cualquier componente:

```typescript
import { PermissionService } from "../_service/permission/permission.service";

export class MyComponent {
  constructor(private permissionService: PermissionService) {}

  ngOnInit() {
    // Verificar si puede acceder a una feature
    this.permissionService.canAccess("dashboard.view").subscribe((allowed) => {
      if (allowed) {
        console.log("✅ Tiene acceso al dashboard");
      }
    });

    // Verificar si puede emitir un documento
    this.permissionService.canEmitDocument("INVOICE").subscribe((allowed) => {
      if (allowed) {
        console.log("✅ Puede emitir facturas");
      }
    });

    // Obtener suscripción actual
    this.permissionService.getSubscription().subscribe((subscription) => {
      console.log("Plan actual:", subscription?.plan.name);
      console.log("Documentos restantes:", subscription?.documentsRemaining);
    });
  }
}
```

### **2. HasPermissionDirective**

Usar en templates para mostrar/ocultar elementos:

```html
<!-- Mostrar solo si tiene permiso -->
<div *hasPermission="'dashboard.view'">
  <app-welcome-dashboard></app-welcome-dashboard>
</div>

<!-- Con template de fallback para upgrade -->
<button *hasPermission="'reports.advanced'; else upgradeTpl">📊 Ver Reportes Avanzados</button>
<ng-template #upgradeTpl>
  <button class="upgrade-btn" (click)="showUpgradeDialog()">🔒 Actualizar Plan para Acceder</button>
</ng-template>

<!-- Ocultar features de planes superiores -->
<mat-menu-item *hasPermission="'billing.draft.manage'" routerLink="/invoices/drafts">
  <mat-icon>drafts</mat-icon>
  <span>Borradores</span>
</mat-menu-item>
```

### **3. PermissionGuard**

Proteger rutas en módulos:

```typescript
import { PermissionGuard } from "../../_guard/permission.guard";

const routes: Routes = [
  {
    path: "advanced-report",
    component: AdvancedReportComponent,
    canActivate: [SessionValidationGuard, PermissionGuard],
    data: { permission: "reports.advanced" },
  },
];
```

---

## 🔑 **Feature Codes Implementados**

Según `FASE1-IMPLEMENTACION-COMPLETA.md`, estos son los códigos de features:

### **Dashboard y Bienvenida**

- `dashboard.view` → Plan: BASIC+

### **Reportes Básicos (Todos los planes)**

- `reports.basic` → Incluido en FREE

### **Reportes Avanzados (PRO+)**

- `reports.advanced` → Plan: PRO+
- Incluye: `sales-dashboard`, `branch-comparison`

### **Borradores (BASIC+)**

- `billing.draft.manage` → Plan: BASIC+

### **Importación CSV (PRO+)**

- `products.csv.import` → Plan: PRO+

### **API y Webhooks (PRO+)**

- `api.access` → Plan: PRO+
- `webhooks.manage` → Plan: PRO+

### **Integraciones ERP (BUSINESS+)**

- `erp.integration` → Plan: BUSINESS+

### **Inventario (BUSINESS+)**

- `inventory.manage` → Plan: BUSINESS+

### **Reportes Programados (PRO+)**

- `reports.scheduled` → Plan: PRO+

### **Guías de Remisión (PRO+)**

- `guides.manage` → Plan: PRO+

### **Notificaciones WhatsApp (PRO+)**

- `whatsapp.notifications` → Plan: PRO+

---

## 📦 **Métodos del PermissionService**

### **Verificación de Acceso**

```typescript
// Verificar acceso simple (boolean)
canAccess(featureCode: string): Observable<boolean>

// Verificar con detalles (incluye motivo)
canAccessWithDetails(featureCode: string): Observable<FeatureValidationResponse>

// Verificar si puede emitir documento
canEmitDocument(documentType): Observable<boolean>

// Verificar emisión con detalles
canEmitDocumentWithDetails(documentType): Observable<CanEmitDocumentResponse>
```

### **Roles**

```typescript
// Verificar si es super admin
isSuperAdmin(): boolean

// Verificar si tiene un rol específico
hasRole(roleName: string): boolean
```

### **Suscripción**

```typescript
// Obtener suscripción actual (Observable)
getSubscription(): Observable<CompanySubscription | null>

// Obtener suscripción actual (valor sincrónico)
getSubscriptionValue(): CompanySubscription | null

// Refrescar suscripción desde backend
refreshSubscription(): void

// Obtener uso de documentos
getUsage(idCompany?: number): Observable<SubscriptionUsageResponse | null>
```

### **Plan**

```typescript
// Obtener código del plan actual
getCurrentPlanCode(): string

// Obtener plan actual
getCurrentPlan(): SubscriptionPlan | null

// Verificar si el plan tiene una feature
planHasFeature(feature: keyof SubscriptionPlan): boolean
```

### **Estado de Suscripción**

```typescript
// Verificar si está activa
isSubscriptionActive(): boolean

// Verificar si está expirada
isSubscriptionExpired(): boolean

// Obtener porcentaje de uso
getUsagePercent(): number

// Obtener documentos restantes
getDocumentsRemaining(): number
```

---

## 🎯 **Rutas Protegidas**

### **Implementadas en esta sesión:**

| Ruta                         | Permiso                | Plan Requerido |
| ---------------------------- | ---------------------- | -------------- |
| `/welcome`                   | `dashboard.view`       | BASIC+         |
| `/reports/sales-dashboard`   | `reports.advanced`     | PRO+           |
| `/reports/branch-comparison` | `reports.advanced`     | PRO+           |
| `/invoices/drafts`           | `billing.draft.manage` | BASIC+         |

---

## 🔄 **Flujo de Validación**

```
1. Usuario intenta acceder a ruta protegida
   ↓
2. PermissionGuard intercepta
   ↓
3. Verifica si es super_admin (bypass)
   ↓ NO
4. Llama a PermissionService.canAccessWithDetails(featureCode)
   ↓
5. PermissionService llama a backend:
   POST /api/permission/validate-feature
   ↓
6. Backend (authentication) valida:
   - ROL: ¿El usuario tiene permiso?
   - PLAN: ¿La suscripción incluye la feature?
   ↓
7. Backend responde: { allowed: boolean, reason?: string, message: string }
   ↓
8. PermissionGuard decide:
   - ✅ allowed=true → Permite acceso
   - ❌ allowed=false → Redirige + muestra toast
```

---

## 💾 **Caché de Suscripción**

El `PermissionService` cachea la suscripción:

1. **En memoria**: `BehaviorSubject` para uso reactivo
2. **En localStorage**: Fallback si backend no responde

```typescript
// Se carga automáticamente al instanciar el servicio
constructor(private http: HttpClient) {
  this.loadSubscription();
}

// Actualizar manualmente después de cambiar plan
this.permissionService.refreshSubscription();
```

---

## 🚨 **Mensajes de Error**

El `PermissionGuard` muestra mensajes específicos:

| Reason            | Mensaje                                         | Acción                |
| ----------------- | ----------------------------------------------- | --------------------- |
| `NO_SUBSCRIPTION` | 🔒 Esta funcionalidad requiere un plan superior | Redirige a `/welcome` |
| `LIMIT_EXCEEDED`  | ⚠️ Has alcanzado el límite de tu plan           | Redirige a `/welcome` |
| `EXPIRED`         | ❌ Tu suscripción ha expirado                   | Redirige a `/welcome` |
| `NO_ROLE`         | 🚫 No tienes permisos para acceder              | Redirige a `/welcome` |

---

## 🧪 **Testing Local**

Para probar sin backend:

1. **Super Admin Bypass**: El frontend detecta `super_admin` en el token JWT y permite todo
2. **Cache localStorage**: Si backend no responde, usa datos cacheados
3. **Fallback seguro**: Si no hay datos, deniega por defecto

---

## 📝 **Próximos Pasos**

### **Pendientes en Backend:**

- [ ] Implementar `POST /api/permission/validate-feature` en `ebill-authentication`
- [ ] Implementar `GET /api/subscription/{id}/current` en `ebill-billing`
- [ ] Implementar `POST /api/subscription/{id}/can-emit` en `ebill-billing`
- [ ] Crear tablas en base de datos

### **Mejoras Frontend (Futuro):**

- [ ] Crear componente de upgrade plan (`/subscription`)
- [ ] Mostrar barra de progreso de uso de documentos
- [ ] Alertas cuando se acerca al límite
- [ ] Modal de "Plan requerido" con detalles de features

---

## 🎓 **Ejemplo Completo: Componente con Permisos**

```typescript
import { Component, OnInit } from "@angular/core";
import { PermissionService } from "../../_service/permission/permission.service";
import { CompanySubscription } from "../../_model/subscription/company-subscription";

@Component({
  selector: "app-dashboard",
  templateUrl: "./dashboard.component.html",
})
export class DashboardComponent implements OnInit {
  subscription: CompanySubscription | null = null;
  canAccessAdvancedReports = false;
  canEmitInvoices = false;

  constructor(private permissionService: PermissionService) {}

  ngOnInit() {
    // Cargar suscripción
    this.permissionService.getSubscription().subscribe((sub) => {
      this.subscription = sub;
    });

    // Verificar permisos
    this.permissionService.canAccess("reports.advanced").subscribe((allowed) => {
      this.canAccessAdvancedReports = allowed;
    });

    this.permissionService.canEmitDocument("INVOICE").subscribe((allowed) => {
      this.canEmitInvoices = allowed;
    });
  }

  createInvoice() {
    this.permissionService.canEmitDocumentWithDetails("INVOICE").subscribe((result) => {
      if (result.allowed) {
        // Proceder con creación
      } else {
        alert(result.message);
      }
    });
  }
}
```

**Template:**

```html
<div class="dashboard">
  <!-- Mostrar info de suscripción -->
  <div *ngIf="subscription">
    <h3>Plan: {{ subscription.plan.name }}</h3>
    <p>Documentos restantes: {{ subscription.documentsRemaining }}</p>
    <mat-progress-bar [value]="subscription.percentUsed"></mat-progress-bar>
  </div>

  <!-- Reportes avanzados (solo si tiene permiso) -->
  <button *hasPermission="'reports.advanced'" (click)="viewAdvancedReports()">📊 Reportes Avanzados</button>

  <!-- Botón de crear factura con validación -->
  <button (click)="createInvoice()" [disabled]="!canEmitInvoices">📄 Nueva Factura</button>
</div>
```

---

## ✅ **Checklist de Implementación Frontend**

- [x] Interfaces TypeScript creadas
- [x] PermissionService implementado
- [x] HasPermissionDirective implementada
- [x] PermissionGuard implementado
- [x] SharedModule actualizado
- [x] Rutas protegidas configuradas
- [x] Environment variables verificadas

**🎉 Implementación Frontend Completa!**

---

## 📚 **Referencias**

- `BACKEND-CHECKLIST-MICROSERVICIOS.md` - Checklist de tareas backend
- `FASE1-IMPLEMENTACION-COMPLETA.md` - Especificación completa del sistema
- `ROLES-SUBSCRIPTION-ARCHITECTURE.md` - Arquitectura de roles y suscripciones
