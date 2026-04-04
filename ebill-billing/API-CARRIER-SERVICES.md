# API de Servicios de Carrier (Transportistas)

Este documento describe todos los endpoints disponibles para gestionar transportistas (carriers) en el sistema.

## Base URL
```
/api/carrier
```

---

## 1. GET /api/carrier - Listar Transportistas

Obtiene todos los transportistas de una empresa con paginación.

### Request (Query Parameters)

| Parámetro | Tipo | Requerido | Descripción |
|-----------|------|-----------|-------------|
| `idCompany` | Long | ✅ Sí | ID de la empresa |
| `activeOnly` | Boolean | ❌ No | Si es `true`, solo retorna transportistas activos (default: `false`) |
| `page` | Integer | ❌ No | Número de página (default: 0) |
| `size` | Integer | ❌ No | Tamaño de página (default: 10) |
| `sort` | String | ❌ No | Campo de ordenamiento (ej: `name,asc`) |

### Request Example
```
GET /api/carrier?idCompany=1&activeOnly=true&page=0&size=10&sort=name,asc
```

### Response (200 OK)

**Tipo:** `Page<CarrierDto>`

```json
{
  "content": [
    {
      "idCarrier": 1,
      "idCompany": 1,
      "identificationType": "RUC",
      "identification": "1234567890001",
      "name": "Transportes ABC S.A.",
      "address": "Av. Principal 123",
      "phone": "0999999999",
      "email": "contacto@transportesabc.com",
      "licenseNumber": "LIC-12345",
      "vehicleType": "CAMION",
      "status": true
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": {
      "sorted": true,
      "unsorted": false,
      "empty": false
    }
  },
  "totalElements": 25,
  "totalPages": 3,
  "last": false,
  "first": true,
  "numberOfElements": 10,
  "size": 10,
  "number": 0,
  "sort": {
    "sorted": true,
    "unsorted": false,
    "empty": false
  },
  "empty": false
}
```

### Response Fields (CarrierDto)

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `idCarrier` | Long | ID único del transportista |
| `idCompany` | Long | ID de la empresa a la que pertenece |
| `identificationType` | String | Tipo de identificación (RUC, CÉDULA, PASAPORTE) |
| `identification` | String | Número de identificación |
| `name` | String | Nombre o razón social del transportista |
| `address` | String | Dirección |
| `phone` | String | Teléfono |
| `email` | String | Correo electrónico |
| `licenseNumber` | String | Número de licencia de conducir |
| `vehicleType` | String | Tipo de vehículo (CAMION, FURGON, etc.) |
| `status` | Boolean | Estado activo/inactivo |

---

## 2. GET /api/carrier/{id} - Obtener Transportista por ID

Obtiene un transportista específico por su ID.

### Request (Path Parameters)

| Parámetro | Tipo | Requerido | Descripción |
|-----------|------|-----------|-------------|
| `id` | Long | ✅ Sí | ID del transportista |

### Request Example
```
GET /api/carrier/1
```

### Response (200 OK)

**Tipo:** `CarrierDto`

```json
{
  "idCarrier": 1,
  "idCompany": 1,
  "identificationType": "RUC",
  "identification": "1234567890001",
  "name": "Transportes ABC S.A.",
  "address": "Av. Principal 123",
  "phone": "0999999999",
  "email": "contacto@transportesabc.com",
  "licenseNumber": "LIC-12345",
  "vehicleType": "CAMION",
  "status": true
}
```

### Response (404 NOT FOUND)

```json
// Sin body
```

---

## 3. POST /api/carrier - Crear Transportista

Crea un nuevo transportista.

### Request (Body)

**Tipo:** `CarrierDto` (sin `idCarrier`)

```json
{
  "idCompany": 1,
  "identificationType": "RUC",
  "identification": "1234567890001",
  "name": "Transportes ABC S.A.",
  "address": "Av. Principal 123",
  "phone": "0999999999",
  "email": "contacto@transportesabc.com",
  "licenseNumber": "LIC-12345",
  "vehicleType": "CAMION",
  "status": true
}
```

### Request Fields (Requeridos)

