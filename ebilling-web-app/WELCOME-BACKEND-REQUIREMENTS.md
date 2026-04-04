# 📋 Requerimientos Backend - Pantalla Welcome

## 🎯 **Objetivo**

Proveer un endpoint optimizado que retorne **todos los datos necesarios** para la pantalla Welcome en una sola llamada, minimizando latencia y requests HTTP.

---

## 📊 **Endpoint Propuesto**

### **GET `/api/dashboard/welcome`**

**Descripción**: Retorna un resumen ejecutivo con KPIs, últimas facturas, y alertas para la pantalla de bienvenida.

---

## 📥 **Request Parameters (Query String)**

| Parámetro   | Tipo     | Requerido | Descripción      | Ejemplo |
| ----------- | -------- | --------- | ---------------- | ------- |
| `companyId` | `number` | ✅ Sí     | ID de la empresa | `1`     |

---

## 📤 **Response Body (JSON)**

```json
{
  "status": "OK",
  "message": "Dashboard de bienvenida cargado exitosamente",
  "data": {
    // ========================================
    // 1. KPIs PRINCIPALES
    // ========================================
    "kpis": {
      "todaySales": {
        "amount": 1245.5,
        "invoiceCount": 3,
        "percentageChange": 15.5 // % vs día anterior (positivo o negativo)
      },
      "weekSales": {
        "amount": 8920.75,
        "invoiceCount": 25,
        "percentageChange": -3.2 // Opcional
      },
      "monthSales": {
        "amount": 35450.0,
        "invoiceCount": 87,
        "percentageChange": 12.8, // % vs mes anterior
        "goalPercentage": 70.5 // % vs meta mensual (si existe)
      },
      "pendingCollection": {
        "amount": 12300.0,
        "invoiceCount": 18,
        "overdueCount": 3 // Facturas vencidas
      }
    },

    // ========================================
    // 2. TENDENCIA DE VENTAS (Últimos 7 días)
    // ========================================
    "salesTrend": [
      {
        "date": "2025-12-07", // ISO 8601 format
        "dayOfWeek": "Lunes", // Opcional, front puede calcularlo
        "amount": 1180.5,
        "invoiceCount": 8
      },
      {
        "date": "2025-12-08",
        "dayOfWeek": "Martes",
        "amount": 1320.75,
        "invoiceCount": 10
      },
      // ... 5 días más hasta completar 7
      {
        "date": "2025-12-13",
        "dayOfWeek": "Viernes",
        "amount": 1245.5,
        "invoiceCount": 9
      }
    ],
    "trendSummary": {
      "average": 1274.5, // Promedio diario últimos 7 días
      "total": 8920.75 // Total últimos 7 días
    },

    // ========================================
    // 3. ÚLTIMAS FACTURAS EMITIDAS (5 más recientes)
    // ========================================
    "recentInvoices": [
      {
        "idInvoice": 123,
        "invoiceNumber": "FAC-001-001-000123",
        "clientName": "Juan Pérez Gómez",
        "clientIdentification": "1234567890",
        "totalAmount": 125.5,
        "issueDate": "2025-12-13T14:30:00Z", // ISO 8601 con hora
        "authorizationStatus": "AUTORIZADA", // AUTORIZADA, PENDIENTE, RECHAZADA
        "paymentStatus": "PENDING" // PAID, PENDING, OVERDUE
      },
      {
        "idInvoice": 122,
        "invoiceNumber": "FAC-001-001-000122",
        "clientName": "María López Rodríguez",
        "clientIdentification": "0987654321",
        "totalAmount": 89.0,
        "issueDate": "2025-12-13T10:15:00Z",
        "authorizationStatus": "AUTORIZADA",
        "paymentStatus": "PAID"
      }
      // ... hasta 5 facturas
    ],

    // ========================================
    // 4. ALERTAS Y NOTIFICACIONES
    // ========================================
    "alerts": [
      {
        "id": "rejected-invoices-001",
        "type": "ERROR", // ERROR, WARNING, INFO, SUCCESS
        "priority": "HIGH", // HIGH, MEDIUM, LOW
        "title": "Facturas rechazadas por el SRI",
        "message": "3 facturas requieren atención inmediata",
        "count": 3,
        "actionUrl": "/viewing-invoices?status=RECHAZADA",
        "createdAt": "2025-12-13T08:00:00Z"
      },
      {
        "id": "low-stock-point-001",
        "type": "WARNING",
        "priority": "MEDIUM",
        "title": "Punto de emisión próximo a agotarse",
        "message": "Punto 001-001 tiene solo 50 números restantes",
        "count": 50,
        "actionUrl": "/establishments",
        "metadata": {
          "establishmentCode": "001",
          "issuancePointCode": "001",
          "remaining": 50,
          "threshold": 100
        },
        "createdAt": "2025-12-12T15:00:00Z"
      },
      {
        "id": "record-sales-week",
        "type": "SUCCESS",
        "priority": "LOW",
        "title": "¡Récord de ventas esta semana!",
        "message": "Has alcanzado $8,920.75 - Tu mejor semana del mes",
        "actionUrl": null,
        "metadata": {
          "amount": 8920.75,
          "previousRecord": 7500.0
        },
        "createdAt": "2025-12-13T23:59:00Z"
      }
    ]
  }
}
```

