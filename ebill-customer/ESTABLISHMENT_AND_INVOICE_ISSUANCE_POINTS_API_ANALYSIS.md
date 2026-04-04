# 📋 Análisis de Implementación - API Establishment & Invoice Issuance Points

## 🎯 Objetivo

Implementar los endpoints REST faltantes según la especificación del documento para completar la gestión de Sucursales y Puntos de Emisión.

---

## 📊 Estado Actual vs Especificación

### 🏢 **1. API Establishment**

#### ✅ **Endpoints Ya Implementados**

| Endpoint Actual | Especificación | Estado |
|-----------------|----------------|--------|
| `GET /api/establishments?companyId={id}&page={}&size={}` | `GET /api/establishment/company/{idCompany}` | ⚠️ **Diferente** (paginado vs sin paginar) |
| `GET /api/establishments/active?companyId={id}` | - | ✅ Extra (útil) |
| `GET /api/establishments/{id}?companyId={id}` | `GET /api/establishment/{idEstablishment}` | ✅ **Implementado** |
| `POST /api/establishments` | `POST /api/establishment` | ✅ **Implementado** |
| `PUT /api/establishments/{id}?companyId={id}` | `PUT /api/establishment/{idEstablishment}` | ✅ **Implementado** |
| `DELETE /api/establishments/{id}?companyId={id}` | `DELETE /api/establishment/{idEstablishment}` | ✅ **Implementado** |

#### ❌ **Endpoints Faltantes**

| Endpoint Especificado | Estado | Prioridad |
|-----------------------|--------|-----------|
| `GET /api/establishment/company/{idCompany}` (sin paginación) | ⚠️ Existe pero paginado | Media |
| `GET /api/establishment/exists/{idCompany}/{code}` | ❌ No existe | Alta |

#### 🔧 **Acciones Requeridas**

1. **Agregar endpoint de existencia**:
   ```java
   @GetMapping("/company/{idCompany}/exists/{code}")
   public ResponseEntity<Boolean> existsByCodeAndCompany(
       @PathVariable Long idCompany, 
       @PathVariable String code
   )
   ```

2. **Agregar endpoint sin paginación** (opcional, ya existe paginado):
   ```java
   @GetMapping("/company/{idCompany}")
   public ResponseEntity<List<EstablishmentResponseDto>> getAllByCompany(
       @PathVariable Long idCompany
   )
   ```

---

### 📍 **2. API Invoice Issuance Point**

#### ✅ **Endpoints Ya Implementados**

| Endpoint Actual | Especificación | Estado |
|-----------------|----------------|--------|
| `GET /api/invoice-issuance-points/{id}?establishmentId={id}` | `GET /api/invoice-issuance-point/{idInvoiceIssuancePoint}` | ✅ Implementado |
| `GET /api/invoice-issuance-points?establishmentId={id}` (paginado) | - | ✅ Extra (útil) |
| `GET /api/invoice-issuance-points/active?establishmentId={id}` | - | ✅ Extra (útil) |
| `GET /api/invoice-issuance-points/available?idEstablishments=[]` | - | ✅ Extra (útil) |
| `GET /api/invoice-issuance-points/current-session?userName={}` | - | ✅ Extra (útil) |
| `POST /api/invoice-issuance-points` | `POST /api/invoice-issuance-point` | ✅ Implementado |
| `PUT /api/invoice-issuance-points/{id}?establishmentId={id}` | `PUT /api/invoice-issuance-point/{idInvoiceIssuancePoint}` | ✅ Implementado |
| `DELETE /api/invoice-issuance-points/{id}?establishmentId={id}` | `DELETE /api/invoice-issuance-point/{idInvoiceIssuancePoint}` | ✅ Implementado |
| `PATCH /api/invoice-issuance-points/{code}/sequential?establishmentId={id}` | - | ✅ Extra (recién implementado) |

#### ❌ **Endpoints Faltantes**

