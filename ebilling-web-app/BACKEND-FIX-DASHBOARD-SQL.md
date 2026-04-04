# 🔧 Fix: Error SQL en Dashboard de Bienvenida

## ⚠️ **PROBLEMA:**

**Error 500 en**: `GET /api/dashboard/welcome?companyId=1`

**Stack Trace:**

```
ERROR: column i.total does not exist
Position: 21

Query problemático:
SELECT COALESCE(SUM(i.total), 0) as amount, COUNT(*) as invoiceCount
FROM invoice i
INNER JOIN authorization_sri a ON i.id_authorization_sri = a.id_authorization_sri
WHERE i.id_company = ?
AND DATE(i.issue_date) = CURRENT_DATE
AND a.authorization_status_sri = 'AUTORIZADO'
AND i.status = true
```

**Ubicación del error:**

- **Servicio**: ebill-billing (puerto 8480)
- **Clase**: `DashboardServiceImpl.java` (línea 87)
- **Método**: `calculateTodaySales()`

---

## 🎯 **CAUSA:**

La columna **`i.total`** no existe en la tabla `invoice` de PostgreSQL.

---

## ✅ **SOLUCIÓN:**

### **1. Identificar el nombre correcto de la columna:**

Ejecutar en PostgreSQL:

```sql
-- Ver estructura completa de la tabla
\d invoice;

-- O buscar columnas que contengan "total"
SELECT column_name, data_type
FROM information_schema.columns
WHERE table_name = 'invoice'
AND column_name LIKE '%total%';
```

**Posibles nombres correctos:**

- `total_amount`
- `totalAmount`
- `amount`
- `invoice_total`
- `total_invoice`

---

### **2. Corregir el query en el Repository:**

**Archivo**: `InvoiceRepository.java` o donde esté definido el query nativo.

**Cambio necesario:**

```java
// ❌ INCORRECTO (actual)
@Query(value = """
    SELECT COALESCE(SUM(i.total), 0) as amount,
           COUNT(*) as invoiceCount
    FROM invoice i
    INNER JOIN authorization_sri a ON i.id_authorization_sri = a.id_authorization_sri
    WHERE i.id_company = :companyId
    AND DATE(i.issue_date) = CURRENT_DATE
    AND a.authorization_status_sri = 'AUTORIZADO'
    AND i.status = true
    """, nativeQuery = true)
List<Object[]> getTodaySales(@Param("companyId") Long companyId);

// ✅ CORRECTO (ajustar según el nombre real de la columna)
@Query(value = """
    SELECT COALESCE(SUM(i.total_amount), 0) as amount,
           COUNT(*) as invoiceCount
    FROM invoice i
    INNER JOIN authorization_sri a ON i.id_authorization_sri = a.id_authorization_sri
    WHERE i.id_company = :companyId
    AND DATE(i.issue_date) = CURRENT_DATE
    AND a.authorization_status_sri = 'AUTORIZADO'
    AND i.status = true
    """, nativeQuery = true)
List<Object[]> getTodaySales(@Param("companyId") Long companyId);
```

**Nota**: Reemplazar `i.total_amount` con el nombre correcto de la columna según tu esquema.

---

### **3. Verificar otros queries con el mismo problema:**

Buscar en el código todos los queries que usen `i.total`:

```bash
# Buscar en el proyecto ebill-billing
grep -r "i\.total" --include="*.java" .
```

**Probablemente también hay que corregir:**

- `getWeekSales()`
- `getMonthSales()`
- `getPendingCollection()`
- `getSalesTrend()`
- Cualquier otro query relacionado con facturas

---

## 📝 **QUERIES QUE NECESITAN CORRECCIÓN:**

### **1. Today Sales**

```sql
-- Cambiar: i.total
-- Por: i.total_amount (o el nombre correcto)
SELECT COALESCE(SUM(i.total_amount), 0) as amount, COUNT(*) as invoiceCount
FROM invoice i
...
```

### **2. Week Sales**

```sql
SELECT COALESCE(SUM(i.total_amount), 0) as amount, COUNT(*) as invoiceCount
FROM invoice i
WHERE i.id_company = ?
AND i.issue_date >= CURRENT_DATE - INTERVAL '7 days'
...
```

### **3. Month Sales**

```sql
SELECT COALESCE(SUM(i.total_amount), 0) as amount, COUNT(*) as invoiceCount
FROM invoice i
WHERE i.id_company = ?
AND EXTRACT(MONTH FROM i.issue_date) = EXTRACT(MONTH FROM CURRENT_DATE)
AND EXTRACT(YEAR FROM i.issue_date) = EXTRACT(YEAR FROM CURRENT_DATE)
...
```

### **4. Pending Collection**

```sql
SELECT COALESCE(SUM(i.total_amount), 0) as amount, COUNT(*) as invoiceCount
FROM invoice i
WHERE i.id_company = ?
AND i.payment_status = 'PENDIENTE'
...
```

### **5. Sales Trend (últimos 7 días)**

```sql
SELECT DATE(i.issue_date) as date,
       COALESCE(SUM(i.total_amount), 0) as total,
       COUNT(*) as invoiceCount
FROM invoice i
INNER JOIN authorization_sri a ON i.id_authorization_sri = a.id_authorization_sri
WHERE i.id_company = ?
AND i.issue_date >= CURRENT_DATE - INTERVAL '7 days'
AND a.authorization_status_sri = 'AUTORIZADO'
AND i.status = true
GROUP BY DATE(i.issue_date)
ORDER BY date DESC
```

---

## 🧪 **TESTING:**

### **Después de corregir, probar:**

```bash
# 1. Reiniciar ebill-billing
# 2. Hacer request desde frontend o curl:

curl -X GET 'http://localhost:8480/api/dashboard/welcome?companyId=1' \
  -H 'Authorization: Bearer YOUR_TOKEN'

# 3. Verificar respuesta exitosa:
{
  "status": "success",
  "message": "Dashboard generado correctamente",
  "data": {
    "kpis": {
      "todaySales": {
        "amount": 123.45,
        "invoiceCount": 5
      },
      ...
    }
  }
}
```

---

## 📋 **CHECKLIST:**

- [ ] Identificar nombre correcto de columna en BD (`\d invoice`)
- [ ] Corregir query en `getTodaySales()`
- [ ] Corregir query en `getWeekSales()`
- [ ] Corregir query en `getMonthSales()`
- [ ] Corregir query en `getPendingCollection()`
- [ ] Corregir query en `getSalesTrend()`
- [ ] Corregir query en `getTopProducts()` (si usa `i.total`)
- [ ] Corregir query en `getBranchDistribution()` (si usa `i.total`)
- [ ] Testing con companyId=1
- [ ] Verificar que frontend recibe datos correctamente

---

## 🚀 **PRIORIDAD:**

🔴 **ALTA** - El Welcome Dashboard no funciona sin este fix.

---

**Fecha**: 13 de Diciembre, 2025  
**Servicio**: ebill-billing (puerto 8480)  
**Equipo**: Backend Team  
**Reportado por**: Frontend Team  
**Versión**: 1.0
