# 🔧 FIX: Mostrar Datos del Cliente en Lista de Borradores

**Fecha:** 6 de Diciembre 2025  
**Problema:** La tabla de borradores muestra "Cliente no especificado" porque el backend NO retorna los datos del cliente  
**Archivos a modificar:** 2 archivos

---

## 📋 Cambio 1: Agregar campos al DTO

**Archivo:** `src/main/java/com/wallparisoft/ebillbilling/dto/InvoiceDraftResponseDto.java`

**ANTES (líneas 17-52):**

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InvoiceDraftResponseDto {

  Long idInvoice;
  Long idCompany;
  Long idClient;
  LocalDateTime issueDate;
  LocalDateTime creationDate;
  String invoiceNumber;
  String environment;
  String additionalInformation;
  BigDecimal totalAmount;
  BigDecimal tip;
  BigDecimal subtotalZero;
  BigDecimal subtotalIva;
  BigDecimal subtotalIce;
  BigDecimal discount;
  Integer retryCount;
  String paymentMethodName;
  Boolean status;
}
```

**DESPUÉS (agregar estos campos):**

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InvoiceDraftResponseDto {

  Long idInvoice;
  Long idCompany;
  Long idClient;
  LocalDateTime issueDate;
  LocalDateTime creationDate;
  String invoiceNumber;
  String environment;
  String additionalInformation;
  BigDecimal totalAmount;
  BigDecimal tip;
  BigDecimal subtotalZero;
  BigDecimal subtotalIva;
  BigDecimal subtotalIce;
  BigDecimal discount;
  Integer retryCount;
  String paymentMethodName;
  Boolean status;

  // ✅ AGREGAR: Campos del cliente para la tabla de borradores
  String buyerIdentification;
  Integer buyerIdType;
  String buyerName;
  String buyerAddress;
  String buyerPhone;
  String buyerMail;

  // ✅ AGREGAR: ClientDto completo (OPCIONAL pero recomendado)
  ClientDto clientDto;
}
```

---

## 📋 Cambio 2: Mapear datos del cliente en el Service

**Archivo:** `src/main/java/com/wallparisoft/ebillbilling/service/impl/InvoiceServiceImpl.java`

**Buscar el método `mapToDraftResponseDto` (línea 293)**

**ANTES:**

```java
private InvoiceDraftResponseDto mapToDraftResponseDto(Invoice invoice) {
  return InvoiceDraftResponseDto.builder()
      .idInvoice(invoice.getIdInvoice())
      .idCompany(invoice.getIdCompany())
      .idClient(invoice.getIdClient())
      .issueDate(invoice.getIssueDate())
      .creationDate(invoice.getCreationDate())
      .invoiceNumber(invoice.getInvoiceNumber())
      .environment(invoice.getEnvironment())
      .additionalInformation(invoice.getAdditionalInformation())
      .totalAmount(invoice.getTotalAmount())
      .tip(invoice.getTip())
      .subtotalZero(invoice.getSubtotalZero())
      .subtotalIva(invoice.getSubtotalIva())
      .subtotalIce(invoice.getSubtotalIce())
      .discount(invoice.getDiscount())
      .retryCount(invoice.getRetryCount())
      .paymentMethodName(
          invoice.getPaymentMethod() != null
              ? invoice.getPaymentMethod().getPaymentMethodType()
              : null)
      .status(invoice.getStatus())
      .build();
}
```

**DESPUÉS:**

```java
private InvoiceDraftResponseDto mapToDraftResponseDto(Invoice invoice) {
  InvoiceDraftResponseDto.InvoiceDraftResponseDtoBuilder builder =
      InvoiceDraftResponseDto.builder()
          .idInvoice(invoice.getIdInvoice())
          .idCompany(invoice.getIdCompany())
          .idClient(invoice.getIdClient())
          .issueDate(invoice.getIssueDate())
          .creationDate(invoice.getCreationDate())
          .invoiceNumber(invoice.getInvoiceNumber())
          .environment(invoice.getEnvironment())
          .additionalInformation(invoice.getAdditionalInformation())
          .totalAmount(invoice.getTotalAmount())
          .tip(invoice.getTip())
          .subtotalZero(invoice.getSubtotalZero())
          .subtotalIva(invoice.getSubtotalIva())
          .subtotalIce(invoice.getSubtotalIce())
          .discount(invoice.getDiscount())
          .retryCount(invoice.getRetryCount())
          .paymentMethodName(
              invoice.getPaymentMethod() != null
                  ? invoice.getPaymentMethod().getPaymentMethodType()
                  : null)
          .status(invoice.getStatus());

  // ✅ AGREGAR: Mapear datos del cliente si existe
  if (invoice.getClient() != null) {
    Client client = invoice.getClient();

    builder
        .buyerIdentification(client.getIdentification())
        .buyerIdType(client.getIdType())
        .buyerName(client.getName())
        .buyerAddress(client.getAddress())
        .buyerPhone(client.getPhone())
        .buyerMail(client.getMail());

    // ClientDto para compatibilidad (OPCIONAL)
    ClientDto clientDto = ClientDto.builder()
        .idClient(client.getIdClient())
        .identification(client.getIdentification())
        .idType(client.getIdType())
        .name(client.getName())
        .address(client.getAddress())
        .phone(client.getPhone())
        .email(client.getMail())
        .build();

    builder.clientDto(clientDto);

    log.debug("✅ Cliente mapeado: {} ({})", client.getName(), client.getIdentification());
  } else {
    log.warn("⚠️ Borrador {} sin cliente asociado", invoice.getIdInvoice());
  }

  return builder.build();
}
```

---

## 🔍 Verificar el JOIN (IMPORTANTE)

**Archivo:** `src/main/java/com/wallparisoft/ebillbilling/repository/InvoiceRepository.java`

**Buscar el método:** `findByAuthorizationSriIsNull()`

**Si NO existe, agregar:**

```java
@Query("SELECT i FROM Invoice i " +
       "LEFT JOIN FETCH i.client " +  // ✅ Carga el cliente en una sola query
       "WHERE i.authorizationSri IS NULL " +
       "ORDER BY i.creationDate DESC")
List<Invoice> findByAuthorizationSriIsNull();
```

**Si ya existe pero NO tiene el FETCH JOIN, modificarlo para agregar:**

```java
LEFT JOIN FETCH i.client
```

---

## ✅ Resultado Esperado

Después de estos cambios, el endpoint `GET /api/invoice/drafts` retornará:

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

Y la tabla en el frontend mostrará:

| Identificación | Cliente         | Total    | Creado     | Acciones |
| -------------- | --------------- | -------- | ---------- | -------- |
| 1707030902001  | Ediciones Laser | $1897.50 | 06/11/2025 | 🔵 ✏️ 🗑️ |

---

## 🧪 Probar los Cambios

1. **Compilar:** `mvn clean compile`
2. **Reiniciar el servicio:** `./run-with-java17.sh` o `mvn spring-boot:run`
3. **Probar endpoint:** `GET http://localhost:8480/api/invoice/drafts`
4. **Verificar logs:** Buscar `✅ Cliente mapeado:` en los logs
5. **Verificar frontend:** Recargar la página de borradores

---

## 📝 Notas Importantes

- El `@Builder` de Lombok genera automáticamente los getters/setters
- El `LEFT JOIN FETCH` es CRÍTICO para evitar N+1 queries
- Si `ClientDto` no tiene `@Builder`, usar el constructor tradicional
- Los logs ayudan a debuggear si el cliente se está mapeando correctamente

-