| Endpoint Especificado | Estado | Prioridad |
|-----------------------|--------|-----------|
| `GET /api/invoice-issuance-point/establishment/{establishmentId}` (sin paginación) | ⚠️ Existe pero paginado | Media |
| `GET /api/invoice-issuance-point/company/{idCompany}` | ❌ No existe | **Alta** |
| `GET /api/invoice-issuance-point/exists/{establishmentId}/{code}` | ❌ No existe | Alta |

#### 🔧 **Acciones Requeridas**

1. **Agregar endpoint por compañía** (⭐ Prioridad Alta):
   ```java
   @GetMapping("/company/{idCompany}")
   public ResponseEntity<List<InvoiceIssuancePointResponseDto>> getAllByCompany(
       @PathVariable Long idCompany
   )
   ```
   - Debe incluir JOIN con Establishment para traer `establishmentCode` y `establishmentName`
   - Necesita DTO extendido: `InvoiceIssuancePointWithEstablishmentDto`

2. **Agregar endpoint de existencia**:
   ```java
   @GetMapping("/establishment/{establishmentId}/exists/{code}")
   public ResponseEntity<Boolean> existsByCodeAndEstablishment(
       @PathVariable Long establishmentId, 
       @PathVariable String code
   )
   ```

3. **Agregar endpoint sin paginación por establecimiento** (opcional):
   ```java
   @GetMapping("/establishment/{establishmentId}")
   public ResponseEntity<List<InvoiceIssuancePointResponseDto>> getAllByEstablishment(
       @PathVariable Long establishmentId
   )
   ```

---

## ⚠️ **3. Discrepancia de Rutas Base**

### Problema Actual

- **Actual**: `/api/establishments` y `/api/invoice-issuance-points` (plural)
- **Especificación**: `/api/establishment` y `/api/invoice-issuance-point` (singular)

### Recomendación

**Mantener las rutas actuales** por las siguientes razones:
1. ✅ RESTful estándar usa plural (`/api/users`, `/api/orders`)
2. ✅ Ya implementadas y probadas
3. ✅ El frontend ya las está consumiendo
4. ⚠️ Cambiar las rutas rompería la compatibilidad

**Decisión**: Actualizar la especificación para reflejar las rutas actuales.

---

## 🔒 **4. Validaciones Requeridas**

### 4.1. **EstablishmentRequestDto**

**Validaciones Faltantes:**

```java
@Data
public class EstablishmentRequestDto {
  
  @NotBlank(message = "El nombre es obligatorio")
  @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
  private String name;
  
  @NotBlank(message = "El código es obligatorio")
  @Pattern(regexp = "^[0-9]{3}$", message = "El código debe ser exactamente 3 dígitos numéricos (001-999)")
  private String code;
  
  @NotBlank(message = "La dirección es obligatoria")
  @Size(max = 200, message = "La dirección no puede exceder 200 caracteres")
  private String address;
  
  @NotBlank(message = "El tipo es obligatorio")
  @Pattern(regexp = "^(MATRIZ|SUCURSAL)$", message = "El tipo debe ser MATRIZ o SUCURSAL")
  private String type;
  
  @NotNull(message = "El ID de la compañía es obligatorio")
  private Long companyId;
}
```

### 4.2. **InvoiceIssuancePointRequestDto**

**Validaciones Faltantes:**

```java
@Data
public class InvoiceIssuancePointRequestDto {
  
  @NotBlank(message = "El código es obligatorio")
  @Pattern(regexp = "^[0-9]{3}$", message = "El código debe ser exactamente 3 dígitos numéricos (001-999)")
  private String code;
  
  @NotNull(message = "El ID del establecimiento es obligatorio")
  private Long establishmentId;
  
  @NotNull(message = "El secuencial es obligatorio")
  @Min(value = 1, message = "El secuencial debe ser mayor o igual a 1")
  private Integer sequentialInvoice;
  
  private Boolean status = true; // Default true
}
```

---

## 🎨 **5. Formato de Respuesta Estandarizado**

### Problema Actual

Las respuestas actuales retornan directamente el DTO:

