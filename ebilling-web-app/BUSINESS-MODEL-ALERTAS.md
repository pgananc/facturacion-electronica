# 💼 Modelo de Negocio: Sistema de Alertas y Suscripciones

**Fecha:** 13 de Diciembre, 2025  
**Contexto:** Definir Fase 3 del Welcome Dashboard

---

## 🎯 **SITUACIÓN ACTUAL:**

### **Endpoints disponibles:**

| Funcionalidad           | Endpoint                            | Estado        | Notas                            |
| ----------------------- | ----------------------------------- | ------------- | -------------------------------- |
| **Facturas Rechazadas** | `POST /api/report/invoices/pending` | ✅ **EXISTE** | Puede filtrar por status         |
| **Últimas Facturas**    | `GET /api/dashboard/welcome`        | ✅ **EXISTE** | Ya implementado                  |
| **Secuencial Bajo**     | ❌ **NO EXISTE**                    | ❌ Pendiente  | Depende de modelo de suscripción |
| **Récord de Ventas**    | ⚠️ **PARCIAL**                      | ⚠️ Calculable | Comparar con histórico           |

---

## 📊 **ANÁLISIS: ¿QUÉ ALERTAS PODEMOS IMPLEMENTAR AHORA?**

### **1. Facturas Rechazadas por SRI** ⭐ (FÁCIL - YA TENEMOS ENDPOINT)

**Endpoint existente:**

```
POST /api/report/invoices/pending
Body: {
  "dateFrom": "2025-12-01",
  "dateTo": "2025-12-13",
  "idCompany": 1,
  "statusFilter": "RECHAZADA"
}
```

**Alerta propuesta:**

```
🔴 ERROR | PRIORIDAD ALTA
"3 facturas rechazadas requieren atención"
Click → Navega a /reports/pending-invoices?status=RECHAZADA
```

**Implementación:** ⏱️ 15 minutos

---

### **2. Secuencial Bajo** ⚠️ (NECESITA DEFINICIÓN DE NEGOCIO)

**Preguntas a responder:**

#### **A. ¿Qué significa "secuencial bajo"?**

**Opción 1: Por punto de emisión (físico)**

- Ejemplo: Punto 001-001 autorizado del 1 al 1000
- Usado hasta: 950
- Disponibles: 50
- **Alerta si:** Disponibles < 100

**Opción 2: Por suscripción (límite mensual)**

- Plan Básico: 100 facturas/mes
- Usadas en diciembre: 85
- Disponibles: 15
- **Alerta si:** Disponibles < 20% del plan

**Opción 3: Ambas**

#### **B. ¿Cuál es tu modelo de negocio?**

**Modelo A: Suscripción por límite de documentos**

```
Plan      | Docs/mes | Usuarios | Precio/mes
----------|----------|----------|------------
Básico    | 100      | 1        | $29
Estándar  | 500      | 3        | $79
Pro       | 2000     | 10       | $199
Ilimitado | ∞        | ∞        | $499
```

**Modelo B: Pago por uso (como AWS)**

```
Primeros 100 docs: $0.25 c/u = $25
Docs 101-500: $0.20 c/u
Docs 501+: $0.15 c/u
```

**Modelo C: Híbrido**

```
Base: $50/mes (incluye 200 docs)
Adicionales: $0.20 c/u
```

**Modelo D: Free forever (solo costo de firma digital)**

```
Usuario paga solo: Firma digital al SRI (~$50/año)
Sistema es gratis (tu negocio es consultoría/soporte)
```

#### **C. ¿Dónde se almacenan los límites?**

**Opción 1: En tabla `company`**

```sql
ALTER TABLE company ADD COLUMN subscription_plan VARCHAR(50);
ALTER TABLE company ADD COLUMN monthly_document_limit INT;
ALTER TABLE company ADD COLUMN current_month_usage INT;
ALTER TABLE company ADD COLUMN subscription_expires_at DATE;
```

**Opción 2: Nueva tabla `subscription`**

```sql
CREATE TABLE subscription (
  id_subscription SERIAL PRIMARY KEY,
  id_company INT REFERENCES company(id_company),
  plan_name VARCHAR(50),
  document_limit_monthly INT,
  users_limit INT,
  establishments_limit INT,
  price_monthly DECIMAL(10,2),
  start_date DATE,
  expires_at DATE,
  is_active BOOLEAN DEFAULT true,
  created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE subscription_usage (
  id_usage SERIAL PRIMARY KEY,
  id_subscription INT REFERENCES subscription(id_subscription),
  month_year VARCHAR(7), -- "2025-12"
  documents_used INT DEFAULT 0,
  invoices_count INT DEFAULT 0,
  credit_notes_count INT DEFAULT 0,
  debit_notes_count INT DEFAULT 0,
  updated_at TIMESTAMP DEFAULT NOW()
);
```

---

### **3. Récord de Ventas** ⭐ (FÁCIL - SOLO CÁLCULO)

**Lógica:**

