# 📋 Requerimientos Backend - Funcionalidad de Edición de Borradores

**Microservicio:** `ebill-billing`  
**Fecha:** 18 de Octubre 2025  
**Prioridad:** Alta

---

## 🔴 PROBLEMA CRÍTICO ACTUAL (Diciembre 2025)

### ❌ Tabla de Borradores muestra "Cliente no especificado"

**Síntoma visible para el usuario:**

- En la tabla de borradores (`/billing/drafts`), la columna "Cliente" muestra "Cliente no especificado"
- Esto ocurre **incluso cuando el cliente SÍ fue seleccionado y guardado** al crear el borrador

**Causa:**

- El backend **SÍ guarda** el `idClient` en la tabla `invoice` ✅
- Pero cuando el frontend llama a `GET /api/invoice/drafts`, el backend **NO retorna** los datos del cliente ❌
- El DTO `InvoiceDraftResponseDto` NO incluye campos como `buyerName`, `buyerIdentification`, o `clientDto`

**Solución requerida:**

- Modificar `getDraftInvoices()` en `InvoiceServiceImpl` para hacer un `JOIN` con la tabla `client`
- Mapear los campos del cliente en el DTO de respuesta (ver sección "Solución Paso a Paso" más abajo)

---

## 🛠️ SOLUCIÓN PASO A PASO - Lista de Borradores

### **Paso 1: Modificar `InvoiceServiceImpl.java`**

**Ubicación:** `com.wallparisoft.ebillbilling.service.impl.InvoiceServiceImpl`

**Buscar el método:** `getDraftInvoices()` (aproximadamente línea 300-350)

**Código ACTUAL (incompleto):**

```java
@Override
public List<InvoiceDraftResponseDto> getDraftInvoices() {
    List<Invoice> drafts = invoiceRepository.findByStatus("DRAFT");

    return drafts.stream()
        .map(invoice -> {
            InvoiceDraftResponseDto dto = new InvoiceDraftResponseDto();
            dto.setIdInvoice(invoice.getId());
            dto.setIdClient(invoice.getClient().getId()); // ✅ Esto ya está
            dto.setTotalAmount(invoice.getTotalAmount());
            dto.setCreationDate(invoice.getCreationDate());
            // ❌ FALTAN los campos del cliente
            return dto;
        })
        .collect(Collectors.toList());
}
```

**Código CORREGIDO (completo):**

```java
@Override
public List<InvoiceDraftResponseDto> getDraftInvoices() {
    log.info("📋 Obteniendo lista de borradores de facturas");
    List<Invoice> drafts = invoiceRepository.findByStatus("DRAFT");
    log.info("📦 {} borradores encontrados en BD", drafts.size());

    return drafts.stream()
        .map(invoice -> {
            InvoiceDraftResponseDto dto = new InvoiceDraftResponseDto();
            dto.setIdInvoice(invoice.getId());
            dto.setTotalAmount(invoice.getTotalAmount());
            dto.setCreationDate(invoice.getCreationDate());

            // ✅ AGREGAR: Mapear datos del cliente
            if (invoice.getClient() != null) {
                Client client = invoice.getClient();

                // Campos directos del cliente (para la tabla de borradores)
                dto.setIdClient(client.getId());
                dto.setBuyerIdentification(client.getIdentification());
                dto.setBuyerIdType(client.getIdType());
                dto.setBuyerName(client.getName());
                dto.setBuyerAddress(client.getAddress());
                dto.setBuyerPhone(client.getPhone());
                dto.setBuyerMail(client.getMail());

                // ClientDto para compatibilidad (OPCIONAL pero recomendado)
                ClientDto clientDto = new ClientDto();
                clientDto.setIdClient(client.getId());
                clientDto.setIdentification(client.getIdentification());
                clientDto.setIdType(client.getIdType());
                clientDto.setName(client.getName());
                clientDto.setAddress(client.getAddress());
                clientDto.setPhone(client.getPhone());
                clientDto.setEmail(client.getMail());
                dto.setClientDto(clientDto);

                log.debug("✅ Cliente mapeado: {} ({})", client.getName(), client.getIdentification());
            } else {
                log.warn("⚠️ Borrador {} sin cliente asociado", invoice.getId());
            }

            return dto;
        })
        .collect(Collectors.toList());
}
```

---

### **Paso 2: Verificar `InvoiceDraftResponseDto.java`**

**Ubicación:** `com.wallparisoft.ebillbilling.dto.InvoiceDraftResponseDto`

