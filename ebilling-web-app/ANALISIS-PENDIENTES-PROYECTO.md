# 📋 Análisis Completo: Qué Falta Probar o Crear

**Fecha:** Diciembre 2025  
**Estado del Proyecto:** ✅ Frontend implementado | ⏳ Backend parcialmente implementado | ❌ Testing insuficiente

---

## 📊 Resumen Ejecutivo

### ✅ Lo que YA está implementado

1. **Módulos Core:**

   - ✅ Facturación (invoices, credit notes, debit notes)
   - ✅ Clientes (CRUD completo)
   - ✅ Productos (CRUD completo)
   - ✅ Usuarios (CRUD completo)
   - ✅ Empresas (CRUD completo)
   - ✅ Establecimientos y Puntos de Emisión
   - ✅ Reportes (7 tipos de reportes)
   - ✅ Admin (Suscripciones, Permisos, Menús)

2. **Sistema de Seguridad:**

   - ✅ Autenticación JWT
   - ✅ Guards de sesión
   - ✅ Sistema de permisos (PermissionService, PermissionGuard, HasPermissionDirective)
   - ✅ Protección de rutas

3. **UI/UX:**
   - ✅ Material Design implementado
   - ✅ Responsive design (parcial)
   - ✅ Lazy loading de módulos
   - ✅ Manejo de errores

---

## 🔴 PRIORIDAD ALTA: Testing

### 1. Tests Unitarios (Cobertura actual: ~1%)

**Estado Actual:** Solo 4 archivos `.spec.ts`:

- `app.component.spec.ts`
- `add-payment-dialog.component.spec.ts`
- `select-product-dialog.component.spec.ts`
- `select-invoice-point-dialog.component.spec.ts`

**Falta Crear Tests Para:**

#### A) Servicios Críticos (0% cobertura)

- [ ] `PermissionService` - Validación de features
- [ ] `PermissionAdminService` - Gestión de permisos
- [ ] `SubscriptionAdminService` - Gestión de suscripciones
- [ ] `InvoiceService` - Creación y gestión de facturas
- [ ] `CreditNoteService` - Creación de notas de crédito
- [ ] `DebitNoteService` - Creación de notas de débito
- [ ] `ClientService` - CRUD de clientes
- [ ] `ProductService` - CRUD de productos
- [ ] `UserService` - CRUD de usuarios
- [ ] `CompanyService` - CRUD de empresas
- [ ] `MenuService` - Carga de menús
- [ ] `MenuFilterService` - Filtrado de menús por permisos
- [ ] `DashboardService` - Carga de datos del dashboard
- [ ] `ReportService` - Generación de reportes

#### B) Guards (0% cobertura)

- [ ] `PermissionGuard` - Protección de rutas por feature
- [ ] `SessionValidationGuard` - Validación de sesión
- [ ] `CanActiveUserGuard` - Validación de usuario activo
- [ ] `AdminGuard` - Validación de super_admin

#### C) Directivas (0% cobertura)

- [ ] `HasPermissionDirective` - Mostrar/ocultar elementos

#### D) Componentes Críticos (0% cobertura)

- [ ] `BillingComponent` - Creación de facturas
- [ ] `CreateCreditNoteComponent` - Creación de NC
- [ ] `CreateDebitNoteComponent` - Creación de ND
- [ ] `SubscriptionListComponent` - Lista de suscripciones
- [ ] `PermissionMatrixComponent` - Matriz de permisos
- [ ] `MenuListComponent` - Lista de menús
- [ ] `SalesDashboardComponent` - Dashboard de ventas

**Meta:** Alcanzar **>80% de cobertura** en servicios y componentes críticos.

---

### 2. Tests de Integración (0% implementado)

**Falta Crear:**

- [ ] **Flujo completo de facturación:**
  - Login → Seleccionar empresa → Crear factura → Guardar borrador → Enviar al SRI → Ver ticket
