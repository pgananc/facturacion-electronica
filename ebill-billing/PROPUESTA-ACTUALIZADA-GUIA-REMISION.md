# 📋 Propuesta Actualizada: Estructura de Base de Datos para Guías de Remisión

**Fecha:** Propuesta actualizada  
**Feature:** Guías de Remisión (Guide/GuiaRemision)  
**Código SRI:** 06  
**Cambios aplicados:**

- ❌ Eliminado `total_amount` (las guías no son documentos de venta)
- ✅ Agregado `id_establishment` (falta en propuesta original)
- ❌ Eliminada tabla `guide_destination` (se usa `id_client` directamente, como en facturas)

---

## 🎯 Decisión: Estructura Simplificada

### ✅ **Uso de `id_client` para Destinatario**

**Análisis:**

En el sistema actual:

- ✅ **Facturas** tienen UN SOLO cliente: `id_client BIGINT` en tabla `invoice`
- ✅ **Clientes** están en microservicio `ebill-customer` y se consultan vía Feign Client
- ✅ **Patrón consistente**: Un documento = Un cliente

**Para Guías de Remisión:**

Según el patrón del sistema y las prácticas comunes en Ecuador, **una guía de remisión típicamente tiene UN SOLO destinatario**. La tabla `guide_destination` propuesta originalmente asume múltiples destinatarios, pero:

1. ❌ **No hay evidencia** en el código existente de que se necesiten múltiples destinatarios
2. ✅ **Consistencia**: Mantener el mismo patrón que facturas (1 cliente por documento)
3. ✅ **Simplicidad**: Evita complejidad innecesaria
4. ✅ **Reutilización**: Los clientes ya están en el sistema (`ebill-customer`)

**Si en el futuro se necesitan múltiples destinatarios**, se puede crear una tabla de relación `guide_destination` sin romper el código existente (migración incremental).

---

## 🗄️ Estructura Propuesta

### 1. `guide` (Tabla Principal)

#### Campos Comunes (como en otros comprobantes):

```sql
CREATE TABLE guide (
    id_guide BIGINT NOT NULL PRIMARY KEY,
    id_company BIGINT NOT NULL,
    id_client BIGINT, -- FK a client (destinatario) - consultado vía Feign desde ebill-customer
    id_establishment BIGINT, -- FK a establishment
    id_issuance_point BIGINT, -- FK a issuance_point
    sequential BIGINT, -- Número secuencial por punto de emisión
    retry_count INTEGER DEFAULT 0, -- Contador de reintentos
    issue_date TIMESTAMP NOT NULL, -- Fecha de emisión
    guide_number VARCHAR(17), -- Formato: estab-ptoEmi-secuencial
    environment VARCHAR(10), -- Ambiente (pruebas/producción)
    additional_information TEXT, -- Información adicional
    email_sent BOOLEAN DEFAULT FALSE, -- Si se envió por email
    email_sent_date TIMESTAMP, -- Fecha de envío por email
    id_authorization_sri BIGINT, -- FK a authorization_sri
    status BOOLEAN DEFAULT TRUE,
    creation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT guide_authorization_sri_id_authorization_sri_fk
        FOREIGN KEY (id_authorization_sri) REFERENCES authorization_sri(id_authorization_sri)
);
```

#### Campos Específicos de Guía de Remisión:

```sql
-- Campos de transporte y traslado
start_date TIMESTAMP NOT NULL, -- Fecha inicio de traslado
end_date TIMESTAMP NOT NULL, -- Fecha fin de traslado
transport_type VARCHAR(50), -- Tipo de transporte (terrestre, aéreo, marítimo, etc.)
transport_plate VARCHAR(20), -- Placa del vehículo
id_carrier BIGINT NOT NULL, -- FK a carrier (transportista) - OBLIGATORIO
origin_address VARCHAR(300) NOT NULL, -- Dirección de partida
destination_address VARCHAR(300) NOT NULL, -- Dirección de destino
reason_for_transfer VARCHAR(300), -- Motivo del traslado
route VARCHAR(300), -- Ruta a seguir (opcional)
```

**Estructura completa:**