---

## 🔧 **Lógica de Negocio**

### **1. KPIs - Cálculo de Períodos**

#### **Ventas HOY**

```sql
SELECT
  COALESCE(SUM(total), 0) as amount,
  COUNT(*) as invoiceCount
FROM invoice
WHERE
  company_id = :companyId
  AND DATE(issue_date) = CURRENT_DATE
  AND authorization_status_sri = 'AUTORIZADA'
  AND status = true;  -- Activas (no anuladas)
```

**Porcentaje de cambio vs día anterior:**

```sql
-- Ventas de ayer
SELECT COALESCE(SUM(total), 0) as yesterdayAmount
FROM invoice
WHERE
  company_id = :companyId
  AND DATE(issue_date) = CURRENT_DATE - INTERVAL '1 day'
  AND authorization_status_sri = 'AUTORIZADA'
  AND status = true;

-- Calcular % cambio
percentageChange = ((todayAmount - yesterdayAmount) / yesterdayAmount) * 100
```

---

#### **Ventas SEMANA (Últimos 7 días)**

```sql
SELECT
  COALESCE(SUM(total), 0) as amount,
  COUNT(*) as invoiceCount
FROM invoice
WHERE
  company_id = :companyId
  AND issue_date >= CURRENT_DATE - INTERVAL '7 days'
  AND issue_date < CURRENT_DATE + INTERVAL '1 day'
  AND authorization_status_sri = 'AUTORIZADA'
  AND status = true;
```

---

#### **Ventas MES (Mes actual)**

```sql
SELECT
  COALESCE(SUM(total), 0) as amount,
  COUNT(*) as invoiceCount
FROM invoice
WHERE
  company_id = :companyId
  AND DATE_TRUNC('month', issue_date) = DATE_TRUNC('month', CURRENT_DATE)
  AND authorization_status_sri = 'AUTORIZADA'
  AND status = true;
```

**Porcentaje de cambio vs mes anterior:**

```sql
SELECT COALESCE(SUM(total), 0) as previousMonthAmount
FROM invoice
WHERE
  company_id = :companyId
  AND DATE_TRUNC('month', issue_date) = DATE_TRUNC('month', CURRENT_DATE - INTERVAL '1 month')
  AND authorization_status_sri = 'AUTORIZADA'
  AND status = true;

percentageChange = ((monthAmount - previousMonthAmount) / previousMonthAmount) * 100
```

---

#### **Pendientes de Cobro**

```sql
SELECT
  COALESCE(SUM(total - COALESCE(amount_paid, 0)), 0) as pendingAmount,
  COUNT(*) as invoiceCount,
  COUNT(CASE WHEN due_date < CURRENT_DATE THEN 1 END) as overdueCount
FROM invoice
WHERE
  company_id = :companyId
  AND payment_status != 'PAID'  -- No pagadas completamente
  AND authorization_status_sri = 'AUTORIZADA'
  AND status = true;
```

---

### **2. Tendencia de Ventas (Últimos 7 Días)**

```sql
SELECT
  DATE(issue_date) as date,
  COALESCE(SUM(total), 0) as amount,
  COUNT(*) as invoiceCount
FROM invoice
WHERE
  company_id = :companyId
  AND issue_date >= CURRENT_DATE - INTERVAL '7 days'
  AND issue_date < CURRENT_DATE + INTERVAL '1 day'
  AND authorization_status_sri = 'AUTORIZADA'
  AND status = true
GROUP BY DATE(issue_date)
ORDER BY date ASC;
```

