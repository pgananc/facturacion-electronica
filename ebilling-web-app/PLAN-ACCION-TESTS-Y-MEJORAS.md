# 🎯 Plan de Acción: Tests y Mejoras Pendientes

**Fecha:** Enero 2026  
**Estado Actual:** ✅ Tests base corregidos | ⏳ Pendiente implementación de tests faltantes

---

## 📊 Estado Actual de Tests

### ✅ Tests Implementados (9 archivos)

1. ✅ `app.component.spec.ts` - Componente raíz
2. ✅ `permission.service.spec.ts` - Servicio de permisos (15+ tests)
3. ✅ `permission.guard.spec.ts` - Guard de permisos (10+ tests)
4. ✅ `has-permission.directive.spec.ts` - Directiva de permisos
5. ✅ `client.service.spec.ts` - CRUD de clientes
6. ✅ `invoice.service.spec.ts` - Gestión de facturas
7. ✅ `add-payment-dialog.component.spec.ts` - Dialog de pagos
8. ✅ `select-product-dialog.component.spec.ts` - Dialog de productos
9. ✅ `select-invoice-point-dialog.component.spec.ts` - Dialog de puntos de emisión

**Cobertura estimada:** ~15-20% del código crítico

---

## 🔴 PRIORIDAD ALTA: Tests Unitarios Faltantes

### 1. Servicios Críticos Sin Tests

#### A) Servicios de Productos y Clientes

- [ ] `product.service.ts` - CRUD de productos
- [ ] `tax-type.service.ts` - Tipos de impuestos
- [ ] `product-cache.service.ts` - Cache de productos
- [ ] `company-client.service.ts` - Clientes por empresa

#### B) Servicios de Usuarios y Empresas

- [ ] `user.service.ts` - CRUD de usuarios
- [ ] `company.service.ts` - CRUD de empresas
- [ ] `company-user.service.ts` - Usuarios por empresa

#### C) Servicios de Facturación

- [ ] `credit-note.service.ts` - Notas de crédito
- [ ] `debit-note.service.ts` - Notas de débito
- [ ] `draft-invoice.service.ts` - Borradores de facturas
- [ ] `payment-method.service.ts` - Métodos de pago

#### D) Servicios de Menú y Autenticación

- [ ] `menu.service.ts` - Carga de menús
- [ ] `menu-filter.service.ts` - Filtrado de menús por permisos
- [ ] `login.service.ts` - Autenticación
- [ ] `token.service.ts` - Gestión de tokens

#### E) Servicios de Admin

- [ ] `permission-admin.service.ts` - Gestión de permisos (admin)
- [ ] `subscription-admin.service.ts` - Gestión de suscripciones (admin)
- [ ] `menu-admin.service.ts` - Gestión de menús (admin)

#### F) Otros Servicios

- [ ] `dashboard.service.ts` - Dashboard de ventas
- [ ] `report.service.ts` - Generación de reportes
- [ ] `role.service.ts` - Gestión de roles
- [ ] `establishment.service.ts` - Establecimientos
- [ ] `issuance-point.service.ts` - Puntos de emisión
- [ ] `digital-signature.service.ts` - Firmas digitales

**Total:** ~25 servicios sin tests

---

### 2. Guards Sin Tests

- [ ] `session-validation.guard.ts` - Validación de sesión
- [ ] `can-active-user.guard.ts` - Validación de usuario activo
- [ ] `client-token.guard.ts` - Validación de token de cliente
- [ ] `session-validation.service.ts` - Servicio de validación de sesión

**Total:** 4 guards/servicios sin tests

---

### 3. Componentes Críticos Sin Tests

#### A) Componentes de Facturación

- [ ] `billing.component.ts` - Creación de facturas
- [ ] `create-credit-note.component.ts` - Creación de NC
- [ ] `create-debit-note.component.ts` - Creación de ND
- [ ] `draft-invoices.component.ts` - Lista de borradores
- [ ] `viewing-invoices.component.ts` - Lista de facturas

#### B) Componentes de Admin

- [ ] `subscription-list.component.ts` - Lista de suscripciones
- [ ] `permission-matrix.component.ts` - Matriz de permisos
- [ ] `menu-list.component.ts` - Lista de menús

#### C) Componentes de Reportes

- [ ] `sales-dashboard.component.ts` - Dashboard de ventas

#### D) Componentes de CRUD

- [ ] `client.component.ts` - CRUD de clientes
- [ ] `product.component.ts` - CRUD de productos
- [ ] `user.component.ts` - CRUD de usuarios
- [ ] `company.component.ts` - CRUD de empresas

