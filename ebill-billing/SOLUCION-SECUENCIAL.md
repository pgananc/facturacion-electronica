# Solución: Asignación Automática de Secuencial por Punto de Emisión

## Problema

El error "Secuencial no asignado" ocurría cuando se intentaba generar una factura sin asignar un número secuencial. Esto sucedía en:

```
InfoTributaria.generarClaveAcceso(InfoTributaria.java:130)
```

## Solución Implementada

### 1. Modificaciones en la Entidad `Invoice`

Se agregaron dos nuevos campos:

- `idIssuancePoint` (Long): ID del punto de emisión
- `sequential` (Long): Número secuencial de la factura para ese punto de emisión

**Archivo**: `src/main/java/com/wallparisoft/ebillbilling/entity/Invoice.java`

```java
@Column(name = "id_issuance_point")
private Long idIssuancePoint;

@Column(name = "sequential")
private Long sequential;
```

### 2. Migración de Base de Datos

**Archivo**: `src/main/resources/flyway/V2025101401__add_issuance_point_and_sequential.sql`

```sql
ALTER TABLE invoice
ADD COLUMN id_issuance_point bigint,
ADD COLUMN sequential bigint;

CREATE INDEX idx_invoice_issuance_point_sequential ON invoice(id_issuance_point, sequential);
```

### 3. Actualización del Repositorio

Se agregó un método para obtener el máximo secuencial por punto de emisión:

**Archivo**: `src/main/java/com/wallparisoft/ebillbilling/repository/IInvoiceRepo.java`

```java
@Query("SELECT COALESCE(MAX(i.sequential), 0) FROM Invoice i WHERE i.idIssuancePoint = :idIssuancePoint")
Long findMaxSequentialByIssuancePoint(@Param("idIssuancePoint") Long idIssuancePoint);
```

### 4. Lógica de Asignación Automática

En `InvoiceServiceImpl.saveInvoice()`, se implementó la lógica para:

1. Validar que se proporcione el ID del punto de emisión
2. Obtener el último secuencial usado para ese punto de emisión
3. Incrementar en 1 para obtener el siguiente secuencial
4. Asignar el secuencial tanto a la entidad como al DTO
5. Formatear el secuencial a 9 dígitos para el SRI (ejemplo: 000000001)

**Archivo**: `src/main/java/com/wallparisoft/ebillbilling/service/impl/InvoiceServiceImpl.java`

```java
// Validate and assign sequential number based on issuance point
if (invoiceDto.getEstablishment() != null
    && invoiceDto.getEstablishment().getIssuancePoint() != null
    && invoiceDto.getEstablishment().getIssuancePoint().getIdIssuancePoint() != null) {

  Long idIssuancePoint = invoiceDto.getEstablishment().getIssuancePoint().getIdIssuancePoint();

  // Get the next sequential for this issuance point
  Long maxSequential = repo.findMaxSequentialByIssuancePoint(idIssuancePoint);
  Long nextSequential = (maxSequential != null ? maxSequential : 0L) + 1;

  // Set the sequential and issuance point ID in the invoice
  invoice.setIdIssuancePoint(idIssuancePoint);
  invoice.setSequential(nextSequential);
  invoiceDto.setIdIssuancePoint(idIssuancePoint);
  invoiceDto.setSequential(nextSequential);

  // Format sequential as a 9-digit string for the issuancePoint DTO (for SRI)
  String formattedSequential = String.format("%09d", nextSequential);
  invoiceDto.getEstablishment().getIssuancePoint().setSequentialInvoice(formattedSequential);

  log.info("Assigned sequential {} for issuance point {}", nextSequential, idIssuancePoint);
} else {
  log.error("Issuance point ID is required to generate invoice sequential");
  response.setCode(HttpStatus.BAD_REQUEST.value());
  response.setStatus(HttpStatus.BAD_REQUEST.name());
  response.setMessage("El ID del punto de emisión es requerido para generar el secuencial de la factura.");
  return response;
}
```

### 5. Actualizaciones en DTOs

- **InvoiceDto**: Se agregaron campos `idIssuancePoint` y `sequential`
- **IssuancePointDto**: Se agregó campo `idIssuancePoint`

## Cómo Funciona

1. Cuando llega un request para crear una factura, debe incluir el `idIssuancePoint` en el objeto `establishment.issuancePoint`
2. El sistema consulta el último secuencial usado para ese punto de emisión
3. Incrementa el secuencial en 1 (si no hay facturas previas, inicia en 1)
4. Asigna el secuencial a la factura antes de enviarla al SRI
5. El secuencial se formatea como string de 9 dígitos (ej: "000000001") para cumplir con el formato del SRI

## Ejemplo de Request

```json
{
  "companyDto": {
    "idCompany": 1
  },
  "establishment": {
    "code": "001",
    "address": "Av. Principal 123",
    "issuancePoint": {
      "idIssuancePoint": 10,
      "code": "001"
    }
  }
  // ... resto de los datos de la factura
}
```

## Consideraciones

- Cada punto de emisión tiene su propia secuencia de facturas que inicia en 1
- El secuencial es numérico en la base de datos (BIGINT) pero se formatea a string para el SRI
- Se valida que el `idIssuancePoint` sea proporcionado, de lo contrario se retorna un error
- El índice `idx_invoice_issuance_point_sequential` mejora el rendimiento de las consultas

## Pasos para Aplicar

1. Asegúrate de que la base de datos esté actualizada ejecutando las migraciones Flyway
2. Reinicia la aplicación para que los cambios en las entidades tomen efecto
3. En tus requests, incluye el `idIssuancePoint` en el objeto del punto de emisión
4. El sistema asignará automáticamente el secuencial correspondiente