```sql
CREATE TABLE guide (
    id_guide BIGINT NOT NULL PRIMARY KEY,
    -- Campos comunes
    id_company BIGINT NOT NULL,
    id_client BIGINT, -- Destinatario (consultado desde ebill-customer)
    id_establishment BIGINT,
    id_issuance_point BIGINT,
    sequential BIGINT,
    retry_count INTEGER DEFAULT 0,
    issue_date TIMESTAMP NOT NULL,
    guide_number VARCHAR(17),
    environment VARCHAR(10),
    additional_information TEXT,
    email_sent BOOLEAN DEFAULT FALSE,
    email_sent_date TIMESTAMP,
    id_authorization_sri BIGINT,
    -- Campos específicos de guía
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    transport_type VARCHAR(50),
    transport_plate VARCHAR(20),
    id_carrier BIGINT NOT NULL, -- FK a carrier (OBLIGATORIO)
    origin_address VARCHAR(300) NOT NULL,
    destination_address VARCHAR(300) NOT NULL,
    reason_for_transfer VARCHAR(300),
    route VARCHAR(300),
    -- Campos BaseEntity
    status BOOLEAN DEFAULT TRUE,
    creation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT guide_authorization_sri_id_authorization_sri_fk
        FOREIGN KEY (id_authorization_sri) REFERENCES authorization_sri(id_authorization_sri),
    CONSTRAINT guide_carrier_fk
        FOREIGN KEY (id_carrier) REFERENCES carrier(id_carrier)
);

COMMENT ON TABLE guide IS 'Tabla de guías de remisión';
COMMENT ON COLUMN guide.id_client IS 'ID del cliente destinatario (consultado desde ebill-customer vía Feign)';
COMMENT ON COLUMN guide.id_establishment IS 'ID del establecimiento emisor';
COMMENT ON COLUMN guide.id_issuance_point IS 'ID del punto de emisión';
COMMENT ON COLUMN guide.sequential IS 'Número secuencial por punto de emisión';
COMMENT ON COLUMN guide.guide_number IS 'Número de guía (formato: estab-ptoEmi-secuencial, 17 caracteres)';
COMMENT ON COLUMN guide.start_date IS 'Fecha inicio de traslado';
COMMENT ON COLUMN guide.end_date IS 'Fecha fin de traslado';
COMMENT ON COLUMN guide.transport_type IS 'Tipo de transporte (terrestre, aéreo, marítimo, etc.)';
COMMENT ON COLUMN guide.transport_plate IS 'Placa del vehículo';
COMMENT ON COLUMN guide.id_carrier IS 'FK a transportista en catálogo (OBLIGATORIO, debe existir en carrier)';
COMMENT ON COLUMN guide.origin_address IS 'Dirección de partida';
COMMENT ON COLUMN guide.destination_address IS 'Dirección de destino';
```

---

### 2. `guide_detail` (Detalles/Productos a Transportar)

```sql
CREATE TABLE guide_detail (
    id_guide_detail BIGINT NOT NULL PRIMARY KEY,
    id_guide BIGINT NOT NULL,
    id_product BIGINT, -- FK a producto (opcional, puede ser producto genérico)
    description VARCHAR(300) NOT NULL, -- Descripción del producto
    quantity NUMERIC(19, 2) NOT NULL, -- Cantidad
    unit VARCHAR(10), -- Unidad de medida (UN, KG, M, etc.)
    weight NUMERIC(19, 2), -- Peso (si aplica, opcional)
    additional_info TEXT, -- Información adicional del producto
    status BOOLEAN DEFAULT TRUE,
    creation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_guide_detail_guide
        FOREIGN KEY (id_guide) REFERENCES guide(id_guide) ON DELETE CASCADE
);

CREATE SEQUENCE IF NOT EXISTS guide_detail_seq START WITH 1 INCREMENT BY 1;

COMMENT ON TABLE guide_detail IS 'Tabla de detalles/productos a transportar en la guía de remisión';
COMMENT ON COLUMN guide_detail.id_product IS 'ID del producto (opcional, puede ser genérico)';
COMMENT ON COLUMN guide_detail.description IS 'Descripción del producto/item a transportar';
COMMENT ON COLUMN guide_detail.quantity IS 'Cantidad a transportar';
COMMENT ON COLUMN guide_detail.unit IS 'Unidad de medida (UN, KG, M, etc.)';
COMMENT ON COLUMN guide_detail.weight IS 'Peso del item (opcional)';
```

**Comentarios:**

