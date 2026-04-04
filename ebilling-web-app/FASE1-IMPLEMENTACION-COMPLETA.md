# 🚀 FASE 1: Implementación Suscripciones - Especificación Completa

**Fecha:** 13 de Diciembre, 2025  
**Sprint:** Fase 1 - Base de Datos + Backend  
**Duración estimada:** 1-2 semanas

---

## ⚡ **RESUMEN EJECUTIVO**

### **Microservicios Oficiales:**

```
✅ ebill-authentication (8280) → Roles + Permisos + Coordinador
✅ ebill-billing (8480)        → Suscripciones + Planes + Uso
✅ ebill-report (8580)         → Reportes (sin cambios)
✅ ebill-product (8380)        → Productos (sin cambios)
✅ ebill-customer (8180)       → Clientes (sin cambios)
```

### **Responsabilidades:**

| Microservicio            | Tablas (nuevas)                                                               | Endpoints (nuevos) | Rol                     |
| ------------------------ | ----------------------------------------------------------------------------- | ------------------ | ----------------------- |
| **ebill-authentication** | `feature_permission`                                                          | 2                  | Validar ROL + Coordinar |
| **ebill-billing**        | `subscription_plan`<br>`company_subscription`<br>`subscription_usage_history` | 4                  | Validar PLAN + Tracking |

---

## 📋 **DECISIONES CONFIRMADAS**

✅ **Roles:** 3 roles base (super_admin, member, admin)  
✅ **Supervisor:** Bajo demanda (futuro)  
✅ **Pagos:** Manuales (sin Stripe/PayPhone por ahora)  
✅ **Pricing:** Opción A ($0, $39, $79, $149, $299)

---

## 🏗️ **ARQUITECTURA DE MICROSERVICIOS (CORREGIDA)**

### **Microservicios Oficiales:**

```
┌─────────────────────────────────────────────────────────────┐
│                    ebill-authentication                      │
│  Puerto: 8280                                               │
│  Responsabilidad: Roles, Permisos, Validación COMPLETA     │
├─────────────────────────────────────────────────────────────┤
│  - Tabla: role (YA EXISTE)                                  │
│  - Tabla: feature_permission (NUEVA)                        │
│  - Endpoint: POST /api/permission/check-role                │
│  - Endpoint: POST /api/permission/validate-feature (NUEVO)  │
│  - Lógica: Validación ROL (Capa 1)                         │
│  - Lógica: Coordinación ROL + PLAN (llama a billing)       │
└─────────────────────────────────────────────────────────────┘
                            ↓ (llama)
┌─────────────────────────────────────────────────────────────┐
│                      ebill-billing                          │
│  Puerto: 8480                                               │
│  Responsabilidad: Suscripciones, Planes, Uso               │
├─────────────────────────────────────────────────────────────┤
│  - Tabla: subscription_plan (NUEVA)                         │
│  - Tabla: company_subscription (NUEVA)                      │
│  - Tabla: subscription_usage_history (NUEVA)                │
│  - Endpoint: GET /api/subscription/{id}/current             │
│  - Endpoint: GET /api/subscription/{id}/usage               │
│  - Endpoint: POST /api/subscription/{id}/can-emit           │
│  - Endpoint: POST /api/subscription/{id}/upgrade (manual)   │
│  - Lógica: Validación PLAN (Capa 2)                        │
│  - Trigger: Incrementar uso al emitir doc                   │
└─────────────────────────────────────────────────────────────┘
```

### **Otros Microservicios (sin cambios):**

- **ebill-report** (8580): Reportes
- **ebill-product** (8380): Productos
- **ebill-customer** (8180): Clientes

---

## 🗄️ **BASE DE DATOS - SQL SCRIPTS**

### **1. ebill-authentication: Tabla `feature_permission`**

