# 🔧 FIX: Deserialización de Datos del Cliente desde ebill-customer

**Fecha:** 6 de Diciembre 2025  
**Problema:** La tabla de borradores mostraba "Cliente no especificado" porque el `CustomerClient` no podía deserializar la respuesta del microservicio `ebill-customer`.

---

## 🐛 Problema Identificado

### Síntoma

```
⚠️ No se pudo cargar cliente 11 para lista de borradores:
Error while extracting response for type [class com.wallparisoft.ebillbilling.dto.ClientDto]
and content type [application/json]
```

### Causa Raíz

El endpoint `GET /api/client/{idClient}` del microservicio `ebill-customer` **NO retorna un `ClientDto` directamente**, sino un objeto `ClientDtoResponse` con esta estructura:

```json
{
  "status": "OK",
  "clientDtos": [
    {
      "idClient": 11,
      "identification": "1707030902001",
      "name": "Ediciones Laser",
      "idType": 1,
      ...
    }
  ]
}
```

Pero nuestro `CustomerClient` en `ebill-billing` estaba esperando un `ClientDto` directamente:

```java
// ❌ ANTES (INCORRECTO)
@GetMapping("/api/client/{idClient}")
ClientDto getClientById(@PathVariable("idClient") Long idClient);
```

Esto causaba un error de deserialización porque Feign no podía mapear el JSON de respuesta a un `ClientDto` simple.

---

## ✅ Solución Implementada

### 1. Crear `ClientDtoResponse.java`

**Archivo:** `src/main/java/com/wallparisoft/ebillbilling/dto/ClientDtoResponse.java`

```java
package com.wallparisoft.ebillbilling.dto;

import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClientDtoResponse {
  String status;
  List<ClientDto> clientDtos;
}
```

### 2. Actualizar `CustomerClient.java`

**Archivo:** `src/main/java/com/wallparisoft/ebillbilling/service/CustomerClient.java`

```java
@FeignClient(
    name = "customer-service",
    url = "${service.customer.base-url}",
    configuration = FeignClientConfig.class)
public interface CustomerClient {

  @GetMapping("/api/client/{idClient}")
  ClientDtoResponse getClientById(@PathVariable("idClient") Long idClient); // ✅ Ahora retorna ClientDtoResponse

  @GetMapping("/api/product/{idProduct}")
  ProductDto getProductById(@PathVariable("idProduct") Long idProduct);
}
```

### 3. Actualizar `InvoiceServiceImpl.java` - Método `mapToDraftResponseDto()`

**Archivo:** `src/main/java/com/wallparisoft/ebillbilling/service/impl/InvoiceServiceImpl.java`

**Líneas 309-338:**

```java
// Mapear datos del cliente desde microservicio ebill-customer
if (invoice.getIdClient() != null) {
  try {
    com.wallparisoft.ebillbilling.dto.ClientDtoResponse response =
        customerClient.getClientById(invoice.getIdClient());

    // ✅ El endpoint retorna una lista de ClientDto, tomamos el primero
    if (response != null && response.getClientDtos() != null
        && !response.getClientDtos().isEmpty()) {
      com.wallparisoft.ebillbilling.dto.ClientDto clientDto =
          response.getClientDtos().get(0);

      builder.buyerIdentification(clientDto.getIdentification())
          .buyerIdType(clientDto.getIdType()).buyerName(clientDto.getName())
          .clientDto(clientDto);

      log.debug("✅ Cliente mapeado en lista: {} ({})", clientDto.getName(),
          clientDto.getIdentification());
    } else {
      log.warn("⚠️ Cliente {} no encontrado en ebill-customer", invoice.getIdClient());
    }
  } catch (Exception e) {
    log.warn("⚠️ No se pudo cargar cliente {} para lista de borradores: {}",
        invoice.getIdClient(), e.getMessage());
    // No agregar datos del cliente si falla
  }
}
```

### 4. Actualizar `InvoiceServiceImpl.java` - Método `getDraftInvoiceById()`

**Archivo:** `src/main/java/com/wallparisoft/ebillbilling/service/impl/InvoiceServiceImpl.java`

**Líneas 383-449:**