- ✅ Cada guía puede tener múltiples productos/items a transportar
- ✅ El `id_product` es opcional porque puede ser un producto genérico o no catalogado
- ✅ `quantity` puede ser decimal (ej: 2.5 kg)
- ✅ `unit` sigue el catálogo del SRI (UN, KG, M, etc.)
- ❌ **NO tiene** `unit_price`, `subtotal`, `discount` (las guías no son documentos de venta)

---

### 3. `guide_document_reference` (Documentos Relacionados - Facturas, etc.)

```sql
CREATE TABLE guide_document_reference (
    id_guide_document_reference BIGINT NOT NULL PRIMARY KEY,
    id_guide BIGINT NOT NULL,
    document_type VARCHAR(2) NOT NULL, -- Tipo documento: 01=Factura, 04=NC, etc.
    document_number VARCHAR(17) NOT NULL, -- Número del documento
    access_key VARCHAR(49), -- Clave de acceso (si aplica)
    issue_date TIMESTAMP, -- Fecha de emisión del documento
    status BOOLEAN DEFAULT TRUE,
    creation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_guide_document_reference_guide
        FOREIGN KEY (id_guide) REFERENCES guide(id_guide) ON DELETE CASCADE
);

CREATE SEQUENCE IF NOT EXISTS guide_document_reference_seq START WITH 1 INCREMENT BY 1;

COMMENT ON TABLE guide_document_reference IS 'Tabla de documentos relacionados con la guía (facturas, notas de crédito, etc.)';
COMMENT ON COLUMN guide_document_reference.document_type IS 'Tipo de documento según SRI (01=Factura, 04=NC, 05=ND, etc.)';
COMMENT ON COLUMN guide_document_reference.document_number IS 'Número del documento (formato: estab-ptoEmi-secuencial)';
COMMENT ON COLUMN guide_document_reference.access_key IS 'Clave de acceso del documento (49 dígitos, opcional)';
COMMENT ON COLUMN guide_document_reference.issue_date IS 'Fecha de emisión del documento relacionado';
```

**Comentarios:**

- ✅ Una guía puede referenciar **múltiples documentos** (facturas, notas de crédito, etc.)
- ✅ El `document_type` sigue el catálogo del SRI (01, 04, 05, etc.)
- ✅ `access_key` es opcional pero recomendado si el documento está autorizado
- ✅ `issue_date` ayuda a validar la relación temporal

---

### 4. `carrier` (Transportistas - Tabla Maestra)

```sql
CREATE TABLE carrier (
    id_carrier BIGINT NOT NULL PRIMARY KEY,
    id_company BIGINT NOT NULL, -- FK a company (el transportista pertenece a una empresa)
    identification_type VARCHAR(2) NOT NULL, -- Tipo identificación (04=RUC, 05=Cédula, etc.)
    identification VARCHAR(13) NOT NULL, -- RUC/Cédula del transportista
    name VARCHAR(300) NOT NULL, -- Razón social/Nombre del transportista
    address VARCHAR(300), -- Dirección
    phone VARCHAR(20), -- Teléfono
    email VARCHAR(100), -- Email
    license_number VARCHAR(50), -- Número de licencia de conducir (opcional)
    vehicle_type VARCHAR(50), -- Tipo de vehículo (camión, furgón, etc.)
    status BOOLEAN DEFAULT TRUE,
    creation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_carrier_company_identification
        UNIQUE (id_company, identification)
);

CREATE SEQUENCE IF NOT EXISTS carrier_seq START WITH 1 INCREMENT BY 1;

COMMENT ON TABLE carrier IS 'Catálogo de transportistas';
COMMENT ON COLUMN carrier.id_company IS 'ID de la empresa que tiene registrado este transportista';
COMMENT ON COLUMN carrier.identification_type IS 'Tipo de identificación (04=RUC, 05=Cédula, etc.)';
COMMENT ON COLUMN carrier.identification IS 'Número de identificación del transportista';
COMMENT ON COLUMN carrier.name IS 'Nombre o razón social del transportista';
COMMENT ON COLUMN carrier.license_number IS 'Número de licencia de conducir (opcional)';
COMMENT ON COLUMN carrier.vehicle_type IS 'Tipo de vehículo (opcional)';
```

**Comentarios:**

- ✅ Cada empresa puede tener sus propios transportistas
- ✅ El constraint `uk_carrier_company_identification` evita duplicados por empresa
- ✅ Campos específicos de transporte: `license_number`, `vehicle_type`
- ✅ Permite reutilización de transportistas en múltiples guías

---