**Asegurarse de que tenga estos campos:**

```java
package com.wallparisoft.ebillbilling.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class InvoiceDraftResponseDto {
    // Campos básicos del borrador
    private Long idInvoice;
    private Long idClient;
    private BigDecimal totalAmount;
    private LocalDateTime creationDate;

    // ✅ AGREGAR: Campos del cliente (si no existen)
    private String buyerIdentification;
    private Integer buyerIdType;
    private String buyerName;
    private String buyerAddress;
    private String buyerPhone;
    private String buyerMail;

    // ✅ AGREGAR: ClientDto completo (OPCIONAL pero recomendado)
    private ClientDto clientDto;
}
```

**NOTA:** Si usas `@Data` de Lombok, los getters/setters se generan automáticamente.

---

### **Paso 3: Verificar el JOIN en `InvoiceRepository.java`**

**Ubicación:** `com.wallparisoft.ebillbilling.repository.InvoiceRepository`

**Opción A - Query personalizada con FETCH JOIN (RECOMENDADO):**

```java
@Query("SELECT i FROM Invoice i " +
       "LEFT JOIN FETCH i.client " +  // ✅ Carga el cliente en una sola query
       "WHERE i.status = :status " +
       "ORDER BY i.creationDate DESC")
List<Invoice> findByStatus(@Param("status") String status);
```

**Opción B - Método por defecto (funciona pero hace queries adicionales):**

```java
List<Invoice> findByStatusOrderByCreationDateDesc(String status);
```

Con esta opción, Hibernate hará el JOIN automáticamente al acceder a `invoice.getClient()`.

---

### **Paso 4: Probar el Endpoint**

**URL:** `GET http://localhost:8480/api/invoice/drafts`

**Respuesta esperada:**

```json
[
  {
    "idInvoice": 22,
    "idClient": 11,
    "buyerIdentification": "1707030902001",
    "buyerIdType": 1,
    "buyerName": "Ediciones Laser",
    "buyerAddress": "Atahualpa",
    "buyerPhone": "0999999999",
    "buyerMail": "elaser@gmail.com",
    "totalAmount": 1897.5,
    "creationDate": "2025-11-06T17:15:59",
    "clientDto": {
      "idClient": 11,
      "identification": "1707030902001",
      "idType": 1,
      "name": "Ediciones Laser",
      "address": "Atahualpa",
      "phone": "0999999999",
      "email": "elaser@gmail.com"
    }
  }
]
```

---

### **Paso 5: Verificar en el Frontend**

Después de estos cambios, la tabla de borradores mostrará:

| Identificación | Cliente         | Total    | Creado     | Acciones |
| -------------- | --------------- | -------- | ---------- | -------- |
| 1707030902001  | Ediciones Laser | $1897.50 | 06/11/2025 | 🔵 ✏️ 🗑️ |

En lugar de:

| Identificación     | Cliente                 | Total    | Creado     | Acciones |
| ------------------ | ----------------------- | -------- | ---------- | -------- |
| Sin identificación | Cliente no especificado | $1897.50 | 06/11/2025 | 🔵 ✏️ 🗑️ |

---

## 🚨 ESTADO ACTUAL DEL BUG (Edición de Borradores)

### Síntoma:

Cuando el frontend llama a `GET /api/invoice/draft/{id}`, el backend retorna datos **incompletos**:

```json
{
  "idInvoice": 21,
  "idClient": 1, // ✅ Se guarda correctamente
  "details": [
    {
      "idProduct": 2,
      "unitPrice": 1650,
      "quantity": 1,
      "code": null, // ❌ FALTA
      "description": null, // ❌ FALTA
      "productDto": null // ❌ FALTA
    }
  ],
  "buyerIdentification": null, // ❌ FALTA - CRÍTICO para mostrar en tabla de borradores
  "buyerIdType": null, // ❌ FALTA
  "buyerName": null, // ❌ FALTA - CRÍTICO para mostrar en tabla de borradores
  "buyerAddress": null, // ❌ FALTA
  "buyerPhone": null, // ❌ FALTA
  "buyerMail": null, // ❌ FALTA
  "clientDto": null, // ❌ FALTA (alternativa a buyerXXX)
  "payments": [] // ❌ FALTA (cuando debería haber 1 pago)
}
```

**NOTA IMPORTANTE:** El frontend muestra "Cliente no especificado" en la tabla de borradores porque el backend NO está retornando `buyerName` ni `clientDto.name`.