```java
if (invoice.getIdClient() != null) {
  try {
    log.info("📞 Llamando a customerClient.getClientById({})", invoice.getIdClient());

    com.wallparisoft.ebillbilling.dto.ClientDtoResponse response =
        customerClient.getClientById(invoice.getIdClient());

    log.info("✅ Respuesta del microservicio cliente: {}", response);

    // ✅ El endpoint retorna una lista de ClientDto, tomamos el primero
    if (response != null && response.getClientDtos() != null
        && !response.getClientDtos().isEmpty()) {
      com.wallparisoft.ebillbilling.dto.ClientDto clientDto =
          response.getClientDtos().get(0);

      log.info("   - idClient: {}", clientDto.getIdClient());
      log.info("   - identification: {}", clientDto.getIdentification());
      log.info("   - name: {}", clientDto.getName());
      log.info("   - idType: {}", clientDto.getIdType());

      dto.setIdClient(clientDto.getIdClient());
      dto.setBuyerIdentification(clientDto.getIdentification());
      dto.setBuyerIdType(clientDto.getIdType());
      dto.setBuyerName(clientDto.getName());
      dto.setClientDto(clientDto);

      log.info("✅ DTO después de setear cliente:");
      log.info("   - dto.getIdClient(): {}", dto.getIdClient());
      log.info("   - dto.getBuyerIdentification(): {}", dto.getBuyerIdentification());
      log.info("   - dto.getBuyerName(): {}", dto.getBuyerName());
      log.info("   - dto.getClientDto(): {}", dto.getClientDto());
    } else {
      log.warn("⚠️ Cliente {} no encontrado en ebill-customer (respuesta vacía)",
          invoice.getIdClient());

      // FALLBACK: Al menos setear el idClient aunque no tengamos los datos completos
      dto.setIdClient(invoice.getIdClient());
      dto.setBuyerName("Cliente ID: " + invoice.getIdClient() + " (no encontrado)");
      dto.setBuyerIdentification("N/A");

      com.wallparisoft.ebillbilling.dto.ClientDto emptyClientDto =
          new com.wallparisoft.ebillbilling.dto.ClientDto();
      emptyClientDto.setIdClient(invoice.getIdClient());
      dto.setClientDto(emptyClientDto);
    }

  } catch (Exception e) {
    log.error("❌ ERROR al cargar el cliente {}: {}", invoice.getIdClient(), e.getMessage(), e);
    log.error("   Tipo de excepción: {}", e.getClass().getName());
    log.error("   Stack trace completo:", e);

    // FALLBACK: Al menos setear el idClient aunque no tengamos los datos completos
    dto.setIdClient(invoice.getIdClient());
    dto.setBuyerName("Cliente ID: " + invoice.getIdClient() + " (datos no disponibles)");
    dto.setBuyerIdentification("N/A");

    com.wallparisoft.ebillbilling.dto.ClientDto emptyClientDto =
        new com.wallparisoft.ebillbilling.dto.ClientDto();
    emptyClientDto.setIdClient(invoice.getIdClient());
    dto.setClientDto(emptyClientDto);

    log.warn("⚠️ Usando datos de fallback para el cliente");
  }
} else {
  log.warn("⚠️ La factura NO tiene idClient asignado");
  dto.setClientDto(new com.wallparisoft.ebillbilling.dto.ClientDto());
}
```

---

## 📋 Archivos Modificados

1. **NUEVO:** `src/main/java/com/wallparisoft/ebillbilling/dto/ClientDtoResponse.java`
2. **MODIFICADO:** `src/main/java/com/wallparisoft/ebillbilling/service/CustomerClient.java`
3. **MODIFICADO:** `src/main/java/com/wallparisoft/ebillbilling/service/impl/InvoiceServiceImpl.java`

---

## 🧪 Probar los Cambios

### 1. Reiniciar el servicio

```bash
./run-with-java17.sh
```

### 2. Probar endpoint de lista de borradores

```bash
GET http://localhost:8480/api/invoice/drafts
Authorization: Bearer {JWT_TOKEN}
```

**Respuesta esperada:**

```json
[
  {
    "idInvoice": 22,
    "idClient": 11,
    "buyerIdentification": "1707030902001",
    "buyerIdType": 1,
    "buyerName": "Ediciones Laser",
    "totalAmount": 1897.5,
    "creationDate": "2025-11-06T17:15:59",
    "clientDto": {
      "idClient": 11,
      "identification": "1707030902001",
      "idType": 1,
      "name": "Ediciones Laser"
    }
  }
]
```

### 3. Verificar logs

Buscar en los logs:

```
✅ Cliente mapeado en lista: Ediciones Laser (1707030902001)
```

---

## 📝 Notas Importantes

1. **Estructura de Respuesta de ebill-customer:**

   - El endpoint `/api/client/{idClient}` retorna un objeto `ClientDtoResponse` con un array `clientDtos`
   - Siempre tomamos el primer elemento del array (`clientDtos.get(0)`)

2. **Manejo de Errores:**

   - Si el microservicio `ebill-customer` no responde, se usa un fallback con datos mínimos
   - Si la respuesta es vacía, se registra un warning pero no se lanza excepción

3. **Autenticación:**

   - El `FeignClientConfig` de `ebill-auth-util` automáticamente agrega el JWT token al header `Authorization`
   - No es necesario configurar manualmente el token en cada llamada

4. **Compatibilidad:**
   - Los cambios son retrocompatibles
   - Si `ebill-customer` no está disponible, el sistema sigue funcionando con datos limitados

---

## ✅ Resultado Final

Después de estos cambios:

- ✅ La tabla de borradores muestra correctamente el nombre del cliente
- ✅ El endpoint `GET /api/invoice/drafts` retorna `buyerName` y `buyerIdentification`
- ✅ El endpoint `GET /api/invoice/draft/{id}` retorna el `clientDto` completo
- ✅ Los logs muestran información detallada del proceso de carga del cliente
- ✅ Hay un mecanismo de fallback si `ebill-customer` no responde

---

**Fin del documento**
