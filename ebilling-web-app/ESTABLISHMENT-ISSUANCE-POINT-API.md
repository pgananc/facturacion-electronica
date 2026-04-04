# API REST - Gestión de Sucursales y Puntos de Emisión

## 📋 Resumen Ejecutivo

Este documento especifica los endpoints REST necesarios para la gestión completa de **Sucursales (Establishments)** y **Puntos de Emisión (Invoice Issuance Points)** en el sistema de facturación electrónica.

**Microservicio**: `ebill-customer`  
**Fecha**: Noviembre 2024  
**Versión**: 1.0

---

## 🏢 1. API de Sucursales (Establishments)

### Base URL

```
/api/establishment
```

---

### 1.1. Obtener todas las sucursales de una compañía

**Endpoint**: `GET /api/establishment/company/{idCompany}`

**Descripción**: Retorna todas las sucursales (activas e inactivas) de una compañía específica.

**Path Parameters**:
| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| idCompany | Long | ID de la compañía |

**Response 200 OK**:

```json
{
  "status": "success",
  "message": "Sucursales encontradas",
  "data": [
    {
      "idEstablishment": 1,
      "name": "Matriz Quito",
      "code": "001",
      "address": "Av. 6 de Diciembre N34-345 y Whimper",
      "idCompany": 1,
      "type": "MATRIZ",
      "status": true
    },
    {
      "idEstablishment": 2,
      "name": "Sucursal Guayaquil",
      "code": "002",
      "address": "Av. Francisco de Orellana Mz 111",
      "idCompany": 1,
      "type": "SUCURSAL",
      "status": true
    }
  ]
}
```

**Response 404 Not Found**:

```json
{
  "status": "error",
  "message": "No se encontraron sucursales para la compañía",
  "data": []
}
```

---

### 1.2. Obtener una sucursal por ID

**Endpoint**: `GET /api/establishment/{idEstablishment}`

**Descripción**: Retorna los datos de una sucursal específica.

**Path Parameters**:
| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| idEstablishment | Long | ID de la sucursal |

**Response 200 OK**:

```json
{
  "status": "success",
  "message": "Sucursal encontrada",
  "data": [
    {
      "idEstablishment": 1,
      "name": "Matriz Quito",
      "code": "001",
      "address": "Av. 6 de Diciembre N34-345 y Whimper",
      "idCompany": 1,
      "type": "MATRIZ",
      "status": true
    }
  ]
}
```

**Response 404 Not Found**:

```json
{
  "status": "error",
  "message": "Sucursal no encontrada",
  "data": []
}
```

---

### 1.3. Crear una nueva sucursal

**Endpoint**: `POST /api/establishment`

**Descripción**: Crea una nueva sucursal para una compañía.

**Request Body**:

```json
{
  "name": "Sucursal Norte",
  "code": "003",
  "address": "Av. Eloy Alfaro y Av. de los Shirys",
  "idCompany": 1,
  "type": "SUCURSAL",
  "status": true
}
```

**Validaciones**:

- `name`: requerido, máximo 100 caracteres
- `code`: requerido, exactamente 3 dígitos numéricos (001-999)
- `address`: requerido, máximo 200 caracteres
- `idCompany`: requerido, debe existir
- `type`: requerido, valores permitidos: "MATRIZ", "SUCURSAL"
- `status`: requerido, boolean
- **Validación única**: No puede existir otra sucursal con el mismo `code` para la misma `idCompany`

**Response 201 Created**:

```json
{
  "status": "success",
  "message": "Sucursal creada exitosamente",
  "data": [
    {
      "idEstablishment": 3,
      "name": "Sucursal Norte",
      "code": "003",
      "address": "Av. Eloy Alfaro y Av. de los Shirys",
      "idCompany": 1,
      "type": "SUCURSAL",
      "status": true
    }
  ]
}
```

**Response 400 Bad Request** (código duplicado):

```json
{
  "status": "error",
  "message": "Ya existe una sucursal con el código 003 para esta compañía",
  "data": []
}
```

**Response 400 Bad Request** (validación):

```json
{
  "status": "error",
  "message": "El código debe ser un número de 3 dígitos (001-999)",
  "data": []
}
```

---

### 1.4. Actualizar una sucursal existente

