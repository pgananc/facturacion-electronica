# 📋 Sistema de Suscripciones - Siguiente Pasos

## ✅ Lo que YA tenemos funcionando

### 1️⃣ Backend (ebill-authentication)

- ✅ Endpoint `/api/permission/validate-feature` funcionando
- ✅ 15 feature codes definidos
- ✅ Validación de ROL + PLAN
- ✅ Integración con ebill-billing

### 2️⃣ Frontend - Core

- ✅ `PermissionService` - Validación de features
- ✅ `PermissionGuard` - Protección de rutas
- ✅ `HasPermissionDirective` - Mostrar/ocultar elementos
- ✅ DTOs correctos (sin `idUser`)
- ✅ Toast deduplication (no spam)
- ✅ Manejo de errores mejorado

### 3️⃣ Frontend - Rutas Protegidas

- ✅ `/invoices` → `INVOICE_CREATE`
- ✅ `/reports/sales-dashboard` → `REPORT_ADVANCED`
- ✅ `/reports/branch-comparison` → `REPORT_ADVANCED`

---

## 🚧 Lo que FALTA implementar

### 🎯 PRIORIDAD ALTA

#### 1. Proteger más rutas con feature codes

**Rutas SIN protección actual:**

| Ruta                   | Feature Code Sugerido      | Acción                  |
| ---------------------- | -------------------------- | ----------------------- |
| `/credit-notes`        | `CREDIT_NOTE_CREATE`       | Agregar PermissionGuard |
| `/credit-notes/create` | `CREDIT_NOTE_CREATE`       | Agregar PermissionGuard |
| `/clients`             | `CLIENT_CREATE`            | Agregar PermissionGuard |
| `/users`               | `USER_MANAGEMENT`          | Agregar PermissionGuard |
| `/companies`           | `COMPANY_SETTINGS`         | Agregar PermissionGuard |
| `/establishments`      | `ESTABLISHMENT_MANAGEMENT` | Agregar PermissionGuard |
| `/invoice-points`      | `ESTABLISHMENT_MANAGEMENT` | Agregar PermissionGuard |

**Impacto:** Sin esto, usuarios con planes básicos pueden acceder a funciones premium.

---

#### 2. Definir feature para Borradores

**Problema:** `/invoices/drafts` no tiene feature code asignado.

**Opciones:**

- **A)** Usar `INVOICE_CREATE` (borradores son parte de crear facturas)
- **B)** Pedir al backend agregar `INVOICE_DRAFT_MANAGE`
- **C)** Dejarlo sin validación (accesible para todos)

**Recomendación:** Opción A - usar `INVOICE_CREATE`

---

#### 3. Usar directiva `*hasPermission` en templates

**Actualmente:** Solo protegemos rutas, pero dentro de los componentes NO ocultamos botones/acciones.

**Ejemplo de lo que falta:**

```html
<!-- En welcome.component.html -->
<mat-card class="action-card" routerLink="/reports/sales-dashboard">
  <!-- ❌ Este card se muestra aunque el usuario no tenga acceso -->
  <h3>Dashboard Avanzado</h3>
</mat-card>

<!-- ✅ DEBERÍA ser: -->
<mat-card *hasPermission="'REPORT_ADVANCED'" class="action-card" routerLink="/reports/sales-dashboard">
  <h3>Dashboard Avanzado</h3>
</mat-card>

<!-- O con mensaje de upgrade: -->
<mat-card *hasPermission="'REPORT_ADVANCED'; else upgradeTpl" class="action-card" routerLink="/reports/sales-dashboard">
  <h3>Dashboard Avanzado</h3>
</mat-card>
<ng-template #upgradeTpl>
  <mat-card class="action-card upgrade-card" (click)="showUpgradeModal()">
    <h3>🔒 Dashboard Avanzado</h3>
    <p>Requiere plan PRO</p>
  </mat-card>
</ng-template>
```

**Impacto:** Mejor UX - usuarios ven solo lo que pueden usar.

---

#### 4. Mostrar información de suscripción en el UI

