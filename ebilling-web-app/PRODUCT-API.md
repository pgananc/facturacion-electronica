# 📚 API Endpoints - Product Microservice

## Base URL

```
http://localhost:8380/api/product
```

---

## 📋 Endpoints Disponibles

### ✅ 1. Obtener Producto por ID

```http
GET /api/product/id/{idProduct}
```

**Parámetros:**

- `idProduct` (path, Long) - ID del producto

**Respuesta:**

- `200 OK` - ProductResponse con el producto

**Ejemplo:**

```bash
curl -X GET "http://localhost:8380/api/product/id/1" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

### ✅ 2. Obtener Productos por Compañía

```http
GET /api/product/company/{idCompany}
```

**Parámetros:**

- `idCompany` (path, Long) - ID de la compañía

**Respuesta:**

- `200 OK` - Lista de productos activos de la compañía ordenados por código

**Ejemplo:**

```bash
curl -X GET "http://localhost:8380/api/product/company/1" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

### ✅ 3. Obtener Productos por Estado

```http
GET /api/product/products-status/{status}
```

**Parámetros:**

- `status` (path, boolean) - `true` para activos, `false` para inactivos

**Respuesta:**

- `200 OK` - Lista de productos con el estado especificado

**Ejemplo:**

```bash
curl -X GET "http://localhost:8380/api/product/products-status/true" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

### ✅ 4. Buscar Productos

```http
GET /api/product/search?term={searchTerm}
```

**Parámetros:**

- `term` (query, String, opcional) - Término de búsqueda

**Respuesta:**

- `200 OK` - Lista de productos que coinciden con el término de búsqueda

**Ejemplo:**

```bash
curl -X GET "http://localhost:8380/api/product/search?term=laptop" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

### ✅ 5. Buscar Productos para Facturación (Billing)

```http
GET /api/product/billing/search?idCompany={id}&term={searchTerm}
```

**Parámetros Query:**

- `idCompany` (Long, **requerido**) - ID de la compañía
- `term` (String, opcional) - Término de búsqueda

**Comportamiento:**

- ✅ **Solo productos activos** (`status = true`)
- ✅ **Solo de la compañía especificada** (`idCompany`)
- ✅ **Ordenados por código principal** (`mainCode ASC`)
- ✅ Si `term` está **vacío o null**: retorna TODOS los productos activos de la compañía
- ✅ Si `term` tiene valor: busca en `mainCode`, `name` o `description` (case-insensitive)

**Respuesta:**

- `200 OK` - ProductResponse con lista de productos

**Ejemplos:**

```bash
# Buscar por término específico
curl -X GET "http://localhost:8380/api/product/billing/search?idCompany=1&term=laptop" \
  -H "Authorization: Bearer YOUR_TOKEN"

# Obtener TODOS los productos activos de la compañía
curl -X GET "http://localhost:8380/api/product/billing/search?idCompany=1" \
  -H "Authorization: Bearer YOUR_TOKEN"

# También funciona con term vacío
curl -X GET "http://localhost:8380/api/product/billing/search?idCompany=1&term=" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Ejemplo de Respuesta:**

```json
{
  "code": 200,
  "status": "OK",
  "productDtos": [
    {
      "idProduct": 1,
      "mainCode": "PROD001",
      "name": "Laptop HP",
      "unitPrice": 1200.5,
      "status": true,
      "taxType": {
        "idTaxType": 2,
        "type": "IVA",
        "name": "IVA 12%",
        "value": 12.0
      }
    },
    {
      "idProduct": 2,
      "mainCode": "PROD002",
      "name": "Laptop Dell",
      "unitPrice": 1350.0,
      "status": true
    }
  ]
}
```

---

### ✅ 6. Listar Productos con Paginación y Filtros

```http
GET /api/product/pageable?page=0&size=10&sort=name,asc
```

**Parámetros Query (todos opcionales):**

- `page` (int) - Número de página (default: 0)
- `size` (int) - Tamaño de página (default: 20)
- `sort` (String) - Campo y dirección de ordenamiento (ej: `name,asc`)
- `idCompany` (Long) - Filtrar por compañía
- `mainCode` (String) - Filtrar por código principal
- `name` (String) - Filtrar por nombre
- `productType` (Integer) - Filtrar por tipo de producto
- `status` (Boolean) - Filtrar por estado

**Respuesta:**

- `200 OK` - Page<ProductDto> con los productos paginados

**Ejemplo:**

```bash
curl -X GET "http://localhost:8380/api/product/pageable?page=0&size=10&idCompany=1&status=true" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

