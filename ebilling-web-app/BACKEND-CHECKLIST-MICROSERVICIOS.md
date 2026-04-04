# 🎯 CHECKLIST: Backend por Microservicio

**Fecha:** 13 de Diciembre, 2025  
**Decisión:** 3 roles (super_admin, member, admin) + Pagos manuales

---

## 📦 **ebill-authentication (Puerto 8280)**

### **Responsabilidad:**
- Gestión de roles y permisos
- Validación de ROL (Capa 1)
- Coordinador de validación completa (ROL + PLAN)

---

### **✅ TAREAS:**

#### **1. Base de Datos:**

```sql
-- [ ] Crear tabla feature_permission
-- [ ] Insertar 15 features base
-- [ ] Crear índices
-- [ ] Verificar que roles 1, 2, 3 existen
```

**Script SQL completo:** Ver `FASE1-IMPLEMENTACION-COMPLETA.md` líneas 48-193

---

#### **2. Endpoints:**

**A. Validar ROL solamente**
```
[ ] POST /api/permission/check-role
    Request: { featureCode, idUser }
    Response: { allowed: boolean, message: string }
```

**B. Validar ROL + PLAN (coordinador)**
```
[ ] POST /api/permission/validate-feature
    Request: { featureCode, idCompany }
    Response: { allowed: boolean, reason?: string, message: string }
    
    Lógica:
    1. Verificar si es super_admin → bypass
    2. Validar ROL en feature_permission (local)
    3. Llamar a ebill-billing GET /api/subscription/{id}/current
    4. Verificar si plan incluye la feature
    5. Responder: permitir o denegar con motivo
```

**Dependencia externa:**
- Necesita `RestTemplate` o `WebClient` configurado para llamar a `ebill-billing:8480`

---

#### **3. Nuevos DTOs/Entities:**

```java
[ ] FeaturePermission.java (entity)
    - idPermission
    - featureCode
    - featureName
    - description
    - module
    - allowedRoles (Integer[])
    - requiredPlans (String[])
    - isActive
    
[ ] FeatureValidationRequest.java (DTO)
    - featureCode
    - idCompany
    
[ ] FeatureValidationResponse.java (DTO)
    - allowed
    - reason (NO_ROLE, NO_SUBSCRIPTION, LIMIT_EXCEEDED, EXPIRED)
    - message
    - upgradeUrl
```

---

#### **4. Configuración:**

```java
[ ] Configurar RestTemplate o WebClient en @Configuration
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    
[ ] Agregar URL de ebill-billing en application.properties
    
    ebill.billing.url=http://localhost:8480
```

---

## 📦 **ebill-billing (Puerto 8480)**

### **Responsabilidad:**
- Gestión de planes de suscripción
- Gestión de suscripciones de compañías
- Validación de PLAN (Capa 2)
- Tracking de uso de documentos

---

### **✅ TAREAS:**

#### **1. Base de Datos:**

```sql
-- [ ] Crear tabla subscription_plan
-- [ ] Crear tabla company_subscription
-- [ ] Crear tabla subscription_usage_history
-- [ ] Insertar 5 planes (FREE, BASIC, PRO, BUSINESS, ENTERPRISE)
-- [ ] Migrar companies existentes a plan FREE
-- [ ] Crear función increment_document_usage()
-- [ ] Crear triggers en invoice, credit_note
-- [ ] Crear índices
```

**Scripts SQL completos:** Ver `FASE1-IMPLEMENTACION-COMPLETA.md` líneas 195-520

---

#### **2. Endpoints:**

**A. Obtener suscripción actual**
```
[ ] GET /api/subscription/{idCompany}/current
    Response: {
      idSubscription,
      plan: { code, name, priceAnnual, documentLimitYearly, ... },
      status,
      startDate,
      expiresAt,
      currentYearUsage,
      percentUsed,
      documentsRemaining
    }
```