**Lo que falta:**

##### A) Indicador de Plan Actual

```html
<!-- En navbar o sidebar -->
<div class="subscription-badge">
  <mat-icon>workspace_premium</mat-icon>
  <span>Plan: {{ currentPlan }}</span>
</div>
```

##### B) Límites de Uso

```html
<!-- Mostrar uso de documentos -->
<div class="usage-indicator">
  <span>Facturas: {{ documentsUsed }}/{{ documentsLimit }}</span>
  <mat-progress-bar [value]="usagePercent" [color]="usagePercent > 80 ? 'warn' : 'primary'"> </mat-progress-bar>
</div>
```

##### C) Alertas de Límite

```typescript
// En un servicio o componente global
if (usagePercent > 90) {
  this.toastService.warning("⚠️ Has usado el 90% de tus documentos");
}
```

**Impacto:** Transparencia para el usuario sobre su plan.

---

#### 5. Modal/Página de Upgrade

**Problema:** Cuando un usuario no tiene acceso, solo ve un toast pero no hay CTA claro.

**Solución:** Crear componente de upgrade:

```typescript
// upgrade-modal.component.ts
@Component({
  selector: "app-upgrade-modal",
  template: `
    <div class="upgrade-modal">
      <h2>🔒 Funcionalidad Premium</h2>
      <p>{{ message }}</p>

      <div class="plans-comparison">
        <div class="plan current">
          <h3>Tu Plan: {{ currentPlan }}</h3>
          <!-- Lista features actuales -->
        </div>

        <div class="plan recommended">
          <h3>Plan Recomendado: {{ recommendedPlan }}</h3>
          <!-- Lista features nuevas -->
          <button mat-raised-button color="primary">Actualizar Plan</button>
        </div>
      </div>

      <a [href]="upgradeUrl" target="_blank">Ver planes</a>
    </div>
  `,
})
export class UpgradeModalComponent {}
```

**Usar en PermissionGuard:**

```typescript
if (!response.allowed && response.upgradeUrl) {
  this.dialog.open(UpgradeModalComponent, {
    data: {
      message: response.message,
      upgradeUrl: response.upgradeUrl,
    },
  });
}
```

---

### 🎨 PRIORIDAD MEDIA

#### 6. Cachear validaciones de features

**Problema:** Cada vez que se navega a una ruta, se hace request al backend.

**Solución:**

```typescript
// En PermissionService
private featureCache = new Map<string, { allowed: boolean, expires: number }>();
private CACHE_TTL = 5 * 60 * 1000; // 5 minutos

canAccess(featureCode: string): Observable<boolean> {
  const cached = this.featureCache.get(featureCode);
  if (cached && cached.expires > Date.now()) {
    return of(cached.allowed);
  }

  return this.http.post(...).pipe(
    tap(response => {
      this.featureCache.set(featureCode, {
        allowed: response.allowed,
        expires: Date.now() + this.CACHE_TTL
      });
    })
  );
}
```

**Impacto:** Menos requests al backend, mejor performance.

---

#### 7. Interceptor para documentos

**Problema:** Si un usuario alcanza su límite de documentos, debería bloquearse ANTES de intentar crear.

**Solución:**

```typescript
// En billing.component.ts antes de crear factura
this.permissionService.canEmitDocument("INVOICE").subscribe((allowed) => {
  if (!allowed) {
    this.toastService.error("Has alcanzado el límite de facturas de tu plan");
    return;
  }
  // Continuar con creación
});
```

**También en backend:** El endpoint `/api/subscription/{id}/can-emit` ya existe, usarlo.

---

#### 8. Logs y Analytics

**Agregar tracking cuando:**

- Usuario intenta acceder a feature sin permiso
- Usuario alcanza límite de documentos
- Usuario ve modal de upgrade

```typescript
// analytics.service.ts
logFeatureAttempt(feature: string, allowed: boolean, reason?: string) {
  console.log(`[Analytics] Feature: ${feature}, Allowed: ${allowed}, Reason: ${reason}`);
  // Enviar a servicio de analytics (Google Analytics, Mixpanel, etc.)
}
```