```sql
-- ===================================================================
-- MICROSERVICIO: ebill-authentication
-- TABLA: feature_permission
-- DESCRIPCIÓN: Mapeo de features a roles permitidos y planes requeridos
-- ===================================================================

CREATE TABLE IF NOT EXISTS public.feature_permission (
  id_permission SERIAL PRIMARY KEY,
  feature_code VARCHAR(100) NOT NULL UNIQUE,
  feature_name VARCHAR(255) NOT NULL,
  description TEXT,
  module VARCHAR(50), -- 'BILLING', 'REPORTS', 'ADMIN', 'DASHBOARD', 'API', 'INTEGRATION'

  -- Roles que tienen acceso (array de IDs)
  -- Roles existentes: 1=super_admin, 2=member, 3=admin
  allowed_roles INT[] DEFAULT '{}',

  -- Planes que incluyen esta feature (array de códigos)
  -- Planes: 'FREE', 'BASIC', 'PRO', 'BUSINESS', 'ENTERPRISE'
  required_plans VARCHAR(50)[] DEFAULT '{}',

  is_active BOOLEAN DEFAULT true,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);

-- Índices para mejorar performance
CREATE INDEX idx_feature_permission_code ON public.feature_permission(feature_code);
CREATE INDEX idx_feature_permission_module ON public.feature_permission(module);
CREATE INDEX idx_feature_permission_active ON public.feature_permission(is_active);

-- ===================================================================
-- INSERTS: Features principales (15 features base)
-- ===================================================================

INSERT INTO public.feature_permission
(feature_code, feature_name, description, module, allowed_roles, required_plans)
VALUES

-- ==================== DASHBOARD ====================
('dashboard.view',
 'Ver Dashboard',
 'Acceso al Welcome Dashboard con KPIs y gráficos',
 'DASHBOARD',
 '{3}', -- solo admin
 '{''BASIC'',''PRO'',''BUSINESS'',''ENTERPRISE''}'),

-- ==================== REPORTES ====================
('reports.basic',
 'Reportes Básicos',
 'Ver reportes básicos en PDF y Excel',
 'REPORTS',
 '{2,3}', -- member y admin
 '{''FREE'',''BASIC'',''PRO'',''BUSINESS'',''ENTERPRISE''}'),

('reports.advanced',
 'Reportes Avanzados',
 'Dashboard de ventas, comparación de sucursales',
 'REPORTS',
 '{3}', -- solo admin
 '{''PRO'',''BUSINESS'',''ENTERPRISE''}'),

('reports.scheduled',
 'Reportes Programados',
 'Envío automático de reportes por email',
 'REPORTS',
 '{3}', -- solo admin
 '{''ENTERPRISE''}'),

-- ==================== FACTURACIÓN ====================
('billing.invoice.create',
 'Emitir Facturas',
 'Crear y autorizar facturas electrónicas',
 'BILLING',
 '{2,3}', -- member y admin
 '{''FREE'',''BASIC'',''PRO'',''BUSINESS'',''ENTERPRISE''}'),

('billing.credit_note.create',
 'Emitir Notas de Crédito',
 'Crear y autorizar notas de crédito',
 'BILLING',
 '{2,3}', -- member y admin
 '{''FREE'',''BASIC'',''PRO'',''BUSINESS'',''ENTERPRISE''}'),

('billing.draft.manage',
 'Gestión de Borradores',
 'Guardar, editar y procesar borradores',
 'BILLING',
 '{2,3}', -- member y admin
 '{''BASIC'',''PRO'',''BUSINESS'',''ENTERPRISE''}'),

('billing.guide.create',
 'Emitir Guías de Remisión',
 'Crear guías de remisión electrónicas',
 'BILLING',
 '{2,3}', -- member y admin
 '{''PRO'',''BUSINESS'',''ENTERPRISE''}'),

-- ==================== ADMINISTRACIÓN ====================
('admin.company.manage',
 'Gestión de Compañía',
 'Ver y editar datos de la empresa',
 'ADMIN',
 '{3}', -- solo admin
 '{''FREE'',''BASIC'',''PRO'',''BUSINESS'',''ENTERPRISE''}'),

('admin.users.manage',
 'Gestión de Usuarios',
 'Crear, editar y eliminar usuarios',
 'ADMIN',
 '{3}', -- solo admin
 '{''FREE'',''BASIC'',''PRO'',''BUSINESS'',''ENTERPRISE''}'),

('admin.establishments.manage',
 'Gestión de Sucursales',
 'Crear, editar y eliminar sucursales',
 'ADMIN',
 '{3}', -- solo admin
 '{''FREE'',''BASIC'',''PRO'',''BUSINESS'',''ENTERPRISE''}'),

('admin.products.manage',
 'Gestión de Productos',
 'Crear, editar y eliminar productos',
 'ADMIN',
 '{3}', -- solo admin
 '{''FREE'',''BASIC'',''PRO'',''BUSINESS'',''ENTERPRISE''}'),

('admin.csv_import',
 'Importación CSV',
 'Importar clientes y productos desde CSV',
 'ADMIN',
 '{3}', -- solo admin
 '{''PRO'',''BUSINESS'',''ENTERPRISE''}'),

-- ==================== API ====================
('api.rest',
 'API REST',
 'Acceso a endpoints de lectura y escritura',
 'API',
 '{3}', -- solo admin
 '{''BUSINESS'',''ENTERPRISE''}'),

-- ==================== INTEGRACIONES ====================
('integration.erp',
 'Integración ERP',
 'Conectar con sistemas contables externos',
 'INTEGRATION',
 '{3}', -- solo admin
 '{''ENTERPRISE''}');

-- ===================================================================
-- VERIFICACIÓN
-- ===================================================================
SELECT
  feature_code,
  feature_name,
  module,
  allowed_roles,
  required_plans
FROM public.feature_permission
ORDER BY module, feature_code;
```

---

### **2. ebill-billing: Tabla `subscription_plan`**

```sql
-- ===================================================================
-- MICROSERVICIO: ebill-billing
-- TABLA: subscription_plan
-- DESCRIPCIÓN: Planes de suscripción disponibles
-- ===================================================================

CREATE TABLE IF NOT EXISTS public.subscription_plan (
  id_plan SERIAL PRIMARY KEY,
  code VARCHAR(50) NOT NULL UNIQUE,
  name VARCHAR(100) NOT NULL,
  description TEXT,

  -- Precios
  price_annual DECIMAL(10,2) NOT NULL DEFAULT 0,
  price_monthly DECIMAL(10,2), -- Para futuro (si ofrecemos mensual)

  -- Límites de uso
  document_limit_yearly INT, -- NULL = ilimitado
  user_limit INT, -- NULL = ilimitado
  establishment_limit INT, -- NULL = ilimitado
  issuance_point_limit INT, -- NULL = ilimitado

  -- Features booleanas (para validación rápida)
  has_dashboard BOOLEAN DEFAULT false,
  has_advanced_reports BOOLEAN DEFAULT false,
  has_drafts BOOLEAN DEFAULT false,
  has_api_access BOOLEAN DEFAULT false,
  has_webhooks BOOLEAN DEFAULT false,
  has_erp_integration BOOLEAN DEFAULT false,
  has_inventory BOOLEAN DEFAULT false,
  has_scheduled_reports BOOLEAN DEFAULT false,
  has_csv_import BOOLEAN DEFAULT false,
  has_guides BOOLEAN DEFAULT false,

  -- Estado y orden de visualización
  is_active BOOLEAN DEFAULT true,
  display_order INT DEFAULT 0,

  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);

-- Índices
CREATE INDEX idx_subscription_plan_code ON public.subscription_plan(code);
CREATE INDEX idx_subscription_plan_active ON public.subscription_plan(is_active);

-- ===================================================================
-- INSERTS: 5 Planes (Pricing Opción A)
-- ===================================================================

INSERT INTO public.subscription_plan
(code, name, description, price_annual, price_monthly,
 document_limit_yearly, user_limit, establishment_limit, issuance_point_limit,
 has_dashboard, has_advanced_reports, has_drafts, has_api_access, has_webhooks,
 has_erp_integration, has_inventory, has_scheduled_reports, has_csv_import, has_guides,
 display_order)
VALUES

-- PLAN 1: GRATIS
('FREE',
 'Gratis',
 'Plan gratuito para pruebas y microemprendedores',
 0, 0,
 50, 1, 1, 1,
 false, false, false, false, false, false, false, false, false, false,
 0),

-- PLAN 2: BÁSICO
('BASIC',
 'Básico',
 'Para emprendedores y pequeños negocios',
 39, 4,
 500, 2, 1, 1,
 true, false, true, false, false, false, false, false, false, false,
 1),

-- PLAN 3: PROFESIONAL
('PRO',
 'Profesional',
 'Para PYMES en crecimiento con múltiples sucursales',
 79, 8,
 1500, 5, 2, 2,
 true, true, true, false, false, false, false, false, true, true,
 2),

-- PLAN 4: EMPRESARIAL
('BUSINESS',
 'Empresarial',
 'Para empresas medianas con necesidades avanzadas',
 149, 15,
 5000, 10, 5, 5,
 true, true, true, true, true, false, true, false, true, true,
 3),

-- PLAN 5: ENTERPRISE
('ENTERPRISE',
 'Enterprise',
 'Para corporativos y cadenas con capacidad ilimitada',
 299, 30,
 NULL, NULL, NULL, NULL, -- NULL = ilimitado
 true, true, true, true, true, true, true, true, true, true,
 4);

-- ===================================================================
-- VERIFICACIÓN
-- ===================================================================
SELECT
  code,
  name,
  price_annual,
  document_limit_yearly,
  user_limit,
  has_dashboard,
  has_advanced_reports
FROM public.subscription_plan
ORDER BY display_order;
```

