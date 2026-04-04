# 🗺️ Hoja de Ruta Backend - Dashboard de Ventas

## 📋 Resumen Ejecutivo

Este documento especifica los requerimientos backend para soportar el **Dashboard de Ventas** implementado en el frontend. El dashboard requiere datos agregados de ventas, tendencias temporales, productos más vendidos y distribución por sucursales.

**Estado Actual**: ✅ Frontend completado con datos de demostración  
**Prioridad**: 🔴 Alta - Dashboard es funcionalidad crítica de reportería  
**Tiempo Estimado**: 5-8 días de desarrollo backend

---

## 🎯 Objetivos del Backend

1. **Proveer KPIs de ventas** agregados por período (hoy, semana, mes)
2. **Calcular crecimiento mensual** comparado con mes anterior
3. **Generar serie temporal de ventas** para gráfico de tendencias (últimos 30 días)
4. **Identificar top productos** más vendidos en el período
5. **Calcular distribución de ventas** por sucursal/establecimiento

---

## 📊 1. Endpoint Principal: Dashboard de Ventas

### **GET `/api/reports/sales-dashboard`**

**Descripción**: Retorna todos los datos necesarios para el dashboard de ventas.

### Request Parameters (Query String)

| Parámetro   | Tipo     | Requerido | Descripción                                    | Ejemplo        |
|-------------|----------|-----------|------------------------------------------------|----------------|
| `dateFrom`  | `string` | ✅ Sí     | Fecha inicio (formato ISO: YYYY-MM-DD)         | `2024-01-01`   |
| `dateTo`    | `string` | ✅ Sí     | Fecha fin (formato ISO: YYYY-MM-DD)            | `2024-12-31`   |
| `companyId` | `number` | ✅ Sí     | ID de la empresa                               | `1`            |
| `branchId`  | `number` | ❌ No     | ID del establecimiento (null = todas)          | `5`            |

### Response Body (JSON)

```json
{
  "status": "OK",
  "message": "Dashboard cargado exitosamente",
  "data": {
    // ==========================================
    // 1. KPIs PRINCIPALES
    // ==========================================
    "todayTotal": 5250.75,
    "weekTotal": 28450.50,
    "monthTotal": 125800.25,
    "previousMonthTotal": 110500.00,
    
    // ==========================================
    // 2. TENDENCIA DE VENTAS (últimos 30 días)
    // ==========================================
    "salesTrend": [
      {
        "date": "2024-11-13",
        "total": 4800.50,
        "invoiceCount": 12
      },
      {
        "date": "2024-11-14",
        "total": 5200.75,
        "invoiceCount": 15
      },
      // ... 28 días más hasta llegar a 30 días
      {
        "date": "2024-12-12",
        "total": 5250.75,
        "invoiceCount": 14
      }
    ],
    
    // ==========================================
    // 3. TOP 10 PRODUCTOS MÁS VENDIDOS
    // ==========================================
    "topProducts": [
      {
        "productId": 101,
        "productCode": "PROD-001",
        "productName": "Laptop Dell Inspiron 15",
        "totalSales": 12500.00,
        "quantitySold": 25,
        "invoiceCount": 20
      },
      {
        "productId": 205,
        "productCode": "PROD-045",
        "productName": "Mouse Inalámbrico Logitech",
        "totalSales": 10800.50,
        "quantitySold": 180,
        "invoiceCount": 85
      },
      // ... hasta 10 productos
    ],
    
    // ==========================================
    // 4. DISTRIBUCIÓN POR SUCURSAL
    // ==========================================
    "branchDistribution": [
      {
        "branchId": 1,
        "branchCode": "001",
        "branchName": "Sucursal Centro",
        "totalSales": 35000.00,
        "invoiceCount": 120,
        "percentage": 29.5
      },
      {
        "branchId": 2,
        "branchCode": "002",
        "branchName": "Sucursal Norte",
        "totalSales": 28000.00,
        "invoiceCount": 95,
        "percentage": 23.6
      },
      // ... todas las sucursales activas
    ],
    
    // ==========================================
    // 5. MÉTRICAS ADICIONALES (OPCIONAL)
    // ==========================================
    "additionalMetrics": {
      "averageTicket": 450.50,
      "totalInvoices": 280,
      "totalClients": 156,
      "totalProducts": 48
    }
  }
}
```

### Error Response

```json
{
  "status": "ERROR",
  "message": "No se pudo cargar el dashboard",
  "errors": [
    {
      "field": "dateFrom",
      "message": "Fecha desde no puede ser posterior a fecha hasta"
    }
  ]
}
```