**Total:** ~15 componentes críticos sin tests

---

## 🟡 PRIORIDAD MEDIA: Features Pendientes

### 1. Protección de Rutas Faltantes

Rutas que necesitan `PermissionGuard`:

- [x] `/clients` → `CLIENT_CREATE` ✅
- [x] `/users` → `USER_MANAGEMENT` ✅
- [x] `/companies` → `COMPANY_SETTINGS` ✅
- [x] `/establishments` → `ESTABLISHMENT_MANAGEMENT` ✅
- [x] `/invoice-points` → `ESTABLISHMENT_MANAGEMENT` ✅
- [x] `/products` → `PRODUCT_MANAGEMENT` ✅ **IMPLEMENTADO**

**Archivos a modificar:** `app-routing.module.ts` o módulos de rutas específicos

---

### 2. Directiva `*hasPermission` en Templates

**Arquitectura de Permisos:**

- ✅ **Nivel de Ruta:** Si puedes acceder a una ruta (con `PermissionGuard`), puedes realizar todas las acciones dentro de esa página
- ✅ **Excepción:** Solo `welcome.component.html` necesita la directiva porque muestra múltiples features en una sola página
- ✅ **Validaciones granulares:** Se implementarán en el futuro si es necesario

**Estado Actual:**

- [x] `welcome.component.html` - ✅ **YA IMPLEMENTADO** - Todas las cards usan `*appHasPermission`
- [x] `sales-dashboard.component.html` - ✅ **NO NECESITA** - La ruta ya está protegida con `PermissionGuard`
- [x] `billing.component.html` - ✅ **NO NECESITA** - La ruta ya está protegida con `PermissionGuard`
- [x] Menú lateral - ✅ **YA IMPLEMENTADO** - Usa `MenuFilterService` para filtrar menús por permisos

**Ejemplo de uso en welcome.component.html:**

```html
<mat-card *appHasPermission="'REPORT_ADVANCED'" class="action-card" routerLink="/reports/sales-dashboard">
  <h3>Dashboard</h3>
  <p>Análisis completo</p>
</mat-card>
```

---

### 3. Información de Suscripción en UI

- [x] **Badge de plan actual** en navbar/sidebar ✅ **IMPLEMENTADO**
- [x] **Barra de progreso** de documentos usados (ej: "Facturas: 150/200") ✅ **IMPLEMENTADO**
- [x] **Alertas** cuando >80% usado ✅ **IMPLEMENTADO**
- [x] **Bloqueo** cuando alcanza 100% de uso ✅ **IMPLEMENTADO**

**Componente creado:** `subscription-badge.component.ts`

**Características implementadas:**

- Badge con color según plan (FREE=gris, BASIC=azul, PRO=naranja, ENT=púrpura)
- Barra de progreso visual con colores dinámicos (verde/naranja/rojo)
- Alertas automáticas cuando alcanza 80% y 100% de uso
- Integrado en navbar (toolbar) y sidebar
- Refresco automático cada minuto
- Responsive: se oculta en toolbar en móviles pequeños

---

### 4. Modal de Upgrade

- [ ] Crear `upgrade-modal.component.ts`
- [ ] Comparación de planes (actual vs recomendado)
- [ ] Botón de upgrade con link externo o integración con backend
- [ ] Integrar en `PermissionGuard` cuando acceso denegado

---

## 🟢 PRIORIDAD BAJA: Optimizaciones

### 1. Cache de Validaciones de Features

- [ ] Cache en `PermissionService` con TTL de 5 minutos
- [ ] Invalidar cache cuando cambian permisos
- [ ] Cache en localStorage como fallback

---

### 2. Variables CSS Compartidas

- [ ] Crear `_shared/variables.css` con:
  - Espaciado (`--spacing-xs` a `--spacing-2xl`)
  - Sombras (`--shadow-sm` a `--shadow-xl`)
  - Border radius (`--radius-sm` a `--radius-full`)
- [ ] Migrar componentes a usar variables

---

### 3. Grid Responsive en Formularios

- [ ] `company.component` - Grid responsive
- [ ] `product.component` - Grid responsive
- [ ] `user.component` - Grid responsive
- [ ] Otros componentes de búsqueda

---

### 4. Tablas Responsive

- [ ] Agregar scroll horizontal en todas las tablas
- [ ] Ocultar columnas menos importantes en mobile
- [ ] Aplicar a todos los componentes de lista

---