---

### **3. ebill-billing: Tabla `company_subscription`**

```sql
-- ===================================================================
-- MICROSERVICIO: ebill-billing
-- TABLA: company_subscription
-- DESCRIPCIÓN: Suscripción activa de cada compañía
-- ===================================================================

CREATE TABLE IF NOT EXISTS public.company_subscription (
  id_subscription SERIAL PRIMARY KEY,
  id_company INT NOT NULL REFERENCES company(id_company) ON DELETE CASCADE,
  id_plan INT NOT NULL REFERENCES subscription_plan(id_plan),

  -- Fechas
  start_date DATE NOT NULL DEFAULT CURRENT_DATE,
  expires_at DATE NOT NULL,

  -- Estado
  status VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE, EXPIRED, CANCELLED, SUSPENDED, TRIAL

  -- Uso actual (contadores)
  current_year_usage INT DEFAULT 0, -- Total de docs emitidos en el año actual
  last_reset_date DATE DEFAULT CURRENT_DATE, -- Fecha del último reset (inicio de año)

  -- Facturación manual
  payment_method VARCHAR(50) DEFAULT 'MANUAL', -- MANUAL, STRIPE, PAYPHONE (futuro)
  last_payment_date DATE,
  last_payment_amount DECIMAL(10,2),
  payment_reference VARCHAR(255), -- Número de factura, recibo, etc.

  -- Notas internas
  notes TEXT,

  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW(),

  -- Constraint: Una compañía solo puede tener UNA suscripción activa
  UNIQUE(id_company)
);

-- Índices
CREATE INDEX idx_company_subscription_company ON public.company_subscription(id_company);
CREATE INDEX idx_company_subscription_plan ON public.company_subscription(id_plan);
CREATE INDEX idx_company_subscription_status ON public.company_subscription(status);
CREATE INDEX idx_company_subscription_expires ON public.company_subscription(expires_at);

-- ===================================================================
-- MIGRACIÓN: Asignar plan FREE a todas las compañías existentes
-- ===================================================================

-- Paso 1: Obtener ID del plan FREE
DO $$
DECLARE
  free_plan_id INT;
BEGIN
  SELECT id_plan INTO free_plan_id FROM public.subscription_plan WHERE code = 'FREE';

  -- Paso 2: Insertar suscripción FREE para cada compañía que NO tenga suscripción
  INSERT INTO public.company_subscription
    (id_company, id_plan, start_date, expires_at, status, payment_method, notes)
  SELECT
    c.id_company,
    free_plan_id,
    CURRENT_DATE,
    CURRENT_DATE + INTERVAL '10 years', -- Plan FREE no expira
    'ACTIVE',
    'MANUAL',
    'Migración automática - Plan FREE por defecto'
  FROM public.company c
  WHERE NOT EXISTS (
    SELECT 1 FROM public.company_subscription cs WHERE cs.id_company = c.id_company
  );

  RAISE NOTICE 'Migración completada: % compañías asignadas a plan FREE',
    (SELECT COUNT(*) FROM public.company_subscription WHERE id_plan = free_plan_id);
END $$;

-- ===================================================================
-- VERIFICACIÓN
-- ===================================================================
SELECT
  c.name AS company_name,
  sp.code AS plan_code,
  sp.name AS plan_name,
  cs.status,
  cs.current_year_usage,
  sp.document_limit_yearly AS limit,
  cs.expires_at
FROM public.company_subscription cs
JOIN public.company c ON cs.id_company = c.id_company
JOIN public.subscription_plan sp ON cs.id_plan = sp.id_plan
ORDER BY c.name;
```

---

### **4. ebill-billing: Tabla `subscription_usage_history`**