```json
{
  "idEstablishment": 1,
  "name": "Matriz Quito",
  ...
}
```

### Especificación Requerida

```json
{
  "status": "success",
  "message": "Sucursal encontrada",
  "data": [
    {
      "idEstablishment": 1,
      "name": "Matriz Quito",
      ...
    }
  ]
}
```

### Solución: ResponseWrapper Genérico

```java
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {
    private String status;    // "success" | "error"
    private String message;
    private T data;
    
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
            .status("success")
            .message(message)
            .data(data)
            .build();
    }
    
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
            .status("error")
            .message(message)
            .data(null)
            .build();
    }
}
```

**Uso:**

```java
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<EstablishmentResponseDto>> getById(@PathVariable Long id) {
    EstablishmentResponseDto dto = service.findById(id);
    return ResponseEntity.ok(ApiResponse.success("Sucursal encontrada", dto));
}
```

---

## 🏗️ **6. Lógica de Negocio Faltante**

### 6.1. **Establishment**

#### ❌ No Implementado

1. **Validar código único por compañía**
   - Antes de crear/actualizar, validar que no exista otro con el mismo código
   - Query: `findByCompany_IdCompanyAndCode(Long idCompany, String code)`

2. **No permitir eliminar si tiene puntos activos**
   ```java
   public void deactivateEstablishment(Long id, Long companyId) {
       Establishment establishment = findById(id, companyId);
       
       // ⚠️ FALTA: Validar puntos activos
       long activePoints = invoiceIssuancePointRepo
           .countByEstablishment_IdEstablishmentAndStatusIsTrue(id);
       
       if (activePoints > 0) {
           throw new BusinessException(
               "No se puede eliminar la sucursal porque tiene " + 
               activePoints + " puntos de emisión activos"
           );
       }
       
       establishment.setStatus(false);
       repository.save(establishment);
   }
   ```

### 6.2. **Invoice Issuance Point**

#### ❌ No Implementado

1. **Sequential solo puede incrementar**
   ```java
   public InvoiceIssuancePointResponseDto update(Long id, Long establishmentId, RequestDto dto) {
       InvoiceIssuancePoint entity = findById(id, establishmentId);
       
       // ⚠️ FALTA: Validar que no disminuya
       if (dto.getSequentialInvoice() < entity.getSequentialInvoice()) {
           throw new BusinessException(
               "El secuencial no puede ser menor al actual (" + 
               entity.getSequentialInvoice() + ")"
           );
       }
       
       // ... resto de la actualización
   }
   ```

2. **Validar código único por sucursal**
   - Query: `findByEstablishment_IdEstablishmentAndCode(Long establishmentId, String code)`

3. **No permitir eliminar si hay sesiones activas** (opcional, depende de la tabla de sesiones)

4. **Validar que la sucursal esté activa al crear**
   ```java
   public InvoiceIssuancePointResponseDto create(RequestDto dto) {
       Establishment establishment = establishmentRepo
           .findById(dto.getEstablishmentId())
           .orElseThrow(() -> new NotFoundException("Sucursal no encontrada"));
       
       // ⚠️ FALTA: Validar que esté activa
       if (!establishment.getStatus()) {
           throw new BusinessException(
               "No se puede crear un punto de emisión en una sucursal inactiva"
           );
       }
       
       // ... resto de la creación
   }
   ```

---

## 🗄️ **7. Constraints de Base de Datos Faltantes**

### 7.1. **Tabla `establishment`**

```sql
-- ⚠️ VERIFICAR si existen:

-- 1. Unique constraint (code + company)
ALTER TABLE establishment 
ADD CONSTRAINT uk_establishment_code_company 
UNIQUE (code, id_company);

-- 2. Check constraint (type)
ALTER TABLE establishment 
ADD CONSTRAINT chk_establishment_type 
CHECK (type IN ('MATRIZ', 'SUCURSAL'));

-- 3. Índices
CREATE INDEX idx_establishment_company ON establishment(id_company);
CREATE INDEX idx_establishment_status ON establishment(status);
```