**Resumen:**

```sql
-- Promedio diario
average = total / 7

-- Total
total = SUM(amount) de los 7 días
```

---

### **3. Últimas Facturas (5 más recientes)**

```sql
SELECT
  i.id_invoice as idInvoice,
  i.invoice_number as invoiceNumber,
  c.name as clientName,
  c.identification as clientIdentification,
  i.total as totalAmount,
  i.issue_date as issueDate,
  i.authorization_status_sri as authorizationStatus,
  i.payment_status as paymentStatus
FROM invoice i
INNER JOIN client c ON i.client_id = c.id_client
WHERE
  i.company_id = :companyId
  AND i.status = true
ORDER BY i.issue_date DESC
LIMIT 5;
```

---

### **4. Alertas y Notificaciones**

#### **4.1 - Facturas Rechazadas**

```sql
SELECT COUNT(*) as rejectedCount
FROM invoice
WHERE
  company_id = :companyId
  AND authorization_status_sri = 'RECHAZADA'
  AND DATE(issue_date) >= CURRENT_DATE - INTERVAL '7 days'  -- Últimos 7 días
  AND status = true;
```

**Condición:** Si `rejectedCount > 0`, generar alerta tipo `ERROR`.

---

#### **4.2 - Punto de Emisión Próximo a Agotarse**

```sql
SELECT
  e.code as establishmentCode,
  ip.code as issuancePointCode,
  ip.current_sequential as currentSequential,
  ip.end_sequential as endSequential,
  (ip.end_sequential - ip.current_sequential) as remaining
FROM issuance_point ip
INNER JOIN establishment e ON ip.establishment_id = e.id_establishment
WHERE
  e.company_id = :companyId
  AND ip.status = true
  AND (ip.end_sequential - ip.current_sequential) < 100  -- Threshold: 100 números
ORDER BY remaining ASC
LIMIT 1;  -- Solo el más crítico
```

**Condición:** Si `remaining < 100`, generar alerta tipo `WARNING`.

---

#### **4.3 - Récord de Ventas**

```sql
-- Ventas de esta semana
SELECT COALESCE(SUM(total), 0) as thisWeekAmount
FROM invoice
WHERE
  company_id = :companyId
  AND issue_date >= DATE_TRUNC('week', CURRENT_DATE)
  AND authorization_status_sri = 'AUTORIZADA'
  AND status = true;

-- Ventas de semanas anteriores del mes (para comparar)
SELECT COALESCE(MAX(week_total), 0) as previousBestWeek
FROM (
  SELECT
    DATE_TRUNC('week', issue_date) as week,
    SUM(total) as week_total
  FROM invoice
  WHERE
    company_id = :companyId
    AND DATE_TRUNC('month', issue_date) = DATE_TRUNC('month', CURRENT_DATE)
    AND DATE_TRUNC('week', issue_date) < DATE_TRUNC('week', CURRENT_DATE)
    AND authorization_status_sri = 'AUTORIZADA'
    AND status = true
  GROUP BY DATE_TRUNC('week', issue_date)
) weekly_sales;
```

**Condición:** Si `thisWeekAmount > previousBestWeek`, generar alerta tipo `SUCCESS`.

---

## 🚀 **Optimizaciones Sugeridas**

### **1. Cache en Memoria (Redis)**

- **Key**: `welcome:dashboard:{companyId}`
- **TTL**: 5 minutos (300 segundos)
- **Invalidar cuando**:
  - Se crea una nueva factura
  - Se autoriza/rechaza una factura
  - Se actualiza un pago

### **2. Índices de Base de Datos**

```sql
-- Índice compuesto para mejorar queries de KPIs
CREATE INDEX idx_invoice_company_date_status
ON invoice(company_id, issue_date, authorization_status_sri, status);

-- Índice para búsqueda de facturas recientes
CREATE INDEX idx_invoice_company_date_desc
ON invoice(company_id, issue_date DESC);

-- Índice para pendientes de cobro
CREATE INDEX idx_invoice_payment_status
ON invoice(company_id, payment_status, authorization_status_sri);
```

### **3. Ejecución Paralela**

Ejecutar las 4 secciones en paralelo usando `CompletableFuture` o `@Async` (Spring):

- KPIs
- Tendencia
- Últimas Facturas
- Alertas

**Tiempo estimado de respuesta:**