**B. Obtener uso mensual/anual**
```
[ ] GET /api/subscription/{idCompany}/usage
    Response: {
      currentMonth: { month, invoices, creditNotes, total },
      currentYear: { year, total, limit, percentUsed, remaining },
      lastMonths: [...]
    }
```

**C. Verificar si puede emitir documento**
```
[ ] POST /api/subscription/{idCompany}/can-emit
    Request: { documentType: 'INVOICE' }
    Response: { 
      allowed: boolean, 
      currentUsage, 
      limit, 
      remaining, 
      message 
    }
    
    Lógica:
    1. Super admin → siempre true
    2. Obtener suscripción activa
    3. Verificar si está expirada
    4. Verificar si límite alcanzado
    5. Responder true/false con detalles
```

**D. Upgrade manual de plan**
```
[ ] POST /api/subscription/{idCompany}/upgrade
    Request: {
      newPlanCode,
      paymentReference,
      paymentAmount,
      paymentDate,
      notes
    }
    Response: {
      success: boolean,
      message,
      subscription: {...}
    }
    
    Lógica:
    1. Verificar que user es admin o super_admin
    2. Obtener suscripción actual
    3. Validar que no sea downgrade (opcional)
    4. Actualizar a nuevo plan
    5. Actualizar fechas (expires_at = +1 año)
    6. Guardar datos de pago manual
```

---

#### **3. Nuevos DTOs/Entities:**

```java
[ ] SubscriptionPlan.java (entity)
    - idPlan
    - code
    - name
    - description
    - priceAnnual
    - priceMonthly
    - documentLimitYearly
    - userLimit
    - establishmentLimit
    - issuancePointLimit
    - has* (11 features boolean)
    - isActive
    - displayOrder
    
[ ] CompanySubscription.java (entity)
    - idSubscription
    - idCompany
    - idPlan
    - startDate
    - expiresAt
    - status
    - currentYearUsage
    - lastResetDate
    - paymentMethod
    - lastPaymentDate
    - lastPaymentAmount
    - paymentReference
    - notes
    
[ ] SubscriptionUsageHistory.java (entity)
    - idHistory
    - idCompany
    - idSubscription
    - monthYear
    - invoicesCount
    - creditNotesCount
    - debitNotesCount
    - retentionsCount
    - guidesCount
    - totalDocuments
    
[ ] SubscriptionResponse.java (DTO)
    - Incluye plan, status, dates, usage, percentUsed, documentsRemaining
    
[ ] UpgradeRequest.java (DTO)
    - newPlanCode
    - paymentReference
    - paymentAmount
    - paymentDate
    - notes
```

---

#### **4. Trigger y Función:**

```sql
[ ] Implementar función increment_document_usage() en PL/pgSQL
    
[ ] Crear triggers:
    - AFTER INSERT ON invoice (solo si authorization_sri IS NOT NULL)
    - AFTER INSERT ON credit_note (solo si authorization_sri IS NOT NULL)
    - AFTER INSERT ON debit_note (si existe)
    - AFTER INSERT ON retention (si existe)
    - AFTER INSERT ON guide (si existe)
```

**Lógica del trigger:**
1. Detectar tipo de documento
2. Obtener `id_company`
3. Obtener `id_subscription` activa
4. Insert/Update en `subscription_usage_history` (mes actual)
5. Actualizar `current_year_usage` en `company_subscription`

---

#### **5. Validaciones de Negocio:**

```java
[ ] Al emitir documento:
    - Verificar que la suscripción no esté expirada
    - Verificar que no se haya alcanzado el límite
    - Si límite alcanzado → lanzar exception + HTTP 403
    
[ ] Al crear usuario:
    - Verificar que no se exceda userLimit del plan
    - Si límite alcanzado → lanzar exception
    
[ ] Al crear establecimiento:
    - Verificar que no se exceda establishmentLimit del plan
    - Si límite alcanzado → lanzar exception
    
[ ] Al crear punto de emisión:
    - Verificar que no se exceda issuancePointLimit del plan
    - Si límite alcanzado → lanzar exception
```

---