**Endpoint**: `PUT /api/establishment/{idEstablishment}`

**Descripción**: Actualiza los datos de una sucursal existente.

**Path Parameters**:
| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| idEstablishment | Long | ID de la sucursal a actualizar |

**Request Body**:

```json
{
  "name": "Matriz Quito - Centro Norte",
  "code": "001",
  "address": "Av. 6 de Diciembre N34-345 y Whimper, Edificio Torre Central",
  "idCompany": 1,
  "type": "MATRIZ",
  "status": true
}
```

**Validaciones**: Las mismas que en CREATE, más:

- El `idEstablishment` debe existir
- Si se cambia el `code`, validar que no exista otro con ese código en la misma compañía

**Response 200 OK**:

```json
{
  "status": "success",
  "message": "Sucursal actualizada exitosamente",
  "data": [
    {
      "idEstablishment": 1,
      "name": "Matriz Quito - Centro Norte",
      "code": "001",
      "address": "Av. 6 de Diciembre N34-345 y Whimper, Edificio Torre Central",
      "idCompany": 1,
      "type": "MATRIZ",
      "status": true
    }
  ]
}
```

**Response 404 Not Found**:

```json
{
  "status": "error",
  "message": "Sucursal no encontrada",
  "data": []
}
```

---

### 1.5. Eliminar una sucursal (lógicamente)

**Endpoint**: `DELETE /api/establishment/{idEstablishment}`

**Descripción**: Elimina una sucursal estableciendo `status = false` (eliminación lógica).

**Path Parameters**:
| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| idEstablishment | Long | ID de la sucursal a eliminar |

**Validaciones**:

- No se puede eliminar si tiene puntos de emisión activos asociados
- La sucursal debe existir

**Response 200 OK**:

```json
{
  "status": "success",
  "message": "Sucursal eliminada exitosamente",
  "data": []
}
```

**Response 400 Bad Request** (tiene puntos activos):

```json
{
  "status": "error",
  "message": "No se puede eliminar la sucursal porque tiene puntos de emisión activos asociados",
  "data": []
}
```

**Response 404 Not Found**:

```json
{
  "status": "error",
  "message": "Sucursal no encontrada",
  "data": []
}
```

---

### 1.6. Validar si existe código de sucursal

**Endpoint**: `GET /api/establishment/exists/{idCompany}/{code}`

**Descripción**: Valida si ya existe una sucursal con ese código para la compañía (útil para validaciones en tiempo real en el frontend).

**Path Parameters**:
| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| idCompany | Long | ID de la compañía |
| code | String | Código de sucursal (3 dígitos) |

**Response 200 OK**:

```json
true
```

o

```json
false
```

---

## 📍 2. API de Puntos de Emisión (Invoice Issuance Points)

### Base URL

```
/api/invoice-issuance-point
```

---

### 2.1. Obtener todos los puntos de emisión de una sucursal

**Endpoint**: `GET /api/invoice-issuance-point/establishment/{establishmentId}`

**Descripción**: Retorna todos los puntos de emisión (activos e inactivos) de una sucursal específica.

**Path Parameters**:
| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| establishmentId | Long | ID de la sucursal |

**Response 200 OK**:

```json
{
  "status": "success",
  "message": "Puntos de emisión encontrados",
  "data": [
    {
      "idInvoiceIssuancePoint": 1,
      "code": "001",
      "establishmentId": 1,
      "sequentialInvoice": 1523,
      "status": true
    },
    {
      "idInvoiceIssuancePoint": 2,
      "code": "002",
      "establishmentId": 1,
      "sequentialInvoice": 845,
      "status": true
    }
  ]
}
```

**Response 404 Not Found**:

```json
{
  "status": "error",
  "message": "No se encontraron puntos de emisión para la sucursal",
  "data": []
}
```

---

### 2.2. Obtener todos los puntos de emisión de una compañía

**Endpoint**: `GET /api/invoice-issuance-point/company/{idCompany}`

**Descripción**: Retorna todos los puntos de emisión de todas las sucursales de una compañía.

**Path Parameters**:
| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| idCompany | Long | ID de la compañía |

**Response 200 OK**:

