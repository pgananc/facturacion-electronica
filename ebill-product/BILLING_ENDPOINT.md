# 🧾 Endpoint de Facturación - Búsqueda de Productos

## 📋 Especificación Completa

### Endpoint

```http
GET /api/product/billing/search
```

### Query Parameters

| Parámetro   | Tipo   | Requerido | Descripción                                        |
| ----------- | ------ | --------- | -------------------------------------------------- |
| `idCompany` | Long   | ✅ **SÍ** | ID de la compañía                                  |
| `term`      | String | ❌ NO     | Término de búsqueda (código, nombre o descripción) |

---

## 🎯 Comportamiento

### Filtros Aplicados Automáticamente

1. ✅ **status = true** (solo productos activos)
2. ✅ **idCompany = :idCompany** (solo de la compañía especificada)
3. ✅ **Ordenamiento por mainCode ASC**

### Lógica de Búsqueda

#### Si `term` está **vacío o null**:

- Retorna **TODOS** los productos activos de la compañía
- Aplicando los filtros automáticos mencionados arriba

#### Si `term` tiene **valor**:

- Busca en **3 campos** (case-insensitive):
  - `mainCode` (código principal)
  - `name` (nombre del producto)
  - `description` (descripción)
- Retorna productos que coincidan en **cualquiera** de estos campos

---

## 📝 Ejemplos de Uso

### 1. Buscar por Término Específico

```bash
GET /api/product/billing/search?idCompany=1&term=laptop
```

**Resultado:**

- Productos activos de la compañía 1
- Que contengan "laptop" en código, nombre o descripción
- Ordenados por mainCode

```bash
curl -X GET "http://localhost:8380/api/product/billing/search?idCompany=1&term=laptop" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### 2. Obtener TODOS los Productos Activos

```bash
GET /api/product/billing/search?idCompany=1
```

**Resultado:**

- **TODOS** los productos activos de la compañía 1
- Sin filtro de búsqueda
- Ordenados por mainCode

```bash
curl -X GET "http://localhost:8380/api/product/billing/search?idCompany=1" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### 3. Con Término Vacío (Equivalente a Sin Término)

```bash
GET /api/product/billing/search?idCompany=1&term=
```

**Resultado:**

- Igual al ejemplo anterior
- **TODOS** los productos activos de la compañía 1

```bash
curl -X GET "http://localhost:8380/api/product/billing/search?idCompany=1&term=" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## 📦 Respuesta Exitosa

### Estructura

```json
{
  "code": 200,
  "status": "OK",
  "productDtos": [
    {
      "idProduct": 1,
      "idCompany": 1,
      "mainCode": "LAPTOP001",
      "name": "Laptop HP Pavilion 15",
      "productType": 1,
      "unitPrice": 1200.5,
      "description": "Laptop HP 15 pulgadas, 8GB RAM, 256GB SSD",
      "ice": false,
      "tourismIva": false,
      "status": true,
      "taxType": {
        "idTaxType": 2,
        "type": "IVA",
        "name": "IVA 12%",
        "value": 12.0,
        "status": true
      },
      "creationDate": "2025-01-15T10:30:00",
      "updateDate": "2025-01-20T15:45:00"
    },
    {
      "idProduct": 15,
      "idCompany": 1,
      "mainCode": "LAPTOP002",
      "name": "Laptop Dell Inspiron",
      "productType": 1,
      "unitPrice": 1350.0,
      "description": "Laptop Dell 14 pulgadas",
      "ice": false,
      "tourismIva": false,
      "status": true,
      "taxType": {
        "idTaxType": 2,
        "type": "IVA",
        "name": "IVA 12%",
        "value": 12.0,
        "status": true
      }
    }
  ]
}
```

---

## 🔒 Autenticación

**Requiere JWT Token** en el header:

```http
Authorization: Bearer YOUR_JWT_TOKEN
```

---

## ⚠️ Validaciones

### Error si `idCompany` no se proporciona

```bash
# ❌ Esto dará error
GET /api/product/billing/search?term=laptop
```

**Respuesta:**

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Required request parameter 'idCompany' is not present"
}
```

### Solución: Siempre incluir `idCompany`

```bash
# ✅ Correcto
GET /api/product/billing/search?idCompany=1&term=laptop
```

---

## 🔍 Casos de Uso

### Caso 1: Autocompletado en Frontend

