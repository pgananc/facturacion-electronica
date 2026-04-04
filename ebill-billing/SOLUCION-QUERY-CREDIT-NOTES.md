# Solución: Consulta de Notas de Crédito (Similar a queryInvoices)

## Resumen
Crear un servicio similar a `queryInvoices` para consultar notas de crédito con filtros y paginación.

## Archivos a Crear/Modificar

### 1. DTO: CreditNoteQueryDto
**Archivo:** `src/main/java/com/wallparisoft/ebillbilling/dto/CreditNoteQueryDto.java`

```java
package com.wallparisoft.ebillbilling.dto;

import java.time.LocalDate;
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
public class CreditNoteQueryDto {

  /** Número de nota de crédito (búsqueda parcial) Ejemplo: "001-001-000000123" o "001" */
  private String creditNoteNumber;

  /**
   * Identificación del comprador (cédula/RUC) Ejemplo: "1234567890" 
   * Nota: Se busca primero el cliente por identificación y luego sus notas de crédito
   */
  private String buyerIdentification;

  /** Fecha de creación desde (inclusive) Ejemplo: "2025-11-01" */
  private LocalDate creationDateFrom;

  /** Fecha de creación hasta (inclusive) Ejemplo: "2025-11-30" */
  private LocalDate creationDateTo;

  /** ID de la factura original (opcional) */
  private Long originalInvoiceId;

  private String receptionStatusSri;

  private String authorizationStatusSri;
}
```

### 2. DTO: CreditNoteSearchResponseDto
**Archivo:** `src/main/java/com/wallparisoft/ebillbilling/dto/CreditNoteSearchResponseDto.java`

```java
package com.wallparisoft.ebillbilling.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
public class CreditNoteSearchResponseDto {

  private Long idCreditNote;

  private String creditNoteNumber;

  private LocalDateTime creationDate;

  private Long idClient;

  private String nameClient;

  private BigDecimal totalAmount;

  private String authorizationNumber;

  private String receptionStatusSri;

  private String authorizationStatusSri;

  /**
   * Ruta donde se guardó el XML y PDF de la nota de crédito autorizada 
   * Ejemplo: "/home/credit_notes/1234567890/2025/11/11"
   */
  private String storagePath;

  /** ID de la factura original */
  private Long originalInvoiceId;

  /** Número de la factura original */
  private String originalInvoiceNumber;

  /** Motivo de la nota de crédito */
  private String reason;
}
```

### 3. Modificar: ICreditNoteService
**Archivo:** `src/main/java/com/wallparisoft/ebillbilling/service/ICreditNoteService.java`

Agregar el método:

```java
Page<CreditNoteSearchResponseDto> queryCreditNotes(CreditNoteQueryDto queryDto, Pageable pageable);
```

### 4. Modificar: CreditNoteServiceImpl
**Archivo:** `src/main/java/com/wallparisoft/ebillbilling/service/impl/CreditNoteServiceImpl.java`

Agregar los siguientes métodos:

```java
@Override
public Page<CreditNoteSearchResponseDto> queryCreditNotes(CreditNoteQueryDto queryDto, Pageable pageable) {
  log.info("Querying credit notes with criteria: {}, pageable: {}", queryDto, pageable);

  // Si se busca por identificación del comprador, primero buscar el cliente
  ClientDto clientDto = null;
  if (StringUtils.hasText(queryDto.getBuyerIdentification())) {
    try {
      ClientDtoResponse clientDtoResponse =
          customerClient.getClientByIdentification(queryDto.getBuyerIdentification());

      if (clientDtoResponse != null
          && !clientDtoResponse.getClientDtos().isEmpty()
          && clientDtoResponse.getClientDtos().get(0) != null
          && clientDtoResponse.getClientDtos().get(0).getIdClient() != null) {
        clientDto = clientDtoResponse.getClientDtos().get(0);
        log.debug(
            "Found client ID {} for identification {}",
            clientDto.getIdClient(),
            queryDto.getBuyerIdentification());
      } else {
        log.info("No client found for identification: {}", queryDto.getBuyerIdentification());
        // Si no se encuentra el cliente, retornar página vacía
        return Page.empty(pageable);
      }
    } catch (Exception ex) {
      log.warn(
          "Error searching client by identification {}: {}",
          queryDto.getBuyerIdentification(),
          ex.getMessage());
      // Si hay error al buscar el cliente, retornar página vacía
      return Page.empty(pageable);
    }
  }

  // Convertir LocalDate a LocalDateTime para la búsqueda
  LocalDateTime creationDateFrom =
      queryDto.getCreationDateFrom() != null
          ? queryDto.getCreationDateFrom().atStartOfDay()
          : null;
  LocalDateTime creationDateTo =
      queryDto.getCreationDateTo() != null
          ? queryDto.getCreationDateTo().atTime(23, 59, 59)
          : null;

  // Usar el método existente de búsqueda
  Page<CreditNote> creditNotes =
      repo.findCreditNotesByCriteria(
          queryDto.getCreditNoteNumber(),
          clientDto != null ? clientDto.getIdClient() : null,
          queryDto.getOriginalInvoiceId(),
          creationDateFrom,
          creationDateTo,
          queryDto.getReceptionStatusSri(),
          queryDto.getAuthorizationStatusSri(),
          pageable);

  // Mapear a DTO de respuesta
  Page<CreditNoteSearchResponseDto> result = creditNotes.map(this::mapToSearchResponseDto);
  
  // Si se encontró el cliente, agregar el nombre
  if (clientDto != null) {
    for (CreditNoteSearchResponseDto creditNote : result) {
      creditNote.setNameClient(clientDto.getName());
    }
  }
  
  log.info("Found {} credit notes matching criteria", result.getTotalElements());
  return result;
}

private CreditNoteSearchResponseDto mapToSearchResponseDto(CreditNote creditNote) {
  return CreditNoteSearchResponseDto.builder()
      .idCreditNote(creditNote.getIdCreditNote())
      .creditNoteNumber(creditNote.getCreditNoteNumber())
      .creationDate(creditNote.getCreationDate())
      .idClient(creditNote.getIdClient())
      .totalAmount(creditNote.getTotalAmount())
      .receptionStatusSri(
          Objects.nonNull(creditNote.getAuthorizationSri())
              ? creditNote.getAuthorizationSri().getReceptionStatusSri()
              : null)
      .authorizationStatusSri(
          Objects.nonNull(creditNote.getAuthorizationSri())
              ? creditNote.getAuthorizationSri().getAuthorizationStatusSri()
              : null)
      .storagePath(
          Objects.nonNull(creditNote.getAuthorizationSri())
              ? creditNote.getAuthorizationSri().getAuthorizationStoragePath()
              : null)
      .authorizationNumber(
          Objects.nonNull(creditNote.getAuthorizationSri())
              ? creditNote.getAuthorizationSri().getAuthorizationNumber()
              : null)
      .originalInvoiceId(creditNote.getOriginalInvoiceId())
      .originalInvoiceNumber(creditNote.getOriginalInvoiceNumber())
      .reason(creditNote.getReason())
      .build();
}
```