### 7.2. **Tabla `invoice_issuance_point`**

```sql
-- ⚠️ VERIFICAR si existen:

-- 1. Unique constraint (code + establishment)
ALTER TABLE invoice_issuance_point 
ADD CONSTRAINT uk_issuance_point_code_establishment 
UNIQUE (code, establishment_id);

-- 2. Check constraint (sequential positivo)
ALTER TABLE invoice_issuance_point 
ADD CONSTRAINT chk_sequential_positive 
CHECK (sequential_invoice >= 1);

-- 3. Índices
CREATE INDEX idx_issuance_point_establishment ON invoice_issuance_point(establishment_id);
CREATE INDEX idx_issuance_point_status ON invoice_issuance_point(status);
```

---

## 📝 **8. DTOs Adicionales Necesarios**

### 8.1. **InvoiceIssuancePointWithEstablishmentDto**

Para el endpoint `GET /api/invoice-issuance-point/company/{idCompany}`:

```java
@Data
@Builder
public class InvoiceIssuancePointWithEstablishmentDto {
    private Long idInvoiceIssuancePoint;
    private String code;
    private Long establishmentId;
    private String establishmentCode;
    private String establishmentName;
    private Integer sequentialInvoice;
    private Boolean status;
}
```

### 8.2. **ApiResponse<T>** (Wrapper genérico)

Ya descrito en la sección 5.

---

## 📊 **Resumen de Prioridades**

| Prioridad | Tarea | Impacto | Esfuerzo |
|-----------|-------|---------|----------|
| 🔴 **Alta** | Implementar `GET /company/{idCompany}` para Invoice Issuance Points | Alto | Medio |
| 🔴 **Alta** | Agregar validaciones (@NotNull, @Pattern, etc.) | Alto | Bajo |
| 🔴 **Alta** | Validar código único (Establishment y Invoice Issuance Point) | Alto | Bajo |
| 🟡 **Media** | Implementar endpoints `exists` | Medio | Bajo |
| 🟡 **Media** | No permitir eliminar sucursal con puntos activos | Alto | Bajo |
| 🟡 **Media** | Sequential solo puede incrementar | Medio | Bajo |
| 🟢 **Baja** | Estandarizar respuestas con ApiResponse | Bajo | Alto |
| 🟢 **Baja** | Endpoints sin paginación (ya existen paginados) | Bajo | Medio |
| 🟢 **Baja** | Verificar constraints de BD | Alto (seguridad) | Bajo |

---

## ✅ **Checklist de Implementación**

### Fase 1: Validaciones y Seguridad (Alta Prioridad)
- [ ] Agregar validaciones a `EstablishmentRequestDto`
- [ ] Agregar validaciones a `InvoiceIssuancePointRequestDto`
- [ ] Implementar validación de código único (Establishment)
- [ ] Implementar validación de código único (Invoice Issuance Point)
- [ ] Validar sucursal activa al crear punto de emisión
- [ ] Validar que sequential no disminuya al actualizar

### Fase 2: Endpoints Faltantes (Alta Prioridad)
- [ ] `GET /api/invoice-issuance-points/company/{idCompany}` (con JOIN)
- [ ] `GET /api/establishments/company/{idCompany}/exists/{code}`
- [ ] `GET /api/invoice-issuance-points/establishment/{establishmentId}/exists/{code}`
- [ ] Crear `InvoiceIssuancePointWithEstablishmentDto`

### Fase 3: Lógica de Negocio (Media Prioridad)
- [ ] No permitir eliminar sucursal con puntos activos
- [ ] Validar sesiones activas antes de eliminar punto (opcional)

### Fase 4: Opcional (Baja Prioridad)
- [ ] Estandarizar respuestas con `ApiResponse<T>`
- [ ] Endpoints sin paginación (GET /company/{id} sin paginación)
- [ ] Verificar constraints de BD

---

## 🚀 **Siguiente Paso**

**Comenzar con Fase 1**: Agregar validaciones y lógica de unicidad.

¿Quieres que proceda con la implementación? 🔧