```javascript
// Buscar mientras el usuario escribe
searchProducts(term: string) {
  return this.http.get(
    `${API_URL}/product/billing/search?idCompany=${this.companyId}&term=${term}`
  );
}
```

### Caso 2: Lista Completa al Cargar Factura

```javascript
// Cargar todos los productos disponibles
loadAllProducts() {
  return this.http.get(
    `${API_URL}/product/billing/search?idCompany=${this.companyId}`
  );
}
```

### Caso 3: Búsqueda por Código de Barras

```javascript
// Buscar producto específico por código
searchByCode(barcode: string) {
  return this.http.get(
    `${API_URL}/product/billing/search?idCompany=${this.companyId}&term=${barcode}`
  );
}
```

---

## 🎓 Diferencias con Otros Endpoints

| Característica       | `/billing/search` | `/search`    | `/company/{id}` |
| -------------------- | ----------------- | ------------ | --------------- |
| Requiere idCompany   | ✅ Query param    | ❌ No        | ✅ Path param   |
| Búsqueda por término | ✅ 3 campos       | ✅ Básico    | ❌ No           |
| Solo activos         | ✅ Siempre        | ✅ Siempre   | ✅ Siempre      |
| Ordenamiento         | mainCode ASC      | mainCode ASC | mainCode ASC    |
| Uso principal        | **Facturación**   | General      | Listado simple  |

---

## 💡 Ventajas de Este Endpoint

1. **Optimizado para Facturación**: Filtros específicos para el proceso de facturación
2. **Flexible**: Puede retornar todos los productos o buscar específicamente
3. **Eficiente**: Query optimizada con índices en los campos de búsqueda
4. **Completo**: Incluye toda la información necesaria (tax type, precios, etc.)
5. **Ordenado**: Siempre retorna resultados ordenados para mejor UX

---

## 🔧 Implementación Técnica

### Repository Query (JPQL)

```java
@Query("""
  SELECT p FROM Product p
  WHERE p.idCompany = :idCompany
    AND p.status = true
    AND (:term IS NULL OR :term = ''
         OR UPPER(p.mainCode) LIKE UPPER(CONCAT('%', :term, '%'))
         OR UPPER(p.name) LIKE UPPER(CONCAT('%', :term, '%'))
         OR UPPER(p.description) LIKE UPPER(CONCAT('%', :term, '%')))
  ORDER BY p.mainCode ASC
""")
List<Product> searchProductsForBilling(
    @Param("idCompany") Long idCompany,
    @Param("term") String term);
```

### Características de la Query:

- ✅ Case-insensitive (UPPER)
- ✅ Búsqueda parcial (LIKE con %)
- ✅ Maneja term null o vacío
- ✅ Ordenamiento consistente

---

## 📊 Performance

### Índices Recomendados (si no existen)

```sql
-- Para mejorar búsquedas por compañía y estado
CREATE INDEX idx_product_company_status
ON product(id_company, status);

-- Para mejorar búsquedas por código
CREATE INDEX idx_product_main_code
ON product(main_code);

-- Para mejorar ordenamiento
CREATE INDEX idx_product_main_code_asc
ON product(main_code ASC) WHERE status = true;
```

---

## 🧪 Testing

### Casos de Prueba

```bash
# 1. Búsqueda exitosa con resultado
GET /api/product/billing/search?idCompany=1&term=laptop
Expected: 200 OK con productos

# 2. Búsqueda sin resultados
GET /api/product/billing/search?idCompany=1&term=xxxxx
Expected: 200 OK con array vacío

# 3. Obtener todos
GET /api/product/billing/search?idCompany=1
Expected: 200 OK con todos los productos activos

# 4. Sin idCompany
GET /api/product/billing/search?term=laptop
Expected: 400 Bad Request

# 5. Compañía sin productos
GET /api/product/billing/search?idCompany=999999
Expected: 200 OK con array vacío

# 6. Term con caracteres especiales
GET /api/product/billing/search?idCompany=1&term=50%
Expected: 200 OK (búsqueda funcional)
```

---

## 📚 Documentación Relacionada

- [API_ENDPOINTS.md](API_ENDPOINTS.md) - Documentación completa de todos los endpoints
- [api-examples.http](api-examples.http) - Ejemplos para REST Client
- [README.md](README.md) - Documentación general del proyecto

---

**Creado:** 2025-10-13  
**Versión:** 1.0.0  
**Autor:** Team eBill