- Sin cache: ~200-500ms
- Con cache: ~10-50ms

---

## 📊 **DTOs Backend (Spring Boot)**

### **WelcomeDashboardDto.java**

```java
package com.wallparisoft.ebilling.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class WelcomeDashboardDto {
    private KpiSummaryDto kpis;
    private List<SalesTrendItemDto> salesTrend;
    private TrendSummaryDto trendSummary;
    private List<RecentInvoiceDto> recentInvoices;
    private List<AlertDto> alerts;
}

@Data
class KpiSummaryDto {
    private KpiDto todaySales;
    private KpiDto weekSales;
    private KpiDto monthSales;
    private PendingCollectionDto pendingCollection;
}

@Data
class KpiDto {
    private BigDecimal amount;
    private Integer invoiceCount;
    private Double percentageChange;
    private Double goalPercentage;  // Opcional
}

@Data
class PendingCollectionDto {
    private BigDecimal amount;
    private Integer invoiceCount;
    private Integer overdueCount;
}

@Data
class SalesTrendItemDto {
    private LocalDate date;
    private String dayOfWeek;
    private BigDecimal amount;
    private Integer invoiceCount;
}

@Data
class TrendSummaryDto {
    private BigDecimal average;
    private BigDecimal total;
}

@Data
class RecentInvoiceDto {
    private Long idInvoice;
    private String invoiceNumber;
    private String clientName;
    private String clientIdentification;
    private BigDecimal totalAmount;
    private LocalDateTime issueDate;
    private String authorizationStatus;
    private String paymentStatus;
}

@Data
class AlertDto {
    private String id;
    private AlertType type;
    private AlertPriority priority;
    private String title;
    private String message;
    private Integer count;
    private String actionUrl;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;
}

enum AlertType {
    ERROR, WARNING, INFO, SUCCESS
}

enum AlertPriority {
    HIGH, MEDIUM, LOW
}
```

---

## 🎯 **Prioridad de Implementación**

| Componente           | Prioridad | Tiempo Estimado |
| -------------------- | --------- | --------------- |
| **KPIs Principales** | 🔴 Alta   | 3 horas         |
| **Últimas Facturas** | 🔴 Alta   | 1 hora          |
| **Tendencia 7 días** | 🟡 Media  | 2 horas         |
| **Alertas básicas**  | 🟡 Media  | 4 horas         |
| **Cache Redis**      | 🟢 Baja   | 2 horas         |
| **TOTAL**            |           | **12 horas**    |

---

## ✅ **Criterios de Aceptación**

1. ✅ El endpoint responde en **menos de 500ms** sin cache
2. ✅ El endpoint responde en **menos de 50ms** con cache
3. ✅ Todos los KPIs muestran datos precisos
4. ✅ Las alertas se generan automáticamente según las condiciones
5. ✅ Los datos de tendencia incluyen los últimos 7 días completos
6. ✅ El endpoint maneja correctamente empresas sin datos (retorna ceros)

---

## 🔄 **Alternativa Temporal (Si no hay recursos)**

Si el backend no puede implementar este endpoint de inmediato, el frontend puede:

1. **Usar endpoints existentes** con múltiples llamadas:

   - `GET /api/report/dashboard/sales` → Para KPIs y tendencia
   - `GET /api/report/pending-invoices` → Para pendientes
   - `POST /api/invoice/query` → Para últimas facturas
   - **Desventaja**: 3-4 requests HTTP, más lento

2. **Mock data** mientras se desarrolla:
   - Frontend funciona con datos de demostración
   - Se conecta al backend cuando esté listo

---

## 📝 **Notas Adicionales**

- Este endpoint es **específico para la pantalla Welcome**
- **NO reemplaza** el dashboard de ventas completo (`/api/report/dashboard/sales`)
- Es un **resumen ejecutivo** optimizado para carga rápida
- Puede ser la **primera llamada** que hace el usuario al iniciar sesión

---

## 🚀 **Siguiente Paso**

Una vez que este documento esté aprobado:

1. Backend implementa el endpoint `/api/dashboard/welcome`
2. Frontend actualiza el servicio para consumirlo
3. Se prueba en ambiente de desarrollo
4. Se documenta en Swagger/OpenAPI

---

**Documento creado**: 13 de Diciembre, 2025  
**Autor**: Frontend Team (Welcome Page Implementation)  
**Versión**: 1.0