```sql
-- ===================================================================
-- MICROSERVICIO: ebill-billing
-- TABLA: subscription_usage_history
-- DESCRIPCIÓN: Histórico mensual de uso de documentos
-- ===================================================================

CREATE TABLE IF NOT EXISTS public.subscription_usage_history (
  id_history SERIAL PRIMARY KEY,
  id_company INT NOT NULL REFERENCES company(id_company) ON DELETE CASCADE,
  id_subscription INT NOT NULL REFERENCES company_subscription(id_subscription) ON DELETE CASCADE,

  month_year VARCHAR(7) NOT NULL, -- '2025-12'

  -- Contadores por tipo de documento
  invoices_count INT DEFAULT 0,
  credit_notes_count INT DEFAULT 0,
  debit_notes_count INT DEFAULT 0,
  retentions_count INT DEFAULT 0,
  guides_count INT DEFAULT 0,
  total_documents INT DEFAULT 0,

  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW(),

  -- Constraint: Un registro por compañía por mes
  UNIQUE(id_company, month_year)
);

-- Índices
CREATE INDEX idx_subscription_usage_company ON public.subscription_usage_history(id_company);
CREATE INDEX idx_subscription_usage_month ON public.subscription_usage_history(month_year);

-- ===================================================================
-- FUNCIÓN: Incrementar uso al emitir documento
-- ===================================================================

CREATE OR REPLACE FUNCTION increment_document_usage()
RETURNS TRIGGER AS $$
DECLARE
  v_id_company INT;
  v_id_subscription INT;
  v_month_year VARCHAR(7);
  v_document_type VARCHAR(50);
BEGIN
  -- Obtener id_company del documento (varía según tabla)
  IF TG_TABLE_NAME = 'invoice' THEN
    v_id_company := NEW.id_company;
    v_document_type := 'INVOICE';
  ELSIF TG_TABLE_NAME = 'credit_note' THEN
    v_id_company := NEW.id_company;
    v_document_type := 'CREDIT_NOTE';
  ELSIF TG_TABLE_NAME = 'debit_note' THEN
    v_id_company := NEW.id_company;
    v_document_type := 'DEBIT_NOTE';
  ELSIF TG_TABLE_NAME = 'retention' THEN
    v_id_company := NEW.id_company;
    v_document_type := 'RETENTION';
  ELSIF TG_TABLE_NAME = 'guide' THEN
    v_id_company := NEW.id_company;
    v_document_type := 'GUIDE';
  ELSE
    RETURN NEW;
  END IF;

  -- Obtener month_year actual
  v_month_year := TO_CHAR(CURRENT_DATE, 'YYYY-MM');

  -- Obtener id_subscription activa
  SELECT id_subscription INTO v_id_subscription
  FROM public.company_subscription
  WHERE id_company = v_id_company AND status = 'ACTIVE'
  LIMIT 1;

  IF v_id_subscription IS NULL THEN
    RAISE WARNING 'No se encontró suscripción activa para company %', v_id_company;
    RETURN NEW;
  END IF;

  -- Insertar o actualizar registro mensual
  INSERT INTO public.subscription_usage_history
    (id_company, id_subscription, month_year,
     invoices_count, credit_notes_count, debit_notes_count, retentions_count, guides_count, total_documents)
  VALUES
    (v_id_company, v_id_subscription, v_month_year,
     CASE WHEN v_document_type = 'INVOICE' THEN 1 ELSE 0 END,
     CASE WHEN v_document_type = 'CREDIT_NOTE' THEN 1 ELSE 0 END,
     CASE WHEN v_document_type = 'DEBIT_NOTE' THEN 1 ELSE 0 END,
     CASE WHEN v_document_type = 'RETENTION' THEN 1 ELSE 0 END,
     CASE WHEN v_document_type = 'GUIDE' THEN 1 ELSE 0 END,
     1)
  ON CONFLICT (id_company, month_year) DO UPDATE SET
    invoices_count = subscription_usage_history.invoices_count +
      CASE WHEN v_document_type = 'INVOICE' THEN 1 ELSE 0 END,
    credit_notes_count = subscription_usage_history.credit_notes_count +
      CASE WHEN v_document_type = 'CREDIT_NOTE' THEN 1 ELSE 0 END,
    debit_notes_count = subscription_usage_history.debit_notes_count +
      CASE WHEN v_document_type = 'DEBIT_NOTE' THEN 1 ELSE 0 END,
    retentions_count = subscription_usage_history.retentions_count +
      CASE WHEN v_document_type = 'RETENTION' THEN 1 ELSE 0 END,
    guides_count = subscription_usage_history.guides_count +
      CASE WHEN v_document_type = 'GUIDE' THEN 1 ELSE 0 END,
    total_documents = subscription_usage_history.total_documents + 1,
    updated_at = NOW();

  -- Actualizar contador anual en company_subscription
  UPDATE public.company_subscription
  SET current_year_usage = current_year_usage + 1,
      updated_at = NOW()
  WHERE id_subscription = v_id_subscription;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ===================================================================
-- TRIGGERS: Aplicar a todas las tablas de documentos
-- ===================================================================

-- Trigger para invoices
CREATE TRIGGER trigger_invoice_usage
AFTER INSERT ON public.invoice
FOR EACH ROW
WHEN (NEW.authorization_sri IS NOT NULL) -- Solo contar docs autorizados
EXECUTE FUNCTION increment_document_usage();

-- Trigger para credit_notes
CREATE TRIGGER trigger_credit_note_usage
AFTER INSERT ON public.credit_note
FOR EACH ROW
WHEN (NEW.authorization_sri IS NOT NULL)
EXECUTE FUNCTION increment_document_usage();

-- Agregar triggers para otras tablas según tu esquema:
-- - debit_note
-- - retention
-- - guide

-- ===================================================================
-- VERIFICACIÓN
-- ===================================================================
SELECT
  c.name AS company_name,
  suh.month_year,
  suh.invoices_count,
  suh.credit_notes_count,
  suh.total_documents,
  sp.document_limit_yearly
FROM public.subscription_usage_history suh
JOIN public.company c ON suh.id_company = c.id_company
JOIN public.company_subscription cs ON suh.id_subscription = cs.id_subscription
JOIN public.subscription_plan sp ON cs.id_plan = sp.id_plan
ORDER BY c.name, suh.month_year DESC;
```

---

## 📡 **ENDPOINTS BACKEND**

### **ebill-authentication (Puerto 8280)**

#### **1. Verificar permiso de rol**

```http
POST /api/permission/check-role
Authorization: Bearer {JWT}
Content-Type: application/json

Request:
{
  "featureCode": "dashboard.view",
  "idUser": 123
}

Response 200:
{
  "allowed": true,
  "userRoles": ["admin"],
  "requiredRoles": [3], // IDs de roles permitidos
  "message": "Usuario tiene rol admin que permite esta feature"
}

Response 403:
{
  "allowed": false,
  "userRoles": ["member"],
  "requiredRoles": [3],
  "message": "Usuario con rol member no tiene permiso para esta feature"
}
```

**Lógica (Java/Spring Boot):**

```java
@PostMapping("/check-role")
public ResponseEntity<RolePermissionResponse> checkRolePermission(
    @RequestBody RolePermissionRequest request,
    @AuthenticationPrincipal UserDetails userDetails
) {
    // 1. Verificar si es super_admin (bypass)
    if (userHasRole(userDetails, "super_admin")) {
        return ResponseEntity.ok(new RolePermissionResponse(true, "Super admin access"));
    }

    // 2. Buscar feature en feature_permission
    FeaturePermission feature = featurePermissionRepository
        .findByFeatureCode(request.getFeatureCode())
        .orElseThrow(() -> new FeatureNotFoundException());

    // 3. Obtener roles del usuario
    List<Integer> userRoleIds = getUserRoleIds(userDetails);

    // 4. Verificar si algún rol del usuario está en allowed_roles
    boolean hasPermission = userRoleIds.stream()
        .anyMatch(roleId -> ArrayUtils.contains(feature.getAllowedRoles(), roleId));

    return ResponseEntity.ok(new RolePermissionResponse(
        hasPermission,
        hasPermission ? "Permiso concedido" : "Rol insuficiente"
    ));
}
```