### ✅ 7. Verificar si Existe un Código Principal

```http
GET /api/product/exists/{mainCode}
```

**Parámetros:**

- `mainCode` (path, String) - Código principal a verificar

**Respuesta:**

- `200 OK` - Boolean (true si existe, false si no)

**Ejemplo:**

```bash
curl -X GET "http://localhost:8380/api/product/exists/PROD001" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

### ✅ 8. Crear Producto

```http
POST /api/product
```

**Body (JSON):**

```json
{
  "idCompany": 1,
  "mainCode": "PROD001",
  "name": "Laptop HP",
  "productType": 1,
  "unitPrice": 1200.5,
  "description": "Laptop HP 15 pulgadas",
  "ice": false,
  "tourismIva": false,
  "status": true,
  "taxType": {
    "idTaxType": 2
  }
}
```

**Respuesta:**

- `200 OK` - ProductResponse con el producto creado

**Ejemplo:**

```bash
curl -X POST "http://localhost:8380/api/product" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "idCompany": 1,
    "mainCode": "PROD001",
    "name": "Laptop HP",
    "productType": 1,
    "unitPrice": 1200.50,
    "status": true
  }'
```

---

### ✅ 9. Actualizar Producto

```http
PATCH /api/product/{idProduct}
```

**Parámetros:**

- `idProduct` (path, Long) - ID del producto a actualizar

**Body (JSON):**

```json
{
  "name": "Laptop HP Actualizada",
  "mainCode": "PROD001",
  "description": "Nueva descripción",
  "unitPrice": 1350.0,
  "status": true,
  "productType": 1
}
```

**Respuesta:**

- `200 OK` - ProductResponse con el producto actualizado

**Ejemplo:**

```bash
curl -X PATCH "http://localhost:8380/api/product/1" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Laptop HP Actualizada",
    "unitPrice": 1350.00,
    "status": true
  }'
```

---

### ✅ 10. Eliminar Producto (Soft Delete)

```http
DELETE /api/product/{idProduct}
```

**Parámetros:**

- `idProduct` (path, Long) - ID del producto a eliminar

**Respuesta:**

- `200 OK` - ProductResponse con el producto marcado como inactivo

**Ejemplo:**

```bash
curl -X DELETE "http://localhost:8380/api/product/1" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

## 🔒 Autenticación

Todos los endpoints requieren autenticación JWT mediante el header `Authorization`:

```http
Authorization: Bearer YOUR_JWT_TOKEN_HERE
```

Para obtener el token, debes autenticarte previamente en el microservicio de autenticación.

---

## 📊 Códigos de Respuesta

| Código | Descripción                                 |
| ------ | ------------------------------------------- |
| 200    | OK - Petición exitosa                       |
| 201    | Created - Recurso creado exitosamente       |
| 400    | Bad Request - Datos de entrada inválidos    |
| 401    | Unauthorized - Token JWT inválido o ausente |
| 404    | Not Found - Recurso no encontrado           |
| 500    | Internal Server Error - Error del servidor  |

---

## 📝 Modelos de Datos

### ProductDto

```json
{
  "idProduct": 1,
  "idCompany": 1,
  "mainCode": "PROD001",
  "name": "Laptop HP",
  "productType": 1,
  "unitPrice": 1200.5,
  "description": "Laptop HP 15 pulgadas",
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
}
```

### ProductResponse

```json
{
  "code": 200,
  "status": "OK",
  "productDto": {
    /* ProductDto object */
  },
  "productDtos": [
    /* Array of ProductDto */
  ]
}
```

---

## 🎯 Tipos de Producto (productType)

| Valor | Descripción |
| ----- | ----------- |
| 1     | Bien        |
| 2     | Servicio    |

---

## ⚠️ Notas Importantes

1. **Soft Delete**: El endpoint DELETE no elimina físicamente el registro, solo cambia `status` a `false`
2. **Paginación**: Por defecto, `/pageable` retorna 20 elementos por página
3. **Búsqueda para Billing**: El endpoint `/billing/search` está optimizado para la facturación, filtrando automáticamente por compañía y estado activo
4. **Validaciones**: Todos los campos requeridos deben ser enviados en POST/PATCH

---

## 🔗 Enlaces Relacionados

- [README.md](README.md) - Documentación general del proyecto
- [BILLING-IMPROVEMENTS.md](BILLING-IMPROVEMENTS.md) - Mejoras en el módulo de facturación

---

**Última actualización:** 2025-10-14  
**Versión del API:** 1.0.0