**Imports adicionales necesarios:**
```java
import com.wallparisoft.ebillbilling.dto.CreditNoteQueryDto;
import com.wallparisoft.ebillbilling.dto.CreditNoteSearchResponseDto;
import org.springframework.data.domain.Page;
```

### 5. Modificar: CreditNoteController
**Archivo:** `src/main/java/com/wallparisoft/ebillbilling/controller/CreditNoteController.java`

Agregar el endpoint:

```java
/**
 * Consulta notas de crédito por número de nota de crédito, identificación del comprador o fecha de creación. 
 * Solo retorna datos básicos de la nota de crédito (número, fecha, cliente, total, estado, path).
 *
 * @param queryDto Criterios de búsqueda
 * @param pageable Paginación
 * @return Página de notas de crédito encontradas
 */
@PostMapping("/query")
public ResponseEntity<Page<CreditNoteSearchResponseDto>> queryCreditNotes(
    @RequestBody CreditNoteQueryDto queryDto, Pageable pageable) {

  StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
  setLogs(
      com.wallparisoft.ebill.utils.log.EventLog.builder()
          .service(traceElement.getClassName())
          .method(traceElement.getMethodName())
          .information("Query criteria: " + queryDto + ", pageable: " + pageable),
      EventType.REQUEST,
      Level.LEVEL_001);

  Page<CreditNoteSearchResponseDto> creditNotes = creditNoteService.queryCreditNotes(queryDto, pageable);

  setLogs(
      com.wallparisoft.ebill.utils.log.EventLog.builder()
          .service(traceElement.getClassName())
          .method(traceElement.getMethodName())
          .information("Found " + creditNotes.getTotalElements() + " credit notes"),
      EventType.RESPONSE,
      Level.LEVEL_001);

  return new ResponseEntity<>(creditNotes, HttpStatus.OK);
}
```

**Imports adicionales necesarios:**
```java
import com.wallparisoft.ebillbilling.dto.CreditNoteQueryDto;
import com.wallparisoft.ebillbilling.dto.CreditNoteSearchResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
```

## Endpoint Resultante

**POST** `/api/credit-notes/query`

**Request Body (JSON):**
```json
{
  "creditNoteNumber": "001-001-000000123",
  "buyerIdentification": "1234567890",
  "creationDateFrom": "2025-11-01",
  "creationDateTo": "2025-11-30",
  "originalInvoiceId": 114,
  "receptionStatusSri": "RECIBIDA",
  "authorizationStatusSri": "AUTORIZADO"
}
```

**Query Parameters (Paginación):**
- `page`: Número de página (default: 0)
- `size`: Tamaño de página (default: 20)
- `sort`: Campo de ordenamiento (opcional)

**Response:**
```json
{
  "content": [
    {
      "idCreditNote": 1,
      "creditNoteNumber": "001-001-000000123",
      "creationDate": "2025-11-15T10:30:00",
      "idClient": 3,
      "nameClient": "Ana Ceron",
      "totalAmount": 2875.00,
      "authorizationNumber": "1234567890",
      "receptionStatusSri": "RECIBIDA",
      "authorizationStatusSri": "AUTORIZADO",
      "storagePath": "/opt/ebilling/credit_notes/1718529934/2025/11/15",
      "originalInvoiceId": 114,
      "originalInvoiceNumber": "001-001-000000323",
      "reason": "REVERSO DETALLE : Producto 001"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

## Notas

1. El repositorio `ICreditNoteRepo` ya tiene el método `findCreditNotesByCriteria` que se puede usar directamente.
2. La búsqueda por `buyerIdentification` primero busca el cliente y luego filtra las notas de crédito por `idClient`.
3. Todos los campos del `CreditNoteQueryDto` son opcionales.
4. El método `mapToSearchResponseDto` mapea la entidad `CreditNote` al DTO de respuesta, similar a como se hace con `Invoice`.