## 🎨 **FRONTEND (Angular)**

### **✅ TAREAS:**

#### **1. Services:**

```typescript
[ ] PermissionService (src/app/_service/permission/permission.service.ts)
    - getSubscription(): Observable<CompanySubscription>
    - canAccess(featureCode): Observable<boolean>
    - canAccessWithDetails(featureCode): Observable<FeatureValidationResponse>
    - canEmitDocument(type): Observable<boolean>
    - isSuperAdmin(): boolean
    - hasRole(roleName): boolean
    - getCurrentPlanCode(): string
    - planHasFeature(feature): boolean
```

**Código completo:** Ver `FASE1-IMPLEMENTACION-COMPLETA.md` líneas 1050-1240

---

#### **2. Directiva:**

```typescript
[ ] HasPermissionDirective (src/app/_directives/has-permission.directive.ts)
    - @Input() hasPermission: string (feature code)
    - @Input() hasPermissionElse?: TemplateRef
    - Muestra/oculta elementos según permisos
```

**Uso:**
```html
<div *hasPermission="'dashboard.view'">
  <!-- Solo visible si tiene permiso -->
</div>
```

**Código completo:** Ver `FASE1-IMPLEMENTACION-COMPLETA.md` líneas 1242-1310

---

#### **3. Guard:**

```typescript
[ ] PermissionGuard (src/app/_guard/permission.guard.ts)
    - canActivate(route): Observable<boolean | UrlTree>
    - Lee route.data['permission']
    - Valida y redirige si no tiene acceso
```

**Uso en routing:**
```typescript
{
  path: 'reports/advanced',
  canActivate: [SessionValidationGuard, PermissionGuard],
  data: { permission: 'reports.advanced' },
  component: SalesDashboardComponent
}
```

**Código completo:** Ver `FASE1-IMPLEMENTACION-COMPLETA.md` líneas 1312-1380

---

#### **4. Módulos:**

```typescript
[ ] Agregar HasPermissionDirective a SharedModule

[ ] Actualizar app-routing.module.ts:
    - Importar PermissionGuard
    - Agregar data: { permission: '...' } a rutas protegidas
```

**Rutas a proteger:**
- `/welcome` → `dashboard.view` (solo BASIC+)
- `/reports/sales-dashboard` → `reports.advanced` (solo PRO+)
- `/reports/branch-comparison` → `reports.advanced` (solo PRO+)
- `/draft-invoices` → `billing.draft.manage` (solo BASIC+)

---

#### **5. Interfaces TypeScript:**

```typescript
[ ] Crear interfaces en src/app/_model/subscription/:
    - subscription-plan.ts
    - company-subscription.ts
    - subscription-usage.ts
    - feature-validation-response.ts
```

---

## 📊 **RESUMEN DE CHECKLIST COMPLETO:**

### **Backend Total: 20 tareas**
- ebill-authentication: 8 tareas (1 tabla + 2 endpoints + DTOs + config)
- ebill-billing: 12 tareas (3 tablas + 4 endpoints + DTOs + triggers + validaciones)

### **Frontend Total: 10 tareas**
- Services: 1 (PermissionService)
- Directivas: 1 (HasPermissionDirective)
- Guards: 1 (PermissionGuard)
- Interfaces: 4
- Módulos: 2 (SharedModule + Routing)
- Rutas: 4 protegidas

---

## 🚀 **SIGUIENTE PASO:**

**¿Comenzamos con el frontend?**

Puedo implementar AHORA:
1. ✅ PermissionService (completo)
2. ✅ HasPermissionDirective (completo)
3. ✅ PermissionGuard (completo)
4. ✅ Interfaces TypeScript
5. ✅ Integración en SharedModule

**Tiempo estimado:** 1 hora (mientras backend se desarrolla)

**Los servicios funcionarán con:**
- ✅ Manejo de errores si backend no está listo
- ✅ Cache local (localStorage) como fallback
- ✅ Super admin bypass desde el frontend

**¿Arranco con el frontend?** 🚀