- [ ] **Flujo de notas de crédito:**
  - Seleccionar factura → Crear NC → Enviar al SRI → Verificar actualización
- [ ] **Flujo de permisos:**
  - Login como super_admin → Cambiar permisos de rol → Login como usuario afectado → Verificar cambios
- [ ] **Flujo de suscripciones:**
  - Login como super_admin → Cambiar plan de empresa → Verificar límites actualizados
- [ ] **Flujo de menús:**
  - Login → Cargar menús → Verificar filtrado por permisos → Verificar feature codes

**Herramientas sugeridas:**

- Cypress o Playwright para E2E
- Angular Testing Utilities para integración

---

### 3. Tests Manuales Pendientes

**Checklist según `INTEGRATION-TESTING-GUIDE.md`:**

- [ ] Servicios backend corriendo (health checks OK)
- [ ] Endpoint validate-feature responde correctamente
- [ ] Endpoint get subscription responde correctamente
- [ ] Endpoint can-emit responde correctamente
- [ ] Frontend inicia sin errores
- [ ] Login exitoso
- [ ] Suscripción se carga (verificar consola)
- [ ] Ruta `/welcome` protegida según plan
- [ ] Ruta `/reports/sales-dashboard` protegida según plan
- [ ] Ruta `/invoices/drafts` protegida según plan
- [ ] Directiva `*hasPermission` funciona
- [ ] Límites de documentos se respetan
- [ ] Super admin bypass funciona
- [ ] Mensajes de error son claros
- [ ] Caché localStorage funciona si backend falla

---

## 🟡 PRIORIDAD MEDIA: Features Pendientes

### 1. Protección de Rutas Faltantes

**Según `SUBSCRIPTION-TODO-NEXT-STEPS.md`:**

Rutas SIN protección actual:

| Ruta                   | Feature Code Sugerido      | Estado          |
| ---------------------- | -------------------------- | --------------- |
| `/credit-notes`        | `CREDIT_NOTE_CREATE`       | ⚠️ Ya protegida |
| `/credit-notes/create` | `CREDIT_NOTE_CREATE`       | ⚠️ Ya protegida |
| `/clients`             | `CLIENT_CREATE`            | ❌ Falta        |
| `/users`               | `USER_MANAGEMENT`          | ❌ Falta        |
| `/companies`           | `COMPANY_SETTINGS`         | ❌ Falta        |
| `/establishments`      | `ESTABLISHMENT_MANAGEMENT` | ❌ Falta        |
| `/invoice-points`      | `ESTABLISHMENT_MANAGEMENT` | ❌ Falta        |
| `/products`            | `PRODUCT_MANAGEMENT`       | ❌ Falta        |

**Acción requerida:** Agregar `PermissionGuard` a estas rutas.

---

### 2. Uso de Directiva `*hasPermission` en Templates

**Problema:** Solo protegemos rutas, pero dentro de los componentes NO ocultamos botones/acciones.

**Componentes que necesitan `*hasPermission`:**

- [ ] `welcome.component.html` - Ocultar cards de features premium
- [ ] `sales-dashboard.component.html` - Ocultar si no tiene `REPORT_ADVANCED`
- [ ] `billing.component.html` - Ocultar botones según permisos
- [ ] Menú lateral - Ocultar items según permisos (ya implementado parcialmente)

**Ejemplo de lo que falta:**