---

## 🏗️ 2. Estructura de DTOs (Java)

### `SalesDashboardDto.java`

```java
package com.wallparisoft.ebilling.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesDashboardDto {
    
    // KPIs principales
    private BigDecimal todayTotal;
    private BigDecimal weekTotal;
    private BigDecimal monthTotal;
    private BigDecimal previousMonthTotal;
    
    // Tendencia de ventas
    private List<SalesTrendItemDto> salesTrend;
    
    // Top productos
    private List<TopProductDto> topProducts;
    
    // Distribución por sucursal
    private List<BranchDistributionDto> branchDistribution;
    
    // Métricas adicionales (opcional)
    private AdditionalMetricsDto additionalMetrics;
}
```

### `SalesTrendItemDto.java`

```java
package com.wallparisoft.ebilling.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesTrendItemDto {
    private LocalDate date;
    private BigDecimal total;
    private Integer invoiceCount;
}
```

### `TopProductDto.java`

```java
package com.wallparisoft.ebilling.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopProductDto {
    private Long productId;
    private String productCode;
    private String productName;
    private BigDecimal totalSales;
    private Integer quantitySold;
    private Integer invoiceCount;
}
```

### `BranchDistributionDto.java`

```java
package com.wallparisoft.ebilling.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchDistributionDto {
    private Long branchId;
    private String branchCode;
    private String branchName;
    private BigDecimal totalSales;
    private Integer invoiceCount;
    private BigDecimal percentage; // Calculado automáticamente
}
```

### `AdditionalMetricsDto.java`

```java
package com.wallparisoft.ebilling.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdditionalMetricsDto {
    private BigDecimal averageTicket;
    private Integer totalInvoices;
    private Integer totalClients;
    private Integer totalProducts;
}
```

---

## 💾 3. Lógica de Negocio (Service Layer)

### Consideraciones Importantes

#### 3.1 **Filtro de Facturas Autorizadas**
```java
// Solo incluir facturas con estado AUTORIZADA del SRI
WHERE i.authorizationStatus = 'AUTORIZADA'
  AND i.status = 'ACTIVE'
  AND i.companyId = :companyId
  AND i.issueDate BETWEEN :dateFrom AND :dateTo
```

#### 3.2 **Exclusión de Notas de Crédito**
- Las facturas con NC aplicadas deben **restar** del monto total
- O alternativamente, no incluir facturas que hayan sido totalmente revertidas

#### 3.3 **Cálculo de Períodos**
- **Hoy**: Ventas del día actual (00:00:00 - 23:59:59)
- **Semana**: Últimos 7 días completos desde hoy
- **Mes**: Del día 1 del mes actual hasta hoy
- **Mes Anterior**: Del día 1 al último día del mes anterior

#### 3.4 **Cálculo de Porcentajes**
```java
// Para distribución por sucursal
percentage = (branchTotal / grandTotal) * 100
```

#### 3.5 **Ordenamiento**
- **Top Productos**: Ordenar por `totalSales DESC`, limitar a 10
- **Sucursales**: Ordenar por `totalSales DESC`
- **Tendencia**: Ordenar por `date ASC`

---

## 📝 4. Queries SQL de Ejemplo (PostgreSQL/MySQL)

### 4.1 KPI: Ventas del Día

```sql
SELECT 
    COALESCE(SUM(i.total_amount), 0) AS today_total
FROM invoices i
WHERE i.company_id = :companyId
  AND i.authorization_status = 'AUTORIZADA'
  AND i.status = 'ACTIVE'
  AND DATE(i.issue_date) = CURRENT_DATE
  AND (:branchId IS NULL OR i.establishment_id = :branchId);
```

### 4.2 KPI: Ventas del Mes Actual

```sql
SELECT 
    COALESCE(SUM(i.total_amount), 0) AS month_total
FROM invoices i
WHERE i.company_id = :companyId
  AND i.authorization_status = 'AUTORIZADA'
  AND i.status = 'ACTIVE'
  AND DATE(i.issue_date) >= DATE_TRUNC('month', CURRENT_DATE)
  AND DATE(i.issue_date) <= CURRENT_DATE
  AND (:branchId IS NULL OR i.establishment_id = :branchId);
```

### 4.3 Tendencia de Ventas (últimos 30 días)