```json
{
  "status": "success",
  "message": "Puntos de emisión encontrados",
  "data": [
    {
      "idInvoiceIssuancePoint": 1,
      "code": "001",
      "establishmentId": 1,
      "establishmentCode": "001",
      "establishmentName": "Matriz Quito",
      "sequentialInvoice": 1523,
      "status": true
    },
    {
      "idInvoiceIssuancePoint": 2,
      "code": "002",
      "establishmentId": 1,
      "establishmentCode": "001",
      "establishmentName": "Matriz Quito",
      "sequentialInvoice": 845,
      "status": true
    },
    {
      "idInvoiceIssuancePoint": 3,
      "code": "001",
      "establishmentId": 2,
      "establishmentCode": "002",
      "establishmentName": "Sucursal Guayaquil",
      "sequentialInvoice": 234,
      "status": true
    }
  ]
}
```

**Nota**: Este endpoint debe incluir información de la sucursal (`establishmentCode` y `establishmentName`) mediante un JOIN para mostrarla en la tabla del frontend.

---

### 2.3. Obtener un punto de emisión por ID

**Endpoint**: `GET /api/invoice-issuance-point/{idInvoiceIssuancePoint}`

**Descripción**: Retorna los datos de un punto de emisión específico.

**Path Parameters**:
| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| idInvoiceIssuancePoint | Long | ID del punto de emisión |

**Response 200 OK**:

```json
{
  "status": "success",
  "message": "Punto de emisión encontrado",
  "data": [
    {
      "idInvoiceIssuancePoint": 1,
      "code": "001",
      "establishmentId": 1,
      "sequentialInvoice": 1523,
      "status": true
    }
  ]
}
```

**Response 404 Not Found**:

```json
{
  "status": "error",
  "message": "Punto de emisión no encontrado",
  "data": []
}
```

---

### 2.4. Crear un nuevo punto de emisión

**Endpoint**: `POST /api/invoice-issuance-point`

**Descripción**: Crea un nuevo punto de emisión para una sucursal.

**Request Body**:

```json
{
  "code": "003",
  "establishmentId": 1,
  "sequentialInvoice": 1,
  "status": true
}
```

**Validaciones**:

- `code`: requerido, exactamente 3 dígitos numéricos (001-999)
- `establishmentId`: requerido, debe existir y estar activo
- `sequentialInvoice`: requerido, debe ser >= 1
- `status`: requerido, boolean
- **Validación única**: No puede existir otro punto de emisión con el mismo `code` para el mismo `establishmentId`

**Response 201 Created**:

```json
{
  "status": "success",
  "message": "Punto de emisión creado exitosamente",
  "data": [
    {
      "idInvoiceIssuancePoint": 5,
      "code": "003",
      "establishmentId": 1,
      "sequentialInvoice": 1,
      "status": true
    }
  ]
}
```

**Response 400 Bad Request** (código duplicado):

```json
{
  "status": "error",
  "message": "Ya existe un punto de emisión con el código 003 para esta sucursal",
  "data": []
}
```

**Response 400 Bad Request** (sucursal inactiva):

```json
{
  "status": "error",
  "message": "No se puede crear un punto de emisión en una sucursal inactiva",
  "data": []
}
```

---

### 2.5. Actualizar un punto de emisión existente

**Endpoint**: `PUT /api/invoice-issuance-point/{idInvoiceIssuancePoint}`

**Descripción**: Actualiza los datos de un punto de emisión existente.

**Path Parameters**:
| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| idInvoiceIssuancePoint | Long | ID del punto de emisión a actualizar |

**Request Body**:

```json
{
  "code": "003",
  "establishmentId": 1,
  "sequentialInvoice": 1523,
  "status": true
}
```

**Validaciones**: Las mismas que en CREATE, más:

- El `idInvoiceIssuancePoint` debe existir
- Si se cambia el `code`, validar que no exista otro con ese código en la misma sucursal
- **IMPORTANTE**: No se puede cambiar `establishmentId` si el punto ya fue usado para emitir facturas
- **IMPORTANTE**: `sequentialInvoice` solo puede aumentar, nunca disminuir (para mantener integridad de secuenciales)

**Response 200 OK**:

```json
{
  "status": "success",
  "message": "Punto de emisión actualizado exitosamente",
  "data": [
    {
      "idInvoiceIssuancePoint": 5,
      "code": "003",
      "establishmentId": 1,
      "sequentialInvoice": 1523,
      "status": true
    }
  ]
}
```