| Campo | Tipo | Requerido | Validación |
|-------|------|-----------|------------|
| `idCompany` | Long | ✅ Sí | Debe existir la empresa |
| `identificationType` | String | ✅ Sí | No vacío |
| `identification` | String | ✅ Sí | No vacío, único por empresa |
| `name` | String | ✅ Sí | No vacío |
| `address` | String | ❌ No | - |
| `phone` | String | ❌ No | - |
| `email` | String | ❌ No | Formato de email válido |
| `licenseNumber` | String | ❌ No | - |
| `vehicleType` | String | ❌ No | - |
| `status` | Boolean | ❌ No | Default: `true` |

### Request Example
```http
POST /api/carrier
Content-Type: application/json

{
  "idCompany": 1,
  "identificationType": "RUC",
  "identification": "9876543210001",
  "name": "Transportes XYZ S.A.",
  "address": "Av. Secundaria 456",
  "phone": "0988888888",
  "email": "info@transportesxyz.com",
  "licenseNumber": "LIC-67890",
  "vehicleType": "FURGON",
  "status": true
}
```

### Response (201 CREATED)

**Tipo:** `CarrierDto` (con `idCarrier` generado)

```json
{
  "idCarrier": 2,
  "idCompany": 1,
  "identificationType": "RUC",
  "identification": "9876543210001",
  "name": "Transportes XYZ S.A.",
  "address": "Av. Secundaria 456",
  "phone": "0988888888",
  "email": "info@transportesxyz.com",
  "licenseNumber": "LIC-67890",
  "vehicleType": "FURGON",
  "status": true
}
```

### Response (400 BAD REQUEST)

```json
{
  "code": 400,
  "status": "ERROR",
  "message": "Ya existe un transportista con esta identificación para esta empresa"
}
```

**Posibles mensajes de error:**
- `"Ya existe un transportista con esta identificación para esta empresa"`
- `"La empresa especificada no existe"`
- `"El nombre del transportista es obligatorio"`
- `"La identificación del transportista es obligatoria"`

---

## 4. PUT /api/carrier/{id} - Actualizar Transportista

Actualiza un transportista existente.

### Request (Path Parameters)

| Parámetro | Tipo | Requerido | Descripción |
|-----------|------|-----------|-------------|
| `id` | Long | ✅ Sí | ID del transportista |

### Request (Body)

**Tipo:** `CarrierDto` (sin `idCarrier` o con el mismo `idCarrier` del path)

```json
{
  "idCompany": 1,
  "identificationType": "RUC",
  "identification": "1234567890001",
  "name": "Transportes ABC S.A. - Actualizado",
  "address": "Av. Principal 123 - Nueva Dirección",
  "phone": "0999999999",
  "email": "nuevo@transportesabc.com",
  "licenseNumber": "LIC-12345",
  "vehicleType": "CAMION",
  "status": true
}
```

### Request Example
```http
PUT /api/carrier/1
Content-Type: application/json

{
  "idCompany": 1,
  "identificationType": "RUC",
  "identification": "1234567890001",
  "name": "Transportes ABC S.A. - Actualizado",
  "address": "Av. Principal 123 - Nueva Dirección",
  "phone": "0999999999",
  "email": "nuevo@transportesabc.com",
  "licenseNumber": "LIC-12345",
  "vehicleType": "CAMION",
  "status": true
}
```

### Response (200 OK)

**Tipo:** `CarrierDto` (actualizado)

```json
{
  "idCarrier": 1,
  "idCompany": 1,
  "identificationType": "RUC",
  "identification": "1234567890001",
  "name": "Transportes ABC S.A. - Actualizado",
  "address": "Av. Principal 123 - Nueva Dirección",
  "phone": "0999999999",
  "email": "nuevo@transportesabc.com",
  "licenseNumber": "LIC-12345",
  "vehicleType": "CAMION",
  "status": true
}
```

### Response (400 BAD REQUEST)

```json
{
  "code": 400,
  "status": "ERROR",
  "message": "Ya existe otro transportista con esta identificación para esta empresa"
}
```

**Posibles mensajes de error:**
- `"El transportista no existe"`
- `"Ya existe otro transportista con esta identificación para esta empresa"`
- `"La empresa especificada no existe"`