---

### **ebill-billing (Puerto 8480)**

#### **1. Obtener suscripción actual**

```http
GET /api/subscription/{idCompany}/current
Authorization: Bearer {JWT}

Response 200:
{
  "idSubscription": 1,
  "plan": {
    "code": "PRO",
    "name": "Profesional",
    "priceAnnual": 79.00,
    "documentLimitYearly": 1500,
    "userLimit": 5,
    "establishmentLimit": 2,
    "issuancePointLimit": 2,
    "hasDashboard": true,
    "hasAdvancedReports": true,
    "hasDrafts": true,
    "hasApiAccess": false
  },
  "status": "ACTIVE",
  "startDate": "2025-01-01",
  "expiresAt": "2025-12-31",
  "currentYearUsage": 450,
  "percentUsed": 30.0,
  "documentsRemaining": 1050
}

Response 404:
{
  "error": "SUBSCRIPTION_NOT_FOUND",
  "message": "La compañía no tiene una suscripción activa"
}
```

**Lógica:**

```java
@GetMapping("/{idCompany}/current")
public ResponseEntity<SubscriptionResponse> getCurrentSubscription(
    @PathVariable Long idCompany,
    @AuthenticationPrincipal UserDetails userDetails
) {
    // 1. Verificar que el usuario pertenece a la compañía (o es super_admin)
    validateCompanyAccess(userDetails, idCompany);

    // 2. Buscar suscripción activa
    CompanySubscription subscription = subscriptionRepository
        .findByIdCompanyAndStatus(idCompany, SubscriptionStatus.ACTIVE)
        .orElseThrow(() -> new SubscriptionNotFoundException());

    // 3. Obtener datos del plan
    SubscriptionPlan plan = subscription.getPlan();

    // 4. Calcular métricas
    int usage = subscription.getCurrentYearUsage();
    int limit = plan.getDocumentLimitYearly();
    double percentUsed = limit > 0 ? (usage * 100.0 / limit) : 0;
    int remaining = limit > 0 ? Math.max(0, limit - usage) : Integer.MAX_VALUE;

    return ResponseEntity.ok(new SubscriptionResponse(
        subscription, plan, usage, percentUsed, remaining
    ));
}
```

---

#### **2. Obtener uso mensual/anual**

```http
GET /api/subscription/{idCompany}/usage
Authorization: Bearer {JWT}

Response 200:
{
  "currentMonth": {
    "month": "2025-12",
    "invoices": 25,
    "creditNotes": 3,
    "debitNotes": 0,
    "retentions": 10,
    "guides": 0,
    "total": 38
  },
  "currentYear": {
    "year": 2025,
    "total": 450,
    "limit": 1500,
    "percentUsed": 30.0,
    "remaining": 1050
  },
  "lastMonths": [
    { "month": "2025-11", "total": 42 },
    { "month": "2025-10", "total": 38 },
    { "month": "2025-09", "total": 45 }
  ]
}
```

---

#### **3. Verificar si puede emitir documento**

```http
POST /api/subscription/{idCompany}/can-emit
Authorization: Bearer {JWT}
Content-Type: application/json

Request:
{
  "documentType": "INVOICE" // INVOICE, CREDIT_NOTE, GUIDE, etc.
}

Response 200:
{
  "allowed": true,
  "currentUsage": 450,
  "limit": 1500,
  "remaining": 1050,
  "message": "Puede emitir documentos"
}

Response 403:
{
  "allowed": false,
  "currentUsage": 1500,
  "limit": 1500,
  "remaining": 0,
  "message": "Límite de documentos alcanzado. Actualice su plan.",
  "upgradeUrl": "/subscription/upgrade"
}
```

**Lógica:**

```java
@PostMapping("/{idCompany}/can-emit")
public ResponseEntity<EmissionCheckResponse> canEmitDocument(
    @PathVariable Long idCompany,
    @RequestBody EmissionCheckRequest request,
    @AuthenticationPrincipal UserDetails userDetails
) {
    // 1. Super admin puede emitir sin límite
    if (userHasRole(userDetails, "super_admin")) {
        return ResponseEntity.ok(new EmissionCheckResponse(true, "Super admin access"));
    }

    // 2. Obtener suscripción activa
    CompanySubscription subscription = subscriptionRepository
        .findByIdCompanyAndStatus(idCompany, SubscriptionStatus.ACTIVE)
        .orElseThrow(() -> new SubscriptionNotFoundException());

    // 3. Verificar si está expirada
    if (subscription.getExpiresAt().isBefore(LocalDate.now())) {
        return ResponseEntity.status(403).body(new EmissionCheckResponse(
            false, "Suscripción expirada"
        ));
    }

    // 4. Obtener límite y uso actual
    SubscriptionPlan plan = subscription.getPlan();
    Integer limit = plan.getDocumentLimitYearly();
    int usage = subscription.getCurrentYearUsage();

    // 5. Si el plan es ilimitado (NULL), siempre permite
    if (limit == null) {
        return ResponseEntity.ok(new EmissionCheckResponse(true, "Plan ilimitado"));
    }

    // 6. Verificar si ha alcanzado el límite
    if (usage >= limit) {
        return ResponseEntity.status(403).body(new EmissionCheckResponse(
            false, "Límite de documentos alcanzado", usage, limit
        ));
    }

    return ResponseEntity.ok(new EmissionCheckResponse(
        true, "Puede emitir documentos", usage, limit, limit - usage
    ));
}
```

---

#### **4. Upgrade manual de plan**

```http
POST /api/subscription/{idCompany}/upgrade
Authorization: Bearer {JWT}
Content-Type: application/json

Request:
{
  "newPlanCode": "BUSINESS",
  "paymentReference": "FAC-001-002-000123",
  "paymentAmount": 149.00,
  "paymentDate": "2025-12-13",
  "notes": "Upgrade por transferencia bancaria"
}

Response 200:
{
  "success": true,
  "message": "Suscripción actualizada exitosamente",
  "subscription": {
    "idSubscription": 1,
    "planCode": "BUSINESS",
    "planName": "Empresarial",
    "status": "ACTIVE",
    "startDate": "2025-12-13",
    "expiresAt": "2026-12-13"
  }
}

Response 400:
{
  "error": "INVALID_UPGRADE",
  "message": "No se puede downgrade a un plan inferior. Contacte soporte."
}
```