**Response 400 Bad Request** (secuencial menor):

```json
{
  "status": "error",
  "message": "El secuencial no puede ser menor al actual (1523)",
  "data": []
}
```

**Response 404 Not Found**:

```json
{
  "status": "error",
  "message": "Punto de emisión no encontrado",
  "data": []
}
```

---

### 2.6. Eliminar un punto de emisión (lógicamente)

**Endpoint**: `DELETE /api/invoice-issuance-point/{idInvoiceIssuancePoint}`

**Descripción**: Elimina un punto de emisión estableciendo `status = false` (eliminación lógica).

**Path Parameters**:
| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| idInvoiceIssuancePoint | Long | ID del punto de emisión a eliminar |

**Validaciones**:

- El punto de emisión debe existir
- No se puede eliminar si hay sesiones activas de usuarios usando este punto
- ⚠️ **CRÍTICO**: Validar si hay facturas emitidas con este punto. Si las hay, mostrar advertencia pero permitir eliminación (solo desactiva para futuras facturas)

**Response 200 OK**:

```json
{
  "status": "success",
  "message": "Punto de emisión eliminado exitosamente",
  "data": []
}
```

**Response 400 Bad Request** (sesión activa):

```json
{
  "status": "error",
  "message": "No se puede eliminar el punto de emisión porque tiene sesiones activas de usuarios",
  "data": []
}
```

**Response 404 Not Found**:

```json
{
  "status": "error",
  "message": "Punto de emisión no encontrado",
  "data": []
}
```

---

### 2.7. Validar si existe código de punto de emisión

**Endpoint**: `GET /api/invoice-issuance-point/exists/{establishmentId}/{code}`

**Descripción**: Valida si ya existe un punto de emisión con ese código para la sucursal.

**Path Parameters**:
| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| establishmentId | Long | ID de la sucursal |
| code | String | Código de punto de emisión (3 dígitos) |

**Response 200 OK**:

```json
true
```

o

```json
false
```

---

## 🗄️ 3. Modelo de Datos

### 3.1. Tabla `establishment`

```sql
CREATE TABLE establishment (
    id_establishment BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(3) NOT NULL,
    address VARCHAR(200) NOT NULL,
    id_company BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('MATRIZ', 'SUCURSAL')),
    status BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_establishment_code_company UNIQUE (code, id_company),
    CONSTRAINT fk_establishment_company FOREIGN KEY (id_company) REFERENCES company(id_company)
);

CREATE INDEX idx_establishment_company ON establishment(id_company);
CREATE INDEX idx_establishment_status ON establishment(status);
```

### 3.2. Tabla `invoice_issuance_point`

```sql
CREATE TABLE invoice_issuance_point (
    id_invoice_issuance_point BIGSERIAL PRIMARY KEY,
    code VARCHAR(3) NOT NULL,
    establishment_id BIGINT NOT NULL,
    sequential_invoice INTEGER NOT NULL DEFAULT 1,
    status BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_issuance_point_code_establishment UNIQUE (code, establishment_id),
    CONSTRAINT fk_issuance_point_establishment FOREIGN KEY (establishment_id) REFERENCES establishment(id_establishment),
    CONSTRAINT chk_sequential_positive CHECK (sequential_invoice >= 1)
);

CREATE INDEX idx_issuance_point_establishment ON invoice_issuance_point(establishment_id);
CREATE INDEX idx_issuance_point_status ON invoice_issuance_point(status);
```

---

## 🔐 4. Seguridad y Permisos

### Validaciones de Seguridad

1. **Todos los endpoints deben validar**:

   - Token JWT válido
   - Usuario autenticado tiene acceso a la compañía especificada
   - Usuario tiene rol adecuado (ADMIN o COMPANY_ADMIN)

2. **Reglas de negocio**:

   - Un usuario solo puede gestionar sucursales/puntos de las compañías a las que tiene acceso
   - Los códigos de sucursal y punto de emisión deben ser ÚNICOS dentro de su contexto (compañía/sucursal)
   - Los secuenciales de facturas solo pueden incrementar

3. **Auditoría**:
   - Registrar `created_at` y `updated_at` en todas las operaciones
   - Considerar tabla de auditoría para cambios en secuenciales