### Response (404 NOT FOUND)

```json
// Sin body
```

---

## 5. DELETE /api/carrier/{id} - Eliminar Transportista

Elimina (desactiva) un transportista. Realiza una eliminación lógica (soft delete), estableciendo `status = false`.

### Request (Path Parameters)

| Parámetro | Tipo | Requerido | Descripción |
|-----------|------|-----------|-------------|
| `id` | Long | ✅ Sí | ID del transportista |

### Request Example
```
DELETE /api/carrier/1
```

### Response (204 NO CONTENT)

```json
// Sin body
```

### Response (404 NOT FOUND)

```json
// Sin body
```

**Nota:** El transportista no se elimina físicamente de la base de datos, solo se marca como inactivo (`status = false`).

---

## Modelo de Datos Completo

### CarrierDto

```typescript
interface CarrierDto {
  idCarrier?: number;              // Generado automáticamente al crear
  idCompany: number;                // REQUERIDO
  identificationType: string;       // REQUERIDO (RUC, CÉDULA, PASAPORTE)
  identification: string;          // REQUERIDO (único por empresa)
  name: string;                    // REQUERIDO
  address?: string;
  phone?: string;
  email?: string;                  // Debe ser un email válido si se proporciona
  licenseNumber?: string;
  vehicleType?: string;            // CAMION, FURGON, etc.
  status?: boolean;                // Default: true
}
```

---

## Códigos de Estado HTTP

| Código | Descripción | Uso |
|--------|-------------|-----|
| 200 | OK | GET, PUT exitosos |
| 201 | Created | POST exitoso |
| 204 | No Content | DELETE exitoso |
| 400 | Bad Request | Validación fallida, datos inválidos |
| 404 | Not Found | Recurso no encontrado |
| 500 | Internal Server Error | Error del servidor |

---

## Ejemplos de Uso

### Ejemplo 1: Listar transportistas activos de una empresa

```bash
curl -X GET "http://localhost:8480/api/carrier?idCompany=1&activeOnly=true&page=0&size=20" \
  -H "Content-Type: application/json"
```

### Ejemplo 2: Crear un nuevo transportista

```bash
curl -X POST "http://localhost:8480/api/carrier" \
  -H "Content-Type: application/json" \
  -d '{
    "idCompany": 1,
    "identificationType": "RUC",
    "identification": "1234567890001",
    "name": "Transportes ABC S.A.",
    "address": "Av. Principal 123",
    "phone": "0999999999",
    "email": "contacto@transportesabc.com",
    "licenseNumber": "LIC-12345",
    "vehicleType": "CAMION",
    "status": true
  }'
```

### Ejemplo 3: Actualizar un transportista

```bash
curl -X PUT "http://localhost:8480/api/carrier/1" \
  -H "Content-Type: application/json" \
  -d '{
    "idCompany": 1,
    "identificationType": "RUC",
    "identification": "1234567890001",
    "name": "Transportes ABC S.A. - Actualizado",
    "address": "Av. Principal 123",
    "phone": "0999999999",
    "email": "nuevo@transportesabc.com",
    "licenseNumber": "LIC-12345",
    "vehicleType": "CAMION",
    "status": true
  }'
```

### Ejemplo 4: Eliminar un transportista

```bash
curl -X DELETE "http://localhost:8480/api/carrier/1" \
  -H "Content-Type: application/json"
```

---

## Notas Importantes

1. **Eliminación Lógica**: El endpoint DELETE realiza una eliminación lógica (soft delete), no elimina físicamente el registro de la base de datos.

2. **Unicidad**: La combinación `identification` + `idCompany` debe ser única. No puede haber dos transportistas con la misma identificación en la misma empresa.

3. **Paginación**: El endpoint GET soporta paginación estándar de Spring Data con los parámetros `page`, `size` y `sort`.

4. **Validación**: Todos los campos requeridos son validados usando Bean Validation (`@Valid`).

5. **Estado**: El campo `status` controla si el transportista está activo o inactivo. Los transportistas inactivos pueden ser filtrados usando el parámetro `activeOnly=true`.