**Lógica:**

```java
@PostMapping("/{idCompany}/upgrade")
public ResponseEntity<UpgradeResponse> upgradeSubscription(
    @PathVariable Long idCompany,
    @RequestBody UpgradeRequest request,
    @AuthenticationPrincipal UserDetails userDetails
) {
    // 1. Verificar permisos (solo admin o super_admin)
    if (!userHasRole(userDetails, "admin") && !userHasRole(userDetails, "super_admin")) {
        throw new ForbiddenException("Solo administradores pueden actualizar suscripciones");
    }

    // 2. Obtener suscripción actual
    CompanySubscription current = subscriptionRepository
        .findByIdCompanyAndStatus(idCompany, SubscriptionStatus.ACTIVE)
        .orElseThrow(() -> new SubscriptionNotFoundException());

    // 3. Buscar nuevo plan
    SubscriptionPlan newPlan = planRepository
        .findByCode(request.getNewPlanCode())
        .orElseThrow(() -> new PlanNotFoundException());

    // 4. Validar upgrade (no permitir downgrade sin validación adicional)
    SubscriptionPlan currentPlan = current.getPlan();
    if (newPlan.getDisplayOrder() < currentPlan.getDisplayOrder()) {
        // Es un downgrade - requiere aprobación especial
        throw new InvalidUpgradeException("No se permite downgrade directo");
    }

    // 5. Actualizar suscripción
    current.setIdPlan(newPlan.getIdPlan());
    current.setStartDate(LocalDate.now());
    current.setExpiresAt(LocalDate.now().plusYears(1)); // 1 año desde hoy
    current.setPaymentMethod("MANUAL");
    current.setLastPaymentDate(request.getPaymentDate());
    current.setLastPaymentAmount(request.getPaymentAmount());
    current.setPaymentReference(request.getPaymentReference());
    current.setNotes(request.getNotes());
    current.setUpdatedAt(LocalDateTime.now());

    subscriptionRepository.save(current);

    // 6. Log de auditoría (opcional)
    auditService.logUpgrade(idCompany, currentPlan.getCode(), newPlan.getCode(), userDetails);

    return ResponseEntity.ok(new UpgradeResponse(
        true, "Suscripción actualizada", current, newPlan
    ));
}
```

---

### **ebill-authentication (Coordinador de validación completa)**

#### **2. Validación completa (rol + plan)**

**⚠️ IMPORTANTE:** Este endpoint se implementa en `ebill-authentication` y llama internamente a `ebill-billing`.

```http
POST /api/permission/validate-feature
Authorization: Bearer {JWT}
Content-Type: application/json

Request:
{
  "featureCode": "dashboard.view",
  "idCompany": 123
}

Response 200:
{
  "allowed": true,
  "validations": {
    "roleCheck": { "passed": true, "message": "Usuario tiene rol admin" },
    "planCheck": { "passed": true, "message": "Plan PRO incluye dashboard" }
  },
  "message": "Acceso permitido"
}

Response 403:
{
  "allowed": false,
  "validations": {
    "roleCheck": { "passed": true, "message": "Usuario tiene rol admin" },
    "planCheck": { "passed": false, "message": "Plan FREE no incluye dashboard" }
  },
  "reason": "NO_SUBSCRIPTION",
  "message": "Su plan actual no incluye esta funcionalidad",
  "upgradeUrl": "/subscription/upgrade"
}
```

**Lógica (Java/Spring Boot - en ebill-authentication):**

```java
// En ebill-authentication (Puerto 8280)
@PostMapping("/validate-feature")
public ResponseEntity<FeatureValidationResponse> validateFeature(
    @RequestBody FeatureValidationRequest request,
    @AuthenticationPrincipal UserDetails userDetails
) {
    // 1. Super admin siempre tiene acceso (bypass total)
    if (userHasRole(userDetails, "super_admin")) {
        return ResponseEntity.ok(new FeatureValidationResponse(
            true, "Super admin bypass"
        ));
    }

    // 2. Validar ROL (local)
    FeaturePermission feature = featurePermissionRepository
        .findByFeatureCode(request.getFeatureCode())
        .orElseThrow(() -> new FeatureNotFoundException());

    List<Integer> userRoleIds = getUserRoleIds(userDetails);
    boolean hasRolePermission = userRoleIds.stream()
        .anyMatch(roleId -> ArrayUtils.contains(feature.getAllowedRoles(), roleId));

    // 3. Si no tiene el rol, denegar inmediatamente
    if (!hasRolePermission) {
        return ResponseEntity.status(403).body(new FeatureValidationResponse(
            false, "NO_ROLE", "No tiene permisos para esta funcionalidad"
        ));
    }

    // 4. Llamar a ebill-billing para validar PLAN (RestTemplate o WebClient)
    String billingUrl = "http://localhost:8480/api/subscription/" +
                        request.getIdCompany() + "/current";

    try {
        SubscriptionResponse subscription = restTemplate.getForObject(
            billingUrl, SubscriptionResponse.class
        );

        // 5. Verificar si el plan incluye la feature
        boolean planIncludesFeature = Arrays.asList(feature.getRequiredPlans())
            .contains(subscription.getPlan().getCode());

        // 6. Si el plan no incluye la feature, denegar
        if (!planIncludesFeature) {
            return ResponseEntity.status(403).body(new FeatureValidationResponse(
                false, "NO_SUBSCRIPTION",
                "Su plan actual no incluye esta funcionalidad. Actualice su suscripción."
            ));
        }

        // 7. Todo OK, permitir acceso
        return ResponseEntity.ok(new FeatureValidationResponse(
            true, "Acceso permitido"
        ));

    } catch (Exception e) {
        // Si ebill-billing no responde, denegar por seguridad
        logger.error("Error al validar suscripción", e);
        return ResponseEntity.status(403).body(new FeatureValidationResponse(
            false, "NO_SUBSCRIPTION", "Error al verificar suscripción"
        ));
    }
}
```

---

## 🎨 **FRONTEND - Especificación Angular**

### **1. PermissionService (Prioridad ALTA)**

**Archivo:** `src/app/_service/permission/permission.service.ts`