---

## 📝 5. Notas de Implementación

### Backend (Java Spring Boot)

**Estructura sugerida**:

```
ebill-customer/
├── controller/
│   ├── EstablishmentController.java
│   └── InvoiceIssuancePointController.java
├── service/
│   ├── EstablishmentService.java
│   └── InvoiceIssuancePointService.java
├── repository/
│   ├── EstablishmentRepository.java
│   └── InvoiceIssuancePointRepository.java
└── dto/
    ├── EstablishmentRequestDto.java
    ├── EstablishmentResponseDto.java
    ├── IssuancePointRequestDto.java
    └── IssuancePointResponseDto.java
```

### Validaciones importantes

1. **Códigos**: Validar con regex `^[0-9]{3}$` (exactamente 3 dígitos)
2. **Secuenciales**: No permitir decrementos
3. **Eliminación lógica**: Nunca eliminar físicamente, solo `status = false`
4. **Cascada**: Al desactivar sucursal, considerar desactivar sus puntos de emisión
5. **⚠️ MATRIZ ÚNICA - LÓGICA IMPLEMENTADA EN FRONTEND**:
   - Solo puede existir una MATRIZ por compañía.
   - **El frontend maneja automáticamente el cambio de MATRIZ**:
     - Cuando el usuario selecciona "MATRIZ", el frontend detecta si ya existe otra MATRIZ.
     - Muestra un warning dialog pidiendo confirmación al usuario.
     - Si el usuario confirma, el frontend ejecuta:
       1. `PUT /api/establishments/{id}` para cambiar la MATRIZ actual a "SUCURSAL"
       2. Luego `POST` o `PUT` para crear/actualizar la nueva MATRIZ
   - **Responsabilidades del backend**:
     - ✅ Permitir múltiples llamadas consecutivas de actualización
     - ✅ **CRÍTICO**: Devolver el campo `status: boolean` en todas las respuestas GET
     - ⚠️ OPCIONAL: Agregar validación adicional que rechace operaciones si detecta 2 MATRIZ activas simultáneamente (capa extra de seguridad)
6. **Campo `status` obligatorio**: Todas las respuestas GET de sucursales y puntos de emisión DEBEN incluir el campo `status: boolean`

---

## 🧪 6. Casos de Prueba Sugeridos

### Sucursales

1. ✅ Crear sucursal con código único
2. ❌ Crear sucursal con código duplicado
3. ✅ Actualizar sucursal existente
4. ❌ Actualizar con código que ya existe en otra sucursal
5. ✅ Eliminar sucursal sin puntos activos
6. ❌ Eliminar sucursal con puntos activos

### Puntos de Emisión

1. ✅ Crear punto con código único
2. ❌ Crear punto con código duplicado en misma sucursal
3. ✅ Crear punto con mismo código pero en sucursal diferente
4. ✅ Incrementar secuencial
5. ❌ Decrementar secuencial
6. ✅ Eliminar punto sin sesiones activas
7. ❌ Eliminar punto con sesiones activas

---

## 📞 7. Contacto

Si tienes dudas sobre algún endpoint o necesitas ajustes, por favor contactar al equipo de frontend.

**Fecha de entrega esperada**: [A DEFINIR]

---

## ✅ Checklist de Implementación

### Backend

- [ ] Crear entidades JPA (Establishment, InvoiceIssuancePoint)
- [ ] Crear repositorios
- [ ] Crear DTOs (Request/Response)
- [ ] Implementar servicios con lógica de negocio
- [ ] Crear controladores REST
- [ ] Agregar validaciones (@Valid, custom validators)
- [ ] Implementar manejo de errores
- [ ] Agregar tests unitarios
- [ ] Agregar tests de integración
- [ ] Documentar con Swagger/OpenAPI

### Base de Datos

- [ ] Crear script de migración (Flyway/Liquibase)
- [ ] Crear tablas
- [ ] Crear índices
- [ ] Crear constraints
- [ ] Agregar datos de prueba (opcional)

### Seguridad

- [ ] Validar JWT en todos los endpoints
- [ ] Validar permisos por rol
- [ ] Validar acceso solo a compañías del usuario
- [ ] Implementar rate limiting (opcional)

---

**FIN DEL DOCUMENTO**