### Causa Raíz:

El método `getDraftInvoiceById()` en `InvoiceServiceImpl.java` (línea ~334) **NO está mapeando correctamente** los datos relacionados:

- ❌ No mapea `code` ni `description` de los productos
- ❌ No mapea `productDto` con información del producto
- ❌ **NO mapea `buyerXXX` ni `clientDto` del cliente** ← **ESTO ES LO MÁS CRÍTICO**
- ❌ No mapea correctamente el array de `payments`

### ⚠️ Problema Específico con el Cliente:

El backend **SÍ guarda** el `idClient` en la tabla `invoice`, pero cuando recupera el borrador:

1. ❌ NO hace un `JOIN` con la tabla `client` para obtener los datos del cliente
2. ❌ NO mapea los campos `buyerIdentification`, `buyerName`, etc. desde la entidad `Invoice`
3. ❌ NO incluye un `clientDto` en el DTO de respuesta

**Resultado:** El frontend no puede mostrar el nombre del cliente en la tabla de borradores.

### Impacto:

- El formulario de facturación no puede cargar el borrador para editar
- El usuario ve campos vacíos en lugar de los datos guardados
- La funcionalidad "Editar Borrador" está **completamente rota**

---

## 🎯 Objetivo

Implementar el endpoint para **obtener un borrador específico por ID** que permita al frontend cargar un borrador en el formulario de facturación para continuar editándolo.

---

## 📝 Cambio 1: Nuevo Endpoint en `InvoiceController.java`

**Ubicación:** `com.wallparisoft.ebillbilling.controller.InvoiceController`

**Agregar después del método `getDraftInvoices()`:**

```java
@GetMapping("/draft/{id}")
public ResponseEntity<InvoiceDto> getDraftById(@PathVariable("id") Long idInvoice) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information("Fetching draft invoice with ID: " + idInvoice),
            EventType.REQUEST, Level.LEVEL_001);

    InvoiceDto draft = invoiceService.getDraftInvoiceById(idInvoice);

    setLogs(com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information("Draft invoice found with " +
                        (draft.getDetails() != null ? draft.getDetails().size() : 0) +
                        " details and " +
                        (draft.getPayments() != null ? draft.getPayments().size() : 0) +
                        " payments"),
            EventType.RESPONSE, Level.LEVEL_001);

    return new ResponseEntity<>(draft, HttpStatus.OK);
}
```

**URL resultante:** `GET http://localhost:8480/api/invoice/draft/{id}`

---

## 📝 Cambio 2: Método en la Interfaz `IInvoiceService.java`

**Ubicación:** `com.wallparisoft.ebillbilling.service.IInvoiceService`

**Agregar:**

```java
/**
 * Obtiene un borrador específico por su ID para edición
 *
 * @param idInvoice ID de la factura borrador
 * @return DTO con todos los datos del borrador incluyendo detalles y pagos
 * @throws ResourceNotFoundException si el borrador no existe
 * @throws BadRequestException si la factura ya fue procesada
 */
InvoiceDto getDraftInvoiceById(Long idInvoice);
```

---

## 📝 Cambio 3: Implementación en `InvoiceServiceImpl.java`

**Ubicación:** `com.wallparisoft.ebillbilling.service.impl.InvoiceServiceImpl`

### ⚠️ PROBLEMA ACTUAL DETECTADO:

El frontend está recibiendo:

```
- code: undefined
- description: undefined
- productDto: undefined
- buyerIdentification: undefined (y todos los buyerXXX)
```

Esto indica que **el mapper NO está poblando los datos relacionados** (producto, cliente, formas de pago).

**Agregar (asegurando que TODOS los campos se mapeen correctamente):**