```typescript
import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable, of, BehaviorSubject } from "rxjs";
import { map, catchError, tap } from "rxjs/operators";
import { environment } from "../../../environments/environment";
import { decodeToken } from "../../_functions/functions";

export interface SubscriptionPlan {
  code: string;
  name: string;
  priceAnnual: number;
  documentLimitYearly: number | null;
  userLimit: number | null;
  establishmentLimit: number | null;
  issuancePointLimit: number | null;
  hasDashboard: boolean;
  hasAdvancedReports: boolean;
  hasDrafts: boolean;
  hasApiAccess: boolean;
  hasWebhooks: boolean;
  hasErpIntegration: boolean;
  hasInventory: boolean;
  hasScheduledReports: boolean;
  hasCsvImport: boolean;
  hasGuides: boolean;
}

export interface CompanySubscription {
  idSubscription: number;
  plan: SubscriptionPlan;
  status: "ACTIVE" | "EXPIRED" | "CANCELLED" | "SUSPENDED" | "TRIAL";
  startDate: string;
  expiresAt: string;
  currentYearUsage: number;
  percentUsed: number;
  documentsRemaining: number;
}

export interface FeatureValidationResponse {
  allowed: boolean;
  reason?: "NO_ROLE" | "NO_SUBSCRIPTION" | "LIMIT_EXCEEDED" | "EXPIRED";
  message: string;
  upgradeUrl?: string;
}

@Injectable({
  providedIn: "root",
})
export class PermissionService {
  private readonly authUrl = `${environment.HOST_AUTH}/api/permission`;
  private readonly billingUrl = `${environment.HOST_BILLING}/api/subscription`;

  // Cache de suscripción para evitar llamadas repetidas
  private subscriptionCache$ = new BehaviorSubject<CompanySubscription | null>(null);

  constructor(private readonly http: HttpClient) {
    // Cargar suscripción al iniciar
    this.loadSubscription();
  }

  /**
   * Cargar suscripción actual desde backend y cachear
   */
  private loadSubscription(): void {
    const token = decodeToken();
    if (!token || !token.idCompany) {
      return;
    }

    this.http.get<CompanySubscription>(`${this.billingUrl}/${token.idCompany}/current`).subscribe({
      next: (subscription) => {
        this.subscriptionCache$.next(subscription);
        // Guardar en localStorage para uso offline
        localStorage.setItem("SUBSCRIPTION_DATA", JSON.stringify(subscription));
      },
      error: (err) => {
        console.error("Error al cargar suscripción:", err);
        // Intentar cargar desde localStorage
        const cached = localStorage.getItem("SUBSCRIPTION_DATA");
        if (cached) {
          this.subscriptionCache$.next(JSON.parse(cached));
        }
      },
    });
  }

  /**
   * Obtener suscripción actual (Observable)
   */
  getSubscription(): Observable<CompanySubscription | null> {
    return this.subscriptionCache$.asObservable();
  }

  /**
   * Refrescar suscripción desde backend
   */
  refreshSubscription(): void {
    this.loadSubscription();
  }

  /**
   * Verificar si el usuario puede acceder a una feature
   * Valida AMBAS capas: rol + suscripción
   */
  canAccess(featureCode: string): Observable<boolean> {
    const token = decodeToken();
    if (!token) {
      return of(false);
    }

    // Super admin siempre tiene acceso
    if (this.isSuperAdmin()) {
      return of(true);
    }

    const request = {
      featureCode,
      idCompany: token.idCompany,
      idUser: token.idUser,
    };

    return this.http.post<FeatureValidationResponse>(`${this.securityUrl}/validate-feature`, request).pipe(
      map((response) => response.allowed),
      catchError((error) => {
        console.error(`Error validando feature ${featureCode}:`, error);
        return of(false);
      })
    );
  }

  /**
   * Verificar permiso con detalles (para mostrar mensajes de upgrade)
   */
  canAccessWithDetails(featureCode: string): Observable<FeatureValidationResponse> {
    const token = decodeToken();
    if (!token) {
      return of({
        allowed: false,
        reason: "NO_ROLE",
        message: "Usuario no autenticado",
      });
    }

    if (this.isSuperAdmin()) {
      return of({
        allowed: true,
        message: "Acceso de super administrador",
      });
    }

    const request = {
      featureCode,
      idCompany: token.idCompany,
      idUser: token.idUser,
    };

    return this.http.post<FeatureValidationResponse>(`${this.authUrl}/validate-feature`, request).pipe(
      catchError((error) => {
        console.error(`Error validando feature ${featureCode}:`, error);
        return of({
          allowed: false,
          reason: "NO_ROLE",
          message: "Error al verificar permisos",
        });
      })
    );
  }

  /**
   * Verificar si puede emitir un documento
   */
  canEmitDocument(documentType: string = "INVOICE"): Observable<boolean> {
    const token = decodeToken();
    if (!token || !token.idCompany) {
      return of(false);
    }

    if (this.isSuperAdmin()) {
      return of(true);
    }

    const request = { documentType };
    return this.http.post<{ allowed: boolean }>(`${this.billingUrl}/${token.idCompany}/can-emit`, request).pipe(
      map((response) => response.allowed),
      catchError(() => of(false))
    );
  }

  /**
   * Verificar si el usuario es super_admin
   */
  isSuperAdmin(): boolean {
    const token = decodeToken();
    if (!token || !token.roles) {
      return false;
    }
    return token.roles.includes("super_admin");
  }

  /**
   * Verificar si el usuario tiene un rol específico
   */
  hasRole(roleName: string): boolean {
    const token = decodeToken();
    if (!token || !token.roles) {
      return false;
    }
    return token.roles.includes(roleName);
  }

  /**
   * Obtener código del plan actual (desde caché)
   */
  getCurrentPlanCode(): string {
    const subscription = this.subscriptionCache$.value;
    return subscription?.plan?.code || "FREE";
  }

  /**
   * Verificar si el plan actual incluye una feature (caché local)
   */
  planHasFeature(feature: keyof SubscriptionPlan): boolean {
    const subscription = this.subscriptionCache$.value;
    if (!subscription) {
      return false;
    }
    return subscription.plan[feature] as boolean;
  }
}
```

---

### **2. HasPermissionDirective**

**Archivo:** `src/app/_directives/has-permission.directive.ts`