## 🔄 Secuencias Necesarias

```sql
CREATE SEQUENCE IF NOT EXISTS guide_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS guide_detail_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS guide_document_reference_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS carrier_seq START WITH 1 INCREMENT BY 1;
```

---

## 📐 Índices Recomendados

```sql
-- Índices para búsquedas frecuentes en guide
CREATE INDEX idx_guide_company ON guide(id_company);
CREATE INDEX idx_guide_client ON guide(id_client);
CREATE INDEX idx_guide_establishment ON guide(id_establishment);
CREATE INDEX idx_guide_issuance_point ON guide(id_issuance_point, sequential);
CREATE INDEX idx_guide_issue_date ON guide(issue_date);
CREATE INDEX idx_guide_number ON guide(guide_number);
CREATE INDEX idx_guide_authorization ON guide(id_authorization_sri);
CREATE INDEX idx_guide_carrier ON guide(id_carrier);

-- Índices para relaciones
CREATE INDEX idx_guide_detail_guide ON guide_detail(id_guide);
CREATE INDEX idx_guide_document_reference_guide ON guide_document_reference(id_guide);
CREATE INDEX idx_guide_document_reference_type ON guide_document_reference(document_type, document_number);

-- Índices para carrier
CREATE INDEX idx_carrier_company ON carrier(id_company);
CREATE INDEX idx_carrier_identification ON carrier(identification);
```

---

## 📐 Diagrama de Relaciones

```
carrier (1) ── (N) guide
                │
guide (1) ──┬── (N) guide_detail
            ├── (N) guide_document_reference
            └── (1) authorization_sri
            └── (1) company
            └── (1) client (destinatario, en ebill-customer vía Feign)
            └── (1) establishment
            └── (1) issuance_point
```

---

## 🔄 Comparación con Propuesta Original

| Aspecto                           | Propuesta Original           | Propuesta Actualizada             | Justificación                                                            |
| --------------------------------- | ---------------------------- | --------------------------------- | ------------------------------------------------------------------------ |
| **`total_amount`**                | ✅ Incluido                  | ❌ **Eliminado**                  | Las guías no son documentos de venta, no tienen totales                  |
| **`id_establishment`**            | ⚠️ Mencionado pero no en SQL | ✅ **Agregado**                   | Consistencia con otras tablas (invoice, credit_note, debit_note)         |
| **`guide_destination`**           | ✅ Tabla separada            | ❌ **Eliminada**                  | Se usa `id_client` directamente (1 destinatario por guía, como facturas) |
| **`id_client`**                   | ❌ No estaba claro           | ✅ **Agregado como destinatario** | Consistencia con patrón de facturas                                      |
| **`carrier`**                     | ✅ Incluido                  | ✅ **Mantenido y OBLIGATORIO**    | Todos los transportistas deben estar en catálogo                         |
| **`id_carrier`**                  | ⚠️ Opcional con fallback     | ✅ **OBLIGATORIO (NOT NULL)**     | Consistencia y validación de datos                                       |
| **Campos de transporte fallback** | ✅ Incluidos                 | ❌ **Eliminados**                 | No se necesitan si carrier es obligatorio                                |

---

## ✅ Ventajas de esta Propuesta

### 1. **Simplicidad** ⭐⭐⭐⭐⭐

- ✅ Menos tablas que mantener
- ✅ Estructura más simple y fácil de entender
- ✅ Menos queries complejas

### 2. **Consistencia con el Sistema** ⭐⭐⭐⭐⭐

- ✅ Mismo patrón que `Invoice`, `CreditNote`, `DebitNote` (1 cliente por documento)
- ✅ Reutiliza infraestructura existente (`CustomerClient` Feign)
- ✅ Facilita el mantenimiento

### 3. **Validación y Consistencia** ⭐⭐⭐⭐⭐

- ✅ Todos los transportistas deben estar en el catálogo `carrier` (validación de datos)
- ✅ Evita duplicación de información de transportistas
- ✅ Facilita mantenimiento y actualización de datos de transportistas

### 4. **Eficiencia** ⭐⭐⭐⭐⭐

- ✅ Menos JOINs necesarios
- ✅ Queries más rápidas
- ✅ Menos datos redundantes

---

## 🔄 Flujo de Trabajo Propuesto

### **Crear Guía de Remisión:**