```html
<!-- En welcome.component.html -->
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

---

### 3. Información de Suscripción en UI

**Falta implementar:**

#### A) Indicador de Plan Actual

- [ ] Badge en navbar/sidebar mostrando plan actual
- [ ] Color según plan (FREE=gray, BASIC=blue, PRO=gold, etc.)

#### B) Límites de Uso

- [ ] Barra de progreso de documentos usados
- [ ] Mostrar: "Facturas: 150/200"
- [ ] Alerta cuando >80% usado

#### C) Alertas de Límite

- [ ] Toast cuando alcanza 90% de uso
- [ ] Toast cuando alcanza 100% de uso
- [ ] Bloquear creación de documentos si alcanzó límite

**Componente sugerido:** `subscription-badge.component.ts`

---

### 4. Modal/Página de Upgrade

**Problema:** Cuando un usuario no tiene acceso, solo ve un toast pero no hay CTA claro.

**Falta crear:**

- [ ] `UpgradeModalComponent` - Modal de upgrade
- [ ] Comparación de planes (actual vs recomendado)
- [ ] Botón de upgrade con link externo o integración con backend
- [ ] Integrar modal en `PermissionGuard` cuando acceso denegado

**Ruta sugerida:** `/subscription/upgrade` o modal global

---

### 5. Cache de Validaciones de Features

**Problema:** Cada vez que se navega a una ruta, se hace request al backend.

**Falta implementar:**

- [ ] Cache en `PermissionService` con TTL de 5 minutos
- [ ] Invalidar cache cuando cambian permisos
- [ ] Cache en localStorage como fallback

**Impacto:** Menos requests al backend, mejor performance.

---

### 6. Interceptor para Límites de Documentos

**Problema:** Si un usuario alcanza su límite de documentos, debería bloquearse ANTES de intentar crear.

**Falta implementar:**

- [ ] Validación en `BillingComponent` antes de crear factura
- [ ] Validación en `CreateCreditNoteComponent` antes de crear NC
- [ ] Validación en `CreateDebitNoteComponent` antes de crear ND
- [ ] Usar endpoint `/api/subscription/{id}/can-emit`

**Código sugerido:**

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

---

## 🟢 PRIORIDAD BAJA: Mejoras y Optimizaciones

### 1. Variables CSS para Espaciado

**Problema:** Valores de padding/margin hardcodeados inconsistentes.

**Falta crear:**

- [ ] Archivo `_shared/variables.css` con variables CSS
- [ ] Variables: `--spacing-xs`, `--spacing-sm`, `--spacing-md`, `--spacing-lg`, `--spacing-xl`
- [ ] Migrar todos los componentes a usar variables

**Archivos afectados:** Todos los componentes con padding/margin hardcodeados.

---

### 2. Variables CSS para Sombras y Border Radius

**Falta crear:**

- [ ] Variables: `--shadow-sm`, `--shadow-md`, `--shadow-lg`
- [ ] Variables: `--radius-sm`, `--radius-md`, `--radius-lg`
- [ ] Migrar componentes a usar variables

---

### 3. Clases de Utilidad Compartidas

**Falta crear:**

- [ ] Archivo `_shared/utilities.css`
- [ ] Clases: `.btn-primary-action`, `.btn-secondary-action`, `.card-standard`, `.search-card-standard`
- [ ] Migrar componentes a usar clases compartidas

---

### 4. Grid Responsive en Formularios de Búsqueda

**Problema:** Solo `client.component` tiene grid responsive completo.

**Componentes pendientes:**

- [ ] `company.component` - Grid 2x2 sin colapsar en mobile
- [ ] `product.component` - Grid 2x2 sin colapsar en mobile
- [ ] `user.component` - Grid 2x2 sin colapsar en mobile
- [ ] `issuance-point-list.component` - Grid 2x2 sin colapsar
- [ ] `establishment-list.component` - Grid 2x2 sin colapsar
- [ ] `admin-issuance-point-list.component` - Grid 3x3 sin colapsar
- [ ] `admin-establishment-list.component` - Grid 3x3 sin colapsar

**Solución:** Aplicar media query `@media (max-width: 959px)` para cambiar a 1 columna.

---

### 5. Tablas Responsive

**Problema:** Solo `client.component` tiene scroll horizontal y ocultación de columnas.

**Componentes pendientes:**

- [ ] `company.component` - Tabla sin scroll horizontal
- [ ] `product.component` - Tabla sin scroll horizontal
- [ ] `user.component` - Tabla sin scroll horizontal
- [ ] `issuance-point-list.component` - Tabla sin scroll horizontal
- [ ] `establishment-list.component` - Tabla sin scroll horizontal
- [ ] `admin-issuance-point-list.component` - Tabla sin scroll horizontal
- [ ] `admin-establishment-list.component` - Tabla sin scroll horizontal
- [ ] `viewing-invoices.component` - Tabla sin scroll horizontal
- [ ] `draft-invoices.component` - Tabla sin scroll horizontal
- [ ] `company-user.component` - Tabla sin scroll horizontal

**Solución:**

1. Envolver tabla en `.table-wrapper` con `overflow-x: auto`
2. Agregar `min-width` a `mat-table`
3. En mobile (`max-width: 600px`), ocultar columnas menos importantes

---

### 6. Preview de Features Premium

**Idea:** Permitir a usuarios "ver" features premium pero deshabilitadas.

**Falta crear:**

- [ ] Componente `FeaturePreviewComponent`
- [ ] Overlay borroso con CTA de upgrade
- [ ] Integrar en templates con `*hasPermission`

---

### 7. Notificaciones de Expiración

**Falta implementar:**

- [ ] Alerta cuando suscripción está por vencer (7 días antes)
- [ ] Alerta cuando suscripción expira
- [ ] Email automático (backend)

---

### 8. Feature Flags

**Idea:** Para habilitar/deshabilitar features sin redeploy.

**Falta crear:**

- [ ] `FeatureFlagService`
- [ ] Integración con backend o config
- [ ] Usar en combinación con permisos

---

## 🔵 BACKEND PENDIENTE

### 1. Endpoints de Dashboard de Ventas

**Según `BACKEND_ROADMAP_DASHBOARD.md`:**

**Falta implementar:**

- [ ] `GET /api/reports/sales-dashboard` - Dashboard completo
- [ ] DTOs: `SalesDashboardDto`, `SalesTrendItemDto`, `TopProductDto`, `BranchDistributionDto`
- [ ] Queries SQL optimizadas
- [ ] Caching (opcional, TTL 15 minutos)

**Estado:** ⏳ Frontend completado con datos de demostración, esperando backend.

---

### 2. Endpoints de Gestión de Suscripciones

**Según `ADMIN-FEATURES-FINAL-STATUS.md`:**

**Falta implementar:**

- [ ] `GET /api/subscriptions` - Listar todas las suscripciones
- [ ] `GET /api/subscriptions/company/{id}` - Ver detalle de una suscripción
- [ ] `PUT /api/subscriptions/company/{id}/plan` - Cambiar plan
- [ ] `PUT /api/subscriptions/company/{id}/extend` - Extender vigencia
- [ ] `DELETE /api/subscriptions/company/{id}` - Cancelar suscripción
- [ ] `GET /api/subscriptions/plans` - Planes disponibles

**Estado:** ⏳ Frontend completado, esperando backend.

---

### 3. Endpoints de Gestión de Permisos

**Según `ADMIN-FEATURES-FINAL-STATUS.md`:**

**Falta implementar:**

- [ ] `GET /api/permissions/features` - Listar features
- [ ] `GET /api/permissions/roles` - Listar roles
- [ ] `GET /api/permissions/role/{id}` - Permisos de rol
- [ ] `PUT /api/permissions/feature/{code}/roles` - Actualizar roles
- [ ] `POST /api/permissions/bulk-update` - Actualizar múltiples ⭐

**Estado:** ⏳ Frontend completado, esperando backend.

---

### 4. Feature Codes en Base de Datos

**Falta verificar/crear:**

- [ ] Todos los menús tienen `featureCode` único en DB
- [ ] Feature codes para rutas faltantes:
  - `CLIENT_CREATE`
  - `USER_MANAGEMENT`
  - `COMPANY_SETTINGS`
  - `ESTABLISHMENT_MANAGEMENT`
  - `PRODUCT_MANAGEMENT`

**Script SQL sugerido:**

```sql
-- Verificar menús sin featureCode
SELECT id_menu, name, url, feature_code
FROM menu
WHERE feature_code IS NULL OR feature_code = '';