```java
@Override
public InvoiceDto getDraftInvoiceById(Long idInvoice) {
    log.info("📝 Buscando borrador con ID: {}", idInvoice);

    // 1. Buscar la factura por ID
    Invoice invoice = invoiceRepository.findById(idInvoice)
            .orElseThrow(() -> {
                log.error("❌ Borrador con ID {} no encontrado", idInvoice);
                return new ResourceNotFoundException("Borrador no encontrado con ID: " + idInvoice);
            });

    // 2. Verificar que sea un borrador (authorizationSri == null o vacío)
    if (invoice.getAuthorizationSri() != null && !invoice.getAuthorizationSri().trim().isEmpty()) {
        log.warn("⚠️ La factura {} ya fue procesada", idInvoice);
        throw new BadRequestException(
            "Esta factura ya fue procesada y autorizada por el SRI. No es un borrador editable."
        );
    }

    log.info("✅ Borrador encontrado. Cliente: {}, Total: ${}",
             invoice.getClient().getName(),
             invoice.getTotalAmount());

    // 3. Construir el DTO de respuesta
    InvoiceDto dto = new InvoiceDto();

    // === Datos básicos ===
    dto.setIdInvoice(invoice.getId());
    dto.setIdCompany(invoice.getCompany().getId());
    dto.setTotalAmount(invoice.getTotalAmount());
    dto.setDiscount(invoice.getDiscount());
    dto.setTip(invoice.getTip());
    dto.setSubtotalZero(invoice.getSubtotalZero());
    dto.setSubtotalIva(invoice.getSubtotalIva());
    dto.setSubtotalIce(invoice.getSubtotalIce());
    dto.setSubtotalDiscount(invoice.getSubtotalDiscount());
    dto.setStatus("DRAFT");
    dto.setAuthorizationSri(null);
    dto.setIssueDate(invoice.getIssueDate());
    dto.setCreationDate(invoice.getCreationDate());
    dto.setAdditionalInformation(invoice.getAdditionalInformation());
    dto.setEnvironment(invoice.getEnvironment());

    // === Datos del cliente ===
    Client client = invoice.getClient();
    dto.setIdClient(client.getId());
    dto.setBuyerIdentification(client.getIdentification());
    dto.setBuyerIdType(client.getIdType());
    dto.setBuyerName(client.getName());
    dto.setBuyerAddress(client.getAddress());
    dto.setBuyerPhone(client.getPhone());
    dto.setBuyerMail(client.getMail());

    // ClientDto para compatibilidad
    ClientDto clientDto = new ClientDto();
    clientDto.setIdClient(client.getId());
    clientDto.setIdentification(client.getIdentification());
    clientDto.setIdType(client.getIdType());
    clientDto.setName(client.getName());
    clientDto.setAddress(client.getAddress());
    clientDto.setPhone(client.getPhone());
    clientDto.setEmail(client.getMail());
    dto.setClientDto(clientDto);

    // === Establecimiento ===
    EstablishmentDto establishmentDto = new EstablishmentDto();
    establishmentDto.setIdEstablishment(invoice.getEstablishment().getId());
    establishmentDto.setCode(invoice.getEstablishment().getCode());
    establishmentDto.setAddress(invoice.getEstablishment().getAddress());
    dto.setEstablishment(establishmentDto);

    // === Detalles de productos ===
    List<InvoiceDetailDto> detailsDto = invoice.getDetails().stream()
            .map(detail -> {
                InvoiceDetailDto detailDto = new InvoiceDetailDto();
                detailDto.setIdInvoiceDetail(detail.getId());
                detailDto.setIdProduct(detail.getProduct().getId());
                detailDto.setUnitPrice(detail.getUnitPrice());
                detailDto.setDiscount(detail.getDiscount());
                detailDto.setSubtotal(detail.getSubtotal());
                detailDto.setQuantity(detail.getQuantity());

                // ⚠️ CRÍTICO: Campos adicionales (ACTUALMENTE FALTA)
                detailDto.setCode(detail.getProduct().getMainCode());
                detailDto.setDescription(detail.getProduct().getName());
                detailDto.setIvaPercentage(
                    detail.getIvaPercentage() != null ? detail.getIvaPercentage() : 0
                );

                // ⚠️ CRÍTICO: ProductDto (ACTUALMENTE FALTA)
                ProductDtoSimple productDto = new ProductDtoSimple();
                productDto.setIdProduct(detail.getProduct().getId());
                productDto.setName(detail.getProduct().getName());
                productDto.setDescription(detail.getProduct().getDescription());
                productDto.setUnitPrice(detail.getProduct().getPrice());
                detailDto.setProductDto(productDto);

                return detailDto;
            })
            .collect(Collectors.toList());

    dto.setInvoiceDetails(detailsDto);
    dto.setDetails(detailsDto); // Alias
    log.info("📦 {} productos cargados", detailsDto.size());

    // === Formas de pago ===
    List<PaymentDto> paymentsDto = invoice.getPayments().stream()
            .map(payment -> {
                PaymentDto paymentDto = new PaymentDto();
                paymentDto.setIdPayment(payment.getId());
                paymentDto.setIdPaymentMethod(payment.getPaymentMethod().getId());
                paymentDto.setPaymentMethod(payment.getPaymentMethod().getName());
                paymentDto.setPaymentType(payment.getPaymentMethod().getType());
                paymentDto.setPaymentMethodCode(payment.getPaymentMethod().getCode());
                paymentDto.setTotal(payment.getTotal());
                paymentDto.setValue(payment.getTotal());
                paymentDto.setTerm(payment.getTerm());
                paymentDto.setTimeUnit(payment.getTimeUnit());
                paymentDto.setTime(payment.getTimeUnit());
                return paymentDto;
            })
            .collect(Collectors.toList());

    dto.setPayments(paymentsDto);
    log.info("💳 {} formas de pago cargadas", paymentsDto.size());

    // Primera forma de pago como paymentMethod
    if (!paymentsDto.isEmpty()) {
        PaymentMethodDto paymentMethodDto = new PaymentMethodDto();
        PaymentDto firstPayment = paymentsDto.get(0);
        paymentMethodDto.setIdPaymentMethod(firstPayment.getIdPaymentMethod());
        paymentMethodDto.setPaymentType(firstPayment.getPaymentType());
        paymentMethodDto.setValue(firstPayment.getTotal());
        paymentMethodDto.setTerm(firstPayment.getTerm());
        paymentMethodDto.setTimeUnit(firstPayment.getTimeUnit());
        dto.setPaymentMethod(paymentMethodDto);
    }

    log.info("✅ Borrador completo retornado para edición (ID: {})", idInvoice);
    return dto;
}
```