---

### 🔮 PRIORIDAD BAJA (Nice to have)

#### 9. Preview de Features

**Permitir a usuarios "ver" features premium pero deshabilitadas:**

```html
<div *hasPermission="'REPORT_ADVANCED'; else previewTpl">
  <!-- Contenido completo -->
</div>

<ng-template #previewTpl>
  <div class="feature-preview">
    <div class="blur-overlay">
      <!-- Vista previa borrosa -->
    </div>
    <div class="upgrade-cta">
      <h3>🔒 Desbloquea Reportes Avanzados</h3>
      <button>Actualizar a PRO</button>
    </div>
  </div>
</ng-template>
```

---

#### 10. Notificaciones de Expiración

**Alertar cuando la suscripción está por vencer:**

```typescript
// En app.component.ts ngOnInit
this.permissionService.getSubscription().subscribe((sub) => {
  if (sub && sub.status === "TRIAL") {
    const daysLeft = this.getDaysUntilExpiration(sub.endDate);
    if (daysLeft <= 7) {
      this.toastService.warning(`⚠️ Tu período de prueba expira en ${daysLeft} días`);
    }
  }
});
```

---

#### 11. Feature Flags

**Para habilitar/deshabilitar features sin redeploy:**

```typescript
// feature-flag.service.ts
isFeatureEnabled(feature: string): boolean {
  const flags = this.getFlags(); // Desde backend o config
  return flags[feature] === true;
}

// Usar en combinación con permisos
*ngIf="featureFlagService.isFeatureEnabled('NEW_DASHBOARD') && hasPermission('REPORT_ADVANCED')"
```

---

## 📊 Resumen de Prioridades

### 🔴 CRÍTICO (Hacer ahora)

1. ✅ Proteger rutas faltantes con feature codes
2. ✅ Definir feature para `/invoices/drafts`
3. ✅ Usar `*hasPermission` en templates principales

### 🟡 IMPORTANTE (Próxima semana)

4. Mostrar info de suscripción en UI
5. Crear modal de upgrade
6. Cachear validaciones

### 🟢 MEJORAS (Cuando haya tiempo)

7. Interceptor para límites de documentos
8. Analytics de features
9. Preview de features
10. Notificaciones de expiración
11. Feature flags

---

## 🎯 Plan de Acción Sugerido

### Fase 1: Completar Protecciones (1-2 horas)

```bash
1. Agregar PermissionGuard a todas las rutas que faltan
2. Usar *hasPermission en welcome.component.html
3. Usar *hasPermission en menú lateral
4. Definir feature para drafts
```

### Fase 2: UX de Suscripción (2-3 horas)

```bash
1. Crear componente subscription-badge
2. Crear componente usage-indicator
3. Crear upgrade-modal.component
4. Integrar modal en PermissionGuard
```

### Fase 3: Optimizaciones (1-2 horas)

```bash
1. Implementar cache de features
2. Agregar interceptor de límites
3. Testing completo
```

---

## 🧪 Checklist de Testing

Después de implementar, probar:

- [ ] Usuario con plan FREE no puede acceder a features premium
- [ ] Usuario con plan BASIC puede crear facturas pero no reportes avanzados
- [ ] Usuario con plan PRO puede acceder a todo
- [ ] Botones/cards de features premium están ocultos o deshabilitados
- [ ] Modal de upgrade se muestra cuando no hay acceso
- [ ] Indicador de plan se muestra correctamente
- [ ] Barra de uso de documentos funciona
- [ ] Cache de features reduce requests al backend
- [ ] Límite de documentos bloquea creación

---

## 📝 ¿Por dónde empezar?

**Te recomiendo empezar por la Fase 1:**

1. Proteger las rutas faltantes (5 minutos cada una)
2. Agregar `*hasPermission` en el welcome (10 minutos)
3. Probar que funcione todo

**¿Quieres que implemente alguna de estas cosas ahora?** 🚀