-- Agregar feature codes faltantes
UPDATE menu SET feature_code = 'CLIENT_CREATE' WHERE url = '/clients';
UPDATE menu SET feature_code = 'USER_MANAGEMENT' WHERE url = '/users';
-- etc...
```

---

## 📊 Resumen de Prioridades

### 🔴 CRÍTICO (Hacer ahora)

1. ✅ **Tests unitarios** - Servicios y componentes críticos (>80% cobertura)
2. ✅ **Tests de integración** - Flujos principales
3. ✅ **Proteger rutas faltantes** - Agregar PermissionGuard
4. ✅ **Usar `*hasPermission` en templates** - Ocultar features premium

### 🟡 IMPORTANTE (Próxima semana)

5. Mostrar info de suscripción en UI
6. Crear modal de upgrade
7. Cache de validaciones de features
8. Interceptor para límites de documentos
9. Backend: Endpoints de dashboard de ventas
10. Backend: Endpoints de gestión de suscripciones

### 🟢 MEJORAS (Cuando haya tiempo)

11. Variables CSS compartidas
12. Grid responsive en formularios
13. Tablas responsive
14. Preview de features premium
15. Notificaciones de expiración
16. Feature flags

---

## 🎯 Plan de Acción Sugerido

### Fase 1: Testing (1-2 semanas)

```bash
1. Crear tests unitarios para servicios críticos (3-4 días)
2. Crear tests unitarios para componentes críticos (3-4 días)
3. Crear tests de integración para flujos principales (2-3 días)
4. Configurar CI/CD para ejecutar tests automáticamente (1 día)
```

### Fase 2: Protección y UX (3-5 días)

```bash
1. Agregar PermissionGuard a rutas faltantes (1 día)
2. Usar *hasPermission en templates principales (1 día)
3. Crear componente subscription-badge (1 día)
4. Crear upgrade-modal.component (1 día)
5. Implementar cache de features (1 día)
```

### Fase 3: Optimizaciones (1 semana)

```bash
1. Variables CSS compartidas (1 día)
2. Grid responsive en formularios (2 días)
3. Tablas responsive (2 días)
4. Testing y ajustes (2 días)
```

---

## 📝 Checklist de Completitud

### Testing

- [ ] Tests unitarios >80% cobertura
- [ ] Tests de integración para flujos principales
- [ ] Tests E2E para flujos críticos
- [ ] CI/CD configurado

### Seguridad

- [ ] Todas las rutas protegidas con PermissionGuard
- [ ] Todos los botones/acciones ocultos según permisos
- [ ] Validación de límites de documentos antes de crear
- [ ] Cache de features implementado

### UX/UI

- [ ] Indicador de plan actual visible
- [ ] Barra de progreso de uso de documentos
- [ ] Modal de upgrade funcional
- [ ] Grid responsive en todos los formularios
- [ ] Tablas responsive en todos los componentes

### Backend

- [ ] Endpoints de dashboard implementados
- [ ] Endpoints de suscripciones implementados
- [ ] Endpoints de permisos implementados
- [ ] Feature codes en DB completos

---

## 📚 Documentación de Referencia

- `INTEGRATION-TESTING-GUIDE.md` - Guía de pruebas de integración
- `SUBSCRIPTION-TODO-NEXT-STEPS.md` - Pasos pendientes de suscripciones
- `MEJORAS-PENDIENTES.md` - Mejoras de UI/UX pendientes
- `BACKEND_ROADMAP_DASHBOARD.md` - Especificación del dashboard backend
- `ADMIN-FEATURES-FINAL-STATUS.md` - Estado de features de admin
- `NEXT-STEPS.md` - Próximos pasos generales

---

**Última actualización:** Diciembre 2025  
**Próxima revisión:** Después de completar Fase 1 (Testing)