---

## 📝 Cambio 4: Campos adicionales en `InvoiceDto`

**Ubicación:** `com.wallparisoft.ebillbilling.dto.InvoiceDto`

**Verificar que existan estos campos (agregar si no existen):**

```java
// Campos del cliente para facilitar carga en frontend
private Long idClient;
private String buyerIdentification;
private Integer buyerIdType;
private String buyerName;
private String buyerAddress;
private String buyerPhone;
private String buyerMail;

// Alias para compatibilidad
private List<InvoiceDetailDto> details; // Alias de invoiceDetails

// Array de formas de pago
private List<PaymentDto> payments;

// Fecha de creación
private LocalDateTime creationDate;

// Ambiente (si no existe)
private String environment;
```

---

## 📝 Cambio 5: Campos adicionales en `InvoiceDetailDto`

**Ubicación:** `com.wallparisoft.ebillbilling.dto.InvoiceDetailDto`

**Verificar que existan estos campos:**

```java
// Campos para facilitar edición
private String code; // Código del producto (MainCode)
private String description; // Nombre/descripción
private Integer ivaPercentage; // Porcentaje de IVA aplicado
private ProductDtoSimple productDto; // Información completa del producto
```

---

## 📝 Cambio 6: DTO completo `PaymentDto`

**Ubicación:** `com.wallparisoft.ebillbilling.dto.PaymentDto`

**Verificar que tenga esta estructura completa:**

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentDto {
    private Long idPayment;
    private Long idPaymentMethod;
    private String paymentMethod; // Nombre de la forma de pago
    private String paymentType; // Tipo (ej: "SIN UTILIZACION DEL SISTEMA FINANCIERO")
    private String paymentMethodCode; // Código SRI (ej: "01")
    private Double total; // Monto de esta forma de pago
    private Double value; // Alias para total (compatibilidad frontend)
    private Integer term; // Plazo
    private String timeUnit; // Unidad de tiempo (ej: "DÍAS")
    private String time; // Alias para timeUnit (compatibilidad frontend)
}
```

---

## 📝 Cambio 7: DTO `ProductDtoSimple` (si no existe)

**Ubicación:** `com.wallparisoft.ebillbilling.dto.ProductDtoSimple`

**Crear si no existe:**

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDtoSimple {
    private Long idProduct;
    private String name;
    private String description;
    private Double unitPrice;
}
```

---

## ✅ Ejemplo de Response Esperado

Cuando el frontend llame a `GET /api/invoice/draft/21`, debe recibir:

```json
{
  "idInvoice": 21,
  "idCompany": 1,
  "idClient": 5,
  "buyerIdentification": "1234567890001",
  "buyerIdType": 1,
  "buyerName": "Cliente ABC S.A.",
  "buyerAddress": "Av. Principal 123",
  "buyerPhone": "0999999999",
  "buyerMail": "cliente@example.com",
  "totalAmount": 1250.5,
  "discount": 50.0,
  "tip": 0.0,
  "subtotalZero": 1000.0,
  "subtotalIva": 200.5,
  "subtotalIce": 0.0,
  "subtotalDiscount": 50.0,
  "status": "DRAFT",
  "authorizationSri": null,
  "issueDate": "2025-10-18T15:30:00",
  "creationDate": "2025-10-18T10:15:00",
  "additionalInformation": "Información adicional",
  "environment": "PRUEBAS",
  "clientDto": {
    "idClient": 5,
    "identification": "1234567890001",
    "idType": 1,
    "name": "Cliente ABC S.A.",
    "address": "Av. Principal 123",
    "phone": "0999999999",
    "email": "cliente@example.com"
  },
  "establishment": {
    "idEstablishment": 1,
    "code": "001",
    "address": "Matriz - Av. Principal"
  },
  "details": [
    {
      "idInvoiceDetail": 101,
      "idProduct": 1,
      "code": "PROD-001",
      "description": "Producto de prueba",
      "unitPrice": 100.0,
      "discount": 5.0,
      "subtotal": 95.0,
      "quantity": 10,
      "ivaPercentage": 15,
      "productDto": {
        "idProduct": 1,
        "name": "Producto de prueba",
        "description": "Descripción del producto",
        "unitPrice": 100.0
      }
    }
  ],
  "invoiceDetails": [
    // ... mismo contenido que "details" (para compatibilidad)
  ],
  "payments": [
    {
      "idPayment": 201,
      "idPaymentMethod": 1,
      "paymentMethod": "Efectivo",
      "paymentType": "SIN UTILIZACION DEL SISTEMA FINANCIERO",
      "paymentMethodCode": "01",
      "total": 1250.5,
      "value": 1250.5,
      "term": null,
      "timeUnit": null,
      "time": null
    }
  ],
  "paymentMethod": {
    // ... Primera forma de pago (para compatibilidad)
    "idPaymentMethod": 1,
    "paymentType": "SIN UTILIZACION DEL SISTEMA FINANCIERO",
    "value": 1250.5,
    "term": null,
    "timeUnit": null
  }
}
```

---

## 🧪 Criterios de Prueba

Una vez implementados los cambios:

1. **Prueba 1 - Borrador existente:**

   ```bash
   GET http://localhost:8480/api/invoice/draft/21
   Headers: Authorization: Bearer {JWT_TOKEN}
   Resultado esperado: HTTP 200 con JSON completo del borrador
   ```

2. **Prueba 2 - Borrador no encontrado:**

   ```bash
   GET http://localhost:8480/api/invoice/draft/99999
   Resultado esperado: HTTP 404 con mensaje "Borrador no encontrado"
   ```

3. **Prueba 3 - Factura ya procesada:**

   ```bash
   GET http://localhost:8480/api/invoice/draft/{id_factura_autorizada}
   Resultado esperado: HTTP 400 con mensaje "Ya fue procesada"
   ```

4. **Prueba 4 - Sin autenticación:**
   ```bash
   GET http://localhost:8480/api/invoice/draft/21
   (Sin header Authorization)
   Resultado esperado: HTTP 401 Unauthorized
   ```

---

## 📌 Notas Importantes

1. **Seguridad:** El endpoint debe validar que el usuario tenga permisos para acceder al borrador (verificar que pertenezca a su compañía).

2. **Performance:** Si hay muchos detalles o pagos, considerar usar paginación o limitar la cantidad retornada.

3. **Compatibilidad:** El DTO incluye campos duplicados (`details` vs `invoiceDetails`, `time` vs `timeUnit`, `value` vs `total`) para mantener compatibilidad con diferentes versiones del frontend.

4. **Logging:** Los logs con emojis (📝, ✅, ❌) son opcionales, pueden usarse logs estándar.

5. **Excepciones:** Usar las excepciones existentes del proyecto (`ResourceNotFoundException`, `BadRequestException`).

---

## 🔗 Dependencias

Este endpoint depende de:

- ✅ `invoiceRepository.findById()` - Ya existe
- ✅ Entidades `Invoice`, `Client`, `InvoiceDetail`, `Payment`, `PaymentMethod`, `Product` - Ya existen
- ✅ DTOs básicos - Ya existen
- ❓ Campos adicionales en DTOs - Verificar y agregar si faltan

---

## 📅 Estimación

- **Tiempo estimado:** 2-3 horas
- **Complejidad:** Media
- **Riesgo:** Bajo (solo lectura, no modifica datos)

---

**Fin del documento**
