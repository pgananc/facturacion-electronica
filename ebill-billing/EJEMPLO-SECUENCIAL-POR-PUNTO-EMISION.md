# Ejemplo: Secuencial por Punto de Emisión

## Cómo Funciona el Secuencial

Cada **punto de emisión** tiene su propia secuencia de facturas que inicia en 1 y es completamente independiente de los demás puntos de emisión.

## Ejemplo Visual

### Base de Datos - Tabla Invoice

| id_invoice | id_issuance_point | sequential | invoice_number    | created_at |
| ---------- | ----------------- | ---------- | ----------------- | ---------- |
| 1          | **10**            | **1**      | 001-001-000000001 | 2025-10-14 |
| 2          | **10**            | **2**      | 001-001-000000002 | 2025-10-14 |
| 3          | **20**            | **1**      | 001-002-000000001 | 2025-10-14 |
| 4          | **10**            | **3**      | 001-001-000000003 | 2025-10-14 |
| 5          | **20**            | **2**      | 001-002-000000002 | 2025-10-14 |
| 6          | **30**            | **1**      | 002-001-000000001 | 2025-10-14 |

### Explicación

- **Punto de Emisión ID 10** (establecimiento 001, punto 001):

  - Secuenciales: 1, 2, 3
  - Próximo secuencial: 4

- **Punto de Emisión ID 20** (establecimiento 001, punto 002):

  - Secuenciales: 1, 2
  - Próximo secuencial: 3

- **Punto de Emisión ID 30** (establecimiento 002, punto 001):
  - Secuenciales: 1
  - Próximo secuencial: 2

## Consulta SQL Utilizada

```sql
-- Para obtener el próximo secuencial del punto de emisión 10
SELECT COALESCE(MAX(sequential), 0) FROM invoice WHERE id_issuance_point = 10;
-- Resultado: 3
-- Próximo secuencial: 4

-- Para obtener el próximo secuencial del punto de emisión 20
SELECT COALESCE(MAX(sequential), 0) FROM invoice WHERE id_issuance_point = 20;
-- Resultado: 2
-- Próximo secuencial: 3

-- Para obtener el próximo secuencial del punto de emisión 30
SELECT COALESCE(MAX(sequential), 0) FROM invoice WHERE id_issuance_point = 30;
-- Resultado: 1
-- Próximo secuencial: 2

-- Para un punto de emisión sin facturas previas (ej: 40)
SELECT COALESCE(MAX(sequential), 0) FROM invoice WHERE id_issuance_point = 40;
-- Resultado: 0 (porque no hay facturas)
-- Próximo secuencial: 1
```

## Flujo de Creación de Factura

### Solicitud 1: Factura para Punto de Emisión 10

```json
{
  "establishment": {
    "issuancePoint": {
      "idIssuancePoint": 10,
      "code": "001"
    }
  }
}
```

**Proceso:**

1. Sistema consulta: `MAX(sequential) WHERE id_issuance_point = 10` → Resultado: 3
2. Calcula próximo secuencial: 3 + 1 = **4**
3. Guarda factura con `id_issuance_point = 10` y `sequential = 4`

### Solicitud 2: Factura para Punto de Emisión 20

```json
{
  "establishment": {
    "issuancePoint": {
      "idIssuancePoint": 20,
      "code": "002"
    }
  }
}
```

**Proceso:**

1. Sistema consulta: `MAX(sequential) WHERE id_issuance_point = 20` → Resultado: 2
2. Calcula próximo secuencial: 2 + 1 = **3**
3. Guarda factura con `id_issuance_point = 20` y `sequential = 3`

### Solicitud 3: Primera factura para Punto de Emisión 99 (nuevo)

```json
{
  "establishment": {
    "issuancePoint": {
      "idIssuancePoint": 99,
      "code": "005"
    }
  }
}
```

**Proceso:**

1. Sistema consulta: `MAX(sequential) WHERE id_issuance_point = 99` → Resultado: 0 (no hay facturas)
2. Calcula próximo secuencial: 0 + 1 = **1**
3. Guarda factura con `id_issuance_point = 99` y `sequential = 1`

## Ventajas de este Enfoque

✅ **Independencia**: Cada punto de emisión maneja su propia secuencia
✅ **Cumplimiento SRI**: Cada punto de emisión empieza en 1 como requiere el SRI
✅ **No hay cruces**: El filtro `WHERE id_issuance_point = X` garantiza la separación
✅ **Escalable**: Puedes tener N puntos de emisión sin conflictos
✅ **Auditable**: Puedes rastrear exactamente cuántas facturas ha emitido cada punto

## Código Relevante

### Repositorio (IInvoiceRepo.java)

```java
@Query("SELECT COALESCE(MAX(i.sequential), 0) FROM Invoice i WHERE i.idIssuancePoint = :idIssuancePoint")
Long findMaxSequentialByIssuancePoint(@Param("idIssuancePoint") Long idIssuancePoint);
```

### Servicio (InvoiceServiceImpl.java)

```java
Long idIssuancePoint = invoiceDto.getEstablishment().getIssuancePoint().getIdIssuancePoint();

// Get the next sequential for THIS SPECIFIC issuance point
Long maxSequential = repo.findMaxSequentialByIssuancePoint(idIssuancePoint);
Long nextSequential = (maxSequential != null ? maxSequential : 0L) + 1;

// Set the sequential and issuance point ID
invoice.setIdIssuancePoint(idIssuancePoint);
invoice.setSequential(nextSequential);
```

## Importante

- El secuencial **NO es global**, es **por punto de emisión**
- Si tienes 3 puntos de emisión, tendrás 3 secuencias independientes
- Esto cumple con las normativas del SRI de Ecuador donde cada punto de emisión tiene su propia numeración