## 📅 Plan de Ejecución Sugerido

### Fase 1: Tests de Servicios (Semana 1-2)

**Día 1-2:** Servicios de Productos y Clientes

- `product.service.spec.ts`
- `tax-type.service.spec.ts`
- `company-client.service.spec.ts`

**Día 3-4:** Servicios de Usuarios y Empresas

- `user.service.spec.ts`
- `company.service.spec.ts`
- `company-user.service.spec.ts`

**Día 5-7:** Servicios de Facturación

- `credit-note.service.spec.ts`
- `debit-note.service.spec.ts`
- `draft-invoice.service.spec.ts`
- `payment-method.service.spec.ts`

**Día 8-10:** Servicios de Menú y Autenticación

- `menu.service.spec.ts`
- `menu-filter.service.spec.ts`
- `login.service.spec.ts`
- `token.service.spec.ts`

**Día 11-14:** Servicios de Admin y Otros

- `permission-admin.service.ts`
- `subscription-admin.service.ts`
- `dashboard.service.ts`
- `report.service.ts`

---

### Fase 2: Tests de Guards y Componentes (Semana 3-4)

**Día 15-16:** Guards

- `session-validation.guard.spec.ts`
- `can-active-user.guard.spec.ts`
- `client-token.guard.spec.ts`

**Día 17-21:** Componentes Críticos

- `billing.component.spec.ts`
- `create-credit-note.component.spec.ts`
- `create-debit-note.component.spec.ts`
- `subscription-list.component.spec.ts`
- `permission-matrix.component.spec.ts`

---

### Fase 3: Features Pendientes (Semana 5)

**Día 22-23:** Protección de Rutas

- Agregar `PermissionGuard` a rutas faltantes
- Verificar feature codes en backend

**Día 24-25:** Directiva en Templates

- Implementar `*hasPermission` en componentes principales
- Crear templates de upgrade

**Día 26-28:** UI de Suscripción

- Crear `subscription-badge.component.ts`
- Implementar barra de progreso
- Crear `upgrade-modal.component.ts`

---

### Fase 4: Optimizaciones (Semana 6)

**Día 29-30:** Cache y Variables CSS

- Implementar cache en `PermissionService`
- Crear variables CSS compartidas

**Día 31-35:** Responsive Design

- Grid responsive en formularios
- Tablas responsive

---

## 📊 Métricas de Éxito

### Testing

- ✅ **Meta:** >80% cobertura en servicios y componentes críticos
- ✅ **Actual:** ~15-20%
- ⏳ **Pendiente:** ~60-65% más

### Features

- ✅ **Meta:** Todas las rutas protegidas
- ✅ **Actual:** ~70% protegidas
- ⏳ **Pendiente:** 6 rutas más

### UX

- ✅ **Meta:** Información de suscripción visible
- ✅ **Actual:** 0%
- ⏳ **Pendiente:** 100%

---

## 🚀 Comenzar Ahora

### Opción 1: Empezar con Tests de Servicios

```bash
# Crear test para ProductService
ng generate service product --skip-tests=false
# O crear manualmente: src/app/_service/product/product.service.spec.ts
```

### Opción 2: Empezar con Protección de Rutas

```typescript
// En app-routing.module.ts o módulo específico
{
  path: 'clients',
  component: ClientComponent,
  canActivate: [PermissionGuard],
  data: { featureCode: 'CLIENT_CREATE' }
}
```

### Opción 3: Empezar con UI de Suscripción

```bash
# Crear componente de badge
ng generate component subscription-badge
```

---

## 📝 Checklist de Progreso

### Tests Unitarios

- [x] Tests base corregidos (9 archivos)
- [ ] Servicios de productos (3 servicios)
- [ ] Servicios de usuarios/empresas (3 servicios)
- [ ] Servicios de facturación (4 servicios)
- [ ] Servicios de menú/auth (4 servicios)
- [ ] Servicios de admin (3 servicios)
- [ ] Otros servicios (6 servicios)
- [ ] Guards (4 guards)
- [ ] Componentes críticos (15 componentes)

### Features

- [ ] Protección de rutas faltantes (6 rutas)
- [ ] Directiva en templates (4 componentes)
- [ ] Badge de suscripción
- [ ] Modal de upgrade

### Optimizaciones

- [ ] Cache de features
- [ ] Variables CSS
- [ ] Grid responsive
- [ ] Tablas responsive

---

**Última actualización:** Enero 2026  
**Próxima revisión:** Después de completar Fase 1 (Tests de Servicios)