```sql
WITH date_series AS (
    SELECT generate_series(
        CURRENT_DATE - INTERVAL '29 days',
        CURRENT_DATE,
        '1 day'::interval
    )::date AS date
)
SELECT 
    ds.date,
    COALESCE(SUM(i.total_amount), 0) AS total,
    COALESCE(COUNT(i.id_invoice), 0) AS invoice_count
FROM date_series ds
LEFT JOIN invoices i 
    ON DATE(i.issue_date) = ds.date
    AND i.company_id = :companyId
    AND i.authorization_status = 'AUTORIZADA'
    AND i.status = 'ACTIVE'
    AND (:branchId IS NULL OR i.establishment_id = :branchId)
GROUP BY ds.date
ORDER BY ds.date ASC;
```

### 4.4 Top 10 Productos

```sql
SELECT 
    p.id_product AS product_id,
    p.code AS product_code,
    p.name AS product_name,
    SUM(id.quantity * id.unit_price) AS total_sales,
    SUM(id.quantity) AS quantity_sold,
    COUNT(DISTINCT i.id_invoice) AS invoice_count
FROM invoice_details id
INNER JOIN invoices i ON id.invoice_id = i.id_invoice
INNER JOIN products p ON id.product_id = p.id_product
WHERE i.company_id = :companyId
  AND i.authorization_status = 'AUTORIZADA'
  AND i.status = 'ACTIVE'
  AND i.issue_date BETWEEN :dateFrom AND :dateTo
  AND (:branchId IS NULL OR i.establishment_id = :branchId)
GROUP BY p.id_product, p.code, p.name
ORDER BY total_sales DESC
LIMIT 10;
```

### 4.5 Distribución por Sucursal

```sql
WITH branch_sales AS (
    SELECT 
        e.id_establishment AS branch_id,
        e.code AS branch_code,
        e.name AS branch_name,
        SUM(i.total_amount) AS total_sales,
        COUNT(i.id_invoice) AS invoice_count
    FROM invoices i
    INNER JOIN establishments e ON i.establishment_id = e.id_establishment
    WHERE i.company_id = :companyId
      AND i.authorization_status = 'AUTORIZADA'
      AND i.status = 'ACTIVE'
      AND i.issue_date BETWEEN :dateFrom AND :dateTo
    GROUP BY e.id_establishment, e.code, e.name
),
total_sales AS (
    SELECT SUM(total_sales) AS grand_total FROM branch_sales
)
SELECT 
    bs.branch_id,
    bs.branch_code,
    bs.branch_name,
    bs.total_sales,
    bs.invoice_count,
    ROUND((bs.total_sales / ts.grand_total * 100), 2) AS percentage
FROM branch_sales bs
CROSS JOIN total_sales ts
ORDER BY bs.total_sales DESC;
```

---

## 🔧 5. Implementación en Spring Boot

### 5.1 Controller

```java
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/sales-dashboard")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<SalesDashboardDto>> getSalesDashboard(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam Long companyId,
            @RequestParam(required = false) Long branchId
    ) {
        // Validaciones
        if (dateFrom.isAfter(dateTo)) {
            return ResponseEntity.badRequest().body(
                ApiResponse.<SalesDashboardDto>builder()
                    .status("ERROR")
                    .message("Fecha desde no puede ser posterior a fecha hasta")
                    .build()
            );
        }

        SalesDashboardDto dashboard = dashboardService.generateDashboard(
            dateFrom, dateTo, companyId, branchId
        );

        return ResponseEntity.ok(
            ApiResponse.<SalesDashboardDto>builder()
                .status("OK")
                .message("Dashboard cargado exitosamente")
                .data(dashboard)
                .build()
        );
    }
}
```

### 5.2 Service (Interfaz)

```java
public interface DashboardService {
    SalesDashboardDto generateDashboard(
        LocalDate dateFrom, 
        LocalDate dateTo, 
        Long companyId, 
        Long branchId
    );
}
```