1. **Seleccionar Destinatario:**

   - Seleccionar cliente existente desde `ebill-customer` vía Feign → `id_client`
   - Si no existe, crear cliente primero en `ebill-customer`, luego usar su `id_client`

2. **Seleccionar Transportista:**

   - **OBLIGATORIO:** Debe existir en el catálogo `carrier`
   - Si no existe, crear transportista en `carrier` primero, luego seleccionar → `id_carrier`

3. **Agregar Productos:**

   - Seleccionar de catálogo (`ebill-product`) → `id_product`
   - O ingresar manualmente → solo `description` (sin `id_product`)

4. **Agregar Documentos Relacionados:**
   - Referenciar facturas, notas de crédito, etc. → `guide_document_reference`

---

## 📝 Endpoints Sugeridos

### **Guías de Remisión:**

```
GET    /api/guide                    - Listar guías de la empresa
GET    /api/guide/{id}               - Obtener guía por ID
POST   /api/guide                    - Crear nueva guía
PUT    /api/guide/{id}               - Actualizar guía
DELETE /api/guide/{id}               - Eliminar guía (soft delete)
GET    /api/guide/search?q={query}   - Buscar guías
GET    /api/guide/by-client/{idClient} - Listar guías de un cliente
```

### **Transportistas (Carrier):**

```
GET    /api/carrier                    - Listar transportistas de la empresa
GET    /api/carrier/{id}               - Obtener transportista por ID
POST   /api/carrier                    - Crear nuevo transportista
PUT    /api/carrier/{id}               - Actualizar transportista
DELETE /api/carrier/{id}               - Eliminar transportista (soft delete)
GET    /api/carrier/search?q={query}   - Buscar transportistas
GET    /api/carrier/by-company/{idCompany} - Listar por empresa
```

**Nota:** Los destinatarios NO necesitan endpoints propios porque:

- ✅ Si son clientes existentes → se gestionan en `ebill-customer`
- ✅ Se consultan vía `CustomerClient` Feign (como en facturas)

---

## ⚠️ Consideraciones Futuras

### **Si se necesitan múltiples destinatarios en el futuro:**

Se puede crear una migración para agregar `guide_destination` sin romper el código existente:

```sql
-- Migración futura (solo si es necesario)
CREATE TABLE guide_destination (
    id_guide_destination BIGINT NOT NULL PRIMARY KEY,
    id_guide BIGINT NOT NULL,
    id_client BIGINT, -- FK opcional a cliente existente
    identification_type VARCHAR(2),
    identification VARCHAR(13),
    name VARCHAR(300),
    address VARCHAR(300),
    ...
    CONSTRAINT fk_guide_destination_guide
        FOREIGN KEY (id_guide) REFERENCES guide(id_guide) ON DELETE CASCADE
);

-- Hacer id_client opcional en guide
ALTER TABLE guide ALTER COLUMN id_client DROP NOT NULL;

-- Agregar comentario explicando cambio
COMMENT ON COLUMN guide.id_client IS 'Destinatario principal (legacy). Si hay múltiples destinatarios, usar guide_destination';
```

Pero por ahora, **la simplicidad y consistencia son más importantes**.

---

## ✅ Conclusión

Esta propuesta:

- ✅ **Elimina complejidad innecesaria** (`guide_destination`)
- ✅ **Mantiene consistencia** con el patrón del sistema (1 cliente por documento)
- ✅ **Elimina campos incorrectos** (`total_amount`)
- ✅ **Agrega campos faltantes** (`id_establishment`)
- ✅ **Hace `id_carrier` obligatorio** (consistencia y validación de datos)
- ✅ **Elimina campos de fallback innecesarios** (`transport_identification`, `transport_name`)
- ✅ **Permite flexibilidad futura** (si se necesitan múltiples destinatarios)
- ✅ **Reutiliza infraestructura existente** (`CustomerClient` Feign)

**Estructura final:**

1. ✅ `guide` - Tabla principal (con `id_client` como destinatario)
2. ✅ `guide_detail` - Productos a transportar
3. ✅ `guide_document_reference` - Documentos relacionados
4. ✅ `carrier` - Catálogo de transportistas

---

**Próximos pasos:**

1. ✅ Revisar y aprobar esta propuesta
2. Crear migración Flyway con las tablas propuestas
3. Crear entidades JPA correspondientes
4. Crear DTOs y servicios siguiendo el patrón de otros comprobantes
5. Implementar generación de XML según esquema del SRI