```sql
-- Ventas de hoy
SELECT COALESCE(SUM(i.total_amount), 0) as today
FROM invoice i
WHERE i.id_company = ? AND DATE(i.issue_date) = CURRENT_DATE;

-- Promedio histórico del mismo día de la semana
SELECT AVG(daily_total) as avg_same_weekday
FROM (
  SELECT DATE(i.issue_date), SUM(i.total_amount) as daily_total
  FROM invoice i
  WHERE i.id_company = ?
  AND EXTRACT(DOW FROM i.issue_date) = EXTRACT(DOW FROM CURRENT_DATE)
  AND i.issue_date < CURRENT_DATE
  GROUP BY DATE(i.issue_date)
) AS weekday_sales;

-- Si today > avg_same_weekday * 1.2 → RÉCORD!
```

**Alerta propuesta:**

```
🏆 SUCCESS | PRIORIDAD BAJA
"¡Récord de ventas! 20% más que el promedio"
```

**Implementación:** ⏱️ 20 minutos

---

## 🎯 **PROPUESTA DE IMPLEMENTACIÓN:**

### **FASE 3A - Alertas Básicas (SIN modelo de suscripción)** ⭐ AHORA

**Alertas a implementar:**

1. ✅ **Facturas Rechazadas** (ya tenemos endpoint)

   - Llama a `/api/report/invoices/pending` con `statusFilter=RECHAZADA`
   - Cuenta cuántas hay en los últimos 7 días
   - Muestra alerta si hay > 0

2. ✅ **Récord de Ventas** (cálculo simple)
   - Compara ventas de hoy vs promedio del mes
   - Muestra alerta si hay incremento > 50%

**Tiempo estimado:** ⏱️ 30 minutos

---

### **FASE 3B - Alertas Avanzadas (CON modelo de suscripción)** ⏸️ FUTURO

**Requiere decisiones de negocio:**

1. ❓ **Modelo de suscripción:**

   - ¿Límite mensual de documentos?
   - ¿Usuarios concurrentes?
   - ¿Sucursales máximas?
   - ¿Precio?

2. ❓ **Base de datos:**

   - Nueva tabla `subscription`
   - Nueva tabla `subscription_usage`
   - Migración de datos existentes

3. ❓ **Backend endpoints nuevos:**

   - `GET /api/subscription/current` - Info del plan actual
   - `GET /api/subscription/usage` - Uso mensual
   - `POST /api/subscription/upgrade` - Cambiar plan

4. ❓ **Alertas relacionadas:**
   - Límite de documentos alcanzado (90%)
   - Suscripción próxima a vencer (7 días)
   - Límite de usuarios alcanzado
   - Plan excedido (cobro extra)

**Tiempo estimado:** ⏱️ 2-3 días (backend + frontend)

---

## 💡 **MI RECOMENDACIÓN:**

### **AHORA (Diciembre 2025):**

✅ Implementar **Fase 3A** (alertas básicas):

- Facturas rechazadas
- Récord de ventas

**Beneficios:**

- ✅ Welcome queda funcional y atractivo
- ✅ No bloquea el lanzamiento
- ✅ Mejora UX inmediatamente

---

### **DESPUÉS (Enero-Febrero 2026):**

⏸️ Diseñar **Fase 3B** (modelo de suscripción):

**Proceso sugerido:**

1. **Semana 1-2: Definir modelo de negocio**

   - Reunión con stakeholders
   - Análisis de mercado (competencia)
   - Definir planes y precios
   - Documentar reglas de negocio

2. **Semana 3-4: Diseño técnico**

   - Esquema de BD (subscription, usage)
   - API spec (endpoints de suscripción)
   - Plan de migración de datos
   - Estrategia de billing (Stripe/PayPal/Manual)

3. **Semana 5-8: Implementación**

   - Backend: Tablas + endpoints
   - Frontend: Panel de suscripción
   - Integración de pago
   - Testing completo

4. **Semana 9: Lanzamiento**
   - Migrar clientes existentes a plan por defecto
   - Comunicación a usuarios
   - Monitoreo de uso

---

## 📋 **PREGUNTAS PARA TI:**

### **1. Modelo de Negocio**

- ¿Quieres cobrar por **límite de documentos** o es **gratis**?
- ¿Cuántos **planes** ofrecerás? (Básico, Estándar, Pro, etc.)
- ¿Límites de **usuarios concurrentes**?
- ¿Límites de **sucursales**?

### **2. Alertas**

- ¿Implementamos **Fase 3A** (facturas rechazadas + récord) **ahora**?
- ¿O esperamos a definir el modelo de suscripción primero?

### **3. Facturación**

- ¿Cobro mensual **automático** (Stripe/PayPal)?
- ¿Cobro **manual** (factura mensual)?
- ¿Sin cobro\*\* (solo firma digital)?

---

## 🚀 **MI SUGERENCIA:**

**AHORA (próximos 30 min):**

```
✅ Implementar Fase 3A (alertas básicas)
✅ Completar Welcome Dashboard
✅ Sistema funcional para lanzamiento
```

**DESPUÉS (próximas semanas):**

```
📋 Reunión de modelo de negocio
🗂️ Diseño de suscripciones
💳 Integración de pagos
📊 Panel de administración de planes
```

---

**¿Qué prefieres?**

1. **"fase 3a ahora"** → Implemento alertas básicas (rechazadas + récord)
2. **"definamos negocio"** → Discutimos modelo de suscripción primero
3. **"déjalo así"** → Welcome queda con Fase 1 + 2 solamente

**Dime cómo proceder.** 🚀