### 5.3 Service (Implementación - Esqueleto)

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceDetailRepository invoiceDetailRepository;
    private final EstablishmentRepository establishmentRepository;

    @Override
    public SalesDashboardDto generateDashboard(
        LocalDate dateFrom, 
        LocalDate dateTo, 
        Long companyId, 
        Long branchId
    ) {
        log.info("Generando dashboard para companyId={}, branchId={}, desde={}, hasta={}", 
                 companyId, branchId, dateFrom, dateTo);

        return SalesDashboardDto.builder()
            .todayTotal(calculateTodayTotal(companyId, branchId))
            .weekTotal(calculateWeekTotal(companyId, branchId))
            .monthTotal(calculateMonthTotal(companyId, branchId))
            .previousMonthTotal(calculatePreviousMonthTotal(companyId, branchId))
            .salesTrend(generateSalesTrend(dateFrom, dateTo, companyId, branchId))
            .topProducts(getTopProducts(dateFrom, dateTo, companyId, branchId))
            .branchDistribution(getBranchDistribution(dateFrom, dateTo, companyId))
            .additionalMetrics(calculateAdditionalMetrics(dateFrom, dateTo, companyId, branchId))
            .build();
    }

    private BigDecimal calculateTodayTotal(Long companyId, Long branchId) {
        // Implementar usando invoiceRepository
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateWeekTotal(Long companyId, Long branchId) {
        // Implementar
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateMonthTotal(Long companyId, Long branchId) {
        // Implementar
        return BigDecimal.ZERO;
    }

    private BigDecimal calculatePreviousMonthTotal(Long companyId, Long branchId) {
        // Implementar
        return BigDecimal.ZERO;
    }

    private List<SalesTrendItemDto> generateSalesTrend(
        LocalDate dateFrom, LocalDate dateTo, Long companyId, Long branchId
    ) {
        // Implementar query para últimos 30 días
        return Collections.emptyList();
    }

    private List<TopProductDto> getTopProducts(
        LocalDate dateFrom, LocalDate dateTo, Long companyId, Long branchId
    ) {
        // Implementar query para top 10 productos
        return Collections.emptyList();
    }

    private List<BranchDistributionDto> getBranchDistribution(
        LocalDate dateFrom, LocalDate dateTo, Long companyId
    ) {
        // Implementar query para distribución por sucursal
        return Collections.emptyList();
    }

    private AdditionalMetricsDto calculateAdditionalMetrics(
        LocalDate dateFrom, LocalDate dateTo, Long companyId, Long branchId
    ) {
        // Implementar métricas adicionales
        return null;
    }
}
```

---

## 🧪 6. Testing

### 6.1 Casos de Prueba Requeridos

| # | Caso de Prueba | Resultado Esperado |
|---|----------------|-------------------|
| 1 | Dashboard con datos válidos | Retorna dashboard completo |
| 2 | dateFrom > dateTo | Error 400 con mensaje claro |
| 3 | companyId no existe | Error 404 |
| 4 | branchId inválido | Error 404 |
| 5 | Sin facturas en período | Dashboard con valores en 0 |
| 6 | Solo facturas BORRADOR | Dashboard con valores en 0 |
| 7 | Facturas con NC aplicadas | Total descontando NCs |
| 8 | Filtro por sucursal específica | Solo datos de esa sucursal |
| 9 | Usuario sin permisos | Error 403 Forbidden |
| 10 | Período de 1 año completo | Performance aceptable (<3s) |

### 6.2 Test Unitario (Ejemplo)

```java
@Test
void testGenerateDashboard_ValidData_ReturnsCompleteData() {
    // Arrange
    LocalDate dateFrom = LocalDate.of(2024, 1, 1);
    LocalDate dateTo = LocalDate.of(2024, 12, 31);
    Long companyId = 1L;
    Long branchId = null;

    // Mock data
    when(invoiceRepository.calculateTodayTotal(companyId, branchId))
        .thenReturn(BigDecimal.valueOf(5250.75));

    // Act
    SalesDashboardDto result = dashboardService.generateDashboard(
        dateFrom, dateTo, companyId, branchId
    );

    // Assert
    assertNotNull(result);
    assertEquals(BigDecimal.valueOf(5250.75), result.getTodayTotal());
    assertNotNull(result.getSalesTrend());
    assertFalse(result.getSalesTrend().isEmpty());
}
```

---

## 📊 7. Optimizaciones de Performance

### 7.1 Índices Recomendados

```sql
-- Índice compuesto para búsquedas por compañía y fecha
CREATE INDEX idx_invoices_company_date 
ON invoices(company_id, issue_date, authorization_status, status);

-- Índice para filtro por establecimiento
CREATE INDEX idx_invoices_establishment 
ON invoices(establishment_id, issue_date);

-- Índice para detalles de factura
CREATE INDEX idx_invoice_details_product 
ON invoice_details(product_id, invoice_id);
```

### 7.2 Caching (Opcional)

```java
@Cacheable(value = "salesDashboard", 
           key = "#companyId + '_' + #dateFrom + '_' + #dateTo + '_' + #branchId")
public SalesDashboardDto generateDashboard(...) {
    // ...
}
```

**TTL Recomendado**: 15 minutos (900 segundos)

### 7.3 Query Optimization

- Usar `COALESCE(SUM(...), 0)` para evitar NULLs
- Limitar queries de tendencia a 30 días máximo
- Usar `LIMIT 10` para top productos
- Considerar materializar vistas para empresas grandes (>100k facturas)

---

## 📅 8. Plan de Implementación (Fases)

### **Fase 1: KPIs Básicos** (1-2 días)
- [ ] Implementar cálculo de `todayTotal`
- [ ] Implementar cálculo de `weekTotal`
- [ ] Implementar cálculo de `monthTotal`
- [ ] Implementar cálculo de `previousMonthTotal`
- [ ] Tests unitarios para KPIs

### **Fase 2: Tendencia de Ventas** (1 día)
- [ ] Implementar generación de serie temporal (30 días)
- [ ] Manejar días sin ventas (retornar 0)
- [ ] Tests unitarios para tendencia

### **Fase 3: Top Productos** (1 día)
- [ ] Query para top 10 productos
- [ ] Incluir cantidad vendida y número de facturas
- [ ] Tests unitarios

### **Fase 4: Distribución por Sucursal** (1 día)
- [ ] Query para todas las sucursales
- [ ] Cálculo de porcentajes
- [ ] Tests unitarios

### **Fase 5: Integración y Testing** (1-2 días)
- [ ] Integrar todos los componentes
- [ ] Tests de integración end-to-end
- [ ] Testing de performance
- [ ] Manejo de casos edge

### **Fase 6: Optimización** (1 día)
- [ ] Crear índices necesarios
- [ ] Implementar caching si es necesario
- [ ] Refactoring y limpieza de código

---

## 🚦 9. Criterios de Aceptación

### ✅ Checklist de Completitud

- [ ] Endpoint `/api/reports/sales-dashboard` implementado
- [ ] Todos los DTOs definidos y documentados
- [ ] KPIs calculados correctamente (hoy, semana, mes, mes anterior)
- [ ] Tendencia de ventas retorna exactamente 30 días
- [ ] Top 10 productos ordenados por ventas
- [ ] Distribución por sucursal con porcentajes correctos
- [ ] Solo facturas AUTORIZADAS son consideradas
- [ ] Filtro por sucursal funciona correctamente
- [ ] Validaciones de entrada implementadas
- [ ] Manejo de errores robusto
- [ ] Tests unitarios con >80% cobertura
- [ ] Tests de integración pasando
- [ ] Performance aceptable (<2s para períodos normales)
- [ ] Documentación Swagger/OpenAPI generada
- [ ] Code review completado
- [ ] Aprobado por QA

---

## 🔒 10. Consideraciones de Seguridad

### 10.1 Autorización
```java
// Usuario solo puede ver dashboard de su empresa
if (!currentUser.getCompanyId().equals(companyId)) {
    throw new ForbiddenException("No autorizado para ver esta empresa");
}

// SUPER_ADMIN puede ver cualquier empresa
```

### 10.2 Validación de Inputs
- Validar que `dateFrom` y `dateTo` no sean más de 1 año de diferencia
- Validar que `companyId` y `branchId` pertenezcan al usuario
- Sanitizar todos los parámetros de entrada

### 10.3 Rate Limiting
- Limitar a 10 requests por minuto por usuario
- Prevenir queries muy costosas

---

## 📞 11. Contacto y Coordinación

**Frontend Lead**: Jorge Guerrero  
**Backend Team**: [Completar con nombres del equipo]

### Dependencias
- ✅ Frontend completado y esperando datos del backend
- ⏳ Backend pendiente de implementación
- ⏳ Testing QA pendiente

### Próxima Reunión de Coordinación
**Fecha sugerida**: [Coordinar con el equipo]  
**Agenda**: 
- Review de esta especificación
- Estimación de tiempos por el backend team
- Definir sprint para implementación
- Acordar estructura de datos de prueba para frontend

---

## 📚 12. Referencias

- **Frontend Component**: `/src/app/pages/reports/sales-dashboard/`
- **Chart.js Docs**: https://www.chartjs.org/docs/
- **Formato de Fechas**: ISO 8601 (YYYY-MM-DD)
- **Estándar API**: REST + JSON
- **Autenticación**: JWT Bearer Token

---

## 📝 Notas Finales

- Este dashboard es la **página principal de reportería** para usuarios
- Los datos deben ser **precisos y auditables**
- Considerar **internacionalización** de mensajes en el futuro
- El frontend está listo para **actualizarse automáticamente** cuando cambien filtros
- Gráficos son **interactivos y responsivos**

---

**Documento Version**: 1.0  
**Fecha**: 12 de Diciembre, 2024  
**Autor**: Jorge Guerrero (Frontend Lead)  
**Última Actualización**: 12/12/2024