```typescript
import { Directive, Input, TemplateRef, ViewContainerRef, OnInit, OnDestroy } from "@angular/core";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { PermissionService } from "../_service/permission/permission.service";

@Directive({
  selector: "[hasPermission]",
})
export class HasPermissionDirective implements OnInit, OnDestroy {
  @Input() hasPermission!: string; // Feature code
  @Input() hasPermissionElse?: TemplateRef<any>; // Fallback template

  private destroy$ = new Subject<void>();

  constructor(private readonly templateRef: TemplateRef<any>, private readonly viewContainer: ViewContainerRef, private readonly permissionService: PermissionService) {}

  ngOnInit(): void {
    // Super admin siempre tiene acceso
    if (this.permissionService.isSuperAdmin()) {
      this.viewContainer.createEmbeddedView(this.templateRef);
      return;
    }

    // Verificar permiso
    this.permissionService
      .canAccess(this.hasPermission)
      .pipe(takeUntil(this.destroy$))
      .subscribe((allowed) => {
        this.viewContainer.clear();

        if (allowed) {
          this.viewContainer.createEmbeddedView(this.templateRef);
        } else if (this.hasPermissionElse) {
          this.viewContainer.createEmbeddedView(this.hasPermissionElse);
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
```

**Uso en templates:**

```html
<!-- Mostrar si tiene permiso -->
<div *hasPermission="'dashboard.view'">
  <app-welcome-dashboard></app-welcome-dashboard>
</div>

<!-- Mostrar con fallback de upgrade -->
<button *hasPermission="'reports.advanced'; else upgradeTpl">Ver Reportes Avanzados</button>
<ng-template #upgradeTpl>
  <button class="upgrade-btn" (click)="showUpgradeDialog()">🔒 Actualizar Plan para acceder</button>
</ng-template>
```

---

### **3. PermissionGuard**

**Archivo:** `src/app/_guard/permission.guard.ts`

```typescript
import { Injectable } from "@angular/core";
import { CanActivate, ActivatedRouteSnapshot, Router, UrlTree } from "@angular/router";
import { Observable } from "rxjs";
import { map } from "rxjs/operators";
import { PermissionService } from "../_service/permission/permission.service";
import { ToastService } from "../_service/notification/toast.service";

@Injectable({
  providedIn: "root",
})
export class PermissionGuard implements CanActivate {
  constructor(private readonly permissionService: PermissionService, private readonly router: Router, private readonly toastService: ToastService) {}

  canActivate(route: ActivatedRouteSnapshot): Observable<boolean | UrlTree> | boolean {
    const requiredPermission = route.data["permission"] as string;

    if (!requiredPermission) {
      console.warn("[PermissionGuard] No se especificó permiso requerido");
      return true;
    }

    // Super admin siempre puede acceder
    if (this.permissionService.isSuperAdmin()) {
      return true;
    }

    // Verificar permiso
    return this.permissionService.canAccessWithDetails(requiredPermission).pipe(
      map((response) => {
        if (response.allowed) {
          return true;
        }

        // Mostrar mensaje según el motivo
        switch (response.reason) {
          case "NO_SUBSCRIPTION":
            this.toastService.warning("🔒 Esta funcionalidad requiere un plan superior. Actualiza tu suscripción.");
            return this.router.createUrlTree(["/subscription"]);

          case "LIMIT_EXCEEDED":
            this.toastService.warning("⚠️ Has alcanzado el límite de tu plan. Actualiza tu suscripción.");
            return this.router.createUrlTree(["/subscription"]);

          case "EXPIRED":
            this.toastService.error("❌ Tu suscripción ha expirado. Renueva tu plan.");
            return this.router.createUrlTree(["/subscription"]);

          case "NO_ROLE":
          default:
            this.toastService.error("⛔ No tienes permisos para acceder a esta sección.");
            return this.router.createUrlTree(["/welcome"]);
        }
      })
    );
  }
}
```

**Uso en routing:**

```typescript
{
  path: 'reports/advanced',
  canActivate: [SessionValidationGuard, PermissionGuard],
  data: { permission: 'reports.advanced' },
  component: SalesDashboardComponent
},
{
  path: 'draft-invoices',
  canActivate: [SessionValidationGuard, PermissionGuard],
  data: { permission: 'billing.draft.manage' },
  component: DraftInvoicesComponent
}
```

---

## ✅ **CHECKLIST DE IMPLEMENTACIÓN**

### **Backend (ebill-authentication):**

- [ ] Crear tabla `feature_permission`
- [ ] Insertar 15 features base
- [ ] Endpoint `POST /api/permission/check-role`
- [ ] Testing de endpoints

### **Backend (ebill-billing):**

- [ ] Crear tabla `subscription_plan`
- [ ] Crear tabla `company_subscription`
- [ ] Crear tabla `subscription_usage_history`
- [ ] Insertar 5 planes (FREE → ENTERPRISE)
- [ ] Migrar compañías existentes a plan FREE
- [ ] Crear función `increment_document_usage()`
- [ ] Crear triggers en `invoice`, `credit_note`, etc.
- [ ] Endpoint `GET /api/subscription/{id}/current`
- [ ] Endpoint `GET /api/subscription/{id}/usage`
- [ ] Endpoint `POST /api/subscription/{id}/can-emit`
- [ ] Endpoint `POST /api/subscription/{id}/upgrade`
- [ ] Testing de endpoints

### **Backend (ebill-authentication - Coordinador):**

- [ ] Endpoint `POST /api/permission/validate-feature` (coordinador)
- [ ] Cliente HTTP (RestTemplate/WebClient) para llamar a ebill-billing
- [ ] Testing de integración entre authentication y billing

### **Frontend (Angular):**

- [ ] Crear `PermissionService`
- [ ] Crear `HasPermissionDirective`
- [ ] Crear `PermissionGuard`
- [ ] Registrar directiva en `SharedModule`
- [ ] Actualizar `app-routing.module.ts` con guards
- [ ] Testing de servicios

---

## 🚀 **SIGUIENTE PASO:**

**¿Comenzamos con el backend o el frontend?**

**Opciones:**

1. **"backend primero"** → SQL scripts + endpoints de cada microservicio
2. **"frontend primero"** → Services + Directiva + Guard (mocking backend)
3. **"ambos paralelo"** → Trabajamos frontend mientras backend se implementa

**Dime cómo prefieres proceder.** 🎯
